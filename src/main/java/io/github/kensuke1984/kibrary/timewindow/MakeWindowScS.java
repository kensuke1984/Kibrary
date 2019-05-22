package io.github.kensuke1984.kibrary.timewindow;

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
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

public class MakeWindowScS {

	public static void main(String[] args) throws IOException, TauModelException {
		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_stf_12.5-200s");
		workdir = Paths.get("/work/anselme/CA_ANEL_NEW/synthetic_s0/filtered_stf_12.5-200s");
		workdir = Paths.get("/work/anselme/CA_ANEL_NEW/synthetic_s0_it1/filtered_stf_12.5-200s");
		workdir = Paths.get("/work/anselme/CA_ANEL_NEW/synthetic_s5/filtered_stf_12.5-200s");
//		Path workdir = Paths.get(".");
		
		boolean select = true;
		double minCorr = 0.5;
		double maxVariance = 1.2;
		double maxRatio = 2.;
		double maxRatio_S = 2.;
		
		double timeBefore = 15.; //30 for Scd
		double timeAfter = 40;
		double timeBeforesS = 5;
		
		Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(workdir);
		
		TauP_Time timetool = new TauP_Time("prem");
		timetool.parsePhaseList("S, ScS, sS");
		
		Set<TimewindowInformation> infoSet = new HashSet<>();
		Path outpath = workdir.resolve("timewindow" + Utilities.getTemporaryString() + ".dat");
		
		Set<TimewindowInformation> infoSetS = new HashSet<>();
		Path outpathS = workdir.resolve("timewindow_S.dat");

		for (EventFolder eventFolder : eventFolderSet) {
			System.out.println(eventFolder);
			timetool.setSourceDepth(6371. - eventFolder.getGlobalCMTID().getEvent().getCmtLocation().getR());
			
			Set<SACFileName> obsNames = eventFolder.sacFileSet();
			obsNames.removeIf(sfn -> !sfn.isOBS());
			obsNames.removeIf(sfn -> !sfn.getComponent().equals(SACComponent.T));
			
			for (SACFileName obsName : obsNames) {
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
//					System.err.println("Problem computing S or ScS " + obsName.getName());
					continue;
				}
				double timeS = arrivalS.getTime();
				double timeScS = arrivalScS.getTime();
				double timesS = arrivalsS.getTime();
				
				if (timesS - timeScS < 20)
					continue;
				
				SACData synData = new SACFileName(Paths.get(obsName.getAbsolutePath().concat("sc"))).read();
				Trace synTrace = synData.createTrace().cutWindow(timeS - 15, timeS + 40);
				double deltaTimeP2P = Math.abs(synTrace.getXforMaxValue() - synTrace.getXforMinValue());
				double timeLatePeak = synTrace.getXforMaxValue() > synTrace.getXforMinValue() ? synTrace.getXforMaxValue() : synTrace.getXforMinValue();
				double timeEndOfS = timeLatePeak + deltaTimeP2P * 0.9;
				
				SACData obsData = obsName.read();
				Trace obsTrace = obsData.createTrace().cutWindow(timeS - 15, timeS + 40);
				double synSP2P = synTrace.getYVector().getLInfNorm();
				double obsSP2P = obsTrace.getYVector().getLInfNorm();
				double ratio_S = synSP2P / obsSP2P;
				
				double startTime = timeScS - timeBefore;
				double endTime = timeScS + timeAfter;
				
				if (startTime < timeEndOfS)
					startTime = timeEndOfS;
				if (endTime > timesS - timeBeforesS)
					endTime = timesS - timeBeforesS;
				
				if (timeEndOfS > timeScS - 5)
					continue;
				
				TimewindowInformation timewindow_S = new TimewindowInformation(timeS - 15, timeS + 35,
						obsHeader.getStation(), obsHeader.getGlobalCMTID(),
						obsName.getComponent(), new Phase[] {Phase.S});
				infoSetS.add(timewindow_S);
				
				if (ratio_S > maxRatio_S || ratio_S < 1./maxRatio_S)
					continue;
				
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
					if (!keep)
						continue;
				}
				
				TimewindowInformation timewindow = new TimewindowInformation(startTime, endTime,
						obsHeader.getStation(), obsHeader.getGlobalCMTID(),
						obsName.getComponent(), new Phase[] {Phase.ScS});
				
				
				
				infoSet.add(timewindow);
			}
		}
		
		TimewindowInformationFile.write(infoSet, outpath);
		TimewindowInformationFile.write(infoSetS, outpathS);
	}

}
