package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.inversion.ObservationEquation;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.RealMatrix;

import com.amazonaws.util.CollectionUtils;

public class Tradeoff {

	public static void main(String[] args) {
		Path root = Paths.get(".");
		Path partialIDPath = root.resolve("partialID.dat");
		Path partialPath = root.resolve("partial.dat");
		Path unknownPath = root.resolve("unknowns.inf");
		Path basicIDPath = root.resolve("waveformID.dat");
		Path basicPath = root.resolve("waveform.dat");
		
		Path outpathVL = root.resolve("VLcorrelations-all.txt");
		Path outpathV = root.resolve("Vcorrelations-all.txt");
		Path outpathL = root.resolve("Lcorrelations-all.txt");
		Path outpath2V = root.resolve("2Vcorrelations-all.txt");
		
		int[] npts = new int[] {131072, 262144};
		double minDistance = 17;
		double maxDistance = 100;
		double dR = 50;
		double dL = 5;
		
		Set<GlobalCMTID> eventSet = new HashSet<>();
		
		try {
			PartialID[] partials = PartialIDFile.read(partialIDPath, partialPath);
			BasicID[] waveforms = BasicIDFile.read(basicIDPath, basicPath);
			
			List<UnknownParameter> parameterList = UnknownParameterFile.read(unknownPath);
			
			List<Double> perturbationRs = parameterList.stream().map(p -> p.getLocation().getR())
					.distinct().collect(Collectors.toList());
			Collections.sort(perturbationRs);
			Collections.reverse(perturbationRs);
			
			List<UnknownParameter> orderedParameterList = new ArrayList<>();
			perturbationRs.stream().forEachOrdered(pR -> {
				parameterList.stream().filter(p -> p.getLocation().getR() == pR)
					.forEachOrdered(p -> orderedParameterList.add(p));
			});
			
//			System.out.println("DEBUG0: " + orderedParameterList.size() + " " + parameterList.size());
			
			//for test on a small dataset
			GlobalCMTID oneID = Stream.of(waveforms)
					.map(id -> id.getGlobalCMTID()).collect(Collectors.toList()).get(0);
//			System.out.println(oneID);
//			BasicID[] oneIDwaveforms = Stream.of(waveforms).filter(id -> id.getGlobalCMTID().equals(oneID))
//				.collect(Collectors.toList()).toArray(new BasicID[0]);
//			Dvector dVector = new Dvector(oneIDwaveforms);
			
			//using one record
//			BasicID[] oneWaveform = new BasicID[] {waveforms[0], waveforms[1]};
//			System.out.println("Using one record: " + oneWaveform[0].getGlobalCMTID() + " " + oneWaveform[0].getStation());
//			Dvector dVector = new Dvector(oneWaveform);
			
			//using the whole dataset
//			Dvector dVector = new Dvector(waveforms);
			
			Predicate<BasicID> chooser = id -> {
				double distance = Math.toDegrees(id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(id.getStation().getPosition()));
				if (distance < minDistance)
					return false;
				return true;
			};
			
			Dvector dVector = new Dvector(waveforms, chooser, WeightingType.RECIPROCAL);
			System.out.println("Using the whole dataset, #waveforms = " + dVector.getNTimeWindow());
			ObservationEquation eq = new ObservationEquation(partials
					, orderedParameterList, dVector, false, false, null, null, null, null);
			
			Tradeoff trade = new Tradeoff(eq.getAtA(), eq.getParameterList());
			
//			System.out.println("DEBUG1: " + eq.getParameterList().size());
			
			double layer1R = 6321.0;
			double minDeltaR = Double.MAX_VALUE;
			double tmpLayerR = 0.;
			for (Double perturbationR : perturbationRs) {
				if (Math.abs(perturbationR - layer1R) < minDeltaR) {
					tmpLayerR = perturbationR;
					minDeltaR = Math.abs(perturbationR - layer1R);
				}
			}
			layer1R = tmpLayerR;
			double layer2Rmin = Earth.EARTH_RADIUS - 900;
			double layer2Rmax = Earth.EARTH_RADIUS - 300.;
			
			System.out.println("Computing correlation between voxels in layer " + layer1R
					+ " and voxels between radii " + layer2Rmin + "~" + layer2Rmax);
			double[][] correlations = trade.computeForLayer(layer1R, layer2Rmin, layer2Rmax, perturbationRs);
			UnknownParameter[][] unknownGrid = trade.unknownsForLayer(layer1R, layer2Rmin, layer2Rmax, perturbationRs);
			
			Path correlationFile = root.resolve(String.format("correlation_%.0f.txt", layer1R));
			try (BufferedWriter writer = Files.newBufferedWriter(correlationFile)) {
				for (int i = 0; i < unknownGrid[0].length; i++)
					writer.write(unknownGrid[0][i].getLocation().toString() + " ");
				writer.write("\n");
				for (int j = 0; j < correlations[0].length; j++) {
					writer.write(unknownGrid[1][j].getLocation().toString() + " ");
					for (int i = 0; i < correlations.length; i++) {
						writer.write(String.format("%.8f", correlations[i][j]) + " ");
					}
					writer.write("\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Tradeoff(RealMatrix AtA, List<UnknownParameter> parameterList) {
		this.ata = AtA;
		this.parameters = parameterList;
		this.paramOrderedLocs = parameterList.stream().map(p -> p.getLocation())
				.collect(Collectors.toList()).toArray(new Location[0]);
	}
	
	public double[] verticalToLateralCorrelation(Location[] voxelLocs) {
		return IntStream.range(0, voxelLocs.length)
				.mapToDouble(i -> verticalToLateralCorrelation(voxelLocs[i]))
				.toArray();
	}
	
	public double[] verticalCorrelation(Location[] voxelLocs) {
		return IntStream.range(0, voxelLocs.length)
				.mapToDouble(i -> verticalCorrelation(voxelLocs[i]))
				.toArray();
	}
	
	public double[] verticalCorrelation2(Location[] voxelLocs) {
		return IntStream.range(0, voxelLocs.length)
				.mapToDouble(i -> verticalCorrelation2(voxelLocs[i]))
				.toArray();
	}
	
	public double[] lateralCorrelation(Location[] voxelLocs) {
		return IntStream.range(0, voxelLocs.length)
				.mapToDouble(i -> lateralCorrelation(voxelLocs[i]))
				.toArray();
	}
	
	public double verticalToLateralCorrelation(Location voxelLoc) {
		return verticalCorrelation(voxelLoc) / lateralCorrelation(voxelLoc);
	}
	
	public double verticalCorrelation(Location voxelLoc) {
		double corr = 0;
		int[] index = getIndexOf2Vertical(voxelLoc);
		int ivoxel = getIndexOf(voxelLoc);
		
		for (int ivertical : index) {
			corr += Math.abs(this.ata.getEntry(ivoxel, ivertical)) / Math.sqrt(this.ata.getEntry(ivoxel, ivoxel) * this.ata.getEntry(ivertical, ivertical));
		}
		
		return corr / index.length;
	}
	
	public double verticalCorrelation2(Location voxelLoc) {
		double corr = 0;
		int[] index = getIndexOf2Vertical2(voxelLoc);
		int ivoxel = getIndexOf(voxelLoc);
		
		for (int ivertical : index) {
			corr += Math.abs(this.ata.getEntry(ivoxel, ivertical)) / Math.sqrt(this.ata.getEntry(ivoxel, ivoxel) * this.ata.getEntry(ivertical, ivertical));
		}
		
		return corr / index.length;
	}
	
	public double lateralCorrelation(Location voxelLoc) {
		double corr = 0;
		int[] index = getIndexOf4Lateral(voxelLoc);
		int ivoxel = getIndexOf(voxelLoc);
		
		for (int ilateral : index) {
			corr += Math.abs(this.ata.getEntry(ivoxel, ilateral)) / Math.sqrt(this.ata.getEntry(ivoxel, ivoxel) * this.ata.getEntry(ilateral, ilateral));
		}
		
		return corr / index.length;
	}
	
	public static Location[] readPartialLocation(Path parPath) {
		try {
			return Files.readAllLines(parPath).stream()
					.map(Tradeoff::toLocation)
					.toArray(n -> new Location[n]);
		} catch (Exception e) {
			throw new RuntimeException("par file has problems");
		}
	}
	
	private static Location toLocation(String line) {
		String[] parts = line.split("\\s+");
		return new Location(Double.parseDouble(parts[1]),
				Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
	}
	
	private int[] getIndexOf4Lateral(Location voxelLoc) throws OutOfRangeException {
		int[] tmp = new int[4];
		
		int j = 0;
		for (int i=0; i < this.paramOrderedLocs.length; i++) {
			Location loci = this.paramOrderedLocs[i];
				if (isLateralNeighbor(voxelLoc, loci)) {
					tmp[j] = i;
					j++;
				}
		}
		if (j == 0)
			throw new OutOfRangeException(j, 1, 4);
		
		int[] index = new int[j];
		for (int i=0; i < j; i++)
			index[i] = tmp[i];
		
		return index;
	}
	
	private int[] getIndexOf2Vertical(Location voxelLoc) throws OutOfRangeException {
		int[] tmp = new int[2];
		
		int j = 0;
		for (int i=0; i < this.paramOrderedLocs.length; i++) {
			Location loci = this.paramOrderedLocs[i];
			if (isVerticalNeighbor(voxelLoc, loci)) {
				tmp[j] = i;
				j++;
			}
		}
		if (j == 0)
			throw new OutOfRangeException(j, 1, 2);
		
		int[] index = new int[j];
		for (int i=0; i < j; i++)
			index[i] = tmp[i];
			
		return index;
	}
	
	private int[] getIndexOf2Vertical2(Location voxelLoc) throws OutOfRangeException {
		int[] tmp = new int[2];
		
		int j = 0;
		for (int i=0; i < this.paramOrderedLocs.length; i++) {
			Location loci = this.paramOrderedLocs[i];
			if (isVerticalSecondNeighbor(voxelLoc, loci)) {
				tmp[j] = i;
				j++;
			}
		}
		if (j == 0)
			throw new OutOfRangeException(j, 1, 2);
		
		int[] index = new int[j];
		for (int i=0; i < j; i++)
			index[i] = tmp[i];
			
		return index;
	}
	
	private boolean isLateralNeighbor(Location loc, Location o) {
		boolean res = false;
		if (loc.getR() == o.getR()) {
			if (o.getLongitude() == loc.getLongitude() + dL && o.getLatitude() == loc.getLatitude())
				res = true;
			else if (o.getLongitude() == loc.getLongitude() - dL && o.getLatitude() == loc.getLatitude())
				res = true;
			else if (o.getLatitude() == loc.getLatitude() + dL && o.getLongitude() == loc.getLongitude())
				res = true;
			else if (o.getLatitude() == loc.getLatitude() - dL && o.getLongitude() == loc.getLongitude())
				res = true;
		}
		return res;
	}
	
	private boolean isVerticalNeighbor(Location loc, Location o) {
		boolean res = false;
		if (loc.getLongitude() == o.getLongitude() && loc.getLatitude() == o.getLatitude()) {
			if (o.getR() == loc.getR() + dR)
				res = true;
			else if (o.getR() == loc.getR() - dR)
				res = true;
		}
		return res;
	}
	
	private boolean isVerticalSecondNeighbor(Location loc, Location o) {
		boolean res = false;
		if (loc.getLongitude() == o.getLongitude() && loc.getLatitude() == o.getLatitude()) {
			if (o.getR() == loc.getR() + 2 * dR)
				res = true;
			else if (o.getR() == loc.getR() - 2 * dR)
				res = true;
		}
		return res;
	}
	
	private int getIndexOf(Location voxelLoc) throws OutOfRangeException {
		int index = -1;
		
		for (int i=0; i < this.paramOrderedLocs.length; i++)
			if (this.paramOrderedLocs[i].equals(voxelLoc))
				index = i;
		
		if (index == -1)
			throw new OutOfRangeException(-1, 0, this.paramOrderedLocs.length);
		return index;
	}
	
	private double[][] computeForLayer(double layer1R, double layer2Rmin, double layer2Rmax, List<Double> orderedR) {
		List<Integer> indexLayer1 = IntStream.range(0, parameters.size()).filter(i -> parameters.get(i).getLocation().getR() == layer1R)
				.boxed().collect(Collectors.toList());
		List<Integer> indexLayer2 = IntStream.range(0, parameters.size()).filter(i -> {
			double pR = parameters.get(i).getLocation().getR();
			if (pR >= layer2Rmin && pR <= layer2Rmax)
				return true;
			else
				return false;
		}).boxed().collect(Collectors.toList());
		
//		System.out.println("DEBUG2 : " + ata.getColumnDimension());
		
		double[][] correlations = new double[indexLayer1.size()][];
		for (int i = 0; i < indexLayer1.size(); i++) {
			correlations[i] = new double[indexLayer2.size()];
			for (int j = 0; j < indexLayer2.size(); j++) {
				int ia = indexLayer1.get(i);
				int ja = indexLayer2.get(j);
				correlations[i][j] = ata.getEntry(ia, ja)
						/ Math.sqrt(ata.getEntry(ia, ia) * ata.getEntry(ja, ja));
			}
		}
		
		return correlations;
	}
	
	private UnknownParameter[][] unknownsForLayer(double layer1R, double layer2Rmin, double layer2Rmax, List<Double> orderedR) {
		List<Integer> indexLayer1 = IntStream.range(0, parameters.size()).filter(i -> parameters.get(i).getLocation().getR() == layer1R)
				.boxed().collect(Collectors.toList());
		List<Integer> indexLayer2 = IntStream.range(0, parameters.size()).filter(i -> {
			double pR = parameters.get(i).getLocation().getR();
			if (pR >= layer2Rmin && pR <= layer2Rmax)
				return true;
			else
				return false;
		}).boxed().collect(Collectors.toList());
		
		UnknownParameter[][] unknowns = new UnknownParameter[2][];
		unknowns[0] = new UnknownParameter[indexLayer1.size()];
		unknowns[1] = new UnknownParameter[indexLayer2.size()];
		for (int i = 0; i < indexLayer1.size(); i++)
			unknowns[0][i] = parameters.get(indexLayer1.get(i));
		for (int i = 0; i < indexLayer2.size(); i++)
			unknowns[1][i] = parameters.get(indexLayer2.get(i));
		
		return unknowns;
	}
	
	private Location[] paramOrderedLocs;
	private RealMatrix ata;
	private List<UnknownParameter> parameters;
	private double dR;
	private double dL;
}
