/**
 * 
 */
package manhattan.datacorrection;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import filehandling.sac.SACData;

/**
 * Source time function. <br>
 * 
 * You have to multiply<br>
 * Source time function: stf[0], .. stf[np-1] <br>
 * on <br>
 * Waveform in frequency domain: U[1].. U[np], respectively. See
 * {@link #convolve(Complex[])}
 * 
 * @version 0.0.3
 * 
 * @author kensuke
 *
 */
public abstract class SourceTimeFunction {
	/**
	 * Triangle source time function
	 * 
	 * The width is determined by the half duration &tau;. <br>
	 * f(t) = 1/&tau;<sup>2</sup> t + 1/&tau; (-&tau; &le; t &le; 0), -1/&tau;
	 * <sup>2</sup> t + 1/&tau; (0 &le; t &le; &tau;), 0 (t &lt; -&tau;, &tau;
	 * &lt; t) <br>
	 * Source time function F(&omega;) = (1-e<sup>-2&pi;i&omega;&tau;</sup>
	 * -2i&pi;&omega;&tau;)/(2&pi;<sup>2</sup>&omega;<sup>2</sup>&tau;
	 * <sup>2</sup>)
	 * 
	 * @param np
	 *            the number of steps in frequency domain
	 * @param tlen
	 *            [s] time length
	 * @param samplingHz
	 *            [Hz]
	 * @param halfDuration
	 *            [s] of the source
	 */
	public static final SourceTimeFunction triangleSourceTimeFunction(int np, double tlen, double samplingHz,
			double halfDuration) {
		return new SourceTimeFunction(np, tlen, samplingHz) {

			@Override
			public Complex[] getSourceTimeFunction() {
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
		};
	}

	/**
	 * Boxcar source time function
	 * 
	 * The width is determined by the half duration &tau;. <br>
	 * f(t) = &tau;/2 (-&tau; &le; t &le; &tau;), 0 (t &lt; -&tau;, &tau; &lt;
	 * t) <br>
	 * Source time function F(&omega;) = sin(&omega;&tau;)/&omega;&tau;
	 * 
	 * @param np
	 *            the number of steps in frequency domain
	 * @param tlen
	 *            [s] time length
	 * @param samplingHz
	 *            [Hz]
	 * @param halfDuration
	 *            [s] of the source
	 */
	public static final SourceTimeFunction boxcarSourceTimeFunction(int np, double tlen, double samplingHz,
			double halfDuration) {
		return new SourceTimeFunction(np, tlen, samplingHz) {
			{
				getSourceTimeFunction();
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
					sourceTimeFunction[i] = new Complex(Math.sin(omegaTau) / omegaTau);
				}
				return sourceTimeFunction;
			}

		};

	}

	/**
	 * @param np
	 *            must be a power of 2
	 * @param tlen
	 *            [s] must be a tenth of powers of 2
	 * @param samplingHz
	 *            20 preferred (now must)
	 */
	protected SourceTimeFunction(int np, double tlen, double samplingHz) {
		if (!checkValues(np, tlen, samplingHz))
			throw new RuntimeException();
		this.np = np;
		this.tlen = tlen;
		this.samplingHz = samplingHz;
		this.nptsInTimeDomain = np * 2 * computeLsmooth(np, tlen, samplingHz);
	}

	protected static boolean checkValues(int np, double tlen, double samplingHz) {
		boolean bool = true;
		if (samplingHz != 20) {
			System.out.println("Only samplingHz 20 is acceptable now.");
			bool = false;
		}
		if (np <= 0 || (np & (np - 1)) != 0) {
			System.out.println("np must be a power of 2");
			bool = false;
		}
		int tlen10 = (int) Math.round(10 * tlen);
		if (tlen10 <= 0 || (tlen10 & (tlen10 - 1)) != 0) {
			System.out.println("tlen must be a tenth of a power of 2");
			bool = false;
		}

		return bool;
	}

	public int getNp() {
		return np;
	}

	public double getTlen() {
		return tlen;
	}

	public double getSamplingHz() {
		return samplingHz;
	}

	/**
	 * The number of steps in frequency domain. It must be a power of 2.
	 */
	protected final int np;

	/**
	 * timeLength [s]. It must be a tenth of powers of 2
	 */
	protected final double tlen;

	protected final double samplingHz;

	protected Complex[] sourceTimeFunction;

	public abstract Complex[] getSourceTimeFunction();

