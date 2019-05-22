package io.github.kensuke1984.kibrary.inversion;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

public class RadialSecondOrderDifferentialOperator {

	private Matrix D2;
	
	List<Double> coeffs;
	
	List<UnknownParameter> parameters;
	
	List<PartialType> types;
	
	private int[][] indexNonZeroElements;
	private double[][] valueNonZeroElements;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public RadialSecondOrderDifferentialOperator(List<UnknownParameter> parameters, List<PartialType> types, List<Double> coeffs) {
		int m = parameters.size();
		indexNonZeroElements = new int[m][];
		valueNonZeroElements = new double[m][];
		this.types = types;
		this.parameters = parameters;
		this.coeffs = coeffs;
		D2 = new Matrix(m, m);
		compute();
	}
	
	private void compute() {
		double dr = parameters.get(1).getLocation().getR() - parameters.get(0).getLocation().getR();
//		double dr2 = 1. / (dr * dr);
		double dr2 = 1.;
		
		int c = 0;
		for (int itype = 0; itype < types.size(); itype++) {
			double coeff = coeffs.get(itype);
			PartialType type = types.get(itype);
			List<UnknownParameter> thisParameters = parameters.stream().filter(p -> p.getPartialType().equals(type)).collect(Collectors.toList());
			int m = thisParameters.size();

			indexNonZeroElements[c] = new int[] {c, c+1, c+2};
			valueNonZeroElements[c] = new double[] {dr2*coeff, -2*dr2*coeff, dr2*coeff};
//			D2.setEntry(c, c, dr2 * coeff);
//			D2.setEntry(c, c+1, -2*dr2 * coeff);
//			D2.setEntry(c, c+2, dr2 * coeff);
			
			D2.setEntry(c, c, -2*dr2 * coeff);
			D2.setEntry(c, c+1, dr2 * coeff);
			
			for (int i = 1; i < m-1; i++) {
				indexNonZeroElements[i + c] = new int[] {i+c-1, i+c, i+c+1};
				valueNonZeroElements[i + c] = new double[] {dr2*coeff, -2*dr2*coeff, dr2*coeff};
				D2.setEntry(c+i, i+c-1, dr2 * coeff);
				D2.setEntry(c+i, i+c, -2*dr2 * coeff);
				D2.setEntry(c+i, i+c+1, dr2 * coeff);
			}
			indexNonZeroElements[m-1 + c] = new int[] {m+c-3, m+c-2, m+c-1};
			valueNonZeroElements[m-1 + c] = new double[] {dr2*coeff, -2*dr2*coeff, dr2*coeff};
//			D2.setEntry(c+m-1, m+c-3, dr2 * coeff);
//			D2.setEntry(c+m-1, m+c-2, -2*dr2 * coeff);
//			D2.setEntry(c+m-1, m+c-1, dr2 * coeff);
			
			D2.setEntry(c+m-1, m+c-2, dr2 * coeff);
			D2.setEntry(c+m-1, m+c-1, -2*dr2 * coeff);
			
			c += m;
		}
	}
	
	public RealMatrix getD2TD2() {
		return D2.computeAtA();
	}
	
	public RealMatrix fastComputeD2TD2() {
		int m = parameters.size();
		RealMatrix DTD = new Array2DRowRealMatrix(m, m);
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < indexNonZeroElements[i].length; j++) {
				
			}
		}
		
		return DTD;
	}
}
