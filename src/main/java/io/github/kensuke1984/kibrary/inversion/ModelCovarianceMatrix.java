package io.github.kensuke1984.kibrary.inversion;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

public class ModelCovarianceMatrix {
	private RealMatrix cm;
	
	List<UnknownParameter> parameters;
	
	private int[][] indexNonZeroElements;
	private double[][] valueNonZeroElements;
	
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
		this.threshold = .005;
		this.indexNonZeroElements = new int[parameters.size()][];
		this.valueNonZeroElements = new double[parameters.size()][];
		preWeight = new double[parameters.size()];
		for (int i = 0; i < preWeight.length; i++)
			preWeight[i] = 1.;
		
		computeMatrix();
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
		try {
			preWeight = readSensitivityFileAndComputeWeight(sensitivityFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		computeMatrix();
	}
	
	private void computeMatrix() {
		int n = parameters.size();
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
				if (v > 0)
					cmV = Math.exp(-(dr/v) * (dr/v));
				
				if (i == j) {
					cm.setEntry(i, i, preWeight[i] * preWeight[j]);
					tmpI.add(i);
					tmpV.add(1.);
				}
				else {
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
	
	public RealMatrix getCm() {
		return cm;
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
