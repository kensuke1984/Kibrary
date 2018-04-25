/**
 * 
 */
package io.github.kensuke1984.kibrary.waveformdata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;

import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;
import io.github.kensuke1984.kibrary.util.sac.SACUtil;
import io.github.kensuke1984.kibrary.util.spc.SACMaker;
import io.github.kensuke1984.kibrary.util.spc.SpcFileName;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.external.SAC;
import io.github.kensuke1984.kibrary.inversion.PerturbationPoint;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.spc.DSMOutput;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.util.spc.ThreeDPartialMaker;


/**
 * @author Yuki S.
 * @since 2017/05/11
 * @version 0.0.1
 * 
 */

public class ThreeDPartialWaveformMaker {

	private static ButterworthFilter filter;
	private static Path perturbFilePath;
//	private static Set<Station> stationSet;
	private static Set<GlobalCMTID> idSet;
//	private static double[][] periodRanges;
	private static PerturbationPoint perturbationPoints;
	private static Set<Location> perturbationLocationSet;
//	private static Phase[] phases;
	private static File horizontalPointFile;
	private static File perturbationPointFile;
	private static Path suzukiDir;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		if (args.length!=8)
			System.err.println("usage: globalCMTID, station name,  SH or PSV, partial type, component, "
					+ "home directry path, horizontal Point File, perturbation point file");

		double fmax = 0.125;
		double fmin = 0.005;
		filter = new BandPassFilter(fmax * 2 * Math.PI / 20, fmin * 2 * Math.PI / 20, 4);
		GlobalCMTID gcmtid = new GlobalCMTID(args[0]);	// global CMT ID
		String stationName = args[1];					// name of station
//		String ppName = args[2];						// name of perturbation point
		String mode = args[2];	//SH or PSV
		PartialType parType = PartialType.valueOf(args[3]);
		SACComponent component = SACComponent.valueOf(args[4]);		
		suzukiDir = Paths.get(args[5]);
		horizontalPointFile = new File(args[6]);
		perturbationPointFile = new File(args[7]);
		perturbationPoints = new PerturbationPoint(horizontalPointFile, perturbationPointFile);
		String[] ppoints = perturbationPoints.getPointName();
		
		if (Files.notExists(suzukiDir.resolve("3DparWave")))
			Files.createDirectories(suzukiDir.resolve("3DparWave"));
		Path tdwPath = suzukiDir.resolve("3DparWave");
		if (Files.notExists(tdwPath.resolve("output")))
			Files.createDirectories(tdwPath.resolve("output"));
		Path outDir = tdwPath.resolve("output");
		Path bpDirPath = suzukiDir.resolve("BPinfo/").resolve("0000"+stationName+"/PREM");
		Path fpDirPath = suzukiDir.resolve("FPinfo/").resolve(gcmtid.toString()+"/PREM");
		// これらはペアになってないといけない (偏微分ポイントとかが)
		
//		for (int i=0;i<ppoints.length;i++)
//			System.out.println(ppoints[i]);
		
