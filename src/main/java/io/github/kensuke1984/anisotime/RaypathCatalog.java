package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.Environment;
import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Raypath catalogue for one model
 * <p>
 * If a new catalog is computed which does not exist in Kibrary share, it
 * automatically is stored.
 * <p>
 * TODO Search should be within branches
 *
 * @author Kensuke Konishi, Anselme Borgeaud
 * @version 0.1.5
 */
public class RaypathCatalog implements Serializable {
    /**
     * 2019/11/20
     */
    private static final long serialVersionUID = 6971463963735925367L;

    private static Path downloadCatalogZip() throws IOException {
        Path zipPath = Files.createTempFile("piac", ".zip");
        URL website = new URL("https://www.dropbox.com/s/dadsqhe47wnfe2k/piac.zip?dl=1");
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        try (FileOutputStream fos = new FileOutputStream(zipPath.toFile())) {
            try (FileChannel channel = fos.getChannel()) {
                channel.transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        }
        return zipPath;
    }

    private static void extractInShare() throws IOException {
        Files.createDirectories(share);
        Path zipPath = downloadCatalogZip();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = share.resolve(entry.getName());
                try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                        Files.newOutputStream(outPath))) {
                    byte[] buf = new byte[1024];
                    int size = 0;
                    while ((size = zis.read(buf)) != -1) bufferedOutputStream.write(buf, 0, size);
                }
            }
        }
    }

    /**
     * Creates a catalog for a model file (model file, or prem, iprem, ak135).
     * The catalog computes so densely as any adjacent raypath pairs has smaller gap
     * than (&delta;&Delta;) in P, PcP, S and ScS.
     * Computation mesh in each part is (inner-core, outer-core and mantle), respectively.
     *
     * @param args [model file (prem, iprem, ak135 or a polynomial file only now)] [&delta;&Delta; (deg)] [inner-core]
     *             [outer-core] [mantle] intervals
     * @throws IOException if any
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 6) throw new IllegalArgumentException(
                "Usage: [model name, polynomial file] [\u03b4\u0394 (deg)] [inner-core] [outer-core] [mantle]");
        VelocityStructure structure;
        switch (args[0]) {
            case "prem":
            case "PREM":
                structure = VelocityStructure.prem();
                break;
            case "iprem":
            case "iPREM":
                structure = VelocityStructure.iprem();
                break;
            case "ak135":
            case "AK135":
                structure = VelocityStructure.ak135();
                break;
            default:
                structure = new PolynomialStructure(Paths.get(args[0]));
        }
        double dDelta = Math.toRadians(Double.parseDouble(args[1]));
        ComputationalMesh mesh =
                new ComputationalMesh(structure, Double.parseDouble(args[2]), Double.parseDouble(args[3]),
                        Double.parseDouble(args[4]));
        computeCatalogue(structure, mesh, dDelta);
    }

    /**
     * Default value of {@link #MAXIMUM_D_DELTA}
     */
    static final double DEFAULT_MAXIMUM_D_DELTA = Math.toRadians(0.1);

    /**
     * Catalog for PREM. &delta;&Delta; = {@link #DEFAULT_MAXIMUM_D_DELTA}. Mesh is simple.
     */
    public final static RaypathCatalog PREM;
    /**
     * Catalog for the isotropic PREM. &delta;&Delta; = {@link #DEFAULT_MAXIMUM_D_DELTA}. Mesh is simple.
     */
    public final static RaypathCatalog ISO_PREM;
    /**
     * Catalog for AK135. &delta;&Delta; = {@link #DEFAULT_MAXIMUM_D_DELTA}. Mesh is simple.
     */
    public final static RaypathCatalog AK135;
    private static final Path share = Environment.KIBRARY_HOME.resolve("share");

    static {
        if (!Files.exists(share)) try {
            extractInShare();
        } catch (IOException e) {
            System.err.println("Could not download catalog files from internet");

        }
        try {
            BiFunction<Path, VelocityStructure, RaypathCatalog> getCatalogue = (p, v) -> {
                RaypathCatalog cat;
                String model = p.getFileName().toString().replace(".cat", "");
                ComputationalMesh simple = ComputationalMesh.simple(v);
                if (Files.exists(p)) {
                    try {
                        cat = read(p);
                    } catch (ClassNotFoundException | IOException ice) {
                        System.err.println("Creating a catalog for " + model +
                                " (due to out of date). This computation is done only once.");
                        (cat = new RaypathCatalog(v, simple, DEFAULT_MAXIMUM_D_DELTA)).create();
                        try {
                            cat.write(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.err.println("Creating a catalog for " + model + ". This computation is done only once.");
                    (cat = new RaypathCatalog(v, simple, DEFAULT_MAXIMUM_D_DELTA)).create();
                    try {
                        cat.write(p);
                    } catch (IOException e) {
                        e.printStackTrace();
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
     * Minimum value of &delta;p [s/rad] (ray parameter). Even if similar raypaths
     * satisfying {@link #MAXIMUM_D_DELTA} are not found within this value, a catalogue
     * does not have a denser ray parameter than the value.
     */
    private static final double MINIMUM_DELTA_P = 1e-3;
    /**
     * Woodhouse formula with certain velocity structure
     */
    private final Woodhouse1981 WOODHOUSE;
    /**
     * Possible maximum gap in &Delta; [rad] for major phases such as P, S, PcP, SKS and so on.
     */
    private final double MAXIMUM_D_DELTA;
    /**
     * List of stored raypaths. Ordered by each ray parameter p.
     */
    private final TreeSet<Raypath> raypathList = new TreeSet<>();
    /**
     * Mesh for computation
     */
    private final ComputationalMesh MESH;
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
        MAXIMUM_D_DELTA = dDelta;
        MESH = mesh;
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
     * @return Catalogue for the input structure
     */
    public static RaypathCatalog computeCatalogue(VelocityStructure structure, ComputationalMesh mesh, double dDelta) {
        try (DirectoryStream<Path> catalogStream = Files.newDirectoryStream(share, "*.cat")) {
            for (Path p : catalogStream)
                try {
                    RaypathCatalog c;
                    switch (p.getFileName().toString()) {
                        case "iprem.cat":
                            c = ISO_PREM;
                            break;
                        case "prem.cat":
                            c = PREM;
                            break;
                        case "ak135.cat":
                            c = AK135;
                            break;
                        default:
                            c = read(p);
                    }
                    if (c.getStructure().equals(structure) && c.MESH.equals(mesh) && c.MAXIMUM_D_DELTA == dDelta) {
//                        System.err.println("Catalog is found. " + p);
                        return c;
                    }
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
            System.err.println(p + " is created.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cat;
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
     * If an input angle is a radian for 370 deg, then one for 10 deg returns.
     * If an input angle is a radian for 190 deg, then one for 170 deg returns.
     *
     * @param angle [rad]
     * @return a relative angle for the input angle. The angle is [0,180]
     */
    private static double toRelativeAngle(double angle) {
        while (0 < angle) angle -= 2 * Math.PI;
        angle += 2 * Math.PI;
        return angle <= Math.PI ? angle : 2 * Math.PI - angle;
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
     * @return {@link ComputationalMesh} used in the catalog.
     */
    ComputationalMesh getMesh() {
        return MESH;
    }

    /**
     * @param raypath to compute and add in {@link #raypathList}
     */
    private void computeANDadd(Raypath raypath) {
        raypath.compute();
        raypathList.add(raypath);
    }


    /**
     * Computes ray parameters of diffraction phases (Pdiff and Sdiff).
     */
    private void computeDiffraction() {
        VelocityStructure structure = getStructure();
        double cmb = structure.coreMantleBoundary() + ComputationalMesh.EPS;
        double rho = structure.getRho(cmb);
        double p_Pdiff = cmb * Math.sqrt(rho / structure.getA(cmb));
        double p_SVdiff = cmb * Math.sqrt(rho / structure.getL(cmb));
        double p_SHdiff = cmb * Math.sqrt(rho / structure.getN(cmb));
        pDiff = new Raypath(p_Pdiff, WOODHOUSE, MESH);
        svDiff = new Raypath(p_SVdiff, WOODHOUSE, MESH);
        shDiff = new Raypath(p_SHdiff, WOODHOUSE, MESH);
        computeANDadd(pDiff);
        computeANDadd(svDiff);
        computeANDadd(shDiff);
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

    private boolean exists(Raypath path) {
        double earthSurface = path.getStructure().earthRadius();
        System.out.println(
                "P:" + path.computeDelta(earthSurface, Phase.P) + " S" + path.computeDelta(earthSurface, Phase.S) +
                        " PcP" + path.computeDelta(earthSurface, Phase.PcP) + " ScS" +
                        path.computeDelta(earthSurface, Phase.ScS));
        System.out.println("P:" + path.computeDelta(earthSurface - 1, Phase.p) + " S" +
                path.computeDelta(earthSurface - 1, Phase.s));
        return !(Double.isNaN(path.computeDelta(earthSurface, Phase.P)) &&
                Double.isNaN(path.computeDelta(earthSurface, Phase.SV)) &&
                Double.isNaN(path.computeDelta(earthSurface, Phase.S)) &&
                Double.isNaN(path.computeDelta(earthSurface, Phase.PcP)) &&
                Double.isNaN(path.computeDelta(earthSurface, Phase.SVcS)) &&
                Double.isNaN(path.computeDelta(earthSurface, Phase.ScS)));
    }

    /**
     * TODO
     * Creates a catalogue.
     * when running into a ray path with all NaN. what should we do.
     */
    private void create() {
        // Compute raparameters for diffration phases.
        long time = System.nanoTime();
        System.err.println("Computing a catalogue. If you use the same model, the catalog is not computed anymore.");
        Raypath firstPath = new Raypath(0, WOODHOUSE, MESH);
        computeANDadd(firstPath);
        catalogOf(Phase.P);
        catalogOf(Phase.S);
        catalogOf(Phase.SV);
        catalogOf(Phase.PcP);
        catalogOf(Phase.ScS);
        catalogOf(Phase.PKP);
        catalogOf(Phase.SKS);
        catalogOf(Phase.PKIKP);
        catalogOf(Phase.PKiKP);
        catalogOf(Phase.SKIKS);
        catalogOf(Phase.SKiKS);

        computeDiffraction();

        System.err.println("Catalogue was made in " + Utilities.toTimeString(System.nanoTime() - time));
    }

    /**
     * If any of Raypaths has NaN for Phase 'p', the condition becomes true.
     *
     * @param phase  to be checked.
     * @param dDelta [rad] If the gap of deltas (epicentral distance) of Raypaths are equal to or less than this value,
     *               the condition becomes true
     */
    private BiPredicate<Raypath, Raypath> simplePredicate(Phase phase, double dDelta) {
        double r = getStructure().earthRadius();
        return (r1, r2) -> Math.abs(r1.getRayParameter() - r2.getRayParameter()) < MINIMUM_DELTA_P ||
                !(Math.abs(r1.computeDelta(r, phase) - r2.computeDelta(r, phase)) > dDelta);
    }

    /**
     * @param phase  target phase
     * @param startP [s/rad] ray parameter
     * @param endP   [s/rad] ray parameter
     * @return Set (catalog) of raypaths. In the catalog, gap of any neighbors is within {@link #MAXIMUM_D_DELTA}.
     */
    private Set<Raypath> catalogInBranch(Phase phase, double startP, double endP) {
        TreeSet<Raypath> catalog = new TreeSet<>();
        BiPredicate<Raypath, Raypath> closeEnough = simplePredicate(phase, MAXIMUM_D_DELTA);
        BinaryOperator<Raypath> centerRayparameterRaypath =
                (r1, r2) -> new Raypath((r1.getRayParameter() + r2.getRayParameter()) / 2, WOODHOUSE, MESH);
        Raypath startRaypath = new Raypath(startP, WOODHOUSE, MESH);
        Raypath endRaypath = new Raypath(endP, WOODHOUSE, MESH);
        computeANDadd(startRaypath);
        computeANDadd(endRaypath);
        //copy from main
        raypathList.stream().filter(r -> startP <= r.getRayParameter() && r.getRayParameter() <= endP)
                .forEach(catalog::add);
        List<Raypath> supplementList = new ArrayList<>();
        do {
            supplementList.clear();
            for (Raypath raypath = catalog.first(); raypath != catalog.last(); raypath = catalog.higher(raypath)) {
                Raypath higher = catalog.higher(raypath);
                if (closeEnough.test(raypath, higher)) continue;
                supplementList.add(centerRayparameterRaypath.apply(raypath, higher));
            }
            supplementList.forEach(Raypath::compute);
        } while (catalog.addAll(supplementList));
        return catalog;
    }

    /**
     * supplement catalog
     *
     * @param phase target phase
     */
    private void catalogOf(Phase phase) {
        List<Double[]> edgeList = computeRaypameterEdge(phase);
        BinaryOperator<Raypath> centerRayparameterRaypath =
                (r1, r2) -> new Raypath((r1.getRayParameter() + r2.getRayParameter()) / 2, WOODHOUSE, MESH);
        for (Double[] edges : edgeList) {
            double startP = Math.min(edges[0], edges[1]);
            double endP = Math.max(edges[0], edges[1]);
            Set<Raypath> catalogPart = catalogInBranch(phase, startP, endP);
            raypathList.addAll(catalogPart);
        }
    }

    /**
     * @param phase target {@link Phase}
     * @return critical ray parameters [s/rad]. e.g. a raypath which has turning depth at a boundary
     * &plusmn; {@link ComputationalMesh#EPS}
     */
    private List<Double[]> computeRaypameterEdge(Phase phase) {
        List<Double[]> edges = new ArrayList<>();
        double cmb = getStructure().coreMantleBoundary();
        double icb = getStructure().innerCoreBoundary();

        //case: bounsing waves
        if (phase == Phase.P || phase == Phase.S || phase == Phase.SV || phase == Phase.SKIKS || phase == Phase.PKIKP) {
            PhasePart turningPP;
            double[] concerningBoundaries;
            if (phase == Phase.P || phase == Phase.S || phase == Phase.SV) {
                turningPP = phase == Phase.P ? PhasePart.P : phase.isPSV() ? PhasePart.SV : PhasePart.SH;
                concerningBoundaries = getStructure().boundariesInMantle();
            } else {
                turningPP = phase == Phase.PKIKP ? PhasePart.I : PhasePart.JV;
                concerningBoundaries = getStructure().boundariesInInnerCore();
            }
            for (int i = 0; i < concerningBoundaries.length - 1; i++) {
                double startR = concerningBoundaries[i] + ComputationalMesh.EPS;
                double endR = concerningBoundaries[i + 1] - ComputationalMesh.EPS;
                double pStartR = computeRayparameterFor(turningPP, startR);
                if (i == 0 && (phase == Phase.SKIKS || phase == Phase.PKIKP)) pStartR = 0;
                double pEndR = computeRayparameterFor(turningPP, endR);
                edges.add(new Double[]{pStartR, pEndR});
            }
        } else if (phase == Phase.PKP || phase == Phase.SKS) {
            PhasePart turningPP = PhasePart.K;
            double[] concerningBoundaries = getStructure().boundariesInOuterCore();
            double v = phase == Phase.PKP ? getStructure().computeVph(cmb) : getStructure().computeVsv(cmb);
            double pMax = cmb / v;
            DoubleUnaryOperator pToTurningR = rayP -> phase == Phase.PcP ? getStructure().pTurningR(rayP) :
                    phase.isPSV() ? getStructure().svTurningR(rayP) : getStructure().shTurningR(rayP);
            while (!Double.isNaN(pToTurningR.applyAsDouble(pMax))) {
                pMax -= MINIMUM_DELTA_P;
            }
            for (int i = 0; i < concerningBoundaries.length - 1; i++) {
                double startR = concerningBoundaries[i] + ComputationalMesh.EPS;
                double endR = concerningBoundaries[i + 1] - ComputationalMesh.EPS;
                double pStartR = computeRayparameterFor(turningPP, startR);
                double pEndR = computeRayparameterFor(turningPP, endR);
                if (pMax <= pStartR) continue;
                else if (pMax < pEndR) edges.add(new Double[]{pStartR, pMax});
                else edges.add(new Double[]{pStartR, pEndR});
            }
            //case: reflecting waves
        } else if (phase == Phase.PcP || phase == Phase.ScS) {
            double v = phase == Phase.PcP ? getStructure().computeVph(cmb) :
                    phase.isPSV() ? getStructure().computeVsv(cmb) : getStructure().computeVsh(cmb);
            double p = cmb / v;
            DoubleUnaryOperator pToTurningR = rayP -> phase == Phase.PcP ? getStructure().pTurningR(rayP) :
                    phase.isPSV() ? getStructure().svTurningR(rayP) : getStructure().shTurningR(rayP);
            while (!Double.isNaN(pToTurningR.applyAsDouble(p))) {
                p -= MINIMUM_DELTA_P;
            }
            edges.add(new Double[]{0d, p});
        } else if (phase == Phase.PKiKP || phase == Phase.SKiKS) {
            double v = getStructure().computeVph(icb + ComputationalMesh.EPS);
            double p = (icb + ComputationalMesh.EPS) / v;
            DoubleUnaryOperator pToTurningR = rayP -> getStructure().kTurningR(rayP);
            while (!Double.isNaN(pToTurningR.applyAsDouble(p))) {
                p -= MINIMUM_DELTA_P;
            }
            edges.add(new Double[]{0d, p});
        } else throw new RuntimeException("NEXPEKTED");
        return edges;
    }

    /**
     * @param pp       target {@link PhasePart}
     * @param turningR [km] radius at a target turning point
     * @return ray parameter of the raypath which has a turning point of the pp at the turningR.
     */
    private double computeRayparameterFor(PhasePart pp, double turningR) {
        switch (pp) {
            case P:
            case K:
            case I:
                return turningR / getStructure().computeVph(turningR);
            case SV:
            case JV:
                return turningR / getStructure().computeVsv(turningR);
            case SH:
            case JH:
                return turningR / getStructure().computeVsh(turningR);
            default:
                throw new IllegalStateException("Unexpected value: " + pp);
        }
    }


    /**
     * Assume that there is a regression curve f(&Delta;) = p(ray parameter) for
     * the small range. The function f is assumed to be a polynomial function.
     * The degree of the function is 1. <S>depends on the number of the input raypaths.</S>
     *
     * @param targetPhase   target phase
     * @param eventR        [km] radius of event
     * @param targetDelta   [rad] epicentral distance to get T for
     * @param relativeAngle if the targetDelta is a relative value.
     * @param raypaths      Polynomial interpolation is done with these. All the raypaths
     *                      must be computed.
     * @return Raypath for the target Delta estimated by the polynomial
     * interpolation with the raypaths.
     */
    private Raypath interpolateRaypath(Phase targetPhase, double eventR, double targetDelta, boolean relativeAngle,
                                       Raypath... raypaths) {
        WeightedObservedPoints deltaP = new WeightedObservedPoints();
        for (Raypath raypath : raypaths) {
            double delta = raypath.computeDelta(eventR, targetPhase);
            if (relativeAngle) delta = toRelativeAngle(delta);
            deltaP.add(delta, raypath.getRayParameter());
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
        PolynomialFunction pf = new PolynomialFunction(fitter.fit(deltaP.toList()));
        Raypath ray = new Raypath(pf.value(targetDelta), WOODHOUSE, MESH);
        ray.compute();
        return ray;
    }

    /**
     * @param path    the path to the write file
     * @param options if any
     * @throws IOException If an I/O error happens. it throws error.
     */
    public void write(Path path, OpenOption... options) throws IOException {
        try (ObjectOutputStream o = new ObjectOutputStream(Files.newOutputStream(path, options))) {
            o.writeObject(this);
        }
    }

    /**
     * @param targetPhase   target phase
     * @param eventR        [km] event radius
     * @param targetDelta   [rad] target &Delta;
     * @param relativeAngle if the input targetDelta is a relative angle.
     * @return Arrays of raypaths which epicentral distances are close to the targetDelta. Never returns null. zero length array is possible.
     */
    public Raypath[] searchPath(Phase targetPhase, double eventR, double targetDelta, boolean relativeAngle) {
        if (targetPhase.isDiffracted()) return new Raypath[]{targetPhase.toString().contains("Pdiff") ? getPdiff() :
                (targetPhase.isPSV() ? getSVdiff() : getSHdiff())};
        if (targetDelta < 0) throw new IllegalArgumentException("A targetDelta must be non-negative.");
        if (relativeAngle && Math.PI < targetDelta) throw new IllegalArgumentException(
                "When you search paths for a relative angle, a targetDelta must be pi or less.");
        Raypath[] raypaths = getRaypaths();
//        System.err.println("Looking for Phase:" + targetPhase + ", \u0394[\u02da]:" + TODO
//                Precision.round(Math.toDegrees(targetDelta), 4));
        List<Raypath> pathList = new ArrayList<>();
        for (int i = 0; i < raypaths.length - 1; i++) {
            Raypath rayI = raypaths[i];
            Raypath rayP = raypaths[i + 1];
            double deltaI = rayI.computeDelta(eventR, targetPhase);
            double deltaP = rayP.computeDelta(eventR, targetPhase);
            if (Double.isNaN(deltaI) || Double.isNaN(deltaP)) continue;
            if (relativeAngle) {
                deltaI = toRelativeAngle(deltaI);
                deltaP = toRelativeAngle(deltaP);
            }
            if (0 < (deltaI - targetDelta) * (deltaP - targetDelta)) continue;
            Raypath rayC = new Raypath((rayI.getRayParameter() + rayP.getRayParameter()) / 2, WOODHOUSE, MESH);
            rayC.compute();
            if (Double.isNaN(rayC.computeDelta(eventR, targetPhase))) continue;
            Raypath rayIn = interpolateRaypath(targetPhase, eventR, targetDelta, relativeAngle, rayI, rayC, rayP);
            if (Double.isNaN(rayIn.computeDelta(eventR, targetPhase))) continue;
            pathList.add(rayIn);
        }
        return pathList.toArray(new Raypath[0]);
    }

    /**
     * The returning travel time is computed by the input raypath0 and the 2 raypaths with the closest larger and smaller rayparameters.
     *
     * @param targetPhase   to look for
     * @param eventR        [km] radius of the source
     * @param targetDelta   [rad]
     * @param relativeAngle if the targetDelta is a relative value.
     * @param raypath0      source of raypath
     * @return travel time for the targetDelta [s]
     */
    double travelTimeByThreePointInterpolate(Phase targetPhase, double eventR, double targetDelta,
                                             boolean relativeAngle, Raypath raypath0) {
        if (targetDelta < 0) throw new IllegalArgumentException("A targetDelta must be non-negative.");
        if (relativeAngle && Math.PI < targetDelta) throw new IllegalArgumentException(
                "When you search paths for a relative angle, a targetDelta must be pi or less.");

        double delta0 = raypath0.computeDelta(eventR, targetPhase);
        Raypath lower = raypathList.lower(raypath0);
        Raypath higher = raypathList.higher(raypath0);
        double lowerDelta = lower.computeDelta(eventR, targetPhase);
        double higherDelta = higher.computeDelta(eventR, targetPhase);
        if (Double.isNaN(lowerDelta) || Double.isNaN(higherDelta)) return Double.NaN;
        if (relativeAngle) {
            lowerDelta = toRelativeAngle(lowerDelta);
            higherDelta = toRelativeAngle(higherDelta);
        }
        WeightedObservedPoints pTime = new WeightedObservedPoints();
        pTime.add(delta0, raypath0.computeT(eventR, targetPhase));
        pTime.add(lowerDelta, lower.computeT(eventR, targetPhase));
        pTime.add(higherDelta, higher.computeT(eventR, targetPhase));
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        PolynomialFunction pf = new PolynomialFunction(fitter.fit(pTime.toList()));
        return pf.value(targetDelta);
    }

    /**
     * The returning ray parameter is computed by the input raypath0 and the 2 raypaths
     * with the closest larger and smaller ray parameters.
     *
     * @param targetPhase   to look for
     * @param eventR        [km] radius of the source
     * @param targetDelta   [rad]
     * @param relativeAngle if the targetDelta is a relative value.
     * @param raypath0      source of raypath
     * @return ray parameter [s/rad] by interpolation
     */
    double rayParameterByThreePointInterpolate(Phase targetPhase, double eventR, double targetDelta,
                                               boolean relativeAngle, Raypath raypath0) {
        if (targetDelta < 0) throw new IllegalArgumentException("A targetDelta must be non-negative.");
        if (relativeAngle && Math.PI < targetDelta) throw new IllegalArgumentException(
                "When you search paths for a relative angle, a targetDelta must be pi or less.");

        double delta0 = raypath0.computeDelta(eventR, targetPhase);
        Raypath lower = raypathList.lower(raypath0);
        Raypath higher = raypathList.higher(raypath0);
        double lowerDelta = lower.computeDelta(eventR, targetPhase);
        double higherDelta = higher.computeDelta(eventR, targetPhase);
        if (Double.isNaN(lowerDelta) || Double.isNaN(higherDelta)) return Double.NaN;
        if (relativeAngle) {
            lowerDelta = toRelativeAngle(lowerDelta);
            higherDelta = toRelativeAngle(higherDelta);
        }

        WeightedObservedPoints distOfP = new WeightedObservedPoints();
        distOfP.add(delta0, raypath0.getRayParameter());
        distOfP.add(lowerDelta, lower.getRayParameter());
        distOfP.add(higherDelta, higher.getRayParameter());
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
        PolynomialFunction pf = new PolynomialFunction(fitter.fit(distOfP.toList()));
        return pf.value(targetDelta);
    }

}
