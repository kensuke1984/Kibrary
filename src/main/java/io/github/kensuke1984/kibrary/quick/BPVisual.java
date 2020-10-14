package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.SPCBody;
import io.github.kensuke1984.kibrary.util.spc.SPCComponent;
import io.github.kensuke1984.kibrary.util.spc.SPCFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.math3.complex.Complex;

public class BPVisual {
	
	final int samplingHz = 20;
	
	private Path timewindowPath;
	private Set<TimewindowInformation> timewindows;
	private Path workingDir;
	
	private int ext;
	private int step;
	private double partialSamplingHz;
	private double finalSamplingHz;
	
	private double minFreq;
	private double maxFreq;
	private int filterNp;
	private ButterworthFilter filter;
	
	
	public BPVisual(Path timewindowPath, Path workingDir, double partialSamplingHz, double finalSamplingHz
			, double minFreq, double maxFreq, int filterNp) throws IOException {
		this.timewindowPath = timewindowPath;
		this.timewindows = TimewindowInformationFile.read(timewindowPath);
		this.workingDir = workingDir;
		
		this.partialSamplingHz = partialSamplingHz;
		this.finalSamplingHz = finalSamplingHz;
		
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.filterNp = filterNp;
	}
	
	public void run() throws IOException {
		setBandPassFilter();
		
		// バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
		ext = (int) (1 / minFreq * partialSamplingHz);

		// sacdataを何ポイントおきに取り出すか
		step = (int) (partialSamplingHz / finalSamplingHz);
		
		for (SPCFile spcName : Utilities.collectSpcFileName(Paths.get("."))) {
			DSMOutput bpSpc = spcName.read();
			
			HorizontalPosition obsPos = bpSpc.getObserverPosition();
			double[] bodyR = bpSpc.getBodyR();
			
			for (int i = 0; i < bpSpc.nbody(); i++) {
				SPCBody body = bpSpc.getSpcBodyList().get(i);
				
				int lsmooth = body.findLsmooth(bpSpc.tlen(), samplingHz);
				body.toTimeDomain(lsmooth);
				
				SPCComponent[] spcComponents = body.getSpcComponents();
				for (int j = 0; j < spcComponents.length; j++) {
					double[] bpserie = spcComponents[j].getTimeseries();
					Complex[] bpspectrum = spcComponents[j].getValueInFrequencyDomain();
					for (TimewindowInformation info : timewindows) {
						Station station = info.getStation();
						GlobalCMTID event = info.getGlobalCMTID();
						
						Complex[] u = cutPartial(bpserie, info);
						u = filter.applyFilter(u);
						double[] cutU = sampleOutput(u, info);
						
						Phases phases = new Phases(info.getPhases());
						Path outpath = workingDir.resolve(station.getName() + "." 
								+ event + "." + "BP" + "." + (int) obsPos.getLatitude()
								+ "." + (int) obsPos.getLongitude() + "." + (int) bodyR[i] + "." + phases + "." + j + ".txt");
//						Files.deleteIfExists(outpath);
//						Files.createFile(outpath);
						try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE_NEW))) {
							for (double y : cutU)
								pw.println(String.format("%.16e", y));
						}
						
						Path outpath2 = workingDir.resolve(station.getName() + "." 
								+ event + "." + "BP" + "." + (int) obsPos.getLatitude()
								+ "." + (int) obsPos.getLongitude() + "." + (int) bodyR[i] + "." + phases + "." + j + ".spectrum.txt");
						try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath2, StandardOpenOption.CREATE_NEW))) {
							for (Complex y : bpspectrum)
								pw.println(String.format("%.16e", y.abs()));
						}
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		Path timewindowPath = Paths.get(args[0]);
		Path workingDir = Paths.get(".");
		double partialSamplingHz = 20.;
		double finalSamplingHz = 4.;
		double minFreq = 0.01;
		double maxFreq = 0.05;
		int filterNp = 6;
		
		BPVisual bpVisual = new BPVisual(timewindowPath, workingDir
				, partialSamplingHz, finalSamplingHz, minFreq, maxFreq, filterNp);
		
		bpVisual.run();
	}
	
	/**
	 * cut partial derivative in [start-ext, start+ext] The ext is for
	 * filtering .
	 * 
	 * @param u
	 * @param property
	 * @return
	 */
	private Complex[] cutPartial(double[] u, TimewindowInformation timewindowInformation) {
		int cutstart = (int) (timewindowInformation.getStartTime() * partialSamplingHz) - ext;
		// cutstartが振り切れた場合0 からにする
		if (cutstart < 0)
			return null;
		int cutend = (int) (timewindowInformation.getEndTime() * partialSamplingHz) + ext;
		Complex[] cut = new Complex[cutend - cutstart];
		Arrays.parallelSetAll(cut, i -> new Complex(u[i + cutstart]));

		return cut;
	}

	private double[] sampleOutput(Complex[] u, TimewindowInformation timewindowInformation) {
		// 書きだすための波形
		int outnpts = (int) ((timewindowInformation.getEndTime() - timewindowInformation.getStartTime())
				* finalSamplingHz);
		double[] sampleU = new double[outnpts];

		// cutting a waveform for outputting
		Arrays.parallelSetAll(sampleU, j -> u[ext + j * step].getReal());
		return sampleU;
	}
	
	private void setBandPassFilter() throws IOException {
		System.err.println("Designing filter.");
		double omegaH = maxFreq * 2 * Math.PI / partialSamplingHz;
		double omegaL = minFreq * 2 * Math.PI / partialSamplingHz;
		filter = new BandPassFilter(omegaH, omegaL, filterNp);
		filter.setBackward(false);
//		filter.setBackward(true);
//		writeLog(filter.toString());
//		periodRanges = new double[][] { { 1 / maxFreq, 1 / minFreq } };
	}

}
