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
import java.util.EnumSet;
import java.util.HashMap;
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
/**
 * @version 0.0.1
 * @since 2017/01/7
 * @author Yuki
 *
 */

public class QCRS_azimuth {
	
	final static int minLat = 20;
	final static int maxLat = 90;
	final static int minLon = 120;
	final static int maxLon = 300;
	
	public static void main(String[] args) throws IOException {
		Path rootPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/Inv");
		Path stationInformationPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/station_all.inf");
//		Map<GlobalCMTID, RealVector> synTraces = new HashMap<>();
//		Map<GlobalCMTID, RealVector> obsTraces = new HashMap<>();
//		Map<GlobalCMTID, RealVector> bornTraces = new HashMap<>();
		Map<GlobalCMTID, double[]> timewindows = new HashMap<>();
		double tBefore0 = 50; // seconds
		double tAfter0 = 150;
		int bornOrder = 6;
		double minDistance = 70;
		double maxDistance = 100;
		double azimuthIncrement = 0.5;
		GlobalCMTID gid = new GlobalCMTID("201110210802A");
		Set<PosixFilePermission> perms =
		         EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
		        		 , PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ);
		File dir = new File("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/Inv/QCRS_CG3500AZ/"+gid);
		
		Files.createDirectory(dir.toPath(), PosixFilePermissions.asFileAttribute(perms));
		Path outPath = Paths.get(dir.getAbsolutePath(), gid.toString() + ".txt");
		Files.createFile(outPath);
		Path outPathPltScript = Paths.get(dir.getAbsolutePath(), "profile.plt");
		Path gmtMapScript = Paths.get(dir.getAbsolutePath(), "makemap.sh");
		Path outStackScript = Paths.get(dir.getAbsolutePath(), "stackprofile.plt");
		plotScriptInitialize(outPathPltScript, gmtMapScript, outStackScript, minLat, maxLat, minLon, maxLon, dir);
		double dt = .05;
		
		RealVector[] stacksObs = new RealVector[(int) (120./azimuthIncrement)];
		RealVector[] stacksSyn = new RealVector[(int) (120./azimuthIncrement)];
		RealVector[] stacksBorn = new RealVector[(int) (120./azimuthIncrement)];
		int[] numOFStack = new int[(int) (120./azimuthIncrement)];
		
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
					double azimuth =id.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(
							stations.stream().filter(sta -> sta.equals(id.getStation()))
							.findAny().get().getPosition()) * 180 / Math.PI;
					try {
						Trace obs = ir.observedOf(id);
//						Trace syn = ir.syntheticOf(id);
//						Trace born = ir.bornOf(id, InverseMethodEnum.CG, bornOrder);
						
						try {
							timeTool.depthCorrect(Earth.EARTH_RADIUS - id.getGlobalCMTID().getEvent().getCmtLocation().getR());
							timeTool.calculate(distance);
						} catch (TauModelException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
						
						if (timeTool.getNumArrivals() > 1)
							System.out.println("Warning: more than one S arrival for " + id.toString());
						
						if (! timewindows.containsKey(id.getGlobalCMTID())) {
							double tBefore = FastMath.min(tBefore0, timeTool.getArrival(0).getTime() - obs.getX()[0]);
							double tAfter = FastMath.min(tAfter0, obs.getX()[obs.getLength() - 1] - timeTool.getArrival(0).getTime());
							timewindows.put(id.getGlobalCMTID(), new double[] {tBefore, tAfter});
							System.out.println(tBefore+" "+ tAfter);
						}
						else {
							double tBefore = FastMath.min(timewindows.get(id.getGlobalCMTID())[0]
									, timeTool.getArrival(0).getTime() - obs.getX()[0]);
							double tAfter = FastMath.min(timewindows.get(id.getGlobalCMTID())[1], obs.getX()[obs.getLength() - 1] - timeTool.getArrival(0).getTime());
							timewindows.replace(id.getGlobalCMTID(), new double[] {tBefore, tAfter});
							System.out.println(tBefore+" "+ tAfter);
						}				
					} catch (IOException e) {
						// TODO 自動生成された catch ブロック
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
					double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
							stations.stream().filter(sta -> sta.equals(id.getStation()))
								.findAny().get().getPosition()) * 180 / Math.PI;
					double azimuth =id.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(
							stations.stream().filter(sta -> sta.equals(id.getStation()))
							.findAny().get().getPosition()) * 180 / Math.PI;
					try {
						timeTool.depthCorrect(Earth.EARTH_RADIUS - id.getGlobalCMTID().getEvent().getCmtLocation().getR());
						timeTool.calculate(distance);
						if (timeTool.getNumArrivals() > 1)
							System.out.println("Warning: more than one S arrival for " + id.toString());
						
					} catch (TauModelException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
					
					double start = timeTool.getArrival(0).getTime() - timewindows.get(id.getGlobalCMTID())[0];
					double end = timeTool.getArrival(0).getTime() + timewindows.get(id.getGlobalCMTID())[1];
					System.out.println(start+" "+end);
					
					if (start < id.getStartTime() || end > id.getStartTime() + id.getNpts() / id.getSamplingHz()) {
						throw new IndexOutOfBoundsException("timewindow shorter than the stack time window "
								+ start + ":" + end + " < " + id.getStartTime() + ":" + (id.getStartTime() + id.getNpts() / id.getSamplingHz())
								+ " for event " + id.getGlobalCMTID().toString()
								);
					}
					
					int istart = (int) ((start - id.getStartTime()) * id.getSamplingHz());
					int iIncr = (int) ((end - start) * id.getSamplingHz());
					System.out.println(istart+" "+iIncr);
					
					try {
						Trace obs = ir.observedOf(id);
						Trace syn = ir.syntheticOf(id);
						Trace born = ir.bornOf(id, InverseMethodEnum.CONJUGATE_GRADIENT, bornOrder);
						double obsMax = FastMath.max(obs.getMaxValue(), FastMath.abs(obs.getMinValue()));
						double synMax = FastMath.max(syn.getMaxValue(), FastMath.abs(syn.getMinValue()));
						double bornMax = FastMath.max(born.getMaxValue(), FastMath.abs(born.getMinValue()));
						
						for (int i=0; i < obs.getLength() / 4; i++) {
							Files.write(outPath, (String.valueOf(ir.observedOf(id).getXVector().getSubVector(istart, iIncr).toArray()[i])
									+" "
									+ String.valueOf(obs.getY()[4*i] / obsMax)
									+" "
									+ String.valueOf(syn.getY()[4*i] / obsMax)
									+" "
									+ String.valueOf(born.getY()[4*i] / obsMax)
									+ "\n").getBytes()
									, StandardOpenOption.APPEND);
						}
						
						int index = (int) (azimuth / azimuthIncrement);
//						if (id.)
						if (stacksObs[index] == null) {
							stacksObs[index] = new ArrayRealVector(obs.getYVector().mapDivide(obsMax));
							stacksSyn[index] = new ArrayRealVector(syn.getYVector().mapDivide(obsMax));
							stacksBorn[index] = new ArrayRealVector(born.getYVector().mapDivide(obsMax));
						}
						else {
							for (int i=0; i < FastMath.min(stacksObs[index].getDimension(), obs.getLength()); i++) {
								stacksObs[index].addToEntry(i, obs.getY()[i] / obsMax);
								stacksSyn[index].addToEntry(i, syn.getY()[i] / obsMax);
								stacksBorn[index].addToEntry(i, born.getY()[i] / obsMax);
							}
							numOFStack[index] += 1;
						}
					} catch (IOException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}									
				});
			
