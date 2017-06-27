/**
 * 
 */
package io.github.kensuke1984.kibrary.waveformdata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;

import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;
import io.github.kensuke1984.kibrary.util.spc.SACMaker;
import io.github.kensuke1984.kibrary.util.spc.SpcFileName;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.util.spc.ThreeDPartialMaker;

//import filehandling.sac.SacFile;
//import filehandling.spc.BackwardPropagation;
//import filehandling.spc.ForwardPropagation;
//import filehandling.spc.SpectrumFile;

/**
 * @author Yuki S.
 * @since 2017/05/11
 * @version 0.0.1
 * 
 */

public class ThreeDPartialWaveformMaker {

	static PartialType parType = PartialType.L;
	static SACComponent component = SACComponent.R;
	private static ButterworthFilter filter;
	static boolean shpsv;
	static double fmax;
	static double fmin;
	Set gcmtIDs = new HashSet<>();
	Set stations = new HashSet<>();

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) {

		fmax = 0.08;
		fmin = 0.005;
		shpsv = false;
		filter = new BandPassFilter(fmax * 2 * Math.PI / 20, fmin * 2 * Math.PI / 20, 4);
		GlobalCMTID gcmtid = new GlobalCMTID("201308041556A");
		String[] stationName = {"062Z"};	//Q44A 544A 456A 062Z	//"SPMN", "Z57A", "I49A", "062Z", "959A", "N58A"
		String ppName = "XY080";
		String[] mode = {"SH","SPV"};	//SH or PSV
		Arrays.stream(stationName).forEach(station -> {
		Path suzukiDir = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/basePREM/threeDPartial_5deg");
		Path bpDirPath = suzukiDir.resolve("BPtmp/").resolve("0000"+station+"/PREM");
		Path fpDirPath = suzukiDir.resolve("FPtmp/").resolve(gcmtid.toString()+"/PREM");
		// これらはペアになってないといけない (偏微分ポイントとかが)
		// backward
		Path bpPath1 = bpDirPath.resolve(ppName + "." + station + ".PB..." + mode[0] + ".spc");
		// forward
		Path fpPath1 = fpDirPath.resolve(ppName + "." + gcmtid.toString() + ".PF..." + mode[0] + ".spc");
		// backward
		Path bpPath2 = bpDirPath.resolve(ppName + "." + station + ".PB..." + mode[1] + ".spc");
		// forward
		Path fpPath2 = fpDirPath.resolve(ppName + "." + gcmtid.toString() + ".PF..." + mode[1] + ".spc");
		Path outDir = suzukiDir.resolve("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/basePREM/threeDPartial_5deg/3DparWave/output");
		Path outFilePath = Paths.get(outDir+"/"+ppName+"_"+station+"_"+gcmtid.toString()+"_"+component+"_"+parType);
		try {
			Files.deleteIfExists(outFilePath);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
//		Files.createDirectories(outFilePath);
//		output(outFilePath, bpPath1, fpPath1, bpPath2, fpPath2);
		output(outFilePath, bpPath2, fpPath2);
		});
	}

	private static void output(Path outPath, Path bpPath, Path fpPath) {
		if (Files.exists(outPath))
			throw new RuntimeException(outPath + " already exists.");
		try {
			Files.createDirectories(outPath);
		} catch (Exception e) {e.printStackTrace();} 
		
//		try{
		SpcFileName bpName = new SpcFileName(bpPath);
		SpcFileName fpName = new SpcFileName(fpPath);
		DSMOutput bp = bpName.read();
		DSMOutput fp = fpName.read();
		
		
		Station station = new Station(bp.getSourceID(), bp.getSourceLocation(), "DSM");
		GlobalCMTID id = new GlobalCMTID(fpName.getSourceID());
		String pointName = bp.getObserverID();
		ThreeDPartialMaker tdpm = new ThreeDPartialMaker(fp, bp);
		
		for (int iZone = 0; iZone < bp.nbody(); iZone++) {
			double[] depths = bp.getBodyR();
//			String depth = String.valueOf((int) bp.getBodyR(iZone));
//			String depth = String.valueOf(bp.getBodyR());
			String depth = String.valueOf((int) depths[iZone]);
			
//			 station.{@link GlobalCMTID}.{@link PartialType}.x.y.z.{@link SacComponent}
			SACFileName sacname = new SACFileName(outPath
					.resolve(station.getName() + "." + id.toString() + "."+parType+"." + pointName +"." + depth + "..R"));
//			double[] data = tdpm.createPartial(component, iZone, parType.getWeightingFactor());
			double[] data = tdpm.createPartial(component, iZone, parType);
			Complex[] complexData = Arrays.stream(data).mapToObj(Complex::valueOf).toArray(n -> new Complex[n]);
			Complex[] filtered = filter.applyFilter(complexData);
			SacFile sacFile = new SacFile(sacname);
			sacFile.setValue(SACHeaderEnum.B, 0);
			sacFile.setStation(station);
			sacFile.setEventLocation((Location) fp.getEventLocation());
			sacFile.setSacString(SACHeaderEnum.KEVNM, id.toString());

			sacFile.setInt(SACHeaderEnum.NPTS, data.length);
			sacFile.setValue(SACHeaderEnum.E, 0.05 * data.length);
			sacFile.setValue(SACHeaderEnum.DELTA, 0.05);
			sacFile.setValue(SACHeaderEnum.GCARC, fp.getEventLocation().getEpicentralDistance(bp.getObserverLocation()));
			sacFile.setValue(SACHeaderEnum.EVDP, fp.getBodyR(0));
			sacFile.setSacString(SACHeaderEnum.KSTNM, "DSM");
			
			data = Arrays.stream(filtered).mapToDouble(c -> c.getReal()).toArray();
			sacFile.setSacData(data);
			sacFile.overWrite();
		}	
	}
	
	private Set<Station> stationSet;
	private Set<GlobalCMTID> idSet;
	private double[][] periodRanges;
	private Set<Location> perturbationLocationSet;
	private Phase[] phases;
	
	private void readPerturbationPoints() throws IOException {
		try (Stream<String> lines = Files.lines(perturbationPath)) {
			perturbationLocationSet = lines.map(line -> line.split("\\s+"))
					.map(parts -> new Location(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
							Double.parseDouble(parts[2])))
					.collect(Collectors.toSet());
		}
	}
	
	private static void output(Path outPath, Path bpPath1, Path fpPath1, Path bpPath2, Path fpPath2) {
		if (Files.exists(outPath))
			throw new RuntimeException(outPath + " already exists.");
		try {
			Files.createDirectories(outPath);
		} catch (Exception e) {e.printStackTrace();} 
		
//		try{
		SpcFileName bpName1 = new SpcFileName(bpPath1);
		SpcFileName fpName1 = new SpcFileName(fpPath1);
		SpcFileName bpName2 = new SpcFileName(bpPath2);
		SpcFileName fpName2 = new SpcFileName(fpPath2);
		BackwardPropagation bp1 = (BackwardPropagation) SpectrumFile.getInstance(bpName1);
		ForwardPropagation fp1 = (ForwardPropagation) SpectrumFile.getInstance(fpName1);
		BackwardPropagation bp2 = (BackwardPropagation) SpectrumFile.getInstance(bpName2);
		ForwardPropagation fp2 = (ForwardPropagation) SpectrumFile.getInstance(fpName2);
		
		Station station1 = new Station(bp1.getStationName(), bp1.getSourceLocation());
		Station station2 = new Station(bp2.getStationName(), bp2.getSourceLocation());
		if (!station1.getName().equals(station2.getName())) {
			System.err.println("station for SH.spc and station for PSV.spc are different!");
			System.out.println(station1 + " "+ station2);
		}	
		GlobalCMTID id1 = fp1.getSpcFileName().getGlobalCMTID();
		GlobalCMTID id2 = fp2.getSpcFileName().getGlobalCMTID();
		String pointName1 = fp1.getPerturbationPointName();
		String pointName2 = fp2.getPerturbationPointName();
		ThreeDPartialMaker tdpm = new ThreeDPartialMaker(fp1, bp1, fp2, bp2, shpsv);
		tdpm.setComputesSourceTimeFunction(true);
		
		for (int iZone = 0; iZone < bp1.getNbody(); iZone++) {
			String depth = String.valueOf((int) bp1.getBodyR(iZone));
//			 station.{@link GlobalCMTID}.{@link PartialType}.x.y.z.{@link SacComponent}
			SACFileName sacname = new SACFileName(outPath
					.resolve(station1.getName() + "." + id1.toString() + "."+parType+"." + pointName1 +"." + depth + ".." + component));
			double[] data = tdpm.createPartial(component, iZone, parType.getWeightingFactor());
			Complex[] complexData = Arrays.stream(data).mapToObj(Complex::valueOf).toArray(n -> new Complex[n]);
			Complex[] filtered = filter.applyFilter(complexData);
			SacFile sacFile = new SacFile(sacname);
			sacFile.setValue(SACHeaderEnum.B, 0);
			sacFile.setStation(station1);
			sacFile.setEventLocation((Location) fp1.getEventLocation());
			sacFile.setSacString(SACHeaderEnum.KEVNM, id1.toString());

			sacFile.setInt(SACHeaderEnum.NPTS, data.length);
			sacFile.setValue(SACHeaderEnum.E, 0.05 * data.length);
			sacFile.setValue(SACHeaderEnum.DELTA, 0.05);
			sacFile.setValue(SACHeaderEnum.GCARC, fp1.getEventLocation().getEpicentralDistance(bp1.getObserverLocation()));
			sacFile.setValue(SACHeaderEnum.EVDP, fp1.getBodyR(0));
			sacFile.setSacString(SACHeaderEnum.KSTNM, "DSM");
			
			data = Arrays.stream(filtered).mapToDouble(c -> c.getReal()).toArray();
			sacFile.setSacData(data);
			sacFile.overWrite();
		}	
	}
	
	private double partialSamplingHz = 20;
	/**
	 * バンドパスを安定させるためwindowを左右に ext = max period(s) ずつ伸ばす
	 */
	private int ext;
	
	/**
	 * cut partial derivative in [start-ext, start+ext] The ext is for
	 * filtering .
	 * 
	 * @param u waveform
	 * @param timewindowInformation time window information
	 * @return cut waveform
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
	
	private static int sourceTimeFunction;
	private Map<GlobalCMTID, SourceTimeFunction> userSourceTimeFunctions;
	/**
	 * The folder contains source time functions.
	 */
	private Path sourceTimeFunctionPath;
	
	/**
	 * time length (DSM parameter)
	 */
	private double tlen;

	/**
	 * step of frequency domain (DSM parameter)
	 */
	private int np;
	
	private void setSourceTimeFunctions() throws IOException {
		if (sourceTimeFunction == 0)
			return;
		if (sourceTimeFunction == -1) {
//			readSourceTimeFunctions();
			return;
		}
		userSourceTimeFunctions = new HashMap<>();
		idSet.forEach(id -> {
			double halfDuration = id.getEvent().getHalfDuration();
			SourceTimeFunction stf = sourceTimeFunction == 1
					? SourceTimeFunction.boxcarSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration)
					: SourceTimeFunction.boxcarSourceTimeFunction(np, tlen, partialSamplingHz, halfDuration);
			userSourceTimeFunctions.put(id, stf);
		});

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

}
