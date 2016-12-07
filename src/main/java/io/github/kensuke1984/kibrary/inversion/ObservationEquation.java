package io.github.kensuke1984.kibrary.inversion;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.math.Matrix;
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

	/**
	 * @param partialIDs
	 *            for equation must contain waveform
	 * @param parameterList
	 *            for equation
	 * @param dVector
	 *            for equation
	 */
	public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector,
			boolean time_source, boolean time_receiver) {
		this.dVector = dVector;
		this.parameterList = parameterList;
		readA(partialIDs, time_receiver, time_source);
		atd = computeAtD(dVector.getD());
	}

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

	/**
	 * Am=dのAを作る まずmとdの情報から Aに必要な偏微分波形を決める。
	 * 
	 * @param ids
	 *            source for A
	 */
	private void readA(PartialID[] ids, boolean time_receiver, boolean time_source) {
		if (time_source)
			dVector.getUsedGlobalCMTIDset().forEach(id -> parameterList.add(new TimeSourceSideParameter(id)));
		if (time_receiver)
			dVector.getUsedStationSet().forEach(station -> parameterList.add(new TimeReceiverSideParameter(station)));
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
					id.getStation(), id.getGlobalCMTID());
			if (column < 0)
				return;
			// 偏微分係数id[i]が何番目のタイムウインドウにあるか
			int k = dVector.whichTimewindow(id);
			if (k < 0)
				return;
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
		if ( count.get() + count_TIMEPARTIAL_RECEIVER.get() + count_TIMEPARTIAL_SOURCE.get() != dVector.getNTimeWindow() * (numberOfParameterForSturcture + n) )
			throw new RuntimeException("Input partials are not enough: " + " " + count.get() + " + " +
					count_TIMEPARTIAL_RECEIVER.get() + " + " + count_TIMEPARTIAL_SOURCE.get() + " != " +
					dVector.getNTimeWindow() + " * (" + numberOfParameterForSturcture + " + 2)");  
		System.err.println("A is read and built in " + Utilities.toTimeString(System.nanoTime() - t));
	}

	/**
	 * @param param
	 *            to look for
	 * @return parameterが何番目にあるか なければ-1
	 */
	private int whatNumer(PartialType type, Location location, Station station, GlobalCMTID id) {
		for (int i = 0; i < parameterList.size(); i++) {
			if (parameterList.get(i).getPartialType() != type)
				continue;
			switch (type) {
			case TIME_SOURCE:
				if (id.equals( ((TimeSourceSideParameter) parameterList.get(i)).getGlobalCMTID() ))
					return i;
				break;
			case TIME_RECEIVER:
				if (station.equals( ((TimeReceiverSideParameter) parameterList.get(i)).getStation() ))
					return i;
				break;
			case PARA:
			case PARC:
			case PARF:
			case PARL:
			case PARN:
			case PARQ:
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

	public RealMatrix getAtA() {
		if (ata == null)
			synchronized (this) {
				if (ata == null)
					ata = a.computeAtA();
			}
		return ata;
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

	public List<UnknownParameter> getParameterList() {
		return parameterList;
	}

	public RealVector getAtD() {
		return atd;
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
