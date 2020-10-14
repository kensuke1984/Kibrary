package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
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


public class Profile_v2 {
	public static void main(String[] args) {
		int methodOrder = Integer.parseInt(args[0]);
		GlobalCMTID oneEvent = null;
		if (args.length == 2)
			oneEvent = new GlobalCMTID(args[1]);
		InverseMethodEnum method = InverseMethodEnum.CONJUGATE_GRADIENT;
		String inversionResultString = null;
		Path inversionResultPath = null;
		try {
			inversionResultString = JOptionPane.showInputDialog("Inversion result folder?", inversionResultString);
		} catch (Exception e) {
			System.out.println("Inversion result folder?");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
				inversionResultString = br.readLine().trim();
				if (!inversionResultString.startsWith("/"))
					inversionResultString = System.getProperty("user.dir") + "/" + inversionResultString;
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new RuntimeException();
			}
		}
		if (inversionResultString == null || inversionResultString.isEmpty())
			return;
		inversionResultPath = Paths.get(inversionResultString);
		if (!Files.isDirectory(inversionResultPath))
			throw new RuntimeException("No such directory " + inversionResultPath.toString());
		
		String phaseString = null;
		try {
			phaseString = JOptionPane.showInputDialog("Phase?", phaseString);
		} catch (Exception e) {
			System.out.println("Phase?");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
				phaseString = br.readLine().trim();
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new RuntimeException();
			}
		}
		if (phaseString == null || phaseString.isEmpty())
			return;
		Phase phase = Phase.create(phaseString);
		
		String slownessString = null;
		try {
			slownessString = JOptionPane.showInputDialog("Differential slowness?", slownessString);
		} catch (Exception e) {
			System.out.println("Differential slowness?");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new CloseShieldInputStream(System.in)))) {
				slownessString = br.readLine().trim();
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new RuntimeException();
			}
		}
		if (slownessString == null || slownessString.isEmpty())
			return;
		double diff_slowness = Double.parseDouble(slownessString);
		
		try {
			Path profilePath = inversionResultPath.resolve("profile").resolve(method.name() + methodOrder);
			Path bornPath = profilePath.relativize(inversionResultPath.resolve("born/" + method + methodOrder));
			Path obsPath = profilePath.relativize(inversionResultPath.resolve("trace"));
			Files.createDirectories(profilePath);
			
			PrintWriter pw1 = new PrintWriter(Files.newBufferedWriter(profilePath.resolve("eventVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			PrintWriter pw2 = new PrintWriter(Files.newBufferedWriter(profilePath.resolve("eachVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pw1.println("# Event synVariance bornVariance varianceReduction;(syn - born)");
			pw2.println("# BasicID synVariance bornVariance varanceReduction;(syn - born)");
			
			Path stackRoot = inversionResultPath.resolve("stack/" + method + methodOrder +  "/" + phase + "/" + String.format("slowness_%.1f", diff_slowness));
			Files.createDirectories(stackRoot);
			
			PrintWriter pw4 = new PrintWriter(Files.newBufferedWriter(stackRoot.resolve("eventVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pw4.println("# Event synVariance bornVariance varianceReduction;(syn - born)");
			
			InversionResult ir = new InversionResult(inversionResultPath);
			List<BasicID> obsList = ir.getBasicIDList().stream().filter(id -> id.getWaveformType().equals(WaveformType.OBS))
				.collect(Collectors.toList());
			Set<GlobalCMTID> events = ir.idSet();
			
			double dt = 1. / obsList.get(0).getSamplingHz();
			
			Map<Station, Double> stationSynVariance = new HashMap<>();
			Map<Station, Double> stationBornVariance = new HashMap<>();
			Map<Station, Double> stationObsNorm = new HashMap<>();
//			obsList.stream().map(id -> id.getStation()).distinct().forEach(station -> {
//				stationSynVariance.put(station, 0.);
//				stationBornVariance.put(station, 0.);
//				stationObsNorm.put(station, 0.);
//			});
			for (Station station : ir.stationSet()) {
				stationSynVariance.put(station, 0.);
				stationBornVariance.put(station, 0.);
				stationObsNorm.put(station, 0.);
			}
			
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
							"set xlabel 'Time aligned on S-wave arrival (s)'",
							"set ylabel 'Distance (deg)'",
							"set xtics 10",
							"set ytics 5",
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
					String[] varString = new String[] {""};
					
					tmpObs.parallelStream().forEach(id -> {
//						i.incrementAndGet();
						try {
							RealVector bornVector = ir.bornOf(id, method, methodOrder).getYVector();
							double maxObs = ir.observedOf(id).getYVector().getLInfNorm();
							String name = ir.getTxtName(id);
							double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(id.getStation().getPosition())
									* 180. / Math.PI;
							if (id.getSacComponent().equals(SACComponent.R))
								scriptString_R[0] += "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($3/%.3e+%.2f) ", maxObs, distance) + "w lines lc \"black\",\\\n"
									+ "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($4/%.3e+%.2f) ", maxObs, distance) + "w lines lc \"red\",\\\n"
									+ "\"" + bornPath + "/" + name + "\" " + String.format("u 0:($2/%.3e+%.2f) ", maxObs, distance) + "w lines lc \"blue\",\\\n";
							else if (id.getSacComponent().equals(SACComponent.T))
							scriptString_T[0] += "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($3/%.3e+%.2f) ", maxObs, distance) + "w lines lc \"black\",\\\n"
									+ "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($4/%.3e+%.2f) ", maxObs, distance) + "w lines lc \"red\",\\\n"
									+ "\"" + bornPath + "/" + name + "\" " + String.format("u 0:($2/%.3e+%.2f) ", maxObs, distance) + "w lines lc \"blue\",\\\n";
							else if (id.getSacComponent().equals(SACComponent.Z))
								scriptString_Z[0] += "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($3/%.3e+%.2f) ", maxObs, distance) + "w lines lc \"black\",\\\n"
									+ "\"" + obsPath + "/" + name + "\" " + String.format("u 0:($4/%.3e+%.2f) ", maxObs, distance) + "w lines lc \"red\",\\\n"
									+ "\"" + bornPath + "/" + name + "\" " + String.format("u 0:($2/%.3e+%.2f) ", maxObs, distance) + "w lines lc \"blue\",\\\n";
//							pw.println("\"" + obsPath + "/" + name + "\" " + String.format("u ($1-8.4*%.2f):($3/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lc \"black\",\\");
//							pw.println("\"" + obsPath + "/" + name + "\" " + String.format("u ($2-8.4*%.2f):($4/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lc \"red\",\\");
////							if (i.get() == n)
////								pw.println("\"" + bornPath + "/" + name + "\" " + String.format("u ($1-8.4*%.2f):($2/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lc \"blue\"");
////							else
//								pw.println("\"" + bornPath + "/" + name + "\" " + String.format("u ($1-8.4*%.2f):($2/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lc \"blue\",\\");
							
							// output variance
							RealVector synVector = ir.syntheticOf(id).getYVector();
							RealVector obsVector = ir.observedOf(id).getYVector();
							double tmpSynVariance = synVector.subtract(obsVector).dotProduct(synVector.subtract(obsVector));
							double tmpBornVariance = bornVector.subtract(obsVector).dotProduct(bornVector.subtract(obsVector));
							double tmpObsNorm = obsVector.dotProduct(obsVector);
							
							double tmpSyn = Math.sqrt(tmpSynVariance / tmpObsNorm);
							double tmpBorn =  Math.sqrt(tmpBornVariance / tmpObsNorm);
							varString[0] += id + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn) + "\n";
//							pw2.println(id + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn));
							
							synVariance[0] += tmpSynVariance;
							bornVariance[0] += tmpBornVariance;
							obsNorm[0] += tmpObsNorm;
							
							totalSynVariance[0] += tmpSynVariance;
							totalBornVariance[0] += tmpBornVariance;
							totalObsNorm[0] += tmpObsNorm;
							
							Station sta = id.getStation();
							try {
								stationSynVariance.put(sta, stationSynVariance.get(sta) + tmpSynVariance);
								stationBornVariance.put(sta, stationBornVariance.get(sta) + tmpBornVariance);
								stationObsNorm.put(sta, stationObsNorm.get(sta) + tmpObsNorm);
							} catch (NullPointerException e) {
								System.err.println(sta + " " + sta.getPosition());
								e.printStackTrace();
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
					pw2.print(varString[0]);
					
					pw_R.close();
					pw_T.close();
					pw_Z.close();
					pw2.close();
					
					double tmpSyn = synVariance[0] / obsNorm[0]; //Math.sqrt(synVariance[0] / obsNorm[0]);
					double tmpBorn =  bornVariance[0] / obsNorm[0]; //Math.sqrt(bornVariance[0] / obsNorm[0]);
					pw1.println(event + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn));
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				int m = 140;
				int l = 36;
				
				RealVector[][] obsVectors = new RealVector[m][l+1];
				RealVector[][] synVectors = new RealVector[m][l+1];
				RealVector[][] bornVectors = new RealVector[m][l+1];
				
				int nBuff = 150;
				
				for (int i = 0; i < m; i++) {
					for (int j = 0; j < l+1; j++) {
						obsVectors[i][j] = new ArrayRealVector(nBuff);
						synVectors[i][j] = new ArrayRealVector(nBuff);
						bornVectors[i][j] = new ArrayRealVector(nBuff);
					}
				}
				
				Path stackPath = stackRoot.resolve(event.toString());
				Files.createDirectories(stackPath);
				
				double synVariance = 0;
				double bornVariance = 0;
				double obsNorm = 0;
				
				for (BasicID id : tmpObs) {
					if (!Stream.of(id.getPhases()).collect(Collectors.toSet()).contains(phase))
						continue;
					RealVector obsVector = ir.observedOf(id).getYVector();
					RealVector synVector = ir.syntheticOf(id).getYVector();
					RealVector bornVector = ir.bornOf(id, method, methodOrder).getYVector();
					
					double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(id.getStation().getPosition())
							* 180. / Math.PI;
					double azimuth = Math.toDegrees(id.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(id.getStation().getPosition()));
					int i = (int) distance;
					int j = (int) (azimuth / 10);
					
					//apply differential slowness shift
					double shift = ((distance - (int) (distance)) * diff_slowness) / dt;
					
					obsVectors[i][0] = add(obsVectors[i][0], obsVector, shift);
					synVectors[i][0] = add(synVectors[i][0], synVector, shift);
					bornVectors[i][0] = add(bornVectors[i][0], bornVector, shift);
					
					obsVectors[i][j+1] = add(obsVectors[i][j+1], obsVector, shift);
					synVectors[i][j+1] = add(synVectors[i][j+1], synVector, shift);
					bornVectors[i][j+1] = add(bornVectors[i][j+1], bornVector, shift);
					
					double tmpSynVariance = synVector.subtract(obsVector).dotProduct(synVector.subtract(obsVector));
					double tmpBornVariance = bornVector.subtract(obsVector).dotProduct(bornVector.subtract(obsVector));
					double tmpObsNorm = obsVector.dotProduct(obsVector);
					
					synVariance += tmpSynVariance;
					bornVariance += tmpBornVariance;
					obsNorm += tmpObsNorm;
				}
					double tmpSyn = Math.sqrt(synVariance / obsNorm);
					double tmpBorn =  Math.sqrt(bornVariance / obsNorm);
					pw4.println(event + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn));
					
				Path outPlot = stackPath.resolve(event + ".plt");
				Files.deleteIfExists(outPlot);
				
				try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPlot, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
					pw.println("set terminal postscript enhanced color font \"Helvetica,14\"");
					pw.println("set output \"" + event + ".ps\"");
					pw.println("unset key");
					pw.println("set yrange [66:96]");
					pw.println("set xlabel 'Time aligned on S-wave arrival (s)'");
					pw.println("set ylabel 'Distance (deg)'");
					pw.println("set xtics 10");
					pw.println("set ytics 5");
					pw.println("set size .5,1");
					pw.print("p ");
					
					for (int i = 0; i < m; i++) {
						if (obsVectors[i][0] == null)
							continue;
						Path outObs = stackPath.resolve(String.format("obsStack_%s_%d-%d.txt", event, i, i+1));
						Path outSyn = stackPath.resolve(String.format("synStack_%s_%d-%d.txt", event, i, i+1));
						Path outBorn = stackPath.resolve(String.format("bornStack_%s_%d-%d.txt", event, i, i+1));
						
						double maxObs = obsVectors[i][0].getLInfNorm();
						
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outObs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < obsVectors[i][0].getDimension(); k++)
								pw3.println(k*dt + " " + obsVectors[i][0].getEntry(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outSyn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < synVectors[i][0].getDimension(); k++)
								pw3.println(k*dt + " " + synVectors[i][0].getEntry(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outBorn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < bornVectors[i][0].getDimension(); k++)
								pw3.println(k*dt + " " + bornVectors[i][0].getEntry(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						
						pw.println("\"" + outObs.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 1.5, i/1.) + "w lines lw .5 lc \"black\",\\");
						pw.println("\"" + outSyn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 1.5, i/1.) + "w lines lw .5 lc \"red\",\\");
						pw.println("\"" + outBorn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 1.5, i/1.) + "w lines lw .5 lc \"blue\",\\");
					}
					pw.println();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				for (int j = 0; j < l; j++) {
					boolean pass = true;
					for (int i = 0; i < m; i++)
						if (obsVectors[i][j+1] != null)
							pass = false;
					if (pass)
						continue;
					outPlot = stackPath.resolve(event + ".az" + j + ".plt");
					Files.deleteIfExists(outPlot);
					PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPlot, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					
					pw.println("set terminal postscript enhanced color font \"Helvetica,14\"");
					pw.println("set output \"" + event + ".az" + j + ".ps\"");
					pw.println("unset key");
					pw.println("set yrange [68:87]");
					pw.println("set xlabel 'Time aligned on S-wave arrival (s)'");
					pw.println("set ylabel 'Distance (deg)'");
					pw.println("set xtics 10");
					pw.println("set ytics 5");
					pw.println("set size .5,1");
					pw.print("p ");
					
					for (int i = 0; i < m; i++) {
						if (obsVectors[i][j+1] == null)
							continue;
						Path outObs = stackPath.resolve(String.format("obsStack_%s_%d-%d.az" + j + ".txt", event, i, i+1));
						Path outSyn = stackPath.resolve(String.format("synStack_%s_%d-%d.az" + j + ".txt", event, i, i+1));
						Path outBorn = stackPath.resolve(String.format("bornStack_%s_%d-%d.az" + j + ".txt", event, i, i+1));
						
						double maxObs = obsVectors[i][j+1].getLInfNorm();
						
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outObs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < obsVectors[i][j+1].getDimension(); k++)
								pw3.println(k*dt + " " + obsVectors[i][j+1].getEntry(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outSyn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < synVectors[i][j+1].getDimension(); k++)
								pw3.println(k*dt + " " + synVectors[i][j+1].getEntry(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outBorn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < bornVectors[i][j+1].getDimension(); k++)
								pw3.println(k*dt + " " + bornVectors[i][j+1].getEntry(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						
						pw.println("\"" + outObs.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 1.5, i/1.) + "w lines lw .5 lc \"black\",\\");
						pw.println("\"" + outSyn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 1.5, i/1.) + "w lines lw .5 lc \"red\",\\");
						pw.println("\"" + outBorn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%.1f) ", maxObs * 1.5, i/1.) + "w lines lw .5 lc \"blue\",\\");
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
			pw1 = new PrintWriter(Files.newBufferedWriter(stackRoot.resolve("stationVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pw1.println("# Station synVariance bornVariance varianceReduction;(syn - born)");
			
			for (Station sta : stationObsNorm.keySet()) {
				try {
				double bornVariance = stationBornVariance.get(sta) / stationObsNorm.get(sta);
				double synVariance = stationSynVariance.get(sta) / stationObsNorm.get(sta);
				pw1.println(sta.getName() + " " + sta.getNetwork() + " " + sta.getPosition() 
						+ " " + synVariance + " " + bornVariance + " " + (synVariance - bornVariance));
				} catch (NullPointerException e) {
					System.err.println(sta + " " + sta.getPosition());
				}
			}
			pw1.close();
			
			// write total variance
			pw2 = new PrintWriter(Files.newBufferedWriter(inversionResultPath.resolve("totalVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pw2.println("SynVariance bornVariance varianceReduction;(syn - born)");
			
			double bornVariance = totalBornVariance[0] / totalObsNorm[0];
			double synVariance = totalSynVariance[0] / totalObsNorm[0];
			pw2.println(synVariance + " " + bornVariance + " " + (synVariance - bornVariance));
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
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static RealVector add(RealVector v1, RealVector v2, double shift) {
		RealVector res = v1;
		
		//upsample
		int finalSamplingHz = 20;
		int nStart = (int) ((30 + shift) * finalSamplingHz);
		
		RealVector resamRes = new ArrayRealVector(IntStream.range(0, v1.getDimension() * finalSamplingHz)
				.mapToDouble(i -> v1.getEntry((int) (i/finalSamplingHz))).toArray());
		RealVector resamV2 = new ArrayRealVector(IntStream.range(0, v2.getDimension() * finalSamplingHz)
				.mapToDouble(i -> v2.getEntry((int) (i/finalSamplingHz))).toArray());
		
		for (int i = 0; i < resamRes.getDimension(); i++)
			if (i >= nStart && i < resamV2.getDimension() + nStart)
				resamRes.setEntry(i, resamRes.getEntry(i) + resamV2.getEntry(i - nStart));
		
		//downsample
		res = new ArrayRealVector(IntStream.range(0, v1.getDimension())
				.mapToDouble(i -> resamRes.getEntry((int) (i * finalSamplingHz))).toArray());
		
		return res;
	}
	
	private static Trace addAndPadd(Trace trace1, Trace trace2) {
		double t0 = Math.min(trace1.getXAt(0), trace2.getXAt(0));
		double t1 = Math.max(trace1.getXAt(trace1.getLength() - 1), trace2.getXAt(trace2.getLength() - 1));
		
		double dt = trace1.getXAt(1) - trace1.getXAt(0);
		int n = (int) ((t1 - t0) / dt) + 1;
		
		double[] xs = IntStream.range(0, n).mapToDouble(i -> t0 + i*dt).toArray();
		double[] ys = new double[n];
		
		if (trace1.getXAt(0) < trace2.getXAt(0)) {
			ys = Arrays.copyOf(trace1.getY(), n);
			int istartOther = (int) ((trace2.getXAt(0) - trace1.getXAt(0)) / dt);
			for (int i = 0; i < trace2.getLength(); i++)
				ys[i + istartOther] += trace2.getYAt(i);
		}
		else if (trace2.getXAt(0) < trace1.getXAt(0)) {
			ys = Arrays.copyOf(trace2.getY(), n);
			int istartOther = (int) ((trace1.getXAt(0) - trace2.getXAt(0)) / dt);
			for (int i = 0; i < trace1.getLength(); i++)
				ys[i + istartOther] += trace1.getYAt(i);
		}
		
		return new Trace(xs, ys);
	}
}
