package io.github.kensuke1984.kibrary.timewindow.addons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.datacorrection.FujiStaticCorrection;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

public class MakeWindowScS {

	public static void main(String[] args) throws IOException, TauModelException {
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_stf_12.5-200s");
//		workdir = Paths.get("/work/anselme/CA_ANEL_NEW/synthetic_s0/filtered_stf_12.5-200s");
//		workdir = Paths.get("/work/anselme/CA_ANEL_NEW/synthetic_s0_it1/filtered_stf_12.5-200s");
//		workdir = Paths.get("/work/anselme/CA_ANEL_NEW/synthetic_s5/filtered_stf_12.5-200s");
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s");
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster3/synthetic_cl3s0_it1/filtered_nostf_8-200s");
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster3/syntheticLVZ_CA_2/filtered_nostf_8-200s");
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster3/synthetic_cl3s0_it2/filtered_nostf_12.5-200s");
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster4/synthetic_sw_it1/filtered_nostf_8-200s");
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/EFFECT_OF_FAR_SLAB/filtered_12.5-200s");
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_8-200s");
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster34/synthetic_cl4s0_it1/filtered_nostf_8-200s");
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster34/synthetic_cl3s0_it1/filtered_nostf_6-200s");
//		Path workdir = Paths.get(".");
		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/SPECFEM_MODELS/CL4/SIMPLE/filtered_12.5-200s");
		
//		Path timewindowPath = workdir.resolve("selectedTimewindow_SScS_60deg.dat");
//		Path timewindowPath = workdir.resolve("selectedTimewindow_ScS_65deg_201205281150A.dat");
//		Path timewindowPath = workdir.resolve("timewindow_S_60deg.dat");
		Path timewindowPath = workdir.resolve("timewindow_ScS_longer.dat");
		
		Set<TimewindowInformation> timewindowsForSelection = TimewindowInformationFile.read(timewindowPath); 
//		timewindowsForSelection = null;
		
		boolean convolved = true;
		
		boolean extendedDistanceRange = true;
		
		boolean select = false;
		double minCorr = 0.5;
		double maxVariance = 2.5;
		double maxRatio = 2.5;
		double maxRatio_S = 2.5;
		
		double timeBefore = 30.; //30 for Scd
//		timeBefore = 10; // 10 for spectral amplitudes
		double timeAfter = 25; //40
		double timeBeforesS = 5;
		
		double minPeriod = 8.;
		
		Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(workdir);
		
		TauP_Time timetool = new TauP_Time("prem");
		timetool.parsePhaseList("S, ScS, sS");
		
		Set<TimewindowInformation> infoSet = new HashSet<>();
		Path outpath = workdir.resolve("selectedTimewindow_ScS" + Utilities.getTemporaryString() + ".dat");
		
		Set<TimewindowInformation> infoSetScStight = new HashSet<>();
		Path outpathTight = workdir.resolve("selectedTimewindow_ScStight" + Utilities.getTemporaryString() + ".dat");
		
		Set<TimewindowInformation> infoSetScd = new HashSet<>();
		Path outpathScd = workdir.resolve("selectedTimewindow_Scd" + Utilities.getTemporaryString() + ".dat");
		
		Set<TimewindowInformation> infoSet_noSelection = new HashSet<>();
		Path outpath_noSelection = workdir.resolve("timewindow_ScS" + Utilities.getTemporaryString() + ".dat");
		
		Set<TimewindowInformation> infoSetS = new HashSet<>();
		Path outpathS = workdir.resolve("timewindow_S.dat");

		int countIncludeS = 0;
		
		for (EventFolder eventFolder : eventFolderSet) {
			System.out.println(eventFolder);
			timetool.setSourceDepth(6371. - eventFolder.getGlobalCMTID().getEvent().getCmtLocation().getR());
			
			Set<SACFileName> obsNames = eventFolder.sacFileSet();
			obsNames.removeIf(sfn -> !sfn.isOBS());
			obsNames.removeIf(sfn -> !sfn.getComponent().equals(SACComponent.T));
			
			for (SACFileName obsName : obsNames) {
				if (timewindowsForSelection != null) {
					if (timewindowsForSelection.parallelStream().filter(tw -> tw.getGlobalCMTID().equals(obsName.getGlobalCMTID())
							&& tw.getStation().getName().equals(obsName.getStationName()) && tw.getComponent().equals(obsName.getComponent()))
							.count() == 0)
						continue;
				}
				
				SACHeaderData obsHeader = obsName.readHeader();
				
				double distance = obsHeader.getValue(SACHeaderEnum.GCARC);
				timetool.calculate(distance);
				if (timetool.getNumArrivals() != 3) {
					continue;
				}
				Arrival arrivalS = timetool.getArrival(0);
				Arrival arrivalScS = timetool.getArrival(1);
				Arrival arrivalsS = timetool.getArrival(2);
				if (!arrivalS.getPhase().getName().equals("S") || !arrivalScS.getPhase().getName().equals("ScS")
						|| !arrivalsS.getPhase().getName().equals("sS")) {
					System.err.println("Problem computing S or ScS " + obsName.getName() + " " + obsHeader.getValue(SACHeaderEnum.GCARC));
					continue;
				}
				double timeS = arrivalS.getTime();
				double timeScS = arrivalScS.getTime();
				double timesS = arrivalsS.getTime();
				
				if (timesS - timeScS < minPeriod * 1.6)
					continue;
				
				SACData synData = null;
				if (!convolved)
					synData = new SACFileName(Paths.get(obsName.getAbsolutePath().concat("s"))).read();
				else
					synData = new SACFileName(Paths.get(obsName.getAbsolutePath().concat("sc"))).read();
				Trace synTrace = synData.createTrace().cutWindow(timeS - 15, timeS + 40);
				double deltaTimeP2P = Math.abs(synTrace.getXforMaxValue() - synTrace.getXforMinValue());
				double timeFirstPeak = synTrace.getXforMaxValue() < synTrace.getXforMinValue() ? synTrace.getXforMaxValue() : synTrace.getXforMinValue();
				double timeLatePeak = synTrace.getXforMaxValue() > synTrace.getXforMinValue() ? synTrace.getXforMaxValue() : synTrace.getXforMinValue();
				double timeEndOfS = timeLatePeak + deltaTimeP2P * 0.9;
				
				SACData obsData = obsName.read();
				Trace obsTrace = obsData.createTrace().cutWindow(timeS - 15, timeS + 40);
				double synSP2P = synTrace.getYVector().getLInfNorm();
				double obsSP2P = obsTrace.getYVector().getLInfNorm();
				double ratio_S = synSP2P / obsSP2P;
				
				double startTime = timeScS - timeBefore;
				double endTime = timeScS + timeAfter;
				
				double startTimeTight = timeScS - 5;
				
				double endTimeScd = timeScS - 2;
				
				if (startTime < timeEndOfS)
					startTime = timeEndOfS;
				if (startTimeTight < timeEndOfS)
					startTimeTight = timeEndOfS;
				if (endTime > timesS - timeBeforesS)
					endTime = timesS - timeBeforesS;
				
				if (extendedDistanceRange) {
					double startTime0 = timeScS - timeBefore;
					double startTime1 = timeScS - 3.;
					if (startTime0 >= timeEndOfS)
						startTime = startTime0;
					else if (startTime0 < timeEndOfS && startTime1 >= timeEndOfS)
						startTime = timeEndOfS;
					else if (startTime1 < timeEndOfS && startTime1 >= timeFirstPeak)
						startTime = startTime1;
					else if (timeScS >= timeFirstPeak)
						startTime = timeFirstPeak;
					else {
						countIncludeS++;
						continue;
					}
				}
				else {
					if (timeEndOfS > timeScS - minPeriod/2.5) {
						countIncludeS++;
						continue;
					}
				}
				
				double endTimeS = timeS + 25;
				double startTimeS = timeS - 25;
//				double endTimeS = timeS + 30;
//				double startTimeS = timeS - 10;
				if (timeS + 25 > timeScS) {
					double tmpTime = timeS + 25 - timeScS;
					endTimeS -= tmpTime;
					startTimeS -= tmpTime;
				}
				
				TimewindowInformation timewindow_S = new TimewindowInformation(startTimeS, endTimeS,
						obsHeader.getStation(), obsHeader.getGlobalCMTID(),
						obsName.getComponent(), new Phase[] {Phase.S});
				infoSetS.add(timewindow_S);
				
//				if (ratio_S > maxRatio_S || ratio_S < 1./maxRatio_S)
//					continue;
				
				boolean addScS = true;
				if (select) {
					Trace synScS = synData.createTrace().cutWindow(startTime, endTime);
					Trace obsScS = obsData.createTrace().cutWindow(startTime, endTime);
					double shift = synScS.getXforMaxValue() - obsScS.getXforMaxValue();
					
					RealVector obsVector = obsData.createTrace().cutWindow(startTime - shift, endTime - shift).getYVector()
							.mapMultiply(ratio_S);
					RealVector synVector = new ArrayRealVector(Arrays.copyOf(synScS.getY(), obsVector.getDimension()));
					
					double corr = obsVector.dotProduct(synVector) / (obsVector.getNorm() * synVector.getNorm());
					double ratio = synVector.getLInfNorm() / obsVector.getLInfNorm();
					double variance = synVector.subtract(obsVector)
							.dotProduct(synVector.subtract(obsVector))
							/ obsVector.dotProduct(obsVector);
					
					boolean keep = corr >= minCorr && ratio < maxRatio && ratio > 1./maxRatio && variance < maxVariance;
					addScS = keep;
				}
				
				TimewindowInformation timewindow = new TimewindowInformation(startTime, endTime,
						obsHeader.getStation(), obsHeader.getGlobalCMTID(),
						obsName.getComponent(), new Phase[] {Phase.ScS});
				if (addScS)
					infoSet.add(timewindow);
				
				if (startTime < endTimeScd) {
					TimewindowInformation timewindowScd = new TimewindowInformation(startTime, endTimeScd,
							obsHeader.getStation(), obsHeader.getGlobalCMTID(),
							obsName.getComponent(), new Phase[] {Phase.ScS});
					if (addScS)
						infoSetScd.add(timewindowScd);
				}
				
				TimewindowInformation timewindowTight = new TimewindowInformation(startTimeTight, endTime,
						obsHeader.getStation(), obsHeader.getGlobalCMTID(),
						obsName.getComponent(), new Phase[] {Phase.ScS});
				if (addScS)
					infoSetScStight.add(timewindowTight);
				
				infoSet_noSelection.add(timewindow);
			}
		}
		
		System.out.println("Excluded " + countIncludeS + " timewindows that include S phase");
		
		TimewindowInformationFile.write(infoSet, outpath);
		TimewindowInformationFile.write(infoSetS, outpathS);
		TimewindowInformationFile.write(infoSet_noSelection, outpath_noSelection);
		TimewindowInformationFile.write(infoSetScd, outpathScd);
		TimewindowInformationFile.write(infoSetScStight, outpathTight);
	}

}
