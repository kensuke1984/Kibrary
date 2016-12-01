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
	NON_NEGATIVE_LEAST_SQUARES_METHOD;

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
		default:
			throw new RuntimeException("UnEXpECCted");
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
		default:
			throw new RuntimeException("soteigai");
		}
	}

}
