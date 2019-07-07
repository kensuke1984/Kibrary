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

import javax.management.RuntimeErrorException;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.correlation.Covariance;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.math.MatrixComputation;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.AtAEntry;
import io.github.kensuke1984.kibrary.waveformdata.AtdEntry;
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
	
	private ModelCovarianceMatrix cm;
	
	private List<Double> unknownParameterWeigths;
	
	public ObservationEquation(RealMatrix ataMatrix, List<UnknownParameter> unknownParameterList, RealVector atdVector) {
		this.ata = ataMatrix;
		this.atd = atdVector;
		this.originalParameterList = unknownParameterList;
		this.parameterList = unknownParameterList;
		this.cm = null;
	}
	
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
			, Integer[]> nUnknowns, UnknownParameterWeightType unknownParameterWeightType, Path verticalMappingPath) {
		if (verticalMappingPath != null) {
			this.mapping = new ParameterMapping(parameterList.toArray(new UnknownParameter[0]), verticalMappingPath);
			combinationType = CombinationType.VERTICAL_MAPPING;
			System.out.println("Using vertical mapping " + verticalMappingPath);
		}
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
		System.out.println("Using combination type " + combinationType);
		readA(partialIDs, time_receiver, time_source, bouncingOrders, combinationType, nUnknowns,
				unknownParameterWeightType);
		atd = computeAtD(dVector.getD());
		ata = a.computeAtA();
		System.out.println("AtA mean trace = " + (ata.getTrace() / ata.getColumnDimension()));
		System.out.println("Atd mean norm = " + (atd.getLInfNorm() / ata.getColumnDimension()));
	}
	
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector,
			boolean time_source, boolean time_receiver, CombinationType combinationType, Map<PartialType
			, Integer[]> nUnknowns, UnknownParameterWeightType unknownParameterWeightType, Path verticalMappingPath, boolean computeAtA) {
		if (verticalMappingPath != null) {
			this.mapping = new ParameterMapping(parameterList.toArray(new UnknownParameter[0]), verticalMappingPath);
			combinationType = CombinationType.VERTICAL_MAPPING;
			System.out.println("Using vertical mapping " + verticalMappingPath);
		}
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
		System.out.println("Using combination type " + combinationType);
		readA(partialIDs, time_receiver, time_source, bouncingOrders, combinationType, nUnknowns,
				unknownParameterWeightType);
		if (computeAtA) {
			atd = computeAtD(dVector.getD());
			ata = a.computeAtA();
			System.out.println("AtA mean trace = " + (ata.getTrace() / ata.getColumnDimension()));
			System.out.println("Atd mean norm = " + (atd.getNorm() / ata.getColumnDimension()));
		}
	}
	
	private RealVector m;
	
	public void applyConditioner(RealVector m) {
		this.m = m;
		int n = getMlength();
		
		if (ata != null) {
			for (int i = 0; i < n; i++)
				atd.setEntry(i, atd.getEntry(i) * m.getEntry(i));
			
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					ata.setEntry(i, j, ata.getEntry(i, j) * m.getEntry(i) * m.getEntry(j));
	//				setRowVector(i, ata.getRowVector(i).mapMultiply(m.getEntry(i)));
				}
			}
		}
		else {
			for (int i = 0; i < n; i++) {
				a.setColumnVector(i, a.getColumnVector(i).mapMultiply(m.getEntry(i)));
			}
		}
	}
	
	public void addRegularization(RealMatrix D) {
		if (ata != null)
			ata = ata.add(D);
		else
			throw new RuntimeException("AtA is null");
	}
	
	public RealVector getM() {
		return m;
	}
	
	public void applyModelCovarianceMatrix(ModelCovarianceMatrix cm) {
		this.cm = cm;
		Matrix identity = new Matrix(ata.getRowDimension(), ata.getColumnDimension());
		for (int i = 0; i < ata.getColumnDimension(); i++)
			identity.setEntry(i, i, 1.);
		
		ata = cm.rightMultiplyByL(ata);
		ata = cm.leftMultiplyByLT(ata).add(identity);
		atd = cm.getL().preMultiply(atd);
	}
	
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector,
			boolean time_source, boolean time_receiver, Map<PartialType, Integer[]> nUnknowns, double lambdaMU, double lambdaQ, double correlationScaling
			, Path verticalMappingPath) {
		CombinationType combinationType = null;
		if (verticalMappingPath != null) {
			this.mapping = new ParameterMapping(parameterList.toArray(new UnknownParameter[0]), verticalMappingPath);
			System.out.println("Using vertical mapping " + verticalMappingPath);
			combinationType = CombinationType.VERTICAL_MAPPING;
		}
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
		readA(partialIDs, time_receiver, time_source, bouncingOrders, combinationType, nUnknowns, null);
		
		double AtANormalizedTrace = 0;
		double count = 0;
		for (int i = 0; i < this.parameterList.size(); i++) {
			AtANormalizedTrace += a.getColumnVector(i).getNorm();
			count++;
		}
		AtANormalizedTrace /= count;
		System.out.println("AtANormalizedTrace = " + AtANormalizedTrace);
		
		double normalization = AtANormalizedTrace * 0.1;
		
		// model covariance matrix
		cm = new ModelCovarianceMatrix(parameterList, 0., correlationScaling, normalization, true);
		
//		this.cm = new Matrix(this.parameterList.size(), this.parameterList.size());
//		RealMatrix matrix = cm.getCm();
//		for (int i = 0; i < this.parameterList.size(); i++) {
//			for (int j = 0; j < this.parameterList.size(); j++) {
//				this.cm.setEntry(i, j, matrix.getEntry(i, j));
//			}
//		}
		
//		cm = new Matrix(this.parameterList.size(), this.parameterList.size());
//		for (int i = 0; i < this.parameterList.size(); i++) {
//			UnknownParameter unknown_i = this.parameterList.get(i);
//			if (unknown_i.getPartialType().isTimePartial()) {
//				cm.setEntry(i, i, 1e4);
//				continue;
//			}
//			for (int j = i; j < this.parameterList.size(); j++) {
//				UnknownParameter unknown_j = this.parameterList.get(j);
//				if (!unknown_i.getPartialType().equals(unknown_j.getPartialType()))
//					continue; // cm.setEntry(i, j, 0.);
//				double ri = unknown_i.getLocation().getR();
//				double rj = unknown_j.getLocation().getR();
//				double vij = .5 * (computeCorrelationLength(ri, correlationScaling)
//						+ computeCorrelationLength(rj, correlationScaling));
//				if (unknown_i.getPartialType().equals(PartialType.PARQ))
//					vij = 1.;//200. * correlationScaling;
//				if (unknown_i.getLocation().getR() >= 6346.6)
//					vij = 1e-3;
//				double rij = ri - rj;
//				double cmij = Math.exp(-2 * rij * rij / vij / vij);
//				if (unknown_i.getLocation().getR() >= 6346.6) {
//					if (unknown_i.getPartialType().equals(PartialType.PAR2) || unknown_i.getPartialType().equals(PartialType.MU))
//						cmij *= 1. / (lambdaMU * AtANormalizedTrace * 20);
//					else if (unknown_i.getPartialType().equals(PartialType.PARQ))
//						cmij *= 1. / (lambdaQ * AtANormalizedTrace * 20);
//					else
//						throw new RuntimeException("Partial type " + unknown_i.getPartialType() + " not yet possible");
//				}
//				else {
//					if (unknown_i.getPartialType().equals(PartialType.PAR2) || unknown_i.getPartialType().equals(PartialType.MU))
//						cmij *= 1. / (lambdaMU * AtANormalizedTrace);
//					else if (unknown_i.getPartialType().equals(PartialType.PARQ))
//						cmij *= 1. / (lambdaQ * AtANormalizedTrace);
//					else
//						throw new RuntimeException("Partial type " + unknown_i.getPartialType() + " not yet possible");
//				}
//				cm.setEntry(i, j, cmij);
//				cm.setEntry(j, i, cmij);
//			}
//		}
		
		Matrix identity = new Matrix(a.getColumnDimension(), a.getColumnDimension());
		for (int i = 0; i < a.getColumnDimension(); i++)
			identity.setEntry(i, i, 1.);
		
//		Matrix tmpata = a.computeAtA();
//		for (int i = 0; i < parameterList.size(); i++) {
//			for (int j = 0; j < parameterList.size(); j++) {
//				System.out.println(i + " " + j + " " + tmpata.getEntry(i, j) + " " + cm.getEntry(i, j));
//			}
//		}
		
		RealMatrix tmpB = cm.rightMultiplyByL(a);
		Matrix b = new Matrix(a.getRowDimension(), a.getColumnDimension());
		for (int i = 0; i < a.getRowDimension(); i++) {
			for (int j = 0; j < a.getColumnDimension(); j++) {
				b.setEntry(i, j, tmpB.getEntry(i, j));
			}
		}
		a = b;
		ata = b.computeAtA();
		cmAtA_1 = ata.add(identity);
		
//		ata = a.computeAtA();
		
//		cmAtA_1 = (cm.multiply(ata)).add(identity);
		
//		System.out.println("Computing cmAtA");
//		cmAtA_1 = cm.leftMultiply(ata);

		//corrected version using cholesky decompostion of the covariance matrix
		
		atd = computeAtD(dVector.getD());
		cmAtd = atd;
		
//		cmAtd = cm.operate(a.transpose().operate(dVector.getD()));
		
//		for (int i = 0; i < parameterList.size(); i++) {
//			for (int j = 0; j < parameterList.size(); j++) {
//				System.out.println(i + " " + j + " " + ata.getEntry(i, j) + " " + atd.getEntry(j) + " " + cmAtA_1.getEntry(i, j) + " " + cmAtd.getEntry(j));
//			}
//		}
	}
	
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector
			, double cm0, double cmH, double cmV, Path verticalMappingPath, boolean computeAtA) {
		CombinationType combinationType = null;
		if (verticalMappingPath != null) {
			this.mapping = new ParameterMapping(parameterList.toArray(new UnknownParameter[0]), verticalMappingPath);
			System.out.println("Using vertical mapping " + verticalMappingPath);
			combinationType = CombinationType.VERTICAL_MAPPING;
		}
		this.dVector = dVector;
		this.parameterList = parameterList;
		this.originalParameterList = parameterList;
	
		readA(partialIDs, false, false, null, combinationType, null, null);
		
		double AtANormalizedTrace = 0;
		for (int i = 0; i < this.parameterList.size(); i++) {
			double norm = a.getColumnVector(i).dotProduct(a.getColumnVector(i));
			AtANormalizedTrace += norm;
		}
		AtANormalizedTrace /= getMlength();
		System.out.println("AtANormalizedTrace = " + AtANormalizedTrace);
		
		double normalization = cm0 / AtANormalizedTrace;
		
		// model covariance matrix
		cm = new ModelCovarianceMatrix(parameterList, cmV, cmH, normalization, true);
		
		Matrix identity = new Matrix(a.getColumnDimension(), a.getColumnDimension());
		for (int i = 0; i < a.getColumnDimension(); i++)
			identity.setEntry(i, i, 1.);
		
		RealMatrix tmpB = cm.rightMultiplyByL(a);
		Matrix b = new Matrix(tmpB.getRowDimension(), tmpB.getColumnDimension());
		for (int i = 0; i < a.getRowDimension(); i++) {
			for (int j = 0; j < a.getColumnDimension(); j++) {
				b.setEntry(i, j, tmpB.getEntry(i, j));
			}
		}
		
		a = b;
		atd = b.preMultiply(dVector.getD());
		
		if (computeAtA)
			ata = a.computeAtA().add(identity);
	}
	
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector
			, double cm0, double cmH, double cmV, Path verticalMappingPath) {
		this(partialIDs, parameterList, dVector, cm0, cmH, cmV, verticalMappingPath, true);
	}
	
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector, Matrix ata_prev, RealVector atd_prev) {
		this.dVector = dVector;
		this.parameterList = parameterList;
		this.originalParameterList = parameterList;
	
		readA(partialIDs, false, false, null, null, null, null);
		
		ata = a.computeAtA().add(ata_prev);
		atd = computeAtD(dVector.getD()).add(atd_prev);
	}
	
	public ObservationEquation(RealMatrix ata, RealVector atd, List<UnknownParameter> parameterList, Dvector dVector) {
		this.dVector = dVector;
		this.parameterList = parameterList;
		this.originalParameterList = parameterList;
	
		this.ata = ata;
		this.atd = atd;
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
		double variance = 0;
		if (ata != null) {
			if (cm != null) {
//				System.out.println("variance withouth model covariance term");
				variance = dVector.getDNorm() * dVector.getDNorm() - 2 * atd.dotProduct(m)
						+ m.dotProduct(getAtA().operate(m)) - m.dotProduct(m);
			}
			else
				variance = dVector.getDNorm() * dVector.getDNorm() - 2 * atd.dotProduct(m)
				+ m.dotProduct(getAtA().operate(m));
		}
		else
			variance = dVector.getDNorm() * dVector.getDNorm() - 2 * atd.dotProduct(m)
				+ m.dotProduct(getA().preMultiply(getA().operate(m))) + m.dotProduct(m);
		return variance / obs2;
	}
	
	public double varianceOf(RealVector m, double residualVariance, double obsNorm) {
		Objects.requireNonNull(m);
		double variance = 0;
		if (ata != null) {
			if (cm != null) {
//				System.out.println("variance withouth model covariance term");
				variance = residualVariance * obsNorm - 2 * atd.dotProduct(m)
				+ m.dotProduct(getAtA().operate(m)) - m.dotProduct(m);
			}
			else
				variance = residualVariance * obsNorm - 2 * atd.dotProduct(m)
					+ m.dotProduct(getAtA().operate(m));
		}
		else
			variance = residualVariance * obsNorm - 2 * atd.dotProduct(m)
				+ m.dotProduct(getA().preMultiply(getA().operate(m)));
		return variance / obsNorm;
	}

	private RealVector atd;

	private RealVector cmAtd;
	
	private ParameterMapping mapping;
	
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
				throw new RuntimeException("Expect List<Integer> bouncingOrders to be defined");
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
			if (column < 0) {
//				System.out.println("Unknown not found in file for " + id.getPerturbationLocation());
				return;
			}
			// 偏微分係数id[i]が何番目のタイムウインドウにあるか
			int k = dVector.whichTimewindow(id);
			if (k < 0) {
//				synchronized(ObservationEquation.class) {
//					System.out.format("Timewindow not found: %s " + id.getStation().getPosition() + "\n", id.toString());
//					BasicID[] tmpids = id.getWaveformType() == WaveformType.OBS ? dVector.getObsIDs() : dVector.getSynIDs();
//					IntStream.range(0, tmpids.length).forEach(i -> System.out.println(dVector.isPair(id, tmpids[i]) + " " + id + "\n" + tmpids[i]));
//				}
				return;
			}
			int row = dVector.getStartPoints(k);
			double weighting = dVector.getWeighting(k) * parameterList.get(column).getWeighting();
