package io.github.kensuke1984.kibrary.timewindow.addons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import io.github.kensuke1984.anisotime.Phase;
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

public class MakeWindowPcPandScS {

	private Set<TimewindowInformation> infoSetPcP;
	
	private Set<TimewindowInformation> infoSetSelectedPcP;
	
	private Set<TimewindowInformation> infoSetStrictSelectedPcP;
	
	private Set<TimewindowInformation> infoSetScS;
	
	private Set<TimewindowInformation> infoSetSelectedScS;
	
	private Set<TimewindowInformation> infoSetP;
	
	private Set<TimewindowInformation> infoSetS;
	
	private Path workdir;
	
	private double timeBeforeScS;
	private double timeAfterScS;
	private double timeBeforesS;
	
	private double timeBeforePcP;
	private double timeAfterPcP;
	private double timeBeforepP;
	
	private boolean select;
	private double minCorr;
	private double maxVariance;
	private double maxRatio;
	private double minSNratio;
	
	public MakeWindowPcPandScS(Path workdir) {
		this.workdir = workdir;
		this.infoSetPcP = new HashSet<>();
		this.infoSetScS = new HashSet<>();
		this.infoSetS = new HashSet<>();
		this.infoSetP = new HashSet<>();
		this.infoSetSelectedPcP = new HashSet<>();
		this.infoSetSelectedScS = new HashSet<>();
		this.infoSetStrictSelectedPcP = new HashSet<>();
		
		timeBeforePcP = 20;
		timeAfterPcP = 20;
		timeBeforepP = 3;
		
		timeBeforeScS = 30.; //30 for Scd
		timeAfterScS = 40;
		timeBeforesS = 5;
		
		select = true;
		minCorr = -0.5;
		maxVariance = 2.5;
		maxRatio = 2.5;
		minSNratio = 2.;
	}
	
	public static void main(String[] args) throws IOException {
//		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_stf_12.5-200s");
		Path workdir = Paths.get(".");
		
		MakeWindowPcPandScS makewindow = new MakeWindowPcPandScS(workdir);
		try {
			makewindow.run();
		} catch (TauModelException e) {
			e.printStackTrace();
		}
		
		String temp = Utilities.getTemporaryString();
		
		Path outpath = workdir.resolve("timewindow_PcP" + ".dat");
		if (makewindow.infoSetPcP.size() > 0)
			TimewindowInformationFile.write(makewindow.infoSetPcP, outpath);
		
		Path outpathScS = workdir.resolve("timewindow_ScS" + ".dat");
		if (makewindow.infoSetScS.size() > 0)
			TimewindowInformationFile.write(makewindow.infoSetScS, outpathScS);
		
		Path outpathSelected = workdir.resolve("selectedTimewindow_PcP" + ".dat");
		if (makewindow.infoSetSelectedPcP.size() > 0)
			TimewindowInformationFile.write(makewindow.infoSetSelectedPcP, outpathSelected);
		
		Path outpathSelectedStrict = workdir.resolve("selectedStricTimewindow_PcP" + ".dat");
		if (makewindow.infoSetSelectedPcP.size() > 0)
			TimewindowInformationFile.write(makewindow.infoSetStrictSelectedPcP, outpathSelectedStrict);
		
		Path outpathSelecetdScS = workdir.resolve("selectedTimewindow_ScS" + ".dat");
		if (makewindow.infoSetSelectedScS.size() > 0)
			TimewindowInformationFile.write(makewindow.infoSetSelectedScS, outpathSelecetdScS);
		
		Path outpathP = workdir.resolve("timewindow_P.dat");
		TimewindowInformationFile.write(makewindow.infoSetP, outpathP);
		
		Path outpathS = workdir.resolve("timewindow_S.dat");
		TimewindowInformationFile.write(makewindow.infoSetS, outpathS);
	}
	
