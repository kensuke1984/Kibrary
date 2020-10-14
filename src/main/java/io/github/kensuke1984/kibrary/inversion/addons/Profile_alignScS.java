package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.inference.TTest;

import com.sleepycat.util.RuntimeExceptionWrapper;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;


public class Profile_alignScS {
	public static void main(String[] args) throws TauModelException {
		int methodOrder = Integer.parseInt(args[0]);
//		int methodOrder = 1;
		GlobalCMTID oneEvent = null;
//		if (args.length == 2)
//			oneEvent = new GlobalCMTID(args[1]);
		InverseMethodEnum method = InverseMethodEnum.CONJUGATE_GRADIENT;
//		String inversionResultString = null;
//		Path inversionResultPath = null;
//		try {
//			inversionResultString = JOptionPane.showInputDialog("Inversion result folder?", inversionResultString);
//		} catch (Exception e) {
//			System.out.println("Inversion result folder?");
//			try (BufferedReader br = new BufferedReader(
//					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
//				inversionResultString = br.readLine().trim();
//				if (!inversionResultString.startsWith("/"))
//					inversionResultString = System.getProperty("user.dir") + "/" + inversionResultString;
//			} catch (Exception e2) {
//				e2.printStackTrace();
//				throw new RuntimeException();
//			}
//		}
//		if (inversionResultString == null || inversionResultString.isEmpty())
//			return;
//		inversionResultPath = Paths.get(inversionResultString);
//		if (!Files.isDirectory(inversionResultPath))
//			throw new RuntimeException("No such directory " + inversionResultPath.toString());
//		
//		String phaseString = null;
//		try {
//			phaseString = JOptionPane.showInputDialog("Phase?", phaseString);
//		} catch (Exception e) {
//			System.out.println("Phase?");
//			try (BufferedReader br = new BufferedReader(
//					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
//				phaseString = br.readLine().trim();
//			} catch (Exception e2) {
//				e2.printStackTrace();
//				throw new RuntimeException();
//			}
//		}
//		if (phaseString == null || phaseString.isEmpty())
//			return;
//		Phase phase = Phase.create(phaseString);
		
//		Path inversionResultPath = Paths.get("/work/anselme/CA_ANEL_NEW/oneDPartialPREM/inversion/NEW/lmi_rec_70deg_az335_201205280507A_mantleCorr_Q");
		Path inversionResultPath = Paths.get(".");
//		Phase phase = Phase.create(args[1]);
		Phase phase = Phase.ScS;
		
//		Path inversionResultPathReference = Paths.get("/work/anselme/CA_ANEL_NEW/oneDPartialPREM/inversion/lmi_forS5");
		Path inversionResultPathReference = inversionResultPath;
		
		TauP_Time timetool = new TauP_Time("prem");
		timetool.parsePhaseList("ScS");
		
		try {
			Path profilePath = inversionResultPath.resolve("profile").resolve(method.name() + methodOrder);
			Path bornPath = profilePath.relativize(inversionResultPath.resolve("born/" + method + methodOrder));
			Path obsPath = profilePath.relativize(inversionResultPath.resolve("trace"));
			Files.createDirectories(profilePath);
			
			PrintWriter pw1 = new PrintWriter(Files.newBufferedWriter(profilePath.resolve("eventVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			PrintWriter pw2 = new PrintWriter(Files.newBufferedWriter(profilePath.resolve("eachVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pw1.println("# Event synVariance bornVariance varianceReduction;(syn - born)");
			pw2.println("# BasicID synVariance bornVariance varanceReduction;(syn - born)");
			
			Path outpathEachMisfit = profilePath.resolve("eachMisfit.inf");
			
			Path stackRoot = inversionResultPath.resolve("stack/" + method + methodOrder +  "/" + phase);
			Files.createDirectories(stackRoot);
			
			Path mapPath = stackRoot.resolve("map");
			Files.createDirectory(mapPath);
			
			PrintWriter pw4 = new PrintWriter(Files.newBufferedWriter(stackRoot.resolve("eventVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pw4.println("# Event synVariance bornVariance varianceReduction(syn - born) lat lon depth Mw");
			
			InversionResult ir = new InversionResult(inversionResultPath);
			List<BasicID> obsList = ir.getBasicIDList().stream().filter(id -> id.getWaveformType().equals(WaveformType.OBS))
				.collect(Collectors.toList());
			Set<GlobalCMTID> events = ir.idSet();
			
			InversionResult irRef = new InversionResult(inversionResultPathReference);
			
			double dt = 1. / obsList.get(0).getSamplingHz();
			
			Map<Station, Double> stationSynVariance = new HashMap<>();
			Map<Station, Double> stationBornVariance = new HashMap<>();
			Map<Station, Double> stationObsNorm = new HashMap<>();
			obsList.parallelStream().map(id -> id.getStation()).distinct().forEach(station -> {
				stationSynVariance.put(station, new Double(0));
				stationBornVariance.put(station, new Double(0));
				stationObsNorm.put(station, new Double(0));
			});
//			for (Station station : ir.stationSet()) {
//				stationSynVariance.put(station, 0.);
//				stationBornVariance.put(station, 0.);
//				stationObsNorm.put(station, 0.);
//			}
			
			double[] totalSynVariance = new double[] {0.};
			double[] totalBornVariance = new double[] {0.};
			double[] totalObsNorm = new double[] {0.};
			
			double[] distanceSynVariance = new double[120];
			double[] distanceBornVariance = new double[120];
			double[] distanceSynCorr = new double[120];
			double[] distanceBornCorr = new double[120];
			double[] distanceObsNorm = new double[120];
			double[] distanceBornNorm = new double[120];
			double[] distanceSynNorm = new double[120];
			
			String[] varString = new String[] {""};
			String[] eachMisfitString = new String[] {""};
			
			for (GlobalCMTID event : events) {
				if (oneEvent != null && !event.equals(oneEvent))
					continue;
				System.out.println(event);
				
				List<BasicID> tmpObs = obsList.stream().filter(id -> id.getGlobalCMTID().equals(event)).collect(Collectors.toList());
				
				Path outpath_R = inversionResultPath.resolve(profilePath.resolve(event.toString() + "_R.plt"));
				Path outpath_T = inversionResultPath.resolve(profilePath.resolve(event.toString() + "_T.plt"));
				Path outpath_Z = inversionResultPath.resolve(profilePath.resolve(event.toString() + "_Z.plt"));
				try {
					PrintWriter pw_R = new PrintWriter(Files.newBufferedWriter(outpath_R, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
					PrintWriter pw_T = new PrintWriter(Files.newBufferedWriter(outpath_T, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
					PrintWriter pw_Z = new PrintWriter(Files.newBufferedWriter(outpath_Z, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
					String header = String.join("\n", "set terminal postscript enhanced color font \"Helvetica,14\"",
							"set output \"" + event + ".ps\"",
							"unset key",
							"set xlabel 'Time aligned on ScS-wave arrival (s)'",
							"set ylabel 'Distance (deg)'",
							"#set xtics 5",
							"#set ytics 2",
							"set size .5,1",
							"p ");
					pw_R.print(header);
					pw_T.print(header);
					pw_Z.print(header);
					
					int n = tmpObs.size();
//					AtomicInteger i = new AtomicInteger();
					double[] synVariance = new double[1];
					double[] bornVariance = new double[1];
					double[] obsNorm = new double[1];
					
					String[] scriptString_R = new String[] {""};
					String[] scriptString_T = new String[] {""};
					String[] scriptString_Z = new String[] {""};
					
					tmpObs.stream().forEach(id -> {
//						i.incrementAndGet();
						try {
							RealVector bornVector = ir.bornOf(id, method, methodOrder).getYVector();
							double maxObs = ir.observedOf(id).getYVector().getLInfNorm() * 4;
							String name = ir.getTxtName(id);
							double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(id.getStation().getPosition())
									* 180. / Math.PI;
							if (id.getSacComponent().equals(SACComponent.R))
								scriptString_R[0] += "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($3/%.3e+%.2f) ", maxObs, distance) + "w lines lt 1 lc rgb \"black\",\\\n"
									+ "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($4/%.3e+%.2f) ", maxObs, distance) + "w lines lt 1 lc rgb \"red\",\\\n"
									+ "\"" + bornPath + "/" + name + "\" " + String.format("u 0:($2/%.3e+%.2f) ", maxObs, distance) + "w lines lt 1 lc rgb \"blue\",\\\n";
							else if (id.getSacComponent().equals(SACComponent.T))
							scriptString_T[0] += "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($3/%.3e+%.2f) ", maxObs, distance) + "w lines lt 1 lc rgb \"black\",\\\n"
									+ "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($4/%.3e+%.2f) ", maxObs, distance) + "w lines lt 1 lc rgb \"red\",\\\n"
									+ "\"" + bornPath + "/" + name + "\" " + String.format("u 0:($2/%.3e+%.2f) ", maxObs, distance) + "w lines lt 1 lc rgb \"blue\",\\\n";
							else if (id.getSacComponent().equals(SACComponent.Z))
								scriptString_Z[0] += "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($3/%.3e+%.2f) ", maxObs, distance) + "w lines lt 1 lc rgb \"black\",\\\n"
									+ "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($4/%.3e+%.2f) ", maxObs, distance) + "w lines lt 1 lc rgb \"red\",\\\n"
									+ "\"" + bornPath + "/" + name + "\" " + String.format("u 0:($2/%.3e+%.2f) ", maxObs, distance) + "w lines lt 1 lc rgb \"blue\",\\\n";
//							pw.println("\"" + obsPath + "/" + name + "\" " + String.format("u ($1-8.4*%.2f):($3/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lt 1 lc rgb \"black\",\\");
//							pw.println("\"" + obsPath + "/" + name + "\" " + String.format("u ($2-8.4*%.2f):($4/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lt 1 lc rgb \"red\",\\");
////							if (i.get() == n)
////								pw.println("\"" + bornPath + "/" + name + "\" " + String.format("u ($1-8.4*%.2f):($2/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lt 1 lc rgb \"blue\"");
////							else
//								pw.println("\"" + bornPath + "/" + name + "\" " + String.format("u ($1-8.4*%.2f):($2/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lt 1 lc rgb \"blue\",\\");
							
							// output variance
//							RealVector synVector = ir.syntheticOf(id).getYVector();
							RealVector synVector = null;
							try {
								synVector = irRef.syntheticOf(id).getYVector();
							} catch (IOException e) {
								System.err.println(e.getMessage());
								System.out.println(id);
								return;
							}
							
							RealVector obsVector = ir.observedOf(id).getYVector();
							double tmpSynVariance = synVector.subtract(obsVector).dotProduct(synVector.subtract(obsVector));
							double tmpBornVariance = bornVector.subtract(obsVector).dotProduct(bornVector.subtract(obsVector));
							double tmpObsNorm = obsVector.dotProduct(obsVector);
							
							double tmpSyn = tmpSynVariance / tmpObsNorm;
							double tmpBorn =  tmpBornVariance / tmpObsNorm;
							varString[0] += id + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn) + "\n";
//							pw2.println(id + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn));
							
							double synCorr = synVector.dotProduct(obsVector) / (synVector.getNorm() * obsVector.getNorm());
							double bornCorr = bornVector.dotProduct(obsVector) / (bornVector.getNorm() * obsVector.getNorm());
							double synRatio = synVector.getLInfNorm() / obsVector.getLInfNorm();
							double bornRatio = bornVector.getLInfNorm() / obsVector.getLInfNorm();
//							double synRatio = (synVector.getMaxValue() - synVector.getMinValue()) / (obsVector.getMaxValue() - obsVector.getMinValue());
//							double bornRatio = (bornVector.getMaxValue() - bornVector.getMinValue()) / (obsVector.getMaxValue() - obsVector.getMinValue());
							eachMisfitString[0] += id.getStation().getName() + " " + id.getStation().getNetwork() + " " + id.getStation().getPosition() + " "
									+ id.getGlobalCMTID() + " " + id.getSacComponent() + " " + (new Phases(id.getPhases())) + " " + synRatio + " " + bornRatio + " "
									+ tmpSyn + " " + tmpBorn + " " + synCorr + " " + bornCorr + "\n";
							
							synVariance[0] += tmpSynVariance;
							bornVariance[0] += tmpBornVariance;
							obsNorm[0] += tmpObsNorm;
							
							totalSynVariance[0] += tmpSynVariance;
							totalBornVariance[0] += tmpBornVariance;
							totalObsNorm[0] += tmpObsNorm;
							
							Station sta = id.getStation();
							try {
//								tmpSynVariance += stationSynVariance.get(sta);
//								tmpBornVariance += stationBornVariance.get(sta);
//								tmpObsNorm += stationObsNorm.get(sta);
//								
//								stationSynVariance.put(sta, tmpSynVariance);
//								stationBornVariance.put(sta, tmpBornVariance);
//								stationObsNorm.put(sta, tmpObsNorm);
							} catch (NullPointerException e) {
								System.err.println(sta + " " + sta.getPosition());
								e.printStackTrace();
								System.exit(1);
							}
							
							double tmpSynCorr = synVector.dotProduct(obsVector);
							double tmpBornCorr = bornVector.dotProduct(obsVector);
							double tmpSynNorm = synVector.dotProduct(synVector);
							double tmpBornNorm = bornVector.dotProduct(bornVector);
							
							int idist = (int) (distance);
							distanceBornCorr[idist] += tmpBornCorr;
							distanceSynCorr[idist] += tmpSynCorr;
							distanceBornNorm[idist] += tmpBornNorm;
							distanceSynNorm[idist] += tmpSynNorm;
							distanceSynVariance[idist] += tmpSynVariance;
							distanceBornVariance[idist] += tmpBornVariance;
							distanceObsNorm[idist] += tmpObsNorm;
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
					pw_R.print(scriptString_R[0]);
					pw_T.print(scriptString_T[0]);
					pw_Z.print(scriptString_Z[0]);
					
					pw_R.close();
					pw_T.close();
					pw_Z.close();
					
					double tmpSyn = synVariance[0] / obsNorm[0]; //Math.sqrt(synVariance[0] / obsNorm[0]);
					double tmpBorn =  bornVariance[0] / obsNorm[0]; //Math.sqrt(bornVariance[0] / obsNorm[0]);
					pw1.println(event + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn));
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				int m = 140;
				int dAz = 5;
				int l = 360 / dAz;
				
				Trace[][] obsTraces = new Trace[m][l+1];
				Trace[][] synTraces = new Trace[m][l+1];
				Trace[][] bornTraces = new Trace[m][l+1];
				
				List<List<Station>> azimuthStationList = new ArrayList<>();
				
				for (int i = 0; i < l; i++)
					azimuthStationList.add(new ArrayList<Station>());
				
				Path stackPath = stackRoot.resolve(event.toString());
				Files.createDirectories(stackPath);
				
				double synVariance = 0;
				double bornVariance = 0;
				double obsNorm = 0;
				
				timetool.setSourceDepth(6371. - event.getEvent().getCmtLocation().getR());
				
				for (BasicID id : tmpObs) {
					if (!Stream.of(id.getPhases()).collect(Collectors.toSet()).contains(phase))
						continue;
					Trace obsTrace = ir.observedOf(id);
					
//					Trace synTrace = ir.syntheticOf(id);
					Trace synTrace = null;
					try {
						synTrace = irRef.syntheticOf(id);
					} catch (IOException e) {
						continue;
					}
					
					Trace bornTrace = ir.bornOf(id, method, methodOrder);
					
					RealVector obsVector = obsTrace.getYVector();
					RealVector synVector = synTrace.getYVector();
					RealVector bornVector = bornTrace.getYVector();
					
					double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(id.getStation().getPosition())
							* 180. / Math.PI;
					double azimuth = Math.toDegrees(id.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(id.getStation().getPosition()));
					int i = (int) (distance);
					int j = (int) (azimuth / dAz);
					
					timetool.calculate(distance);
					Arrival pcpArrival = timetool.getArrival(0);
					if (!pcpArrival.getName().equals("ScS")) {
						System.out.println("Problem computing ScS time for " + id);
						continue;
					}
					double timePcP = pcpArrival.getTime();
					
					obsTrace = obsTrace.shiftX(-timePcP - obsTrace.getXAt(0) + synTrace.getXAt(0));
					synTrace = synTrace.shiftX(-timePcP);
					bornTrace = bornTrace.shiftX(-timePcP);
					
					
					obsTraces[i][0] = obsTraces[i][0] == null ? obsTrace : add(obsTraces[i][0], obsTrace);
					synTraces[i][0] = synTraces[i][0] == null ? synTrace : add(synTraces[i][0], synTrace);
					bornTraces[i][0] = bornTraces[i][0] == null ? bornTrace : add(bornTraces[i][0], bornTrace);
					
					obsTraces[i][j+1] = obsTraces[i][j+1] == null ? obsTrace : add(obsTraces[i][j+1], obsTrace);
					synTraces[i][j+1] = synTraces[i][j+1] == null ? synTrace : add(synTraces[i][j+1], synTrace);
					bornTraces[i][j+1] = bornTraces[i][j+1] == null ? bornTrace : add(bornTraces[i][j+1], bornTrace);
					
					double tmpSynVariance = synVector.subtract(obsVector).dotProduct(synVector.subtract(obsVector));
					double tmpBornVariance = bornVector.subtract(obsVector).dotProduct(bornVector.subtract(obsVector));
					double tmpObsNorm = obsVector.dotProduct(obsVector);
					
					synVariance += tmpSynVariance;
					bornVariance += tmpBornVariance;
					obsNorm += tmpObsNorm;
					
					List<Station> tmpList = azimuthStationList.get(j);
					tmpList.add(id.getStation());
					azimuthStationList.set(j, tmpList);
				}
					double tmpSyn = synVariance / obsNorm;
					double tmpBorn =  bornVariance / obsNorm;
					double lat = event.getEvent().getCmtLocation().getLatitude();
					double lon = event.getEvent().getCmtLocation().getLongitude();
					double depth = 6371. - event.getEvent().getCmtLocation().getR();
					pw4.println(event + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn) + " " + lat + " " + lon + " " + depth + " " + event.getEvent().getCmt().getMw());
					
				Path outPlot = stackPath.resolve(event + ".plt");
				Files.deleteIfExists(outPlot);
				
				try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPlot, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
					pw.println("set terminal postscript enhanced color font \"Helvetica,14\"");
					pw.println("set output \"" + event + ".ps\"");
					pw.println("unset key");
					pw.println("#set yrange [65:102]");
					pw.println("set xlabel 'Time aligned on ScS-wave arrival (s)'");
					pw.println("set ylabel 'Distance (deg)'");
					pw.println("#set xtics 5");
					pw.println("#set ytics 2");
					pw.println("set size .5,1");
					pw.print("p ");
					
					for (int i = 0; i < m; i++) {
						if (obsTraces[i][0] == null)
							continue;
						Path outObs = stackPath.resolve(String.format("obsStack_%s_%d-%d.txt", event, i, i+1));
						Path outSyn = stackPath.resolve(String.format("synStack_%s_%d-%d.txt", event, i, i+1));
						Path outBorn = stackPath.resolve(String.format("bornStack_%s_%d-%d.txt", event, i, i+1));
						
						double maxObs = obsTraces[i][0].getYVector().getLInfNorm();
						
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outObs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < obsTraces[i][0].getLength(); k++)
								pw3.println(obsTraces[i][0].getXAt(k) + " " + obsTraces[i][0].getYAt(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outSyn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < synTraces[i][0].getLength(); k++)
								pw3.println(synTraces[i][0].getXAt(k) + " " + synTraces[i][0].getYAt(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outBorn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < bornTraces[i][0].getLength(); k++)
								pw3.println(bornTraces[i][0].getXAt(k) + " " + bornTraces[i][0].getYAt(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						
						pw.println("\"" + outObs.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 2, (double) i) + "w lines lw .5 lt 1 lc rgb \"black\",\\");
						pw.println("\"" + outSyn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 2, (double) i) + "w lines lw .5 lt 1 lc rgb \"red\",\\");
						pw.println("\"" + outBorn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 2, (double) i) + "w lines lw .5 lt 1 lc rgb \"blue\",\\");
					}
					pw.println();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				for (int j = 0; j < l; j++) {
					boolean pass = true;
					for (int i = 0; i < m; i++)
						if (obsTraces[i][j+1] != null)
							pass = false;
					if (pass)
						continue;
					
					//write map
					writeGMT(mapPath, event, azimuthStationList.get(j), j * dAz);
					
					outPlot = stackPath.resolve(event + ".az" + (dAz * j) + ".plt");
					Files.deleteIfExists(outPlot);
					PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPlot, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					
					pw.println("set terminal postscript enhanced color font \"Helvetica,14\"");
					pw.println("set output \"" + event + ".az" + (dAz * j) + ".ps\"");
					pw.println("unset key");
					pw.println("#set yrange [68:102]");
					pw.println("set xlabel 'Time aligned on ScS-wave arrival (s)'");
					pw.println("set ylabel 'Distance (deg)'");
					pw.println("#set xtics 5");
					pw.println("#set ytics 2");
					pw.println("set size .5,1");
					pw.print("p ");
					
					for (int i = 0; i < m; i++) {
						if (obsTraces[i][j+1] == null)
							continue;
						Path outObs = stackPath.resolve(String.format("obsStack_%s_%d-%d.az" +  (dAz * j) + ".txt", event, i, i+1));
						Path outSyn = stackPath.resolve(String.format("synStack_%s_%d-%d.az" +  (dAz * j) + ".txt", event, i, i+1));
						Path outBorn = stackPath.resolve(String.format("bornStack_%s_%d-%d.az" + (dAz * j) + ".txt", event, i, i+1));
						
						double maxObs = obsTraces[i][j+1].getYVector().getLInfNorm();
						
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outObs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < obsTraces[i][j+1].getLength(); k++)
								pw3.println(obsTraces[i][j+1].getXAt(k) + " " + obsTraces[i][j+1].getYAt(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outSyn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < synTraces[i][j+1].getLength(); k++)
								pw3.println(synTraces[i][j+1].getXAt(k) + " " + synTraces[i][j+1].getYAt(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outBorn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < bornTraces[i][j+1].getLength(); k++)
								pw3.println(bornTraces[i][j+1].getXAt(k) + " " + bornTraces[i][j+1].getYAt(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						
						pw.println("\"" + outObs.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 2, i / 1.) + "w lines lw .5 lt 1 lc rgb \"black\",\\");
						pw.println("\"" + outSyn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 2, i / 1.) + "w lines lw .5 lt 1 lc rgb \"red\",\\");
						pw.println("\"" + outBorn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 2, i / 1.) + "w lines lw .5 lt 1 lc rgb \"blue\",\\");
					}
					pw.println();
					pw.close();
				}
			}
			pw1.flush();
			pw1.close();
			pw4.flush();
			pw4.close();
			
			// write variance at each station
//			pw1 = new PrintWriter(Files.newBufferedWriter(stackRoot.resolve("stationVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
//			pw1.println("# Station synVariance bornVariance varianceReduction;(syn - born)");
//			
//			for (Station sta : stationObsNorm.keySet()) {
//				try {
//				double bornVariance = stationBornVariance.get(sta) / stationObsNorm.get(sta);
//				double synVariance = stationSynVariance.get(sta) / stationObsNorm.get(sta);
//				pw1.println(sta.getStationName() + " " + sta.getNetwork() + " " + sta.getPosition() 
//						+ " " + synVariance + " " + bornVariance + " " + (synVariance - bornVariance));
//				} catch (NullPointerException e) {
//					System.err.println(sta + " " + sta.getPosition());
//				}
//			}
//			pw1.close();
			
			// write total variance
			Path totalvariancePath = inversionResultPath.resolve("totalVariance.inf");
			boolean firstWrite = false;
			if (!Files.exists(totalvariancePath))
				firstWrite = true;
			pw2 = new PrintWriter(Files.newBufferedWriter(totalvariancePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
			if (firstWrite)
				pw2.println("nCG SynVariance bornVariance varianceReduction;(syn - born)");
			
			double bornVariance = totalBornVariance[0] / totalObsNorm[0];
			double synVariance = totalSynVariance[0] / totalObsNorm[0];
			pw2.println(methodOrder + " " + synVariance + " " + bornVariance + " " + (synVariance - bornVariance));
			pw2.close();
			
			//write variance and correlation for each epicentral distance
			PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(inversionResultPath.resolve("distanceVarianceCorrelation.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pw3.println("Distance synVariance bornVariance varianceReduction synCorr bornCorr)");
			
			for (int i = 0; i < distanceObsNorm.length; i++) {
				if (distanceObsNorm[i] == 0)
					continue;
				bornVariance = distanceBornVariance[i] / distanceObsNorm[i];
				synVariance = distanceSynVariance[i] / distanceObsNorm[i];
				double bornCorr = distanceBornCorr[i] / Math.sqrt(distanceObsNorm[i] * distanceBornNorm[i]);
				double synCorr = distanceSynCorr[i] / Math.sqrt(distanceSynNorm[i] * distanceObsNorm[i]);
				pw3.println(i + " " + synVariance + " " + bornVariance + " " + (synVariance - bornVariance) + " " + synCorr + " " + bornCorr);
			}
			pw3.close();
			
			//write variance and misfit for each timewindow
			pw2.print(varString[0]);
			pw2.close();
			
			PrintWriter pwEachMisfit = new PrintWriter(Files.newBufferedWriter(outpathEachMisfit, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pwEachMisfit.println("station, network, lat, lon, event, component, phase, synRatio, bornRatio, synVar, bornVar, synCC, bornCC");
			pwEachMisfit.print(eachMisfitString[0]);
			pwEachMisfit.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static RealVector add(RealVector v1, RealVector v2) {
		RealVector res = null;
		
		if (v1.getDimension() == 0)
			res = v2;
		else if (v2.getDimension() == 0)
			res = v1;
		else
			res = v1.getDimension() > v2.getDimension() ? v2.add(v1.getSubVector(0, v2.getDimension())) 
					: v1.add(v2.getSubVector(0, v1.getDimension()));
		
		return res;
	}
	
	private static Trace add(Trace v1, Trace v2) {
		Trace res = null;
		
		if (v1.getLength() == 0)
			res = v2;
		else if (v2.getLength() == 0)
			res = v1;
		else {
			double ti1 = v1.getXAt(0);
			double ti2 = v2.getXAt(0);
			double tf1 = v1.getXAt(v1.getLength() - 1);
			double tf2 = v2.getXAt(v2.getLength() - 1);
			double tStart = ti1 < ti2 ? ti2 : ti1;
			double tEnd = tf2 < tf1 ? tf2 : tf1;
			Trace cut1 = v1.cutWindow(tStart, tEnd);
			Trace cut2 = v2.cutWindow(tStart, tEnd);
			cut2 = new Trace(cut1.getX(), Arrays.copyOf(cut2.getY(), cut1.getLength()));
			res = cut1.add(cut2);
		}
		
		return res;
	}
	
private static Trace addAndPadd(Trace trace1, Trace trace2) {
		double t0 = Math.min(trace1.getXAt(0), trace2.getXAt(0));
		double t1 = Math.max(trace1.getXAt(trace1.getLength()), trace2.getXAt(trace2.getLength()));
		
		double dt = trace1.getXAt(1) - trace1.getXAt(0);
		int n = (int) ((t1 - t0) / dt) + 1;
		
		double[] xs = IntStream.range(0, n).mapToDouble(i -> t0 + i*dt).toArray();
		double[] ys = new double[n];
		
		if (trace1.getXAt(0) < trace2.getXAt(0)) {
			ys = Arrays.copyOf(trace1.getY(), n);
			int istartOther = (int) ((trace2.getXAt(0) - trace1.getXAt(0)) / dt);
			for (int i = istartOther; i < n; i++)
				ys[i] += trace2.getYAt(i);
		}
		else if (trace2.getXAt(0) < trace1.getXAt(0)) {
			ys = Arrays.copyOf(trace2.getY(), n);
			int istartOther = (int) ((trace1.getXAt(0) - trace2.getXAt(0)) / dt);
			for (int i = istartOther; i < n; i++)
				ys[i] += trace1.getYAt(i);
		}
		
		return new Trace(xs, ys);
	}

	private static void writeGMT(Path rootpath, GlobalCMTID event, List<Station> stations, double azimuth) throws IOException {
		Path outpath = rootpath.resolve("plot_map_" + event + "_az" + (int) (azimuth) + ".gmt");
		String outpathps = "map_" + event + "_az" + (int) (azimuth) + ".ps";
		PrintWriter pw = new PrintWriter(outpath.toFile());
		
		String ss = String.join("\n",
				"#!/bin/sh"
				, "outputps=" + outpathps
				, "gmt set FONT 12p,Helvetica,black"
				, "gmt set PS_MEDIA 5ix4.8i"
				, "gmt set MAP_FRAME_WIDTH 2p"
				, "gmt pscoast -R-170/-52/-41/75 -JQ270/4.4i -P -G205 -K -Di -X.4i -Y.3i -BWSne -Bx60g30f30 -By60g30f30 > $outputps"
				, ""
				);
		
		ss += "gmt psxy -R-170/-52/-41/75 -JQ270/4.4i -Wthinner,red -t0 -K -O >> $outputps <<END\n";
		double evLat = event.getEvent().getCmtLocation().getLatitude();
		double evLon = event.getEvent().getCmtLocation().getLongitude();
		for (Station station : stations)
			ss += String.format(">\n%.2f %.2f\n%.2f %.2f\n", evLon, evLat, station.getPosition().getLongitude(), station.getPosition().getLatitude());
		ss += "END\n";
		
		ss += "gmt psxy -R -J -Si0.11 -P -Groyalblue -Wthinnest,black -K -O >> $outputps <<END\n";
		for (Station station : stations)
			ss += String.format("%.2f %.2f\n", station.getPosition().getLongitude(), station.getPosition().getLatitude());
		ss += "END\n";
		
		ss += "gmt psxy -R -J -P -Sa0.22 -Gred -Wthinnest,black -K -O >> $outputps<<END\n";
		ss += String.format("%.2f %.2f\n", evLon, evLat);
		ss += "END\n";
		
		ss += "gmt pstext -R -J -P -O >> $outputps <<END\n"
			 + "END\n"
			 + "gmt ps2raster $outputps -Tf\n";
		
		pw.println(ss);
		
		pw.close();
	}
}
