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
	
	public static void main(String[] args) {
		// x^2 + 2x + 3
		Matrix a1 = new Matrix(4, 3);
		a1.setColumn(0, new double[] {1, 0, 4, 4});
		a1.setColumn(1, new double[] {1, 0, 2, -2});
		a1.setColumn(2, new double[] {1, 1, 1, 1});
		RealVector d1 = new ArrayRealVector(new double[] {6.2, 2.9, 10.5, 2.8});
		
		// x^2 + 2x - 0.1
		Matrix a2 = new Matrix(4, 3);
		a2.setColumn(0, new double[] {1, 0, 4, 4});
		a2.setColumn(1, new double[] {1, 0, 2, -2});
		a2.setColumn(2, new double[] {1, 1, 1, 1});
		RealVector d2 = new ArrayRealVector(new double[] {2.7, 0.1, 8.1, -0.3});
		
		NonNegativeLeastSquaresMethod nnls1 = new NonNegativeLeastSquaresMethod(a1, d1);
		nnls1.compute();
		
		NonNegativeLeastSquaresMethod nnls2 = new NonNegativeLeastSquaresMethod(a2, d2);
		nnls2.compute();
		
		
		double[] coeff1 = new double[] {1, 2, 3};
		double[] coeff2 = new double[] {1, 2, -0.1};
		for (int i = 0; i < 3; i++)
			System.out.printf("%.3f %.3f\n",nnls1.getAnsVector().getEntry(i), coeff1[i]);
		System.out.println();
		for (int i = 0; i < 3; i++)
			System.out.printf("%.3f %.3f\n", nnls2.getAnsVector().getEntry(i), coeff2[i]);
	}
	
	/**
	 * F is the set of free (unbounded) indices
	 * L is the set of lower-bounded indices 
	 */
	private Set<Integer> F, L;
	
	private RealVector d;
	
	private RealMatrix a;
	
	private RealVector x;
	
	private final int MAX_ITERATION;
	
	public NonNegativeLeastSquaresMethod(Matrix a, RealVector d, int MAX_ITERATION) {
		this.d = d;
		this.a = a;
		this.ata = a.computeAtA();
		this.atd = a.transpose().operate(d);
		F = new HashSet<>();
		L = new HashSet<>();
		this.x = new ArrayRealVector(a.getColumnDimension());
		this.MAX_ITERATION = MAX_ITERATION;
		//initialization
		x.set(0);
		L = IntStream.range(0, atd.getDimension()).boxed().collect(Collectors.toSet());
	}
	
	public NonNegativeLeastSquaresMethod(Matrix a, RealVector d) {
		this.d = d;
		this.a = a;
		this.ata = a.computeAtA();
		this.atd = a.transpose().operate(d);
		F = new HashSet<>();
		L = new HashSet<>();
		this.x = new ArrayRealVector(a.getColumnDimension());
		x.set(0);
		this.MAX_ITERATION = 50000;
		//initialization
		x.set(0);
		L = IntStream.range(0, atd.getDimension()).boxed().collect(Collectors.toSet());
	}
	
	public NonNegativeLeastSquaresMethod(Matrix a, RealVector d, RealVector x) {
		this.d = d;
		this.a = a;
		this.ata = a.computeAtA();
		this.atd = a.transpose().operate(d);
		F = new HashSet<>();
		L = new HashSet<>();
		this.MAX_ITERATION = 50000;
		// initialization
		this.x = x;
		for (int i = 0; i < x.getDimension(); i++) {
			if (x.getEntry(i) <= 0) {
				L.add(i);
				x.setEntry(i, 0.);
			}
			else
				F.add(i);
		}
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
		
		int nIteration = 0;
		
		// step 2
		
		
		do  {    // step 3
			w = atd.subtract(ata.operate(x));
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
			boolean goToStep6 = true;
			while (goToStep6 && nIteration < MAX_ITERATION) {
				List<Integer> jPrimeToj = new ArrayList<>();
				for (Integer j : fullSet) {
					if (F.contains(j)) {
						jPrimeToj.add(j);
					}
				}
				
				// b' (bPrime)
				RealVector xPrime = new ArrayRealVector(x);
				for (Integer k : fullSet) {
					if (F.contains(k))
						xPrime.setEntry(k.intValue(), 0.);
				}
				RealVector bPrime = d.subtract(a.operate(xPrime));
				
				// a' (aPrime)
				Matrix aPrime = new Matrix(a.getRowDimension(), jPrimeToj.size());
				for (int i = 0; i < jPrimeToj.size(); i++)
					aPrime.setColumnVector(i, a.getColumnVector(jPrimeToj.get(i).intValue()));
				
				// solve z = arg min ||a' z - b'||_2
				ConjugateGradientMethod cgMethod = new ConjugateGradientMethod(aPrime.computeAtA(), aPrime.transpose().operate(bPrime));
				cgMethod.compute();
				RealVector z = cgMethod.getAns(jPrimeToj.size());
				System.out.println("Z length " + z.getDimension());
				for (double zi : z.toArray())
					System.out.println(zi);
				
				// step 7
				Set<Integer> J = new HashSet<>();
				boolean goToStep2 = true;
				for (int i = 0; i < z.getDimension(); i++) {
					if (z.getEntry(i) <= 0) {
						goToStep2 = false;
						J.add(i);
					}
//					if (z.getEntry(i) < 0)
						
				}
				if (goToStep2) {
					// step 2
					for (int i = 0; i < z.getDimension(); i++)
						x.setEntry(jPrimeToj.get(i), z.getEntry(i));
//					w = atd.subtract(ata.operate(x));
					goToStep6 = false;
					nIteration++;
				}
				else {
					// step 8
					double valueAtqPrime = Double.MAX_VALUE;
					for (Integer jPrime : J) {
						double xj = x.getEntry(jPrimeToj.get(jPrime.intValue()));
						double tmp = Math.abs(xj / (z.getEntry(jPrime.intValue()) - xj));
						if (tmp < valueAtqPrime) {
							valueAtqPrime = tmp;
						}
					}
					
					// step 9
					double alpha = valueAtqPrime;
					
					// step 10
					int jPrime = 0;
					for (Integer j : F) {
						x.setEntry( j.intValue(), x.getEntry(j.intValue()) + alpha * (z.getEntry(jPrime) - x.getEntry(j.intValue())) );
						jPrime++;
					}
					
					// step 11
					for (int i = 0; i < x.getDimension(); i++) {
						if (x.getEntry(i) <= 0) {
							L.add(i);
							F.remove(i);
						}
					}
					nIteration++;
				}
			} // end of step 6 while loop
		} while ( !KuhnTuckerConvergenceTest(w, fullSet) && nIteration < MAX_ITERATION ); // end of step 2 while loop
	}
	
	public RealVector getAnsVector() {
		return x;
	}
	
	private boolean KuhnTuckerConvergenceTest(RealVector w, Set<Integer> fullSet) {
		boolean conditionOnW = true;
		for (Integer i : L)
			if (w.getEntry(i.intValue()) > 0) {
				conditionOnW = false;
				break;
			}
		System.out.println(conditionOnW + "," + F.equals(fullSet));
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