package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.dsminformation.TransverselyIsotropicParameter;
import io.github.kensuke1984.kibrary.math.LinearEquation;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.complex.Complex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Polynomial structure.
 *
 * @author Kensuke Konishi, Anselme Borgeaud
 * @version 0.0.10.2
 */
public class PolynomialStructure implements VelocityStructure {

    /**
     * Transversely isotropic (TI) PREM by Dziewonski &amp; Anderson 1981
     */
    public static final PolynomialStructure PREM =
            new PolynomialStructure(io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.PREM);
    /**
     * isotropic PREM by Dziewonski &amp; Anderson 1981
     */
    public static final PolynomialStructure ISO_PREM =
            new PolynomialStructure(io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.ISO_PREM);
    /**
     * AK135 by Kennett <i>et al</i>. (1995)
     */
    public static final PolynomialStructure AK135 =
            new PolynomialStructure(io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.AK135);
    /**
     * Homogeneous structure used for test purposes
     */
    public static final PolynomialStructure HOMOGEN =
            new PolynomialStructure(io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.HOMOGEN);
    /**
     * 2019/11/9
     */
    private static final long serialVersionUID = -247740073015527700L;

    private final io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure STRUCTURE;
    /*
     * -radius x this is only for computations for bouncing points.
     */
    private final PolynomialFunction RADIUS_SUBTRACTION;

    public PolynomialStructure(io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure structure) {
        STRUCTURE = structure;
        RADIUS_SUBTRACTION = new PolynomialFunction(new double[]{0, -earthRadius()});
    }