	public void writeSourceTimeFunction(Path outPath, OpenOption... options) throws IOException {
		if (sourceTimeFunction == null)
			throw new RuntimeException("Source time function is not computed yet.");

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#np tlen samplingHz");
			pw.println(np + " " + tlen + " " + samplingHz);
			for (int i = 0; i < sourceTimeFunction.length; i++)
				pw.println(sourceTimeFunction[i].getReal() + " " + sourceTimeFunction[i].getImaginary());
		}

	}

	private static Complex toComplex(String line) {
		String[] parts = line.split("\\s+");
		double real = Double.parseDouble(parts[0]);
		double imag = Double.parseDouble(parts[1]);
		return new Complex(real, imag);
	}

	public static SourceTimeFunction readSourceTimeFunction(Path sourcePath) throws IOException {
		List<String> lines = Files.readAllLines(sourcePath);
		String[] parts = lines.get(1).split("\\s+");
		int np = Integer.parseInt(parts[0]);
		double tlen = Double.parseDouble(parts[1]);
		double samplingHz = Double.parseDouble(parts[2]);
		Complex[] function = IntStream.range(0, np).mapToObj(i -> toComplex(lines.get(i + 2)))
				.toArray(n -> new Complex[n]);

		SourceTimeFunction stf = new SourceTimeFunction(np, tlen, samplingHz) {
			@Override
			public Complex[] getSourceTimeFunction() {
				return function;
			}
		};
		stf.sourceTimeFunction = function;
		return stf;
	}

	private static int computeLsmooth(int np, double tlen, double samplingHz) {
		int lsmooth = (int) (0.5 * tlen * samplingHz / np);
		int i = Integer.highestOneBit(lsmooth);
		if (i < lsmooth)
			i *= 2;
		return lsmooth;
	}

	protected static final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

	private int nptsInTimeDomain;

	private double[] inverseFourierTransform(Complex[] dataInFrequency) {
		// pack to temporary Complex array
		Complex[] data = new Complex[nptsInTimeDomain];
		System.arraycopy(dataInFrequency, 0, data, 0, np + 1);

		// set blank due to lsmooth
		Arrays.fill(data, np + 1, nptsInTimeDomain / 2 + 1, Complex.ZERO);

		// set values for imaginary frequency
		for (int i = 0, nnp = nptsInTimeDomain / 2; i < nnp - 1; i++)
			data[nnp + 1 + i] = data[nnp - 1 - i].conjugate();

		// fast fourier transformation
		data = fft.transform(data, TransformType.INVERSE);

		return Arrays.stream(data).mapToDouble(d -> d.getReal()).toArray();
	}

	/**
	 * Operates convolution for data in <b>time</b> domain.
	 * 
	 * @param data
	 *            to be convolved in <b>time</b> domain. The data is convolved
	 *            after FFTed.
	 * @return convolute data in <b>time</b> domain
	 */
	public final double[] convolve(double[] data) {
		if (data.length != nptsInTimeDomain)
			throw new IllegalArgumentException("Input data is invalid (length).");
		Complex[] dataInFrequencyDomain = fft.transform(data, TransformType.FORWARD);
		Complex[] convolvedDataInFrequencyDomain = convolve(dataInFrequencyDomain);
		return inverseFourierTransform(convolvedDataInFrequencyDomain);
	}

	/**
	 * Operates convolution for data in <b>frequency</b> domain.
	 * 
	 * @param data
	 *            to be convolved in <b>frequency</b> domain.
	 * @return convolute data in <b>frequency</b> domain
	 */
	public final Complex[] convolve(Complex[] data) {
		if (data.length != np+1)
			throw new IllegalArgumentException("Input data length is invalid.");
		return IntStream.range(0, np + 1).parallel()
				.mapToObj(i -> i == 0 ? data[i] : data[i].multiply(sourceTimeFunction[i - 1]))
				.toArray(n -> new Complex[n]);
	}

	/**
	 * @param sacData
	 *            to convolute with this.
	 * @return convoluted SACData
	 */
	public final SACData convolve(SACData sacData) {
		double[] data = sacData.getData();
		double[] convolute = convolve(data);
		return sacData.setSACData(convolute);
	}

	/**
	 * Source time function is computed simply by division.
	 * 
	 * @param obs
	 *            waveform of observed
	 * @param syn
	 *            waveform of syn
	 * @param np
	 *            steps of frequency [should be same as synthetics]
	 * @param tlen
	 *            [s] length of waveform [should be same as synthetics]
	 * @param samplingHz
	 *            [Hz]
	 * @return Source time function F(obs)/F(syn) in <b>frequency domain</b>
	 */
	public static SourceTimeFunction computeSourceTimeFunction(int np, double tlen, double samplingHz, double[] obs,
			double[] syn) {
		int inputLength = obs.length;
		if (inputLength != syn.length)
			throw new IllegalArgumentException("Input obs and syn waveform must have same lengths");
		int nptsInTimeDomain = computeLsmooth(np, tlen, samplingHz) * np * 2;
		double[] realObs = new double[nptsInTimeDomain];
		double[] realSyn = new double[nptsInTimeDomain];
		for (int i = 0; i < inputLength; i++) {
			realObs[i] = obs[i];
			realSyn[i] = syn[i];
		}
		Complex[] obsInFrequencyDomain = fft.transform(realObs, TransformType.FORWARD);
		Complex[] synInFrequencyDomain = fft.transform(realSyn, TransformType.FORWARD);
		Complex[] sourceTimeFunction = new Complex[np];
		for (int i = 0; i < np; i++)
			sourceTimeFunction[i] = obsInFrequencyDomain[i + 1].divide(synInFrequencyDomain[i + 1]);
		SourceTimeFunction stf = new SourceTimeFunction(np, tlen, samplingHz) {
			@Override
			public Complex[] getSourceTimeFunction() {
				return sourceTimeFunction;
			}
		};
		stf.sourceTimeFunction = sourceTimeFunction;
		return stf;
	}

}
