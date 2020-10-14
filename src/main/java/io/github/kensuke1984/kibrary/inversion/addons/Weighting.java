package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.inversion.ConjugateGradientMethod;
import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.inversion.NonNegativeLeastSquaresMethod;
import io.github.kensuke1984.kibrary.inversion.ObservationEquation;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import com.sun.jna.ptr.ByteByReference;

public class Weighting {
	
//	public static void main(String[] args) {
//		byte[] bytes =  ByteBuffer.allocate(4).putInt(WeightingType.RECIPROCAL_AZED_TZCA.getValue()).array();
//		ByteBuffer bb = ByteBuffer.wrap(bytes);
//		WeightingType type = WeightingType.getType(bb.getShort());
//		System.out.println(bytes.length);
//		System.out.println(type);
//	}
	
	public static double[] LowerUpperMantle1D(PartialID[] ids) {
		double[] lowerUpperW = new double[2];
		Map<Phases, Double> phasesSensitivityMap = Sensitivity.sensitivityPerWindowType(ids);
		Set<Phases> keySet = phasesSensitivityMap.keySet();
		Set<Phases> lowerMantle = keySet.stream().filter(phases -> phases.isLowerMantle())
				.collect(Collectors.toSet());
		Set<Phases> upperMantle = keySet.stream().filter(phases -> phases.isUpperMantle())
				.collect(Collectors.toSet());
		double upperMantleSensitivity = 0;
		double lowerMantleSensitivity = 0;
		for (Map.Entry<Phases, Double> entry : phasesSensitivityMap.entrySet()) {
			Phases p = entry.getKey();
			double s = entry.getValue();
			if (upperMantle.contains(p))
				upperMantleSensitivity += s;
			else if (lowerMantle.contains(p))
				lowerMantleSensitivity += s;
		}
		lowerUpperW[0] = 1. / lowerMantleSensitivity;
		lowerUpperW[1] = 1. / upperMantleSensitivity;
		return lowerUpperW;
	}
	
	public static double[] TakeuchiKobayashi1D(PartialID[] ids, List<UnknownParameter> parameterList, Dvector dVector, double gamma) {
		int n = dVector.getNTimeWindow();
		
		Matrix a = setAForSSL(ids, parameterList, dVector, gamma);
		
		RealVector b = new ArrayRealVector(n);
		b.set(-2 * gamma);
		
		// non negative least square
		QuadraticProgrammingProblem qpp = new QuadraticProgrammingProblem(a, b);
		
		return qpp.compute().toArray();
	}
	
	public static double[] TakeuchiKobayashi1D(PartialID[] ids, List<UnknownParameter> parameterList, Dvector dVector, double gamma, RealVector xInitial) {
		int n = dVector.getNTimeWindow();
		
		Matrix a = setA(ids, parameterList, dVector, gamma);
		
		RealVector d = new ArrayRealVector(n);
		d.set(gamma);
		
		// non negative least square
		NonNegativeLeastSquaresMethod nnls = new NonNegativeLeastSquaresMethod(a, d, xInitial);
		nnls.compute();
		
		return nnls.getAnsVector().toArray();
	}
	
	public static double[] CG(PartialID[] ids, List<UnknownParameter> parameterList, Dvector dVector, double gamma) {
		int n = dVector.getNTimeWindow();
		
		Matrix a = setA(ids, parameterList, dVector, gamma);
		
		RealVector d = new ArrayRealVector(n);
		d.set(gamma);
		
		// least square
		ConjugateGradientMethod cgMethod = new ConjugateGradientMethod(a.computeAtA(), a.transpose().operate(d));
		cgMethod.compute();
		RealMatrix m = cgMethod.getANS();
		double[] cgn = m.getColumnVector(n-1).toArray();
		
		System.out.println();
		for (double cgi : cgn)
			System.out.println(cgi);
		System.out.println();
		
//		for (int i = 0; i < m.getColumnDimension(); i++) {
//			for (int j = 0; j < m.getRowDimension(); j++)
//				System.out.printf("%.3f ", m.getColumn(i)[j]);
//			System.out.println();
//		}
		
		return cgn;
	}
	
