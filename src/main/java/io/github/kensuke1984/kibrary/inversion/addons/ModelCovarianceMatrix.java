package io.github.kensuke1984.kibrary.inversion.addons;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;

import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

public class ModelCovarianceMatrix {
	
	public static void main(String[] args) throws IOException {
//		List<UnknownParameter> parameters = UnknownParameterFile.read(Paths.get(args[0]));
//		
//		double v = Double.parseDouble(args[1]);
//		double h = Double.parseDouble(args[2]);
//		
//		System.out.println(v + " " + h);
//		
//		ModelCovarianceMatrix cm = new ModelCovarianceMatrix(parameters, v, h);
//		
////		EigenDecomposition eigen = new EigenDecomposition(cm.getCm());
////		double[] eigenValues = eigen.getRealEigenvalues();
//		
////		for (double y : eigenValues)
////			System.out.println(y);
//		
//		RealMatrix l = cm.getL();
//		for (int i = 0; i < l.getRowDimension(); i++) {
//			for (int j = 0; j < l.getColumnDimension(); j++) {
//				System.out.println(i + " " + j + " " + l.getEntry(i, j));
//			}
//		}
		
		//test
		List<UnknownParameter> params = new ArrayList<>();
		UnknownParameter p1 = new Physical3DParameter(PartialType.MU, new Location(0, 0, 6371),  1.);
		UnknownParameter p2 = new Physical3DParameter(PartialType.MU, new Location(2, 0, 6371),  1.);
		UnknownParameter p3 = new Physical3DParameter(PartialType.MU, new Location(4, 0, 6371),  1.);
		params.add(p1);
		params.add(p2);
		params.add(p3);
		
		UnknownParameter l1 = new Physical3DParameter(PartialType.LAMBDA, new Location(0, 0, 6371),  1.);
		UnknownParameter l2 = new Physical3DParameter(PartialType.LAMBDA, new Location(2, 0, 6371),  1.);
		UnknownParameter l3 = new Physical3DParameter(PartialType.LAMBDA, new Location(4, 0, 6371),  1.);
		params.add(l1);
		params.add(l2);
		params.add(l3);
		
		ModelCovarianceMatrix c = new ModelCovarianceMatrix(params, 0., 1.);
		
		RealMatrix tmpC = c.getCm();
		System.out.println("Cm =");
		for (int i = 0; i < params.size(); i++) {
			for (int j = 0; j < params.size(); j++) {
				System.out.printf("%.5f ", tmpC.getEntry(i, j));
			}
			System.out.println();
		}
		
		RealMatrix l = c.getL();
		System.out.println("\nL =");
		for (int i = 0; i < params.size(); i++) {
			for (int j = 0; j < params.size(); j++) {
				System.out.printf("%.5f ", l.getEntry(i, j));
			}
			System.out.println();
		}
		
//		RealMatrix lt = c.getLt();
//		System.out.println("\nLt =");
//		for (int i = 0; i < params.size(); i++) {
//			for (int j = 0; j < params.size(); j++) {
//				System.out.printf("%.5f ", lt.getEntry(i, j));
//			}
//			System.out.println();
//		}
		
		RealMatrix llt = l.multiply(c.getLt());
		System.out.println("\nLtL =");
		for (int i = 0; i < params.size(); i++) {
			for (int j = 0; j < params.size(); j++) {
				System.out.printf("%.5f ", llt.getEntry(i, j));
			}
			System.out.println();
		}
		
		RealMatrix m = new Array2DRowRealMatrix(new double[][] {{1, 0, 0, 0, 0, 0}, {0, 2, 0, 0, 0, 0}, {0, 0, 1, 0, 0, 0}, {0, 0, 0, 2, 0, 0}, {0, 0, 0, 0, 1, 0}, {0, 0, 0, 0, 0, 2}});
		
		RealMatrix tmpML = c.rightMultiplyByL(m);
		System.out.println("\nML (fast) =");
		for (int i = 0; i < params.size(); i++) {
			for (int j = 0; j < params.size(); j++) {
				System.out.printf("%.5f ", tmpML.getEntry(i, j));
			}
			System.out.println();
		}
		
		tmpML = m.multiply(c.getL());
		System.out.println("\nML =");
		for (int i = 0; i < params.size(); i++) {
			for (int j = 0; j < params.size(); j++) {
				System.out.printf("%.5f ", tmpML.getEntry(i, j));
			}
			System.out.println();
		}
		
		RealMatrix tmpLM = c.leftMultiplyByL(m);
		System.out.println("\nLM (fast) =");
		for (int i = 0; i < params.size(); i++) {
			for (int j = 0; j < params.size(); j++) {
				System.out.printf("%.5f ", tmpLM.getEntry(i, j));
			}
			System.out.println();
		}
		
		tmpLM = c.getL().multiply(m);
		System.out.println("\nLM =");
		for (int i = 0; i < params.size(); i++) {
			for (int j = 0; j < params.size(); j++) {
				System.out.printf("%.5f ", tmpLM.getEntry(i, j));
			}
			System.out.println();
		}
		
		RealMatrix tmpLTM = c.leftMultiplyByLT(m);
		System.out.println("\nLTM (fast) =");
		for (int i = 0; i < params.size(); i++) {
			for (int j = 0; j < params.size(); j++) {
				System.out.printf("%.5f ", tmpLTM.getEntry(i, j));
			}
			System.out.println();
		}
		
		tmpLTM = c.getLt().multiply(m);
		System.out.println("\nLTM =");
		for (int i = 0; i < params.size(); i++) {
			for (int j = 0; j < params.size(); j++) {
				System.out.printf("%.5f ", tmpLTM.getEntry(i, j));
			}
			System.out.println();
		}
	}
	
