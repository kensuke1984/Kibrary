/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
//import manhattan.timewindow.Timewindow;
//import manhattan.timewindow.TimewindowInformation;
//import manhattan.timewindow.TimewindowInformationFile;
//import recordSection.RecordSectionStack;

/**
 * @version 0.0.1
 * @since 2016/10/26
 * @author Yuki
 *
 */
public class QCrecordSection {
	final static int minLat = 20;
	final static int maxLat = 90;
	final static int minLon = 120;
	final static int maxLon = 300;
	final static double deltaAZ = 3.;
	final static double AZref = 336;
	
	public static void main (String args[]) {
	Path rootPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/Inv");
	Path stationInformationPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/station_all.inf");
//	Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(Paths.get());
	
	Map<GlobalCMTID, RealVector> synTraces = new HashMap<>();
	Map<GlobalCMTID, RealVector> obsTraces = new HashMap<>();
	Map<GlobalCMTID, RealVector> bornTraces = new HashMap<>();
	Map<GlobalCMTID, double[]> timewindows = new HashMap<>();
	

	int bornOrder = 6;
	double minDistance = 70;
	double maxDistance = 100;
	double distanceIncrement = 1.0;
	double tBefore0 = 50; // seconds
	double tAfter0 = 150;
	GlobalCMTID gcmtid = new GlobalCMTID("200806292053A");
	double dt = .05;
	
	try {
		File dir = new File("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/Inv/QCRS/"+gcmtid);
		TauP_Time timeTool = new TauP_Time("prem");
		timeTool.parsePhaseList("S");
		Set<Station> stations = StationInformationFile.read(stationInformationPath);
		InversionResult ir = new InversionResult(rootPath);
		RealVector[] stacksObs = new RealVector[(int) (120./distanceIncrement)];
		RealVector[] stacksSyn = new RealVector[(int) (120./distanceIncrement)];
		RealVector[] stacksBorn = new RealVector[(int) (120./distanceIncrement)];
		int[] numOFStack = new int[(int) (120./distanceIncrement)];
		Set<PosixFilePermission> perms =
		         EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
		        		 , PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ);
		Files.createDirectory(dir.toPath(), PosixFilePermissions.asFileAttribute(perms));
		Path outpathscriptstack = Paths.get(dir.getAbsolutePath(), "stackprofile.plt");
		Path outPath = Paths.get(dir.getAbsolutePath(), gcmtid.toString() + ".txt");
		Path outPathPltScript = Paths.get(dir.getAbsolutePath(), "profile.plt");
		Path gmtMapScript = Paths.get(dir.getAbsolutePath(), "makemap.sh");
		Path outStackScript = Paths.get(dir.getAbsolutePath(), "stackprofile.plt");
		Files.createFile(outPath);
		
		
		
		plotScriptInitialize(outPathPltScript, gmtMapScript, outStackScript, minLat, maxLat, minLon, maxLon, dir);
//		timewindows.stream()
//				.filter(tw -> tw.getGlobalCMTID().toString().equals(dir.toString()))
//				.forEachOrdered(tw -> thisEventTimewindow.add(tw));
		
		
		ir.getBasicIDList().stream()
		.filter(id -> id.getGlobalCMTID().equals(gcmtid))
//		.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
		.filter(id -> {
			double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
					stations.stream().filter(sta -> sta.equals(id.getStation()))
						.findAny().get().getPosition()) * 180 / Math.PI;
//			System.out.println(distance);
			return distance <= maxDistance && distance >= minDistance;
		})
		.forEach(id -> {
//			System.out.println(id);
		try {
			double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
				   stations.stream().filter(sta -> sta.equals(id.getStation()))
						.findAny().get().getPosition()) * 180 / Math.PI;
			Trace obsTrace = ir.observedOf(id);
			
			try {
				timeTool.depthCorrect(Earth.EARTH_RADIUS - id.getGlobalCMTID().getEvent().getCmtLocation().getR());
				timeTool.calculate(distance);
			} catch (TauModelException e) {
				e.printStackTrace();
			}
			
			if (! timewindows.containsKey(id.getGlobalCMTID())) {
				double tBefore = Math.min(tBefore0, timeTool.getArrival(0).getTime() - obsTrace.getX()[0]);
				double tAfter = Math.min(tAfter0, obsTrace.getX()[obsTrace.getLength() - 1] - timeTool.getArrival(0).getTime());
				timewindows.put(id.getGlobalCMTID(), new double[] {tBefore, tAfter});
//				System.out.println(tBefore+ " "+tAfter);
			}
			else {
				double tBefore = Math.min(timewindows.get(id.getGlobalCMTID())[0]
						, timeTool.getArrival(0).getTime() - obsTrace.getX()[0]);
				double tAfter = Math.min(timewindows.get(id.getGlobalCMTID())[1], obsTrace.getX()[obsTrace.getLength() - 1] - timeTool.getArrival(0).getTime());
//				System.out.println(tBefore+ " "+tAfter);
				timewindows.replace(id.getGlobalCMTID(), new double[] {tBefore, tAfter});
			}
			
			
//			System.out.println(distance);
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
//			System.out.println(start+ " "+end);
			if (start < id.getStartTime() || end > id.getStartTime() + id.getNpts() / id.getSamplingHz()) {
				throw new IndexOutOfBoundsException("timewindow shorter than the stack time window "
						+ start + ":" + end + " < " + id.getStartTime() + ":" + (id.getStartTime() + id.getNpts() / id.getSamplingHz())
						+ " for event " + id.getGlobalCMTID().toString()
						);
			}
			
			int istart = (int) ((start - id.getStartTime()) * id.getSamplingHz());
			int iIncr = (int) ((end - start) * id.getSamplingHz());
//			System.out.println(istart+" "+iIncr);
			
//			Trace obsTrace = ir.observedOf(id);
			Trace synTrace = ir.syntheticOf(id);
			Trace bornTrace = ir.bornOf(id, InverseMethodEnum.CONJUGATE_GRADIENT, bornOrder);
			RealVector obs = ir.observedOf(id).getYVector().getSubVector(istart, iIncr);
			RealVector syn = ir.syntheticOf(id).getYVector().getSubVector(istart, iIncr);
			RealVector born = ir.bornOf(id, InverseMethodEnum.CONJUGATE_GRADIENT, bornOrder).getYVector().getSubVector(istart, iIncr);
//			System.out.println(born.getDimension());
			
			double obsMax = FastMath.max(obs.getMaxValue(), FastMath.abs(obs.getMinValue()));
			double synMax = FastMath.max(syn.getMaxValue(), FastMath.abs(syn.getMinValue()));
			double bornMax = FastMath.max(born.getMaxValue(), FastMath.abs(born.getMinValue()));
			
			try {
				for (int i=0; i<obs.toArray().length / 4; i++) {	//TODO 4で割るのは？？
					Files.write(outPath, (String.valueOf(ir.observedOf(id).getXVector().getSubVector(istart, iIncr).toArray()[i])
							+" "
							+ String.valueOf(obsTrace.getY()[4*i] / obsMax)
							+" "
							+ String.valueOf(synTrace.getY()[4*i] / synMax)
							+" "
							+ String.valueOf(bornTrace.getY()[4*i] / bornMax)
							+ "\n").getBytes()
							, StandardOpenOption.APPEND);
				}
			} catch (IOException e) {e.printStackTrace();}
			
			int index = (int) (distance / distanceIncrement);
			if (stacksObs[index] == null && stacksSyn[index] == null && stacksBorn[index] == null) {
				stacksObs[index] = new ArrayRealVector(obs.mapDivide(obsMax));
				stacksSyn[index] = new ArrayRealVector(syn.mapDivide(synMax));
				stacksBorn[index] = new ArrayRealVector(born.mapDivide(bornMax));
			} else {
				for (int i = 0; i < FastMath.min(stacksObs[index].getDimension(), obs.getMaxIndex()); i++) {
					stacksObs[index].addToEntry(i, obsTrace.getY()[i] / obsMax);
					stacksSyn[index].addToEntry(i, synTrace.getY()[i] / synMax);
					stacksBorn[index].addToEntry(i, bornTrace.getY()[i] / bornMax);
				}
			}
			numOFStack[index] += 1;
//			System.out.println(numOFStack[index]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	});	
		
		for (int i=0; i < numOFStack.length; i++) {
			if (numOFStack[i] != 0) {
				Path outpathstack = Paths.get(dir.getAbsolutePath(), "stack" + String.format("%.1f", i * distanceIncrement) + ".T.txt");
				Files.deleteIfExists(outpathstack);
				Files.createFile(outpathstack);
				for (int j=0; j < stacksObs[i].getDimension(); j++)
					Files.write(outpathstack
							, (String.valueOf(j * dt *20)
									+ " "
									+ String.valueOf(stacksObs[i].getEntry(j) / numOFStack[i])
									+ " "
									+ String.valueOf(stacksSyn[i].getEntry(j) / numOFStack[i])
									+ " "
									+ String.valueOf(stacksBorn[i].getEntry(j) / numOFStack[i])
									+ "\n").getBytes()
							, StandardOpenOption.APPEND);					
					Files.write(outpathscriptstack, ("\'"
						+ "stack" + i * distanceIncrement + ".T.txt"
						+ "\'"
						+ " u 1:($2+"
						+ String.valueOf(i * distanceIncrement)
						+ ") "
						+ "w lines lt -1 lc rgb \"black\" "
						+ "lw .2,\\"
						+ "\n").getBytes()
						, StandardOpenOption.APPEND);
					Files.write(outpathscriptstack, ("\'"
							+ "stack" + i * distanceIncrement + ".T.txt"
							+ "\'"
							+ " u 1:($3+"
							+ String.valueOf(i * distanceIncrement)
							+ ") "
							+ "w lines lt -1 lc rgb \"green\" "
							+ "lw .2,\\"
							+ "\n").getBytes()
							, StandardOpenOption.APPEND);
					Files.write(outpathscriptstack, ("\'"
							+ "stack" + i * distanceIncrement + ".T.txt"
							+ "\'"
							+ " u 1:($4+"
							+ String.valueOf(i * distanceIncrement)
							+ ") "
							+ "w lines lt -1 lc rgb \"pink\" "
							+ "lw .2,\\"
							+ "\n").getBytes()
							, StandardOpenOption.APPEND);
			}
		}
		
	} catch (TauModelException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
	
	
	/**
	synTraces.forEach( (event, synTrace) -> {
		try {
			Path outPath = Paths.get(rootPath.toString(), "stack", event.toString() + ".txt");
			Files.write(outPath, "time from start of timewinow, synthetics, observed, born\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			for (int i=0; i < synTrace.getDimension(); i++)
				Files.write(outPath, (String.join(" ", String.valueOf(i)
						, String.valueOf(synTrace.getEntry(i))
						, String.valueOf(obsTraces.get(event).getEntry(i))
						, String.valueOf(bornTraces.get(event).getEntry(i))) 
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
	**/
	}
	
	public static void plotScriptInitialize (Path outPathPltScript, Path gmtMapScript, Path outStackScript,
			int minlat2, int maxlat2, int minlon2, int maxlon2, File dir) {
		try {
			Files.write(outPathPltScript, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
					,"unset key"
					,"set size .5,1" 
					,"set xlabel 'Reduced time (s)'"
					,"set ylabel 'Epicentral distance (deg)'"
					,"set output 'profile" + String.format("%.1f", AZref) + ".eps'\np").getBytes()
					, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.write(outStackScript, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
					,"unset key"
					,"set size .5,1"
					,"set xlabel 'Reduced time (s)'"
					,"set ylabel 'distance (deg)'"
					,"set output 'stackprofile" +  String.format("-%.1f", AZref) + ".eps'\np").getBytes()
					, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
