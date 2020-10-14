package io.github.kensuke1984.kibrary.datacorrection;

import io.github.kensuke1984.kibrary.stacking.PeakStack;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.TransformType;

import java.util.Arrays;
import java.util.Set;

/**
 * Source time function estimation by stacked peaks.
 *
 * @author Kensuke Konishi
 * @version 0.0.3.3
 */
public final class SourceTimeFunctionByStackedPeaks extends SourceTimeFunction {

    private SACData[] obsSacs;
    private SACData[] synSacs;

    private double range = 10; // sec

    /**
     * Ratio of peak to peak (Observed/Synthetic)
     */
    private double[] ampRatio;
    private PeakStack ps = new PeakStack();
    private Set<TimewindowInformation> timewindow;
    /**
     * @param np         the number of steps in frequency domain
     * @param tlen       the time length
     * @param samplingHz must be 20 now
     * @param obsSacs    same order as synSacs
     * @param synSacs    same order as obsSacs
     * @param timewindow search region for Peaks
     */
    public SourceTimeFunctionByStackedPeaks(int np, double tlen, double samplingHz, SACData[] obsSacs,
                                            SACData[] synSacs, Set<TimewindowInformation> timewindow) {
        super(np, tlen, samplingHz);
        if (!pairCheck(obsSacs, synSacs)) throw new RuntimeException("Input sac files are invalid.");
        this.timewindow = timewindow;
        this.obsSacs = obsSacs;
        this.synSacs = synSacs;
    }

    /**
     * @param obsSacs Arrays of observed
     * @param synSacs Arrays of synthetics
     * @return if the input pair is valid
     */
    private static boolean pairCheck(SACData[] obsSacs, SACData[] synSacs) {
        if (obsSacs.length != synSacs.length) {
            System.out.println("The length of observed and synthetics is different.");
            return false;
        }
        for (int i = 0; i < obsSacs.length; i++) {
            if (!obsSacs[i].getSACString(SACHeaderEnum.KEVNM).equals(synSacs[i].getSACString(SACHeaderEnum.KEVNM)))
                return false;
            if (!Station.of(obsSacs[i]).equals(Station.of(synSacs[i]))) return false;
            if (obsSacs[i].getValue(SACHeaderEnum.USER1) != synSacs[i].getValue(SACHeaderEnum.USER1) ||
                    obsSacs[i].getValue(SACHeaderEnum.USER2) != synSacs[i].getValue(SACHeaderEnum.USER2)) return false;
        }

        return true;
    }

    private Trace toStackTrace(Trace trace) {
        return ps.stack(null, null, null, null, trace);
    }

    private Trace createTrace(SACData sacFile) {
        Station station = sacFile.getStation();
        GlobalCMTID id = new GlobalCMTID(sacFile.getSACString(SACHeaderEnum.KEVNM));
        SACComponent component = SACComponent.of(sacFile);

        TimewindowInformation window = timewindow.stream()
                .filter(info -> info.getStation().equals(station) && info.getGlobalCMTID().equals(id) &&
                        info.getComponent() == component).findAny().get();

        return sacFile.createTrace().cutWindow(window);
    }

    private int findLsmooth() {
        int lsmooth = (int) (0.5 * tlen * samplingHz / np);
        int i = Integer.highestOneBit(lsmooth);
        if (i < lsmooth) i *= 2;
        return i;
    }

    private void compute() {
        Complex[] sourceTimeFunction = new Complex[np];
        Trace[] obsTraces = Arrays.stream(obsSacs).map(this::createTrace).toArray(Trace[]::new);
        Trace[] synTraces = Arrays.stream(synSacs).map(this::createTrace).toArray(Trace[]::new);
        ampRatio = new double[obsTraces.length];
        for (int i = 0; i < obsTraces.length; i++) {
            double obsP2P = obsTraces[i].getMaxValue() - obsTraces[i].getMinValue();
            double synP2P = synTraces[i].getMaxValue() - synTraces[i].getMinValue();
            ampRatio[i] = obsP2P / synP2P;
        }

        Trace[] obsStackTraces = Arrays.stream(obsTraces).map(this::toStackTrace).toArray(Trace[]::new);
        Trace[] synStackTraces = Arrays.stream(synTraces).map(this::toStackTrace).toArray(Trace[]::new);

        int n = (int) (range * 2 * samplingHz) + 1;
        double[] obsStack = new double[n];
        double[] synStack = new double[n];
        for (int i = 0; i < n; i++) {
            double t = -range + i / samplingHz;
            for (int iTrace = 0; iTrace < obsStackTraces.length; iTrace++) {
                obsStack[i] += obsStackTraces[iTrace].toValue(0, t);
                synStack[i] += synStackTraces[iTrace].toValue(0, t) / ampRatio[iTrace];
            }
            // System.out.println(t + " " + obsStack[i] + " " + synStack[i]);
        }
        // System.exit(0);
        int npts = 2 * np * findLsmooth();
        // System.out.println(TLEN * 20);
        double[] obsUt = new double[npts];
        double[] synUt = new double[npts];
        for (int i = 0; i < n; i++) {
            obsUt[i] = obsStack[i];
            synUt[i] = synStack[i];
        }

        Complex[] obsUf = fft.transform(obsUt, TransformType.FORWARD);
        Complex[] synUf = fft.transform(synUt, TransformType.FORWARD);

        for (int i = 0; i < np; i++)
            sourceTimeFunction[i] = obsUf[i + 1].divide(synUf[i + 1]);
        this.sourceTimeFunction = sourceTimeFunction;
    }

    @Override
    public Complex[] getSourceTimeFunctionInFrequencyDomain() {
        if (sourceTimeFunction != null) return sourceTimeFunction;
        synchronized (this) {
            if (sourceTimeFunction != null) return sourceTimeFunction;
            compute();
        }
        return sourceTimeFunction;
    }
	
}
