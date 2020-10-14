package io.github.kensuke1984.kibrary.waveformdata;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.RuntimeErrorException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.jsoup.select.Collector;

import com.google.common.util.concurrent.UncheckedExecutionException;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.MomentTensor;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTCatalog;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.FormattedSPCFile;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.util.spc.SPCBody;
import io.github.kensuke1984.kibrary.util.spc.SPCComponent;
import io.github.kensuke1984.kibrary.util.spc.SPCFile;
import io.github.kensuke1984.kibrary.util.spc.SPCType;
import io.github.kensuke1984.kibrary.util.spc.SPC_SAC;
import io.github.kensuke1984.kibrary.util.spc.ThreeDPartialMaker;
import io.github.kensuke1984.kibrary.util.spc.SPCMode;

/**
 * 
 * shfp、shbp、psvfp,psvbp から作られたSPCファイルがあるディレクトリの操作
 * 
 * fpDIR, bpDIR(未定義の場合workDir)の下に イベントごとのフォルダ（FP）、ステーションごとのフォルダ(BP)があり その下の
 * modelNameにspcfileをおく
 * 
 * イベントフォルダはeventnameで定義されたもの ステーショフォルダは0000(stationname)
 * 
 * spcfileの名前は (point name).(station or eventname).(PB or PF)...(sh or psv).spc
 * 
 * halfDurationはevent informationファイルから読み取る
 * 
 * time window informationファイルの中からtime windowを見つける。 その中に入っている震源観測点成分の組み合わせのみ計算する
 * 
 * バンドパスをかけて保存する
 * 
 * 
 * TODO station とかの書き出し
 * 
 * 例： directory/19841006/*spc directory/0000KKK/*spc
 * 
 * 摂動点の情報がない摂動点に対しては計算しない
 * 
 * <b>Assume there are no stations with the same name and different networks</b>
 * TODO
 * <p>
 * Because of DSM condition, stations can not have the same name...
 * 
 * @version 2.3.0.5
 * 
 * @author Kensuke Konishi
 */
public class PartialDatasetMaker_v2 implements Operation {

	private Set<SACComponent> components;

	/**
	 * time length (DSM parameter)
	 */
	private double tlen;

	/**
	 * step of frequency domain (DSM parameter)
	 */
	private int np;

	/**
	 * BPinfo このフォルダの直下に 0000????を置く
	 */
	private Path bpPath;
	/**
	 * FPinfo このフォルダの直下に イベントフォルダ（FP）を置く
	 */
	private Path fpPath;

	/**
	 * bp, fp フォルダの下のどこにspcファイルがあるか 直下なら何も入れない（""）
	 */
	private String modelName;

	/**
	 * タイムウインドウ情報のファイル
	 */
	private Path timewindowPath;
	/**
	 * Information file about locations of perturbation points.
	 */
	private Path perturbationPath;

	/**
	 * set of partial type for computation
	 */
	private Set<PartialType> partialTypes;

	/**
	 * bandpassの最小周波数（Hz）
	 */
	private double minFreq;

	/**
	 * bandpassの最大周波数（Hz）
	 */
	private double maxFreq;

	private int filterNp;
	
	private Properties property;
	
	private Path workPath;
	
	private Path timePartialPath;
	
