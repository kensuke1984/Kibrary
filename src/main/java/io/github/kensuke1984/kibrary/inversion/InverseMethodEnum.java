package io.github.kensuke1984.kibrary.inversion;

import java.util.stream.IntStream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Names of methods for inversion. such as conjugate gradient method, singular
 * value decomposition.. etc
 *
 * @author Kensuke Konishi
 * @version 0.0.3
 * @author anselme added several methods
 */
public enum InverseMethodEnum {
	SINGULAR_VALUE_DECOMPOSITION, CONJUGATE_GRADIENT, LEAST_SQUARES_METHOD,
	NON_NEGATIVE_LEAST_SQUARES_METHOD, BICONJUGATE_GRADIENT_STABILIZED_METHOD,
	FAST_CONJUGATE_GRADIENT, FAST_CONJUGATE_GRADIENT_DAMPED, NONLINEAR_CONJUGATE_GRADIENT,
	CONSTRAINED_CONJUGATE_GRADIENT;
	
	RealVector conditioner;

	public static InverseMethodEnum of(String simple) {
		switch (simple) {
		case "svd":
		case "SVD":
			return SINGULAR_VALUE_DECOMPOSITION;
		case "cg":
		case "CG":
			return CONJUGATE_GRADIENT;
		case "LSM":
		case "lsm":
			return LEAST_SQUARES_METHOD;
		case "NNLS":
		case "nnls":
			return NON_NEGATIVE_LEAST_SQUARES_METHOD;
		case "BCGS":
		case "bcgs":
			return BICONJUGATE_GRADIENT_STABILIZED_METHOD;
		case "FCG":
		case "fcg":
			return FAST_CONJUGATE_GRADIENT;
		case "FCGD":
		case "fcgd":
			return FAST_CONJUGATE_GRADIENT_DAMPED;
		case "NCG":
			return NONLINEAR_CONJUGATE_GRADIENT;
		case "CCG":
			return CONSTRAINED_CONJUGATE_GRADIENT;
		default:
			throw new IllegalArgumentException("Invalid name for InverseMethod");
		}
	}
	
	
	public void setConditioner(RealVector m) {
		conditioner = m;
	}
	
	public String simple() {
		switch (this) {
		case SINGULAR_VALUE_DECOMPOSITION:
			return "SVD";
		case CONJUGATE_GRADIENT:
			return "CG";
		case LEAST_SQUARES_METHOD:
			return "LSM";
		case NON_NEGATIVE_LEAST_SQUARES_METHOD:
			return "NNLS";
		case BICONJUGATE_GRADIENT_STABILIZED_METHOD:
			return "BCGS";
		// TODO: implements FCG FCGD NLCG and CCG to work with the workflow. Now works only if CG
		case FAST_CONJUGATE_GRADIENT:
			return "FCG";
		case FAST_CONJUGATE_GRADIENT_DAMPED:
			return "FCGD";
		case NONLINEAR_CONJUGATE_GRADIENT:
			return "NLCG";
		case CONSTRAINED_CONJUGATE_GRADIENT:
			return "CCG";
		default:
			throw new RuntimeException("Unexpected");
		}
	}

	InverseProblem getMethod(RealMatrix ata, RealVector atd) {
		switch (this) {
		case SINGULAR_VALUE_DECOMPOSITION:
			return new SingularValueDecomposition(ata, atd);
		case CONJUGATE_GRADIENT:
			return new ConjugateGradientMethod(ata, atd);
		case FAST_CONJUGATE_GRADIENT:
			return new FastConjugateGradientMethod(ata, atd, false); //TODO the name should be changed, but "ata" for FastConjugateGradientMethod is actually "a" (ata not needed for CG).
		case FAST_CONJUGATE_GRADIENT_DAMPED:
			if (conditioner == null) {
				conditioner = new ArrayRealVector(atd.getDimension());
				IntStream.range(0, atd.getDimension()).forEach(i -> conditioner.setEntry(i, 1.));
			}
			return new FastConjugateGradientMethod(ata, atd, true, conditioner); //TODO the name should be changed, but "ata" for FastConjugateGradientMethod is actually "a" (ata not needed for CG).
		case BICONJUGATE_GRADIENT_STABILIZED_METHOD:
			return new BiConjugateGradientStabilizedMethod(ata, atd);
		default:
			throw new RuntimeException("soteigai");
		}
	}
	
	InverseProblem getMethod(RealMatrix ata, RealMatrix a, RealVector u, RealVector s0) {
		switch (this) {
		case NONLINEAR_CONJUGATE_GRADIENT:
			return new NonlinearConjugateGradientMethod(ata, a, s0, u);
		default:
			throw new RuntimeException("soteigai");
		}
	}
	
	InverseProblem getMethod(RealMatrix ata, RealVector atd, RealMatrix h) {
		switch (this) {
		case CONSTRAINED_CONJUGATE_GRADIENT:
			return new ConstrainedConjugateGradientMethod(ata, atd, h);
		default:
			throw new RuntimeException("soteigai");
		}
	}

}
