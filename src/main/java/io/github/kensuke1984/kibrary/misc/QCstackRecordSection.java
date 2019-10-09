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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
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

/**
 * @version 0.0.1
 * @since 2016/11/16
 * @author Yuki
 *
 */
public class QCstackRecordSection {
	
	static Path irPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/Inv");
	static Path stationInfPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/station_all.inf");
	static Path outRootPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/Inv/QCRS");
	static Path twPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/selectedTimewindow.dat");
	static int bornOrder = 6;
	static Map<GlobalCMTID, RealVector> synTraces = new HashMap<>();
	static Map<GlobalCMTID, RealVector> obsTraces = new HashMap<>();
	static Map<GlobalCMTID, RealVector> bornTraces = new HashMap<>();
	static Map<GlobalCMTID, double[]> timewindows = new HashMap<>();
	static double maxDistance = 100;
	static double minDistance = 70;
	static double tBefore0 = 50; // seconds
	static double tAfter0 = 150;
	static double distanceIncrement = 1.;
	static double dt = .05;
	
	public static void main(String[] args) throws IOException {
		InversionResult ir = new InversionResult(irPath);
		GlobalCMTID[] ids = {new GlobalCMTID("200609160222A")};
		List<BasicID> idList = ir.getBasicIDList();
		Set<Station> stations = StationInformationFile.read(stationInfPath);
//		Set<TimewindowInformation> twSet = TimewindowInformationFile.read(twPath);
		RealVector[] stacksObs = new RealVector[(int) (120. / distanceIncrement)];
		RealVector[] stacksSyn = new RealVector[(int) (120. / distanceIncrement)];
		RealVector[] stacksBorn = new RealVector[(int) (120. / distanceIncrement)];
		int[] numOfStack = new int[(int) (120. / distanceIncrement)];
		Arrays.stream(ids).forEachOrdered( id -> {
//			System.out.println(id);
			try {
				makeStackRecordSection(ir, stations, id, stacksObs, stacksSyn, stacksBorn, numOfStack);
			} catch (IOException e) {e.printStackTrace();}
		});
	}
	
	
	public static void makeStackRecordSection (InversionResult ir, Set<Station> stations, GlobalCMTID gid,
			RealVector[] stacksObs, RealVector[] stacksSyn, RealVector[] stacksBorn, int[] numOfStack) throws IOException {
		Set<PosixFilePermission> perms =
		         EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
		        		 , PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ);
		Files.createDirectory(outRootPath.resolve(gid.toString()), PosixFilePermissions.asFileAttribute(perms));
		Path outpathscriptstack = Paths.get(outRootPath.resolve(gid.toString()).toString(), "stackprofile.plt");
		Path outPath = Paths.get(outRootPath.resolve(gid.toString()).toString(), gid.toString() + ".txt");
//		Path outPathPltScript = Paths.get(outRootPath.resolve(gid.toString()).toString(), "profile.plt");
//		Path gmtMapScript = Paths.get(outRootPath.resolve(gid.toString()).toString(), "makemap.sh");
//		Path outStackScript = Paths.get(outRootPath.resolve(gid.toString()).toString(), "stackprofile.plt");
		try {
			TauP_Time timetool = new TauP_Time("prem");
			timetool.parsePhaseList("S");
			ir.getBasicIDList().stream()
				.filter(id -> id.getGlobalCMTID().equals(gid))
				.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
				.filter(id -> {
					double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
							stations.stream().filter(sta -> sta.equals(id.getStation()))
							.findAny().get().getPosition()) * 180 / Math.PI;
//					System.out.println(distance);
					return distance <= maxDistance && distance >= minDistance;
				})
				.forEachOrdered( id -> {
//					System.out.println(id);
					double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
							stations.stream().filter(sta -> sta.equals(id.getStation()))
							.findAny().get().getPosition()) * 180 / Math.PI;
//					System.out.println(distance);
					try {
						Trace obs = ir.observedOf(id);
						Trace syn = ir.syntheticOf(id);
						
						try {
							timetool.depthCorrect(Earth.EARTH_RADIUS - id.getGlobalCMTID().getEvent().getCmtLocation().getR());
							timetool.calculate(distance);
						} catch (TauModelException e) {e.printStackTrace();}
						
						if (timetool.getNumArrivals() > 1)
							System.out.println("Warning: more than one S arrival for " + id.toString());
						if (! timewindows.containsKey(id.getGlobalCMTID())) {
							double tBefore = Math.min(tBefore0, timetool.getArrival(0).getTime() - syn.getX()[0]);
							double tAfter = Math.max(tAfter0, syn.getX()[syn.getLength() - 1] - timetool.getArrival(0).getTime());
							timewindows.put(id.getGlobalCMTID(), new double[] {tBefore, tAfter});
						}
						else {
							double tBefore = Math.min(timewindows.get(id.getGlobalCMTID())[0]
									, timetool.getArrival(0).getTime() - syn.getX()[0]);
							double tAfter = Math.min(timewindows.get(id.getGlobalCMTID())[1]
									, syn.getX()[syn.getLength() - 1] - timetool.getArrival(0).getTime());
//							System.out.println(tBefore + " " + tAfter);
							timewindows.replace(id.getGlobalCMTID(), new double[] {tBefore, tAfter});
						}
					} catch (IOException e) {e.printStackTrace();}
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
				.forEachOrdered(id -> {
					try {
						double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(
								stations.stream().filter(sta -> sta.equals(id.getStation()))
									.findAny().get().getPosition()) * 180 / Math.PI;
						try {
							timetool.depthCorrect(Earth.EARTH_RADIUS - id.getGlobalCMTID().getEvent().getCmtLocation().getR());
							timetool.calculate(distance);
						} catch (TauModelException e) {e.printStackTrace();}
						if (timetool.getNumArrivals() > 1)
							System.out.println("Warning: more than one S arrival for " + id.toString());
						
						double start = timetool.getArrival(0).getTime() - timewindows.get(id.getGlobalCMTID())[0];
						double end = timetool.getArrival(0).getTime() + timewindows.get(id.getGlobalCMTID())[1];
						
//						/**
						if (start < id.getStartTime() || end > id.getStartTime() + id.getNpts() / id.getSamplingHz()) {
							throw new IndexOutOfBoundsException("timewindow shorter than the stack time window "
									+ start + ":" + end + " < " + id.getStartTime() + ":" + (id.getStartTime() + id.getNpts() / id.getSamplingHz())
									+ " for event " + id.getGlobalCMTID().toString()
									);
						}
						
						int istart = (int) ((start - id.getStartTime()) * id.getSamplingHz());
						int iIncr = (int) ((end - start) * id.getSamplingHz());
						
						RealVector obs = ir.observedOf(id).getYVector().getSubVector(istart, iIncr);
						Trace obsTrace = ir.observedOf(id);
						double maxObs = FastMath.max(obs.getMaxValue(), FastMath.abs(obs.getMinValue()));
						RealVector syn = ir.syntheticOf(id).getYVector().getSubVector(istart, iIncr);
						Trace synTrace = ir.syntheticOf(id);
						double maxSyn = FastMath.max(syn.getMaxValue(), FastMath.abs(syn.getMinValue()));
						RealVector born = ir.bornOf(id, InverseMethodEnum.CONJUGATE_GRADIENT, bornOrder).getYVector().getSubVector(istart, iIncr);
						Trace bornTrace = ir.bornOf(id, InverseMethodEnum.CONJUGATE_GRADIENT, bornOrder);
						double maxBorn = FastMath.max(born.getMaxValue(), FastMath.abs(born.getMinValue()));
						
						try {
							for (int i=0;i<obs.toArray().length;i++) {
								Files.write(outPath, (String.valueOf(obsTrace.getX()[i])
										+" "
										+ String.valueOf(obsTrace.getY()[i] / maxObs)
										+" "
										+ String.valueOf(synTrace.getY()[i] / maxSyn)
										+" "
										+ String.valueOf(bornTrace.getY()[i] / maxBorn)
										+ "\n").getBytes()
										, StandardOpenOption.APPEND);
							}
						} catch (IOException e) {e.printStackTrace();}
						
						int index = (int) (distance / distanceIncrement);
						if (stacksObs[index] == null) {
							stacksObs[index] = new ArrayRealVector(obsTrace.getYVector().mapDivide(maxObs));
						}
						else if (stacksSyn[index] == null) {
							stacksSyn[index] = new ArrayRealVector(synTrace.getYVector().mapDivide(maxSyn));
						}
						else if (stacksBorn[index] == null) {
							stacksBorn[index] = new ArrayRealVector(bornTrace.getYVector().mapDivide(maxBorn));
						}
						else {
							for (int i = 0; i < FastMath.min(stacksObs[index].getDimension(), obsTrace.getLength()); i++)
								stacksObs[index].addToEntry(i, obsTrace.getY()[i] / maxObs);
							for (int i = 0; i < FastMath.min(stacksSyn[index].getDimension(), synTrace.getLength()); i++)
								stacksObs[index].addToEntry(i, synTrace.getY()[i] / maxSyn);
							for (int i = 0; i < FastMath.min(stacksBorn[index].getDimension(), bornTrace.getLength()); i++)
								stacksBorn[index].addToEntry(i, bornTrace.getY()[i] / maxBorn);
						}
						numOfStack[index] += 1;
						
					} catch (IOException e) {e.printStackTrace();}
				});
			
			for (int i=0; i < numOfStack.length; i++) {
				if (numOfStack[i] != 0) {
					Path outpathstack = Paths.get(outRootPath.toString(), "stack" + String.format("%.1f", i * distanceIncrement) + ".T.txt");
					Files.deleteIfExists(outpathstack);
					Files.createFile(outpathstack);
					for (int j=0; j < stacksBorn[i].getDimension(); j++)
						Files.write(outpathstack
								, (String.valueOf(j * dt)
										+ " "
										+ String.valueOf(stacksObs[i].getEntry(j) / numOfStack[i])
										+ " "
										+ String.valueOf(stacksSyn[i].getEntry(j) / numOfStack[i])
										+ " "
										+ String.valueOf(stacksBorn[i].getEntry(j) / numOfStack[i])
										+ "\n").getBytes()
								, StandardOpenOption.APPEND);					
						Files.write(outpathscriptstack, ("\'"
							+ "stack" + i * distanceIncrement + ".txt"
							+ "\'"
							+ " u 1:($2+"
							+ String.valueOf(i * distanceIncrement)
							+ ") "
							+ "w lines lt -1 lc rgb \"black\" "
							+ "lw .2,\\"
							+ "\n").getBytes()
							, StandardOpenOption.APPEND);
						Files.write(outpathscriptstack, ("\'"
								+ "stack" + i * distanceIncrement + ".txt"
								+ "\'"
								+ " u 1:($3+"
								+ String.valueOf(i * distanceIncrement)
								+ ") "
								+ "w lines lt -1 lc rgb \"green\" "
								+ "lw .2,\\"
								+ "\n").getBytes()
								, StandardOpenOption.APPEND);
						Files.write(outpathscriptstack, ("\'"
								+ "stack" + i * distanceIncrement + ".txt"
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
			
		} catch (TauModelException e ) {e.printStackTrace();}
	}
	
	public static void plotScriptInitialize (Path outPathPltScript, Path gmtMapScript, Path outStackScript,
			int minlat2, int maxlat2, int minlon2, int maxlon2, File dir) {
		try {
			Files.write(outPathPltScript, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
					,"unset key"
					,"set size .5,1" 
					,"set xlabel 'Reduced time (s)'"
					,"set ylabel 'distance (deg)'"
					,"set output 'profile" +  ".eps'\np").getBytes()
					, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.write(outStackScript, String.join("\n", "set terminal postscript enhanced color font 'Helvetica,18'"
					,"unset key"
					,"set size .5,1"
					,"set xlabel 'Reduced time (s)'"
					,"set ylabel 'distance (deg)'"
					,"set output 'stackprofile" + dir.toString() + ".eps'\np").getBytes()
					, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