	private static Matrix setAForSSL(PartialID[] ids, List<UnknownParameter> parameterList, Dvector dVector, double gamma) {
		int n = dVector.getNTimeWindow();
		int m = parameterList.size();
		
		if (parameterList.stream().filter(unknown -> unknown.getPartialType().isTimePartial()).count() > 0)
			throw new RuntimeException("parameterList cannot contains time partials for TakeuchiKobayashi weigthing scheme");
		
		Matrix a = new Matrix(n, n);
		
		ObservationEquation eq = new ObservationEquation(ids, parameterList, dVector, false, false, null, null, null, null);
		Matrix eqA = (Matrix) eq.getA();
		
		for (int i = 0; i < eqA.getRowDimension(); i++) {
			for (int k = 0; k < eqA.getColumnDimension(); k++)
				eqA.setEntry(i, k, eqA.getEntry(i, k) * parameterList.get(k).getWeighting()); // multiply by the weight (volume) of each voxel in case voxels have different volumes)
		}
		
		double maxEqA = Double.MIN_VALUE;
		for (int i = 0; i < eqA.getRowDimension(); i++) {
			for (int j = 0; j < eqA.getColumnDimension(); j++) {
				double tmp = eqA.getEntry(i, j);
				if (tmp > maxEqA)
					maxEqA = tmp;
			}
		}
		for (int i = 0; i < eqA.getRowDimension(); i++) {
			for (int j = 0; j < eqA.getColumnDimension(); j++)
				eqA.setEntry(i, j, eqA.getEntry(i, j) / maxEqA);
		}
		
		double[] Tbar = new double[n];
		double[][] s = new double[n][];
		for (int i = 0; i < n; i++) 
			s[i] = new double[m];
		
		for (int i = 0; i < n; i++) {
			int jstart = dVector.getStartPoints(i);
			int jend = 0;
			if (i < n-1)
				jend = dVector.getStartPoints(i+1);
			else
				jend = dVector.getNpts();
			for (int k = 0; k < m; k++) {
				for (int j = jstart; j < jend; j++) {
					double tmp = eqA.getEntry(j, k);
					s[i][k] += tmp * tmp;
				}
				s[i][k] = Math.sqrt(s[i][k]);
				Tbar[i] += s[i][k];
			}
			Tbar[i] /= m;
		}
		
		double[] dia = new double[n];
		for (int i = 0; i < n; i++) {
			for (int k = 0; k < m; k++) {
				double tmp = (s[i][k] - Tbar[i]);
				dia[i] += tmp * tmp;
			}
		}
		double median = getMedian(dia);
		System.out.println("Recommand to use the value " + String.format("%.3e", median) + " for the parameter gamma");
		
		//build a
//		int count = 0;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j <= i; j++) {
				double value = 0;
				for (int k = 0; k < m; k++)
					value += (s[i][k] - Tbar[i]) * (s[j][k] - Tbar[j]);
				if (j == i)
					a.setEntry(i, i, 2 * (value + gamma));
				else {
					a.setEntry(i, j, 2 * value);
					a.setEntry(j, i, 2 * value);
				}
				
//				if (count % n == 0)
//					System.out.println();
//				System.out.printf("%.3f ", value);
//				count++;
			}
		}
		
		return a;
	}
	
	private static Matrix setA(PartialID[] ids, List<UnknownParameter> parameterList, Dvector dVector, double gamma) {
		int n = dVector.getNTimeWindow();
		int m = parameterList.size();
		
		if (parameterList.stream().filter(unknown -> unknown.getPartialType().isTimePartial()).count() > 0)
			throw new RuntimeException("parameterList cannot contains time partials for TakeuchiKobayashi weigthing scheme");
		
		Matrix a = new Matrix(n, n);
		
		ObservationEquation eq = new ObservationEquation(ids, parameterList, dVector, false, false, null, null, null, null);
		Matrix eqA = (Matrix) eq.getA();
		
		double maxEqA = Double.MIN_VALUE;
		for (int i = 0; i < eqA.getRowDimension(); i++) {
			for (int j = 0; j < eqA.getColumnDimension(); j++) {
				double tmp = eqA.getEntry(i, j);
				if (tmp > maxEqA)
					maxEqA = tmp; 
			}
		}
		for (int i = 0; i < eqA.getRowDimension(); i++) {
			for (int j = 0; j < eqA.getColumnDimension(); j++)
				eqA.setEntry(i, j, eqA.getEntry(i, j) / maxEqA);
		}
		
		double[] Tbar = new double[n];
		double[][] s = new double[n][];
		for (int i = 0; i < n; i++) 
			s[i] = new double[m];
		
		for (int i = 0; i < n; i++) {
			int jstart = dVector.getStartPoints(i);
			int jend = 0;
			if (i < n-1)
				jend = dVector.getStartPoints(i+1);
			else
				jend = dVector.getNpts();
			for (int k = 0; k < m; k++) {
				for (int j = jstart; j < jend; j++) {
					double tmp = eqA.getEntry(j, k);
					s[i][k] += tmp * tmp;
				}
				s[i][k] = Math.sqrt(s[i][k]);
				Tbar[i] += s[i][k];
			}
			Tbar[i] /= m;
		}
		
		double[] dia = new double[n];
		for (int i = 0; i < n; i++) {
			for (int k = 0; k < m; k++) {
				double tmp = (s[i][k] - Tbar[i]);
				dia[i] += tmp * tmp;
			}
		}
		double median = getMedian(dia);
		System.out.println("Recommand to use the value " + String.format("%.3e", median) + " for the parameter gamma");
		
		//build a
//		int count = 0;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				double value = 0;
				for (int k = 0; k < m; k++)
					value += (s[i][k] - Tbar[i]) * (s[j][k] - Tbar[j]);
				if (j == i)
					value += gamma;
				a.setEntry(i, j, value);
//				if (count % n == 0)
//					System.out.println();
//				System.out.printf("%.3f ", value);
//				count++;
			}
		}
		
		return a;
	}
	
	private static double getMedian(double[] w) {
		List<Double> wlist = new ArrayList<>();
		for (double wi : w)
			wlist.add(wi);
		Collections.sort(wlist);
		int n = wlist.size();
		if (n == 0)
			throw new RuntimeException("Length of array should be greater than zero");
		if (n % 2 == 1)
			return wlist.get(n/2);
		else
			return (wlist.get(n/2 - 1) + wlist.get(n/2)) / 2;
	}
	
	public static double weightingAzimuthTZCA(BasicID obs) {
		double weight = 1.;
		Location loc = obs.getGlobalCMTID().getEvent().getCmtLocation();
		double azimuth = Math.toDegrees(loc.getAzimuth(obs.getStation().getPosition()));
		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		
		double groupFactor = 1.;
		double[][] histogramAzimuth = null;
		if (lat < 10.) {
			histogramAzimuth = new double[][] { {0, 1.60}, {5, 1.60}, {10, 1.00}, {15, 1.00}, {20, 1.00}, {25, 1.00}, {30, 1.00}, {35, 1.00}, {40, 1.60}, {45, 1.00}, {50, 1.00}, {55, 1.00}, {60, 1.00}, {65, 1.00}, {70, 1.00}, {75, 1.00}, {80, 1.00}, {85, 1.00}, {90, 1.00}, {95, 1.00}, {100, 1.00}, {105, 1.00}, {110, 1.00}, {115, 1.00}, {120, 1.00}, {125, 1.00}, {130, 1.00}, {135, 1.00}, {140, 1.00}, {145, 1.60}, {150, 1.60}, {155, 1.60}, {160, 1.60}, {165, 1.00}, {170, 1.60}, {175, 1.00}, {180, 1.00}, {185, 1.00}, {190, 1.00}, {195, 1.00}, {200, 1.00}, {205, 1.00}, {210, 1.00}, {215, 1.00}, {220, 1.00}, {225, 1.00}, {230, 1.00}, {235, 1.00}, {240, 1.00}, {245, 1.00}, {250, 1.00}, {255, 1.00}, {260, 1.00}, {265, 1.00}, {270, 1.00}, {275, 1.00}, {280, 1.00}, {285, 1.00}, {290, 1.00}, {295, 1.00}, {300, 1.00}, {305, 1.00}, {310, 1.00}, {315, 1.60}, {320, 1.00}, {325, 1.60}, {330, 1.00}, {335, 1.60}, {340, 1.60}, {345, 1.06}, {350, 1.00}, {355, 1.60} };
			groupFactor = 1.2;
		}
		else if (lat >= 10. && lon < -80.) {
			histogramAzimuth = new double[][] { {0, 1.14}, {5, 1.22}, {10, 1.25}, {15, 1.17}, {20, 1.25}, {25, 1.07}, {30, 1.28}, {35, 1.60}, {40, 1.60}, {45, 1.00}, {50, 1.00}, {55, 1.00}, {60, 1.00}, {65, 1.00}, {70, 1.60}, {75, 1.60}, {80, 1.60}, {85, 1.00}, {90, 1.60}, {95, 1.00}, {100, 1.00}, {105, 1.00}, {110, 1.00}, {115, 1.00}, {120, 1.00}, {125, 1.00}, {130, 1.00}, {135, 1.60}, {140, 1.00}, {145, 1.60}, {150, 1.60}, {155, 1.60}, {160, 1.00}, {165, 1.00}, {170, 1.00}, {175, 1.00}, {180, 1.00}, {185, 1.00}, {190, 1.00}, {195, 1.00}, {200, 1.00}, {205, 1.00}, {210, 1.00}, {215, 1.00}, {220, 1.00}, {225, 1.00}, {230, 1.00}, {235, 1.00}, {240, 1.00}, {245, 1.00}, {250, 1.00}, {255, 1.00}, {260, 1.00}, {265, 1.00}, {270, 1.00}, {275, 1.00}, {280, 1.00}, {285, 1.00}, {290, 1.00}, {295, 1.00}, {300, 1.00}, {305, 1.00}, {310, 1.14}, {315, 1.00}, {320, 1.37}, {325, 1.18}, {330, 1.35}, {335, 1.35}, {340, 1.39}, {345, 1.21}, {350, 1.07}, {355, 1.05} };
			groupFactor = 1.;
		}
		else if (lat >= 10. && lon >= -80.) {
			histogramAzimuth = new double[][] { {0, 1.00}, {5, 1.00}, {10, 1.00}, {15, 1.00}, {20, 1.00}, {25, 1.00}, {30, 1.00}, {35, 1.00}, {40, 1.00}, {45, 1.00}, {50, 1.00}, {55, 1.00}, {60, 1.00}, {65, 1.00}, {70, 1.00}, {75, 1.00}, {80, 1.00}, {85, 1.00}, {90, 1.00}, {95, 1.00}, {100, 1.00}, {105, 1.00}, {110, 1.00}, {115, 1.00}, {120, 1.00}, {125, 1.00}, {130, 1.00}, {135, 1.00}, {140, 1.00}, {145, 1.00}, {150, 1.00}, {155, 1.00}, {160, 1.00}, {165, 1.00}, {170, 1.00}, {175, 1.00}, {180, 1.00}, {185, 1.60}, {190, 1.00}, {195, 1.00}, {200, 1.00}, {205, 1.00}, {210, 1.00}, {215, 1.00}, {220, 1.00}, {225, 1.00}, {230, 1.00}, {235, 1.00}, {240, 1.00}, {245, 1.00}, {250, 1.00}, {255, 1.00}, {260, 1.00}, {265, 1.00}, {270, 1.00}, {275, 1.00}, {280, 1.00}, {285, 1.00}, {290, 1.00}, {295, 1.54}, {300, 1.02}, {305, 1.00}, {310, 1.21}, {315, 1.60}, {320, 1.39}, {325, 1.29}, {330, 1.39}, {335, 1.39}, {340, 1.60}, {345, 1.00}, {350, 1.00}, {355, 1.00} };
			groupFactor = 1.4;
		}
		
		for (double[] p : histogramAzimuth) {
			if (azimuth >= p[0] && azimuth < p[0] + 5.)
				weight = p[1] * groupFactor;
		}
		
		return weight;
	}
	
	public static double weightingDistanceTZCA(BasicID obs) {
		double weight = 1.;
		Location loc = obs.getGlobalCMTID().getEvent().getCmtLocation();
		double distance = Math.toDegrees(loc.getEpicentralDistance(obs.getStation().getPosition()));
		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		
		double groupFactor = 1.;
		double[][] histogramDistance = null;
		if (lat < 10.) {
			histogramDistance = new double[][] { {0, 1.00}, {5, 1.00}, {10, 1.60}, {15, 1.60}, {20, 1.60}, {25, 1.60}, {30, 1.00}, {35, 1.00}, {40, 1.00} };
			groupFactor = 1.;
		}
		else if (lat >= 10. && lon < -80.) {
			histogramDistance = new double[][] { {0, 1.00}, {5, 1.00}, {10, 1.60}, {15, 1.60}, {20, 1.15}, {25, 1.00}, {30, 1.04}, {35, 1.00}, {40, 1.00} };
			groupFactor = 1.;
		}
		else if (lat >= 10. && lon >= -80.) {
			histogramDistance = new double[][] { {0, 1.00}, {5, 1.00}, {10, 1.00}, {15, 1.60}, {20, 1.60}, {25, 1.32}, {30, 1.00}, {35, 1.00}, {40, 1.00} };
			groupFactor = 1.;
		}
		
		for (double[] p : histogramDistance) {
			if (distance >= p[0] && distance < p[0] + 5.)
				weight = p[1] * groupFactor;
		}
		
		return weight;
	}
	
	public static double weightingStationTZCA(BasicID obs) {
		double weight = 1.;
		Station station = obs.getStation();
		
		if (station.getPosition().getLatitude() < 25.)
			weight = 2.;
		if (station.getName().equals("BBSR"))
			weight = 2.;
		
		return weight;
	}
	
	public static double weightEventTZCA(BasicID obs) {
		double weight = 1.;
		Location location = obs.getGlobalCMTID().getEvent().getCmtLocation();
		
		if (location.getLongitude() > -89)
			weight = 1.5;
		if (obs.getGlobalCMTID().equals(new GlobalCMTID("200503171337A")))
			weight = 1.5;
		
		return weight;
	}
}
