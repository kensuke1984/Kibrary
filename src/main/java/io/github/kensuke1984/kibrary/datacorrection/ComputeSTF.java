package io.github.kensuke1984.kibrary.datacorrection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

public class ComputeSTF {
	

	public static void main(String[] args) throws IOException, TauModelException {
//		Path timewindowPath = Paths.get("/work/anselme/CA_ANEL/syntheticPREM-v2/filtered_triangle_12.5-200s/selectedTimewindow_SScS.dat");
		
		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_4-200s");
		
		Path stfPath = workdir.resolve("stf_np256");
		
		int np = 256;
//		int np = 512;
//		int np = 1024;
//		double tlen = 3276.8;
		double tlen = 1638.4;
		int samplingHz = 20;
		
		double minDistance = 30.;
		double maxDistance = 75.;
		double minSN = 2;
		
		if (!Files.exists(stfPath))
			Files.createDirectories(stfPath);
//		
//		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath).stream()
//			.filter(tw -> {
//					double distance = Math.toDegrees(tw.getGlobalCMTID().getEvent().getCmtLocation()
//							.getEpicentralDistance(tw.getStation().getPosition()));
//					if (distance < minDistance || distance > maxDistance)
//						return false;
//					return true;
//				})
//			.collect(Collectors.toSet());
		
//		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
		
		Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(workdir);
		
		TauP_Time timetool = new TauP_Time("prem");
		timetool.parsePhaseList("S, ScS");
		
		for (EventFolder eventFolder : eventFolderSet) {
			System.out.println(eventFolder.getGlobalCMTID());
			
			timetool.setSourceDepth(6371. - eventFolder.getGlobalCMTID().getEvent().getCmtLocation().getR());
			
//			Set<TimewindowInformation> thisWindows = timewindows.stream()
//					.filter(tw -> tw.getGlobalCMTID().equals(eventFolder.getGlobalCMTID()))
//					.collect(Collectors.toSet());
			
			Set<TimewindowInformation> thisWindows = new HashSet<>();
			
			Set<SACFileName> obsNames = eventFolder.sacFileSet();
			obsNames.removeIf(sfn -> !sfn.isOBS());
			
//			obsNames.removeIf(sfn -> timewindows.stream().filter(tw -> tw.getGlobalCMTID().equals(sfn.getGlobalCMTID()) 
//					&& tw.getStation().getStationName().equals(sfn.getStationName())).count() == 0);
			
			List<SACData> obsSacs = new ArrayList<>();
			List<SACData> synSacs = new ArrayList<>();
			
			for (SACFileName obsName : obsNames) {
//				SACFileName synName = new SACFileName(obsName.getAbsolutePath().concat("sc"));
				SACFileName synName = new SACFileName(obsName.getAbsolutePath());
				SACData obsSac = obsName.read();
				SACData synSac = synName.read();
				
				if (obsSac.getValue(SACHeaderEnum.DELTA) != 0.05)
					continue;
				
				//-------
				double distance = obsSac.getValue(SACHeaderEnum.GCARC);
				if (distance < minDistance || distance > maxDistance)
					continue;
				
				timetool.calculate(distance);
				Arrival arrivalS = timetool.getArrival(0);
				Arrival arrivalScS = timetool.getArrival(1);
				if (!arrivalS.getPhase().getName().equals("S") || !arrivalScS.getPhase().getName().equals("ScS")) {
					System.err.println("Problem computing S or ScS " + obsName.getName());
					continue;
				}
				double timeS = arrivalS.getTime();
				
				Trace synS;
				Trace obsS;
				Trace noiseTrace;
				try {
					synS = synSac.createTrace().cutWindow(timeS - 20, timeS + 35).removeTrend();
					obsS = obsSac.createTrace().cutWindow(timeS - 20, timeS + 35).removeTrend();
					noiseTrace = obsSac.createTrace().cutWindow(timeS - 100, timeS - 20).removeTrend();
				} catch (RuntimeException e) {
					System.out.println("Ignore error for " + obsName.getName());
					continue;
				}
				
				//discard traces with low signal to noise ratio
				double obsLinf = obsS.getYVector().getLInfNorm();
				if (Double.isNaN(obsLinf) || obsLinf == 0)
					continue;
				double sn = obsLinf / noiseTrace.getYVector().getLInfNorm();
				if (sn < minSN)
					continue;
				
				obsSacs.add(obsSac);
				synSacs.add(synSac);
				
				double tS0 = timeS - 20;
				double tS1 = timeS + 35;
				
//				int synUpS = synS.getIndexOfUpwardConvex()[0];
//				int synDownS = synS.getIndexOfDownwardConvex()[0];
				
//				if (synUpS < synDownS) {
//					double tS0 = (synUpS - (synDownS - synUpS) )
//				}
				//-------
				
				thisWindows.add(new TimewindowInformation(tS0, tS1, obsSac.getStation()
						, obsSac.getGlobalCMTID(), obsName.getComponent(), new Phase[] {Phase.S}));
			}
			
			System.out.println(eventFolder.getGlobalCMTID() + " " + thisWindows.size() + " records");
			if (thisWindows.size() == 0)
				continue;
			SourceTimeFunctionByStackedPeaks stfsp = 
					new SourceTimeFunctionByStackedPeaks(np, tlen, samplingHz, 
							obsSacs.toArray(new SACData[obsSacs.size()]), synSacs.toArray(new SACData[synSacs.size()]), thisWindows);
			
			Trace stf = stfsp.getSourceTimeFunctionInTimeDomain();
			
			Path outpathstf = stfPath.resolve(eventFolder.getGlobalCMTID() + ".stf");
			stfsp.writeSourceTimeFunction(outpathstf);
			
			Path outpath = stfPath.resolve(eventFolder.getGlobalCMTID() + ".trace");
			writeTrace(outpath, stf);
		}
	}
	
	
	private static void writeTrace(Path outpath, Trace trace) {
		try {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		
		for (int i = 0; i < trace.getLength(); i++)
			pw.println(trace.getXAt(i) + " " + trace.getYAt(i));
		
		pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}

