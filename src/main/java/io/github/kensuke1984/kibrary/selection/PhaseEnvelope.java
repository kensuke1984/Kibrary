package io.github.kensuke1984.kibrary.selection;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.external.TauPPhase;
import io.github.kensuke1984.kibrary.external.TauPTimeReader;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACExtension;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.AbstractXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;


/**
 * 
 * Implements the phase Envelope as defined in Fichtner et al. (2008).
 * 
 * @author Anselme
 * 
 * @version 1.0
 * 
 */
public class PhaseEnvelope implements Operation {
	
	private Properties property;
	
	public PhaseEnvelope(Properties property) {
		this.property = (Properties) property.clone();
		set();
	}
	
	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(PhaseEnvelope.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan PhaseEnvelope");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Path of a root folder containing observed dataset (.)");
			pw.println("#obsPath");
			pw.println("##Path of a root folder containing synthetic dataset (.)");
			pw.println("#synPath");
			pw.println("##boolean convolute (true)");
			pw.println("#convolute");
			pw.println("##double spectral amplitude misfit (3.0); may be better not to actually use it and rely only on the phase misfit");
			pw.println("#spcAmpMisfit");
			pw.println("##double phaseMisfit (degrees) (85)");
			pw.println("#phaseMisfit");
			pw.println("##int np (1024)");
			pw.println("#np 1024");
			pw.println("##double tlen (s) (6553.6)");
			pw.println("#tlen 6553.6");
			pw.println("##double samplingHz (20)");
			pw.println("#samplingHz");
			pw.println("##double dt (s) (0.05)");
			pw.println("#dt");
			pw.println("##double maxFrequency (Hz) (0.08). Please set it to the maximum frequency of the bandpass filter you used.");
			pw.println("#maxFrequency");
			pw.println("##boolean show time series (true)");
			pw.println("#show");
			pw.println("##double simgaFactor (0.5); multiplicative factor of the dominant period for the Gabor window's width");
			pw.println("#sigmaFactor");
			pw.println("##double threshold (0.025); multiplicative threshold of the maximum amplitude in the timeserie; 0.1 would be conservative");
			pw.print("#threshold");
			pw.println("##double variance (2.0)");
			pw.println("#variance");
		}
	}
	
	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("obsPath"))
			property.setProperty("obsPath", "");
		if (!property.containsKey("synPath"))
			property.setProperty("synPath", "");
		if (!property.containsKey("convolute"))
			property.setProperty("convolute", "true");
		if (!property.containsKey("spcAmpMisfit"))
			property.setProperty("spcAmpMisfit", "3.0");
		if (!property.containsKey("phaseMisfit"))
			property.setProperty("phaseMisfit", "85");
		if (!property.containsKey("np"))
			property.setProperty("np", "1024");
		if (!property.containsKey("tlen"))
			property.setProperty("tlen", "6553.6");
		if (!property.containsKey("samplingHz"))
			property.setProperty("samplingHz", "20");
		if (!property.containsKey("dt"));
			property.setProperty("dt", "0.05");
		if (!property.containsKey("maxFrequency"))
			property.setProperty("maxFrequency", "0.08");
		if (!property.containsKey("show"))
			property.setProperty("show", "true");
		if (!property.containsKey("sigmaFactor"))
			property.setProperty("sigmaFactor", "0.5");
		if (!property.containsKey("threshold"))
			property.setProperty("threshold", "0.025");
		if (!property.containsKey("variance"))
			property.setProperty("variance", "2.0");
	}
	
	private Path workPath;
	
	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		String date = Utilities.getTemporaryString();
		outputPath = workPath.resolve("timewindow" + date + ".dat");
		timewindowSet = Collections.synchronizedSet(new HashSet<>());
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		obsPath = getPath("obsPath");
		synPath = getPath("synPath");
		convolute = Boolean.parseBoolean(property.getProperty("convolute"));
		spcAmpMisfit = Double.parseDouble(property.getProperty("spcAmpMisfit"));
		phaseMisfit = Double.parseDouble(property.getProperty("phaseMisfit"));
		samplingHz = Double.parseDouble(property.getProperty("samplingHz"));
		tlen = Double.parseDouble(property.getProperty("tlen"));
		np = Integer.parseInt(property.getProperty("np"));
		dt = Double.parseDouble(property.getProperty("dt"));
		maxFrequency = Double.parseDouble(property.getProperty("maxFrequency"));
		show = Boolean.parseBoolean(property.getProperty("show"));
		sigmaFactor = Double.parseDouble(property.getProperty("sigmaFactor"));
		threshold = Double.parseDouble(property.getProperty("threshold"));
		variance = Double.parseDouble(property.getProperty("variance"));
	}
	
	public static void main(String[] args) throws Exception {
		Properties property = new Properties();
		if (args.length == 0)
			property.load(Files.newBufferedReader(Operation.findPath()));
		else if (args.length == 1)
			property.load(Files.newBufferedReader(Paths.get(args[0])));
		else
			throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");

		PhaseEnvelope phaseEnvelope = new PhaseEnvelope(property);
		System.err.println(PhaseEnvelope.class.getName() + " is going.");
		long startT = System.nanoTime();
		phaseEnvelope.run();
		System.err.println(
				PhaseEnvelope.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - startT));
	}
	
	@Override
	public void run() throws Exception {
		Set<TimewindowInformation> infoset = new HashSet<>();
		Utilities.runEventProcess(obsPath, obsEventDir -> {
			try {
				obsEventDir.sacFileSet().stream().filter(sfn -> sfn.isOBS() && components.contains(sfn.getComponent()))
					.forEach(obsname -> {
						Path synEventPath = synPath.resolve(obsEventDir.getGlobalCMTID().toString());
						if (!Files.exists(synEventPath))
							throw new RuntimeException(synEventPath + " does not exist.");
						
//							String network = obsname.readHeader().getSACString(SACHeaderEnum.KNETWK);
//							String stationString = obsname.getStationName() + "_" + network;
							String stationString = obsname.getStationName();
							GlobalCMTID id = obsname.getGlobalCMTID();
							SACComponent component = obsname.getComponent();
							String name = convolute
									? stationString + "." + id + "." + SACExtension.valueOfConvolutedSynthetic(component)
									: stationString + "." + id + "." + SACExtension.valueOfSynthetic(component);
							SACFileName synname = new SACFileName(synEventPath.resolve(name));
							System.out.println(obsname);
							
							SACData obssac = null;
							SACData synsac = null;
							
							double obsDep = 0;
							double synDep = 0;
							try {
								obssac = obsname.read();
								synsac = synname.read();
								obsDep = new ArrayRealVector(obssac.getData()).getLInfNorm();
								synDep = new ArrayRealVector(synsac.getData()).getLInfNorm();
							} catch (IOException e) {
								e.printStackTrace();
							}
							if (obsDep != 0 && synDep !=0 && (obsDep/synDep > 30 || synDep/obsDep > 30)) {
								System.err.println("Amplitude ratio > 30. Ignored " + obsname + " "+ obsDep/synDep + " "+ synDep/obsDep);
							}
							else {
								double[][][] phaseEnvelope = computePhaseEnvelope(obsname, synname);
								if (phaseEnvelope != null) {
									double frequencyIncrement = phaseEnvelope[2][0][1] - phaseEnvelope[2][0][0];
									
									double[][] freqIntPE = freqIntegrated(phaseEnvelope, maxFrequency);
	//								Path outpath = workPath.resolve("frequencyIntegratedPhaseEnvelope.txt");
	//								try {
	//									Files.deleteIfExists(outpath);
	//									Files.createFile(outpath);
	//									for (int i = 0; i < freqIntPE[0].length; i++)
	//										Files.write(outpath, (freqIntPE[0][i] + " " + freqIntPE[1][i] + "\n").getBytes()
	//												, StandardOpenOption.APPEND);
	//								} catch (IOException e) {
	//									e.printStackTrace();
	//								}
									
									double minLength = Math.min(1.2/maxFrequency, 20.);
									double[][] timewindows = selectTimewindows(freqIntPE, 0.33/maxFrequency, minLength);
//									if (timewindows != null) {
			//							for (int i = 0; i < timewindows.length; i++)
			//								System.out.println(timewindows[i][0] + " " + timewindows[i][1]);
										
										double distance = obssac.getEventLocation().getEpicentralDistance(obssac.getStation().getPosition())
											* 180 / Math.PI;
										double eventR = obssac.getEventLocation().getR();
										
//										timewindows = filterOnTimeserieAmplitude(timewindows, obssac, synsac, threshold);
//										TauPPhase[][] phases = findPhases(timewindows, distance, eventR);
										
										TauPPhase[][] phases = null;
										
										if (show) {
											try {
												System.out.println("plotting...");
												String title = obssac.getStation() + " " + obsEventDir.getGlobalCMTID();
												showSeriesAndSpectra(obssac.createTrace(), synname.read().createTrace(), freqIntPE, timewindows, phases, title);
				//								showTimeSeries(obsname.read().createTrace(), synname.read().createTrace(), timewindows);
												System.in.read();
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
										
			//							String titleFig = stationString + "." + id;
			//							showPhaseMisfit(phaseEnvelope[0], titleFig, frequencyIncrement);
			//							showRatio(phaseEnvelope[1], titleFig, frequencyIncrement);
										
										try {
											if (timewindows != null) {
												for (int i = 0; i < timewindows.length; i++) {
													Phase[] phasenames = new Phase[phases[i].length];
													phasenames = Stream.of(phases[i]).map(phase -> phase.getPhaseName()).collect(Collectors.toList()).toArray(phasenames);
													TimewindowInformation info = new TimewindowInformation(timewindows[i][0], timewindows[i][1], obsname.read().getStation()
															, obsEventDir.getGlobalCMTID(), obsname.getComponent(), phasenames);
													infoset.add(info);
												}
											}
										} catch (IOException e) {
											e.printStackTrace();
										}
//									}
								}
							}
					});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 10, TimeUnit.HOURS);
		TimewindowInformationFile.write(infoset, outputPath);
	}
	
	private double[][][] computePhaseEnvelope(SACFileName obsname, SACFileName synname) {
		double[][][] phaseEnvelope = new double[3][][];
		
		double endtime = 2000.;
		double margin = 850.;
		double starttime = 0;
		int npts = (int) (tlen * samplingHz);
		int n = (int) (endtime - starttime); // re-sampling at 1Hz
//		int npow2 = (int) (tlen * 20);
		
		phaseEnvelope[0] = new double[n][];
		phaseEnvelope[1] = new double[n][];
		phaseEnvelope[2] = new double[n][];
		double[][] obsSpcmemo = new double[n][];
		double[][] synSpcmemo = new double[n][];
//		double[] freqIntegratedPhaseMisfit = new double[n];
//		double[] freqIntegratedEnvelope = new double[n];
		
		try {
			if (endtime + margin > synname.readHeader().getValue(SACHeaderEnum.E))
				throw new IllegalArgumentException(synname + " end time smaller than the given end time: " + endtime);
			if (obsname.readHeader().getValue(SACHeaderEnum.B) > 0)
				throw new IllegalArgumentException(obsname + " start time > 0: " + obsname.readHeader().getValue(SACHeaderEnum.B));
			
			double[] tmpobsdata = obsname.read().createTrace().cutWindow(starttime, endtime + margin).getY();
			double[] tmpsyndata = synname.read().createTrace().cutWindow(starttime, endtime + margin).getY();
			
			if (samplingHz != 20.)
				System.err.println("Warning: sampling Hz != 20");
			
//			for (int i = 0; i < n; i++) {
//				obsdata[i] = tmpobsdata[i];
//			}
//			Arrays.fill(obsdata, n, npow2, 0.); // pad with 0 for the fft
			
			double[] frequencyAmplitude = null;
			
			try {
				frequencyAmplitude = dominantFrequencySwave(obsname, 5./maxFrequency, 5./maxFrequency);
			} catch (IllegalStateException e) {
				System.out.println(e.getMessage());
			}
			if (frequencyAmplitude == null) {
				return null;
			}
			
			double sigma = 1. / frequencyAmplitude[0] * sigmaFactor;
			double spcAmplitude = frequencyAmplitude[1];
			if (frequencyAmplitude[0] / maxFrequency < .4 || frequencyAmplitude[0] / maxFrequency > 1.1) {
				System.out.println("Ignoring: dominant frequency strongly differs from maximum frequency " + frequencyAmplitude[0] + "," + maxFrequency + " Possible fix: is the maximum frequency you set in the parameter file equal to the maximum frequency of the bandpass filter?");
				return null;
			}
			else {
	//			System.out.println(sigma + " " + spcAmplitude);
	//			sigma = 15;
				
				int nn = 20 * (int) sigma < n ? 10 * (int) sigma : n;
				int nnpow2 = Integer.highestOneBit(2*nn+1) < 2*nn+1 ? Integer.highestOneBit(2*nn+1) * 2 : Integer.highestOneBit(2*nn+1);
				nnpow2 = nnpow2 < 2048 ? 2048 : nnpow2;
				double frequencyIncrement = (npts) / (nnpow2 * samplingHz) * 1. / tlen; // nnpow2 is sampling at 1 Hz
				
				int nnp = (int) (np * nnpow2 * samplingHz / npts);
	//			System.out.println(nnp + " " + frequencyIncrement);
				for (int i = 0; i < n; i++) {
					phaseEnvelope[0][i] = new double[nnp];
					phaseEnvelope[1][i] = new double[nnp];
					phaseEnvelope[2][i] = new double[nnp];
					obsSpcmemo[i] = new double[nnp];
					synSpcmemo[i] = new double[nnp];
				}
				
				double[] obsdata = new double[nnpow2];
				double[] syndata = new double[nnpow2];
				double[] gaborWindow = gaborWindow(sigma, 2*nn+1, dt*samplingHz);
				
				double spcMax = Double.MIN_VALUE;
				for (int i = 0; i < n; i++) {
					int nlow = i - nn < 0 ? 0 : i - nn;
					int nnlow = i - nn < 0 ? nn - i : 0;
					int k = 0;
					for (int j = nlow; j <= i+nn; j++) {
						obsdata[nnlow + k] = tmpobsdata[j * (int) samplingHz] * gaborWindow[2*nn - k];
						syndata[nnlow + k] = tmpsyndata[j * (int) samplingHz] * gaborWindow[2*nn - k];
						k++;
					}
					Arrays.fill(obsdata, 2*nn+1, nnpow2, 0.);
					Arrays.fill(syndata, 2*nn+1, nnpow2, 0.);
					Arrays.fill(obsdata, 0, nnlow, 0.);
					Arrays.fill(syndata, 0, nnlow, 0.);
					
	//				if (i==1200) {
	//					for (k = 0; k < syndata.length; k++)
	//					System.out.println(syndata[k]);
	//				}
					
					syndata = rmean(syndata);
					obsdata = rmean(obsdata);
						
					Complex[] synSpc = fft.transform(syndata, TransformType.FORWARD);
					Complex[] obsSpc = fft.transform(obsdata, TransformType.FORWARD);
					
					for (int j = 0; j < nnp; j++) {
						if (synSpc[j].abs() > 0 && obsSpc[j].abs() > 0) {
	//						 double sin = (obsSpc[j].getReal()*synSpc[j].getImaginary() - obsSpc[j].getImaginary()*synSpc[j].getReal())
	//							/ (obsSpc[j].abs() * synSpc[j].abs());
	//						 if (sin < -1.)
	//							 sin = -1.;
	//						 else if (sin > 1.)
	//							 sin = 1.;
//							Complex obsunit = obsSpc[j].divide(obsSpc[j].abs());
//							Complex synunit = synSpc[j].divide(synSpc[j].abs());
//							Complex misfit = obsunit.divide(synunit).
							Complex c = obsSpc[j].multiply(synSpc[j].conjugate());
							Complex deltaPhi = c.divide(c.abs()).log();
							if (Math.abs(deltaPhi.getReal() / deltaPhi.getImaginary()) > 1e-5)
								throw new IllegalArgumentException("Imaginary part of deltaPhi > 0 (" 
							+ -1*deltaPhi.getImaginary() + ", " + -1*deltaPhi.getReal() + ")");
							phaseEnvelope[0][i][j] = deltaPhi.getImaginary() * 180 / Math.PI;
						}
						else
							phaseEnvelope[0][i][j] = Double.NaN;
	//					if (synSpc[j].abs() != 0)
	//						phaseEnvelope[1][i][j] = obsSpc[j].abs() / synSpc[j].abs();
	//					else
	//						phaseEnvelope[1][i][j] = Double.NaN;
						phaseEnvelope[1][i][j] = (obsSpc[j].abs() - synSpc[j].abs()) * (obsSpc[j].abs() - synSpc[j].abs());
						
	//					if (i == 1200)
	//						System.out.println(obsSpc[j].abs() + " " + synSpc[j].abs() + " " + obsSpc[j].getArgument() *180/ Math.PI + " " + synSpc[j].getArgument() *180/ Math.PI+ " " + phaseEnvelope[0][i][j]);
						phaseEnvelope[2][i][j] = j * frequencyIncrement;
						
						if (synSpc[j].abs() > spcMax)
							spcMax = synSpc[j].abs();
						obsSpcmemo[i][j] = obsSpc[j].abs();
						synSpcmemo[i][j] = synSpc[j].abs();
					}
				}
				
				double wSum = 0;
				for (int i = 0; i < n; i ++) {
					for (int j = 0; j < nnp; j++) {
						double w = Math.log(1. + synSpcmemo[i][j]) / Math.log(1. + spcMax);
						wSum += w;
					}
				}
	//			wSum /= nnp*n;
	//			System.out.println(wSum);
				
				for (int i = 0; i < n; i++) {
					double obsSpcAbsInt = 0;
					for (int j = 0; j < nnp; j++) {
	//					if (obsSpcmemo[i][j] < spcMax / 40) {
	//						phaseEnvelope[1][i][j] = Double.NaN;
	//						phaseEnvelope[0][i][j] = Double.NaN;
	//					}
						obsSpcAbsInt += obsSpcmemo[i][j];
					}
	//				double spcFreqMax = Double.MIN_VALUE;
	//				for (int j = 0; j < nnp; j++) {
	//					if (spcFreqMax < obsSpcmemo[i][j])
	//						spcFreqMax = obsSpcmemo[i][j];
	//				}
					for (int j = 0; j < nnp; j++) {
						double w = Math.log(1. + synSpcmemo[i][j]) / Math.log(1. + spcMax) / wSum; //Eq. (12) of Fichtner et al. (2008) modified using spcFreqMax instead of spcMax
	//					if (w == 0) {
	//						phaseEnvelope[0][i][j] = Double.NaN;
	//						phaseEnvelope[1][i][j] = Double.NaN;
	//					}
						if (w > 0)
							phaseEnvelope[1][i][j] = Math.sqrt(phaseEnvelope[1][i][j] / (obsSpcAbsInt * obsSpcAbsInt)) / w;
						else
							phaseEnvelope[1][i][j] = Double.MAX_VALUE;
						
						phaseEnvelope[0][i][j] /=  w;
						
						if (phaseEnvelope[1][i][j] > 5e3)
							phaseEnvelope[1][i][j] = 5e3;
						if (phaseEnvelope[0][i][j] > 5e3)
							phaseEnvelope[0][i][j] = 5e3;
						
	//					System.out.println(w + " " + phaseEnvelope[0][i][j]);
						// TO DO: normalize each time points by the frequency-integrated weighting factor !!
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return phaseEnvelope;
	}
	
	private double[][] freqIntegrated(double[][][] phaseEnvelope, double maxFreq) {
		double[][] frequInt = new double[][] {new double [phaseEnvelope[0].length]
				, new double [phaseEnvelope[0].length]};
		double frequencyIncrement = phaseEnvelope[2][0][1] - phaseEnvelope[2][0][0];
		int nmax = (int) (maxFreq / frequencyIncrement) + 1;
		if (nmax > phaseEnvelope[0][0].length) {
			throw new IllegalArgumentException("maximum frequency for integration is greater than the maximum frequency in the synthetics");
		}
		int n0;
		int n1;
		for (int i = 0; i < phaseEnvelope[0].length; i++) {
			n0 = 0;
//			n1 = 0;
			for (int j = 0; j < nmax; j++) {
				if (!Double.isNaN(phaseEnvelope[0][i][j])) {
					frequInt[0][i] += Math.abs(phaseEnvelope[0][i][j]);
					n0++;
				}
			}
			for (int j = 0; j < phaseEnvelope[0][0].length; j++) {
				if (!Double.isNaN(phaseEnvelope[1][i][j])) {
					frequInt[1][i] += phaseEnvelope[1][i][j];
//					n1++;
				}
			}
			frequInt[0][i] /= n0;
//			frequInt[1][i] /= n1;
		}
		return frequInt;
	}
	
	private double[][] selectTimewindows(double[][] freqIntegratedPE, double mergeTolerance, double minLength) {
		List<Integer> iselect = new ArrayList<>();
		for (int i = 0; i < freqIntegratedPE[0].length; i++) {
			if (freqIntegratedPE[0][i] <= phaseMisfit
					&& freqIntegratedPE[1][i] <= spcAmpMisfit)
				iselect.add(i);
		}
		if (iselect.size() <= 1)
			return null;
		else {
			List<int[]> intervals = new ArrayList<>();
			int i = iselect.get(0);
			int ibefore = i;
			int j = 0;
			while (j < iselect.size() - 1) {
				if (iselect.get(j+1) == i + 1)
					i++;
				else {
					intervals.add(new int[] {ibefore, i});
					i = iselect.get(j+1);
					ibefore = i;
				}
				j++;
			}
			intervals.add(new int[] {ibefore, i});
			
			List<int[]> mergedIntervals = new ArrayList<>();
			int i0 = intervals.get(0)[0];
			int i1 = intervals.get(0)[1];
			if (intervals.size() == 1) {
				mergedIntervals.add(new int[] {i0, i1});
			}
			else {
				i = 0;
				while (i < intervals.size() - 1) {
					while (i < intervals.size() - 1 && intervals.get(i+1)[0] - intervals.get(i)[1] <= mergeTolerance) {
						i++;
					}
					i1 = intervals.get(i)[1];
					mergedIntervals.add(new int[] {i0, i1});
					if (i < intervals.size() - 1) {
						i++;
						i0 = intervals.get(i)[0];
						i1 = intervals.get(i)[1];
					}
				}
				if (i0 != mergedIntervals.get(mergedIntervals.size() - 1)[0])
					mergedIntervals.add(new int[] {i0, i1});
			}
			
			for (Iterator<int[]> iterator = mergedIntervals.iterator(); iterator.hasNext(); ) {
				int[] interval = iterator.next();
				if (interval[1] - interval[0] < minLength) {
					iterator.remove();
				}
			}
			
			double[][] res = new double[mergedIntervals.size()][];
			IntStream.range(0, mergedIntervals.size()).forEach(index -> res[index] = new double[] {mergedIntervals.get(index)[0] * dt*samplingHz
					, mergedIntervals.get(index)[1] * dt*samplingHz});
			return res;
		}
	}
	
	private double[][] filterOnTimeserieAmplitude(double[][] timewindows, SACData obsdata, SACData syndata, double threshold) {
		Trace obstrace = obsdata.createTrace().cutWindow(0, 4000.);
		Trace syntrace = syndata.createTrace().cutWindow(0, 4000);
		double maxobs = obstrace.getMaxValue();
		double maxsyn = obstrace.getMaxValue();
		double minAllowedAmplitude = threshold * Math.max(maxobs, maxsyn);
		List<double[]> filteredTimewindowList = new ArrayList<>();
		for (double[] tw : timewindows) {
			Trace syn = syntrace.cutWindow(tw[0], tw[1]);
			Trace obs = obstrace.cutWindow(tw[0], tw[1]);
			maxobs = syn.getMaxValue();
			maxsyn = obs.getMaxValue();
			RealVector synVector = syn.getYVector();
			RealVector obsVector = obs.getYVector().getSubVector(0, synVector.getDimension());
			double variance = Math.sqrt(obsVector.dotProduct(synVector)) / obsVector.getNorm();
			if (maxsyn / maxobs > 2.5  || maxsyn / maxobs < 0.4 || variance > this.variance)
				continue;
			if (Math.min(maxsyn, maxobs) >= minAllowedAmplitude)
				filteredTimewindowList.add(tw);
		}
		double[][] filteredTimewindow = new double[filteredTimewindowList.size()][]; 
		return filteredTimewindowList.toArray(filteredTimewindow);
	}
	
	private double[] dominantFrequencySwave(SACFileName sfn, double beforeArrival, double afterArrival) throws IllegalStateException {
		double[] frequencySpcAmplitude = new double[2];
		try {
			SACData sd = sfn.read();
			double distance = sd.getEventLocation().getEpicentralDistance(sd.getStation().getPosition())
				* 180 / Math.PI;
			double eventR = sd.getEventLocation().getR();
			Phase[] phases = null;
			if (sfn.getComponent() == SACComponent.T)
				phases = new Phase[] {Phase.S, Phase.create("Sdiff", false), Phase.s};
			else if (sfn.getComponent() == SACComponent.R || sfn.getComponent() == SACComponent.Z)
				phases = new Phase[] {Phase.P, Phase.create("Pdiff", false), Phase.p};
			else
				throw new IllegalArgumentException("Error: SACComponent is neither R, T, or Z " + sfn);
			Set<TauPPhase> taupPhaseSet = TauPTimeReader.getTauPPhase(eventR, distance, phases);
			if (taupPhaseSet.size() > 1)
				System.err.println("Warning: S-wave multiplications " + sfn);
			if (taupPhaseSet.size() == 0) {
				throw new IllegalStateException("Empty arrivals " + sfn);
			}
			double t = taupPhaseSet.stream().findFirst().get().getTravelTime();
			RealVector tmp = sd.createTrace().cutWindow(t-beforeArrival, t+afterArrival).getYVector();
			tmp = taper(tmp, 0.05);
			int n = tmp.getDimension();
			int npow2 = Integer.highestOneBit(n) < n ? 2 * Integer.highestOneBit(n) : Integer.highestOneBit(n);
			npow2 = npow2 < np+1 ? 2 * npow2 : npow2;
			double frequencyIncrement = (tlen*samplingHz)/npow2 * 1./tlen;
			int nnp = (int) (np * npow2 / (tlen*samplingHz));
			double[] data = Arrays.copyOf(tmp.toArray(), npow2);
			data = rmean(data);
			ArrayRealVector spcAbs = new ArrayRealVector( Stream.of(fft.transform(data, TransformType.FORWARD))
					.map(z -> z.abs()).toArray(Double[]::new) );
			frequencySpcAmplitude[0] = spcAbs.getSubVector(0, nnp).getMaxIndex()
			* frequencyIncrement;
			frequencySpcAmplitude[1] = spcAbs.getSubVector(0, nnp).getMaxValue();
//			for (int i = 0; i < spcAbs.getDimension(); i++)
//				System.out.println(i + " " + i*1./tlen + " " + spcAbs.getEntry(i));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return frequencySpcAmplitude;
	}
	
	private TauPPhase[][] findPhases(double[][] timewindows, double distance, double eventR) {
		TauPPhase[][] foundPhases = new TauPPhase[timewindows.length][];
		String[] includePhaseNames = new String[] {};
		if (components.contains(SACComponent.R) || components.contains(SACComponent.Z))
			includePhaseNames = new String[] {"S", "SS", "SSS", "SSS", "SSSS", "SSSSS", "ScS", "ScSScS", "ScSScSScS", "ScSScSScSScS", "sS", "sSS", "sSSS"
				, "sScS", "sScSScS", "sScSScSScS", "Sdiff", "sSdiff"
				, "P", "PP", "PPP", "PPPP", "PcP", "PcPPcP", "PcPPcPPcP", "pP", "pPP", "pPPP", "pPPPP", "pPcP", "pPcPPcP", "pPcPPcPPcP", "Pdiff", "pPdiff"};
		else
			includePhaseNames = new String[] {"S", "SS", "SSS", "SSS", "SSSS", "SSSSS", "ScS", "ScSScS", "ScSScSScS", "ScSScSScSScS", "sS", "sSS", "sSSS"
				, "sScS", "sScSScS", "sScSScSScS", "Sdiff", "sSdiff"};
		Phase[] includePhases = new Phase[includePhaseNames.length];
		for (int i = 0; i < includePhases.length; i++)
			includePhases[i] = Phase.create(includePhaseNames[i], false);
		Set<TauPPhase> taupPhaseSet = TauPTimeReader.getTauPPhase(eventR, distance, includePhases);
		AtomicInteger i = new AtomicInteger();
		for (double[] tw : timewindows) {
			Set<TauPPhase> tmpPhases = taupPhaseSet.stream().filter(phase -> phase.getTravelTime() <= tw[1] 
					&& phase.getTravelTime() >= tw[0])
					.collect(Collectors.toSet());
			Map<Phase, TauPPhase> map = new HashMap<>();
			for (TauPPhase ph : tmpPhases) {
				if (map.containsKey(ph.getPhaseName())) {
					if (map.get(ph.getPhaseName()).getTravelTime() < ph.getTravelTime())
						map.put(ph.getPhaseName(), ph);
				}
				else
					map.put(ph.getPhaseName(), ph);
			}
			
			Comparator<TauPPhase> comparator = new Comparator<TauPPhase>() {
			    @Override
			    public int compare(TauPPhase p1, TauPPhase p2) {
			        return (int) Math.signum(p1.getTravelTime() - p2.getTravelTime());
			    }
			};
			List<TauPPhase> list = map.values().stream().collect(Collectors.toList());
			Collections.sort(list, comparator);
			
			foundPhases[i.get()] = new TauPPhase[list.size()];
			foundPhases[i.get()] = list.toArray(foundPhases[i.get()]);
			i.incrementAndGet();
		}
		return foundPhases;
	}
	
	private RealVector taper(RealVector v, double dt) {
		int n = v.getDimension();
		RealVector vtap = v.copy();
		for (int i = 0; i < n;i++) {
			vtap.setEntry(i, vtap.getEntry(i) * gaborDist(n/2*dt/3, n/2*dt, i * dt));
		}
		return vtap;
	}
	
	private static JFreeChart createChart(XYDataset dataset, double lowerbound, double upperbound, String title, double dx, double dy, boolean phaseMisfit) {
//		System.out.println(dataset.getXValue(1, dataset.getItemCount(1)) + " " + dx);
		
		NumberAxis xAxis = new NumberAxis("Time from origin time (s)");
        NumberAxis yAxis = new NumberAxis("Frequency (Hz)");
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
        XYBlockRenderer r = new XYBlockRenderer();
        PaintScale ps = null;
        if (phaseMisfit)
        	ps = new SpectrumPaintScale(lowerbound, upperbound);
        else
        	ps = new RatioPaintScale(upperbound);
        r.setPaintScale(ps);
        r.setBlockHeight(dy);
        r.setBlockWidth(dx);
        plot.setRenderer(r);
        JFreeChart chart = new JFreeChart(title,
            JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        NumberAxis scaleAxis = new NumberAxis("Scale");
        scaleAxis.setAxisLinePaint(Color.white);
        scaleAxis.setTickMarkPaint(Color.white);
        PaintScaleLegend legend = new PaintScaleLegend(ps, scaleAxis);
        legend.setSubdivisionCount(128);
        legend.setAxisLocation(AxisLocation.TOP_OR_RIGHT);
        legend.setPadding(new RectangleInsets(10, 10, 10, 10));
        legend.setStripWidth(20);
        legend.setPosition(RectangleEdge.RIGHT);
        legend.setBackgroundPaint(Color.WHITE);
        chart.addSubtitle(legend);
        chart.setBackgroundPaint(Color.white);
        return chart;
	}
	
	private static XYZDataset createDataset(double[][] data, int xresample, int yresample, double dx, double dy) 
			throws IllegalArgumentException {
		if (Integer.highestOneBit(yresample) != yresample)
			throw new IllegalArgumentException("Error: y resampling factor should be a power of 2");
		double[][] dataResample = new double[data.length/xresample][];
		for (int i = 0; i < dataResample.length; i++) {
			dataResample[i] = new double[data[0].length/yresample];
			for (int j = 0; j < dataResample[0].length; j++) {
				dataResample[i][j] = data[i*xresample][j*yresample];
			}
		}
        XYZArrayDataset dataset = new XYZArrayDataset(dataResample, dx*xresample, dy*yresample);
        return dataset;
    }
	
	private XYDataset createDataset(Trace syn, Trace obs) {
		XYSeries synseries = new XYSeries("Synthetics");
		XYSeries obsseries = new XYSeries("Observed");
		for (int i = 0; i < 2000; i++) {
			  synseries.add(i, syn.getYAt(i * (int) samplingHz));
			  obsseries.add(i, obs.getYAt(i * (int) samplingHz));
		}
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(obsseries);
		dataset.addSeries(synseries);
		
		return dataset;
	}
	
   private JFreeChart createChart(XYDataset dataset, double[][] timewindows) {
      JFreeChart chart = ChartFactory.createXYLineChart("Time series", "Time from origin time (s)", "Velocity (m/s)", dataset
    		  , PlotOrientation.VERTICAL, true, true, false);
      final XYPlot plot = chart.getXYPlot();
      XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
      renderer.setSeriesPaint(0, Color.RED);
      renderer.setSeriesPaint(1, Color.BLACK);
      renderer.setSeriesStroke(0, new BasicStroke(1.0f));
      renderer.setSeriesStroke(1, new BasicStroke(1.0f));
      renderer.setBaseShapesVisible(false);
      plot.setRenderer(renderer);
      plot.setDomainGridlinePaint(Color.WHITE);
      plot.setRangeGridlinePaint(Color.WHITE);
      plot.setBackgroundPaint(Color.WHITE);
      
      double h = chart.getXYPlot().getRangeAxis().getUpperBound() * .5;
      for (double[] tw : timewindows) {
	      Rectangle2D.Double rect = new Rectangle2D.Double(tw[0], -h, tw[1]-tw[0], 2*h);
	      plot.addAnnotation(new XYShapeAnnotation(rect, new BasicStroke(1.0f), Color.BLUE));
      }
      return chart;
   }
   
   private JFreeChart createChart(XYDataset timeseries, double[][] freqIntegratedPE, double[][] timewindows, TauPPhase[][] phases, String title) {
	   Font helvetica12 = new Font("Helvetica", Font.PLAIN, 12);
       Color[] colors = new Color[] {Color.BLUE, Color.GREEN.darker(), Color.GRAY, Color.BLACK, Color.RED};
	   
       final XYItemRenderer renderer1 = new StandardXYItemRenderer();
       renderer1.setSeriesPaint(0, Color.RED);
       renderer1.setSeriesPaint(1, Color.BLACK);
       renderer1.setSeriesStroke(0, new BasicStroke(1.0f));
       renderer1.setSeriesStroke(1, new BasicStroke(1.0f));
       final NumberAxis rangeAxis1 = new NumberAxis("Velocity (m/s)");
       rangeAxis1.setTickLabelFont(helvetica12);
       final XYPlot subplot1 = new XYPlot(timeseries, null, rangeAxis1, renderer1);
       subplot1.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
       
       if (timewindows != null) {
	       double h = subplot1.getRangeAxis().getUpperBound() * .5;
	       for (int i = 0; i < timewindows.length; i++) {
	 	      Rectangle2D.Double rect = new Rectangle2D.Double(timewindows[i][0], -h, timewindows[i][1]-timewindows[i][0], 2*h);
	 	      subplot1.addAnnotation(new XYShapeAnnotation(rect, new BasicStroke(1.0f), Color.BLUE));
	 	      for (int j = 0; j < phases[i].length; j++) {
	 	    	  XYTextAnnotation annotation = new XYTextAnnotation(phases[i][j].getPhaseName().getEXPANDED_NAME(), timewindows[i][0], h*1.05 + j*h/6.);
	 	    	  annotation.setPaint(colors[j % colors.length]);
	 	    	  annotation.setFont(helvetica12);
	 	    	  annotation.setTextAnchor(TextAnchor.BOTTOM_LEFT);
	 	    	  subplot1.addAnnotation(annotation);
	 	      }
	       }
       }
       
       XYSeries ampMisfit = new XYSeries("Amplitude misfit");
       XYSeries phaseMisfit = new XYSeries("Phase misfit");
		for (int i = 0; i < freqIntegratedPE[0].length; i++) {
			ampMisfit.add(i, freqIntegratedPE[1][i]);
			phaseMisfit.add(i, freqIntegratedPE[0][i]);
		}
		XYSeriesCollection datasetPhase = new XYSeriesCollection();
		XYSeriesCollection datasetAmp = new XYSeriesCollection();
		datasetPhase.addSeries(phaseMisfit);
		datasetAmp.addSeries(ampMisfit);
       
       // create subplot 2...
       
       final XYPlot subplot2 = new XYPlot();
       subplot2.setDataset(0, datasetAmp);
       final XYItemRenderer rendererAmp = new StandardXYItemRenderer();
       rendererAmp.setSeriesPaint(0, Color.BLACK);
       rendererAmp.setSeriesStroke(0, new BasicStroke(1.0f));
       final NumberAxis rangeAxis2 = new NumberAxis("Spectrum amplitude misfit");
       rangeAxis2.setAutoRangeIncludesZero(false);
       rangeAxis2.setTickLabelFont(helvetica12);
       subplot2.setRangeAxis(0, rangeAxis2);
       subplot2.setRenderer(0, rendererAmp);
       subplot2.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
       subplot2.mapDatasetToRangeAxis(0, 0);
       double upperbound = rangeAxis2.getUpperBound();
       double lowerbound = rangeAxis2.getLowerBound();
       
       
       // add secondary axis
       subplot2.setDataset(1, datasetPhase);
       final XYItemRenderer rendererPhase = new StandardXYItemRenderer();
       rendererPhase.setSeriesPaint(0, Color.RED);
       rendererPhase.setSeriesStroke(0, new BasicStroke(1.0f));
       subplot2.setRenderer(1, rendererPhase);
       final NumberAxis axis2 = new NumberAxis("Phase misfit");
//       axis2.setAutoRangeIncludesZero(false);
//       System.out.println(subplot2.getRangeAxis(0).getUpperBound() + " " +upperbound  +" " + this.phaseMisfit / this.spcAmpMisfit);
       axis2.setRange(lowerbound * this.phaseMisfit / this.spcAmpMisfit, upperbound * this.phaseMisfit / this.spcAmpMisfit);
       axis2.setUpperMargin(0);
       axis2.setAxisLinePaint(Color.RED);
       axis2.setLabelPaint(Color.RED);
       axis2.setTickMarkPaint(Color.RED);
       axis2.setTickLabelPaint(Color.RED);
       axis2.setTickLabelFont(helvetica12);
       subplot2.setRangeAxis(1, axis2);
       subplot2.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
       subplot2.mapDatasetToRangeAxis(1, 1);
       
       // parent plot...
       final NumberAxis domainAxis = new NumberAxis("Time from origin time (s)");
       domainAxis.setTickLabelFont(helvetica12);
       final CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
       plot.setGap(10.0);
       
       BasicStroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {2.0f, 6.0f}, 0.0f);
       subplot2.addAnnotation(new XYLineAnnotation(0, spcAmpMisfit, 2000*(1+domainAxis.getUpperMargin()), spcAmpMisfit, stroke, Color.GREEN.darker()));
       
       if (phases != null) {
	       for (TauPPhase[] phs : phases) {
	    	   int i = 0;
	    	   for (TauPPhase ph : phs) {
	    		   XYLineAnnotation verticalLine = new XYLineAnnotation(ph.getTravelTime(), upperbound, ph.getTravelTime(), lowerbound
	    				   , new BasicStroke(1.5f), colors[i % colors.length]);
	    		   subplot2.addAnnotation(verticalLine);
	    		   i++;
	    	   }
	       }
       }
       
       // add the subplots...
       plot.add(subplot1, 1);
       plot.add(subplot2, 1);
       plot.setOrientation(PlotOrientation.VERTICAL);

       // return a new chart containing the overlaid plot...
       return new JFreeChart(title,
                             JFreeChart.DEFAULT_TITLE_FONT, plot, true);
   }
   
   private void showSeriesAndSpectra(Trace obs, Trace syn, double[][] freqIntegratedPE, double[][] timewindows, TauPPhase[][] phases, String title) {
   	  XYDataset timeseries = createDataset(syn, obs);
      JFreeChart chart = createChart(timeseries, freqIntegratedPE, timewindows, phases, title); 
      ChartPanel chartPanel = new ChartPanel(chart);
      chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 370 ) );         
      chartPanel.setMouseZoomable( true , false );         
      JFrame f = new JFrame("Time series and Spectra");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.add(chartPanel);
      f.pack();
      f.setLocationRelativeTo(null);
      f.setVisible(true);
   }
   
   private void showTimeSeries(Trace obs, Trace syn, double[][] timewindows) {
	  XYDataset dataset = createDataset(syn, obs);         
      JFreeChart chart = createChart(dataset, timewindows);         
      ChartPanel chartPanel = new ChartPanel(chart);
      chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 370 ) );         
      chartPanel.setMouseZoomable( true , false );         
      JFrame f = new JFrame("Time series");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.add(chartPanel);
      f.pack();
      f.setLocationRelativeTo(null);
      f.setVisible(true);
   }
   
	private void showPhaseMisfit(double[][] xyphase, String title, double frequencyIncrement) {
		show("phase misfit", title, xyphase, -90, 90, frequencyIncrement, true);
	}
	
	private void showRatio(double[][] xyratio, String title, double frequencyIncrement) {
		show("amplitude ratio", title, xyratio, 1/3.5, 3.5, frequencyIncrement, false);
	}
	
	private void show(String titleFrame, String titleFig, double[][] xydata, double lowerbound, double upperbound, double frequencyIncrement, boolean phaseMisfit) {
		JFrame f = new JFrame(titleFrame);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ChartPanel chartPanel = new ChartPanel(createChart(createDataset(xydata, 10, 1, 1., frequencyIncrement)
        		, lowerbound, upperbound, titleFig, 10., frequencyIncrement, phaseMisfit)) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(640, 480);
            }
        };
        chartPanel.setMouseZoomable(true, false);
        f.add(chartPanel);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
	}
	
	private double[] rmean(double[] data) {
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
	
	protected double[] gaborWindow(double s, int n, double dt) {
//		double a = 1. / Math.pow(Math.PI * s, 0.25);
		double a = 1.;
		double b = -1. / (2 * s * s);
		if (n % 2 == 1)
			return IntStream.range(-(n-1)/2, (n-1)/2+1).mapToDouble(i -> a*Math.exp(b * i * dt * i * dt)).toArray();
		else
			return IntStream.range(-n/2-1, n/2).mapToDouble(i -> a*Math.exp(b * i * dt * i * dt)).toArray();
	}
	
	protected double gaborDist(double s, double m, double t) {
//		double a = 1. / Math.pow(Math.PI * s, 0.25);
		double a = 1.;
		double b = -1. / (2 * s * s);
		return a*Math.exp(b * (t-m) * (t-m));
	}
	
	private Set<SACComponent> components;
	
	private Path synPath;
	
	private Path obsPath;
	
	private Path outputPath;
	
//	private double ratio;

	private double spcAmpMisfit;
	
	private double phaseMisfit;
	
	private double samplingHz;
	
	private double tlen;
	
	private double dt;
	
	private double maxFrequency;
	
	private double sigmaFactor;
	
	private double threshold;
	
	private double variance;
	
	private int np;
	
	private boolean convolute;
	
	private boolean show; 
	
	private Set<TimewindowInformation> timewindowSet;
	
	protected final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
	
	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}
	
	 private static class XYZArrayDataset extends AbstractXYZDataset {
	      double[][] data;
	      int rowCount = 0;
	      int columnCount = 0;
	      double dx;
	      double dy;
	      
	      XYZArrayDataset(double[][] data, double dx, double dy){
	         this.data = data;
	         rowCount = data.length;
	         columnCount = data[0].length;
	         this.dx = dx;
	         this.dy = dy;
	      }
	      public int getSeriesCount(){
	         return 1;
	      }
	      public Comparable getSeriesKey(int series){
	         return "serie";
	      }
	      public int getItemCount(int series){
	         return rowCount*columnCount;
	      }
	      public double getXValue(int series,int item){
	         return (int)(item/columnCount) * dx;
	      }
	      public double getYValue(int series,int item){
	         return (item % columnCount) * dy;
	      }
	      public double getZValue(int series,int item){
	         return data[(int)(item/columnCount)][item % columnCount];
	      }
	      public Number getX(int series,int item){
	         return new Double((int)(item/columnCount)) * dx;
	      }
	      public Number getY(int series,int item){
	         return new Double(item % columnCount) * dy;
	      }
	      public Number getZ(int series,int item){
	         return new Double(data[(int)(item/columnCount)][item % columnCount]);
	      }
	   }
	
	private static class SpectrumPaintScale implements PaintScale {

        private final double lowerBound;
        private final double upperBound;

        public SpectrumPaintScale(double lowerBound, double upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        @Override
        public double getLowerBound() {
            return lowerBound;
        }

        @Override
        public double getUpperBound() {
            return upperBound;
        }

        @Override
        public Paint getPaint(double value) {
//            float scaledValue = (float) (value / (getUpperBound() - getLowerBound()));
//            float scaledH = H1 + scaledValue * (H2 - H1);
//            if (value < -75 && ! Double.isNaN(value))
//            	return Color.BLUE;
//            else if (value >= -75 && value < -60)
//            	return Color.CYAN.darker();
//            else if (value >= -60 && value < -45)
//            	return Color.GREEN.darker();
//            else if (value >= -45 && value < -30)
//            	return Color.GREEN;
//            else if (value >= -30 && value < -15)
//            	return Color.lightGray;
//            else if (value >= -15 && value <= 15)
//            	return Color.WHITE;
//            else if (value > 15 && value <= 30)
//            	return Color.YELLOW;
//            else if (value > 30 && value <= 45)
//            	return Color.PINK;
//            else if (value > 45 && value <= 60)
//            	return Color.ORANGE;
//            else if (value > 60 && value <= 75)
//            	return Color.ORANGE.darker();
//            else if (value > 75 && ! Double.isNaN(value))
//            	return Color.RED;
        	if (value >= -75 && value < -10)
        		return Color.BLUE;
        	else if (value >= -10 && value <= 10)
        		return Color.WHITE;
        	else if (value > 10 && value <= 75)
        		return Color.RED;
        	else if ((value < -75 || value > 75) && !Double.isNaN(value))
        		return Color.BLACK;
            else if (Double.isNaN(value))
            	return Color.BLACK;
            else {
            	throw new IllegalArgumentException(String.valueOf(value));
            }
        }
    }
	
	private static class RatioPaintScale implements PaintScale {

        private final double lowerBound;
        private final double upperBound;

        public RatioPaintScale(double maxRatio) {
            this.lowerBound = 1. / maxRatio;
            this.upperBound = maxRatio;
        }

        @Override
        public double getLowerBound() {
            return lowerBound;
        }

        @Override
        public double getUpperBound() {
            return upperBound;
        }

        @Override
        public Paint getPaint(double value) {
//            float scaledValue = (float) ((value - getLowerBound()) / (getUpperBound() - getLowerBound()));
            if (value >= 1./3.5 && value < 1./2.5)
            	return Color.RED;
            else if (value >= 1/2.5 && value < 1/1.5)
            	return Color.YELLOW;
            else if (value >= 1/1.5 && value <= 1.5)
            	return Color.WHITE;
            else if (value > 1.5 && value <= 2.5)
            	return Color.GREEN;
            else if (value > 2.5 && value <= 3.5)
            	return Color.BLUE;
            else if (Double.isNaN(value))
            	return Color.BLACK;
            else if (value < 1./3.5 || value > 3.5)
            	return Color.BLACK;
            else {
            	throw new IllegalArgumentException(String.valueOf(value));
            }
        }
    }
	
	 private static class SpectrumPaintScaleOriginal implements PaintScale {

	        private static final float H1 = 0f;
	        private static final float H2 = 1f;
	        private final double lowerBound;
	        private final double upperBound;

	        public SpectrumPaintScaleOriginal(double lowerBound, double upperBound) {
	            this.lowerBound = lowerBound;
	            this.upperBound = upperBound;
	        }

	        @Override
	        public double getLowerBound() {
	            return lowerBound;
	        }

	        @Override
	        public double getUpperBound() {
	            return upperBound;
	        }

	        @Override
	        public Paint getPaint(double value) {
	            float scaledValue = (float) (value / (getUpperBound() - getLowerBound()));
	            float scaledH = H1 + scaledValue * (H2 - H1);
	            return Color.getHSBColor(scaledH, 1f, 1f);
	        }
	    }

}
