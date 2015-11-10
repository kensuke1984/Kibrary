/**
 * 
 */
package manhattan.inversion;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Names of methods for inversion. such as conjugate gradient method, singular
 * value decomposition.. etc
 * 
 * @author kensuke
 * @since 2015/01/28
 * @version 0.0.1
 */
public enum InverseMethodEnum {
	SVD, CG;
	
	InverseProblem getMethod(RealMatrix ata, RealVector atd){
		switch (this){
		case SVD:
			return new SingularValueDecomposition(ata, atd);
		case CG: 
			return new ConjugateGradientMethod(ata, atd);
		default:
			throw new RuntimeException("soteigai");
		}
		
		
	}
	
}
