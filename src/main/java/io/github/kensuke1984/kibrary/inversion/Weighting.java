package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;

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

public class Weighting {
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
		
		ObservationEquation eq = new ObservationEquation(ids, parameterList, dVector, false, false, null, null);
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
		
		ObservationEquation eq = new ObservationEquation(ids, parameterList, dVector, false, false, null, null);
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
	
}