	private RealMatrix cm;
	
	private RealMatrix l;
	
	private RealMatrix lt;
	
	List<UnknownParameter> parameters;
	
	private int[][] indexNonZeroElements;
	private double[][] valueNonZeroElements;
	
	private int[][] ltindexNonZeroElements;
	private double[][] ltvalueNonZeroElements;
	
	private int[][] lindexNonZeroElements;
	private double[][] lvalueNonZeroElements;
	
	private double[] preWeight;
	
	/**
	 * horizontal correlation length 
	 */
	private double h;
	
	private double[] radii;
	
	private double[] layerThickness;
	
	
	/**
	 * vertical correlation length 
	 */
	private double v;
	
	private double threshold;
	
	public ModelCovarianceMatrix(List<UnknownParameter> parameters, double v, double[] radii, double[] layerThickness, double h) {
		this.parameters = parameters;
		this.radii = radii;
		this.layerThickness = layerThickness;
		this.h = h;
		this.v = v;
		computeMatrix();
	}
	
	public ModelCovarianceMatrix(List<UnknownParameter> parameters, double v, double h) {
		this.parameters = parameters;
		this.radii = null;
		this.layerThickness = null;
		this.h = h;
		this.v = v;
		this.threshold = 0.; //0.005
		this.indexNonZeroElements = new int[parameters.size()][];
		this.valueNonZeroElements = new double[parameters.size()][];
		this.ltindexNonZeroElements = new int[parameters.size()][];
		this.ltvalueNonZeroElements = new double[parameters.size()][];
		this.lindexNonZeroElements = new int[parameters.size()][];
		this.lvalueNonZeroElements = new double[parameters.size()][];
		preWeight = new double[parameters.size()];
		for (int i = 0; i < preWeight.length; i++)
			preWeight[i] = 1.;
		
		computeMatrix();
		computeCholeskyDecomposition();
	}
	
