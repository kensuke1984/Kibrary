package io.github.kensuke1984.anisotime;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * Raypath catalogue for one model
 * <p>
 * TODO boundaries close points EPS TODO default catalogue in .Kibrary
 * <p>
 * If a new catalog is computed which does not exist in Kibrary share, it
 * automatically is stored.
 *
 * @author Kensuke Konishi
 * @version 0.0.8.1b
 */
public class RaypathCatalog implements Serializable {

    /**
     * 2016/12/3
     */
    private static final long serialVersionUID = -5169958584689352786L;

    static final Path share = Paths.get(System.getProperty("user.home") + "/.Kibrary/share");

    /**
     * Woodhouse formula with certain velocity structure
     */
    private final Woodhouse1981 WOODHOUSE;

    /**
     * Catalog for PREM. &delta;&Delta; = 1. Mesh is simple.
     */
    public final static RaypathCatalog PREM;

    /**
     * Catalog for the isotropic PREM. &delta;&Delta; = 1. Mesh is simple.
     */
    public final static RaypathCatalog ISO_PREM;

    /**
     * Catalog for AK135. &delta;&Delta; = 1. Mesh is simple.
     */
    public final static RaypathCatalog AK135;

    static {
        try {
            BiFunction<Path, VelocityStructure, RaypathCatalog> getCatalogue = (p, v) -> {
                RaypathCatalog cat;
                String model = p.getFileName().toString().replace(".cat", "");
                ComputationalMesh simple = ComputationalMesh.simple(v);
                if (!Files.exists(p)) {
                    System.err.println("Creating a catalog for " + model + ". This computation is done only once.");
                    cat = new RaypathCatalog(v, simple, Math.toRadians(1));
                    cat.create();
                    try {
                        cat.write(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        cat = read(p);
                    } catch (ClassNotFoundException | IOException ice) {
                        System.err.println("Creating a catalog for " + model +
                                " (due to out of date).  This computation is done only once.");
                        cat = new RaypathCatalog(v, simple, Math.toRadians(1));
                        cat.create();
                        try {
                            cat.write(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return cat;
            };
            Files.createDirectories(share);
            PREM = getCatalogue.apply(share.resolve("prem.cat"), VelocityStructure.prem());
            ISO_PREM = getCatalogue.apply(share.resolve("iprem.cat"), VelocityStructure.iprem());
            AK135 = getCatalogue.apply(share.resolve("ak135.cat"), VelocityStructure.ak135());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Woodhouse1981 used in the catalog.
     */
    Woodhouse1981 getWoodhouse1981() {
        return WOODHOUSE;
    }

    /**
     * @return Velocity structure used in the catalog.
     */
    public VelocityStructure getStructure() {
        return WOODHOUSE.getStructure();
    }

    /**
     * @return Raypaths in a catalog in order by the p (ray parameter).
     */
    public Raypath[] getRaypaths() {
        return raypathList.toArray(new Raypath[raypathList.size()]);
    }

    /**
     * This value is in [rad].
     * <p>
     * We compute epicentral distances &Delta;<sup>(P)</sup><sub>i</sub> (P or
     * PcP) and &Delta;<sup>(S)</sup><sub>i</sub> (S or ScS) for ray parameters
     * p<sub>i</sub> (p<sub>i</sub> &lt; p<sub>i+1</sub>) for a catalogue. If
     * &delta;&Delta;<sub>i</sub> (|&Delta;<sub>i</sub> - &Delta;<sub>i</sub>|)
     * &lt; this value, both p<sub>i</sub> and p<sub>i+1</sub> are stored,
     * otherwise either only one of them is stored.
     */
    private final double D_DELTA;

    /**
     * Standard &delta;p (ray parameter). In case the &delta;p is too big to
     * have &Delta; satisfying {@link #D_DELTA}, another value (2.5, 1.25) is
     * used instantly.
     */
    private final double DELTA_P = 5;

    /**
     * Minimum value of &delta;p (ray parameter). Even if similar raypaths
     * satisfying {@link #D_DELTA} are not found within this value, a catalogue
     * does not have a denser ray parameter than the value.
     */
    final double MINIMUM_DELTA_P = 0.01;

    /**
     * List of stored raypaths. Ordered by each ray parameter p.
     */
    private final TreeSet<Raypath> raypathList = new TreeSet<>();

    /**
     * Mesh for computation
     */
    private final ComputationalMesh MESH;

    /**
     * @return {@link ComputationalMesh} used in the catalog.
     */
    ComputationalMesh getMesh() {
        return MESH;
    }

    /**
     * We compute epicentral distances &Delta;<sup>(P)</sup><sub>i</sub> (P or
     * PcP) and &Delta;<sup>(S)</sup><sub>i</sub> (S or ScS) for ray parameters
     * p<sub>i</sub> (p<sub>i</sub> &lt; p<sub>i+1</sub>) for a catalogue.<br>
     * If &delta;&Delta;<sub>i</sub> (|&Delta;<sub>i</sub> -
     * &Delta;<sub>i</sub>|) &lt; this value, both p<sub>i</sub> and
     * p<sub>i+1</sub> are stored, otherwise either only one of them is stored.
     * <p>
     * Note that if a catalog for the input parameter already exists in
     * KibraryHOME/share, the stored catalog returns.
     *
     * @param structure for computation of raypaths
     * @param mesh      for computation of raypaths.
     * @param dDelta    &delta;&Delta; [rad] for creation of a catalog.
     */
    public static RaypathCatalog computeCatalogue(VelocityStructure structure, ComputationalMesh mesh, double dDelta) {
        try (DirectoryStream<Path> catalogStream = Files.newDirectoryStream(share, "*.cat")) {
            for (Path p : catalogStream)
                try {
                    RaypathCatalog c = read(p);
                    if (c.getStructure().equals(structure) && c.MESH.equals(mesh) && c.D_DELTA == dDelta) return c;
                } catch (InvalidClassException ice) {
                    System.err.println(p + " may be out of date.");
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        RaypathCatalog cat = new RaypathCatalog(structure, mesh, dDelta);
        cat.create();
        try {
            Path p = Files.createTempFile(share, "raypath", ".cat");
            cat.write(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cat;
    }

    /**
     * We compute epicentral distances &Delta;<sup>(P)</sup><sub>i</sub> (P or
     * PcP) and &Delta;<sup>(S)</sup><sub>i</sub> (S or ScS) for ray parameters
     * p<sub>i</sub> (p<sub>i</sub> &lt; p<sub>i+1</sub>) for a catalogue.<br>
     * If &delta;&Delta;<sub>i</sub> (|&Delta;<sub>i</sub> -
     * &Delta;<sub>i</sub>|) &lt; this value, both p<sub>i</sub> and
     * p<sub>i+1</sub> are stored, otherwise either only one of them is stored.
     *
     * @param structure for computation of raypaths
     * @param mesh      for computation of raypaths.
     * @param dDelta    &delta;&Delta; [rad] for creation of a catalog.
     */
    private RaypathCatalog(VelocityStructure structure, ComputationalMesh mesh, double dDelta) {
        WOODHOUSE = new Woodhouse1981(structure);
        if (dDelta <= 0) throw new IllegalArgumentException("Input dDelta must be positive.");
        D_DELTA = dDelta;
        MESH = mesh;
    }

    /**
     * Raypath of Pdiff
     */
    private Raypath pDiff;

    /**
     * Raypath of SVdiff
     */
    private Raypath svDiff;

    /**
     * Raypath of SHdiff
     */
    private Raypath shDiff;

    /**
     * TODO boundaries Computes ray parameters of diffraction phases (Pdiff and
     * Sdiff).
     */
    private void computeDiffraction() {
        VelocityStructure structure = WOODHOUSE.getStructure();
        double cmb = structure.coreMantleBoundary() + ComputationalMesh.eps;
        double rho = structure.getRho(cmb);
        double p_Pdiff = cmb * Math.sqrt(rho / structure.getA(cmb));
        double p_SVdiff = cmb * Math.sqrt(rho / structure.getL(cmb));
        double p_SHdiff = cmb * Math.sqrt(rho / structure.getN(cmb));
        (pDiff = new Raypath(p_Pdiff, WOODHOUSE, MESH)).compute();
        (svDiff = new Raypath(p_SVdiff, WOODHOUSE, MESH)).compute();
        (shDiff = new Raypath(p_SHdiff, WOODHOUSE, MESH)).compute();
    }

    /**
     * @return Raypath of Pdiff
     */
    public Raypath getPdiff() {
        return pDiff;
    }

    /**
     * @return Raypath of SVdiff
     */
    public Raypath getSVdiff() {
        return svDiff;
    }

    /**
     * @return Raypath of SHdiff
     */
    public Raypath getSHdiff() {
        return shDiff;
    }

    // TODO low high

    /**
     * Creates a catalogue.
     */
    private void create() {
        double pMax = computeRayparameterLimit();
        // System.out.println("pMax=" + pMax);
        // Compute raparameters for diffration phases.
        computeDiffraction();
        long time = System.nanoTime();
        // pMax = 200;
        Raypath firstPath = new Raypath(0, WOODHOUSE, MESH);
        firstPath.compute();
        raypathList.add(firstPath);
        System.err.println("Computing a catalogue. (If you use the same model, the catalog is not computed anymore.)");
        double p_Pdiff = pDiff.getRayParameter();
        double p_SVdiff = svDiff.getRayParameter();
        double p_SHdiff = shDiff.getRayParameter();

        for (double p = firstPath.getRayParameter() + DELTA_P, nextP = p + DELTA_P; p < pMax; p = nextP) {
            Raypath rp = new Raypath(p, WOODHOUSE, MESH);
            rp.compute();
            if (closeEnough(raypathList.last(), rp)) {
                raypathList.add(rp);
                nextP = p + DELTA_P;
            } else {
                raypathPool.add(rp);
                nextP = (p + raypathList.last().getRayParameter()) / 2;
            }

            // System.out.println(p + " " + nextP);
            if (lookIntoPool()) {
                p = raypathList.last().getRayParameter();
                nextP = p + DELTA_P;
            }

            if (p < p_Pdiff && p_Pdiff < nextP) {
                closeDiff(pDiff);
                nextP = raypathList.last().getRayParameter() + DELTA_P;
            } else if (p < p_SVdiff && p_SVdiff < nextP) {
                closeDiff(svDiff);
                nextP = raypathList.last().getRayParameter() + DELTA_P;
            } else if (p < p_SHdiff && p_SHdiff < nextP) {
                closeDiff(shDiff);
                nextP = raypathList.last().getRayParameter() + DELTA_P; // TODO
                // SHSV
            }
        }
        raypathList.add(pDiff);
        raypathList.add(svDiff);
        raypathList.add(shDiff);

        System.err.println("Catalogue was made in " + Utilities.toTimeString(System.nanoTime() - time));
    }

    private void closeDiff(Raypath diffPath) {
        double diffP = diffPath.getRayParameter();
        Raypath last = raypathList.last();
        Raypath diffMinus = new Raypath(diffP - MINIMUM_DELTA_P, WOODHOUSE, MESH);
        Raypath diffPlus = new Raypath(diffP + MINIMUM_DELTA_P, WOODHOUSE, MESH);
        diffMinus.compute();
        diffPlus.compute();
        for (double p = (diffP + last.getRayParameter()) / 2, nextP = p + DELTA_P; ; p = nextP) {
            Raypath candidate = new Raypath(p, WOODHOUSE, MESH);
            candidate.compute();
            if (!closeEnough(raypathList.last(), candidate)) {
                raypathPool.add(candidate);
                nextP = (raypathList.last().getRayParameter() + candidate.getRayParameter()) / 2;
                continue;
            }
            raypathList.add(candidate);
            lookIntoPool();
            candidate = raypathList.last();
            if (!closeEnough(candidate, diffMinus)) {
                nextP = (candidate.getRayParameter() + diffP - MINIMUM_DELTA_P) / 2;
                continue;
            }
            raypathList.add(candidate);
            raypathList.add(diffMinus);
            raypathList.add(diffPlus);
            return;
        }
    }

    private final transient TreeSet<Raypath> raypathPool = new TreeSet<>();

    /**
     * Look for a raypath to be a next one for the {@link #raypathList}. If one
     * is found and another is also found for the next next one, all are added
     * recursively.
     *
     * @return If any good raypath in the pool
     */
    private boolean lookIntoPool() {
        boolean added = false;
        for (Raypath raypath : raypathPool)
            if (closeEnough(raypathList.last(), raypath)) {
                raypathList.add(raypath);
                added = true;
            }
        return added;
    }

    /**
     * Criterion for the catalog is {@link #D_DELTA} so far in both P and S
     * wave. The ray parameter of raypath1 must be smaller than that of
     * raypath2, otherwise, false is returned. TODO SH SV?? Both raypaths must
     * be computed before this method.
     *
     * @param raypath1 to be checked
     * @param raypath2 to be checked
     * @return If they are similar path.
     */
    private boolean closeEnough(Raypath raypath1, Raypath raypath2) {
        if (raypath2.getRayParameter() <= raypath1.getRayParameter()) return false;
        if (raypath2.getRayParameter() - raypath1.getRayParameter() < MINIMUM_DELTA_P) return true;
        double earthRadius = WOODHOUSE.getStructure().earthRadius();
        double p1 = raypath1.computeDelta(earthRadius, Phase.P);
        double p2 = raypath2.computeDelta(earthRadius, Phase.P);
        // System.out.println(Math.toDegrees(p1) + " p" + Math.toDegrees(p2));
        if (Double.isNaN(p1) ^ Double.isNaN(p2)) return false;
        if (Double.isNaN(p1)) {
            p1 = raypath1.computeDelta(earthRadius, Phase.PcP);
            p2 = raypath2.computeDelta(earthRadius, Phase.PcP);
        }
        // System.out.println(Math.toDegrees(p1) + " " + Math.toDegrees(p2));
        if (D_DELTA < Math.abs(p1 - p2)) return false;
        double s1 = raypath1.computeDelta(earthRadius, Phase.S);
        double s2 = raypath2.computeDelta(earthRadius, Phase.S);
        // System.out.println(Math.toDegrees(s1) + " s " + Math.toDegrees(s2));

        if (Double.isNaN(s1) ^ Double.isNaN(s2)) return false;
        if (Double.isNaN(s1)) {
            s1 = raypath1.computeDelta(earthRadius, Phase.ScS);
            s2 = raypath2.computeDelta(earthRadius, Phase.ScS);
        }
        return Math.abs(s1 - s2) <= D_DELTA;
    }

    /**
     * Computes ray parameter p with which q<sub>&tau;</sub> =0 at the earth
     * surface for P, SV and SH. Returns the maximum value of them. (basically
     * the one of S)
     * <p>
     * P &rarr; r*(&rho;/A)<sup>1/2</sup> <br>
     * SV &rarr; r*(&rho;/L)<sup>1/2</sup> <br>
     * SH &rarr; r*(&rho;/N)<sup>1/2</sup> <br>
     *
     * @return maximum ray parameter
     * @see "Woodhouse (1981)"
     */
    private double computeRayparameterLimit() {
        VelocityStructure structure = WOODHOUSE.getStructure();
        double r = structure.earthRadius();
        double rho = structure.getRho(r);
        double p = r * Math.sqrt(rho / structure.getA(r));
        double sv = r * Math.sqrt(rho / structure.getL(r));
        double sh = r * Math.sqrt(rho / structure.getN(r));
        return Math.max(Math.max(p, sv), sh);
    }

    /**
     * @param path    the path for the catalogue file.
     * @param options open option
     * @return Catalogue read from the path
     * @throws IOException            if any
     * @throws ClassNotFoundException if any
     */
    public static RaypathCatalog read(Path path, OpenOption... options) throws IOException, ClassNotFoundException {
        try (ObjectInputStream oi = new ObjectInputStream(Files.newInputStream(path, options))) {
            return (RaypathCatalog) oi.readObject();
        }
    }

    /**
     * Assume that there is a regression curve f(&Delta;) = p(ray parameter) for
     * the small range. The function f is assumed to be a polynomial function.
     * The degree of the function depends on the number of the input raypaths.
     *
     * @param targetDelta [rad] epicentral distance to get T for
     * @param raypaths    Polynomial interpolation is done with these. All the raypaths
     *                    must be computed.
     * @return travel time [s] for the target Delta estimated by the polynomial
     * interpolation with the raypaths.
     */
    private Raypath interpolateRaypath(Phase targetPhase, double eventR, double targetDelta, Raypath... raypaths) {
        WeightedObservedPoints deltaP = new WeightedObservedPoints();
        for (Raypath raypath : raypaths)
            deltaP.add(raypath.computeDelta(eventR, targetPhase), raypath.getRayParameter());
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(raypaths.length - 1);
        PolynomialFunction pf = new PolynomialFunction(fitter.fit(deltaP.toList()));
        Raypath ray = new Raypath(pf.value(targetDelta), WOODHOUSE, MESH);
        ray.compute();
        return ray;
    }

    /**
     * @param path the path to the output file
     * @throws IOException If an I/O error happens. it throws error.
     */
    public void write(Path path, OpenOption... options) throws IOException {
        try (ObjectOutputStream o = new ObjectOutputStream(Files.newOutputStream(path, options))) {
            o.writeObject(this);
        }
    }

    /**
     * @param targetPhase target phase
     * @param eventR      [km] event radius
     * @param targetDelta [rad] target &Delta;
     * @return Never returns null. zero length array is possible.
     */
    public Raypath[] searchPath(Phase targetPhase, double eventR, double targetDelta) {
        Raypath[] raypaths = getRaypaths();
        // System.err.println("Looking for Phase:" + targetPhase + ",
        // \u0394[\u02da]:"
        // + Precision.round(Math.toDegrees(targetDelta), 4));

        if (targetPhase.isDiffracted()) return new Raypath[]{targetPhase.toString().contains("Pdiff") ? getPdiff() :
                (targetPhase.isPSV() ? getSVdiff() : getSHdiff())};

        List<Raypath> pathList = new ArrayList<>();
        for (int i = 0; i < raypaths.length - 1; i++) {
            Raypath rayI = raypaths[i];
            Raypath rayP = raypaths[i + 1];
            double deltaI = rayI.computeDelta(eventR, targetPhase);
            double deltaP = rayP.computeDelta(eventR, targetPhase);
            if (Double.isNaN(deltaI) || Double.isNaN(deltaP)) continue;
            if (0 < (deltaI - targetDelta) * (deltaP - targetDelta)) continue;
            Raypath rayC = new Raypath((rayI.getRayParameter() + rayP.getRayParameter()) / 2, WOODHOUSE, MESH);
            rayC.compute();
            double deltaC = rayC.computeDelta(eventR, targetPhase);
            if (Double.isNaN(deltaC)) continue;
            Raypath rayIn = interpolateRaypath(targetPhase, eventR, targetDelta, rayI, rayC, rayP);
            if (Double.isNaN(rayIn.computeDelta(eventR, targetPhase))) continue;
            pathList.add(rayIn);
        }
        return pathList.toArray(new Raypath[0]);
    }

    /**
     * @param targetPhase target phase
     * @param eventR      [km] event radius
     * @param targetDelta [rad] target &Delta;
     */
    public double[] searchTime(Phase targetPhase, double eventR, double targetDelta) {
        // System.err.println("Looking for Phase:" + targetPhase + ",
        // \u0394[\u02da]:"
        // + Precision.round(Math.toDegrees(targetDelta), 4));
        List<Double> timeList = new ArrayList<>();
        Raypath[] raypaths = getRaypaths();
        for (int i = 0; i < raypaths.length - 1; i++) {
            Raypath rayI = raypaths[i];
            Raypath rayP = raypaths[i + 1];
            double deltaI = rayI.computeDelta(eventR, targetPhase);
            double deltaP = rayP.computeDelta(eventR, targetPhase);
            if (Double.isNaN(deltaI) || Double.isNaN(deltaP)) continue;
            if (0 < (deltaI - targetDelta) * (deltaP - targetDelta)) continue;
            Raypath rayC = new Raypath((rayI.getRayParameter() + rayP.getRayParameter()) / 2, WOODHOUSE, MESH);
            rayC.compute();
            Raypath rayIn = interpolateRaypath(targetPhase, eventR, targetDelta, rayI, rayC, rayP);
            double deltaC = rayC.computeDelta(eventR, targetPhase);
            double deltaIn = rayIn.computeDelta(eventR, targetPhase);
            if (Double.isNaN(deltaC) || Double.isNaN(deltaIn)) continue;
            timeList.add(interpolateTraveltime(targetPhase, eventR, targetDelta, rayI, rayC, rayP, rayIn));
        }
        return timeList.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * Assume that there is a regression curve f(&Delta;) = T for the small
     * range. The function f is assumed to be a polynomial function. The degree
     * of the function depends on the number of the input raypaths.
     *
     * @param targetDelta [rad] epicentral distance to get T for
     * @param raypaths    Polynomial interpolation is done with these. All the raypaths
     *                    must be computed.
     * @return travel time [s] for the target Delta estimated by the polynomial
     * interpolation with the raypaths.
     */
    private static double interpolateTraveltime(Phase targetPhase, double eventR, double targetDelta,
                                                Raypath... raypaths) {
        WeightedObservedPoints deltaTime = new WeightedObservedPoints();
        for (Raypath raypath : raypaths)
            deltaTime.add(raypath.computeDelta(eventR, targetPhase), raypath.computeT(eventR, targetPhase));
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(raypaths.length - 1);
        return new PolynomialFunction(fitter.fit(deltaTime.toList())).value(targetDelta);
    }

    /**
     * @param targetDelta [deg]
     * @param raypath0    source of raypath
     * @param eventR      radius of the source [km]
     * @param phase       to look for
     * @return travel time for the targetDelta [s]
     */
    double travelTimeByThreePointInterpolate(double targetDelta, Raypath raypath0, double eventR, Phase phase) {
        targetDelta = Math.toRadians(targetDelta);
        double delta0 = raypath0.computeDelta(eventR, phase);
        Raypath lower = raypathList.lower(raypath0);
        Raypath higher = raypathList.higher(raypath0);
        double lowerDelta = lower.computeDelta(eventR, phase);
        double higherDelta = higher.computeDelta(eventR, phase);
        if (Double.isNaN(lowerDelta) || Double.isNaN(higherDelta)) return Double.NaN;
        // delta1<delta0<delta2 or delta2<delta0<delta1
        double[] p = new double[]{delta0, lowerDelta, higherDelta};
        double[] time = new double[]{raypath0.computeT(eventR, phase), lower.computeT(eventR, phase),
                higher.computeT(eventR, phase),};
        Trace t = new Trace(p, time);
        return t.toValue(2, targetDelta);
    }

}
