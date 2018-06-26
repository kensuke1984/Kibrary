package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.inversion.montecarlo.DataGenerator;
import io.github.kensuke1984.kibrary.math.Matrix;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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

    private final List<UnknownParameter> PARAMETER_LIST;
    private final Dvector DVECTOR;
    private final DataGenerator<RealVector, RealVector[]> BORN_GENERATOR;
    private final DataGenerator<RealVector, Double> VARIANCE_GENERATOR;
    private Matrix a;
    private RealVector atd;
    private RealMatrix ata;

    /**
     * @param partialIDs    for A
     * @param parameterList for &delta;m
     * @param dVector       for &delta;d
     */
    public ObservationEquation(PartialID[] partialIDs, List<UnknownParameter> parameterList, Dvector dVector) {
        DVECTOR = dVector;
        PARAMETER_LIST = Collections.unmodifiableList(parameterList);
        readA(partialIDs);
        atd = RealVector.unmodifiableRealVector(computeAtD(dVector.getD()));
        BORN_GENERATOR = model -> DVECTOR.separate(operate(model).add(dVector.getSyn()));
        VARIANCE_GENERATOR = this::varianceOf;
    }

    public int getDlength() {
        return DVECTOR.getNpts();
    }

    /**
     * @param m 解のベクトル
     * @return Amをsyntheticに足したd列 順番はDvectorと同じ
     */
    public RealVector[] bornOut(RealVector m) {
        RealVector[] am = DVECTOR.separate(operate(m));
        RealVector[] syn = DVECTOR.getSynVectors();
        RealVector[] born = new ArrayRealVector[DVECTOR.getNTimeWindow()];
        Arrays.setAll(born, i -> syn[i].add(am[i]));
        return born;
    }

    /**
     * TODO
     * Finite frequency kernel of time correlation with respect to the input.
     *
     * @param id        of the kernel.
     * @param parameter for K.
     * @return K<sub>p</sub><sup>T</sup>.
     */
    public double computeTraveltimeKernel(BasicID id, UnknownParameter parameter) {
        RealVector jt = DVECTOR.computeJtime(id);
        RealVector kUt = getPartialOf(id, parameter);
        return jt.dotProduct(kUt) / id.getSamplingHz();
    }

    /**
     * TODO
     * Finite frequency kernel of amplitude with respect to the parameter.
     *
     * @param id        of the kernel.
     * @param parameter for K.
     * @return K<sub>p</sub><sup>lnA</sup>.
     */
    public double computeAmplitudeKernel(BasicID id, UnknownParameter parameter) {
        RealVector jlnA = DVECTOR.computeJlnA(id);
        RealVector kUt = getPartialOf(id, parameter);
        return jlnA.dotProduct(kUt) / id.getSamplingHz();
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
        double obs2 = DVECTOR.getObsNorm() * DVECTOR.getObsNorm();
        double variance =
                DVECTOR.getDNorm() * DVECTOR.getDNorm() - 2 * atd.dotProduct(m) + m.dotProduct(getAtA().operate(m));
        return variance / obs2;
    }

    /**
     * Am=dのAを作る まずmとdの情報から Aに必要な偏微分波形を決める。
     *
     * @param ids source for A
     */
    private void readA(PartialID[] ids) {
        a = new Matrix(DVECTOR.getNpts(), PARAMETER_LIST.size());
        // partialDataFile.readWaveform();
        long t = System.nanoTime();
        AtomicInteger count = new AtomicInteger();
        Arrays.stream(ids).parallel().forEach(id -> {
            if (count.get() == DVECTOR.getNTimeWindow() * PARAMETER_LIST.size()) return;
            int column = whatNumber(id.getPartialType(), id.getPerturbationLocation());
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
     * @param type     to look for
     * @param location to look for
     * @return i, m<sub>i</sub> = type, parameterが何番目にあるか なければ-1
     */
    private int whatNumber(PartialType type, Location location) {
        for (int i = 0; i < PARAMETER_LIST.size(); i++) {
            if (PARAMETER_LIST.get(i).getPartialType() != type) continue;
            switch (type) {
                case TIME:
                    throw new RuntimeException("time  madamuripo");
                case PARA:
                case PARC:
                case PARF:
                case PARL:
                case PARN:
                case PARQ:
                case PAR1:
                case PAR2:
                    if (location.getR() == ((Physical1DParameter) PARAMETER_LIST.get(i)).getPerturbationR()) return i;
                    break;
                case A:
                case C:
                case F:
                case L:
                case N:
                case Q:
                case MU:
                case LAMBDA:
                    if (location.equals(((Physical3DParameter) PARAMETER_LIST.get(i)).getPointLocation())) return i;
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
        if (ata == null) synchronized (this) {
            if (ata == null) ata = a.computeAtA();
        }
        return ata;
    }

    /**
     * @param parameter target unknown parameter
     * @return the column of A for the parameter
     */
    public RealVector getPartialOf(UnknownParameter parameter) {
        Location location;
        if (parameter instanceof Physical1DParameter)
            location = new Location(0, 0, ((Physical1DParameter) parameter).getPerturbationR());
        else if (parameter instanceof Physical3DParameter)
            location = ((Physical3DParameter) parameter).getPointLocation();
        else throw new RuntimeException("UNEXPECTED");
        return a.getColumnVector(whatNumber(parameter.getPartialType(), location));
    }

    /**
     * @param basicID   target ID
     * @param parameter target parameter
     * @return timewindow of partial derivative for the basicID with respect to the parameter.
     */
    public RealVector getPartialOf(BasicID basicID, UnknownParameter parameter) {
        return DVECTOR.separate(getPartialOf(parameter))[DVECTOR.whichTimewindow(basicID)];
    }

    /**
     * Aを書く それぞれのpartialごとに分けて出す debug用？
     *
     * @param outputPath {@link Path} for an write folder
     */
    void outputA(Path outputPath) throws IOException {
        if (a == null) {
            System.err.println("no more A");
            return;
        }
        if (Files.exists(outputPath)) throw new FileAlreadyExistsException(outputPath.toString());
        Files.createDirectories(outputPath);
        BasicID[] ids = DVECTOR.getSynIDs();
        IntStream.range(0, ids.length).forEach(i -> {
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
    
    /**
     * AtAを書く それぞれのpartialごとに分けて出す debug用？
     *
     * @param outputPath {@link Path} for an write folder
     */
    void outputAtA(Path AtAPath) throws IOException {
        if (a == null) {
            System.err.println("no more A");
            return;
        }
        if (Files.exists(AtAPath)) throw new FileAlreadyExistsException(AtAPath.toString());
        Files.createDirectories(AtAPath);
        System.out.println("AtAPath is " + AtAPath);
        File newfile = new File(AtAPath.resolve("AtA.dat").toString());
        newfile.createNewFile();
        ata = a.computeAtA();
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(newfile)))){
        	System.out.println(PARAMETER_LIST.size());
			for (int i = 0; i < PARAMETER_LIST.size(); i++) {
//				for (int j = 0; j <= i; j++) {
					pw.println(PARAMETER_LIST.get(i) + " " + PARAMETER_LIST.get(i) + " " + ata.getEntry(i, i));
//				}
			}
		} catch (IOException e) {
			e.printStackTrace();
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

    public List<UnknownParameter> getparameterList() {
        return PARAMETER_LIST;
    }

    public RealVector getAtD() {
        return atd;
    }

    public Dvector getDVector() {
        return DVECTOR;
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
    public DataGenerator<RealVector, RealVector[]> getBornGenerator() {
        return BORN_GENERATOR;
    }

    public DataGenerator<RealVector, Double> getVarianceGenerator() {
        return VARIANCE_GENERATOR;
    }

}
