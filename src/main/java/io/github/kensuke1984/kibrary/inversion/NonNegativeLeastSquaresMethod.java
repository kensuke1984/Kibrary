package io.github.kensuke1984.kibrary.inversion;

import java.util.HashSet;
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
		
		
		while ( !KuhnTuckerConvergenceTest(w, fullSet) ) {
			w = atd.subtract(ata.operate(x));
			
			t = 0;
			double valueAtt = Double.MIN_VALUE;
			for (Integer i : L) {
				if (w.getEntry(i.intValue()) > valueAtt) {
					t = i;
					valueAtt = w.getEntry(i.intValue());
				}
			}
			
			F.add(t);
			L.remove(t);
			
			RealVector xPrime = new ArrayRealVector(x);
			for (Integer i :  fullSet) {
				if (!L.contains(i))
					xPrime.setEntry(i.intValue(), 0.);
			}
			RealVector bPrime = d.subtract(a.operate(xPrime));
			
			
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