	/**
	 * spcFileをコンボリューションして時系列にする時のサンプリングHz デフォルトは２０ TODOまだ触れない
	 */
	private double partialSamplingHz = 20;

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}

	/**
	 * 最後に時系列で切り出す時のサンプリングヘルツ(Hz)
	 */
	private double finalSamplingHz;

	/**
	 * structure for Q partial
	 */
	private PolynomialStructure structure;
	/**
	 * 0:none, 1:boxcar, 2:triangle.
	 */
	private int sourceTimeFunction;
	/**
	 * The folder contains source time functions.
	 */
	private Path sourceTimeFunctionPath;

	/**
	 * 一つのBackPropagationに対して、あるFPを与えた時の計算をさせるスレッドを作る
	 * 
	 * @author Kensuke
	 * 
	 */
	private class PartialComputation implements Runnable {

		private DSMOutput bp;
		private DSMOutput bp_other;
		private SPCFile fpname;
		private SPCFile fpname_other;
		private DSMOutput fp;
		private DSMOutput fp_other;
		private Station station;
		private GlobalCMTID id;
		private String mode;
		
		private void checkMode(DSMOutput bp, SPCFile fpFile) {
			if (bp.getSpcFileName().getMode().equals(SPCMode.SH) 
					&& fpFile.getMode().equals(SPCMode.SH)) {
				this.bp = bp;
				fpname = fpFile;
				bp_other = null;
				fpname_other = null;
				mode = "SH";
			}
			else if (bp.getSpcFileName().getMode().equals(SPCMode.PSV) 
					&& fpFile.getMode().equals(SPCMode.PSV)) {
				this.bp = bp;
				fpname = fpFile;
				bp_other = null;
				fpname_other = null;
				mode = "PSV";
			}
			else
				throw new RuntimeException("Mode misatch " + bp.getSpcFileName() +  " " + fpFile);
		}
		
		/**
		 * @param fp
		 * @param bpFile
		 */
		private PartialComputation(DSMOutput bp, Station station, SPCFile fpFile) {
			checkMode(bp, fpFile);
			this.station = station;
//			fpname = fpFile;
			id = new GlobalCMTID(fpFile.getSourceID());
		}
		
		private PartialComputation(DSMOutput bp_SH, DSMOutput bp_PSV, Station station, SPCFile fpFile_SH, SPCFile fpFile_PSV) {
			this.bp = bp_PSV;
			fpname = fpFile_PSV;
			bp_other = bp_SH;
			fpname_other = fpFile_SH;
			this.station = station;
			mode = "BOTH";
//			fpname = fpFile;
			id = new GlobalCMTID(fpname.getSourceID());
			if (!checkPair(bp, bp_other))
				throw new RuntimeException("SH and PSV bp files are not a pair" + bp + " " + bp_other);
		}
		
		private boolean checkPair(DSMOutput bp1, DSMOutput bp2) {
			boolean res = true;
			if (!bp1.getObserverPosition().equals(bp2.getObserverPosition()))
				res = false;
			return res;
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
		
		private Complex[] cutPartial(double[] u, TimewindowInformation timewindowInformation, double shift) {
			int cutstart = (int) ((timewindowInformation.getStartTime() - shift) * partialSamplingHz) - ext;
			// cutstartが振り切れた場合0 からにする
			if (cutstart < 0)
				return null;
			int cutend = (int) ((timewindowInformation.getEndTime() - shift) * partialSamplingHz) + ext;
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

		private SourceTimeFunction getSourceTimeFunction() {
			return sourceTimeFunction == 0 ? null : userSourceTimeFunctions.get(id);
		}
		
		private boolean shiftConvolution;
		
		@Override
		public void run() {
//			Location[] perturbationLocations = perturbationLocationSet.stream().toArray(Location[]::new);
//			double[] perturbationRs = new double[perturbationLocations.length];
//			for (int i = 0; i < perturbationRs.length; i++)
//				perturbationRs[i] = perturbationLocations[i].getR();
			
			String stationName = bp.getSourceID();
			if (!station.getPosition().toLocation(0).equals(bp.getSourceLocation()))
				throw new RuntimeException("There may be a station with the same name but other networks.");

			if (bp.tlen() != tlen || bp.np() != np)
				throw new RuntimeException("BP for " + station + " has invalid tlen or np: " + tlen + " " + bp.tlen() + " " + np + " " + bp.np());
			GlobalCMTID id = new GlobalCMTID(fpname.getSourceID());

			touchedSet.add(id);
			
			// Pickup timewindows
			Set<TimewindowInformation> timewindowList = timewindowInformation.stream()
					.filter(info -> info.getStation().getStringID().equals(stationName))
					.filter(info -> info.getGlobalCMTID().equals(id)).collect(Collectors.toSet());

			System.out.println(id + " " + timewindowList.size() + " " + stationName);
			
			// timewindow情報のないときスキップ
			if (timewindowList.isEmpty())
				return;

			// System.out.println("I am " + Thread.currentThread().getName());
			try {
				fp = fpname.read();
				if (mode.equals("BOTH"))
					fp_other = fpname_other.read();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
			if (!checkPair(bp, fp))
				throw new RuntimeException("BP and FP files are not a pair" + bp + " " + fp);
			if (mode.equals("BOTH")) {
				if (!checkPair(bp_other, fp_other))
					throw new RuntimeException("BP and FP files are not a pair" + bp_other + " " + fp_other);
			}
			
			ThreeDPartialMaker threedPartialMaker = null;
			if (mode.equals("BOTH")) {
//				System.out.println("Using both PSV and SH");
				threedPartialMaker = new ThreeDPartialMaker(fp, fp_other, bp, bp_other);
			}
			else {
				threedPartialMaker = new ThreeDPartialMaker(fp, bp);
			}
			threedPartialMaker.setSourceTimeFunction(getSourceTimeFunction());
			
			if (structure != null)
				threedPartialMaker.setStructure(structure);
			
////			if (bp.getSourceLocation().getLongitude() == fp.getObserverPosition().getLongitude()) {
//			if (fp.getObserverPosition().getLongitude() == 14.5) {
////				Path path = Paths.get(fp.getObserverPosition().getLongitude() + "_" + bp.getSourceLocation().getLongitude() 
////						+ "." + fp.getSourceID() + ".txt");
//				Path path = Paths.get(fp.getObserverPosition().getLongitude() + "_" + fp.getBodyR()[0] 
//						+ "." + ".txt");
//				try {
//					Files.deleteIfExists(path);
//					Files.createFile(path);
//					int lsmooth = (int) (0.5 * 3276.8 * 20. / 512.);
//					int j = Integer.highestOneBit(lsmooth);
//					if (j < lsmooth)
//						j *= 2;
//					lsmooth = j;
//					SpcBody fpBody = fp.getSpcBodyList().get(0);
//					SpcBody bpBody = bp.getSpcBodyList().get(0);
//					
//					System.out.println("To time domain lmsooth=" + lsmooth);
//					fpBody.toTimeDomain(lsmooth);
//					bpBody.toTimeDomain(lsmooth);
//					
//					double[] fpserieT = fpBody.getTimeseries(SACComponent.T); //wrong should be SpcComponent no SACComponent
//					double[] bpserieT = bpBody.getTimeseries(SACComponent.T);
//					double[] fpserieR = fpBody.getTimeseries(SACComponent.R);
//					double[] bpserieR = bpBody.getTimeseries(SACComponent.R);
////					System.out.println("FP serie has npts=" + fpserie.length);
////					System.out.println("BP serie has npts=" + bpserie.length);
//					
//					for (int i = 0; i < fpserieT.length / 20; i++) {
//						Files.write(path, (i + " " + fpserieT[i * 20] + " " + bpserieT[i * 20] + " " + fpserieR[i * 20] + " " + bpserieR[i * 20] + "\n").getBytes(), StandardOpenOption.APPEND);
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
			
			// i番目の深さの偏微分波形を作る
			for (int ibody = 0, nbody = bp.nbody(); ibody < nbody; ibody++) {
//			for (int ibody = 0, nbody = perturbationRs.length; ibody < nbody; ibody++) {
				// とりあえずtransverse（２）成分についての名前
//				Location location = fp.getObserverPosition().toLocation(perturbationRs[ibody]);//fp.getObserverPosition().toLocation(fp.getBodyR()[ibody]);
				Location location = bp.getObserverPosition().toLocation(bp.getBodyR()[ibody]);
//				System.out.println(location);
				
				if (!perturbationLocationSet.contains(location))
					continue;
				
				for (PartialType type : partialTypes) {
					if (type.isTimePartial())
						continue;
					for (SACComponent component : components) {
						if (timewindowList.stream().noneMatch(info -> info.getComponent() == component))
							continue;
//						System.out.println(bp.getBodyR()[ibody] + " " + fpname);
						double[] partial = threedPartialMaker.createPartialSerial(component, ibody, type);
//						System.out.println(component + " " + type + " " + new ArrayRealVector(partial).getLInfNorm());

						timewindowList.stream().filter(info -> info.getComponent() == component).forEach(info -> {
//							System.out.println(component + " " + info.getComponent());
							Complex[] u;
//							if (!shiftConvolution)
							u = cutPartial(partial, info);
							
							u = filter.applyFilter(u);
							double[] cutU = sampleOutput(u, info);
							
							
							//DEBUG
							if (location.getR() == 5581 && location.getLongitude() == 14.5) {
								Path outpath = Paths.get("par_" + type + "_5581_14.5_" + new Phases(info.getPhases()) + "_" + component + ".txt");
								try {
								PrintWriter pwtmp = new PrintWriter(outpath.toFile());
								double t0 = info.getStartTime();
								for (int ii = 0; ii < cutU.length; ii++)
									pwtmp.println((t0 + ii / finalSamplingHz) + " " + cutU[ii]);
								pwtmp.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							//
							
							PartialID pid = new PartialID(station, id, component, finalSamplingHz, info.getStartTime(),
									cutU.length, 1 / maxFreq, 1 / minFreq, info.getPhases(), 0, sourceTimeFunction != 0, location, type,
									cutU);
//							System.out.println(pid.getPerturbationLocation());
							
							try {
								partialDataWriter.addPartialID(pid);
								System.out.print(".");
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
					}
				}
			}
		}
	}
	
private class WorkerTimePartial implements Runnable {
		
		private EventFolder eventDir;
		private GlobalCMTID id;
		
		@Override
		public void run() {
			try {
				writeLog("Running on " + id);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Path timePartialFolder = eventDir.toPath();

			if (!Files.exists(timePartialFolder)) {
				throw new RuntimeException(timePartialFolder + " does not exist...");
			}
			
			Set<SACFileName> sacnameSet;
			try {
				sacnameSet = eventDir.sacFileSet()
						.stream()
						.filter(sacname -> sacname.isTemporalPartial())
						.collect(Collectors.toSet());
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
			
//			System.out.println(sacnameSet.size());
//			sacnameSet.forEach(name -> System.out.println(name));
			
			Set<TimewindowInformation> timewindowCurrentEvent = timewindowInformation
					.stream()
					.filter(tw -> tw.getGlobalCMTID().equals(id))
					.collect(Collectors.toSet());
			
			// すべてのsacファイルに対しての処理
			for (SACFileName sacname : sacnameSet) {
				try {
					addTemporalPartial(sacname, timewindowCurrentEvent);
				} catch (ClassCastException e) {
					// 出来上がったインスタンスがOneDPartialSpectrumじゃない可能性
					System.err.println(sacname + "is not 1D partial.");
					continue;
				} catch (Exception e) {
					System.err.println(sacname + " is invalid.");
					e.printStackTrace();
					try {
						writeLog(sacname + " is invalid.");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					continue;
				}
			}
			System.out.print(".");
//			
		}
		
		private void addTemporalPartial(SACFileName sacname, Set<TimewindowInformation> timewindowCurrentEvent) throws IOException {
			Set<TimewindowInformation> tmpTws = timewindowCurrentEvent.stream()
					.filter(info -> info.getStation().getName().equals(sacname.getStationName()))
					.collect(Collectors.toSet());
			if (tmpTws.size() == 0) {
				return;
			}
			
			System.out.println(sacname + " (time partials)");
			
			SACData sacdata = sacname.read();
			Station station = sacdata.getStation();
			
			for (SACComponent component : components) {
				Set<TimewindowInformation> tw = tmpTws.stream()
						.filter(info -> info.getStation().equals(station))
						.filter(info -> info.getGlobalCMTID().equals(id))
						.filter(info -> info.getComponent().equals(component)).collect(Collectors.toSet());

				if (tw.isEmpty()) {
					tmpTws.forEach(window -> {
						System.out.println(window);
						System.out.println(window.getStation().getPosition());
					});
					System.err.println(station.getPosition());
					System.err.println("Ignoring empty timewindow " + sacname + " " + station);
					continue;
				}
				
				for (TimewindowInformation t : tw) {
					double[] filteredUt = sacdata.createTrace().getY();
					cutAndWrite(station, filteredUt, t);
				}
			}
		}
		
		/**
		 * @param u
		 *            partial waveform
		 * @param timewindowInformation
		 *            cut information
		 * @return u cut by considering sampling Hz
		 */
		private double[] sampleOutput(double[] u, TimewindowInformation timewindowInformation) {
			int cutstart = (int) (timewindowInformation.getStartTime() * partialSamplingHz);
			// 書きだすための波形
			int outnpts = (int) ((timewindowInformation.getEndTime() - timewindowInformation.getStartTime())
					* finalSamplingHz);
			double[] sampleU = new double[outnpts];
			// cutting a waveform for outputting
			Arrays.setAll(sampleU, j -> u[cutstart + j * step]);

			return sampleU;
		}
		
		private void cutAndWrite(Station station, double[] filteredUt, TimewindowInformation t) {

			double[] cutU = sampleOutput(filteredUt, t);
			Location stationLocation = new Location(station.getPosition().getLatitude(), station.getPosition().getLongitude(), Earth.EARTH_RADIUS);
			
			if (sourceTimeFunction == -1)
				System.err.println("Warning: check that the source time function used for the time partial is the same as the one used here.");
			
			PartialID PIDReceiverSide = new PartialID(station, id, t.getComponent(), finalSamplingHz, t.getStartTime(), cutU.length,
					1 / maxFreq, 1 / minFreq, t.getPhases(), 0, true, stationLocation, PartialType.TIME_RECEIVER,
					cutU);
			PartialID PIDSourceSide = new PartialID(station, id, t.getComponent(), finalSamplingHz, t.getStartTime(), cutU.length,
					1 / maxFreq, 1 / minFreq, t.getPhases(), 0, true, id.getEvent().getCmtLocation(), PartialType.TIME_SOURCE,
					cutU);
			
			try {
				if (partialTypes.contains(PartialType.TIME_RECEIVER))
					partialDataWriter.addPartialID(PIDReceiverSide);
				if (partialTypes.contains(PartialType.TIME_SOURCE))
					partialDataWriter.addPartialID(PIDSourceSide);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private WorkerTimePartial(EventFolder eventDir) {
			this.eventDir = eventDir;
			id = eventDir.getGlobalCMTID();
		};
	}

	private ButterworthFilter filter;

	/**
	 * バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
	 */
	private int ext;

	/**
	 * sacdataを何ポイントおきに取り出すか
	 */
	private int step;

	private Set<TimewindowInformation> timewindowInformation;

	private Set<GlobalCMTID> touchedSet = new HashSet<>();

	public PartialDatasetMaker_v2(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
	}
	
	private boolean backward;

	private String mode;
	
	private boolean catalogue;
	
	private double thetamin;
	private double thetamax;
	private double dtheta;
	
	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(PartialDatasetMaker_v2.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan PartialDatasetMaker");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Path of a back propagate spc folder (BPinfo)");
			pw.println("#bpPath");
			pw.println("##Path of a forward propagate spc folder (FPinfo)");
			pw.println("#fpPath");
			pw.println("##Boolean interpolate fp and bp from a catalogue (false)");
			pw.println("#catalogue");
			pw.println("##Theta- range and sampling for the BP catalog in the format: thetamin thetamax thetasampling. (1. 50. 2e-2)");
			pw.println("#thetaRange");
			pw.println("##Mode: PSV, SH, BOTH (SH)");
			pw.println("#mode");
			pw.println("##String if it is PREM spector file is in bpdir/PREM  (PREM)");
			pw.println("#modelName");
			pw.println("##Type source time function 0:none, 1:boxcar, 2:triangle, 3: asymmetric triangle (user catalog), 4: coming soon, 5: symmetric triangle (user catalog). (0)");
			pw.println("##or folder name containing *.stf if you want to your own GLOBALCMTID.stf ");
			pw.println("#sourceTimeFunction");
			pw.println("##Path of a time window file, must be set");
			pw.println("#timewindowPath timewindow.dat");
			pw.println("##PartialType[] compute types (MU)");
			pw.println("#partialTypes");
			pw.println("##double time length DSM parameter tlen, must be set");
			pw.println("#tlen 6553.6");
			pw.println("##int step of frequency domain DSM parameter np, must be set");
			pw.println("#np 1024");
			pw.println("##double minimum value of passband (0.005)");
			pw.println("#minFreq");
			pw.println("##double maximum value of passband (0.08)");
			pw.println("#maxFreq");
			pw.println("##The value of np for the filter (4)");
			pw.println("#filterNp");
			pw.println("##Filter if backward filtering is applied (false)");
			pw.println("#backward");
			pw.println("#double (20)");
			pw.println("#partialSamplingHz cant change now");
			pw.println("##double SamplingHz in output dataset (1)");
			pw.println("#finalSamplingHz");
			pw.println("##perturbationPath, must be set");
			pw.println("#perturbationPath perturbationPoint.inf");
			pw.println("##File for Qstructure (if no file, then PREM)");
			pw.println("#qinf");
			pw.println("##path of the time partials directory, must be set if PartialType containes TIME_SOURCE or TIME_RECEIVER");
			pw.println("#timePartialPath");
		}
		System.err.println(outPath + " is created.");
	}

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("bpPath"))
			property.setProperty("bpPath", "BPinfo");
		if (!property.containsKey("fpPath"))
			property.setProperty("fpPath", "FPinfo");
		if (!property.containsKey("catalogue"))
			property.setProperty("catalogue", "false");
		if (!property.containsKey("modelName"))
			property.setProperty("modelName", "PREM");
		if (!property.containsKey("maxFreq"))
			property.setProperty("maxFreq", "0.08");
		if (!property.containsKey("minFreq"))
			property.setProperty("minFreq", "0.005");
		// if (!property.containsKey("backward")) TODO allow user to change
		// property.setProperty("backward", "true");partialSamplingHz
		if (!property.containsKey("sourceTimeFunction"))
			property.setProperty("sourceTimeFunction", "0");
		if (!property.containsKey("partialTypes"))
			property.setProperty("partialTypes", "MU");
		if (!property.containsKey("partialSamplingHz"))
			property.setProperty("partialSamplingHz", "20");
		if (!property.containsKey("finalSamplingHz"))
			property.setProperty("finalSamplingHz", "1");
		if (!property.containsKey("filterNp"))
			property.setProperty("filterNp", "4");
		if (!property.containsKey("backward"))
			property.setProperty("backward", "false");
		if (!property.containsKey("mode"))
			property.setProperty("mode", "SH");
		
		// additional unused info
		property.setProperty("STFcatalog", stfcatName);
	}

	/**
	 * parameterのセット
	 */
	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");

		bpPath = getPath("bpPath");
		fpPath = getPath("fpPath");
		timewindowPath = getPath("timewindowPath");
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());

		if (property.containsKey("qinf"))
			structure = new PolynomialStructure(getPath("qinf"));
		try {
			sourceTimeFunction = Integer.parseInt(property.getProperty("sourceTimeFunction"));
		} catch (Exception e) {
			sourceTimeFunction = -1;
			sourceTimeFunctionPath = getPath("sourceTimeFunction");
		}
		modelName = property.getProperty("modelName");

		partialTypes = Arrays.stream(property.getProperty("partialTypes").split("\\s+")).map(PartialType::valueOf)
				.collect(Collectors.toSet());
		
		if (partialTypes.contains(PartialType.TIME_RECEIVER) || partialTypes.contains(PartialType.TIME_SOURCE)) {
			timePartialPath = Paths.get(property.getProperty("timePartialPath"));
			if (!Files.exists(timePartialPath))
				throw new RuntimeException("The timePartialPath: " + timePartialPath + " does not exist");
		}
		
		tlen = Double.parseDouble(property.getProperty("tlen"));
		np = Integer.parseInt(property.getProperty("np"));
		minFreq = Double.parseDouble(property.getProperty("minFreq"));
		maxFreq = Double.parseDouble(property.getProperty("maxFreq"));
		perturbationPath = getPath("perturbationPath");
		// partialSamplingHz
		// =Double.parseDouble(reader.getFirstValue("partialSamplingHz")); TODO

		finalSamplingHz = Double.parseDouble(property.getProperty("finalSamplingHz"));
		
		filterNp = Integer.parseInt(property.getProperty("filterNp"));
		
		backward = Boolean.parseBoolean(property.getProperty("backward"));
		
		mode = property.getProperty("mode").trim().toUpperCase();
		if (!(mode.equals("SH") || mode.equals("PSV") || mode.equals("BOTH")))
				throw new RuntimeException("Error: mode should be one of the following: SH, PSV, BOTH");
		System.out.println("Using mode " + mode);
		
		catalogue = Boolean.parseBoolean(property.getProperty("catalogue"));
		if (catalogue) {
			double[] tmpthetainfo = Stream.of(property.getProperty("thetaInfo").trim().split("\\s+")).mapToDouble(Double::parseDouble)
					.toArray();
			thetamin = tmpthetainfo[0];
			thetamax = tmpthetainfo[1];
			dtheta = tmpthetainfo[2];
		}
	}

	private void setLog() throws IOException {
		synchronized (PartialDatasetMaker_v2.class) {
			do {
				dateString = Utilities.getTemporaryString();
				logPath = workPath.resolve("pdm" + dateString + ".log");
			} while (Files.exists(logPath));
			Files.createFile(logPath);
		}
	}

	private void setOutput() throws IOException {

		// 書き込み準備
		Path idPath = workPath.resolve("partialID" + dateString + ".dat");
		Path datasetPath = workPath.resolve("partial" + dateString + ".dat");

		partialDataWriter = new WaveformDataWriter(idPath, datasetPath, stationSet, idSet, periodRanges,
				phases, perturbationLocationSet);
		writeLog("Creating " + idPath + " " + datasetPath);
		System.out.println("Creating " + idPath + " " + datasetPath);

	}

	// TODO
	private Set<Station> stationSet;
	private Set<GlobalCMTID> idSet;
	private double[][] periodRanges;
	private Phase[] phases;
	private Set<Location> perturbationLocationSet;

	private void readPerturbationPoints() throws IOException {
		try (Stream<String> lines = Files.lines(perturbationPath)) {
			perturbationLocationSet = lines.map(line -> line.split("\\s+"))
					.map(parts -> new Location(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
							Double.parseDouble(parts[2])))
					.collect(Collectors.toSet());
		}
		if (timePartialPath != null) {
			if (stationSet.isEmpty() || idSet.isEmpty())
				throw new RuntimeException("stationSet and idSet must be set before perturbationLocation");
			stationSet.forEach(station -> perturbationLocationSet.add(new Location(station.getPosition().getLatitude(),
					station.getPosition().getLongitude(), Earth.EARTH_RADIUS)));
			idSet.forEach(id -> perturbationLocationSet.add(id.getEvent().getCmtLocation()));
		}
	}

	private String dateString;

	private WaveformDataWriter partialDataWriter;

	private Path logPath;

	private long startTime = System.nanoTime();
	private long endTime;
	
	private boolean jointCMT;
	
	/*
	 * sort timewindows comparing stations
	 */
	private List<TimewindowInformation> sortTimewindows() {
		List<TimewindowInformation> timewindows = timewindowInformation.stream().collect(Collectors.toList());
		
		Comparator<TimewindowInformation> comparator = new Comparator<TimewindowInformation>() {
			@Override
			public int compare(TimewindowInformation o1, TimewindowInformation o2) {
				int res = o1.getStation().compareTo(o2.getStation());
				if (res != 0)
					return res;
				else {
					return o1.getGlobalCMTID().compareTo(o2.getGlobalCMTID());
				}
			}
		};
		timewindows.sort(comparator);
		
		return timewindows;
	}
	
	@Override
	public void run() throws IOException {
		setLog();
		final int N_THREADS = Runtime.getRuntime().availableProcessors();
//		final int N_THREADS = 1;
		writeLog("Running " + N_THREADS + " threads");
		writeLog("CMTcatalogue: " + GlobalCMTCatalog.getCatalogPath().toString());
		writeLog("SourceTimeFunction=" + sourceTimeFunction);
		if (sourceTimeFunction == 3 || sourceTimeFunction == 5)
			writeLog("STFcatalogue: " + stfcatName);
		setTimeWindow();
		// filter設計
		setBandPassFilter();
		// read a file for perturbation points.
		readPerturbationPoints();

		// バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
		ext = (int) (1 / minFreq * partialSamplingHz);

		// sacdataを何ポイントおきに取り出すか
		step = (int) (partialSamplingHz / finalSamplingHz);
		setOutput();
		int bpnum = 0;
		setSourceTimeFunctions();
		
		// time partials for each event
		if (timePartialPath != null) {
			ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
			Set<EventFolder> timePartialEventDirs = Utilities.eventFolderSet(timePartialPath);
			for (EventFolder eventDir : timePartialEventDirs) {
				execs.execute(new WorkerTimePartial(eventDir));
				System.out.println("Working for time partials for " + eventDir);
			}
			execs.shutdown();
			while (!execs.isTerminated()) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			partialDataWriter.flush();
			System.out.println();
		}
		
		for (Station station : stationSet) {
			Path bp0000Path = bpPath.resolve("0000" + station.toString());
			Path bpModelPath = bp0000Path.resolve(modelName);

			// Set of global cmt IDs for the station in the timewindow.
			Set<GlobalCMTID> idSet = timewindowInformation.stream()
					.filter(info -> components.contains(info.getComponent()))
					.filter(info -> info.getStation().equals(station)).map(TimewindowInformation::getGlobalCMTID)
					.collect(Collectors.toSet());

			if (idSet.isEmpty())
				continue;

			// bpModelFolder内 spectorfile
			List<SPCFile> bpFiles = null;
			List<SPCFile> bpFiles_PSV = null;
			if (mode.equals("SH"))
				bpFiles = Utilities.collectOrderedSHSpcFileName(bpModelPath);
			else if (mode.equals("PSV"))
				bpFiles = Utilities.collectOrderedPSVSpcFileName(bpModelPath);
			else if (mode.equals("BOTH")) {
				bpFiles = Utilities.collectOrderedSHSpcFileName(bpModelPath);
				bpFiles_PSV = Utilities.collectOrderedPSVSpcFileName(bpModelPath);
			}
			System.out.println(bpFiles.size() + " bpfiles are found");

			// stationに対するタイムウインドウが存在するfp内のmodelフォルダ
			Path[] fpEventPaths = null;
			List<Path[]> fpPathList = null;
			if (!jointCMT) {
				fpEventPaths = idSet.stream().map(id -> fpPath.resolve(id + "/" + modelName))
					.filter(Files::exists).toArray(Path[]::new);
			}
			else {
				fpPathList = collectFP_jointCMT(idSet);
			}
			
			int donebp = 0;
			// bpフォルダ内の各bpファイルに対して
			// create ThreadPool
			ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
			for (int i = 0; i < bpFiles.size(); i++) {
				SPCFile bpname = bpFiles.get(i);
				SPCFile bpname_PSV = null;
				if (mode.equals("BOTH"))
					bpname_PSV = bpFiles_PSV.get(i);
				
//				System.out.println(bpname + " " + bpname_PSV);
				
				System.out.println("Working for " + bpname.getName() + " " + ++donebp + "/" + bpFiles.size());
				// 摂動点の名前
				DSMOutput bp = bpname.read();
				DSMOutput bp_PSV = null;
				if (mode.equals("BOTH"))
					bp_PSV = bpname_PSV.read();
				String pointName = bp.getObserverID();

				// timewindowの存在するfpdirに対して
				// ｂｐファイルに対する全てのfpファイルを
//				if (!jointCMT)
//				{
					for (Path fpEventPath : fpEventPaths) {
						String eventName = fpEventPath.getParent().getFileName().toString();
						SPCFile fpfile = new FormattedSPCFile(
								fpEventPath.resolve(pointName + "." + eventName + ".PF..." + bpname.getMode() + ".spc"));
						SPCFile fpfile_PSV = null;
						if (mode.equals("BOTH")) {
							fpfile_PSV = new FormattedSPCFile(
									fpEventPath.resolve(pointName + "." + eventName + ".PF..." + "PSV" + ".spc"));
							if (!fpfile_PSV.exists()) {
								System.err.println("Fp file not found " + fpfile_PSV);
								continue;
							}
						}
						if (!fpfile.exists()) {
							System.err.println("Fp file not found " + fpfile);
							continue;
						}
						
						PartialComputation pc = null;
						if (mode.equals("BOTH"))
							pc = new PartialComputation(bp, bp_PSV, station, fpfile, fpfile_PSV);
						else
							pc = new PartialComputation(bp, station, fpfile);
						
						execs.execute(pc);
					}
			}
			execs.shutdown();
			while (!execs.isTerminated()) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			partialDataWriter.flush();
			System.out.println();
			writeLog(bpnum++ + "th " + bp0000Path + " was done ");
		}
		terminate();
	}
	
	private List<Path[]> collectFP_jointCMT(Set<GlobalCMTID> idSet) {
		List<Path[]> paths = new ArrayList<>();
		
		for (GlobalCMTID id : idSet) {
			Path[] tmpPaths = new Path[6];
			for (int i = 0; i < 6; i++)
				tmpPaths[i] = fpPath.resolve(id + "_mt" + i + "/" + modelName);
			paths.add(tmpPaths);
		}
		
		return paths;
	}
	
	private Map<GlobalCMTID, SourceTimeFunction> userSourceTimeFunctions;
	
	private final String stfcatName = "astf_cc_ampratio_ca.catalog"; //LSTF1 ASTF1 ASTF2
	private final List<String> stfcat = readSTFCatalogue(stfcatName);
	
	private void setSourceTimeFunctions() throws IOException {
		if (sourceTimeFunction == 0)
			return;
		if (sourceTimeFunction == -1) {
			readSourceTimeFunctions();
			return;
		}
		userSourceTimeFunctions = new HashMap<>();
		idSet.forEach(id -> {
			double halfDuration = id.getEvent().getHalfDuration();
			SourceTimeFunction stf;
			switch (sourceTimeFunction) {
			case 1:
				stf = SourceTimeFunction.boxcarSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
				break;
			case 2:
				System.out.println("Using triangle STF");
				stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
//				System.out.println("Triangle STF " + halfDuration);
				break;
			case 3:
				if (stfcat.contains("LSTF")) {
		        	double halfDuration1 = id.getEvent().getHalfDuration();
		        	double halfDuration2 = id.getEvent().getHalfDuration();
		        	boolean found = false;
			      	for (String str : stfcat) {
			      		String[] stflist = str.split("\\s+");
			      	    GlobalCMTID eventID = new GlobalCMTID(stflist[0]);
			      	    if(id.equals(eventID)) {
			      	    	if(Integer.valueOf(stflist[3]) >= 5.) {
			      	    		halfDuration1 = Double.valueOf(stflist[1]);
			      	    		halfDuration2 = Double.valueOf(stflist[2]);
			      	    		found = true;
			      	    	}
			      	    }
			      	}
			      	if (found) {
			      		stf = SourceTimeFunction.asymmetrictriangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration1, halfDuration2);
		//	      		System.out.println(id + " Using LSTF with duration " + (halfDuration1 + halfDuration2));
			      	}
			      	else
			      		stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, id.getEvent().getHalfDuration());
				}
				else {
					boolean found = false;
					double ampCorr = 1.;
					for (String str : stfcat) {
			      		String[] ss = str.split("\\s+");
			      	    GlobalCMTID eventID = new GlobalCMTID(ss[0]);
			      	    if (id.equals(eventID)) {
			      	    	halfDuration = Double.parseDouble(ss[1]);
			      	    	ampCorr = Double.parseDouble(ss[2]);
			      	    	found = true;
			      	    	break;
			      	    }
			      	}
					if (found)
						stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration, ampCorr);
					else
						stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, id.getEvent().getHalfDuration());
				}
	            break;
			case 4:
				throw new RuntimeException("Case 4 not implemented yet");
			case 5:
//				double mw = id.getEvent().getCmt().getMw();
////				double duration = 9.60948E-05 * Math.pow(10, 0.6834 * mw);
//				double duration = 0.018084 * Math.pow(10, 0.3623 * mw);
//				halfDuration = duration / 2.;
////				System.out.println("DEBUG1: id, mw, half-duration = " + id + " " + mw + " " + halfDuration);
//				return SourceTimeFunction.triangleSourceTimeFunction(np, tlen, samplingHz, halfDuration);
				halfDuration = 0.;
				double amplitudeCorrection = 1.;
				boolean found = false;
		      	for (String str : stfcat) {
		      		String[] stflist = str.split("\\s+");
		      	    GlobalCMTID eventID = new GlobalCMTID(stflist[0].trim());
		      	    if(id.equals(eventID)) {
		      	    	halfDuration = Double.valueOf(stflist[1].trim());
		      	    	amplitudeCorrection = Double.valueOf(stflist[2].trim());
		      	    	found = true;
		      	    }
		      	}
		      	if (found)
		      		stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration, 1. / amplitudeCorrection);
		      	else
		      		stf = SourceTimeFunction.triangleSourceTimeFunction(np, tlen, partialSamplingHz, id.getEvent().getHalfDuration());
		      	break;
			default:
				throw new RuntimeException("Error: undefined source time function identifier (0: none, 1: boxcar, 2: triangle).");
			}
			userSourceTimeFunctions.put(id, stf);
		});
	}
	
	private List<String> readSTFCatalogue(String STFcatalogue) throws IOException {
//		System.out.println("STF catalogue: " +  STFcatalogue);
		return IOUtils.readLines(PartialDatasetMaker_v2.class.getClassLoader().getResourceAsStream(STFcatalogue)
					, Charset.defaultCharset());
	}

	private void readSourceTimeFunctions() throws IOException {
		userSourceTimeFunctions = idSet.stream().collect(Collectors.toMap(id -> id, id -> {
			try {
				Path sourceTimeFunctionPath = this.sourceTimeFunctionPath.resolve(id + ".stf");
				return SourceTimeFunction.readSourceTimeFunction(sourceTimeFunctionPath);
			} catch (Exception e) {
				throw new RuntimeException("Source time function file for " + id + " is broken.");
			}
		}));
	}

	/**
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		PartialDatasetMaker_v2 pdm = new PartialDatasetMaker_v2(Property.parse(args));
		long startTime = System.nanoTime();

		System.err.println(PartialDatasetMaker_v2.class.getName() + " is going..");
		pdm.run();
		System.err.println(PartialDatasetMaker_v2.class.getName() + " finished in "
				+ Utilities.toTimeString(System.nanoTime() - startTime));
	}

	private void terminate() throws IOException {
		partialDataWriter.close();
		endTime = System.nanoTime();
		long nanoSeconds = endTime - startTime;
		String endLine = "Everything is done in " + Utilities.toTimeString(nanoSeconds) + ". Over n out! ";
		System.err.println(endLine);
		writeLog(endLine);
		writeLog(partialDataWriter.getIDPath() + " " + partialDataWriter.getDataPath() + " were created");
	}

	private synchronized void writeLog(String line) throws IOException {
		try (PrintWriter pw = new PrintWriter(
				Files.newBufferedWriter(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
			pw.print(new Date() + " : ");
			pw.println(line);
		}
	}

	private void setBandPassFilter() throws IOException {
		System.err.println("Designing filter.");
		double omegaH = maxFreq * 2 * Math.PI / partialSamplingHz;
		double omegaL = minFreq * 2 * Math.PI / partialSamplingHz;
		filter = new BandPassFilter(omegaH, omegaL, filterNp);
		filter.setBackward(backward);
//		filter.setBackward(true);
		writeLog(filter.toString());
		periodRanges = new double[][] { { 1 / maxFreq, 1 / minFreq } };
	}

	/**
	 * Reads timewindow information
	 * 
	 * @throws IOException if any
	 */
	private void setTimeWindow() throws IOException {
		// タイムウインドウの情報を読み取る。
		System.err.println("Reading timewindow information");
		timewindowInformation = TimewindowInformationFile.read(timewindowPath);
		idSet = new HashSet<>();
		stationSet = new HashSet<>();
		timewindowInformation.forEach(t -> {
			idSet.add(t.getGlobalCMTID());
			stationSet.add(t.getStation());
		});
		phases = timewindowInformation.parallelStream().map(TimewindowInformation::getPhases).flatMap(p -> Stream.of(p))
				.distinct().toArray(Phase[]::new);

		// TODO
		if (stationSet.size() != stationSet.stream().map(Station::toString).distinct().count()) {
			System.err.println("CAUTION!! Stations with same name and network but different positions detected!");
			Map<String, List<Station>> nameToStation = new HashMap<>();
			stationSet.forEach(sta -> {
				if (nameToStation.containsKey(sta.toString())) {
					List<Station> tmp = nameToStation.get(sta.toString());
					tmp.add(sta);
					nameToStation.put(sta.toString(), tmp);
				}
				else {
					List<Station> tmp = new ArrayList<>();
					tmp.add(sta);
					nameToStation.put(sta.toString(), tmp);
				}
			});
			nameToStation.forEach((name, sta) -> {
				if (sta.size() > 1) {
					sta.stream().forEach(s -> System.out.println(s));
				}
			});
			throw new RuntimeException("Station duplication");
		}

		boolean fpExistence = idSet.stream().allMatch(id -> Files.exists(fpPath.resolve(id.toString())));
		boolean bpExistence = stationSet.stream().allMatch(station -> Files.exists(bpPath.resolve("0000" + station)));
		if (!fpExistence) {
			idSet.stream().filter(id -> !Files.exists(fpPath.resolve(id.toString())))
				.forEach(id -> System.out.println(id));
			throw new RuntimeException("propagation spectors are not enough for " + timewindowPath);
		}
		if (!bpExistence) {
			stationSet.stream().filter(station -> !Files.exists(bpPath.resolve("0000" + station)))
				.forEach(sta -> System.out.println(sta));
			throw new RuntimeException("propagation spectors are not enough for " + timewindowPath);
		}
		writeLog(timewindowInformation.size() + " timewindows are found in " + timewindowPath + ". " + idSet.size()
				+ " events and " + stationSet.size() + " stations.");
	}

}