	public ModelCovarianceMatrix(List<UnknownParameter> parameters, double v, double h, Path sensitivityFile) {
		this.parameters = parameters;
		this.radii = null;
		this.layerThickness = null;
		this.h = h;
		this.v = v;
		this.threshold = .005;
		this.indexNonZeroElements = new int[parameters.size()][];
		this.valueNonZeroElements = new double[parameters.size()][];
		this.ltindexNonZeroElements = new int[parameters.size()][];
		this.ltvalueNonZeroElements = new double[parameters.size()][];
		this.lindexNonZeroElements = new int[parameters.size()][];
		this.lvalueNonZeroElements = new double[parameters.size()][];
		try {
			preWeight = readSensitivityFileAndComputeWeight(sensitivityFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		computeMatrix();
		computeCholeskyDecomposition();
	}
	
	public ModelCovarianceMatrix(List<UnknownParameter> parameters, double v, double h, double[] normalization, boolean applyRadialWeight) {
		this.parameters = parameters;
		this.radii = null;
		this.layerThickness = null;
		this.h = h;
		this.v = v;
		this.threshold = 0.;//.005;
		this.indexNonZeroElements = new int[parameters.size()][];
		this.valueNonZeroElements = new double[parameters.size()][];
		this.ltindexNonZeroElements = new int[parameters.size()][];
		this.ltvalueNonZeroElements = new double[parameters.size()][];
		this.lindexNonZeroElements = new int[parameters.size()][];
		this.lvalueNonZeroElements = new double[parameters.size()][];
		preWeight = new double[parameters.size()];
		for (int i = 0; i < preWeight.length; i++)
			preWeight[i] = 1.;
		//MTZ
//		if (applyRadialWeight) {
//			for (int i = 0; i < preWeight.length; i++) {
//				UnknownParameter p = parameters.get(i);
//				double depth = 6371 - p.getLocation().getR();
//				if (depth >= 600)
//					preWeight[i] *= 1.2248;//1.2248 (for 12.5 s); //sqrt(1.4) // 1.4 is the calculated value but may be too strong. Trying sqrt(1.5) ~ 1.2248
//			}
//		}
		//D"
//		if (applyRadialWeight) {
//			for (int i = 0; i < preWeight.length; i++) {
//				UnknownParameter p = parameters.get(i);
//				double depth = 6371 - p.getLocation().getR();
//				if (depth > 2791.)
//					preWeight[i] *= 1.2248;//1.2248 (for 12.5 s); //sqrt(1.4) // 1.4 is the calculated value but may be too strong. Trying sqrt(1.5) ~ 1.2248
//			}
//		}
		//normalize weights
		double mean = new ArrayRealVector(preWeight).getL1Norm() / preWeight.length;
		System.out.println("mean = " + mean);
		for (int i = 0; i < preWeight.length; i++) {
//			preWeight[i] *= Math.sqrt(normalization[i]) / mean;
			preWeight[i] *= Math.sqrt(normalization[i]) / mean / (Math.sqrt(Math.PI) * h);
		}
		
		computeMatrix();
		computeCholeskyDecomposition();
	}
	
	public ModelCovarianceMatrix(List<UnknownParameter> parameters, double v, double h, double normalization, boolean applyRadialWeight) {
		this(parameters, v, h, IntStream.range(0, parameters.size()).mapToDouble(i -> normalization).toArray(), applyRadialWeight);
	}
	
	
	private void computeMatrix() {
		int n = parameters.size();
		System.out.println("Computing model covariance matrix with " + n + " parameters");
		cm = new Array2DRowRealMatrix(n, n);
		
		List<List<Integer>> tmpIndexes = new ArrayList<>();
		List<List<Double>> tmpValues = new ArrayList<>();
		
		for (int i = 0; i < n; i++) {
			List<Integer> tmpI = new ArrayList<>();
			List<Double> tmpV = new ArrayList<>();
			for (int j = 0; j < n ; j++) {
				Location loci = parameters.get(i).getLocation();
				Location locj = parameters.get(j).getLocation();
				double delta = Math.toDegrees(loci.getEpicentralDistance(locj));
				double dr = Math.abs(loci.getR() - locj.getR());
				double cmH = 0;
				double cmV = 0;
				if (h > 0)
					cmH = Math.exp(-(delta/h) * (delta/h));
				else if (h == 0) {
					if (delta == 0)
						cmH = 1.;
					else
						cmH = 0.;
				}
				if (v > 0)
					cmV = Math.exp(-(dr/v) * (dr/v));
				else if (v == 0) {
					if (dr == 0)
						cmV = 1.;
					else
						cmV = 0.;
				}
				
				if (!parameters.get(i).getPartialType().equals(parameters.get(j).getPartialType())) {
					cm.setEntry(i, j, 0.);
					cm.setEntry(j, i, 0.);
				}
				else {
					if (i == j) {
						cm.setEntry(i, i, preWeight[i] * preWeight[j]);
						tmpI.add(i);
						tmpV.add(preWeight[i] * preWeight[j]);
					}
					else {
	//					double cmij = cmH * cmV;
						double cmij = preWeight[i] * preWeight[j] * cmH * cmV;
						if (cmij < threshold)
							cmij = 0;
						else {
							tmpI.add(j);
							tmpV.add(cmij);
						}
						cm.setEntry(i, j, cmij);
						cm.setEntry(j, i, cmij);
					}
				}
			}
			tmpIndexes.add(tmpI);
			tmpValues.add(tmpV);
		}
		
		for (int i = 0; i < n; i++) {
			List<Integer> tmpI = tmpIndexes.get(i);
			List<Double> tmpV = tmpValues.get(i);
			indexNonZeroElements[i] = new int[tmpI.size()];
			valueNonZeroElements[i] = new double[tmpV.size()];
//			System.out.println(tmpI.size());
			for (int j = 0; j < tmpI.size(); j++) {
				indexNonZeroElements[i][j] = tmpI.get(j);
				valueNonZeroElements[i][j] = tmpV.get(j);
			}
		}
	}
	
	public void computeCholeskyDecomposition() {
		System.out.println("Computing Cholesky decomposition");
		CholeskyDecomposition cholesky = new CholeskyDecomposition(cm);
		l = cholesky.getL();
		lt = cholesky.getLT();
		int n = parameters.size();
		
		List<List<Integer>> tmpIndexes = new ArrayList<>();
		List<List<Double>> tmpValues = new ArrayList<>();
		
		List<List<Integer>> tmpIndexesL = new ArrayList<>();
		List<List<Double>> tmpValuesL = new ArrayList<>();
		
		for (int i = 0; i < n; i++) {
			List<Integer> tmpI = new ArrayList<>();
			List<Double> tmpV = new ArrayList<>();
			List<Integer> tmpIL = new ArrayList<>();
			List<Double> tmpVL = new ArrayList<>();
			for (int j = 0; j < n; j++) {
				double ltij = lt.getEntry(i, j);
				if (ltij > 0.) {
					tmpI.add(j);
					tmpV.add(ltij);
				}
				double lij = l.getEntry(i, j);
				if (lij > 0) {
					tmpIL.add(j);
					tmpVL.add(lij);
				}
			}
			
			tmpIndexes.add(tmpI);
			tmpValues.add(tmpV);
			
			tmpIndexesL.add(tmpIL);
			tmpValuesL.add(tmpVL);
		}
		
		for (int i = 0; i < n; i++) {
			List<Integer> tmpI = tmpIndexes.get(i);
			List<Double> tmpV = tmpValues.get(i);
			ltindexNonZeroElements[i] = new int[tmpI.size()];
			ltvalueNonZeroElements[i] = new double[tmpV.size()];
			
			List<Integer> tmpIL = tmpIndexesL.get(i);
			List<Double> tmpVL = tmpValuesL.get(i);
			lindexNonZeroElements[i] = new int[tmpIL.size()];
			lvalueNonZeroElements[i] = new double[tmpVL.size()];
			
//			System.out.println(tmpI.size());
			for (int j = 0; j < tmpI.size(); j++) {
				ltindexNonZeroElements[i][j] = tmpI.get(j);
				ltvalueNonZeroElements[i][j] = tmpV.get(j);
			}
			
			for (int j = 0; j < tmpIL.size(); j++) {
				lindexNonZeroElements[i][j] = tmpIL.get(j);
				lvalueNonZeroElements[i][j] = tmpVL.get(j);
			}
		}
	}
	
	public RealMatrix rightMultiplyByL(RealMatrix m) {
		if (m.getColumnDimension() != l.getRowDimension())
			throw new RuntimeException("M column dimension and L row dimension mismatch " + m.getColumnDimension() + " " + l.getRowDimension());
		int n1 = m.getRowDimension();
//		int n2 = m.getColumnDimension();
		int n2 = l.getColumnDimension();
		RealMatrix res = new Array2DRowRealMatrix(n1, n2);
		for (int i = 0; i < n1; i++) {
			for (int j = 0; j < n2; j++) {
				int[] tmpI = ltindexNonZeroElements[j];
				double[] tmpV = ltvalueNonZeroElements[j];
				double resij = 0.;
				for (int k = 0; k < tmpI.length; k++) {
					resij += tmpV[k] * m.getEntry(i, tmpI[k]);
				}
				res.setEntry(i, j, resij);
			}
		}
		return res;
	}
	
	public RealMatrix leftMultiplyByL(RealMatrix m) {
		if (m.getRowDimension() != l.getColumnDimension())
			throw new RuntimeException("M row dimension and L column dimension mismatch " + m.getRowDimension() + " " + l.getColumnDimension());
		int n1 = l.getRowDimension();
		int n2 = m.getColumnDimension();
		RealMatrix res = new Array2DRowRealMatrix(n1, n2);
		for (int i = 0; i < n1; i++) {
			int[] tmpI = lindexNonZeroElements[i];
			double[] tmpV = lvalueNonZeroElements[i];
			for (int j = 0; j < n2; j++) {
				double resij = 0.;
				for (int k = 0; k < tmpI.length; k++) {
					resij += tmpV[k] * m.getEntry(tmpI[k], j);
				}
				res.setEntry(i, j, resij);
			}
		}
		return res;
	}
	
	public RealMatrix leftMultiplyByLT(RealMatrix m) {
		if (m.getRowDimension() != l.getColumnDimension())
			throw new RuntimeException("M row dimension and L column dimension mismatch " + m.getRowDimension() + " " + l.getColumnDimension());
		int n1 = l.getRowDimension();
		int n2 = m.getColumnDimension();
		RealMatrix res = new Array2DRowRealMatrix(n1, n2);
		for (int i = 0; i < n1; i++) {
			int[] tmpI = ltindexNonZeroElements[i];
			double[] tmpV = ltvalueNonZeroElements[i];
			for (int j = 0; j < n2; j++) {
				double resij = 0.;
				for (int k = 0; k < tmpI.length; k++) {
					resij += tmpV[k] * m.getEntry(tmpI[k], j);
				}
				res.setEntry(i, j, resij);
			}
		}
		return res;
	}
	
	public RealMatrix leftMultiply(RealMatrix m) {
		int n = parameters.size();
		RealMatrix res = new Array2DRowRealMatrix(n, n);
		for (int i = 0; i < n; i++) {
			int[] tmpI = indexNonZeroElements[i];
			double[] tmpV = valueNonZeroElements[i];
			for (int j = 0; j < n; j++) {
				double resij = 0.;
				for (int k = 0; k < tmpI.length; k++) {
					resij += tmpV[k] * m.getEntry(tmpI[k], j);
				}
				res.setEntry(i, j, resij);
			}
		}
		return res;
	}
	
	public RealVector operate(RealVector v) {
		int n = parameters.size();
		RealVector res = new ArrayRealVector(n);
		for (int i = 0; i < n; i++) {
			int[] tmpI = indexNonZeroElements[i];
			double[] tmpV = valueNonZeroElements[i];
			double resi = 0.;
			for (int k = 0; k < tmpI.length; k++) {
				resi += tmpV[k] * v.getEntry(tmpI[k]);
			}
			res.setEntry(i, resi);
		}
		return res;
	}
	
	private int countLayer(double r1, double r2) {
		int i1 = -1;
		int i2 = -1;
		for (int i = 0; i < radii.length; i++) {
			if (Utilities.equalWithinEpsilon(r1, radii[i], eps))
				i1 = i;
			if (Utilities.equalWithinEpsilon(r1, radii[i], eps))
				i2 = i;
		}
		return Math.abs(i2 - i1);
	}
	
	public void mapMultiply(double d) {
		for (int i = 0; i < valueNonZeroElements.length; i++) {
			for (int j = 0; j < valueNonZeroElements[i].length; j++) {
				valueNonZeroElements[i][j] *= d;
			}
		}
	}
	
	public RealMatrix getCm() {
		return cm;
	}
	
	public RealMatrix getL() {
		return l;
	}
	
	public RealMatrix getLt() {
		return lt;
	}
	
	public static double[] readSensitivityFileAndComputeWeight(Path inpath) throws IOException {
		List<Double> weightList = new ArrayList<>();
		
		BufferedReader br = Files.newBufferedReader(inpath);
		String line;
		while((line = br.readLine()) != null) {
			String[] ss = line.trim().split("\\s+");
			double lat = Double.parseDouble(ss[1]);
			double lon = Double.parseDouble(ss[2]);
			double r = Double.parseDouble(ss[3]);
			double sensitivity = Double.parseDouble(ss[4]);
			PartialType type = PartialType.valueOf(ss[0]);
			
			double weight = 1. / sensitivity;
			if (weight > 3)
				weight = 3;
			else if (weight < 1./3)
				weight = 1./3;
			weight = Math.sqrt(weight);
			weightList.add(weight);
		}
		br.close();
		
		double[] weights = new double[weightList.size()];
		for (int i = 0; i < weights.length; i++)
			weights[i] = weightList.get(i);
		
		return weights;
	}
	
	private final double eps = 1e-6;
}
