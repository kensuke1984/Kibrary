package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.inversion.addons.CombinationType;
import io.github.kensuke1984.kibrary.inversion.addons.ModelCovarianceMatrix;
import io.github.kensuke1984.kibrary.inversion.addons.ParameterMapping;
import io.github.kensuke1984.kibrary.inversion.addons.Sensitivity;
import io.github.kensuke1984.kibrary.inversion.addons.TimeReceiverSideParameter;
import io.github.kensuke1984.kibrary.inversion.addons.TimeSourceSideParameter;
import io.github.kensuke1984.kibrary.inversion.addons.UnknownParameterWeightType;
import io.github.kensuke1984.kibrary.inversion.addons.WeightUnknownParameter;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.math.MatrixComputation;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;

import io.github.kensuke1984.kibrary.inversion.montecarlo.DataGenerator;
import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtAEntry;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtdEntry;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import java.awt.PageAttributes;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.management.RuntimeErrorException;

/**
 * A&delta;m=&delta;d
 * <p>
 * This class is <b>immutable</b>.
 * <p>
 *
 * @author Kensuke Konishi
 * @version 0.2.1.3
 * @see Dvector {@link UnknownParameter}
 */
public class ObservationEquation {
	
	//TODO update this part of the code (not yet used)
//    private final DataGenerator<RealVector, RealVector[]> BORN_GENERATOR;
//    private final DataGenerator<RealVector, Double> VARIANCE_GENERATOR;

	private List<UnknownParameter> PARAMETER_LIST;
	private List<UnknownParameter> ORIGINAL_PARAMETER_LIST;
	private Dvector DVECTOR;
	private Matrix a;
	private RealVector atd;
	private RealMatrix ata;
	
	private RealMatrix cmAtA_1;
	private RealVector cmAtd;
	private ModelCovarianceMatrix cm;
	private List<Double> unknownParameterWeigths;
	private ParameterMapping mapping;
	
	private double mul;
	private RealVector m;
	
	public ObservationEquation(RealMatrix ataMatrix, List<UnknownParameter> unknownParameterList, RealVector atdVector) {
		this.ata = ataMatrix;
		this.atd = atdVector;
		this.ORIGINAL_PARAMETER_LIST = unknownParameterList;
		this.PARAMETER_LIST = unknownParameterList;
		this.cm = null;
	}
	