	public void run() throws IOException, TauModelException {
		TauP_Time timetool = new TauP_Time("prem");
		timetool.parsePhaseList("P, PcP, pP, S, ScS, sS");
		
//		try {
//		Utilities.runEventProcess(workdir, eventFolder -> {
		
		for (EventFolder eventFolder : Utilities.eventFolderSet(workdir)) {
			System.out.println(eventFolder);
			timetool.setSourceDepth(6371. - eventFolder.getGlobalCMTID().getEvent().getCmtLocation().getR());
			
//			Set<SACFileName> obsNames = eventFolder.sacFileSet();
//			obsNames.removeIf(sfn -> !sfn.isOBS() || !sfn.getComponent().equals(SACComponent.Z));
			
//			for (SACFileName obsName : obsNames) {
			try {
			eventFolder.sacFileSet().stream().filter(sfn -> sfn.isOBS() && sfn.getComponent().equals(SACComponent.Z)).forEach(obsName -> {
				SACHeaderData obsHeader = null;
				try {
					obsHeader = obsName.readHeader();
				} catch (IOException e) {
					e.printStackTrace();
				}
				double distance = obsHeader.getValue(SACHeaderEnum.GCARC);
				try {
					timetool.calculate(distance);
				} catch (TauModelException e) {
					e.printStackTrace();
				}
				if (timetool.getNumArrivals() != 6) {
					return;
				}
				Arrival arrivalP = timetool.getArrival(0);
				Arrival arrivalPcP = timetool.getArrival(1);
				Arrival arrivalpP = timetool.getArrival(2);
				Arrival arrivalS = timetool.getArrival(3);
				Arrival arrivalScS = timetool.getArrival(4);
				Arrival arrivalsS = timetool.getArrival(5);
				if (!arrivalS.getPhase().getName().equals("S") || !arrivalScS.getPhase().getName().equals("ScS")
						|| !arrivalsS.getPhase().getName().equals("sS") || !arrivalP.getPhase().getName().equals("P") 
						|| !arrivalPcP.getPhase().getName().equals("PcP")
						|| !arrivalpP.getPhase().getName().equals("pP")) {
					System.err.println("Problem computing phases " + obsName.getName());
					return;
				}
				double timeS = arrivalS.getTime();
				double timeScS = arrivalScS.getTime();
				double timesS = arrivalsS.getTime();
				double timeP = arrivalP.getTime();
				double timePcP = arrivalPcP.getTime();
				double timepP = arrivalpP.getTime();
				
				boolean addScS = true;
				boolean addPcP = true;
				boolean addPcPstrict = true;
				
				if (timesS - timeScS < 12)
					return;
				
				SACData synData = null;
				SACData obsData = null;
				SACData obsData_T = null;
				SACData synData_T = null;
				try {
					synData = new SACFileName(Paths.get(obsName.getAbsolutePath().concat("sc"))).read();
					obsData = obsName.read();
					obsData_T = new SACFileName(Paths.get(obsName.getAbsolutePath().replace(".Z", ".T"))).read();
					synData_T = new SACFileName(Paths.get(obsName.getAbsolutePath().replace(".Z", ".Tsc"))).read();
				} catch (IOException e) {
					System.out.println("Error reading " + obsName.getAbsolutePath());
					e.printStackTrace();
					return;
				}
				Trace synTrace_S = synData_T.createTrace().cutWindow(timeS - 15, timeS + 40);
				Trace synTrace_P = synData.createTrace().cutWindow(timeP - 5, timeP + 20);
				
				//traveltimes S
				double deltaTimeP2P_S = Math.abs(synTrace_S.getXforMaxValue() - synTrace_S.getXforMinValue());
				double timeLatePeak_S = synTrace_S.getXforMaxValue() > synTrace_S.getXforMinValue() ? synTrace_S.getXforMaxValue() : synTrace_S.getXforMinValue();
				double timeEndOfS = timeLatePeak_S + deltaTimeP2P_S * 1.;
				
				double startTime_ScS = timeScS - timeBeforeScS;
				double endTime_ScS = timeScS + timeAfterScS;
				
				if (startTime_ScS < timeEndOfS)
					startTime_ScS = timeEndOfS;
				if (endTime_ScS > timesS - timeBeforesS)
					endTime_ScS = timesS - timeBeforesS;
				
				if (timeEndOfS > timeScS - 5)
					addScS = false;
				
				double shift_ScS = 0.;
				if (select) {
					Trace obsS = obsData_T.createTrace().cutWindow(timeS - 15, timeS + 40);
					Trace noise = obsData_T.createTrace().cutWindow(timeS - 65, timeS - 15);
					double noiseValue = noise.getYVector().getLInfNorm();
					double signalValue = obsS.getYVector().getLInfNorm();
					double signalNoiseRatio = signalValue / noiseValue;
					
					Trace synScS = synData_T.createTrace().cutWindow(startTime_ScS, endTime_ScS);
					Trace obsScS = obsData_T.createTrace().cutWindow(startTime_ScS, endTime_ScS);
					shift_ScS = synScS.getXforMaxValue() - obsScS.getXforMaxValue();
					
					RealVector obsVector = obsData.createTrace().cutWindow(startTime_ScS - shift_ScS, endTime_ScS - shift_ScS).getYVector();
					RealVector synVector = new ArrayRealVector(Arrays.copyOf(synScS.getY(), obsVector.getDimension()));
					
					double corr = obsVector.dotProduct(synVector) / (obsVector.getNorm() * synVector.getNorm());
					double ratio = synVector.getLInfNorm() / obsVector.getLInfNorm();
					double variance = synVector.subtract(obsVector)
							.dotProduct(synVector.subtract(obsVector))
							/ obsVector.dotProduct(obsVector);
					
					boolean keep = corr >= minCorr && ratio < maxRatio && ratio > 1./maxRatio && variance < maxVariance
							&& signalNoiseRatio > minSNratio;
					addScS = addScS && keep;
				
					// eliminate records that are clearly problematic
					if (ratio > 10 || ratio < 0.1)
						return;
				}
				
				TimewindowInformation timewindow_S = new TimewindowInformation(timeS - 15, timeS + 40,
						obsHeader.getStation(), obsHeader.getGlobalCMTID(),
						SACComponent.T, new Phase[] {Phase.S});
				infoSetS.add(timewindow_S);
				
				TimewindowInformation timewindow_P = new TimewindowInformation(timeP - 5, timeP + 20,
						obsHeader.getStation(), obsHeader.getGlobalCMTID(),
						SACComponent.Z, new Phase[] {Phase.P});
				infoSetP.add(timewindow_P);
				
				//traveltimes P
				double deltaTimeP2P_P = Math.abs(synTrace_P.getXforMaxValue() - synTrace_P.getXforMinValue());
				double timeLatePeak_P = synTrace_P.getXforMaxValue() > synTrace_P.getXforMinValue() ? synTrace_P.getXforMaxValue() : synTrace_P.getXforMinValue();
				double timeEndOfP = timeLatePeak_P + deltaTimeP2P_P * 1.;
				
				double startTime_PcP = timePcP - timeBeforePcP;
				double endTime_PcP = timePcP + timeAfterPcP;
				
				if (startTime_PcP < timeEndOfP)
					startTime_PcP = timeEndOfP;
				if (endTime_PcP > timepP - timeBeforepP)
					endTime_PcP = timepP - timeBeforepP;
				
				if (timeEndOfP > timePcP - 2)
					addPcP = false;
				
				if (endTime_PcP - startTime_PcP < 15) {
//					System.err.println("Record shorter than 10 s or incorrect peak detection " + deltaTimeP2P_P + ", " + endTime_PcP + " to " + startTime_PcP);
					return;
				}
				
				double shift_PcP = shift_ScS * 0.2;
				if (select) {
					Trace obsP = obsData.createTrace().cutWindow(timeP - 5, timeP + 20);
					Trace noise = obsData.createTrace().cutWindow(timeP - 60, timeP - 10);
					double noiseValue = noise.getYVector().getLInfNorm();
					double signalValue = obsP.getYVector().getLInfNorm();
					double signalNoiseRatio = signalValue / noiseValue;
					
					Trace obsPcPcentered = obsData.createTrace().cutWindow(timePcP - 2, timePcP + 10);
					Trace noisePcP = obsData.createTrace().cutWindow(timePcP + 10, timePcP + 30);
					noiseValue = noisePcP.getYVector().getLInfNorm();
					signalValue = obsPcPcentered.getYVector().getLInfNorm();
					double signalNoiseRatioPcP = signalValue / noiseValue;
					
					Trace synPcP = synData.createTrace().cutWindow(startTime_PcP, endTime_ScS);
					Trace obsPcP = obsData.createTrace().cutWindow(startTime_PcP, endTime_ScS);
					shift_ScS = synPcP.getXforMaxValue() - obsPcP.getXforMaxValue();
					
					RealVector obsVector = obsData.createTrace().cutWindow(startTime_PcP - shift_PcP, endTime_PcP - shift_PcP).getYVector();
					RealVector synVector = new ArrayRealVector(Arrays.copyOf(synPcP.getY(), obsVector.getDimension()));
					
					double corr = obsVector.dotProduct(synVector) / (obsVector.getNorm() * synVector.getNorm());
					double ratio = synVector.getLInfNorm() / obsVector.getLInfNorm();
					double variance = synVector.subtract(obsVector)
							.dotProduct(synVector.subtract(obsVector))
							/ obsVector.dotProduct(obsVector);
					
					boolean keep = corr >= minCorr && ratio < maxRatio && ratio > 1./maxRatio && variance < maxVariance
							&& signalNoiseRatio > minSNratio;
					addPcP = addPcP && keep;
					
					addPcPstrict = addPcP && signalNoiseRatioPcP > minSNratio;
				}
				
				TimewindowInformation timewindow_ScS = new TimewindowInformation(startTime_ScS, endTime_ScS,
						obsHeader.getStation(), obsHeader.getGlobalCMTID(),
						SACComponent.T, new Phase[] {Phase.ScS});
				
				TimewindowInformation timewindow_PcP = new TimewindowInformation(startTime_PcP, endTime_PcP,
						obsHeader.getStation(), obsHeader.getGlobalCMTID(),
						SACComponent.Z, new Phase[] {Phase.PcP});
				
				if (addPcP)
					infoSetSelectedPcP.add(timewindow_PcP);
				if (addScS)
					infoSetSelectedScS.add(timewindow_ScS);
				if (addPcPstrict)
					infoSetStrictSelectedPcP.add(timewindow_PcP);
				
				infoSetPcP.add(timewindow_PcP);
				infoSetScS.add(timewindow_ScS);
			});
			} catch (IOException e) {
				e.printStackTrace();
			}
//		}, 10, TimeUnit.HOURS);
		}
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}
	
	public Set<TimewindowInformation> getInfoSetPcP() {
		return infoSetPcP;
	}

}
