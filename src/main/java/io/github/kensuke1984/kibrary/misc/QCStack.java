package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

public class QCStack {

	public static void main(String args[]) {
		Path rootPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/Inv");
		Path stationInformationPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/station_all.inf");
		Map<GlobalCMTID, RealVector> synTraces = new HashMap<>();
		Map<GlobalCMTID, RealVector> obsTraces = new HashMap<>();
		Map<GlobalCMTID, RealVector> bornTraces = new HashMap<>();
		Map<GlobalCMTID, double[]> timewindows = new HashMap<>();
		double tBefore0 = 50; // seconds
		double tAfter0 = 150;
		int bornOrder = 6;
		
//		for (double k=70; k< 100; k++) {
			
		double minDistance = 95;
		double maxDistance = 96;
//		GlobalCMTID[] ids = {new GlobalCMTID("200609160222A")};
		GlobalCMTID gid = new GlobalCMTID("200806292053A");
		
			
//		for (int k=0; k< 31; k++) {
//			double minDis =  k;
//			double maxDis = minDis + 1;
//			System.out.println(minDis);
//		for (GlobalCMTID gid : ids) {
		try {
			TauP_Time timeTool = new TauP_Time("prem");
			timeTool.parsePhaseList("S");
			Set<Station> stations = StationInformationFile.read(stationInformationPath);
			InversionResult ir = new InversionResult(rootPath);
		
			ir.getBasicIDList().stream()
				.filter(id -> id.getGlobalCMTID().equals(gid))
				.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
				.filter(id -> {
					double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
							stations.stream().filter(sta -> sta.equals(id.getStation()))
								.findAny().get().getPosition()) * 180 / Math.PI;
					return distance <= maxDistance && distance >= minDistance;
				})
				.forEach(id -> {
					double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
							stations.stream().filter(sta -> sta.equals(id.getStation()))
								.findAny().get().getPosition()) * 180 / Math.PI;			
					
					try {
					 Trace obs = ir.observedOf(id);
					 
					try {
						timeTool.depthCorrect(Earth.EARTH_RADIUS - id.getGlobalCMTID().getEvent().getCmtLocation().getR());
						timeTool.calculate(distance);
					} catch (TauModelException e) {
						e.printStackTrace();
					}
					
					if (timeTool.getNumArrivals() > 1)
						System.out.println("Warning: more than one S arrival for " + id.toString());
					if (! timewindows.containsKey(id.getGlobalCMTID())) {
						double tBefore = Math.min(tBefore0, timeTool.getArrival(0).getTime() - obs.getX()[0]);
						double tAfter = Math.min(tAfter0, obs.getX()[obs.getLength() - 1] - timeTool.getArrival(0).getTime());
						timewindows.put(id.getGlobalCMTID(), new double[] {tBefore, tAfter});
					}
					else {
						double tBefore = Math.min(timewindows.get(id.getGlobalCMTID())[0]
								, timeTool.getArrival(0).getTime() - obs.getX()[0]);
						double tAfter = Math.min(timewindows.get(id.getGlobalCMTID())[1], obs.getX()[obs.getLength() - 1] - timeTool.getArrival(0).getTime());
						timewindows.replace(id.getGlobalCMTID(), new double[] {tBefore, tAfter});
					}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			
			ir.getBasicIDList().stream()
				.filter(id -> id.getGlobalCMTID().equals(gid))
				.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
				.filter(id -> {
					double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
							stations.stream().filter(sta -> sta.equals(id.getStation()))
								.findAny().get().getPosition()) * 180 / Math.PI;
					return distance <= maxDistance && distance >= minDistance;
				})
				.forEach(id -> {
				try {
					double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
							stations.stream().filter(sta -> sta.equals(id.getStation()))
								.findAny().get().getPosition()) * 180 / Math.PI;
					try {
						timeTool.depthCorrect(Earth.EARTH_RADIUS - id.getGlobalCMTID().getEvent().getCmtLocation().getR());
						timeTool.calculate(distance);
					} catch (TauModelException e) {
						e.printStackTrace();
					}
					if (timeTool.getNumArrivals() > 1)
						System.out.println("Warning: more than one S arrival for " + id.toString());
					
					double start = timeTool.getArrival(0).getTime() - timewindows.get(id.getGlobalCMTID())[0];
					double end = timeTool.getArrival(0).getTime() + timewindows.get(id.getGlobalCMTID())[1];
					
					if (start < id.getStartTime() || end > id.getStartTime() + id.getNpts() / id.getSamplingHz()) {
						throw new IndexOutOfBoundsException("timewindow shorter than the stack time window "
								+ start + ":" + end + " < " + id.getStartTime() + ":" + (id.getStartTime() + id.getNpts() / id.getSamplingHz())
								+ " for event " + id.getGlobalCMTID().toString()
								);
					}
					
					int istart = (int) ((start - id.getStartTime()) * id.getSamplingHz());
					int iIncr = (int) ((end - start) * id.getSamplingHz());
					
					RealVector obs = ir.observedOf(id).getYVector().getSubVector(istart, iIncr);
					RealVector syn = ir.syntheticOf(id).getYVector().getSubVector(istart, iIncr);
					RealVector born = ir.bornOf(id, InverseMethodEnum.CONJUGATE_GRADIENT, bornOrder).getYVector().getSubVector(istart, iIncr);
					
					if (synTraces.containsKey(id.getGlobalCMTID())) {			//ここでstack
						synTraces.replace(id.getGlobalCMTID(), syn.add(synTraces.get(id.getGlobalCMTID())));
						obsTraces.replace(id.getGlobalCMTID(), obs.add(obsTraces.get(id.getGlobalCMTID())));
						bornTraces.replace(id.getGlobalCMTID(), born.add(bornTraces.get(id.getGlobalCMTID())));
					}
					else {
						synTraces.put(id.getGlobalCMTID(), syn);
						obsTraces.put(id.getGlobalCMTID(), obs);
						bornTraces.put(id.getGlobalCMTID(), born);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}  catch (TauModelException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		synTraces.forEach( (event, synTrace) -> {
			try {
				Path outPath = Paths.get(rootPath.toString(), "stack", event.toString() + minDistance +"-"+ maxDistance +".txt");
				Files.write(outPath, "time from start of timewinow, synthetics, observed, born\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				for (int i=0; i < synTrace.getDimension(); i++)
					Files.write(outPath, (String.join(" ", String.valueOf(i)
							, String.valueOf(synTrace.getEntry(i) / synTrace.getMaxValue())
							, String.valueOf(obsTraces.get(event).getEntry(i) / synTrace.getMaxValue())
							, String.valueOf(bornTraces.get(event).getEntry(i)/ synTrace.getMaxValue())) 
							+ "\n").getBytes(), StandardOpenOption.APPEND);
				outPath = Paths.get(rootPath.toString(), "stack", event.toString() + ".plt");
				Files.write(outPath, String.join("\n"
						, "set terminal postscript enhanced color font \"Helvetica,26pt\""
						, "set xtics " + String.format("%.0f", synTrace.getDimension() / 4.) + " nomirror"
						, "set ytics " + String.format("%.0f", Math.max(Math.max(synTrace.getMaxValue(), Math.abs(synTrace.getMinValue()))
								, Math.max(obsTraces.get(event).getMaxValue(), Math.abs(obsTraces.get(event).getMinValue())))
								/ 2) + " nomirror"
						, "set title \"" + event + "\""
						, "set output \"" + event + ".ps\""
						, "p \"" + event + ".txt\" u 1:3 w lines lw 1 lc rgb \"black\" ti \"observed\",\\"
						, "\"" + event + ".txt\" u 1:2 w lines lw 1 lc rgb \"blue\" ti \"synthetics\",\\"
						, "\"" + event + ".txt\" u 1:4 w lines lw 1  lc rgb \"red\" ti \"born\"" 
						).getBytes()
						, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
			});
	}
//	}
//	}
		
}