    public PolynomialStructure(Path path) throws IOException {
        this(new io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure(path));
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + ((STRUCTURE == null) ? 0 : STRUCTURE.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PolynomialStructure other = (PolynomialStructure) obj;
        if (STRUCTURE == null) {
            if (other.STRUCTURE != null) return false;
        } else if (!STRUCTURE.equals(other.STRUCTURE)) return false;
        return true;
    }

    /**
     * @param i  zone number for the search
     * @param eq equation to solve
     * @return [km] turningR in the i-th zone or -1 if no valid R in the i-th zone
     */
    private double findTurningR(int i, LinearEquation eq) {
        double minR = STRUCTURE.getRMinOf(i);
        double maxR = STRUCTURE.getRMaxOf(i);
        int ansType = eq.Discriminant();
        Complex[] answer = eq.compute();
        if (ansType == 1) {
            double radius = answer[0].getReal() * earthRadius();
            return minR <= radius && radius < maxR ? radius : -1;
        }

        if (ansType < 19) return -1;

        if (ansType == 20 || ansType == 28 || ansType == 29 || ansType == 30) {
            double radius = answer[0].getReal() * earthRadius();
            return minR <= radius && radius < maxR ? radius : -1;
        }

        return Arrays.stream(answer).map(a -> a.getReal() * earthRadius()).sorted(Comparator.reverseOrder())
                .filter(x -> minR <= x && x < maxR).findFirst().orElse(-1d);
    }

    @Override
    public double earthRadius() {
        return STRUCTURE.getRMaxOf(STRUCTURE.getNzone() - 1);
    }

    @Override
    public double innerCoreBoundary() {
        return 1221.5; // TODO
    }

    @Override
    public double coreMantleBoundary() {
        return STRUCTURE.getRMinOf(STRUCTURE.getCoreZone());
    }

    @Override
    public double[] velocityBoundaries() {
        double[] boundaries = new double[STRUCTURE.getNzone() + 1];
        for (int i = 0; i < boundaries.length - 1; i++)
            boundaries[i] = STRUCTURE.getRMinOf(i);
        boundaries[boundaries.length - 1] = earthRadius();
        return boundaries;
    }

    @Override
    public double pTurningR(double p) {
        PolynomialFunction pFunction = new PolynomialFunction(new double[]{p});
        for (int i = STRUCTURE.getNzone() - 1; -1 < i; i--) {
            PolynomialFunction pvr = STRUCTURE.getVphOf(i).multiply(pFunction).add(RADIUS_SUBTRACTION); // pv-r=0
            LinearEquation eq = new LinearEquation(pvr);
            double r = findTurningR(i, eq);
            if (coreMantleBoundary() <= r) return r;
        }
        return Double.NaN;
    }

    @Override
    public double iTurningR(double p) {
        PolynomialFunction pFunction = new PolynomialFunction(new double[]{p});
        for (int i = STRUCTURE.getNzone() - 1; -1 < i; i--) {
            PolynomialFunction pvr = STRUCTURE.getVphOf(i).multiply(pFunction).add(RADIUS_SUBTRACTION); // pv-r=0
            LinearEquation eq = new LinearEquation(pvr);
            double r = findTurningR(i, eq);
            if (0 <= r && r <= innerCoreBoundary()) return r;
        }
        return Double.NaN;
    }

    @Override
    public double svTurningR(double p) {
        PolynomialFunction pFunction = new PolynomialFunction(new double[]{p});
        for (int i = STRUCTURE.getNzone() - 1; i > -1; i--) {
            PolynomialFunction pvr = STRUCTURE.getVsvOf(i).multiply(pFunction).add(RADIUS_SUBTRACTION); // pv-r=0
            LinearEquation eq = new LinearEquation(pvr);
            double r = findTurningR(i, eq);
            if (coreMantleBoundary() <= r) return r;
        }
        return Double.NaN;
    }

    @Override
    public double shTurningR(double p) {
        PolynomialFunction pFunction = new PolynomialFunction(new double[]{p});
        for (int i = STRUCTURE.getNzone() - 1; -1 < i; i--) {
            PolynomialFunction pvr = STRUCTURE.getVshOf(i).multiply(pFunction).add(RADIUS_SUBTRACTION); // pv-r=0
            LinearEquation eq = new LinearEquation(pvr);
            double r = findTurningR(i, eq);
            if (coreMantleBoundary() <= r) return r;
        }
        return Double.NaN;
    }

    @Override
    public double jvTurningR(double p) {
        PolynomialFunction pFunction = new PolynomialFunction(new double[]{p});
        for (int i = STRUCTURE.getNzone() - 1; i > -1; i--) {
            PolynomialFunction pvr = STRUCTURE.getVsvOf(i).multiply(pFunction).add(RADIUS_SUBTRACTION); // pv-r=0
            LinearEquation eq = new LinearEquation(pvr);
            double r = findTurningR(i, eq);
            if (0 <= r && r <= innerCoreBoundary()) return r;
        }
        return Double.NaN;
    }

    @Override
    public double jhTurningR(double p) {
        PolynomialFunction pFunction = new PolynomialFunction(new double[]{p});
        for (int i = STRUCTURE.getNzone() - 1; -1 < i; i--) {
            PolynomialFunction pvr = STRUCTURE.getVshOf(i).multiply(pFunction).add(RADIUS_SUBTRACTION); // pv-r=0
            LinearEquation eq = new LinearEquation(pvr);
            double r = findTurningR(i, eq);
            if (0 <= r && r <= innerCoreBoundary()) return r;
        }
        return Double.NaN;
    }

    @Override
    public double kTurningR(double p) {
        PolynomialFunction pFunction = new PolynomialFunction(new double[]{p});
        for (int i = STRUCTURE.getNzone() - 1; -1 < i; i--) {
            PolynomialFunction pvr = STRUCTURE.getVphOf(i).multiply(pFunction).add(RADIUS_SUBTRACTION); // pv-r=0
            LinearEquation eq = new LinearEquation(pvr);
            double r = findTurningR(i, eq);
            if (innerCoreBoundary() < r && r < coreMantleBoundary()) return r;
        }
        return Double.NaN;
    }

    @Override
    public double getRho(double r) {
        return STRUCTURE.getRhoAt(r);
    }

    @Override
    public double getA(double r) {
        return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.A, r);
    }

    @Override
    public double getC(double r) {
        return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.C, r);
    }

    @Override
    public double getF(double r) {
        return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.F, r);
    }

    @Override
    public double getL(double r) {
        return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.L, r);
    }

    @Override
    public double getN(double r) {
        return STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.N, r);
    }

}
