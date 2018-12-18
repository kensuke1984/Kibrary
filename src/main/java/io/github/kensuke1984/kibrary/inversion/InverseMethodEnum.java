/**
 * 
 */
package io.github.kensuke1984.kibrary.inversion;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Names of methods for inversion. such as conjugate gradient method, singular
 * value decomposition.. etc
 * 
 * @author Kensuke Konishi
 * @version 0.0.2.1
 */
public enum InverseMethodEnum {
	SINGURAR_VALUE_DECOMPOSITION, CONJUGATE_GRADIENT, LEAST_SQUARES_METHOD,
	NON_NEGATIVE_LEAST_SQUARES_METHOD, BICONJUGATE_GRADIENT_STABILIZED_METHOD,
	FAST_CONJUGATE_GRADIENT, FAST_CONJUGATE_GRADIENT_DAMPED;
	
	public String simple() {
		switch (this) {
		case SINGURAR_VALUE_DECOMPOSITION:
			return "SVD";
		case CONJUGATE_GRADIENT:
			return "CG";
		case LEAST_SQUARES_METHOD:
			return "LSM";
		case NON_NEGATIVE_LEAST_SQUARES_METHOD:
			return "NNLS";
		case BICONJUGATE_GRADIENT_STABILIZED_METHOD:
			return "BCGS";
		case FAST_CONJUGATE_GRADIENT:
			return "CG";
		case FAST_CONJUGATE_GRADIENT_DAMPED:
			return "CG";
		default:
			throw new RuntimeException("Unexpected");
		}
	}

	public static InverseMethodEnum of(String simple) {
		switch (simple) {
		case "svd":
		case "SVD":
			return SINGURAR_VALUE_DECOMPOSITION;
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
		default:
			throw new IllegalArgumentException("Invalid name for InverseMethod");
		}
	}

	InverseProblem getMethod(RealMatrix ata, RealVector atd) {
		switch (this) {
		case SINGURAR_VALUE_DECOMPOSITION:
			return new SingularValueDecomposition(ata, atd);
		case CONJUGATE_GRADIENT:
			return new ConjugateGradientMethod(ata, atd);
		case FAST_CONJUGATE_GRADIENT:
			return new FastConjugateGradientMethod(ata, atd, false); //TODO the name should be changed, but "ata" for FastConjugateGradientMethod is actually "a" (ata not needed for CG).
		case FAST_CONJUGATE_GRADIENT_DAMPED:
			return new FastConjugateGradientMethod(ata, atd, true); //TODO the name should be changed, but "ata" for FastConjugateGradientMethod is actually "a" (ata not needed for CG).
		case BICONJUGATE_GRADIENT_STABILIZED_METHOD:
			return new BiConjugateGradientStabilizedMethod(ata, atd);
		default:
			throw new RuntimeException("soteigai");
		}
	}

}