			for (int i=0; i < numOFStack.length; i++) {
				if (numOFStack[i] != 0) {
					Path outpathstack = Paths.get(dir.getAbsolutePath(), "stack" + String.format("%.1f", i * azimuthIncrement) + ".T.txt");
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
										+ " "
										+ String.valueOf(numOFStack[i])
										+ "\n").getBytes()
								, StandardOpenOption.APPEND);					
						Files.write(outStackScript, ("\'"
							+ "stack" + i * azimuthIncrement + ".T.txt"
							+ "\'"
							+ " u 1:($2+"
							+ String.valueOf(i * azimuthIncrement)
							+ ") "
							+ "w lines lt -1 lc rgb \"black\" "
							+ "lw .5,\\"
							+ "\n").getBytes()
							, StandardOpenOption.APPEND);
						Files.write(outStackScript, ("\'"
								+ "stack" + i * azimuthIncrement + ".T.txt"
								+ "\'"
								+ " u 1:($3+"
								+ String.valueOf(i * azimuthIncrement)
								+ ") "
								+ "w lines lt -1 lc rgb \"green\" "
								+ "lw .5,\\"
								+ "\n").getBytes()
								, StandardOpenOption.APPEND);
						Files.write(outStackScript, ("\'"
								+ "stack" + i * azimuthIncrement + ".T.txt"
								+ "\'"
								+ " u 1:($4+"
								+ String.valueOf(i * azimuthIncrement)
								+ ") "
								+ "w lines lt -1 lc rgb \"pink\" "
								+ "lw .5,\\"
								+ "\n").getBytes()
								, StandardOpenOption.APPEND);
				}
			}			
		} catch (TauModelException e ) {e.printStackTrace();}
	}
	
	public static void plotScriptInitialize (Path outPathPltScript, Path gmtMapScript, Path outStackScript,
			int minlat2, int maxlat2, int minlon2, int maxlon2, File dir) {
		try {
			Files.write(outPathPltScript, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
					,"unset key"
					,"set size .5,1" 
					,"set xlabel 'Reduced time (s)'"
					,"set ylabel 'Epicentral distance (deg)'"
					,"set output 'profile" + ".eps'\np").getBytes()
					, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.write(outStackScript, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
					,"unset key"
					,"set size .5,1"
					,"set xlabel 'Reduced time (s)'"
					,"set ylabel 'Azimuth (deg)'"
					,"set xtics 20"
					,"set mxtics 2"
					,"set ytics 5"
					,"set output 'stackprofile_" + dir.getName() + ".eps'\np").getBytes()
					, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