		for (int i=0; i<ppoints.length; i++) {
//		Arrays.stream(ppoints).forEach(pp -> {
//			System.out.println(pp);
			// backward
			Path bpPath = bpDirPath.resolve(ppoints[i] + "." + stationName + ".PB..." + mode + ".spc");
			// forward
			Path fpPath = fpDirPath.resolve(ppoints[i] + "." + gcmtid.toString() + ".PF..." + mode + ".spc");
			Path outFilePath = Paths.get(outDir+"/"+ppoints[i]+"_"+stationName+"_"+gcmtid.toString()+"_"+mode);
			try {
				if (!Files.exists(outFilePath))
					Files.createDirectory(outFilePath);
//			 Files.deleteIfExists(outFilePath);
//			 Files.createDirectories(outFilePath);
			 output(outFilePath, bpPath, fpPath, parType, component);
			} catch (IOException e) {e.printStackTrace();}
//		});
		}
	
	}

	private static void output(Path outPath, Path bpPath, Path fpPath, PartialType parType, SACComponent component) throws IOException {
//		if (Files.exists(outPath))
//			throw new RuntimeException(outPath + " already exists.");
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
		String pointName = bp.getObserverName();
		ThreeDPartialMaker tdpm = new ThreeDPartialMaker(fp, bp);
		
		for (int iZone = 0; iZone < bp.nbody(); iZone++) {
//			double[] depths = bp.getBodyR();
			String depth = String.valueOf((int) bp.getBodyR()[iZone]);

			//TODO //
//			if ((int) bp.getBodyR()[iZone] == 3505){
				
//			 station.{@link GlobalCMTID}.{@link PartialType}.x.y.z.{@link SacComponent}
			SACFileName sacname = new SACFileName(outPath
					.resolve(station.getName() + "." + id.toString() + "."+parType+"." + pointName +"." + depth + ".."+component.toString()));
			double[] data = tdpm.createPartial(component, iZone, parType);
			Complex[] complexData = Arrays.stream(data).mapToObj(Complex::valueOf).toArray(n -> new Complex[n]);
			Complex[] filtered = filter.applyFilter(complexData);
			
			Path source = suzukiDir.resolve("template.sac");
			Map<SACHeaderEnum, String>header = SACUtil.readHeader(source);
			header.put(SACHeaderEnum.B, Double.toString(0));
			header.put(SACHeaderEnum.KSTNM, station.getName());
			header.put(SACHeaderEnum.EVLA, Double.toString(fp.getSourceLocation().getLatitude()));
			header.put(SACHeaderEnum.EVLO, Double.toString(fp.getSourceLocation().getLongitude()));
			header.put(SACHeaderEnum.EVDP, Double.toString(Earth.EARTH_RADIUS-fp.getSourceLocation().getR()));
			
			header.put(SACHeaderEnum.KEVNM, id.toString());

			header.put(SACHeaderEnum.NPTS, Integer.toString(data.length));
			header.put(SACHeaderEnum.E, Double.toString(0.05 * data.length));
			header.put(SACHeaderEnum.DELTA, Double.toString(0.05));
			header.put(SACHeaderEnum.GCARC, Double.toString(fp.getSourceLocation().getEpicentralDistance(bp.getSourceLocation())));
			header.put(SACHeaderEnum.KNETWK, "DSM");
			data = Arrays.stream(filtered).mapToDouble(c -> c.getReal()).toArray();
//			System.out.println(Integer.parseInt(header.get(SACHeaderEnum.NPTS))+" "+Double.toString(data.length));
			SACUtil.writeSAC(sacname.toPath(), header, data);
			
//			} //if(depth=="3505")
		}	
	}
	
	private void readPerturbationLocations() throws IOException {
		try (Stream<String> lines = Files.lines(perturbFilePath)) {		
			perturbationLocationSet = lines.map(line -> line.split("\\s+"))
					.map(parts -> new Location(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
							Double.parseDouble(parts[2])))
					.collect(Collectors.toSet());
		}
	}
	
	/**
	private static void output(Path outPath, Path bpPath1, Path fpPath1, Path bpPath2, Path fpPath2, Path parPath, PartialType parType, SACComponent component) {
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
		ThreeDPartialMaker tdpm = new ThreeDPartialMaker(fp1, bp1, fp2, bp2);
//		tdpm.setComputesSourceTimeFunction(true);
		tdpm.setSourceTimeFunction(stfs);
		
		for (int iZone = 0; iZone < bp1.getNbody(); iZone++) {
			String depth = String.valueOf((int) bp1.getBodyR(iZone));
//			 station.{@link GlobalCMTID}.{@link PartialType}.x.y.z.{@link SacComponent}
			SACFileName sacname = new SACFileName(outPath
					.resolve(station1.getName() + "." + id1.toString() + "."+parType+"." + pointName1 +"." + depth + ".." + component));
			double[] data = tdpm.createPartial(component, iZone, parType);
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
**/
	

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
	private static SourceTimeFunction stfs;
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
