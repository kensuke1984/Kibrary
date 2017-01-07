package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;


public class Profile {
	public static void main(String[] args) {
		int methodOrder = Integer.parseInt(args[0]);
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
		
		try {
			Path profilePath = inversionResultPath.resolve("profile").resolve(method.name() + methodOrder);
			Path bornPath = profilePath.relativize(inversionResultPath.resolve("born/" + method + methodOrder));
			Path obsPath = profilePath.relativize(inversionResultPath.resolve("trace"));
			Files.createDirectories(profilePath);
			
			PrintWriter pw1 = new PrintWriter(Files.newBufferedWriter(profilePath.resolve("eventVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			PrintWriter pw2 = new PrintWriter(Files.newBufferedWriter(profilePath.resolve("eachVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pw1.println("# Event synVariance bornVariance varianceReduction;(syn - born)");
			pw2.println("# BasicID synVariance bornVariance varanceReduction;(syn - born)");
			
			Path stackRoot = inversionResultPath.resolve("stack/" + method + methodOrder +  "/" + phase);
			Files.createDirectories(stackRoot);
			
			PrintWriter pw4 = new PrintWriter(Files.newBufferedWriter(stackRoot.resolve("eventVariance.inf"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
			pw4.println("# Event synVariance bornVariance varianceReduction;(syn - born)");
			
			InversionResult ir = new InversionResult(inversionResultPath);
			List<BasicID> obsList = ir.getBasicIDList().stream().filter(id -> id.getWaveformType().equals(WaveformType.OBS))
				.collect(Collectors.toList());
			Set<GlobalCMTID> events = ir.idSet();
			for (GlobalCMTID event : events) {
				List<BasicID> tmpObs = obsList.stream().filter(id -> id.getGlobalCMTID().equals(event)).collect(Collectors.toList());
				
				Path outpath = inversionResultPath.resolve(profilePath.resolve(event.toString() + ".plt"));
				try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
					pw.println("set terminal postscript enhanced color font \"Helvetica,12\"");
					pw.println("set output \"" + event + ".ps\"");
					pw.println("unset key");
					pw.println("set size .5,1");
					pw.print("p ");
					int n = tmpObs.size();
					int i = 0;
					double synVariance = 0;
					double bornVariance = 0;
					double obsNorm = 0;
					for (BasicID id : tmpObs) {
						i++;
						RealVector bornVector = ir.bornOf(id, method, methodOrder).getYVector();
						double maxObs = ir.observedOf(id).getYVector().getLInfNorm();
						String name = ir.getTxtName(id);
						double distance = id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(id.getStation().getPosition())
								* 180. / Math.PI;
						pw.println("\"" + obsPath + "/" + name + "\" " + String.format("u ($1-8.4*%.2f):($3/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lc \"black\",\\");
						pw.println("\"" + obsPath + "/" + name + "\" " + String.format("u ($2-8.4*%.2f):($4/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lc \"red\",\\");
						if (i == n)
							pw.println("\"" + bornPath + "/" + name + "\" " + String.format("u ($1-8.4*%.2f):($2/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lc \"blue\"");
						else
							pw.println("\"" + bornPath + "/" + name + "\" " + String.format("u ($1-8.4*%.2f):($2/%.3e+%.2f) ", distance, maxObs, distance) + "w lines lc \"blue\",\\");
						
						// output variance
						RealVector synVector = ir.syntheticOf(id).getYVector();
						RealVector obsVector = ir.observedOf(id).getYVector();
						double tmpSynVariance = synVector.subtract(obsVector).dotProduct(synVector.subtract(obsVector));
						double tmpBornVariance = bornVector.subtract(obsVector).dotProduct(bornVector.subtract(obsVector));
						double tmpObsNorm = obsVector.dotProduct(obsVector);
						
						double tmpSyn = Math.sqrt(tmpSynVariance / tmpObsNorm);
						double tmpBorn =  Math.sqrt(tmpBornVariance / tmpObsNorm);
						pw2.println(id + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn));
						
						synVariance += tmpSynVariance;
						bornVariance += tmpBornVariance;
						obsNorm += tmpObsNorm;
					}
					double tmpSyn = Math.sqrt(synVariance / obsNorm);
					double tmpBorn =  Math.sqrt(bornVariance / obsNorm);
					pw1.println(event + " " + tmpSyn + " " + tmpBorn + " " + (tmpSyn - tmpBorn));
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				int m = 140;
				
				RealVector[] obsVectors = new RealVector[m];
				RealVector[] synVectors = new RealVector[m];
				RealVector[] bornVectors = new RealVector[m];
				
				
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
					int i = (int) (distance);
					
					obsVectors[i] = obsVectors[i] == null ? obsVector : add(obsVectors[i], obsVector);
					synVectors[i] = synVectors[i] == null ? synVector : add(synVectors[i], synVector);
					bornVectors[i] = bornVectors[i] == null ? bornVector : add(bornVectors[i], bornVector);
					
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
					pw.println("set terminal postscript enhanced color font \"Helvetica,12\"");
					pw.println("set output \"" + event + ".ps\"");
					pw.println("unset key");
					pw.println("set size .5,1");
					pw.print("p ");
					
					for (int i = 0; i < m; i++) {
						if (obsVectors[i] == null)
							continue;
						Path outObs = stackPath.resolve(String.format("obsStack_%s_%d-%d.txt", event, i, i+1));
						Path outSyn = stackPath.resolve(String.format("synStack_%s_%d-%d.txt", event, i, i+1));
						Path outBorn = stackPath.resolve(String.format("bornStack_%s_%d-%d.txt", event, i, i+1));
						
						double maxObs = obsVectors[i].getLInfNorm();
						
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outObs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < obsVectors[i].getDimension(); k++)
								pw3.println(k + " " + obsVectors[i].getEntry(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outSyn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < synVectors[i].getDimension(); k++)
								pw3.println(k + " " + synVectors[i].getEntry(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						try (PrintWriter pw3 = new PrintWriter(Files.newBufferedWriter(outBorn, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
							for (int k = 0; k < bornVectors[i].getDimension(); k++)
								pw3.println(k + " " + bornVectors[i].getEntry(k));
						} catch (IOException e) {
						e.printStackTrace();
						}
						
						pw.println("\"" + outObs.getFileName() + "\" " + String.format("u 1:($2/%.3e+%d) ", maxObs, i) + "w lines lc \"black\",\\");
						pw.println("\"" + outSyn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%d) ", maxObs, i) + "w lines lc \"red\",\\");
						pw.println("\"" + outBorn.getFileName() + "\" " + String.format("u 1:($2/%.3e+%d) ", maxObs, i) + "w lines lc \"blue\",\\");
					}
					pw.println();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			pw1.flush();
			pw1.close();
			pw4.flush();
			pw4.close();
			
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
}
