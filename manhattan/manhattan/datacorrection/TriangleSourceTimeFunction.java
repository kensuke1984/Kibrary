/**
 * 
 */
package manhattan.datacorrection;


import org.apache.commons.math3.complex.Complex;

/**
 * 
 * Triangle source time function
 * 
 * The width is determined by the half duration &tau;. <br>
 * f(t) = 1/&tau;<sup>2</sup> t + 1/&tau; (-&tau; &le; t &le; 0), -1/&tau;
 * <sup>2</sup> t + 1/&tau; (0 &le; t &le; &tau;), 0 (t &lt; -&tau;, &tau; &lt;
 * t) <br>
 * Source time function F(&omega;) = (1-e<sup>-2&pi;i&omega;&tau;</sup>
 * -2i&pi;&omega;&tau;)/(2&pi;<sup>2</sup>&omega;<sup>2</sup>&tau;<sup>2</sup>)
 * 
 * @version 0.0.1
 * @author kensuke
 *
 */
public class TriangleSourceTimeFunction extends SourceTimeFunction {

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
	public TriangleSourceTimeFunction(int np, double tlen, double samplingHz, double halfDuration) {
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
			Complex c = new Complex(2, -2 * omegaTau);
			Complex c2 = new Complex(2 * Math.cos(omegaTau), -2 * Math.sin(omegaTau));
			sourceTimeFunction[i] = c.subtract(c2).divide(omegaTau * omegaTau);
		}
		return sourceTimeFunction;
	}


}