//			if (unknownParameterWeightType != null && unknownParameterWeightType.equals(UnknownParameterWeightType.NO_WEIGHT))
//				weighting = 1.;
			weighting = dVector.getWeighting(k); // TO CHANGE
			
			RealVector weightingVector = dVector.getWeightingVector(k);
			
			//only for 1D!!! TO CHANGE
			weightingVector = weightingVector.mapMultiply(parameterList.get(column).getWeighting());

			double[] partial = id.getData();
//			for (int j = 0; j < partial.length; j++) {
//			if (dVector.getWindowNPTS(k) != partial.length) {
//				System.out.println("Trim partial ID to " + dVector.getWindowNPTS(k) + " points");
//				partial = Arrays.copyOf(partial, dVector.getWindowNPTS(k));
//			}
			for (int j = 0; j < dVector.getWindowNPTS(k); j++) {
//				a.setEntry(row + j, column, partial[j] * weighting);
				a.setEntry(row + j, column, partial[j] * weightingVector.getEntry(j));
//				a.setEntry(row + j, column, partial[j] * weightingVector.getEntry(j) / parameterList.get(column).getWeighting() * 16); //REALLY TEMPORARY
			}
			if (!id.getPartialType().isTimePartial())
				count.incrementAndGet();
			else if (id.getPartialType().equals(PartialType.TIME_SOURCE))
				count_TIMEPARTIAL_SOURCE.incrementAndGet();
			else if (id.getPartialType().equals(PartialType.TIME_RECEIVER))
				count_TIMEPARTIAL_RECEIVER.incrementAndGet();
		});
		if ( count.get() + count_TIMEPARTIAL_RECEIVER.get() + count_TIMEPARTIAL_SOURCE.get() != dVector.getNTimeWindow() * nn ) {
			System.out.println("Printing BasicIDs that are not in the partialID set...");
			Set<id_station> idStationSet 
				= Stream.of(ids).map(id -> new id_station(id.getGlobalCMTID(), id.getStation()))
					.distinct().collect(Collectors.toSet());
			Stream.of(dVector.getObsIDs()).forEach(id -> {
				id_station idStation = new id_station(id.getGlobalCMTID(), id.getStation());
				if (!idStationSet.contains(idStation)) {
					System.out.println(id);
				}
			});
			throw new RuntimeException("Input partials are not enough: " + " " + count.get() + " + " +
					count_TIMEPARTIAL_RECEIVER.get() + " + " + count_TIMEPARTIAL_SOURCE.get() + " != " +
					dVector.getNTimeWindow() + " * (" + numberOfParameterForSturcture + " + " + n + ")");  
		}
		System.err.println("A is read and built in " + Utilities.toTimeString(System.nanoTime() - t));
		
		
		if (combinationType != null) {
//---------------------------------------------------------------------------------
//------------------------------ CORRIDOR_BOXCAR -------------------------------
//---------------------------------------------------------------------------------
			
			if (combinationType.equals(CombinationType.VERTICAL_MAPPING)) {
				System.out.println("--> Using " + combinationType);
				
				parameterList = Stream.of(mapping.getUnknowns()).collect(Collectors.toList());
				
				Matrix aPrime = new Matrix(dVector.getNpts(), parameterList.size());
				
				for (int i = 0; i < parameterList.size(); i++) {
					RealVector vector = new ArrayRealVector(a.getRowDimension());
					for (int index : mapping.getiNewToOriginal(i))
						vector = vector.add(a.getColumnVector(index));
					aPrime.setColumnVector(i, vector);
				}
				
				a = aPrime;
			}
			
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

//---------------------------------------------------------------------------------
//------------------------------ LOWERMANTLE_BOXCAR_3D -------------------------------
//---------------------------------------------------------------------------------
			
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

//---------------------------------------------------------------------------------
//------------------------------ CORRIDOR_TRIANGLE -------------------------------
//---------------------------------------------------------------------------------
			
			// Triangle splines
			if (combinationType.equals(CombinationType.CORRIDOR_TRIANGLE)) {
				System.out.println("Combining 1-D pixels into triangles");
				TriangleRadialSpline trs = new TriangleRadialSpline(nUnknowns, parameterList);
				a = trs.computeNewA(a);
				parameterList = trs.getNewParameters();
			}
			
//---------------------------------------------------------------------------------
//------------------------------ TRANSITION_ZONE_23 -------------------------------
//---------------------------------------------------------------------------------
			
			if (combinationType.equals(CombinationType.TRANSITION_ZONE_23)) {
				System.out.println("--> Using " + combinationType);
				
				int nUpperMantle = 16;
				int originalNlowerMantle = 14;
				int nLowerMantle = originalNlowerMantle / 2;
				
				double maxR = 6034.507;
				double minR = 5430.321;
				double originalDeltaR = 20.834;
				maxR += originalDeltaR / 2.;
				minR -= originalDeltaR / 2.;
				int nOriginal = originalNlowerMantle + nUpperMantle;
				int nNewPerturbationR = nLowerMantle + nUpperMantle;
				double deltaR = originalDeltaR;
				double deltaR_lowerMantle = originalDeltaR * 2.;
				
				List<HorizontalPosition> horizontalPositions = parameterList.stream()
						.map(p -> p.getLocation().toHorizontalPosition())
						.distinct()
						.collect(Collectors.toList());
				
				Matrix aPrime = new Matrix(dVector.getNpts(), a.getColumnDimension() 
						- numberOfParameterForSturcture + nNewPerturbationR * horizontalPositions.size());
				List<UnknownParameter> parameterPrime = new ArrayList<>();
				
				double[] newPerturbationR = new double[nNewPerturbationR];
				for (int i = 0; i < nLowerMantle; i++)
					newPerturbationR[i] = minR + i * deltaR_lowerMantle + deltaR_lowerMantle / 2.;
				for (int i = 0; i < nUpperMantle; i++)
					newPerturbationR[i + nLowerMantle] = newPerturbationR[nLowerMantle - 1] + deltaR_lowerMantle / 2. 
						+ i * deltaR + deltaR / 2.;
				
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
					Location loc = parameterList.get(i).getLocation();
					double r = loc.getR();
					int iR = 0;
					if (r > 5721)
						iR = (int) ((r - 5721.) / deltaR) + nLowerMantle;
					else
						iR = (int) ((r - minR) / deltaR_lowerMantle);
					Location newLoc = loc.toLocation(newPerturbationR[iR]);
					System.out.println("DEBUG1: " + r + " " + newPerturbationR[iR]);
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
					aPrime.setColumnVector(i, vector);
					//
					//fill new Unknown parameter list for current PartialType
					double weighting = combinedWeightingMap.get(newLoc);
					parameterPrime.add(new Physical3DParameter(PartialType.MU, newLoc, weighting));
				}
			
			//TODO set time partials
			
			parameterList = parameterPrime;
			a = aPrime;
			
			//debug
			System.out.println("Debug 3: norm of A matrix = " + a.getNorm());
			//
			}
		
