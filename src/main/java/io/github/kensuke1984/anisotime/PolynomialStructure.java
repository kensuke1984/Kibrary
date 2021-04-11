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
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

/**
 * Polynomial structure.
 * Outer-core must have a value of Q<sub>&mu;</sub> =-1
 *
 * @author Kensuke Konishi, Anselme Borgeaud
 * @version 0.1.3
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
     * 2019/12/7
     */
    private static final long serialVersionUID = -7292410325252292009L;

    private final io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure STRUCTURE;
    /*
     * -radius x this is only for computations for bouncing points.
     */
    private final PolynomialFunction RADIUS_SUBTRACTION;

    public PolynomialStructure(io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure structure) {
        STRUCTURE = checkBoundaries(structure);
        if (!STRUCTURE.isDefault() && !checkStructure())
        	throw new RuntimeException(
        			"The structure must have strictly positive velocity and density,"
        			+ "except for vsh=vsv=0 in the outer-core.");
        RADIUS_SUBTRACTION = new PolynomialFunction(new double[]{0, -earthRadius()});
    }

    private io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure checkBoundaries(
            io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure structure) {
        double[] dBoundaries = IntStream.range(1, structure.getNzone()).mapToDouble(structure::getRMinOf)
                .filter(r -> isDBoundary(r, structure)).toArray();
        io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure newStructure = structure;
        double earthRadius = structure.getRMaxOf(structure.getNzone() - 1);
        for (double boundary : dBoundaries) {
            newStructure = newStructure.addBoundaries(boundary - D_BOUNDARY_ZONE, boundary + D_BOUNDARY_ZONE);
            newStructure = newStructure.mergeLayer(newStructure.zoneOf(boundary));
            int izone = newStructure.zoneOf(boundary);
            //rho
            newStructure = newStructure.setRho(izone,
                    computeReplacement(boundary, earthRadius, newStructure.getRhoOf(izone),
                            newStructure.getRhoOf(izone + 1)));
            //Vpv
            newStructure = newStructure.setVpv(izone,
                    computeReplacement(boundary, earthRadius, newStructure.getVpvOf(izone),
                            newStructure.getVpvOf(izone + 1)));
            //Vph
            newStructure = newStructure.setVph(izone,
                    computeReplacement(boundary, earthRadius, newStructure.getVphOf(izone),
                            newStructure.getVphOf(izone + 1)));
            //Vsv
            newStructure = newStructure.setVsv(izone,
                    computeReplacement(boundary, earthRadius, newStructure.getVsvOf(izone),
                            newStructure.getVsvOf(izone + 1)));
            //Vsh
            newStructure = newStructure.setVsh(izone,
                    computeReplacement(boundary, earthRadius, newStructure.getVshOf(izone),
                            newStructure.getVshOf(izone + 1)));
        }
        return newStructure;
    }
    
    /**
     * Check if vpv, vph, vsv, vsh, and rho are strictly positive over all depths,
     * excepts for the outer core for vsh and vsv, where it checks if vsh=vsv=0
     * @return true if check passed, else false
     */
    private boolean checkStructure() {
    	if (IntStream.range(0, (int) earthRadius()).filter(r -> r >= coreMantleBoundary() && r < innerCoreBoundary())
    			.mapToDouble(STRUCTURE::getVshAt).anyMatch(v -> v <= 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius()).filter(r -> r < coreMantleBoundary() && r > innerCoreBoundary())
    				.mapToDouble(STRUCTURE::getVshAt).anyMatch(v -> v != 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius()).filter(r -> r >= coreMantleBoundary() && r < innerCoreBoundary())
        			.mapToDouble(STRUCTURE::getVsvAt).anyMatch(v -> v <= 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius()).filter(r -> r < coreMantleBoundary() && r > innerCoreBoundary())
    			.mapToDouble(STRUCTURE::getVsvAt).anyMatch(v -> v != 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius())
        			.mapToDouble(STRUCTURE::getVphAt).anyMatch(v -> v <= 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius())
    				.mapToDouble(STRUCTURE::getVpvAt).anyMatch(v -> v <= 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius())
    				.mapToDouble(STRUCTURE::getRhoAt).anyMatch(v -> v <= 0 )) return false;
    	return true;
    }

    /**
     * TODO
     *
     * @param boundary      [km] radius of boundary
     * @param earthRadius   [km] earth radius
     * @param lowerFunction polynomial function at the layer beneath the boundary
     * @param upperFunction polynomial function at the layer above the boundary
     * @return function to be replaced with the upper one
     */
    private PolynomialFunction computeReplacement(double boundary, double earthRadius, PolynomialFunction lowerFunction,
                                                  PolynomialFunction upperFunction) {
        double xLower = (boundary - D_BOUNDARY_ZONE) / earthRadius;
        double xBoundary = boundary / earthRadius;
        double boundaryValue = lowerFunction.value(xBoundary);
        double lowerValue = lowerFunction.value(xLower);
        double xUpper = (boundary + D_BOUNDARY_ZONE) / earthRadius;
        double upperValue = upperFunction.value(xUpper);
        double a = (upperValue - lowerValue) / (xUpper - xLower);
        double b = upperValue - a * xUpper;
        return new PolynomialFunction(new double[]{b, a});
    }

    /**
     * @param r         radius to be checked
     * @param structure structure to be checked
     * @return if the boundary is D boundary.
     * If the functions and values(velocities and density) in the upper and lower boundaries are identical, false returns.
     */
    private static boolean isDBoundary(double r,
                                       io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure structure) {
        double rPlus = r + ComputationalMesh.EPS;
        double rMinus = r - ComputationalMesh.EPS;
        int upperZone = structure.zoneOf(rPlus);
        int lowerZone = structure.zoneOf(rMinus);
        Function<IntFunction<PolynomialFunction>, Boolean> compare =
                ic -> ic.apply(upperZone).equals(ic.apply(lowerZone));
        if (compare.apply(structure::getVphOf) && compare.apply(structure::getVpvOf) &&
                compare.apply(structure::getVshOf) && compare.apply(structure::getVsvOf) &&
                compare.apply(structure::getRhoOf)) return false;
        double criterion = 1 - MAXIMUM_RATIO_OF_D_BOUNDARY / 100;
        ToDoubleFunction<DoubleUnaryOperator> toRatio = compute -> {
            double ratio = compute.applyAsDouble(rPlus) / compute.applyAsDouble(rMinus);
            return ratio < 1 ? ratio : 1 / ratio;
        };
        Function<TransverselyIsotropicParameter, DoubleUnaryOperator> getOperater =
                ti -> a -> structure.getTransverselyIsotropicValue(ti, a);
        return !(toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.A)) < criterion ||
                toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.C)) < criterion ||
                toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.F)) < criterion ||
                toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.L)) < criterion ||
                toRatio.applyAsDouble(getOperater.apply(TransverselyIsotropicParameter.N)) < criterion ||
                toRatio.applyAsDouble(structure::getRhoAt) < criterion);
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
        if (STRUCTURE == null) return other.STRUCTURE == null;
        else return STRUCTURE.equals(other.STRUCTURE);
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
            if (innerCoreBoundary() < STRUCTURE.getRMinOf(i)) continue;
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
            if (innerCoreBoundary() < STRUCTURE.getRMinOf(i)) continue;
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
            if (coreMantleBoundary() < STRUCTURE.getRMinOf(i)) continue;
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
