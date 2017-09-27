package io.github.kensuke1984.kibrary.inversion;

import java.awt.PageAttributes;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;

/**
 * Am=d
 * 
 * @version 0.2.1.2
 * 
 * 
 * @author Kensuke Konishi
 * @see {@link Dvector} {@link UnknownParameter}
 */
public class ObservationEquation {

	private Matrix a;
	
	private Matrix cm;
	
	private List<Double> unknownParameterWeigths;

	/**
	 * @param partialIDs
	 *            for equation must contain waveform
	 * @param parameterList
	 *            for equation
	 * @param dVector
	 *            for equation
	 */
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector,
			boolean time_source, boolean time_receiver, CombinationType combinationType, Map<PartialType
			, Integer[]> nUnknowns, UnknownParameterWeightType unknownParameterWeightType) {
		this.dVector = dVector;
		this.parameterList = parameterList;
		this.originalParameterList = parameterList;
		List<Integer> bouncingOrders = null;
		if (time_receiver) {
			bouncingOrders = Stream.of(dVector.getObsIDs()).map(id -> id.getPhases()).flatMap(Arrays::stream).distinct()
					.map(phase -> phase.nOfBouncingAtSurface()).distinct().collect(Collectors.toList());
			Collections.sort(bouncingOrders);
			System.out.print("Bouncing orders (at Earth's surface): ");
			for (Integer i : bouncingOrders) {
				System.out.print(i + " ");
			}
			System.out.println();
		}
		readA(partialIDs, time_receiver, time_source, bouncingOrders, combinationType, nUnknowns,
				unknownParameterWeightType);
		atd = computeAtD(dVector.getD());
	}
	
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector,
			boolean time_source, boolean time_receiver, Map<PartialType, Integer[]> nUnknowns, double lambdaMU, double lambdaQ, double correlationScaling) {
		this.dVector = dVector;
		this.parameterList = parameterList;
		this.originalParameterList = parameterList;
		List<Integer> bouncingOrders = null;
		if (time_receiver) {
			bouncingOrders = Stream.of(dVector.getObsIDs()).map(id -> id.getPhases()).flatMap(Arrays::stream).distinct()
					.map(phase -> phase.nOfBouncingAtSurface()).distinct().collect(Collectors.toList());
			Collections.sort(bouncingOrders);
			System.out.print("Bouncing orders (at Earth's surface): ");
			for (Integer i : bouncingOrders) {
				System.out.print(i + " ");
			}
			System.out.println();
		}
		readA(partialIDs, time_receiver, time_source, bouncingOrders, null, nUnknowns, null);
		
		double AtANormalizedTrace = 0;
		double count = 0;
		for (int i = 0; i < this.parameterList.size(); i++) {
			AtANormalizedTrace += a.getColumnVector(i).getNorm();
			count++;
		}
		AtANormalizedTrace /= count;
		System.out.println("AtANormalizedTrace = " + AtANormalizedTrace);
		
		// model covariance matrix
		cm = new Matrix(this.parameterList.size(), this.parameterList.size());
		for (int i = 0; i < this.parameterList.size(); i++) {
			UnknownParameter unknown_i = this.parameterList.get(i);
			if (unknown_i.getPartialType().isTimePartial()) {
				cm.setEntry(i, i, 1e4);
				continue;
			}
			for (int j = i; j < this.parameterList.size(); j++) {
				UnknownParameter unknown_j = this.parameterList.get(j);
				if (!unknown_i.getPartialType().equals(unknown_j.getPartialType()))
					continue; // cm.setEntry(i, j, 0.);
				double ri = unknown_i.getLocation().getR();
				double rj = unknown_j.getLocation().getR();
				double vij = .5 * (computeCorrelationLength(ri, correlationScaling)
						+ computeCorrelationLength(rj, correlationScaling));
				if (unknown_i.getPartialType().equals(PartialType.PARQ))
					vij = 1.;//200. * correlationScaling;
				if (unknown_i.getLocation().getR() >= 6346.6)
					vij = 1e-3;
				double rij = ri - rj;
				double cmij = Math.exp(-2 * rij * rij / vij / vij);
				if (unknown_i.getLocation().getR() >= 6346.6) {
					if (unknown_i.getPartialType().equals(PartialType.PAR2) || unknown_i.getPartialType().equals(PartialType.MU))
						cmij *= 1. / (lambdaMU * AtANormalizedTrace * 20);
					else if (unknown_i.getPartialType().equals(PartialType.PARQ))
						cmij *= 1. / (lambdaQ * AtANormalizedTrace * 20);
					else
						throw new RuntimeException("Partial type " + unknown_i.getPartialType() + " not yet possible");
				}
				else {
					if (unknown_i.getPartialType().equals(PartialType.PAR2) || unknown_i.getPartialType().equals(PartialType.MU))
						cmij *= 1. / (lambdaMU * AtANormalizedTrace);
					else if (unknown_i.getPartialType().equals(PartialType.PARQ))
						cmij *= 1. / (lambdaQ * AtANormalizedTrace);
					else
						throw new RuntimeException("Partial type " + unknown_i.getPartialType() + " not yet possible");
				}
				cm.setEntry(i, j, cmij);
				cm.setEntry(j, i, cmij);
			}
		}
		
		Matrix identity = new Matrix(a.getColumnDimension(), a.getColumnDimension());
		for (int i = 0; i < a.getColumnDimension(); i++)
			identity.setEntry(i, i, 1.);
		
//		Matrix tmpata = a.computeAtA();
//		for (int i = 0; i < parameterList.size(); i++) {
//			for (int j = 0; j < parameterList.size(); j++) {
//				System.out.println(i + " " + j + " " + tmpata.getEntry(i, j) + " " + cm.getEntry(i, j));
//			}
//		}
		
		ata = a.computeAtA();
		cmAtA_1 = (cm.multiply(ata)).add(identity);

		atd = computeAtD(dVector.getD());
		cmAtd = cm.operate(a.transpose().operate(dVector.getD()));
		
//		for (int i = 0; i < parameterList.size(); i++) {
//			for (int j = 0; j < parameterList.size(); j++) {
//				System.out.println(i + " " + j + " " + ata.getEntry(i, j) + " " + atd.getEntry(j) + " " + cmAtA_1.getEntry(i, j) + " " + cmAtd.getEntry(j));
//			}
//		}
	}
	
	// scaling = 1. gives a correlation lenght of 50 km in the uppermost mantle
	private static double computeCorrelationLength(double r, double scaling) {
		return scaling * 250;
//		if (r < 3550.0 && r >= 3480.0)
//			return scaling * 186.68;
//		else if (r < 3690.0 && r >= 3550.0)
//			return scaling * (4.565e-01 * (r-3690.0) + 250.59);
//		if (r < 3690.0 && r >= 3480.0)
//			return scaling * (0.24089 * (r - 3690.0) + 250.588);
//			return scaling * 250.;
//		else if (r < 4410.0 && r >= 3690.0)
//			return scaling * (-2.064e-03 * (r-4410.0) + 249.10);
//		else if (r < 5330.0 && r >= 4410.0)
//			return scaling * (-7.354e-02 * (r-5330.0) + 181.45);
//		else if (r < 5670.0 && r >= 5330.0)
//			return scaling * (-1.555e-01 * (r-5670.0) + 128.57);
//		else if (r < 6310.0 && r >= 5670.0)
//			return scaling * (-1.051e-01 * (r-6310.0) + 61.30);
//			return scaling * (-0.1384 * (r-6310.0) + 40.);
//		else
//			return scaling * (-1.853e-01 * (r-6371.0) + 50.00);
//			return scaling * (-0.1639 * (r-6371.0) + 30.);
	}
	
	private List<UnknownParameter> originalParameterList;
	private List<UnknownParameter> parameterList;
	private Dvector dVector;

	public int getDlength() {
		return dVector.getNpts();
	}

	/**
	 * @param m
	 *            解のベクトル
	 * @return Amをsyntheticに足したd列 順番はDvectorと同じ
	 */
	public RealVector[] bornOut(RealVector m) {
		RealVector[] am = dVector.separate(operate(m));
		RealVector[] syn = dVector.getSynVec();
		RealVector[] born = new ArrayRealVector[dVector.getNTimeWindow()];
		Arrays.setAll(born, i -> syn[i].add(am[i]));
		return born;
	}

	/**
	 * Am=d 求めたいのは (d-Am)<sup>T</sup>(d-Am)/obs<sup>2</sup>
	 * 
	 * (d<sup>T</sup>-m<sup>T</sup>A<sup>T</sup>)(d-Am)= d<sup>T</sup>d-d<sup>T
	 * </sup>Am-m<sup>T</sup>A<sup>T</sup>d+m<sup>T</sup>A<sup>T</sup>Am =d<sup>T
	 * </sup>d-2*(A<sup>T</sup>d)m<sup>T</sup>+m<sup>T</sup>(A<sup>T</sup>A)m
	 * 
	 * 
	 * @param m
	 *            解のベクトル
	 * @return あたえたｍに対してのvarianceを計算する
	 */
	public double varianceOf(RealVector m) {
		Objects.requireNonNull(m);
		double obs2 = dVector.getObsNorm() * dVector.getObsNorm();
		double variance = dVector.getDNorm() * dVector.getDNorm() - 2 * atd.dotProduct(m)
				+ m.dotProduct(getAtA().operate(m));
		return variance / obs2;
	}

	private RealVector atd;

	private RealVector cmAtd;
	
	/**
	 * Am=dのAを作る まずmとdの情報から Aに必要な偏微分波形を決める。
	 * 
	 * @param ids
	 *            source for A
	 */
	private void readA(PartialID[] ids, boolean time_receiver, boolean time_source, List<Integer> bouncingOrders
			, CombinationType combinationType, Map<PartialType, Integer[]> nUnknowns,
			UnknownParameterWeightType unknownParameterWeightType) {
		if (time_source)
			dVector.getUsedGlobalCMTIDset().forEach(id -> parameterList.add(new TimeSourceSideParameter(id)));
		if (time_receiver) {
			if (bouncingOrders == null)
				throw new RuntimeException("Exepct List<Integer> bouncingOrders to be defined");
			for (Integer i : bouncingOrders)
				dVector.getUsedStationSet().forEach(station -> parameterList.add(new TimeReceiverSideParameter(station, i.intValue())));
		}
		
		a = new Matrix(dVector.getNpts(), parameterList.size());
		a.scalarMultiply(0);
		
		// partialDataFile.readWaveform();
		long t = System.nanoTime();
		AtomicInteger count = new AtomicInteger();
		AtomicInteger count_TIMEPARTIAL_SOURCE = new AtomicInteger();
		AtomicInteger count_TIMEPARTIAL_RECEIVER = new AtomicInteger();
		
		int n = 0;
		if (time_receiver)
			n++;
		if (time_source)
			n++;
		int numberOfParameterForSturcture = (int) parameterList.stream().filter(unknown -> !unknown.getPartialType().isTimePartial()).count();
		final int nn = numberOfParameterForSturcture + n;
		
		Arrays.stream(ids).parallel().forEach(id -> {
			if (count.get() + count_TIMEPARTIAL_RECEIVER.get() + count_TIMEPARTIAL_SOURCE.get() == dVector.getNTimeWindow() * nn)
				return;
			int column = whatNumer(id.getPartialType(), id.getPerturbationLocation(),
					id.getStation(), id.getGlobalCMTID(), id.getPhases());
			if (column < 0)
				return;
			// 偏微分係数id[i]が何番目のタイムウインドウにあるか
			int k = dVector.whichTimewindow(id);
			if (k < 0) {
//				System.err.format("Timewindow not found: %s%n", id.toString());
				return;
			}
			int row = dVector.getStartPoints(k);
			double weighting = dVector.getWeighting(k) * parameterList.get(column).getWeighting();
			double[] partial = id.getData();
			for (int j = 0; j < partial.length; j++)
				a.setEntry(row + j, column, partial[j] * weighting);
			if (!id.getPartialType().isTimePartial())
				count.incrementAndGet();
			else if (id.getPartialType().equals(PartialType.TIME_SOURCE))
				count_TIMEPARTIAL_SOURCE.incrementAndGet();
			else if (id.getPartialType().equals(PartialType.TIME_RECEIVER))
				count_TIMEPARTIAL_RECEIVER.incrementAndGet();
		});
		if ( count.get() + count_TIMEPARTIAL_RECEIVER.get() + count_TIMEPARTIAL_SOURCE.get() != dVector.getNTimeWindow() * nn )
			throw new RuntimeException("Input partials are not enough: " + " " + count.get() + " + " +
					count_TIMEPARTIAL_RECEIVER.get() + " + " + count_TIMEPARTIAL_SOURCE.get() + " != " +
					dVector.getNTimeWindow() + " * (" + numberOfParameterForSturcture + " + " + n + ")");  
		System.err.println("A is read and built in " + Utilities.toTimeString(System.nanoTime() - t));
		
		if (combinationType != null) {
			if (combinationType.equals(CombinationType.CORRIDOR_BOXCAR)) {
				System.out.println("Combining 1-D pixels into boxcars");
				int totalUnknown = 0; 
				for (Integer[] ns : nUnknowns.values())
					totalUnknown += ns[0] + ns[1];
				Matrix aPrime = new Matrix(dVector.getNpts(), a.getColumnDimension() - numberOfParameterForSturcture + totalUnknown);
				List<UnknownParameter> parameterPrime = new ArrayList<>();
				
				int jCurrent = 0;
				for (Map.Entry<PartialType, Integer[]> entry : nUnknowns.entrySet()) {
					int thisNunknowns = entry.getValue()[0] + entry.getValue()[1]; 
					
					double[] layers = new double[thisNunknowns - 1];
	//				int nnUnknowns = nUnknowns / 2;
					double UpperWidth = Earth.EARTH_RADIUS - 24.4 - 5721.;
					double LowerWidth = 5721. - 3480.;
					double deltaUpper = UpperWidth / entry.getValue()[0];
					double deltaLower = LowerWidth / entry.getValue()[1];
					if (thisNunknowns == 2)
						layers[0] = 5721.;
					else {
						for (int i = 0; i < entry.getValue()[0] - 1; i++) {
							layers[i] = Earth.EARTH_RADIUS - 24.4 - (i+1) * deltaUpper;
						}
						layers[entry.getValue()[0] - 1] = 5721.;
						for (int i = 0; i < entry.getValue()[1] - 1; i++) {
							layers[i + entry.getValue()[0]] = 5721. - (i + 1) * deltaLower;
						}
					}
					
					for (int i = 0; i < numberOfParameterForSturcture; i++) {
						if (!parameterList.get(i).getPartialType().equals(entry.getKey()))
							continue;
						int j = -1;
						for (int k = 0; k < layers.length; k++) {
							double r = parameterList.get(i).getLocation().getR();
							if (r > layers[k]) {
								System.out.println(parameterList.get(i).getPartialType() + " " + parameterList.get(i).getLocation().getR() + " " + layers[k] + " " + k);
								j = k;
								break;
							}
						}
						if (j == -1)
							j = thisNunknowns - 1;
						j += jCurrent;
						aPrime.setColumnVector(j, aPrime.getColumnVector(j).add(a.getColumnVector(i)));
					}
					
					for (int i = 0; i < layers.length; i++) {
						double r = 0;
						double w = 0;
						if (i < entry.getValue()[0]) {
							r = layers[i] + deltaUpper / 2.;
							w = deltaUpper;
						}
						else {
							r = layers[i] + deltaLower / 2.;
							w = deltaLower;
						}
						parameterPrime.add(new Physical1DParameter(entry.getKey(), r, w));
					}
					double r = 3480. + deltaLower / 2.;
					double w = deltaLower;
					parameterPrime.add(new Physical1DParameter(entry.getKey(), r, w));
					for (int i = numberOfParameterForSturcture; i < parameterList.size(); i++)
						parameterPrime.add(parameterList.get(i));
					
					jCurrent += thisNunknowns;
				}
				
				parameterList = parameterPrime;
				
				// set time partials if needed
				for (int i = 0; i < a.getColumnDimension() - numberOfParameterForSturcture; i++)
					aPrime.setColumnVector(i + totalUnknown, a.getColumnVector(i + numberOfParameterForSturcture));
				a = aPrime;
			}
			
			if (combinationType.equals(CombinationType.LOWERMANTLE_BOXCAR_3D)) {
				System.out.println("Combining voxels into boxcar");
				int totalNR = 0;
				for (Integer[] ns : nUnknowns.values()) {
					if (ns.length > 1)
						throw new RuntimeException("Error: the combination type " 
							+ combinationType + " let you specify only one integer per PartialType");
					totalNR += ns[0];
				}
				
				List<HorizontalPosition> horizontalPositions = parameterList.stream()
						.map(p -> p.getLocation().toHorizontalPosition())
						.distinct()
						.collect(Collectors.toList());
				
				Matrix aPrime = new Matrix(dVector.getNpts(), a.getColumnDimension() 
						- numberOfParameterForSturcture + totalNR * horizontalPositions.size());
				List<UnknownParameter> parameterPrime = new ArrayList<>();
				
				// fill new A matrix
				int iTypeShift = 0;
				for (Map.Entry<PartialType, Integer[]> entry : nUnknowns.entrySet()) {
					int nNewPerturbationR = entry.getValue()[0];
					
					//collect radii and horizontal positions of original perturbations
					List<Double> radii = parameterList.stream().filter(p -> p.getPartialType()
							.equals(entry.getKey()))
						.map(p -> p.getLocation().getR())
						.distinct()
						.collect(Collectors.toList());
					
					Collections.sort(radii);
					double maxR = radii.get(radii.size() - 1);
					double minR = radii.get(0);
					double originalDeltaR = radii.get(1) - radii.get(0);
					maxR += originalDeltaR / 2.;
					minR -= originalDeltaR / 2.;
					int nOriginal = radii.size();
					if ((double) nOriginal / nNewPerturbationR != nOriginal / nNewPerturbationR)
						throw new RuntimeException("Please specify a number of new Unkowns "
								+ "that divides the number of original unknowns");
					double deltaR = originalDeltaR * nOriginal / nNewPerturbationR;
					
					//debug
					System.out.println("DEBUG1: " + originalDeltaR + " " + maxR + " " + minR + " " + deltaR + " " + nNewPerturbationR);
					//
					
					double[] newPerturbationR = new double[nNewPerturbationR];
					for (int i = 0; i < nNewPerturbationR; i++)
						newPerturbationR[i] = minR + i * deltaR + deltaR / 2.;
					
					List<Location> newLocations = new ArrayList<>();
					for (double r : newPerturbationR) {
						for (HorizontalPosition hp : horizontalPositions) {
							Location loc = hp.toLocation(r);
							newLocations.add(loc);
						}
					}
					
					Map<Location, List<Integer>> combinationIndexMap = new HashMap<>();
					Map<Location, Double> combinedWeightingMap = new HashMap<>();
					
					for (int i = 0; i < originalParameterList.size(); i++) {
						if (!parameterList.get(i).getPartialType().equals(entry.getKey()))
							continue;
						Location loc = parameterList.get(i).getLocation();
						int iR = (int) ((loc.getR() - minR) / deltaR);
						Location newLoc = loc.toLocation(newPerturbationR[iR]);
						//
						if (!combinationIndexMap.containsKey(newLoc)) {
							List<Integer> indices = new ArrayList<>();
							indices.add(i);
							combinationIndexMap.put(newLoc, indices);
							Double w = parameterList.get(i).getWeighting();
							combinedWeightingMap.put(newLoc, w);
						}
						else {
							List<Integer> indices = combinationIndexMap.get(newLoc);
							indices.add(i);
							combinationIndexMap.replace(newLoc, indices);
							Double w = combinedWeightingMap.get(newLoc);
							w += parameterList.get(i).getWeighting();
							combinedWeightingMap.replace(newLoc, w);
						}
					}
					
					//debug
					for (Location newLoc : newLocations) {
						List<Integer> indices = combinationIndexMap.get(newLoc);
						double w = combinedWeightingMap.get(newLoc);
						String s = "";
						for (int i : indices) {
							s += parameterList.get(i).getLocation().toString() 
									+ " " + parameterList.get(i).getWeighting() + ", ";
						}
						s += ": " + newLoc + " " + w;
						System.out.println("DEBUG2: " + s);
					}
					//
					
					//fill the new A matrix using the index map using the order of newLocations
					for (int i = 0; i < newLocations.size(); i++) {
						Location newLoc = newLocations.get(i);
						List<Integer> indices = combinationIndexMap.get(newLoc);
						RealVector vector = new ArrayRealVector(a.getRowDimension());
						for (int index : indices)
							vector = vector.add(a.getColumnVector(index));
						aPrime.setColumnVector(i + iTypeShift, vector);
						//
						//fill new Unknown parameter list for current PartialType
						double weighting = combinedWeightingMap.get(newLoc);
						parameterPrime.add(new Physical3DParameter(entry.getKey(), newLoc, weighting));
					}
					
					iTypeShift += newLocations.size();
				}
				
				//TODO set time partials
				
				parameterList = parameterPrime;
				a = aPrime;
				
				//debug
				System.out.println("Debug 3: norm of A matrix = " + a.getNorm());
				//
			}
			
			// Triangle splines
			if (combinationType.equals(CombinationType.CORRIDOR_TRIANGLE)) {
				System.out.println("Combining 1-D pixels into triangles");
				TriangleRadialSpline trs = new TriangleRadialSpline(nUnknowns, parameterList);
				a = trs.computeNewA(a);
				parameterList = trs.getNewParameters();
			}
		}
		
		// Normalize time partials
		double meanAColumnNorm = 0;
		int ntmp = 0;
		for (int j = 0; j < a.getColumnDimension(); j++) {
//			if (!parameterList.get(j).getPartialType().equals(PartialType.PAR2))
			if (parameterList.get(j).getPartialType().isTimePartial())
				continue;
			meanAColumnNorm += a.getColumnVector(j).getNorm();
			ntmp++;
		}
		meanAColumnNorm /= ntmp; 
		for (int j = 0; j < a.getColumnDimension(); j++) {
			if (!parameterList.get(j).getPartialType().isTimePartial())
				continue;
			double tmpNorm = a.getColumnVector(j).getNorm();
			if (tmpNorm > 0)
				a.setColumnVector(j, a.getColumnVector(j).mapMultiply(meanAColumnNorm / tmpNorm));
			System.out.println(j + " " + meanAColumnNorm / tmpNorm);
		}
		
		//normalize PARQ
		double empiricalFactor = .1;
		meanAColumnNorm = 0;
		double meanAQNorm = 0;
		ntmp = 0;
		int ntmpQ = 0;
		for (int j = 0; j < a.getColumnDimension(); j++) {
			if (parameterList.get(j).getPartialType().isTimePartial())
				continue;
			if (parameterList.get(j).getPartialType().equals(PartialType.PARQ)) {
				meanAQNorm += a.getColumnVector(j).getNorm();
				ntmpQ++;
			}
			else if (parameterList.get(j).getPartialType().equals(PartialType.PAR2)){
				meanAColumnNorm += a.getColumnVector(j).getNorm();
				ntmp++;
			}
		}
		meanAColumnNorm /= ntmp;
		meanAQNorm /= ntmpQ;
		if (ntmpQ > 0) {
			for (int j = 0; j < a.getColumnDimension(); j++) {
				if (!parameterList.get(j).getPartialType().equals(PartialType.PARQ))
					continue;
				if (ntmp == 0 || ntmpQ == 0)
					continue;
				a.setColumnVector(j, a.getColumnVector(j).mapMultiply(empiricalFactor * meanAColumnNorm / meanAQNorm));
			}
			System.out.println("PAR2 / PARQ = " + empiricalFactor * meanAColumnNorm / meanAQNorm);
		}
		
		//for Sci. Adv. revisions
//		double sensitivityDpp = 0;
//		double sensitivityUM = 0;
//		int nDpp = 0;
//		int nUM = 0;
//		for (int j = 0; j < a.getColumnDimension(); j++) {
//			if (parameterList.get(j).getLocation().getR() > 5721) {
//				sensitivityUM += a.getColumnVector(j).getNorm();
//				nUM++;
//			}
//			else {
//				sensitivityDpp += a.getColumnVector(j).getNorm();
//				nDpp++;
//			}
//		}
//		sensitivityUM /= nUM;
//		sensitivityDpp /= nDpp;
//		System.out.println("(for Sci. Adv) upper mantle partials amplification factor = " + (sensitivityDpp / sensitivityUM));
//		for (int j = 0; j < a.getColumnDimension(); j++) {
//			if (parameterList.get(j).getLocation().getR() > 5721)
//				a.setColumnVector(j, a.getColumnVector(j).mapMultiply(sensitivityDpp / sensitivityUM));
//		}
		
//		for (int i = numberOfParameterForSturcture; i < parameterList.size(); i++) {
//			int j = 0;
//			for(double ai : a.getColumn(i)) {
//				if (ai != 0)
//					System.out.printf("%.2e ", ai);
//				else if (j % 50 == 0)
//					System.out.printf(".");
//				j++;
//			}
//			System.out.println("\n");
//		}
		
		if (unknownParameterWeightType != null) {
			unknownParameterWeigths = new ArrayList<>();
			WeightUnknownParameter wup = new WeightUnknownParameter(unknownParameterWeightType, parameterList);
			Map<UnknownParameter, Double> weights = wup.getWeights();
			for (int i = 0; i < parameterList.size(); i++) {
				double weight = weights.get(parameterList.get(i));
				a.setColumnVector(i, a.getColumnVector(i).mapMultiply(weight));
				unknownParameterWeigths.add(weight);
			}
		}
	}

	/**
	 * @param param
	 *            to look for
	 * @return parameterが何番目にあるか なければ-1
	 */
	private int whatNumer(PartialType type, Location location, Station station, GlobalCMTID id, Phase[] phases) {
		for (int i = 0; i < parameterList.size(); i++) {
			if (parameterList.get(i).getPartialType() != type)
				continue;
			switch (type) {
			case TIME_SOURCE:
				if (id.equals( ((TimeSourceSideParameter) parameterList.get(i)).getGlobalCMTID() ))
					return i;
				break;
			case TIME_RECEIVER:
				List<Integer> bouncingOrders = Arrays.stream(phases).map(phase -> phase.nOfBouncingAtSurface()).distinct().collect(Collectors.toList());
				Collections.sort(bouncingOrders);
				int lowestBouncingOrder = bouncingOrders.get(0);
				if (station.equals( ((TimeReceiverSideParameter) parameterList.get(i)).getStation() ) &&
						((TimeReceiverSideParameter) parameterList.get(i)).getBouncingOrder() == lowestBouncingOrder)
					return i;
				break;
			case PARA:
			case PARC:
			case PARF:
			case PARL:
			case PARN:
			case PARQ:
				if (location.getR() == ((Physical1DParameter) parameterList.get(i)).getPerturbationR())
					return i;
				break;
			case PAR1:
			case PAR2:
				if (location.getR() == ((Physical1DParameter) parameterList.get(i)).getPerturbationR())
					return i;
				break;
			case A:
			case C:
			case F:
			case L:
			case N:
			case Q:
			case MU:
			case LAMBDA:
				if (location.equals(((Physical3DParameter) parameterList.get(i)).getPointLocation()))
					return i;
				break;
			}
		}
		return -1;
	}

	public RealMatrix getA() {
		return a;
	}

	private RealMatrix ata;
	
	private RealMatrix cmAtA_1;
	
	public RealMatrix getAtA() {
		if (ata == null)
			synchronized (this) {
				if (ata == null) {
					ata = a.computeAtA();
				}
			}
		return ata;
	}
	
	public RealMatrix getCmAtA_1() {
		if (cm == null)
			throw new RuntimeException("The model covariance matrix Cm is not set");
		if (ata == null) {
			synchronized (this) {
				if (ata == null) {
					ata = a.computeAtA();
				}
				if (cmAtA_1 == null) {
					Matrix identity = new Matrix(a.getColumnDimension(), a.getColumnDimension());
					for (int i = 0; i < a.getColumnDimension(); i++)
						identity.setEntry(i, i, 1.);
					cmAtA_1 = (cm.multiply(ata)).add(identity);
				}
			}
		}
		return cmAtA_1;
	}

	/**
	 * Aを書く それぞれのpartialごとに分けて出す debug用？
	 * 
	 * @param outputPath
	 *            {@link Path} for an output folder
	 */
	void outputA(Path outputPath) throws IOException {
		if (a == null) {
			System.out.println("no more A");
			return;
		}
		if (Files.exists(outputPath))
			throw new FileAlreadyExistsException(outputPath.toString());
		Files.createDirectories(outputPath);
		BasicID[] ids = dVector.getSynIDs();
		IntStream.range(0, ids.length).forEach(i -> {
			BasicID id = ids[i];
			Path eventPath = outputPath.resolve(id.getGlobalCMTID().toString());
			try {
				Files.createDirectories(eventPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
			int start = dVector.getStartPoints(i);
			double synStartTime = id.getStartTime();
			Path outPath = eventPath.resolve(
					id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent() + "." + i + ".txt");
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
				pw.println("#syntime par0 par1, .. parN");
				for (int k = 0; k < id.getNpts(); k++) {
					double synTime = synStartTime + k / id.getSamplingHz();
					pw.print(synTime + " ");
					for (int j = 0, mlen = parameterList.size(); j < mlen; j++)
						pw.print(a.getEntry(start + k, j) + " ");
					pw.println();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		});
	}
	
	public void outputSensitivity(Path outPath) throws IOException {
		if (a == null) {
			System.out.println("no more A");
			return;
		}
		if (ata == null) {
			System.err.println(" No more ata. Computing again");
				if (cm == null)
					ata = a.computeAtA();
				else {
					Matrix identity = new Matrix(a.getColumnDimension(), a.getColumnDimension());
					for (int i = 0; i < a.getColumnDimension(); i++)
						identity.setEntry(i, i, 1.);
					ata = (cm.multiply(a.computeAtA())).add(identity);
				}
		}
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			Map<UnknownParameter, Double> sMap = Sensitivity.sensitivityMap(ata, parameterList);
			List<UnknownParameter> unknownForStructure = parameterList.stream().filter(unknown -> !unknown.getPartialType().isTimePartial())
					.collect(Collectors.toList());
			for (UnknownParameter unknown : unknownForStructure)
				pw.println(unknown.getPartialType() + " " + unknown.getLocation() + " " + sMap.get(unknown));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void outputUnkownParameterWeigths(Path outpath) throws IOException {
		if (unknownParameterWeigths != null) {
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
				for (int i = 0; i < unknownParameterWeigths.size(); i++)
					pw.println(unknownParameterWeigths.get(i));
			}
		}
	}

	/**
	 * 与えたベクトルdに対して A<sup>T</sup>dを計算する
	 * 
	 * A<sup>T</sup>d = v <br>
	 * 
	 * v<sup>T</sup> = (A<sup>T</sup>d)<sup>T</sup>= d<sup>T</sup>A
	 * 
	 * @param d
	 *            of A<sup>T</sup>d
	 * @return A<sup>T</sup>d
	 */
	public RealVector computeAtD(RealVector d) {
			return a.preMultiply(d);
	}

	public List<UnknownParameter> getOriginalParameterList() {
		return originalParameterList;
	}
	
	public List<UnknownParameter> getParameterList() {
		return parameterList;
	}

	public RealVector getAtD() {
		return atd;
	}
	
	public RealVector getCmAtD() {
		if (cm == null)
			throw new RuntimeException("The model covariance matrix Cm is not set");
		return cmAtd;
	}

	public Dvector getDVector() {
		return dVector;
	}

	public int getMlength() {
		return parameterList.size();
	}

	/**
	 * Computes Am
	 * 
	 * @param m
	 *            for Am
	 * @return Am
	 */
	public RealVector operate(RealVector m) {
		return a.operate(m);
	}

}
