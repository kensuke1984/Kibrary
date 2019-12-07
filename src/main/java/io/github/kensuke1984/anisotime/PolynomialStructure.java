package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.dsminformation.TransverselyIsotropicParameter;
import io.github.kensuke1984.kibrary.math.LinearEquation;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.complex.Complex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

/**
 * Polynomial structure.
 * Outer-core must have a value of Q<sub>&mu;</sub> =-1
 *
 * @author Kensuke Konishi, Anselme Borgeaud
 * @version 0.1.1
 */
public class PolynomialStructure implements VelocityStructure {

    /**
     * @param r       [km] target radius
     * @param compute formulation for variable
     * @return the value at r. if the r is in D boundary range. the value is modified.
     */
    private double checkRInBoundaryAndSmooth(double r, DoubleUnaryOperator compute) {
        if (indexOfDBoundaryZone(r) < 0) return compute.applyAsDouble(r);
        double boundary = STRUCTURE.getRMinOf(STRUCTURE.zoneOf(r));
        double rLower = boundary - VelocityStructure.D_BOUNDARY_ZONE;
        double rUpper = boundary + VelocityStructure.D_BOUNDARY_ZONE;
        double vLower = compute.applyAsDouble(rLower);
        return vLower + (compute.applyAsDouble(rUpper) - vLower) / (rUpper - rLower) * (r - rLower);
    }


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
    private static final long serialVersionUID = 3585094774865301836L;

    private final io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure STRUCTURE;
    /*
     * -radius x this is only for computations for bouncing points.
     */
    private final PolynomialFunction RADIUS_SUBTRACTION;

    public PolynomialStructure(io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure structure) {
        STRUCTURE = structure;
        RADIUS_SUBTRACTION = new PolynomialFunction(new double[]{0, -earthRadius()});
        checkBoundaries();
    }

    private void checkBoundaries() {
        rMinIndexOfDBoundary =
                IntStream.range(1, STRUCTURE.getNzone()).filter(i -> isDBoundary(STRUCTURE.getRMinOf(i))).toArray();
    }

    /**
     * @param r [km] target radius
     * @return if the target radius is in a D boundary zone, returns the index (rmin) of the boundary, otherwise -1.
     */
    private int indexOfDBoundaryZone(double r) {
        return Arrays.stream(rMinIndexOfDBoundary).filter(i -> STRUCTURE.getRMinOf(i) - D_BOUNDARY_ZONE <= r &&
                r <= STRUCTURE.getRMinOf(i) + D_BOUNDARY_ZONE).findAny().orElse(-1);
    }

    /**
     * @param r radius to be checked
     * @return if the layer is D boundary.
     */
    private boolean isDBoundary(double r) {
        double rPlus = r + D_BOUNDARY_ZONE;
        double rMinus = r - D_BOUNDARY_ZONE;
        double criterion = 1 - MAXIMUM_RATIO_OF_D_BOUNDARY / 100;
        ToDoubleFunction<DoubleUnaryOperator> toRatio = compute -> {
            double ratio = compute.applyAsDouble(rPlus) / compute.applyAsDouble(rMinus);
            return ratio < 1 ? ratio : 1 / ratio;
        };
        Function<TransverselyIsotropicParameter, DoubleUnaryOperator> getOperater =
                ti -> a -> STRUCTURE.getTransverselyIsotropicValue(ti, a);
        return !(toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.A)) < criterion ||
                toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.C)) < criterion ||
                toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.F)) < criterion ||
                toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.L)) < criterion ||
                toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.N)) < criterion ||
                toRatio.applyAsDouble(STRUCTURE::getRhoAt) < criterion);
    }

    /**
     * index of D-Boundary for rmin
     */
    private int[] rMinIndexOfDBoundary;

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
        return STRUCTURE.getRMinOf(
                IntStream.range(0, STRUCTURE.getNzone()).filter(i -> STRUCTURE.getQMuOf(i) < 0).min().getAsInt());
    }

    @Override
    public double coreMantleBoundary() {
        return STRUCTURE.getRMaxOf(
                IntStream.range(0, STRUCTURE.getNzone()).filter(i -> STRUCTURE.getQMuOf(i) < 0).max().getAsInt());

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
        return checkRInBoundaryAndSmooth(r,
                a -> STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.A, a));
    }

    @Override
    public double getC(double r) {
        return checkRInBoundaryAndSmooth(r,
                a -> STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.C, a));
    }

    @Override
    public double getF(double r) {
        return checkRInBoundaryAndSmooth(r,
                a -> STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.F, a));
    }

    @Override
    public double getL(double r) {
        return checkRInBoundaryAndSmooth(r,
                a -> STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.L, a));
    }

    @Override
    public double getN(double r) {
        return checkRInBoundaryAndSmooth(r,
                a -> STRUCTURE.getTransverselyIsotropicValue(TransverselyIsotropicParameter.N, a));
    }

}
