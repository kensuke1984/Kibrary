/**
 * 
 */
package manhattan.datacorrection;



import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.FastMath;



/**
 * 
 * Boxcar source time function
 * 
 * The width is determined by the half duration &tau;. <br>
 * f(t) = &tau;/2 (-&tau; &le; t &le; &tau;), 0 (t &lt; -&tau;, &tau; &lt; t)
 * <br>
 * Source time function F(&omega;) = sin(&omega;&tau;)/&omega;&tau;
 * 
 * @version 0.0.1
 * @author kensuke
 *
 */
public final class BoxcarSourceTimeFunction extends SourceTimeFunction {

	private double halfDuration;

	/**
	 * @param np
	 *            the number of steps in frequency domain
	 * @param tlen
	 *            [s] time length
	 * @param samplingHz
	 *            [Hz]
	 * @param halfDuration
	 *            [s] of the source
	 */
	public BoxcarSourceTimeFunction(int np, double tlen, double samplingHz, double halfDuration) {
		super(np, tlen, samplingHz);
		this.halfDuration = halfDuration;
	}

	@Override
	public synchronized Complex[] getSourceTimeFunction() {
		if (sourceTimeFunction != null)
			return sourceTimeFunction;
		sourceTimeFunction = new Complex[np];
		double deltaF = 1.0 / tlen;
		double constant = 2 * Math.PI * deltaF * halfDuration;
		for (int i = 0; i < np; i++) {
			double omegaTau = (i + 1) * constant;
			sourceTimeFunction[i] = new Complex(FastMath.sin(omegaTau) / omegaTau);
		}
		return sourceTimeFunction;
	}

}
