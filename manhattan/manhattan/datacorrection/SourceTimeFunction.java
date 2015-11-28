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
 * Waveform in frequency domain: U[1].. U[np], respectively.
 * 
 * 
 * @version 0.0.2.1
 * 
 * @author kensuke
 *
 */
public abstract class SourceTimeFunction {

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
	 * @param data
	 *            to be convolved
	 * @return convolved data
	 */
	public final double[] convolute(double[] data) {
		if (data.length != nptsInTimeDomain)
			throw new RuntimeException("Input data is invalid.");
		Complex[] dataInFrequencyDomain = fft.transform(data, TransformType.FORWARD);
		Complex[] convolvedDataInFrequencyDomain = IntStream.range(0, np + 1).parallel().mapToObj(
				i -> i == 0 ? dataInFrequencyDomain[i] : dataInFrequencyDomain[i].multiply(sourceTimeFunction[i - 1]))
				.toArray(n -> new Complex[n]);
		return inverseFourierTransform(convolvedDataInFrequencyDomain);
	}
	
	/**
	 * @param sacData to convolute with this.
	 * @return convoluted SACData
	 */
	public final SACData convolute(SACData sacData){
		double[] data = sacData.getData();
		double[] convoluted = convolute(data);
		return sacData.setSACData(convoluted);
	}
	

	/**
	 * Source time function is computed simply by division.
	 * 
	 * @param obs
	 *            waveform of observed
	 * @param syn
	 *            waveform of syn
	 * @param np steps of frequency [should be same as synthetics]
	 * @param tlen [s] length of waveform [should be same as synthetics] 
	 * @param samplingHz [Hz]
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
