package io.github.kensuke1984.kibrary.util.statistics;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SeismicPhase;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.TauP.TimeDist;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.EventCluster;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACExtension;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

public class ComputeTimediffSScS {

	public static void main(String[] args) throws IOException, TauModelException {
		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s");
		Path timewindowPath = workdir.resolve("selectedTimewindow_ScS_70deg.dat");
		
		Path clusterfilePath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf");
		
		List<EventCluster> clusters = EventCluster.readClusterFile(clusterfilePath);
		
		Path mantleCorrectionFile = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/mantleCorrection_S-ScS_semucb.dat");
		
		Set<StaticCorrection> mantleCorrections = StaticCorrectionFile.read(mantleCorrectionFile);
		
//		mantleCorrections = null;
		
//		workdir = Paths.get("/work/anselme/CA_ANEL_NEW/synthetic_s0/filtered_stf_12.5-200s");
//		timewindowPath = workdir.resolve("selectedTimewindow_ScScutS_cc05.dat");
		
//		Path timewindowPath = Paths.get(args[0]);
		
		double minCorrScS = 0.5;
		double maxVarScS = 0.8;
		
		Path outpath = workdir.resolve("differential_cluster.txt");
		Files.deleteIfExists(outpath);
		PrintWriter pw = new PrintWriter(outpath.toFile());
		
		Path outdir = workdir.resolve("picked");
		if (!Files.exists(outdir))
			Files.createDirectory(outdir);
		
		int np = 512;
		double tlen = 1638.4;
		double samplingHz = 20;
		
		double minDistance = 30.;
		double maxDistance = 100.;
		
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath).stream()
			.filter(tw -> {
					double distance = Math.toDegrees(tw.getGlobalCMTID().getEvent().getCmtLocation()
							.getEpicentralDistance(tw.getStation().getPosition()));
					if (distance < minDistance || distance > maxDistance)
						return false;
					return true;
				})
			.collect(Collectors.toSet());
		
//		timewindows.removeIf(tw -> !tw.getGlobalCMTID().equals(new GlobalCMTID("201007261731A")));
		
		Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(workdir);
		
		TauP_Time timetool = new TauP_Time("prem");
		timetool.parsePhaseList("S, ScS");
		
