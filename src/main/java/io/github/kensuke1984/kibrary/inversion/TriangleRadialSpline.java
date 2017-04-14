package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class TriangleRadialSpline {
	Map<PartialType, Integer[]> nNewParameter;
	private List<UnknownParameter> originalUnknowns;
	private List<UnknownParameter> newUnknowns;
	private final double UMLMB = 5721.;
	
	private int iUM, iLM;
	private Map<PartialType, Integer[]> orders;
	private double dz;
	
	public TriangleRadialSpline(Map<PartialType, Integer[]> nNewParameter, List<UnknownParameter> unknowns) {
		this.nNewParameter = nNewParameter;
		this.originalUnknowns = unknowns;
		this.newUnknowns = new ArrayList<>();
		this.orders = new HashMap<>();
		
		for (PartialType type : nNewParameter.keySet()) {
			int nUM = nNewParameter.get(type)[0];
			int nLM = nNewParameter.get(type)[1];
			List<UnknownParameter> typeUnknowns = unknowns.stream().filter(p -> p.getPartialType().equals(type)).collect(Collectors.toList());
			
			// check constant dz
			double testDz = Math.abs((Double) typeUnknowns.get(typeUnknowns.size() - 1).getLocation()
					- (Double) typeUnknowns.get(typeUnknowns.size() - 2).getLocation());
			for (int i = 0; i < typeUnknowns.size() - 1; i++) {
				dz = (Double) typeUnknowns.get(i + 1).getLocation() - (Double) typeUnknowns.get(i).getLocation();
				if (dz != testDz)
					throw new RuntimeException("Variable radial spacing not allowed");
			}
			
			iUM = 0;
			iLM = 0;
			for (UnknownParameter p : typeUnknowns) {
				double r = (Double) p.getLocation();
				if (r > 5721.)
					iUM++;
				else if (r < 5721.)
					iLM++;
			}
//			iUM--; // crustal layer
			System.out.println("iUM: "+iUM+", iLM: "+iLM+", nUM: "+nUM+", nLM: "+nLM);
			System.out.println(iLM / (nLM - 1) +" "+ ((double) iLM) / (nLM - 1));
			System.out.println((double) iLM);
			System.out.println(iLM + " " + (nLM-1));
			System.out.println(iLM/(nLM-1));
			System.out.println(iLM / (nLM - 1) * 2);
			if (nUM > iUM || nLM > iLM)
				throw new RuntimeException("nUM or nLM greater than the number of parameters available");
			
			if (iUM / (nUM - 1) != ((double) iUM) / (nUM - 1))
				throw new RuntimeException("nUM is incompatible with the number of parameters available in the upper mantle " + nUM + " " + iUM);
			if (iLM / (nLM - 1) != ((double) iLM) / (nLM - 1)){
				System.out.println(iLM / (nLM - 1) +" "+ ((double) iLM) / (nLM - 1));
				System.out.println((double) iLM);
				System.out.println(iLM + " " + (nLM-1));
				System.out.println(iLM/(nLM-1));
				System.out.println(iLM / (nLM - 1) * 2);
				throw new RuntimeException("nLM is incompatible with the number of parameters available in the lower mantle " + nLM + " " + iLM);
			}
			int orderUM = iUM / (nUM - 1) * 2;
			int orderLM = iLM / (nLM - 1) * 2;
			orders.put(type, new Integer[] {orderUM, orderLM});
			
			for (int i = 0; i < nLM; i++) {
				UnknownParameter tmpUnknown = null;
				double perturbationR = orderLM / 2 * dz * i + 3480.;
				double weighting = orderLM * dz;
				if (i == nLM - 1) {
					weighting /= 2;
					perturbationR = (nLM - 1) * orderLM / 2. * dz - orderLM / 4. * dz + 3480.;
				}
				if (i == 0) {
					weighting /= 2;
					perturbationR = orderLM / 4. * dz + 3480.;
				}
				switch (type) {
				case PAR2:
					tmpUnknown = new Physical1DParameter(PartialType.PAR2, perturbationR, weighting);
					break;
				case PARQ:
					tmpUnknown = new Physical1DParameter(PartialType.PARQ, perturbationR, weighting);
				default:
					break;
				}
				newUnknowns.add(tmpUnknown);
			}
			
			for (int i = 0; i < nUM; i++) {
				UnknownParameter tmpUnknown = null;
				double perturbationR = orderUM / 2 * dz * i + 5720.;
				double weighting = orderUM * dz;
				if (i == nUM - 1) {
					weighting /= 2;
					perturbationR = (nUM - 1) * orderUM / 2. * dz - orderUM / 4. * dz + 5720.;
				}
				if (i == 0) {
					weighting /= 2;
					perturbationR = orderUM / 4. * dz + 5720.;
				}
				switch (type) {
				case PAR2:
					tmpUnknown = new Physical1DParameter(PartialType.PAR2, perturbationR, weighting);
					break;
				case PARQ:
					tmpUnknown = new Physical1DParameter(PartialType.PARQ, perturbationR, weighting);
				default:
					break;
				}
				newUnknowns.add(tmpUnknown);
			}
		}
	}
	
	public Map<UnknownParameter, Double> computeCoefficients(UnknownParameter newParameter) {
		Map<UnknownParameter, Double> coefficientMap = new HashMap<>();
		double r = (Double) newParameter.getLocation();
		List<UnknownParameter> neighbors = getNeighbors(newParameter);
		boolean isBoundaryParameter = isBoundaryParameter(newParameter);
		
		int order = isBoundaryParameter ? neighbors.size() : neighbors.size() / 2;
		int n = order;
		System.out.println((Double) newParameter.getLocation());
		neighbors.stream().forEach(pp -> {
			if (!isBoundaryParameter) {
				double dr = Math.abs(r - (Double) pp.getLocation());
				double res = (1 - dr / (n * dz));
				System.out.println(pp.getLocation() + " " + res);
				coefficientMap.put(pp, res);
			}
			else if (isDownBoundary(newParameter)) {
				double dr = - r + (Double) pp.getLocation();
				double res = 0.5 * (1 - 2 * dr / (n * dz));
				System.out.println(pp.getLocation() + " " + res);
				coefficientMap.put(pp, res);
			}
			else { // upBoundary
				double dr = r - (Double) pp.getLocation();
				double res = 0.5 * (1 - 2 * dr / (n * dz));
				System.out.println(pp.getLocation() + " " + res);
				coefficientMap.put(pp, res);
			}
		});
		
		return coefficientMap;
	}
	
	private List<UnknownParameter> getNeighbors(UnknownParameter target) {
		List<UnknownParameter> neighbors = new ArrayList<>();
		double targetRadius = (Double) target.getLocation();
		PartialType type = target.getPartialType();
		boolean isUM = targetRadius > UMLMB ? true : false;
		
		int order = 0;
		if (isUM)
			order = orders.get(type)[0] / 2;
		else
			order = orders.get(type)[1] / 2;
		
		int imax = isBoundaryParameter(target) ? order / 2 : order;
		System.out.println((double) target.getLocation() + " " + isBoundaryParameter(target));
		for (UnknownParameter p : originalUnknowns.stream().filter(p -> p.getPartialType().equals(type)).collect(Collectors.toList())) {
			double r = (Double) p.getLocation();
			if (isUM && r < UMLMB)
				continue;
			for (int i = 0; i < imax; i++) {
				double dr = (i + .5) * dz;
				boolean tmp = true;
				if (isUM && r < UMLMB)
					tmp = false;
				if (!isUM && r > UMLMB)
					tmp = false;
//				if (r == 6350.) // need to be modified
//					continue;
				if (tmp && targetRadius == r + dr)
					neighbors.add(p);
			}
			for (int i = 0; i < imax; i++) {
				boolean tmp = true;
				double dr = (i + .5) * dz;
				if (isUM && r < UMLMB)
					tmp = false;
				if (!isUM && r > UMLMB)
					tmp = false;
//				if (r == 6350.) // need to be modified
//					continue;
				if (tmp && targetRadius == r - dr)
					neighbors.add(p);
			}
		}
		
		return neighbors;
	}
	
	public boolean isUpBoundary(UnknownParameter newParameter) {
		double r = (Double) newParameter.getLocation();
		double halfLength = r > UMLMB ? orders.get(newParameter.getPartialType())[0] / 2 * dz
				: orders.get(newParameter.getPartialType())[1] / 2 * dz;
		if (Earth.EARTH_RADIUS - r < halfLength - dz / 4.)
			return true;
		else if (UMLMB - r > 0 && UMLMB - r < halfLength - dz / 4.)
			return true;
		return false;
	}
	
	public boolean isDownBoundary(UnknownParameter newParameter) {
		double r = (Double) newParameter.getLocation();
		double halfLength = r > UMLMB ? orders.get(newParameter.getPartialType())[0] / 2 * dz
				: orders.get(newParameter.getPartialType())[1] / 2 * dz;
		if (r - UMLMB > 0 && r - UMLMB < halfLength - dz / 4.)
			return true;
		else if (r - 3480. < halfLength - dz / 4.)
			return true;
		return false;
	}
	
	public boolean isBoundaryParameter(UnknownParameter newParameter) {
		return isUpBoundary(newParameter) || isDownBoundary(newParameter);
	}
	
	public void writeNewParameters(Path outPath) throws IOException {
		UnknownParameterFile.write(newUnknowns.stream().collect(Collectors.toList()), outPath);
	}
	
	public Matrix computeNewA(RealMatrix a) {
		Matrix newA = new Matrix(a.getRowDimension(), newUnknowns.size());
		
		for (int i = 0; i < newUnknowns.size(); i++) {
			Map<UnknownParameter, Double> coefficients = computeCoefficients(newUnknowns.get(i));
			List<UnknownParameter> neighbors = getNeighbors(newUnknowns.get(i));
			RealVector column = new ArrayRealVector(a.getRowDimension());
			for (UnknownParameter p : neighbors) {
				int j = 0;
				boolean found = false;
				UnknownParameter tmp = null;
				for (UnknownParameter pp : originalUnknowns) {
					if (pp.equals(p)) {
						found = true;
						tmp = p;
					}
					if (!found)
						j++;
				}
				System.out.println((Double) newUnknowns.get(i).getLocation() + " " + (Double) tmp.getLocation());
				column = column.add(a.getColumnVector(j)).mapMultiply(coefficients.get(p));
			}
			System.out.println((Double) newUnknowns.get(i).getLocation() + " " + column.getLInfNorm());
			newA.setColumnVector(i, column);
		}
		
		return newA;
	}
	
	public List<UnknownParameter> getOriginalUnknowns() {
		return originalUnknowns;
	}
	
	public List<UnknownParameter> getNewParameters() {
		return newUnknowns;
	}
}