    /**
     * @param partialIDs    for A
     * @param parameterList for &delta;m
     * @param dVector       for &delta;d
     */
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector) {
		this(partialIDs, parameterList, dVector, false, false, null, null, null, null);
	}

	/**
	 * @param partialIDs
	 * @param parameterList
	 * @param dVector
	 * @param time_source
	 * @param time_receiver
	 * @param combinationType
	 * @param nUnknowns
	 * @param unknownParameterWeightType
	 * @param verticalMappingPath
	 * @author anselme
	 */
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector,
			boolean time_source, boolean time_receiver, CombinationType combinationType, Map<PartialType
			, Integer[]> nUnknowns, UnknownParameterWeightType unknownParameterWeightType, Path verticalMappingPath) {
		if (verticalMappingPath != null) {
			this.mapping = new ParameterMapping(parameterList.toArray(new UnknownParameter[0]), verticalMappingPath);
			combinationType = CombinationType.VERTICAL_MAPPING;
			System.out.println("Using vertical mapping " + verticalMappingPath);
		}
		DVECTOR = dVector;
		PARAMETER_LIST = parameterList;
		ORIGINAL_PARAMETER_LIST = parameterList;
		List<Integer> bouncingOrders = null;
		if (time_receiver) {
			bouncingOrders = new ArrayList<Integer>();
			bouncingOrders.add(1);
		}
		System.out.println("Using combination type " + combinationType);
		readA(partialIDs, time_receiver, time_source, bouncingOrders, combinationType, nUnknowns,
				unknownParameterWeightType);
		atd = computeAtD(dVector.getD());
		ata = a.computeAtA();
		System.out.println("AtA mean trace = " + (ata.getTrace() / ata.getColumnDimension()));
		System.out.println("Atd mean norm = " + (atd.getLInfNorm() / ata.getColumnDimension()));
	}
	
	/**
	 * @param partialIDs
	 * @param parameterList
	 * @param dVector
	 * @param time_source
	 * @param time_receiver
	 * @param combinationType
	 * @param nUnknowns
	 * @param unknownParameterWeightType
	 * @param verticalMappingPath
	 * @param computeAtA
	 * @author anselme
	 */
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector,
			boolean time_source, boolean time_receiver, CombinationType combinationType, Map<PartialType
			, Integer[]> nUnknowns, UnknownParameterWeightType unknownParameterWeightType, Path verticalMappingPath, boolean computeAtA) {
		if (verticalMappingPath != null) {
			this.mapping = new ParameterMapping(parameterList.toArray(new UnknownParameter[0]), verticalMappingPath);
			combinationType = CombinationType.VERTICAL_MAPPING;
			System.out.println("Using vertical mapping " + verticalMappingPath);
		}
		DVECTOR = dVector;
		PARAMETER_LIST = parameterList;
		ORIGINAL_PARAMETER_LIST = parameterList;
		List<Integer> bouncingOrders = null;
		if (time_receiver) {
			bouncingOrders = new ArrayList<Integer>();
			bouncingOrders.add(1);
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
	
	/**
	 * @param partialIDs
	 * @param parameterList
	 * @param dVector
	 * @param time_source
	 * @param time_receiver
	 * @param nUnknowns
	 * @param lambdaMU
	 * @param lambdaQ
	 * @param correlationScaling
	 * @param verticalMappingPath
	 * @author anselme
	 */
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector,
			boolean time_source, boolean time_receiver, Map<PartialType, Integer[]> nUnknowns, double lambdaMU, double lambdaQ, double correlationScaling
			, Path verticalMappingPath) {
		CombinationType combinationType = null;
		if (verticalMappingPath != null) {
			this.mapping = new ParameterMapping(parameterList.toArray(new UnknownParameter[0]), verticalMappingPath);
			System.out.println("Using vertical mapping " + verticalMappingPath);
			combinationType = CombinationType.VERTICAL_MAPPING;
		}
		DVECTOR = dVector;
		PARAMETER_LIST = parameterList;
		ORIGINAL_PARAMETER_LIST = parameterList;
		List<Integer> bouncingOrders = null;
		if (time_receiver) {
			//TODO remove the use of bouncingOrders
			bouncingOrders = new ArrayList<Integer>();
			bouncingOrders.add(1);
		}
		readA(partialIDs, time_receiver, time_source, bouncingOrders, combinationType, nUnknowns, null);
		
		double AtANormalizedTrace = 0;
		double count = 0;
		for (int i = 0; i < PARAMETER_LIST.size(); i++) {
			AtANormalizedTrace += a.getColumnVector(i).getNorm();
			count++;
		}
		AtANormalizedTrace /= count;
		System.out.println("AtANormalizedTrace = " + AtANormalizedTrace);
		
		double normalization = AtANormalizedTrace * 0.1;
		
		// model covariance matrix
		cm = new ModelCovarianceMatrix(parameterList, 0., correlationScaling, normalization, true);
		
		Matrix identity = new Matrix(a.getColumnDimension(), a.getColumnDimension());
		for (int i = 0; i < a.getColumnDimension(); i++)
			identity.setEntry(i, i, 1.);
		
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
		
		atd = computeAtD(dVector.getD());
		cmAtd = atd;
	}
	
	/**
	 * @param partialIDs
	 * @param parameterList
	 * @param dVector
	 * @param cm0
	 * @param cmH
	 * @param cmV
	 * @param verticalMappingPath
	 * @param computeAtA
	 * @author anselme
	 */
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector
			, double cm0, double cmH, double cmV, Path verticalMappingPath, boolean computeAtA) {
		CombinationType combinationType = null;
		if (verticalMappingPath != null) {
			this.mapping = new ParameterMapping(parameterList.toArray(new UnknownParameter[0]), verticalMappingPath);
			System.out.println("Using vertical mapping " + verticalMappingPath);
			combinationType = CombinationType.VERTICAL_MAPPING;
		}
		DVECTOR = dVector;
		PARAMETER_LIST = parameterList;
		ORIGINAL_PARAMETER_LIST = parameterList;
	
		readA(partialIDs, false, false, null, combinationType, null, null);
		
		double AtANormalizedTrace = 0;
		for (int i = 0; i < PARAMETER_LIST.size(); i++) {
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
	
	/**
	 * @param partialIDs
	 * @param parameterList
	 * @param dVector
	 * @param cm0
	 * @param cmH
	 * @param cmV
	 * @param verticalMappingPath
	 * @author anselme
	 */
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector
			, double cm0, double cmH, double cmV, Path verticalMappingPath) {
		this(partialIDs, parameterList, dVector, cm0, cmH, cmV, verticalMappingPath, true);
	}
	
	/**
	 * @param partialIDs
	 * @param parameterList
	 * @param dVector
	 * @param ata_prev
	 * @param atd_prev
	 * @author anselme
	 */
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector, Matrix ata_prev, RealVector atd_prev) {
		DVECTOR = dVector;
		PARAMETER_LIST = parameterList;
		ORIGINAL_PARAMETER_LIST = parameterList;
	
		readA(partialIDs, false, false, null, null, null, null);
		
		ata = a.computeAtA().add(ata_prev);
		atd = computeAtD(dVector.getD()).add(atd_prev);
	}
	
	/**
	 * @param ata
	 * @param atd
	 * @param parameterList
	 * @param dVector
	 * @author anselme
	 */
	public ObservationEquation(RealMatrix ata, RealVector atd, List<UnknownParameter> parameterList, Dvector dVector) {
		DVECTOR = dVector;
		PARAMETER_LIST = parameterList;
		ORIGINAL_PARAMETER_LIST = parameterList;
	
		this.ata = ata;
		this.atd = atd;
	}
	
	/**
	 * @param ata
	 * @param atd
	 * @param parameterList
	 * @param dVector
	 * @param a
	 * @author anselme
	 */
	public ObservationEquation(RealMatrix ata, RealVector atd, List<UnknownParameter> parameterList, Dvector dVector, Matrix a) {
		DVECTOR = dVector;
		PARAMETER_LIST = parameterList;
		ORIGINAL_PARAMETER_LIST = parameterList;
	
		this.ata = ata;
		this.atd = atd;
		this.a = a;
	}
	
	/**
	 * @param equation
	 * @return
	 * @author anselme
	 */
	public ObservationEquation add(ObservationEquation equation) {
		RealMatrix atatmp = ata.add(equation.getAtA());
		RealVector atdtmp = atd.add(equation.getAtD());
		DVECTOR.setVariance(DVECTOR.getVariance() + equation.getDVector().getVariance());
		
//		dVector.setObsNormSquare(dVector.getObsNormSquare() + equation.getDVector().getObsNormSquare());
		
		return new ObservationEquation(atatmp, atdtmp, PARAMETER_LIST, DVECTOR, a);
	}
	
	public ObservationEquation scalarMultiply(double d) {
		mul = d;
		RealMatrix atatmp = ata.scalarMultiply(d);
		RealVector atdtmp = atd.mapMultiply(d);
		DVECTOR.setVariance(DVECTOR.getVariance() * d);
//		dVector.mapMultiply(Math.sqrt(d)); // TODO check if valid
//		dVector.setObsNormSquare(dVector.getObsNormSquare() * d);
		ObservationEquation eq = new ObservationEquation(atatmp, atdtmp, PARAMETER_LIST, DVECTOR, a);
		return eq;
	}
	
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
	
	public void applyModelCovarianceMatrix(ModelCovarianceMatrix cm) {
		this.cm = cm;
		Matrix identity = new Matrix(ata.getRowDimension(), ata.getColumnDimension());
		for (int i = 0; i < ata.getColumnDimension(); i++)
			identity.setEntry(i, i, 1.);
		
		ata = cm.rightMultiplyByL(ata);
		ata = cm.leftMultiplyByLT(ata).add(identity);
		atd = cm.getL().preMultiply(atd);
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

	/**
     * Build a kernel matrix from the PartialIDs
     * according to unknown parameter and waveform data
     * @param ids source for A
     */
    private void readA(PartialID[] ids) {
        a = new Matrix(DVECTOR.getNpts(), PARAMETER_LIST.size());
        // partialDataFile.readWaveform();
        long t = System.nanoTime();
        AtomicInteger count = new AtomicInteger();
        Arrays.stream(ids).parallel().forEach(id -> {
            if (count.get() == DVECTOR.getNTimeWindow() * PARAMETER_LIST.size()) return;
            int column = whatNumber(id.getPartialType(), id.getPerturbationLocation(), null, null, null);
            if (column < 0) return;
            // 偏微分係数id[i]が何番目のタイムウインドウにあるか
            int k = DVECTOR.whichTimewindow(id);
            if (k < 0) return;
            int row = DVECTOR.getStartPoints(k);
            double weighting = DVECTOR.getWeighting(k) * PARAMETER_LIST.get(column).getWeighting();
            double[] partial = id.getData();
            for (int j = 0; j < partial.length; j++)
                a.setEntry(row + j, column, partial[j] * weighting);
            count.incrementAndGet();
        });
//		System.out.println(count.get()+" "+ DVECTOR.getNTimeWindow() * PARAMETER_LIST.size()+" "+PARAMETER_LIST.size());
        if (count.get() != DVECTOR.getNTimeWindow() * PARAMETER_LIST.size())
            throw new RuntimeException("Input partials are not enough.");
        System.err.println("A is read and built in " + Utilities.toTimeString(System.nanoTime() - t));
    }
	
    /**
     * Build a kernel matrix from the PartialIDs
     * according to unknown parameter and waveform data
     * @param ids source for A
     */
	private void readA(PartialID[] ids, boolean time_receiver, boolean time_source, List<Integer> bouncingOrders
			, CombinationType combinationType, Map<PartialType, Integer[]> nUnknowns,
			UnknownParameterWeightType unknownParameterWeightType) {
		if (time_source)
			DVECTOR.getUsedGlobalCMTIDset().forEach(id -> PARAMETER_LIST.add(new TimeSourceSideParameter(id)));
		if (time_receiver) {
			if (bouncingOrders == null)
				throw new RuntimeException("Expect List<Integer> bouncingOrders to be defined");
			for (Integer i : bouncingOrders)
				DVECTOR.getUsedStationSet().forEach(station -> PARAMETER_LIST.add(new TimeReceiverSideParameter(station, i.intValue())));
		}
		
		a = new Matrix(DVECTOR.getNpts(), PARAMETER_LIST.size());
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
		int numberOfParameterForSturcture = (int) PARAMETER_LIST.stream().filter(unknown -> !unknown.getPartialType().isTimePartial()).count();
		final int nn = numberOfParameterForSturcture + n;
		
		Arrays.stream(ids).parallel().forEach(id -> {
			if (count.get() + count_TIMEPARTIAL_RECEIVER.get() + count_TIMEPARTIAL_SOURCE.get() == DVECTOR.getNTimeWindow() * nn)
				return;
			int column = whatNumber(id.getPartialType(), id.getPerturbationLocation(),
					id.getStation(), id.getGlobalCMTID(), id.getPhases());
			if (column < 0) {
//				System.out.println("Unknown not found in file for " + id.getPerturbationLocation());
				return;
			}
			// 偏微分係数id[i]が何番目のタイムウインドウにあるか
			int k = DVECTOR.whichTimewindow(id);
			if (k < 0) {
//				synchronized(ObservationEquation.class) {
//					System.out.format("Timewindow not found: %s " + id.getStation().getPosition() + "\n", id.toString());
//					BasicID[] tmpids = id.getWaveformType() == WaveformType.OBS ? dVector.getObsIDs() : dVector.getSynIDs();
//					IntStream.range(0, tmpids.length).forEach(i -> System.out.println(dVector.isPair(id, tmpids[i]) + " " + id + "\n" + tmpids[i]));
//				}
				return;
			}
			int row = DVECTOR.getStartPoints(k);
			double weighting = DVECTOR.getWeighting(k) * PARAMETER_LIST.get(column).getWeighting();
//			if (unknownParameterWeightType != null && unknownParameterWeightType.equals(UnknownParameterWeightType.NO_WEIGHT))
//				weighting = 1.;
			weighting = DVECTOR.getWeighting(k); // TO CHANGE
			
			RealVector weightingVector = DVECTOR.getWeightingVector(k);
			
			//only for 1D!!! TO CHANGE
			weightingVector = weightingVector.mapMultiply(PARAMETER_LIST.get(column).getWeighting());

			double[] partial = id.getData();
			
			double max = new ArrayRealVector(partial).getLInfNorm();
			if (Double.isNaN(max)) System.out.println("NaN " + id);
			
			if (partial.length != DVECTOR.getWindowNPTS(k)) {
				System.err.println(id + " " + partial.length + " " + DVECTOR.getWindowNPTS(k));
				throw new RuntimeException("Partial length does not match window length");
			}
			for (int j = 0; j < DVECTOR.getWindowNPTS(k); j++) {
//				a.setEntry(row + j, column, partial[j] * weighting);
				a.setEntry(row + j, column, partial[j] * weightingVector.getEntry(j));
			}
			if (!id.getPartialType().isTimePartial())
				count.incrementAndGet();
			else if (id.getPartialType().equals(PartialType.TIME_SOURCE))
				count_TIMEPARTIAL_SOURCE.incrementAndGet();
			else if (id.getPartialType().equals(PartialType.TIME_RECEIVER))
				count_TIMEPARTIAL_RECEIVER.incrementAndGet();
		});
		if ( count.get() + count_TIMEPARTIAL_RECEIVER.get() + count_TIMEPARTIAL_SOURCE.get() != DVECTOR.getNTimeWindow() * nn ) {
			System.out.println("Printing BasicIDs that are not in the partialID set...");
			Set<id_station> idStationSet 
				= Stream.of(ids).map(id -> new id_station(id.getGlobalCMTID(), id.getStation()))
					.distinct().collect(Collectors.toSet());
			Stream.of(DVECTOR.getObsIDs()).forEach(id -> {
				id_station idStation = new id_station(id.getGlobalCMTID(), id.getStation());
				if (!idStationSet.contains(idStation)) {
					System.out.println(id);
				}
			});
			throw new RuntimeException("Input partials are not enough: " + " " + count.get() + " + " +
					count_TIMEPARTIAL_RECEIVER.get() + " + " + count_TIMEPARTIAL_SOURCE.get() + " != " +
					DVECTOR.getNTimeWindow() + " * (" + numberOfParameterForSturcture + " + " + n + ")");  
		}
		System.err.println("A is read and built in " + Utilities.toTimeString(System.nanoTime() - t));
		
		// Normalize time partials
		Map<UnknownParameter, Double> timeWeightsMap = new HashMap<>();
		double meanAColumnNorm = 0;
		int ntmp = 0;
		double amplifyTimePartial = 5.;
		for (int j = 0; j < a.getColumnDimension(); j++) {
//			if (!parameterList.get(j).getPartialType().equals(PartialType.PAR2))
			if (PARAMETER_LIST.get(j).getPartialType().isTimePartial())
				continue;
			meanAColumnNorm += a.getColumnVector(j).getNorm();
			ntmp++;
		}
		meanAColumnNorm /= ntmp; 
		for (int j = 0; j < a.getColumnDimension(); j++) {
			if (!PARAMETER_LIST.get(j).getPartialType().isTimePartial())
				continue;
			double tmpNorm = a.getColumnVector(j).getNorm();
			if (tmpNorm > 0) {
				double weight = meanAColumnNorm / tmpNorm * amplifyTimePartial;
				a.setColumnVector(j, a.getColumnVector(j).mapMultiply(weight));
				timeWeightsMap.put(PARAMETER_LIST.get(j), weight);
				System.out.println(j + " " + weight);
			}
		}
		
		//normalize PARQ
		if (PARAMETER_LIST.stream().filter(p -> p.getPartialType().equals(PartialType.PARQ)).count() > 0
				&& PARAMETER_LIST.stream().filter(p -> p.getPartialType().equals(PartialType.PAR2)).count() > 0) {
			double empiricalFactor = 1.5;
			meanAColumnNorm = 0;
			double meanAQNorm = 0;
			ntmp = 0;
			int ntmpQ = 0;
			for (int j = 0; j < a.getColumnDimension(); j++) {
				if (PARAMETER_LIST.get(j).getPartialType().isTimePartial())
					continue;
				if (PARAMETER_LIST.get(j).getPartialType().equals(PartialType.PARQ)) {
					meanAQNorm += a.getColumnVector(j).getNorm();
					ntmpQ++;
				}
				else if (PARAMETER_LIST.get(j).getPartialType().equals(PartialType.PAR2)){
					meanAColumnNorm += a.getColumnVector(j).getNorm();
					ntmp++;
				}
			}
			meanAColumnNorm /= ntmp;
			meanAQNorm /= ntmpQ;
			if (ntmpQ > 0) {
				for (int j = 0; j < a.getColumnDimension(); j++) {
					if (!PARAMETER_LIST.get(j).getPartialType().equals(PartialType.PARQ))
						continue;
					if (ntmp == 0 || ntmpQ == 0)
						continue;
					a.setColumnVector(j, a.getColumnVector(j).mapMultiply(empiricalFactor * meanAColumnNorm / meanAQNorm));
				}
				System.out.println("PAR2 / PARQ = " + empiricalFactor * meanAColumnNorm / meanAQNorm);
			}
		}
		
		if (PARAMETER_LIST.stream().filter(p -> p.getPartialType().equals(PartialType.PARQ)).count() > 0
				&& PARAMETER_LIST.stream().filter(p -> p.getPartialType().equals(PartialType.PARVS)).count() > 0) {
			double empiricalFactor = 1.5;
			meanAColumnNorm = 0;
			double meanAQNorm = 0;
			ntmp = 0;
			int ntmpQ = 0;
			for (int j = 0; j < a.getColumnDimension(); j++) {
				if (PARAMETER_LIST.get(j).getPartialType().isTimePartial())
					continue;
				if (PARAMETER_LIST.get(j).getPartialType().equals(PartialType.PARQ)) {
					meanAQNorm += a.getColumnVector(j).getNorm();
					ntmpQ++;
				}
				else if (PARAMETER_LIST.get(j).getPartialType().equals(PartialType.PARVS)){
					meanAColumnNorm += a.getColumnVector(j).getNorm();
					ntmp++;
				}
			}
//			meanAColumnNorm /= ntmp;
//			meanAQNorm /= ntmpQ;
			if (ntmpQ > 0) {
				for (int j = 0; j < a.getColumnDimension(); j++) {
					if (!PARAMETER_LIST.get(j).getPartialType().equals(PartialType.PARQ))
						continue;
					if (ntmp == 0 || ntmpQ == 0)
						continue;
//					a.setColumnVector(j, a.getColumnVector(j).mapMultiply(empiricalFactor * meanAColumnNorm / meanAQNorm));
					a.setColumnVector(j, a.getColumnVector(j).mapMultiply(1.3)); // 1.3 TODO change this terrible way of doing it
				}
				System.out.println("PARVS / PARQ = " + empiricalFactor * meanAColumnNorm / meanAQNorm);
//				System.out.println("PARVS / PARQ = 1.3");
			}
		}
			
		//normalize PAR00
		if (PARAMETER_LIST.stream().filter(p -> p.getPartialType().equals(PartialType.PAR00)).count() > 0
				&& PARAMETER_LIST.stream().filter(p -> p.getPartialType().equals(PartialType.PARVS)).count() > 0) {
			double empiricalFactor = 1.;
			meanAColumnNorm = 0;
			double meanAQNorm = 0;
			ntmp = 0;
			int ntmpQ = 0;
			for (int j = 0; j < a.getColumnDimension(); j++) {
				if (PARAMETER_LIST.get(j).getPartialType().isTimePartial())
					continue;
				if (PARAMETER_LIST.get(j).getPartialType().equals(PartialType.PAR00)) {
					meanAQNorm += a.getColumnVector(j).getNorm();
					ntmpQ++;
				}
				else if (PARAMETER_LIST.get(j).getPartialType().equals(PartialType.PARVS)){
					meanAColumnNorm += a.getColumnVector(j).getNorm();
					ntmp++;
				}
			}
			meanAColumnNorm /= ntmp;
			meanAQNorm /= ntmpQ;
			if (ntmpQ > 0) {
				for (int j = 0; j < a.getColumnDimension(); j++) {
					if (!PARAMETER_LIST.get(j).getPartialType().equals(PartialType.PAR00))
						continue;
					if (ntmp == 0 || ntmpQ == 0)
						continue;
					a.setColumnVector(j, a.getColumnVector(j).mapMultiply(empiricalFactor * meanAColumnNorm / meanAQNorm));
				}
				System.out.println("PARVS / PAR00 = " + empiricalFactor * meanAColumnNorm / meanAQNorm);
			}
		}
	}
	
	/**
     * A&delta;m = &delta;d 求めたいのは (&delta;d - A&delta;m)<sup>T</sup>(&delta;d - A&delta;m) / |obs|<sup>2</sup>
     * <p>
     * (&delta;d<sup>T</sup> - &delta;m<sup>T</sup>A<sup>T</sup>)(&delta;d - A&delta;m) = &delta;d<sup>T</sup>&delta;d - &delta;d<sup>T
     * </sup>A&delta;m - &delta;m<sup>T</sup>A<sup>T</sup>&delta;d + &delta;m<sup>T</sup>A<sup>T</sup>A&delta;m = &delta;d<sup>T
     * </sup>&delta;d - 2*(A<sup>T</sup>&delta;d)&delta;m<sup>T</sup> + &delta;m<sup>T</sup>(A<sup>T</sup>A)&delta;m
     *
     * @param m &delta;m
     * @return |A&delta;m - &delta;d|<sup>2</sup>/|obs|<sup>2</sup>
     */
	public double varianceOf(RealVector m) {
		Objects.requireNonNull(m);
//		double obs2 = dVector.getObsNorm() * dVector.getObsNorm();
		double obs2 = DVECTOR.getObsNormSquare();
		double var0 = DVECTOR.getVariance() * obs2;
		double variance = 0;
		if (ata != null) {
			if (cm != null) {
//				System.out.println("variance withouth model covariance term");
				variance = DVECTOR.getDNorm() * DVECTOR.getDNorm() - 2 * atd.dotProduct(m)
						+ m.dotProduct(getAtA().operate(m)) - m.dotProduct(m);
			}
			else {
//				variance = dVector.getDNorm() * dVector.getDNorm() - 2 * atd.dotProduct(m)
//				+ m.dotProduct(getAtA().operate(m));
				variance = var0 - 2 * atd.dotProduct(m)
						+ m.dotProduct(getAtA().operate(m));
//				System.out.println(var0 + " " + 2 * atd.dotProduct(m) + " " + m.dotProduct(getAtA().operate(m)));
			}
		}
		else
			variance = DVECTOR.getDNorm() * DVECTOR.getDNorm() - 2 * atd.dotProduct(m)
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

    /**
     * @param type     to look for
     * @param location to look for
     * @return i, m<sub>i</sub> = type, parameterが何番目にあるか なければ-1
     */
	private int whatNumber(PartialType type, Location location, Station station, GlobalCMTID id, Phase[] phases) {
		for (int i = 0; i < PARAMETER_LIST.size(); i++) {
			if (PARAMETER_LIST.get(i).getPartialType() != type)
				continue;
			switch (type) {
			case TIME_SOURCE:
				if (id.equals( ((TimeSourceSideParameter) PARAMETER_LIST.get(i)).getGlobalCMTID() ))
					return i;
				break;
			case TIME_RECEIVER:
				//TODO
				List<Integer> bouncingOrders = new ArrayList<Integer>();
				bouncingOrders.add(1);
				Collections.sort(bouncingOrders);
				int lowestBouncingOrder = bouncingOrders.get(0);
				if (station.equals( ((TimeReceiverSideParameter) PARAMETER_LIST.get(i)).getStation() ) &&
						((TimeReceiverSideParameter) PARAMETER_LIST.get(i)).getBouncingOrder() == lowestBouncingOrder)
					return i;
				break;
			case PARA:
			case PARC:
			case PARF:
			case PARL:
			case PARN:
			case PARQ:
				if (location.getR() == ((Physical1DParameter) PARAMETER_LIST.get(i)).getPerturbationR())
					return i;
				break;
			case PAR1:
			case PAR2:
			case PARVS:
			case PARVP:
			case PARG:
			case PARM:
			case PAR00:
				if (location.getR() == ((Physical1DParameter) PARAMETER_LIST.get(i)).getPerturbationR())
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
				if (location.equals(((Physical3DParameter) PARAMETER_LIST.get(i)).getPointLocation())) {
					return i;
				}
				break;
			default:
				break;
			}
		}
		return -1;
	}
	
    /**
     * @return (deep)copy of A, which can be heavy load.
     */
	public RealMatrix getA() {
		return a.copy();
	}

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
		RealVector r = new ArrayRealVector(PARAMETER_LIST.size());
		if (ata != null)
			for (int i = 0; i < PARAMETER_LIST.size(); i++) {
				r.setEntry(i, ata.getEntry(i, i));
			}
		else
			for (int i = 0; i < PARAMETER_LIST.size(); i++) {
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
     * @param outputPath {@link Path} for an write folder
     */
	public void outputA(Path outputPath) throws IOException {
		if (a == null) {
			System.out.println("no more A");
			return;
		}
		if (Files.exists(outputPath))
			throw new FileAlreadyExistsException(outputPath.toString());
		Files.createDirectories(outputPath);
		BasicID[] ids = DVECTOR.getSynIDs();
		IntStream.range(0, ids.length).parallel().forEach(i -> {
			BasicID id = ids[i];
			Path eventPath = outputPath.resolve(id.getGlobalCMTID().toString());
			try {
				Files.createDirectories(eventPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
			int start = DVECTOR.getStartPoints(i);
			double synStartTime = id.getStartTime();
			Path outPath = eventPath.resolve(
					id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent() + "." + i + ".txt");
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath))) {
				pw.println("#syntime par0 par1, .. parN");
				for (int k = 0; k < id.getNpts(); k++) {
					double synTime = synStartTime + k / id.getSamplingHz();
					pw.print(synTime + " ");
					for (int j = 0, mlen = PARAMETER_LIST.size(); j < mlen; j++)
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
			for (int i = 0; i < PARAMETER_LIST.size(); i++) {
				for (int j = 0; j <= i; j++) {
					pw.println(PARAMETER_LIST.get(i) + " " + PARAMETER_LIST.get(j) + " " + ata.getEntry(i, j));
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
			atd = computeAtD(DVECTOR.getD());
		}
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(AtdPath))) {
			for (int i = 0; i < PARAMETER_LIST.size(); i++) {
				pw.println(PARAMETER_LIST.get(i) + " " + atd.getEntry(i));
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
			Map<UnknownParameter, Double> sMap = Sensitivity.sensitivityMap(ata, PARAMETER_LIST);
			List<UnknownParameter> unknownForStructure = PARAMETER_LIST.stream().filter(unknown -> !unknown.getPartialType().isTimePartial())
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
     * <p>
     * A<sup>T</sup>d = v <br>
     * <p>
     * v<sup>T</sup> = (A<sup>T</sup>d)<sup>T</sup>= d<sup>T</sup>A
     *
     * @param d of A<sup>T</sup>d
     * @return A<sup>T</sup>d
     */
	public RealVector computeAtD(RealVector d) {
			return a.preMultiply(d);
	}

	public List<UnknownParameter> getOriginalParameterList() {
		return ORIGINAL_PARAMETER_LIST;
	}
	
	public List<UnknownParameter> getParameterList() {
		return PARAMETER_LIST;
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
        return DVECTOR;
    }
    
    public int getDlength() {
        return DVECTOR.getNpts();
    }
	
    public int getMlength() {
        return PARAMETER_LIST.size();
    }
    
	/**
	 * Computes Am
	 * 
	 * @param m for Am
	 * @return Am
	 */
	public RealVector operate(RealVector m) {
		return a.operate(m);
	}
	
    /**
     * @return generator of born waveforms
     */
//    public DataGenerator<RealVector, RealVector[]> getBornGenerator() {
//        return BORN_GENERATOR;
//    }
//
//    public DataGenerator<RealVector, Double> getVarianceGenerator() {
//        return VARIANCE_GENERATOR;
//    }
    
    public void setAtdForCheckerboard(RealVector checkeboardPerturbationVector) {
		if (ata == null)
			throw new RuntimeException("Cannot set checkerboard since ata=null");
		atd = ata.operate(checkeboardPerturbationVector);
	}
	
	public ObservationEquation setTypeToZero(PartialType type) {
		ObservationEquation eq = new ObservationEquation(ata, atd, PARAMETER_LIST, DVECTOR, a);
		for (int i = 0; i < PARAMETER_LIST.size(); i++) {
			if (PARAMETER_LIST.get(i).getPartialType().equals(type))
				atd.setEntry(i, 0.);
		}
		for (int i = 0; i < PARAMETER_LIST.size(); i++) {
			for (int j = 0; j < PARAMETER_LIST.size(); j++) {
				if (PARAMETER_LIST.get(i).getPartialType().equals(type) || PARAMETER_LIST.get(j).getPartialType().equals(type))
					ata.setEntry(i, j, 0.);
			}
		}
		return eq;
	}
	
	public void setVariance(double variance) {
		DVECTOR.setVariance(variance);
	}
	
	public void setObsNormSquare(double obs2) {
		DVECTOR.setVariance(DVECTOR.getVariance() * DVECTOR.getObsNormSquare() / obs2);
		DVECTOR.setObsNormSquare(obs2);
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
}