//-------------------------------------------------------------------------------
//---------------------------------TRANSITION_ZONE_20 ---------------------------
//-------------------------------------------------------------------------------
		
		if (combinationType.equals(CombinationType.TRANSITION_ZONE_20)) {
			System.out.println("--> Using " + combinationType);
			
			int nUpperMantle = 16;
			int originalNlowerMantle = 8;
			int nLowerMantle = originalNlowerMantle / 2;
			
			double maxR = 6034.507;
			double minR = 5555.325;
			double originalDeltaR = 20.834;
			maxR += originalDeltaR / 2.;
			minR -= originalDeltaR / 2.;
			int nOriginal = originalNlowerMantle + nUpperMantle;
			int nNewPerturbationR = nLowerMantle + nUpperMantle;
			double deltaR = originalDeltaR;
			double deltaR_lowerMantle = originalDeltaR * 2.;
			
			List<HorizontalPosition> horizontalPositions = parameterList.stream()
					.filter(p -> !p.getPartialType().isTimePartial())
					.map(p -> p.getLocation().toHorizontalPosition())
					.distinct()
					.collect(Collectors.toList());
			
			Matrix aPrime = new Matrix(dVector.getNpts(), a.getColumnDimension() 
					- numberOfParameterForSturcture + nNewPerturbationR * horizontalPositions.size());
			List<UnknownParameter> parameterPrime = new ArrayList<>();
			
			double[] newPerturbationR = new double[nNewPerturbationR];
			for (int i = 0; i < nLowerMantle; i++)
				newPerturbationR[i] = minR + i * deltaR_lowerMantle + deltaR_lowerMantle / 2.;
			for (int i = 0; i < nUpperMantle; i++)
				newPerturbationR[i + nLowerMantle] = newPerturbationR[nLowerMantle - 1] + deltaR_lowerMantle / 2. 
					+ i * deltaR + deltaR / 2.;
			
			List<Location> newLocations = new ArrayList<>();
			for (double r : newPerturbationR) {
				for (HorizontalPosition hp : horizontalPositions) {
					Location loc = hp.toLocation(r);
					newLocations.add(loc);
				}
			}
			
			Map<Location, List<Integer>> combinationIndexMap = new HashMap<>();
			Map<Location, Double> combinedWeightingMap = new HashMap<>();
			Map<UnknownParameter, Integer> timeParameterMap = new HashMap<>();
			
			for (int i = 0; i < parameterList.size(); i++) {
				UnknownParameter parameter = parameterList.get(i);
				if (parameter.getPartialType().isTimePartial()) {
					timeParameterMap.put(parameter, i);
				}
				else {
					Location loc = parameter.getLocation();
					double r = loc.getR();
					int iR = 0;
					if (r > 5721)
						iR = (int) ((r - 5721.) / deltaR) + nLowerMantle;
					else
						iR = (int) ((r - minR) / deltaR_lowerMantle);
					Location newLoc = loc.toLocation(newPerturbationR[iR]);
					System.out.println("DEBUG1: " + r + " " + newPerturbationR[iR]);
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
			int iFilled = 0;
			for (int i = 0; i < newLocations.size(); i++) {
				Location newLoc = newLocations.get(i);
				List<Integer> indices = combinationIndexMap.get(newLoc);
				RealVector vector = new ArrayRealVector(a.getRowDimension());
				for (int index : indices)
					vector = vector.add(a.getColumnVector(index));
				aPrime.setColumnVector(i, vector);
				//
				//fill new Unknown parameter list for current PartialType
				double weighting = combinedWeightingMap.get(newLoc);
				parameterPrime.add(new Physical3DParameter(PartialType.MU, newLoc, weighting));
				iFilled++;
			}
		
			if (time_receiver || time_source) {
				List<UnknownParameter> timeParameters =
					parameterList.stream().filter(p -> p.getPartialType().isTimePartial())
						.collect(Collectors.toList());
				for (int i = 0; i < timeParameters.size(); i++) {
					UnknownParameter p = timeParameters.get(i);
					int ia = timeParameterMap.get(p);
					aPrime.setColumnVector(iFilled + i, a.getColumnVector(ia));
					parameterPrime.add(p);
				}
			}
		
			parameterList = parameterPrime;
			a = aPrime;
		
			//debug
			System.out.println("Debug 3: norm of A matrix = " + a.getNorm());
			//
		}
		
//--------------------
//-------------------- TRANSITION ZONE 14 --------------------------
//-------------------------------------------------
		if (combinationType.equals(CombinationType.TRANSITION_ZONE_14)) {
			System.out.println("--> Using " + combinationType);
			
			int nUpperMantle = 4;
			int originalNTZ = 12;
			int originalNlowerMantle = 8;
			int nTZ = originalNTZ / 2;
			int nLowerMantle = originalNlowerMantle / 2;
			
			double maxR = 6034.507;
			double minR = 5555.325;
			double originalDeltaR = 20.834;
			maxR += originalDeltaR / 2.;
			minR -= originalDeltaR / 2.;
			int nOriginal = originalNlowerMantle + originalNTZ + nUpperMantle;
			int nNewPerturbationR = nLowerMantle + nUpperMantle + nTZ;
			double deltaR = originalDeltaR;
			double deltaR_TZ = originalDeltaR * 2;
			double deltaR_lowerMantle = originalDeltaR * 2.;
			
			List<HorizontalPosition> horizontalPositions = parameterList.stream()
					.filter(p -> !p.getPartialType().isTimePartial())
					.map(p -> p.getLocation().toHorizontalPosition())
					.distinct()
					.collect(Collectors.toList());
			
			Matrix aPrime = new Matrix(dVector.getNpts(), a.getColumnDimension() 
					- numberOfParameterForSturcture + nNewPerturbationR * horizontalPositions.size());
			List<UnknownParameter> parameterPrime = new ArrayList<>();
			
			double[] newPerturbationR = new double[nNewPerturbationR];
			for (int i = 0; i < nLowerMantle; i++)
				newPerturbationR[i] = minR + i * deltaR_lowerMantle + deltaR_lowerMantle / 2.;
			for (int i = 0; i < nTZ; i++)
				newPerturbationR[i + nLowerMantle] = newPerturbationR[nLowerMantle - 1] + deltaR_lowerMantle / 2.
					+ i * deltaR_TZ + deltaR_TZ / 2.;
			for (int i = 0; i < nUpperMantle; i++)
				newPerturbationR[i + nLowerMantle + nTZ] = newPerturbationR[nLowerMantle + nTZ - 1] + deltaR_TZ / 2. 
					+ i * deltaR + deltaR / 2.;
			
			for (int i = 0; i < newPerturbationR.length; i++)
				System.out.println("DEBUG0: " + newPerturbationR[i]);
			
			List<Location> newLocations = new ArrayList<>();
			for (double r : newPerturbationR) {
				for (HorizontalPosition hp : horizontalPositions) {
					Location loc = hp.toLocation(r);
					newLocations.add(loc);
				}
			}
			
			Map<Location, List<Integer>> combinationIndexMap = new HashMap<>();
			Map<Location, Double> combinedWeightingMap = new HashMap<>();
			Map<UnknownParameter, Integer> timeParameterMap = new HashMap<>();
			
			for (int i = 0; i < parameterList.size(); i++) {
				UnknownParameter parameter = parameterList.get(i);
				if (parameter.getPartialType().isTimePartial()) {
					timeParameterMap.put(parameter, i);
				}
				else {
					Location loc = parameter.getLocation();
					double r = loc.getR();
					int iR = 0;
					if (r > 5961.)
						iR = (int) ((r - 5961.) / deltaR) + nLowerMantle + nTZ;
					else if (r < 5961. && r > 5721.)
						iR = (int) ((r - 5721.) / deltaR_TZ) + nLowerMantle;
					else
						iR = (int) ((r - minR) / deltaR_lowerMantle);
					Location newLoc = loc.toLocation(newPerturbationR[iR]);
					System.out.println("DEBUG1: " + r + " " + newPerturbationR[iR]);
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
			int iFilled = 0;
			for (int i = 0; i < newLocations.size(); i++) {
				Location newLoc = newLocations.get(i);
				List<Integer> indices = combinationIndexMap.get(newLoc);
				RealVector vector = new ArrayRealVector(a.getRowDimension());
				for (int index : indices)
					vector = vector.add(a.getColumnVector(index));
				aPrime.setColumnVector(i, vector);
				//
				//fill new Unknown parameter list for current PartialType
				double weighting = combinedWeightingMap.get(newLoc);
				parameterPrime.add(new Physical3DParameter(PartialType.MU, newLoc, weighting));
				iFilled++;
			}
		
			if (time_receiver || time_source) {
				List<UnknownParameter> timeParameters =
					parameterList.stream().filter(p -> p.getPartialType().isTimePartial())
						.collect(Collectors.toList());
				for (int i = 0; i < timeParameters.size(); i++) {
					UnknownParameter p = timeParameters.get(i);
					int ia = timeParameterMap.get(p);
					aPrime.setColumnVector(iFilled + i, a.getColumnVector(ia));
					parameterPrime.add(p);
				}
			}
		
			parameterList = parameterPrime;
			a = aPrime;
		
			//debug
			System.out.println("Debug 3: norm of A matrix = " + a.getNorm());
			//
		}
//--------------------------------------- END IF COMBINATION_TYPE != NULL
		}
//---------------------------------------------------------------------------------
//---------------------------------------------------------------------------------
//---------------------------------------------------------------------------------
		
		// Normalize time partials
		Map<UnknownParameter, Double> timeWeightsMap = new HashMap<>();
		double meanAColumnNorm = 0;
		int ntmp = 0;
		double amplifyTimePartial = 5.;
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
			if (tmpNorm > 0) {
				double weight = meanAColumnNorm / tmpNorm * amplifyTimePartial;
				a.setColumnVector(j, a.getColumnVector(j).mapMultiply(weight));
				timeWeightsMap.put(parameterList.get(j), weight);
				System.out.println(j + " " + weight);
			}
		}
		
		//normalize PARQ
		if (parameterList.stream().filter(p -> p.getPartialType().equals(PartialType.PARQ)).count() > 0
				&& parameterList.stream().filter(p -> p.getPartialType().equals(PartialType.PAR2)).count() > 0) {
			double empiricalFactor = 1.;
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
		}
//		
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
		
		if (unknownParameterWeightType != null && !unknownParameterWeightType.equals(UnknownParameterWeightType.NO_WEIGHT)) {
			System.out.println("Further weighting AtA");
			unknownParameterWeigths = new ArrayList<>();
			WeightUnknownParameter wup = null;
			if (time_receiver || time_source)
				wup = new WeightUnknownParameter(unknownParameterWeightType
						, parameterList, timeWeightsMap);
			else
				wup = new WeightUnknownParameter(unknownParameterWeightType
					, parameterList);
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
			case PARVS:
			case PARVSIM:
			case PARVP:
			case PARG:
			case PARM:
			case PAR00:
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
			case KAPPA:
			case LAMBDA2MU:
			case Vs:
				if (location.equals(((Physical3DParameter) parameterList.get(i)).getPointLocation())) {
					return i;
				}
				break;
			default:
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
	
	public RealVector getDiagonalOfAtA() {
		RealVector r = new ArrayRealVector(parameterList.size());
		if (ata != null)
			for (int i = 0; i < parameterList.size(); i++) {
				r.setEntry(i, ata.getEntry(i, i));
			}
		else
			for (int i = 0; i < parameterList.size(); i++) {
				double ataii = a.getColumnVector(i).dotProduct(a.getColumnVector(i));
				r.setEntry(i, ataii);
			}
		return r;
	}
	
	public ModelCovarianceMatrix getCm() {
		return cm;
	}
	
	public RealMatrix getCmAtA_1() {
		if (cmAtA_1 != null) {
			System.out.println("No recomputation of cmAtA_1");
			return cmAtA_1;
		}
		if (cm == null)
			throw new RuntimeException("The model covariance matrix Cm is not set");
		if (ata == null) {
			synchronized (this) {
				if (ata == null) {
					ata = a.computeAtA();
				}
				if (cmAtA_1 == null) {
					throw new RuntimeException("The model covariance matrix Cm is not set");
//					Matrix identity = new Matrix(a.getColumnDimension(), a.getColumnDimension());
//					for (int i = 0; i < a.getColumnDimension(); i++)
//						identity.setEntry(i, i, 1.);
//					cmAtA_1 = (cm.multiply(ata)).add(identity);
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
		IntStream.range(0, ids.length).parallel().forEach(i -> {
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
	
	public void outputAtA(Path AtAPath) {
		if (a == null) {
//			System.out.println("no more A");
			return;
		}
		if (ata == null) {
			System.err.println(" No more ata. Computing again");
			ata = a.computeAtA();
		}
		
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(AtAPath))) {
			for (int i = 0; i < parameterList.size(); i++) {
				for (int j = 0; j <= i; j++) {
					pw.println(parameterList.get(i) + " " + parameterList.get(j) + " " + ata.getEntry(i, j));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void outputAtd(Path AtdPath) {
		if (a == null) {
//			System.out.println("no more A");
			return;
		}
		if (atd == null) {
			System.err.println(" No more atd. Computing again");
			atd = computeAtD(dVector.getD());
		}
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(AtdPath))) {
			for (int i = 0; i < parameterList.size(); i++) {
				pw.println(parameterList.get(i) + " " + atd.getEntry(i));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void outputSensitivity(Path outPath) throws IOException {
		if (a == null) {
//			System.out.println("no more A");
//			return;
		}
		if (ata == null) {
			System.err.println(" No more ata. Computing again");
				if (cm == null)
					ata = a.computeAtA();
				else {
					throw new RuntimeException("No cm");
//					Matrix identity = new Matrix(a.getColumnDimension(), a.getColumnDimension());
//					for (int i = 0; i < a.getColumnDimension(); i++)
//						identity.setEntry(i, i, 1.);
//					ata = (cm.multiply(a.computeAtA())).add(identity);
				}
		}
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			Map<UnknownParameter, Double> sMap = Sensitivity.sensitivityMap(ata, parameterList);
			List<UnknownParameter> unknownForStructure = parameterList.stream().filter(unknown -> !unknown.getPartialType().isTimePartial())
					.collect(Collectors.toList());
			for (UnknownParameter unknown : unknownForStructure) {
				double lat = unknown.getLocation().getLatitude();
				double lon = unknown.getLocation().getLongitude();
				if (lon < 0)
					lon += 360.;
				double r = unknown.getLocation().getR();
				pw.println(unknown.getPartialType() + " " + lat + " " + lon + " " + r + " " + sMap.get(unknown));
			}
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
	
	private class id_station {
		private GlobalCMTID id;
		private Station station;
		
		public id_station(GlobalCMTID id, Station station) {
			this.id = id;
			this.station = station;
		}
		
		@Override
		public boolean equals(Object obj) {
			   if (obj == null) {
			        return false;
			    }
			    if (!id_station.class.isAssignableFrom(obj.getClass())) {
			        return false;
			    }
			    final id_station other = (id_station) obj;
			    if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
			        return false;
			    }
			    if ((this.station == null) ? (other.station != null) : !this.station.equals(other.station)) {
			        return false;
			    }
			    return true;
		}
		
		@Override
		public int hashCode() {
		    int hash = 3;
		    hash = 53 * hash + (this.id != null ? this.id.hashCode() : 0);
		    hash = 53 * hash + (this.station != null ? this.station.hashCode() : 0);
		    return hash;
		}
	}
	
	public void setAtdForCheckerboard(RealVector checkeboardPerturbationVector) {
		if (ata == null)
			throw new RuntimeException("Cannot set checkerboard since ata=null");
		atd = ata.operate(checkeboardPerturbationVector);
	}
}
