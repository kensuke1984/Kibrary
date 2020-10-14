package io.github.kensuke1984.kibrary.waveformdata.addons;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

public class Vespagram {
	
	public static void main(String[] args) {
		Path waveformIDPath = Paths.get(args[0]);
		Path waveformPath = Paths.get(args[1]);
		
		double ds = 0.1;
		double smax = 3;
		double tbefore = 15;
		
		try {
			BasicID[] waveforms = BasicIDFile.read(waveformIDPath, waveformPath);
			Set<GlobalCMTID> events = Stream.of(waveforms).map(BasicID::getGlobalCMTID).collect(Collectors.toSet());
			
			for (GlobalCMTID event : events) {
				Vespagram vespagram = new Vespagram(waveforms, event, ds, smax);
				double distanceBin = 70.1;
				double[][] obsVespa = vespagram.computeObs(distanceBin);
				double[][] synVespa = vespagram.computeSyn(distanceBin);
				
				Path outpathObs = Paths.get(event + "." 
						+ String.format("%.1f-%.1f", vespagram.getDistanceBinMin(distanceBin), vespagram.getDistanceBinMax(distanceBin))
						+ ".obs.vespa");
				Path outpathSyn = Paths.get(event + "." 
						+ String.format("%.1f-%.1f", vespagram.getDistanceBinMin(distanceBin), vespagram.getDistanceBinMax(distanceBin))
						+ ".syn.vespa");
				
				if (obsVespa != null)
					vespagram.writeVespa(outpathObs, obsVespa, tbefore);
				if (synVespa != null)
					vespagram.writeVespa(outpathSyn, synVespa, tbefore);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BasicID[] obsIDs;
	private BasicID[] synIDs;
	private int[] id2bin;
	private Set<Integer> usedBinIndexes;
	private int[] minNPTSinBin;
	private int[] recordsinBin;
	private double ds;
	private double smax;
	private double samplingHz;
	private GlobalCMTID event;
	
	private final double deltaDistance = 5.;  
	
	public Vespagram(BasicID[] waveforms, GlobalCMTID event, double ds, double smax) {
		this.event = event;
		sort(waveforms);
		id2bin = new int[obsIDs.length];
		minNPTSinBin = new int[(int) (180 / deltaDistance)];
		for (int i = 0; i < minNPTSinBin.length; i++) {
			minNPTSinBin[i] = Integer.MAX_VALUE;
		}
		recordsinBin = new int[(int) (180 / deltaDistance)];
		usedBinIndexes = new HashSet<>();
		for (int i = 0; i < obsIDs.length; i++) {
			double distance = Math.toDegrees(obsIDs[i].getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obsIDs[i].getStation().getPosition()));
			int ibin = (int) (distance / deltaDistance);
			id2bin[i] = ibin;
			usedBinIndexes.add(ibin);
			recordsinBin[ibin]++;
			
			int npts = obsIDs[i].getNpts();
			if (npts < minNPTSinBin[ibin])
				minNPTSinBin[ibin] = npts;
		}
		this.ds = ds;
		this.smax = smax;
		samplingHz = waveforms[0].getSamplingHz();
	}
	
	public double[][] computeObs(double distanceBin) {
		int ibin = (int) (distanceBin / deltaDistance);
		double distance0 = ibin * deltaDistance;
		if (!usedBinIndexes.contains(ibin)) {
			System.out.println("No records at epicentral distance " + distanceBin);
			return null;
		}
		
		System.out.println(minNPTSinBin[ibin]);
		
		int ns = (int) (smax / ds) * 2 + 1;
		int nt = minNPTSinBin[ibin] - (int) (2 * smax * deltaDistance * samplingHz);
		double[][] vespa = new double[ns][nt];
		
		for (int is = 0; is < ns; is++) {
			double slowness = is * ds - smax;
			for (int i = 0; i < id2bin.length; i++) {
				if (id2bin[i] != ibin)
					continue;
				
				double[] data = obsIDs[i].getData();
				double max = new ArrayRealVector(data).getLInfNorm();
				
				double distance = Math.toDegrees(obsIDs[i].getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obsIDs[i].getStation().getPosition()));
				double timeshift = smax * deltaDistance - slowness * (distance - distance0);
				int shift = (int) (timeshift * samplingHz);
				
				for (int it = 0; it < nt; it++)
					vespa[is][it] += data[it + shift] / max / recordsinBin[ibin];
			}
		}
		
		// Normalize vespagram
		double maxVespa = 0;
		for (int is = 0; is < ns; is++)
			for (int it = 0; it < nt; it++)
				if (Math.abs(vespa[is][it]) > maxVespa)
					maxVespa = Math.abs(vespa[is][it]);
		
		for (int is = 0; is < ns; is++)
			for (int it = 0; it < nt; it++)
				vespa[is][it] = Math.abs(vespa[is][it]) / maxVespa;
		
		return vespa;
	}
	
	public double[][] computeSyn(double distanceBin) {
		int ibin = (int) (distanceBin / deltaDistance);
		double distance0 = ibin * deltaDistance;
		if (!usedBinIndexes.contains(ibin)) {
			System.out.println("No records at epicentral distance " + distanceBin);
			return null;
		}
		
		int ns = (int) (smax / ds) * 2 + 1;
		int nt = minNPTSinBin[ibin] - (int) (2 * smax * deltaDistance * samplingHz);
		double[][] vespa = new double[ns][nt];
		
		for (int is = 0; is < ns; is++) {
			double slowness = is * ds - smax;
			for (int i = 0; i < id2bin.length; i++) {
				if (id2bin[i] != ibin)
					continue;
				
				double[] data = synIDs[i].getData();
				double max = new ArrayRealVector(data).getLInfNorm();
				
				double distance = Math.toDegrees(synIDs[i].getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(synIDs[i].getStation().getPosition()));
				double timeshift = smax * deltaDistance - slowness * (distance - distance0);
				int shift = (int) (timeshift * samplingHz);
				
				for (int it = 0; it < nt; it++)
					vespa[is][it] += data[it + shift] / max / recordsinBin[ibin];
			}
		}
		
		// Normalize vespagram
		double maxVespa = 0;
		for (int is = 0; is < ns; is++)
			for (int it = 0; it < nt; it++)
				if (Math.abs(vespa[is][it]) > maxVespa)
					maxVespa = Math.abs(vespa[is][it]);
		
		for (int is = 0; is < ns; is++)
			for (int it = 0; it < nt; it++)
				vespa[is][it] = Math.abs(vespa[is][it]) / maxVespa;
		
		return vespa;
	}
	
	public void writeVespa(Path outpath, double[][] vespa, double tbefore) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int is = 0; is < vespa.length; is++) {
			double slowness = -(is * ds - smax);
			for (int it = 0; it < vespa[0].length; it++) {
				double t = it / samplingHz - tbefore + smax * deltaDistance;
				pw.println(String.format("%.2f %.2f", t, slowness) + " " + vespa[is][it] + " " + Math.log10(vespa[is][it]));
			}
		}
		pw.close();
	}
	
	public double getDistanceBinMin(double distanceBin) {
		return (int) (distanceBin / deltaDistance) * deltaDistance;
	}
	
	public double getDistanceBinMax(double distanceBin) {
		return (int) (distanceBin / deltaDistance) * deltaDistance + deltaDistance;
	}
	
	private void sort(BasicID[] ids) {
		Predicate<BasicID> chooser = id -> id.getGlobalCMTID().equals(event);
		
		List<BasicID> obsList = Arrays.stream(ids).filter(id -> id.getWaveformType() == WaveformType.OBS)
				.filter(chooser::test).collect(Collectors.toList());

		for (int i = 0; i < obsList.size(); i++)
			for (int j = i + 1; j < obsList.size(); j++)
				if (obsList.get(i).equals(obsList.get(j)))
					throw new RuntimeException("Duplicate observed detected");

		List<BasicID> synList = Arrays.stream(ids).filter(id -> id.getWaveformType() == WaveformType.SYN)
				.filter(chooser::test).collect(Collectors.toList());

		for (int i = 0; i < synList.size() - 1; i++)
			for (int j = i + 1; j < synList.size(); j++)
				if (synList.get(i).equals(synList.get(j)))
					throw new RuntimeException("Duplicate synthetic detected");

		System.out.println("Number of obs IDs before pairing with syn IDs = " + obsList.size());
		if (obsList.size() != synList.size())
			System.out.println("The numbers of observed IDs " + obsList.size() + " and " + " synthetic IDs "
					+ synList.size() + " are different ");
		int size = obsList.size() < synList.size() ? synList.size() : obsList.size();

		List<BasicID> useObsList = new ArrayList<>(size);
		List<BasicID> useSynList = new ArrayList<>(size);

		for (int i = 0; i < synList.size(); i++) {
			boolean foundPair = false;
			for (int j = 0; j < obsList.size(); j++) {
				if (Dvector.isPair(synList.get(i), obsList.get(j))) {
					useObsList.add(obsList.get(j));
					useSynList.add(synList.get(i));
					foundPair = true;
					break;
				}
			}
			if (!foundPair)
				System.out.println("Didn't find OBS for " + synList.get(i));
		}
		
		obsIDs = useObsList.toArray(new BasicID[0]);
		synIDs = useSynList.toArray(new BasicID[0]);
	}
}
