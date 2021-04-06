package io.github.kensuke1984.anisotime;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Named discontinuity structure
 *
 * @author Kensuke Konishi
 * @version 0.1.2
 */
class NamedDiscontinuityStructure implements VelocityStructure, Serializable {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NamedDiscontinuityStructure)) return false;
        NamedDiscontinuityStructure that = (NamedDiscontinuityStructure) o;
        return structure.equals(that.structure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(structure);
    }

    /**
     * 2020/8/12
     */
    private static final long serialVersionUID = -67293605910714390L;
    io.github.kensuke1984.kibrary.util.NamedDiscontinuityStructure structure;

    private NamedDiscontinuityStructure() {
    }

    NamedDiscontinuityStructure(Path path) throws IOException {
        structure = new io.github.kensuke1984.kibrary.util.NamedDiscontinuityStructure(path);
        if (!checkStructure())
        	throw new RuntimeException(
        			"The structure must have strictly positive velocity and density,"
        			+ "except for vsh=vsv=0 in the outer-core.");
    }

    public static NamedDiscontinuityStructure prem() {
        NamedDiscontinuityStructure nd = new NamedDiscontinuityStructure();
        nd.structure = io.github.kensuke1984.kibrary.util.NamedDiscontinuityStructure.prem();
        return nd;
    }
    
    /**
     * Check if vpv, vph, vsv, vsh, and rho are strictly positive over all depths,
     * excepts for the outer core for vsh and vsv, where it checks if vsh=vsv=0
     * @return true if check passed, else false
     */
    private boolean checkStructure() {
    	if (IntStream.range(0, (int) earthRadius()).filter(r -> r >= coreMantleBoundary() && r < innerCoreBoundary())
    			.mapToDouble(structure::getVsh).anyMatch(v -> v <= 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius()).filter(r -> r < coreMantleBoundary() && r > innerCoreBoundary())
    				.mapToDouble(structure::getVsh).anyMatch(v -> v != 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius()).filter(r -> r >= coreMantleBoundary() && r < innerCoreBoundary())
        			.mapToDouble(structure::getVsv).anyMatch(v -> v <= 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius()).filter(r -> r < coreMantleBoundary() && r > innerCoreBoundary())
    			.mapToDouble(structure::getVsv).anyMatch(v -> v != 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius())
        			.mapToDouble(structure::getVph).anyMatch(v -> v <= 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius())
    				.mapToDouble(structure::getVpv).anyMatch(v -> v <= 0 )) return false;
    	else if (IntStream.range(0, (int) earthRadius())
    				.mapToDouble(structure::getRho).anyMatch(v -> v <= 0 )) return false;
    	return true;
    }

    @Override
    public double[] velocityBoundaries() {
        double[] boundaries = new double[structure.getNzone() + 1];
        for (int i = 0; i < boundaries.length; i++)
            boundaries[i] = structure.getBoundary(i);
        return Arrays.stream(boundaries).distinct().toArray();
    }

    /**
     * @param rayParameter for the raypath
     * @param a            a of the Bullen law
     * @param b            b of the Bullen law
     * @return r [km] for the bounce point
     */
    private static double computeTurningR(double rayParameter, double a, double b) {
        return Math.pow(1 / (a * rayParameter), 1 / (b - 1));
    }


    @Override
    public double shTurningR(double rayParameter) {
        for (int i = structure.getNzone() - 1; 0 <= i; i--) {
            double r = computeTurningR(rayParameter, structure.getVshA(i), structure.getVshB(i));
            if (coreMantleBoundary() <= r && structure.getBoundary(i) <= r && r <= structure.getBoundary(i + 1))
                return r;
        }
        return Double.NaN;
    }

    @Override
    public double jhTurningR(double rayParameter) {
        for (int i = structure.getNzone() - 1; 0 <= i; i--) {
            double r = computeTurningR(rayParameter, structure.getVshA(i), structure.getVshB(i));
            if (r <= innerCoreBoundary() && structure.getBoundary(i) <= r && r <= structure.getBoundary(i + 1))
                return r;
        }
        return Double.NaN;
    }

    @Override
    public double jvTurningR(double rayParameter) {
        for (int i = structure.getNzone() - 1; 0 <= i; i--) {
            double r = computeTurningR(rayParameter, structure.getVsvA(i), structure.getVsvB(i));
            if (r <= innerCoreBoundary() && structure.getBoundary(i) <= r && r <= structure.getBoundary(i + 1))
                return r;
        }
        return Double.NaN;
    }

    @Override
    public double svTurningR(double rayParameter) {
        for (int i = structure.getNzone() - 1; 0 <= i; i--) {
            double r = computeTurningR(rayParameter, structure.getVsvA(i), structure.getVsvB(i));
            if (coreMantleBoundary() <= r && structure.getBoundary(i) <= r && r <= structure.getBoundary(i + 1))
                return r;
        }
        return Double.NaN;
    }

    @Override
    public double pTurningR(double rayParameter) {
        for (int i = structure.getNzone() - 1; 0 <= i; i--) {
            double r = computeTurningR(rayParameter, structure.getVphA(i), structure.getVphB(i));
            if (coreMantleBoundary() <= r && structure.getBoundary(i) <= r && r < structure.getBoundary(i + 1))
                return r;
        }
        return Double.NaN;
    }

    @Override
    public double iTurningR(double rayParameter) {
        for (int i = structure.getNzone() - 1; 0 <= i; i--) {
            double r = computeTurningR(rayParameter, structure.getVphA(i), structure.getVphB(i));
            if (r <= innerCoreBoundary() && structure.getBoundary(i) <= r && r < structure.getBoundary(i + 1)) return r;
        }
        return Double.NaN;
    }

    @Override
    public double getA(double r) {
        double v = structure.getVph(r);
        return v * v * getRho(r);
    }

    @Override
    public double getC(double r) {
        double v = structure.getVpv(r);
        return v * v * getRho(r);
    }

    @Override
    public double getF(double r) {
        return structure.getEta(r) * (getA(r) - 2 * getL(r));
    }

    @Override
    public double getL(double r) {
        double v = structure.getVsv(r);
        return v * v * getRho(r);
    }

    @Override
    public double getN(double r) {
        double v = structure.getVsh(r);
        return v * v * getRho(r);
    }

    @Override
    public double innerCoreBoundary() {
        return structure.innerCoreBoundary();
    }

    @Override
    public double coreMantleBoundary() {
        return structure.coreMantleBoundary();
    }

    @Override
    public double earthRadius() {
        return structure.getBoundary(structure.getNzone());
    }

    @Override
    public double getRho(double r) {
        return structure.getRho(r);
    }

    @Override
    public double kTurningR(double rayParameter) {
        for (int i = structure.getNzone() - 1; 0 <= i; i--) {
            double r = computeTurningR(rayParameter, structure.getVphA(i), structure.getVphB(i));
            if (structure.getBoundary(i) <= r && r < structure.getBoundary(i + 1) && r < coreMantleBoundary() &&
                    innerCoreBoundary() < r) return r;
        }
        return Double.NaN;
    }

}
