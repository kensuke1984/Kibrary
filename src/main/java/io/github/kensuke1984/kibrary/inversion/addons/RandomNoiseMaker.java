package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.util.Trace;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Arrays;

/**
 * 波形のノイズを作る
 * <p>
 * １、samplingHzとtlenとnpを設定 ２、周波数空間でランダム波形作成 ３、実空間に戻す
 *
 * @author Kensuke Konishi
 * @version 0.1.0 ランダム波形作成を並列化
 */
public final class RandomNoiseMaker {

    private RandomNoiseMaker() {
    }

    /**
     * 周波数空間のspectorの波形を求めてセットする 波形は振幅固定で 角度を散らした周波数スペクトル
     */
    private static Complex[] createRandomComplex(double amplitude, double samplingHz, double tlen, int np) {
        int nnp = np * findLsmooth(samplingHz, tlen, np);
        Complex[] spectorU = new Complex[nnp * 2];
        // pack to temporary Complex array
        for (int i = 0; i <= np; i++) {
            double argument = 2 * Math.PI * Math.random();
            spectorU[i] = new Complex(amplitude * Math.cos(argument), amplitude * Math.sin(argument));
        }

        // set blank due to lsmooth
        Arrays.fill(spectorU, np + 1, nnp + 1, Complex.ZERO);

        // set values for imaginary frequency
        for (int i = 0; i < nnp - 1; i++) {
            int ii = nnp + 1 + i;
            int jj = nnp - 1 - i;
            spectorU[ii] = spectorU[jj].conjugate();
        }
        return spectorU;
    }

    /**
     * @param amplitude  of noize
     * @param samplingHz [Hz] of noize
     * @param tlen       [s] time length of noize
     * @param np         the number of step in frequency domain. (must be a power of 2)
     * @return Trace of time and noize
     */
    public static Trace create(double amplitude, double samplingHz, double tlen, int np) {
        Complex[] spectorU = createRandomComplex(amplitude, samplingHz, tlen, np);
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] timeU = fft.transform(spectorU, TransformType.INVERSE);
        int npts = 2 * np * findLsmooth(samplingHz, tlen, np);
        double[] noise = new double[npts];
        double[] time = new double[npts];
        for (int i = 0; i < npts; i++)
            noise[i] = timeU[i].getReal();
        Arrays.setAll(time, j -> j / samplingHz);
        return new Trace(time, noise);
    }

    /**
     * frequency domain をsamplingFrequencyでtime-domain TLEN(s)にもってくるスムージング値を探す
     */
    private static int findLsmooth(double samplingHz, double tlen, int np) {

        int lsmooth = (int) (0.5 * tlen * samplingHz / np);
        int i = Integer.highestOneBit(lsmooth);
        if (i < lsmooth) i *= 2;
        return i;
    }
}