		for (EventFolder eventFolder : eventFolderSet) {
			System.out.println(eventFolder.getGlobalCMTID());
			
			EventCluster cluster = clusters.stream().filter(c -> c.getID().equals(eventFolder.getGlobalCMTID())).findFirst().get();
			int index = cluster.getIndex();
			
			Path eventPath = outdir.resolve(eventFolder.getGlobalCMTID().toString());
			if (!Files.exists(eventPath))
				Files.createDirectory(eventPath);
			
			timetool.setSourceDepth(6371. - eventFolder.getGlobalCMTID().getEvent().getCmtLocation().getR());
			
			Set<TimewindowInformation> thisWindows = timewindows.stream()
					.filter(tw -> tw.getGlobalCMTID().equals(eventFolder.getGlobalCMTID()))
					.collect(Collectors.toSet());
			
			Set<StaticCorrection> thisCorrections = null;
			if (mantleCorrections != null)
				thisCorrections = mantleCorrections.stream()
					.filter(c -> c.getGlobalCMTID().equals(eventFolder.getGlobalCMTID()))
					.collect(Collectors.toSet());
			
			Set<SACFileName> obsNames = eventFolder.sacFileSet();
			obsNames.removeIf(sfn -> !sfn.isOBS());
			obsNames.removeIf(obsName -> 
				thisWindows.stream().filter(tw -> tw.getGlobalCMTID().equals(obsName.getGlobalCMTID())
						&& tw.getStation().getName().equals(obsName.getStationName())
						&& tw.getComponent().equals(obsName.getComponent()))
				.count() != 1
			);
			SACFileName[] obsNamesArray = obsNames.toArray(new SACFileName[obsNames.size()]);
			
			SACData[] obsSacs = new SACData[obsNames.size()];
			SACData[] synSacs = new SACData[obsNames.size()];
			double[] timeS = new double[obsNames.size()];
			double[] timeScS = new double[obsNames.size()];
			double[] pierceDists = new double[obsNames.size()];
			int[] az_cluster_index = new int[obsNames.size()]; 
			
			int count = 0;
			for (SACFileName obsName : obsNamesArray) {
				SACFileName synName = new SACFileName(obsName.getAbsolutePath().concat("sc"));
				obsSacs[count] = obsName.read();
				synSacs[count] = synName.read();
				
				double distance = obsSacs[count].getValue(SACHeaderEnum.GCARC);
				timetool.calculate(distance);
				Arrival arrivalS = timetool.getArrival(0);
				Arrival arrivalScS = timetool.getArrival(1);
				if (!arrivalS.getPhase().getName().equals("S") || !arrivalScS.getPhase().getName().equals("ScS")) {
					System.err.println("Problem computing S or ScS " + obsName.getName());
					continue;
				}
				timeS[count] = arrivalS.getTime();
				timeScS[count] = arrivalScS.getTime();
				
				TimeDist[] pierces = arrivalScS.getPierce();
				for (TimeDist p : pierces) {
					if (Math.abs(p.getDepth() - 2891.) < 1) {
						pierceDists[count] = p.getDistDeg();
						break;
					}
				}
				
				double azimuth = obsSacs[count].getValue(SACHeaderEnum.AZ);
				for (int iaz = 0; iaz < cluster.getNAzimuthSlices(); iaz++) {
					double[] bounds = cluster.getAzimuthBound(iaz);
					if (bounds[0] <= azimuth && bounds[1] > azimuth) az_cluster_index[count] = iaz;
				}
				
				count++;
			}
			
			for (int i = 0; i < obsSacs.length; i++) {
				Trace synS = synSacs[i].createTrace().cutWindow(timeS[i] - 10, timeS[i] + 30).removeTrend();
				Trace synScS = synSacs[i].createTrace().cutWindow(timeScS[i] - 10, timeScS[i] + 30).removeTrend();
				Trace obsS = obsSacs[i].createTrace().cutWindow(timeS[i] - 10, timeS[i] + 30).removeTrend();
				Trace obsScS = obsSacs[i].createTrace().cutWindow(timeScS[i] - 10, timeScS[i] + 30).removeTrend();
				
				int synUpS = synS.getIndexOfUpwardConvex()[0];
				int synDownS = synS.getIndexOfDownwardConvex()[0];
				int synUpScS = synScS.getIndexOfUpwardConvex()[0];
				int synDownScS = synScS.getIndexOfDownwardConvex()[0];
				
				double tSynDownS = synS.getXAt(synDownS);
				double tSynUpS = synS.getXAt(synUpS);
				double synWidthS = Math.abs(tSynUpS - tSynDownS);
				
				double synDeltaAS = synS.getYAt(synUpS) - synS.getYAt(synDownS);
				double synDeltaAScS = synScS.getYAt(synUpScS) - synScS.getYAt(synDownScS);
				
				//-------------------
				double tUpScS = synScS.getXAt(synUpScS);
				double tDownScS = synScS.getXAt(synDownScS);
				double t0ScS = 0;
				double t1ScS = 0;
				if (tUpScS < tDownScS) {
					t0ScS = tUpScS - (tDownScS - tUpScS) * 0.75;
					t1ScS = tDownScS + (tDownScS - tUpScS) * 0.75;
				}
				else {
					t1ScS = tUpScS + (-tDownScS + tUpScS) *0.75;
					t0ScS = tDownScS - (-tDownScS + tUpScS) *0.75;
				}
				
				boolean positivePolarity = synUpS < synDownS;
				
				double tSynScS = 0;
				double tSynS = 0;
				if (positivePolarity) {
					tSynScS = obsScS.getXAt(synUpScS);
					tSynS = obsS.getXAt(synUpS);
				}
				else {
					tSynScS = obsScS.getXAt(synDownScS);
					tSynS = obsS.getXAt(synDownS);
				}
				
				
				Trace templateScS = synSacs[i].createTrace().cutWindow(t0ScS, t1ScS);
				
				int synMeanS = (synUpS + synDownS) / 2;
				int synMeanScS = (synUpScS + synDownScS) / 2;
				double synSScSratio = synDeltaAScS / synDeltaAS;
				
//				double shift = obsSacs[i].createTrace().cutWindow(t0ScS - 10.5, t1ScS + 10.5).findBestShift(templateScS);
//				double tBestCorr = t0ScS + shift;
//				System.out.println(obsSacs[i].getGlobalCMTID() + " " + obsSacs[i].getStation() + " " + shift);
				
				int obsUpS = obsS.getIndexOfUpwardConvex()[0];
				int obsDownS = obsS.getIndexOfDownwardConvex()[0];
				int obsUpScS = obsScS.getIndexOfUpwardConvex()[0];
				int obsDownScS = obsScS.getIndexOfDownwardConvex()[0];
				
				double tObsUpS = obsS.getXAt(obsUpS);
				double tObsDownS = obsS.getXAt(obsDownS);
				double obsWidthS = Math.abs(tObsUpS - tObsDownS);
				
				double tObsScS = 0;
				double tObsS = 0;
				double obsSScSratio = 0.;
				if (obsScS.getYAt(obsUpScS) > -obsScS.getYAt(obsDownScS)) {
					tObsScS = obsScS.getXAt(obsUpScS);
					tObsS = obsS.getXAt(obsUpS);
					obsSScSratio = obsScS.getYAt(obsUpScS) / obsS.getYAt(obsUpS);
					
					tSynS = synS.getXAt(synUpS);
					tSynScS = synScS.getXAt(synUpScS);
				}
				else {
					tObsScS = obsScS.getXAt(obsDownScS);
					tObsS = obsS.getXAt(obsDownS);
					obsSScSratio = obsScS.getYAt(obsDownScS) / obsS.getYAt(obsDownS);
					
					tSynS = synS.getXAt(synDownS);
					tSynScS = synScS.getXAt(synDownScS);
				}
					
				
				Path sacPath = eventPath.resolve(obsNamesArray[i].getName() + "sc");
				synSacs[i].setTimeMarker(SACHeaderEnum.T2, tSynDownS)
					.setTimeMarker(SACHeaderEnum.T3, tSynUpS)
					.setTimeMarker(SACHeaderEnum.T4, tSynScS)
//					.setTimeMarker(SACHeaderEnum.T5, synScS.getXAt(synDownScS))
//					.setTimeMarker(SACHeaderEnum.T6, t0ScS)
//					.setTimeMarker(SACHeaderEnum.T7, t1ScS)
//					.setTimeMarker(SACHeaderEnum.T8, (synScS.getXAt(synUpScS) + synScS.getXAt(synDownScS)) / 2.)
					.writeSAC(sacPath);
				
//				double obsDeltaAS = obsS.getYAt(obsUpS) - obsS.getYAt(obsDownS);
//				double obsDeltaAScS = obsScS.getYAt(obsUpScS) - obsScS.getYAt(obsDownScS);
				
				
				
//				boolean goodScS = true;
//				if (positivePolarity) {
//					if (obsUpScS > obsDownScS) {
//						goodScS = false;
//					}
//					tObsScS = obsScS.getXAt(obsUpScS);
//					tObsS = obsS.getXAt(obsDownS);
//				}
//				else {
//					if (obsDownScS > obsUpScS) {
//						goodScS = false;
//					}
//					tObsScS = obsScS.getXAt(obsDownScS);
//					tObsS = obsS.getXAt(obsDownS);
//				}
				
				
				sacPath = eventPath.resolve(obsNamesArray[i].getName());
				obsSacs[i].setTimeMarker(SACHeaderEnum.T2, tObsS)
					.setTimeMarker(SACHeaderEnum.T4, tObsScS)
					.writeSAC(sacPath);
				
				int obsMeanS = (obsUpS + obsDownS) / 2;
				int obsMeanScS = (obsUpScS + obsDownScS) / 2;
//				double obsSScSratio = obsDeltaAScS / obsDeltaAS;
				
//				double deltaTS = (obsMeanS - synMeanS) / (double) samplingHz;
//				double deltaTScS = (obsMeanScS - synMeanScS) / (double) samplingHz;
//				double deltaTSScS = deltaTS - deltaTScS;
//				double ratioSScS = synSScSratio / obsSScSratio;
				
				double deltaTS = tObsS - tSynS;
				double deltaTScS = tObsScS - tSynScS;
				double deltaTSScS = deltaTS - deltaTScS;
				double ratioSScS = synSScSratio / obsSScSratio;
				
				double widthSratio = synWidthS / obsWidthS;
				
				//
				double evtLat = obsSacs[i].getValue(SACHeaderEnum.EVLA);
				double evtLon = obsSacs[i].getValue(SACHeaderEnum.EVLO);
				double azimuth = obsSacs[i].getValue(SACHeaderEnum.AZ);
				
				double lat = SphericalCoords.latFor(evtLat, evtLon, pierceDists[i], azimuth);
				double lon = SphericalCoords.lonFor(evtLat, evtLon, pierceDists[i], azimuth);
				
				HorizontalPosition midPoint = new HorizontalPosition(lat, lon);
				
				//correlation
				Trace tmpObsScS = obsSacs[i].createTrace().cutWindow(t0ScS + tObsScS - tSynScS, t1ScS + tObsScS - tSynScS)
						.truncateToLength(templateScS.getLength());
				double correlation = tmpObsScS.correlation(templateScS);
				double var = tmpObsScS.getYVector().subtract(templateScS.getYVector())
						.dotProduct(tmpObsScS.getYVector().subtract(templateScS.getYVector()))
						/ tmpObsScS.getYVector().dotProduct(tmpObsScS.getYVector());
				
				SACData tmpsac = obsSacs[i];
				double corrShift = 0;
				if (mantleCorrections != null) {
					StaticCorrection mantleCorr = thisCorrections.stream().filter(c -> c.getStation().equals(tmpsac.getStation()))
						.findFirst().get();
					corrShift = mantleCorr.getTimeshift();
				}
				double deltaTSScSCorr = deltaTSScS - corrShift;
				
				if (correlation > minCorrScS && var < maxVarScS)
					pw.println(obsSacs[i].getGlobalCMTID() + " " + obsSacs[i].getStation() + " " + 
						midPoint.getLatitude() + " " + midPoint.getLongitude() + " " + deltaTSScS + " " 
							+ deltaTSScSCorr + " " + ratioSScS + " " + widthSratio + " " 
							+ "index_" + index + " " + "az_" + az_cluster_index[i] + " " + obsSacs[i].getValue(SACHeaderEnum.GCARC));
//				System.out.println(obsSacs[i].getGlobalCMTID() + " " + obsSacs[i].getStation() + " " + deltaTSScS + " " + correlation + " " + var);
			}
			pw.flush();
		}
		pw.close();
	}

}
