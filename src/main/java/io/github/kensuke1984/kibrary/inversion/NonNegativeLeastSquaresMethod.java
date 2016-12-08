package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.math.Matrix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * @author Anselme
 * Implementation of the non negative least square algorithm of Stark and Parker (1993)
 */
public class NonNegativeLeastSquaresMethod extends InverseProblem {
	
	private final double lambda;
	
	
	/**
	 * F is the set of free (unbounded) indices
	 * L is the set of lower-bounded indices 
	 */
	private Set<Integer> F, L;
	
	private RealVector d;
	
	private RealMatrix a;
	
	public NonNegativeLeastSquaresMethod(RealMatrix ata, RealVector atd, RealVector d, RealMatrix a, double lambda) {
		this.d = d;
		this.a = a;
		this.ata = ata;
		this.atd = atd;
		this.lambda = lambda;
		F = new HashSet<>();
		L = new HashSet<>();
	}
	
	@Override
	public InverseMethodEnum getEnum() {
		return InverseMethodEnum.NON_NEGATIVE_LEAST_SQUARES_METHOD;
	}
	
	@Override
	public void compute() {
		int n = atd.getDimension();
		RealVector w = new ArrayRealVector(n);
		Set<Integer> fullSet = IntStream.range(0, n).boxed().collect(Collectors.toSet());
		Integer t = 0;
		
		// initialize
		RealVector x = new ArrayRealVector(n);
		x.set(0);
		L = IntStream.range(0, n).boxed().collect(Collectors.toSet());
		
		// step 2
		w = atd.subtract(ata.operate(x));
		while ( !KuhnTuckerConvergenceTest(w, fullSet) ) {    // step 3
			
			//step 4
			t = 0;
			double valueAtt = Double.MIN_VALUE;
			for (Integer i : L) {
				if (w.getEntry(i.intValue()) > valueAtt) {
					t = i;
					valueAtt = w.getEntry(i.intValue());
				}
			}
			
			// step 5
			F.add(t);
			L.remove(t);
			
			// step 6
			List<Integer> jPrimeToj = new ArrayList<>();
			for (Integer j :  fullSet) {
				if (F.contains(j)) {
					jPrimeToj.add(j);
				}
			}
			
			// b' (bPrime)
			RealVector xPrime = new ArrayRealVector(x);
			for (Integer jPrime :  jPrimeToj)
				xPrime.setEntry(jPrime.intValue(), 0.);
			RealVector bPrime = d.subtract(a.operate(xPrime));
			
			// a' (aPrime)
			Matrix aPrime = new Matrix(a.getRowDimension(), jPrimeToj.size());
			for (int i = 0; i < jPrimeToj.size(); i++)
				aPrime.setColumnVector(i, a.getColumnVector(jPrimeToj.get(i).intValue()));
			
			// solve z = arg min ||a' z - b'||_2
			ConjugateGradientMethod cgMethod = new ConjugateGradientMethod(aPrime.computeAtA(), aPrime.transpose().operate(bPrime));
			cgMethod.compute();
			RealVector z = cgMethod.getAns(jPrimeToj.size() - 1);
			
			// step 7
			boolean goToStep2 = true;
			for (int i = 0; i < z.getDimension(); i++) {
				if (z.getEntry(i) <= 0) {
					goToStep2 = false;
					break;
				}
			}
			if (goToStep2) {
				// step 2
				w = atd.subtract(ata.operate(x));
				continue;
			}
			else {
				
			}
			
		}
	}
	
	private boolean KuhnTuckerConvergenceTest(RealVector w, Set<Integer> fullSet) {
		boolean conditionOnW = true;
		for (Integer i : L)
			if (w.getEntry(i.intValue()) > 0) {
				conditionOnW = false;
				break;
			}
		return conditionOnW || F.equals(fullSet);
	}
	
	@Override
	public RealMatrix getBaseVectors() {
		throw new RuntimeException("No base vectors.");
	}
	
	@Override
	public RealMatrix computeCovariance(double sigmaD, int j) {
		// TODO
		return null;
	}
}