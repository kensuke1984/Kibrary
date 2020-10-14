package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class SpectrumProfile {

	protected static final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
	
	public static void main(String[] args) throws IOException {
		Path idPath = Paths.get(args[0]);
		Path dataPath = Paths.get(args[1]);
		BasicID[] ids = BasicIDFile.read(idPath, dataPath);
		
		Path root = Paths.get("spc_traces");
		Files.createDirectories(root);
		
		Path root2 = Paths.get("transformed_traces");
		Files.createDirectories(root2);
		
		for (BasicID id : ids) {
			int npts = id.getNpts();
			int npow2 = Integer.highestOneBit(npts) < npts ? Integer.highestOneBit(npts) * 2 : Integer.highestOneBit(npts);
//			npow2 *= 8;
			double[] padded = new double[npow2];
//			System.out.println(id.getSamplingHz());
			double[] tmpdata = taper_hanning(id.getData());
			for (int i = 0; i < npts; i++)
				padded[i] = tmpdata[i];
			Complex[] tmp = fft.transform(padded, TransformType.FORWARD);
			double df = 1. / npow2;
			Complex[] spc = new Complex[tmp.length];
			for (int i = 0; i < spc.length; i++) {
				if (i < spc.length / 2)
					spc[i] = tmp[i];
				else
					spc[i] = new Complex(0.);
			}
//			System.out.println(df + " " + npts + " " + spc.length);
			
			Path eventPath = root.resolve(id.getGlobalCMTID().toString());
			if (!Files.exists(eventPath))
				Files.createDirectory(eventPath);
			
//			int n = (int) ((id.getMinPeriod() + 1.) / df);
			
			double[] smoothed = new double[spc.length];
			for (int i = 0; i < spc.length; i++) {
				int ntmp = 0;
				for (int j = i - 3; j <= i + 3; j++) {
					if (j < 0 || j >= spc.length)
						continue;
					smoothed[i] += spc[j].abs();
					ntmp++;
				}
				smoothed[i] /= ntmp;
			}
			
			double max = new ArrayRealVector(smoothed).getMaxValue();
			double[] ampli = new double[smoothed.length];
			for (int i = 0; i < ampli.length; i++) {
				ampli[i] = max / smoothed[i] < 2.5 ? max / smoothed[i] : 2.5;
			}
			
			int idtime = (int) id.getStartTime();
			String component = id.getWaveformType().equals(WaveformType.SYN) ? id.getSacComponent().toString() + "sc" : id.getSacComponent().toString();
			String name = id.getStation() + "." + id.getGlobalCMTID() + "." + idtime + "." + component + ".spc.txt";
			Path outpath = eventPath.resolve(name);
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
				for (int i = 0; i < spc.length; i++)
					pw.println(df * i + " " + spc[i].abs() / max + " " + smoothed[i] / max + " " + ampli[i]);
			}
			
			Complex[] dataprime = fft.transform(spc, TransformType.INVERSE);
			double dt = (double) (npts) / npow2;
			
			Path eventPath2 = root2.resolve(id.getGlobalCMTID().toString());
			if (!Files.exists(eventPath2))
				Files.createDirectory(eventPath2);
			
			idtime = (int) id.getStartTime();
			component = id.getWaveformType().equals(WaveformType.SYN) ? id.getSacComponent().toString() + "sc" : id.getSacComponent().toString();
			name = id.getStation() + "." + id.getGlobalCMTID() + "." + idtime + "." + component + ".txt";
			Path outpath2 = eventPath2.resolve(name);
			try (PrintWriter pw2 = new PrintWriter(Files.newBufferedWriter(outpath2, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
				for (int i = 0; i < dataprime.length; i++)
					pw2.println(id.getStartTime() + dt * i + " " + dataprime[i].getReal());
			}
			
			idtime = (int) id.getStartTime();
			component = id.getWaveformType().equals(WaveformType.SYN) ? id.getSacComponent().toString() + "sc" : id.getSacComponent().toString();
			name = id.getStation() + "." + id.getGlobalCMTID() + "." + idtime + "." + component + "original.txt";
			outpath2 = eventPath2.resolve(name);
			try (PrintWriter pw2 = new PrintWriter(Files.newBufferedWriter(outpath2, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
				for (int i = 0; i < npts; i++)
					pw2.println(id.getStartTime() + i + " " + id.getData()[i]);
			}
		}
	}
	
	private static double[] taper_hanning(double[] v) {
		int n = v.length;
		double[] res = new double[n];
		for (int i = 0; i < n;i++) {
			res[i] = v[i] * 0.5 * (1 - Math.cos(Math.PI / (n-1) * i));
		}
		return res;
	}
	
	protected static double gaborDist(double s, double m, double t) {
//		double a = 1. / Math.pow(Math.PI * s, 0.25);
		double a = 1.;
		double b = -1. / (2 * s * s);
		return a*Math.exp(b * (t-m) * (t-m));
	}
	
	private static double[] rmean(double[] data) {
		int n = data.length;
		double[] y = new double[n];
		double mean = 0;
		for (double m : data)
			mean += m;
		mean /= n;
		for (int i = 0; i < n; i++)
			y[i] = data[i] - mean;
		return y;
	}
}
