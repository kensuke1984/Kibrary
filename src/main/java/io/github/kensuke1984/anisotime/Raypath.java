package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.math.Integrand;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

import static io.github.kensuke1984.kibrary.math.Integrand.bySimpsonRule;
import static io.github.kensuke1984.kibrary.math.Integrand.jeffreysMethod1;

/**
 * Every depth is written as <b>radius [km]</b>. Every angle value returns in
 * [rad]. The raypath is of a given ray parameter p.
 * <p>
 * Cannot compute travel time for structures that have layers with 0 velocity
 * and non zero velocity. Layers must have only non zero velocity or zero
 * velocity.
 * <p>
 * now diffraction phase is computable but only for the raypath which turning R
 * is near the CMB by {@link #permissibleGapForDiff}
 * <p>
 * calculation of a travel time and a epicentral distance region 0 near turning
 * depth (eps +turningDepth) will be calculated by Geffreys region 1 deeper part
 * than event depth but not including region 0 region 2 isShallower part than
 * event depth to the surface
 * <p>
 * &Delta; (delta) denotes epicentral distance.<br>
 * T (time) denotes travel time. &tau; (tau) denotes tau. Q<sub>T</sub>&ne;Q
 * <sub>&tau;</sub>
 * <p>
 * P: P wave in the mantle<br>
 * SV,SH: S wave (SV, SH) in the mantle<br>
 * K: P(K) wave in the outer-core<br>
 * I: P(I) wave in the inner-core<br>
 * JV,JH: SV, SH(J) wave in the inner-core<br>
 * No more JH
 * <p>
 * TODO when the path partially exists. I have to change drastically the structure of dealing with layers each layer has a phase or not
 * <p>
 * TODO cache eventR phase    Tau
 *
 * @author Kensuke Konishi, Anselme Borgeaud
 * @version 0.7.6b
 * @see "Woodhouse, 1981"
 */
public class Raypath implements Serializable, Comparable<Raypath> {
    /**
     * If the gap between the CMB and the turning r is under this value, then
     * diffracted phase can be computed.
     */
    static final double permissibleGapForDiff = 1e-5;
    /**
     * 2020/6/7
     */
    private static final long serialVersionUID = 3791903712513910094L;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Raypath raypath = (Raypath) o;
        return Double.compare(raypath.RAY_PARAMETER, RAY_PARAMETER) == 0 &&
                Objects.equals(WOODHOUSE, raypath.WOODHOUSE) && Objects.equals(MESH, raypath.MESH);
    }

    @Override
    public int hashCode() {
        return Objects.hash(RAY_PARAMETER);
    }

    /**
     * ray parameter [s/rad] dt/d&Delta;
     */
    private final double RAY_PARAMETER;
    private final Woodhouse1981 WOODHOUSE;
    /**
     * Mesh for integration
     */
    private final ComputationalMesh MESH;
    /**
     * &delta;&Delta;<sub>i</sub> at r<sub>i</sub> &le; r &le; r<sub>i+1</sub>
     * (i = 0, 1, ..., n-1)
     * <p>
     * r<sub>i</sub> is in {@link ComputationalMesh}<br>
     * r<sub>0</sub> = core-mantle boundary, r<sub>n</sub> = surface. <u>The
     * number of &delta;&Delta; is 1 less than the number of the mesh.</u>
     * <p>
     * &delta;&Delta; = 0 where the raypath does not reach or the range is in
     * the Jefferey's range.
     */
    private transient Map<PhasePart, double[]> dThetaMap;
    /**
     * &delta;T<sub>i</sub> at r<sub>i</sub>&le; r &le; r<sub>i+1</sub> (i = 0,
     * 1, ..., n-1)
     * <p>
     * r<sub>i</sub> is in {@link ComputationalMesh}<br>
     * r<sub>0</sub> = core-mantle boundary, r<sub>n</sub> = surface. <u>The
     * number of d&Delta; is 1 less than the number of the mesh.</u>
     * <p>
     * &delta;T = 0 where the raypath does not reach or the range is in the
     * Jefferey's range.
     */
    private transient Map<PhasePart, double[]> dTMap;
    /**
     * Radius of bouncing points for all phase parts.
     * TODO turning depth may be many and we have to care of jeffreys as well.
     */
    private transient Map<PhasePart, Double> turningRMap;

    /**
     * &Delta; [rad] of phase parts. Each part is only half way, not including election, bouncing.
     */
    private transient Map<PhasePart, Double> deltaMap;

    /**
     * T [s] (travel time) of phase parts
     */
    private transient Map<PhasePart, Double> timeMap;

    /**
     * &Delta; [rad] (epicentral distance) in the Jeffrey's zone of each phase part(pp).
     * If the pp has no bounce point, the value is NaN.
     */
    private transient Map<PhasePart, Double> jeffreysDeltaMap;

    /**
     * T [s] (travel time) in the Jeffrey's zone of each phase part (pp).
     * If the pp has no bounce point, the value is NaN.
     */
    private transient Map<PhasePart, Double> jeffreysTMap;

    /**
     * Radius [km] of the Jeffrey's boundary for each phase part(pp).
     * The boundary is on mesh. If the pp has no bounce point, the value is NaN.
     */
    private transient Map<PhasePart, Double> jeffreysBoundaryMap;

    /**
     * The source is on the surface. PREM is used.
     *
     * @param rayParameter [s/rad] ray parameter P
     */
    public Raypath(double rayParameter) {
        this(rayParameter, PolynomialStructure.PREM);
    }

    /**
     * @param rayParameter [s/rad] ray parameter P
     * @param structure    {@link VelocityStructure}
     */
    public Raypath(double rayParameter, VelocityStructure structure) {
        this(rayParameter, structure, null);
    }

    /**
     * @param rayParameter [s/rad] ray parameter P
     * @param structure    {@link VelocityStructure}
     * @param mesh         {@link ComputationalMesh}
     */
    public Raypath(double rayParameter, VelocityStructure structure, ComputationalMesh mesh) {
        this(rayParameter, new Woodhouse1981(structure), mesh);
    }

    /**
     * @param rayParameter [s/rad] ray parameter P
     * @param woodhouse    {@link Woodhouse1981}
     * @param mesh         {@link ComputationalMesh}
     */
    Raypath(double rayParameter, Woodhouse1981 woodhouse, ComputationalMesh mesh) {
        if (rayParameter < 0)
            throw new IllegalArgumentException("Input ray parameter must be a non-negative number. " + rayParameter);
        RAY_PARAMETER = rayParameter;
        WOODHOUSE = woodhouse;
        MESH = mesh == null ? ComputationalMesh.simple(woodhouse.getStructure()) : mesh;
        setTurningRs();
        createMaps();
        computeJeffreysRange();
        computeTau();
        computeDelta();
        computeT();
    }

    /**
     * Creates maps of &tau;, &Delta; T and so on..
     */
    private void createMaps() {
        jeffreysBoundaryMap = new ConcurrentHashMap<>();
        jeffreysDeltaMap = new ConcurrentHashMap<>();
        jeffreysTMap = new ConcurrentHashMap<>();
        criticalTauMap = new ConcurrentHashMap<>();
        tauMap = new ConcurrentHashMap<>();
        dTauMap = new ConcurrentHashMap<>();
        deltaMap = new ConcurrentHashMap<>();
        dThetaMap = new ConcurrentHashMap<>();
        timeMap = new ConcurrentHashMap<>();
        dTMap = new ConcurrentHashMap<>();
    }

    /**
     * @return {@link VelocityStructure} of this raypath.
     */
    VelocityStructure getStructure() {
        return WOODHOUSE.getStructure();
    }

    /**
     * Create an information file of {@link Phase}
     *
     * @param informationFile Path of an informationFile
     * @param eventR          [km] radius of event
     * @param phase           Seismic {@link Phase}
     */
    public void outputInfo(Path informationFile, Phase phase, double eventR) {
        try (PrintWriter pw = new PrintWriter(informationFile.toFile())) {
            pw.println("Phase: " + phase);
            pw.println("Ray parameter: " + RAY_PARAMETER);
            pw.println("Epicentral distance[deg]: " + Math.toDegrees(computeDelta(phase, eventR)));
            pw.println("Travel time[s]: " + computeT(phase, eventR));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param dataFile Path of a dataFile
     * @param eventR   [km] radius of event
     * @param phase    to write
     */
    void outputDat(Path dataFile, Phase phase, double eventR) {
        if (Double.isNaN(computeDelta(phase, eventR))) return;
        try (PrintWriter os = new PrintWriter(dataFile.toFile())) {
            double[][] points = getRoute(phase, eventR);
            os.println("#Radius[km] Theta[deg]");
            if (points != null) for (double[] point : points) os.println(point[0] + " " + Math.toDegrees(point[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create an EPS file of {@link Phase}
     *
     * @param epsFile Path of an eps file name
     * @param phase   Seismic {@link Phase}
     * @param eventR  [km] radius of event
     * @param options open options
     * @throws IOException if any
     */
    public void outputEPS(Path epsFile, Phase phase, double eventR, OpenOption... options) throws IOException {
        double delta = computeDelta(phase, eventR);
        double t = computeT(phase, eventR);
        if (Double.isNaN(delta)) return;
        try (BufferedOutputStream os = new BufferedOutputStream(Files.newOutputStream(epsFile, options))) {
            createPanel(phase, eventR).toEPS(os, phase, RAY_PARAMETER, delta, t, eventR);
        }
    }

    /**
     * @param phase  target phase
     * @param eventR [km] radius of the event
     * @return RaypathPanel for the input values
     */
    RaypathPanel createPanel(Phase phase, double eventR) {
        RaypathPanel panel = new RaypathPanel(getStructure());
        double[][] points = getRouteXY(phase, eventR);
        if (points != null) {
            double[] x = new double[points.length];
            double[] y = new double[points.length];
            for (int i = 0; i < points.length; i++) {
                x[i] = points[i][0];
                y[i] = points[i][1];
            }
            panel.addPath(x, y);
        }
        return panel;
    }

    /**
     * When the instance is deserialized, turning Rs and Jeffreys range must be
     * computed. They are done here.
     *
     * @param stream to be read
     * @throws ClassNotFoundException if happens
     * @throws IOException            if any
     * @serialData &Delta; and T (travel time)
     */
    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        int existFlag = stream.readByte();
        int jeffFlag = stream.readByte();
        createMaps();

        for (PhasePart pp : PhasePart.values())
            if ((existFlag & pp.getFlag()) == 0) {
                criticalTauMap.put(pp, Double.NaN);
                tauMap.put(pp, Double.NaN);
                deltaMap.put(pp, Double.NaN);
                timeMap.put(pp, Double.NaN);
            } else {
                criticalTauMap.put(pp, stream.readDouble());
                tauMap.put(pp, stream.readDouble());
                deltaMap.put(pp, stream.readDouble());
                timeMap.put(pp, stream.readDouble());
            }
        for (PhasePart pp : PhasePart.values())
            if ((jeffFlag & pp.getFlag()) == 0) {
                jeffreysBoundaryMap.put(pp, Double.NaN);
                jeffreysDeltaMap.put(pp, Double.NaN);
                jeffreysTMap.put(pp, Double.NaN);
            } else {
                jeffreysBoundaryMap.put(pp, stream.readDouble());
                jeffreysDeltaMap.put(pp, stream.readDouble());
                jeffreysTMap.put(pp, stream.readDouble());
            }
        setTurningRs();
        readDTheta(stream);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        //flags if phase parts exists (1) or not (0)
        AtomicInteger existFlag = new AtomicInteger();
        //flags if a phase bounces (1) or not (0)
        AtomicInteger bounceFlag = new AtomicInteger();
        List<Double> outputList = new ArrayList<>();
        List<Double> jeffList = new ArrayList<>();
        Arrays.stream(PhasePart.values())
                .filter(pp -> !Double.isNaN(deltaMap.get(pp)) || !Double.isNaN(jeffreysBoundaryMap.get(pp)))
                .forEach(pp -> {
                    existFlag.set(existFlag.get() | pp.getFlag());
                    if (!Double.isNaN(jeffreysBoundaryMap.get(pp))) {
                        bounceFlag.set(bounceFlag.get() | pp.getFlag());
                        jeffList.add(jeffreysBoundaryMap.get(pp));
                        jeffList.add(jeffreysDeltaMap.get(pp));
                        jeffList.add(jeffreysTMap.get(pp));
                    }
                    outputList.add(criticalTauMap.get(pp));
                    outputList.add(tauMap.get(pp));
                    outputList.add(deltaMap.get(pp));
                    outputList.add(timeMap.get(pp));
                });
        stream.writeByte(existFlag.get());
        stream.writeByte(bounceFlag.get());
        for (double value : outputList)
            stream.writeDouble(value);
        for (double value : jeffList)
            stream.writeDouble(value);
        writeDTheta(stream);
    }

    private void readDTheta(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        RealVector mantle = MESH.getMesh(Partition.MANTLE);
        double limitR = getStructure().earthRadius() - 700;
        double[] pTheta = new double[mantle.getDimension() - 1];
        double[] svTheta = new double[mantle.getDimension() - 1];
        double[] shTheta = new double[mantle.getDimension() - 1];
        dThetaMap.put(PhasePart.P, pTheta);
        dThetaMap.put(PhasePart.SV, svTheta);
        dThetaMap.put(PhasePart.SH, shTheta);
        double pJeff = jeffreysBoundaryMap.get(PhasePart.P);
        double svJeff = jeffreysBoundaryMap.get(PhasePart.SV);
        double shJeff = jeffreysBoundaryMap.get(PhasePart.SH);
        for (int i = 0; i < mantle.getDimension() - 1; i++) {
            double r = mantle.getEntry(i);
            if (r < limitR) continue;
            if (Double.isNaN(pJeff) || pJeff <= r) pTheta[i] = stream.readDouble();
            if (Double.isNaN(svJeff) || svJeff <= r) svTheta[i] = stream.readDouble();
            if (Double.isNaN(shJeff) || shJeff <= r) shTheta[i] = stream.readDouble();
        }
    }

    private void writeDTheta(ObjectOutputStream stream) throws IOException {
        RealVector mantle = MESH.getMesh(Partition.MANTLE);
        double limitR = getStructure().earthRadius() - 700;
        double[] pTheta = dThetaMap.get(PhasePart.P);
        double[] svTheta = dThetaMap.get(PhasePart.SV);
        double[] shTheta = dThetaMap.get(PhasePart.SH);
        double pJeff = jeffreysBoundaryMap.get(PhasePart.P);
        double svJeff = jeffreysBoundaryMap.get(PhasePart.SV);
        double shJeff = jeffreysBoundaryMap.get(PhasePart.SH);
        for (int i = 0; i < mantle.getDimension() - 1; i++) {
            double r = mantle.getEntry(i);
            if (r < limitR) continue;
            if (Double.isNaN(pJeff) || pJeff <= r) stream.writeDouble(pTheta[i]);
            if (Double.isNaN(svJeff) || svJeff <= r) stream.writeDouble(svTheta[i]);
            if (Double.isNaN(shJeff) || shJeff <= r) stream.writeDouble(shTheta[i]);
        }
    }


    /**
     * @param startR [km]
     * @param endR   [km]
     * @return if [startR, endR] contains any boundaries in VelocityStructure.
     */
    private double[] boundariesIn(double startR, double endR) {
        return Arrays.stream(getStructure().velocityBoundaries()).filter(r -> startR < r && r < endR).toArray();
    }

    /**
     * @param dXdr   {@link DoubleUnaryOperator} to compute q<sub>&Delta;</sub> or q<sub>T</sub>
     * @param startR [km]
     * @param endR   [km]
     * @return the number of layers to have enough mesh for &Delta;
     * where any neighbors are similar (according to {@link ComputationalMesh#EPS})
     */
    private static int computeEnoughMesh(DoubleUnaryOperator dXdr, double startR, double endR) {
        int n = 1;
        for (; n < 100000; ++n) {
            double deltaR = (endR - startR) / n;
            boolean closeEnough = true;
            for (int i = 0; i < n && closeEnough; i++) {
                double r0 = startR + deltaR * i;
                double r1 = startR + deltaR * (i + 1);
                double q0 = dXdr.applyAsDouble(r0);
                double q1 = dXdr.applyAsDouble(r1);
                double ratio = q0 < q1 ? q0 / q1 : q1 / q0;
                closeEnough = INTEGRAL_THRESHOLD < ratio;
                if (Double.isNaN(ratio)) return 0;
            }
            if (closeEnough) break;
        }
        return n;
    }

    /**
     * Use this method when dX/dr at startR and endR are very different.
     * When dXdr is for q<sub>&Delta;</sub>, this method returns &Delta;.
     * When dXdr is for q<sub>T</sub>, this method returns T (travel time).
     *
     * @param dXdr   {@link DoubleUnaryOperator} to compute q<sub>&Delta;</sub> or q<sub>T</sub>
     * @param startR [km]
     * @param endR   [km]
     * @return [s] &Delta; or T (travel time) for startR &le; r &le; endR
     */
    private double simpsonInCriticalRange(DoubleUnaryOperator dXdr, double startR, double endR) {
        int n = computeEnoughMesh(dXdr, startR, endR);
        if (n == 0) return Double.NaN;
        double sum = 0;
        double deltaR = (endR - startR) / n;
        for (int i = 0; i < n; i++)
            sum += simpson(dXdr, startR + i * deltaR, startR + (i + 1) * deltaR);
        return sum;
    }

    /**
     * 2019/12/8 expected value  qtau <0.01
     *
     * @param pp target phase part
     * @return jeffreys boundary for the target pp
     */
    private double computeJeffreysBoundary(PhasePart pp) {
        int index = MESH.getNextIndexOf(turningRMap.get(pp), pp.whichPartition());
        RealVector mesh = MESH.getMesh(pp.whichPartition());
        double turningR = turningRMap.get(pp);
        double boundary = turningR;
        while (++index < mesh.getDimension()) {
            double next = mesh.getEntry(index);
            double q = WOODHOUSE.computeQT(pp, RAY_PARAMETER, boundary);
            double qNext = WOODHOUSE.computeQT(pp, RAY_PARAMETER, next);
            double ratio = q < qNext ? q / qNext : qNext / q;
            if (INTEGRAL_THRESHOLD < ratio && ComputationalMesh.EPS < (next - boundary)) break;
            boundary = next;
        }
        DoubleUnaryOperator rToX = r -> WOODHOUSE.computeQTau(pp, RAY_PARAMETER, r);
        while (0.01 < rToX.applyAsDouble(boundary)) boundary = (boundary + turningR) / 2;
        return boundary;
    }

    /**
     * Range is from the turning point to a radius which is good enough for
     * a given mesh threshold ({@link #INTEGRAL_THRESHOLD}).
     * <p>
     * Each boundary is one of the radius set in {@link #MESH}.
     */
    private void computeJeffreysRange() {
        Arrays.stream(PhasePart.values()).parallel().forEach(pp -> {
            if (Double.isNaN(turningRMap.get(pp))) {
                jeffreysBoundaryMap.put(pp, Double.NaN);
                jeffreysDeltaMap.put(pp, Double.NaN);
                jeffreysTMap.put(pp, Double.NaN);
            } else {
                jeffreysBoundaryMap.put(pp, computeJeffreysBoundary(pp));
                jeffreysDeltaMap.put(pp, computeJeffreys(r -> WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, r), pp));
                jeffreysTMap.put(pp, computeJeffreys(r -> WOODHOUSE.computeQT(pp, RAY_PARAMETER, r), pp));
            }
        });
    }

    /**
     * Computes T [s] (travel time) for the mantle, outer-core and inner-core. It
     * also computes transient arrays of &delta;&Delta; and &delta;T.
     */
    private void computeT() {
        Arrays.stream(PhasePart.values()).parallel().forEach(pp -> {
            RealVector mesh = MESH.getMesh(pp.whichPartition());
            dTMap.computeIfAbsent(pp, p -> computeTransients(pp, r -> WOODHOUSE.computeQT(pp, RAY_PARAMETER, r)));
            double turningR = getTurningR(pp);
            double startR = Double.isNaN(turningR) ? mesh.getEntry(0) : turningR;
            timeMap.put(pp, computeT(pp, startR, mesh.getEntry(mesh.getDimension() - 1)));
        });
    }

    /**
     * Computed transient maps of d&Delta; or dT. (not for d&tau;)
     */
    private double[] computeTransients(PhasePart pp, DoubleUnaryOperator dXdr) {
        RealVector mesh = MESH.getMesh(pp.whichPartition());
        double[] transients = new double[mesh.getDimension() - 1];
        double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
        for (int i = 0; i < transients.length; i++) {
            if (mesh.getEntry(i) < jeffreysBoundary) continue;
            transients[i] = simpson(dXdr, mesh.getEntry(i), mesh.getEntry(i + 1));
        }
        return transients;
    }

    /**
     * @param depth [km] distance from the Earth surface
     * @return [km] radius of the depth
     */
    private double toRadius(double depth) {
        return getStructure().earthRadius() - depth;
    }

    /**
     * @param part   part of path
     * @param eventR [km] radius of the event
     * @return [rad] &Delta; for the part
     */
    private double computeDelta(GeneralPart part, double eventR) {
        PhasePart pp = part.getPhase();
        PassPoint inner = part.getInnerPoint();
        PassPoint outer = part.getOuterPoint();
        boolean innerIsBoundary = PassPoint.isBoundary(inner);
        boolean outerIsBoundary = PassPoint.isBoundary(outer) ||
                (outer == PassPoint.SEISMIC_SOURCE && eventR == getStructure().earthRadius());
        double turningR = getTurningR(pp);
        if (innerIsBoundary && !Double.isNaN(turningR)) return Double.NaN;
        if (innerIsBoundary || (inner == PassPoint.BOUNCE_POINT && !Double.isNaN(turningR))) {
            if (outerIsBoundary) return deltaMap.get(pp);
            else if (outer == PassPoint.SEISMIC_SOURCE)
                return deltaMap.get(pp) - computeDelta(pp, eventR, getStructure().earthRadius());
            else if (!Double.isNaN(deltaMap.get(pp))) return deltaMap.get(pp) -
                    computeDelta(pp, getStructure().earthRadius() - part.getOuterDepth(), getStructure().earthRadius());
        }
        double[] interval = getIntegralInterval(part, eventR);
        if (Double.isNaN(interval[0]) || Double.isNaN(interval[1]) || interval[1] < interval[0]) return Double.NaN;
        return computeDelta(pp, interval[0], interval[1]);
    }

    /**
     * Returns edge values of the interval for a general part.
     *
     * @param eventR radius of a seismic event.
     * @return [0] start, [1] end
     */
    private double[] getIntegralInterval(GeneralPart part, double eventR) {
        double innerR;
        PassPoint inner = part.getInnerPoint();
        PassPoint outer = part.getOuterPoint();
        PhasePart pp = part.getPhase();
        double turningR = turningRMap.get(pp);
        switch (inner) {
            case OTHER:
                innerR = toRadius(part.getInnerDepth());
                break;
            case BOUNCE_POINT:
                innerR = turningR;
                break;
            case SEISMIC_SOURCE:
                innerR = eventR;
                break;
            case CMB:
                innerR = getStructure().coreMantleBoundary();      //TODO
                if (Math.abs(turningR - innerR) < permissibleGapForDiff) innerR = turningR;
                break;
            case ICB:
                innerR = getStructure().innerCoreBoundary();
                break;
            case EARTH_SURFACE:
            default:
                throw new RuntimeException("Unexpected");
        }
        double outerR;
        switch (outer) {
            case OTHER:
                outerR = toRadius(part.getOuterDepth());
                break;
            case BOUNCE_POINT:
                outerR = turningR;
                break;
            case EARTH_SURFACE:
                outerR = getStructure().earthRadius();
                break;
            case SEISMIC_SOURCE:
                outerR = eventR;
                break;
            case CMB:
                outerR = getStructure().coreMantleBoundary();
                break;
            case ICB:
                outerR = getStructure().innerCoreBoundary();
                break;
            default:
                throw new RuntimeException("Unexpected");
        }
        return new double[]{innerR, outerR};
    }

    /**
     * @param part   part of path
     * @param eventR [km] radius of the event
     * @return [s] T (travel time) for the part
     */
    private double computeT(GeneralPart part, double eventR) {
        PhasePart pp = part.getPhase();
        PassPoint inner = part.getInnerPoint();
        PassPoint outer = part.getOuterPoint();
        boolean innerIsBoundary = PassPoint.isBoundary(inner);
        boolean outerIsBoundary = PassPoint.isBoundary(outer) ||
                (outer == PassPoint.SEISMIC_SOURCE && eventR == getStructure().earthRadius());
        double turningR = getTurningR(pp);
        if (innerIsBoundary || (inner == PassPoint.BOUNCE_POINT && !Double.isNaN(turningR))) {
            if (outerIsBoundary) return timeMap.get(pp);
            else if (outer == PassPoint.SEISMIC_SOURCE)
                return timeMap.get(pp) - computeT(pp, eventR, getStructure().earthRadius());
            else if (!Double.isNaN(timeMap.get(pp))) return timeMap.get(pp) -
                    computeT(pp, getStructure().earthRadius() - part.getOuterDepth(), getStructure().earthRadius());
        }
        double[] interval = getIntegralInterval(part, eventR);
        if (Double.isNaN(interval[0]) || Double.isNaN(interval[1]) || interval[1] < interval[0]) return Double.NaN;
        return computeT(pp, interval[0], interval[1]);
    }


    /**
     * @param part   to compute for
     * @param eventR [km] radius at the event
     * @return [rad] &Delta; for the part
     */
    private double computeDelta(PathPart part, double eventR) {
        if (part.isDiffraction()) return ((Diffracted) part).getAngle();
        else if (part.isPropagation()) return computeDelta((GeneralPart) part, eventR);
        else return 0;
    }

    /**
     * @param part   to compute for
     * @param eventR [km] radius at the event
     * @return [s] T (travel time) for the part
     */
    private double computeT(PathPart part, double eventR) {
        if (part.isDiffraction()) {
            Diffracted diffracted = ((Diffracted) part);
            double angle = diffracted.getAngle();
            double r;
            if (diffracted instanceof Located) switch (((Located) diffracted).getPassPoint()) {
                case OTHER:
                    r = toRadius(((Arbitrary) diffracted).getDepth());
                    break;
                case CMB:
                    r = getStructure().coreMantleBoundary();
                    break;
                case ICB:
                    throw new RuntimeException("Still under construction");//TODO
                default:
                case EARTH_SURFACE:
                case BOUNCE_POINT:
                case SEISMIC_SOURCE:
                    throw new RuntimeException("Something wrong related to diffraction");
            }
            else throw new RuntimeException("Something wrong related to diffraction");
            return computeTAlongBoundary(diffracted.getPhase(), r, angle, true);
        } else if (part.isPropagation()) {
            return computeT((GeneralPart) part, eventR);
        } else return 0;
    }

    /**
     * Compute delta for the input {@link Phase}
     *
     * @param phase  Seismic {@link Phase}
     * @param eventR [km] must be in the mantle
     * @return [rad] Epicentral distance(&Delta;) for the phase if the phase does not
     * exist or anything wrong, returns Double.NaN
     */
    public double computeDelta(Phase phase, double eventR) {
        if (getStructure().earthRadius() < eventR || eventR < getStructure().coreMantleBoundary())
            throw new IllegalArgumentException("Event radius (" + eventR + ") must be in the mantle.");
        //TODO because of computeSourceSideDelta
        if (eventR < getStructure().earthRadius() - 700)
            throw new RuntimeException("Not super deep earthquakes yet. The event depth must be shallower than 700");
        PathPart[] parts = phase.getPassParts();
        double delta = 0;
        for (PathPart part : parts)
            delta += computeDelta(part, eventR);
        return delta;
    }

    /**
     * Compute T for the input.
     *
     * @param phase  Seismic {@link Phase}
     * @param eventR [km] must be in the mantle
     * @return [s] T (travel time) for the phase
     */
    public double computeT(Phase phase, double eventR) {
        if (getStructure().earthRadius() < eventR || eventR < getStructure().coreMantleBoundary())
            throw new IllegalArgumentException("Event radius (" + eventR + ") must be in the mantle.");
//        if (!exists(eventR, phase)) return Double.NaN;
        double time = 0;
        PathPart[] parts = phase.getPassParts();
        for (PathPart part : parts)
            time += computeT(part, eventR);
        return time;
    }

    /**
     * @return ray parameter [s/rad] dT/d&Delta;
     */
    public double getRayParameter() {
        return RAY_PARAMETER;
    }

    /**
     * rList must not be empty. &Delta; and T is computed for range rStart &le;
     * r &le; nextR. rStart is the last entry of the rList. (The $Delta; + the last
     * entry of thetaList) is added to the thetaList. T is same. The nextR and
     * the last entry of rList must be in a same partition.
     *
     * @param pp             target {@link PhasePart}
     * @param nextR          [km]
     * @param rList          to add nextR
     * @param thetaList      to add &Delta;
     * @param travelTimeList to add T
     */
    private void addRThetaTime(PhasePart pp, double nextR, LinkedList<Double> rList, LinkedList<Double> thetaList,
                               LinkedList<Double> travelTimeList) {
        double beforeR = rList.getLast();
        double cmbR = getStructure().coreMantleBoundary();
        double earthR = getStructure().earthRadius();
        double icbR = getStructure().innerCoreBoundary();
        if (Math.abs(nextR - turningRMap.get(pp)) < ComputationalMesh.EPS)
            nextR = turningRMap.get(pp) + ComputationalMesh.EPS;
        else if (Math.abs(nextR - icbR) < permissibleGapForDiff)
            nextR = icbR + ComputationalMesh.EPS * (nextR < icbR ? -1 : 1);
        else if (Math.abs(nextR - cmbR) < permissibleGapForDiff)
            nextR = cmbR + ComputationalMesh.EPS * (nextR < cmbR ? -1 : 1);
        else if (Math.abs(nextR - earthR) < permissibleGapForDiff) nextR = earthR - ComputationalMesh.EPS;
        else if (nextR < permissibleGapForDiff) nextR = ComputationalMesh.EPS;
        if (Math.abs(beforeR - icbR) < permissibleGapForDiff)
            beforeR = icbR + permissibleGapForDiff * (nextR < icbR ? -1 : 1);
        else if (Math.abs(beforeR - cmbR) < permissibleGapForDiff)
            beforeR = cmbR + permissibleGapForDiff * (nextR < cmbR ? -1 : 1);
        else if (Math.abs(beforeR - earthR) < permissibleGapForDiff) beforeR = earthR - ComputationalMesh.EPS;

        double smallerR = Math.min(beforeR, nextR);
        double biggerR = Math.max(beforeR, nextR);
        double theta = computeDelta(pp, smallerR, biggerR);
        if (beforeR <= ComputationalMesh.EPS) theta += Math.toRadians(180);
        double time = computeT(pp, smallerR, biggerR);
        rList.add(nextR);
        thetaList.add(theta + thetaList.getLast());
        travelTimeList.add(time + travelTimeList.getLast());
    }

    /**
     * route[i] is a set of radius[i][0], theta[i][1], T[i][2].<br>
     * Note that the i-th theta indicates the &Delta; [rad] between the i-th point and
     * epicenter. T is the same. 0 th point is epicenter.
     * radius, theta and T are in [km], [rad] and [s], respectively.
     *
     * @param phase  Seismic {@link Phase}
     * @param eventR [km] must be in the mantle
     * @return route[point][]{r, theta, T}
     */
    public double[][] getRoute(Phase phase, double eventR) {
        if (getStructure().earthRadius() < eventR || eventR <= getStructure().coreMantleBoundary())
            throw new IllegalArgumentException("Input eventR:" + eventR + " is out of the mantle.");
        if (Double.isNaN(computeDelta(phase, eventR))) throw new RuntimeException(phase + " does not exist.");
        // radius [km]
        LinkedList<Double> rList = new LinkedList<>();
        // theta [rad]
        LinkedList<Double> thetaList = new LinkedList<>();
        // time [s]
        LinkedList<Double> tList = new LinkedList<>();
        PathPart[] passParts = phase.getPassParts();
        for (PathPart part : passParts) {
            if (part.isEmission()) {
                rList.add(eventR);
                thetaList.add(0.);
                tList.add(0.);
            } else if (part.isDiffraction()) {
                Diffracted d = (Diffracted) part;
                double angle = d.getAngle();
                PhasePart pp = d.getPhase();
                double r;
                if (part instanceof Arbitrary) {
                    ArbitraryDiffracted ad = ((ArbitraryDiffracted) part);
                    r = toRadius(ad.getDepth());
                } else if (part instanceof Located) {
                    Located l = (Located) part;
                    r = getStructure().getROf(l.getPassPoint());
                } else throw new RuntimeException("wow unexpected.");
                double time = computeTAlongBoundary(pp, r, angle, d.isShallower());
                double lastTime = tList.getLast() + time;
                double lastAngle = thetaList.getLast() + angle;
                double ONE = Math.toRadians(1);
                double timeONE = computeTAlongBoundary(pp, r, ONE, d.isShallower());
                for (double traveledAngle = 0; traveledAngle < angle; traveledAngle += ONE) {
                    rList.add(r + permissibleGapForDiff * (d.isShallower() ? -1 : 1));
                    thetaList.add(thetaList.getLast() + ONE);
                    tList.add(tList.getLast() + timeONE);
                }
                rList.add(r + permissibleGapForDiff);
                thetaList.add(lastAngle);
                tList.add(lastTime);
            } else if (part.isPropagation()) {
                GeneralPart g = (GeneralPart) part;
                PhasePart pp = g.getPhase();
                Partition partition = g.getPhase().whichPartition();
                double startR = getROf(g, eventR, !g.isDownward()) + (g.isDownward() ? -1 : 1) * ComputationalMesh.EPS;
                double endR = getROf(g, eventR, g.isDownward()) + (g.isDownward() ? 1 : -1) * ComputationalMesh.EPS;
                int startIndex = MESH.getNextIndexOf(startR, partition);
                int endIndex = MESH.getNextIndexOf(endR, partition);
                if (!g.isDownward()) startIndex++;
                RealVector mesh = MESH.getMesh(partition);
                double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
                for (int j = startIndex; j != endIndex; ) {
                    double r = mesh.getEntry(j);
                    //when the path reaches the jeffreys boundary.
                    if (r <= jeffreysBoundary) {
                        if (g.isDownward()) {
                            addRThetaTime(pp, jeffreysBoundary, rList, thetaList, tList);
                            rList.add(endR);
                            thetaList.add(thetaList.getLast() + jeffreysDeltaMap.get(pp));
                            tList.add(tList.getLast() + jeffreysTMap.get(pp));
                            break;
                        } else {
                            while (r < jeffreysBoundary) r = mesh.getEntry(j++);
                            rList.add(jeffreysBoundary + ComputationalMesh.EPS);
                            thetaList.add(thetaList.getLast() + jeffreysDeltaMap.get(pp));
                            tList.add(tList.getLast() + jeffreysTMap.get(pp));
                            if (j == endIndex) break;
                        }
                    }
                    addRThetaTime(pp, r, rList, thetaList, tList);
                    if (g.isDownward()) j--;
                    else j++;
                }
                addRThetaTime(pp, endR, rList, thetaList, tList);
            }
//            else if (part.isBottomsideReflection() || part.isTransmission() || part.isPenetration() ||
//                    part.isBounce() || part.isTopsideReflection()) {
//            } else throw new RuntimeException("an unexpected part");
        }

        double[][] points = new double[rList.size()][3];
        points[0][0] = eventR;
        for (int i = 1; i < points.length; i++) {
            points[i][0] = rList.get(i);
            points[i][1] = thetaList.get(i);
            points[i][2] = tList.get(i);
        }
        return points;
    }

    /**
     * @param part   for the innerR.
     * @param eventR radius [km] of the event
     * @param inner  inner(true) or outer (false)
     * @return radius [km] of the inner(true) or outer (false) point of the part
     */
    private double getROf(GeneralPart part, double eventR, boolean inner) {
        PassPoint point = inner ? part.getInnerPoint() : part.getOuterPoint();
        switch (point) {
            case OTHER:
                return toRadius(inner ? part.getInnerDepth() : part.getOuterDepth());
            case BOUNCE_POINT:
                return turningRMap.get(part.getPhase());
            case EARTH_SURFACE:
            case CMB:
            case ICB:
                return getStructure().getROf(point);
            case SEISMIC_SOURCE:
                return eventR;
            default:
                throw new RuntimeException("Wow, it's unexpected.");
        }
    }

    /**
     * The center of the Earth is (0, 0) Starting point is (0, eventR)
     *
     * @param phase  Seismic {@link Phase}
     * @param eventR [km] radius of event
     * @return [point]{x, y} coordinates of the path
     */
    double[][] getRouteXY(Phase phase, double eventR) {
        if (Double.isNaN(computeDelta(phase, eventR))) {
            System.err.println(phase + " does not exist.");
            return null;
        }
        double[][] rTheta = getRoute(phase, eventR);
        double[][] points = new double[rTheta.length][2];
        for (int i = 0; i < points.length; i++) {
            points[i][0] = rTheta[i][0] * Math.sin(rTheta[i][1]);
            points[i][1] = rTheta[i][0] * Math.cos(rTheta[i][1]);
        }
        return points;
    }


    /**
     * Compute &Delta; or T (travel time) in a Jeffreys radius range by a device of Jeffreys and Jeffreys.
     * In case when the range contains velocity boundaries, use the device for only the depth range
     * from the turningR to the lowermost boundary and compute values by the Simpson's rule for other depths.
     * When dXdr is for q<sub>&Delta;</sub>, this method returns &Delta;.
     * When dXdr is for q<sub>T</sub>, this method returns T (travel time).
     *
     * @param dXdr {@link DoubleUnaryOperator} to compute q<sub>&Delta;</sub> or q<sub>T</sub> for integration
     * @param pp   to compute &Delta; for
     * @return [rad] &Delta; or [s] T (travel time_for Jeffreys radius range.
     */
    private double computeJeffreys(DoubleUnaryOperator dXdr, PhasePart pp) {
        double jeffBoundary = jeffreysBoundaryMap.get(pp);
        double turningR = getTurningR(pp);
        double[] boundaries = boundariesIn(turningR, jeffBoundary - ComputationalMesh.EPS);
        if (boundaries.length == 0) return jeffreys(dXdr, pp, jeffBoundary - ComputationalMesh.EPS);
        double jeff0 = jeffreys(dXdr, pp, boundaries[0] - ComputationalMesh.EPS);
        double simpson = simpsonInCriticalRange(dXdr, boundaries[boundaries.length - 1] + ComputationalMesh.EPS,
                jeffBoundary - ComputationalMesh.EPS);
        for (int i = 0; i < boundaries.length - 1; i++)
            simpson += simpsonInCriticalRange(dXdr, boundaries[i] + ComputationalMesh.EPS,
                    boundaries[i + 1] - ComputationalMesh.EPS);
        return jeff0 + simpson;
    }


    /**
     * Compute &Delta; or T for turningR &le; r &le; endR by a device of Jeffreys and Jeffreys
     * <p>
     * When dXdr is for q<sub>&Delta;</sub>, this method returns &Delta;.
     * When dXdr is for q<sub>T</sub>, this method returns T (travel time).
     *
     * @param dXdr {@link DoubleUnaryOperator} to compute q<sub>&Delta;</sub> or q<sub>T</sub> for integration
     * @param endR [km]
     * @return [rad] &Delta; or [s] T (travel time) for turningR &le; r &le; endR.
     * endR is in many cases jeffreys boundary.
     */
    private double jeffreys(DoubleUnaryOperator dXdr, PhasePart pp, double endR) {
        double turningR = turningRMap.get(pp);
        if (Math.abs(endR - turningR) <= 5 * ComputationalMesh.EPS) return 0;
        DoubleFunction<Double> rToY = r -> dXdr.applyAsDouble(r) * drdx(pp, r);
        double rCenter = (endR + turningR) / 2;
        double modifiedR = rCenter + (toX(pp, endR) / 2 - toX(pp, rCenter)) * drdx(pp, rCenter);
        if (Double.isNaN(modifiedR) || modifiedR <= turningR || endR <= modifiedR) modifiedR = rCenter;
        return jeffreysMethod1(toX(pp, modifiedR), rToY.apply(modifiedR), rToY.apply(endR));
    }


    /**
     * x &equiv; q<sub>&tau;</sub><sup>2</sup>
     *
     * @param r [km]
     * @return x for P
     */
    private double toX(PhasePart pp, double r) {
        switch (pp) {
            case P:
            case I:
            case SV:
            case JV:
            case SH:
//            case JH:
                return Math.pow(WOODHOUSE.computeQTau(pp, RAY_PARAMETER, r), 2);
            case K:
                return 1 -
                        Math.pow(RAY_PARAMETER * Math.sqrt(getStructure().getA(r) / getStructure().getRho(r)) / r, 2);
        }
        throw new RuntimeException("unEXPeCteD");
    }

    private double drdx(PhasePart pp, double r) {
        return ComputationalMesh.EPS / (toX(pp, r) - toX(pp, r - ComputationalMesh.EPS));
    }

    /**
     * This method computes &Delta; with precomputed values. The startR and endR
     * must be in the section (inner-core, outer-core or mantle).
     *
     * @param pp     to compute
     * @param startR [km] must be a positive number
     * @param endR   [km] must be a positive number
     * @return [rad] &Delta; for rStart &le; r &le; rEnd
     */
    private double computeDelta(PhasePart pp, double startR, double endR) {
        if (Double.isNaN(startR + endR) || startR < 0 || endR < 0)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        if (Math.abs(endR - startR) < ComputationalMesh.EPS) return 0;
        Partition partition = pp.whichPartition();
        RealVector radii = MESH.getMesh(partition);
        double minR = radii.getEntry(0);
        double maxR = radii.getEntry(radii.getDimension() - 1);
        if (startR < minR - ComputationalMesh.EPS || endR < startR || maxR + ComputationalMesh.EPS < endR)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        double turningR = getTurningR(pp);
        //Integral interval contains a singular(bounce) point.
        if (startR + ComputationalMesh.EPS < turningR && turningR < endR) return Double.NaN;
        startR = Math.max(startR, minR);
        endR = Math.min(endR, maxR);
        double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
        // the value <= jeffreysBoundary
        double closestSmallerJeffreysBoundaryInMesh = Double.isNaN(jeffreysBoundary) ? Double.NaN :
                radii.getEntry(MESH.getNextIndexOf(jeffreysBoundary, partition));
        double closestLargerJeffreysBoundaryInMesh =
                Double.isNaN(jeffreysBoundary) || Math.abs(jeffreysBoundary - endR) < ComputationalMesh.EPS ||
                        endR < jeffreysBoundary ? Double.NaN :
                        radii.getEntry(MESH.getNextIndexOf(jeffreysBoundary, partition) + 1);
        double jeffreysDelta = jeffreysDeltaMap.get(pp);
        //index of the mesh point next to startR  (startR < mesh[firstIndex]) TODO redundant? when startR is on mesh
        int firstIndexForMemory = MESH.getNextIndexOf(startR, partition) + 1;
        //index of the mesh point next to endR  (mesh[endIndex] <= endR)
        int endIndexForMemory = MESH.getNextIndexOf(endR, partition);

        DoubleUnaryOperator qDelta = r -> WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, r);
        // the case where the integral interval is inside the jeffrey interval TODO
        if (turningR < endR && endR <= jeffreysBoundary)
            return jeffreys(qDelta, pp, endR) - jeffreys(qDelta, pp, startR);

        // the case where integral interval is shorter than mesh grid
        if (endIndexForMemory < firstIndexForMemory) {
            if (turningR < startR && startR <= jeffreysBoundary) {
                double delta = simpson(qDelta, jeffreysBoundary, endR);
                double jeff = jeffreysDelta - jeffreys(qDelta, pp, startR);
                if (Double.isNaN(jeff)) throw new RuntimeException("YoCHECK");
                return delta + jeff;
            }
            return simpson(qDelta, startR, endR);
        }
        double nextREnd = radii.getEntry(endIndexForMemory);
        //outside the nextREnd, if it is inside the jeffreys region, outside the region.
        double delta = simpson(qDelta, nextREnd < jeffreysBoundary ? jeffreysBoundary : nextREnd, endR);
        double[] theta = dThetaMap.computeIfAbsent(pp, p -> computeTransients(pp, qDelta));
        for (int i = firstIndexForMemory; i < endIndexForMemory; i++)
            delta += theta[i];
        if (Double.isNaN(jeffreysBoundary) || jeffreysBoundary <= startR)
            return delta + simpson(qDelta, startR, radii.getEntry(firstIndexForMemory));
        if (closestSmallerJeffreysBoundaryInMesh < jeffreysBoundary && closestLargerJeffreysBoundaryInMesh < nextREnd)
            delta += simpson(qDelta, jeffreysBoundary, closestLargerJeffreysBoundaryInMesh);
        double jeffreys = jeffreysDelta - jeffreys(qDelta, pp, startR);
        return delta + jeffreys;
    }

    /**
     * This method computes travel time by precomputed values. The startR and endR must be in the same partition
     * (mantle or inner-core).
     *
     * @param startR [km] start value of integration
     * @param endR   [km] end value of integration
     * @return [s] T (travel time) for startR &le; r &le; endR
     */
    private double computeT(PhasePart pp, double startR, double endR) {
        if (Double.isNaN(startR + endR) || startR < 0 || endR < 0)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        if (endR - startR < ComputationalMesh.EPS) return 0;
        Partition partition = pp.whichPartition();
        RealVector radii = MESH.getMesh(partition);
        double minR = radii.getEntry(0);
        double maxR = radii.getEntry(radii.getDimension() - 1);
        if (startR < minR - ComputationalMesh.EPS || endR < startR || maxR + ComputationalMesh.EPS < endR)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        double turningR = getTurningR(pp);
        //Integral interval contains a singular(bounce) point.
        if (startR + ComputationalMesh.EPS < turningR && turningR < endR) return Double.NaN;
        startR = Math.max(startR, minR);
        endR = Math.min(endR, maxR);
        double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
        // the value <= jeffreysBoundary
        double closestSmallerJeffreysBoundaryInMesh = Double.isNaN(jeffreysBoundary) ? Double.NaN :
                radii.getEntry(MESH.getNextIndexOf(jeffreysBoundary, partition));
        double closestLargerJeffreysBoundaryInMesh =
                Double.isNaN(jeffreysBoundary) || Math.abs(jeffreysBoundary - endR) < ComputationalMesh.EPS ||
                        endR < jeffreysBoundary ? Double.NaN :
                        radii.getEntry(MESH.getNextIndexOf(jeffreysBoundary, partition) + 1);
        double jeffreysT = jeffreysTMap.get(pp);
        //index of the mesh point next to startR  (startR < mesh[firstIndex])//TODO when startR is on mesh
        int firstIndexForMemory = MESH.getNextIndexOf(startR, partition) + 1;
        //index of the mesh point next to endR (mesh[endIndex]<=endR)
        int endIndexForMemory = MESH.getNextIndexOf(endR, partition);
        DoubleUnaryOperator qT = r -> WOODHOUSE.computeQT(pp, RAY_PARAMETER, r);

        // the case where the integral interval is inside the jeffrey interval TODO
        if (turningR < endR && endR <= jeffreysBoundary) return jeffreys(qT, pp, endR) - jeffreys(qT, pp, startR);
        // the case where integral interval is shorter than mesh grid
        if (endIndexForMemory < firstIndexForMemory) {
            if (turningR < startR && startR <= jeffreysBoundary) {
                double delta = simpson(qT, jeffreysBoundary, endR);
                double jeff = jeffreysT - jeffreys(qT, pp, startR);
                if (Double.isNaN(jeff)) throw new RuntimeException("YoCHECK");
                return delta + jeff;
            }
            return simpson(qT, startR, endR);
        }
        double nextREnd = radii.getEntry(endIndexForMemory);
        //outside the nextREnd, if it is inside the jeffreys region, outside the region.
        double time = simpson(qT, nextREnd < jeffreysBoundary ? jeffreysBoundary : nextREnd, endR);
        double[] t = dTMap.computeIfAbsent(pp, p -> computeTransients(pp, qT));
        for (int i = firstIndexForMemory; i < endIndexForMemory; i++)
            time += t[i];
        if (Double.isNaN(jeffreysBoundary) || jeffreysBoundary <= startR)
            return time + simpson(qT, startR, radii.getEntry(firstIndexForMemory));
        if (closestSmallerJeffreysBoundaryInMesh < jeffreysBoundary && closestLargerJeffreysBoundaryInMesh < nextREnd)
            time += simpson(qT, jeffreysBoundary, closestLargerJeffreysBoundaryInMesh);
        double jeffreys = jeffreysT - jeffreys(qT, pp, startR);
        return time + jeffreys;
    }

    /**
     * TODO confirm no problem
     * <p>
     * This method computes &tau; with precomputed values. The startR and endR
     * must be in the section (inner-core, outer-core or mantle).
     *
     * @param pp     to compute
     * @param startR [km] must be a positive number
     * @param endR   [km] must be a positive number
     * @return [s] &tau; for rStart &le; r &le; rEnd
     */
    private double computeTau(PhasePart pp, double startR, double endR) {
        if (Double.isNaN(startR + endR) || startR < 0 || endR < 0)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        if (Math.abs(endR - startR) < ComputationalMesh.EPS) return 0;
        Partition partition = pp.whichPartition();
        RealVector radii = MESH.getMesh(partition);
        double minR = radii.getEntry(0);
        double maxR = radii.getEntry(radii.getDimension() - 1);
        if (startR < minR - ComputationalMesh.EPS || endR < startR || maxR + ComputationalMesh.EPS < endR)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        double turningR = getTurningR(pp);
        //Integral interval contains a singular(bounce) point.
//        if (startR < turningR && turningR < endR) return Double.NaN;
        if (startR + ComputationalMesh.EPS < turningR && turningR < endR) return Double.NaN;
        startR = Math.max(startR, minR);
        endR = Math.min(endR, maxR);
        //index of the mesh point next to startR  (startR < mesh[firstIndex])
        int firstIndexForMemory = MESH.getNextIndexOf(startR, partition) + 1;//TODO
        //index of the mesh point next to endR  (mesh[endIndex]<=endR)
        int endIndexForMemory = MESH.getNextIndexOf(endR, partition);

        DoubleUnaryOperator qTau = r -> WOODHOUSE.computeQTau(pp, RAY_PARAMETER, r);

        // the case where the integral interval is inside the jeffrey interval TODO

        // the case where integral interval is shorter than mesh grid
        if (endIndexForMemory < firstIndexForMemory) return simpson(qTau, startR, endR);

        double nextREnd = radii.getEntry(endIndexForMemory);
        //outside the nextREnd
        double tau = simpson(qTau, nextREnd, endR);

        double[] dTau = dTauMap.computeIfAbsent(pp, this::computeTransientTau);
        for (int i = firstIndexForMemory; i < endIndexForMemory; i++)
            tau += dTau[i];
        return tau + startR == turningR ? criticalTauMap.get(pp) :
                simpson(qTau, startR, radii.getEntry(firstIndexForMemory));
    }

    /**
     * Computes &Delta; for the mesh and compute the ones for the mantle,
     * outer-core and inner-core. If one does not exist, the value is default
     * ({@link Double#NaN}).
     * <p>
     */
    private void computeDelta() {
        Arrays.stream(PhasePart.values()).parallel().forEach(pp -> {
            RealVector mesh = MESH.getMesh(pp.whichPartition());
            dThetaMap.computeIfAbsent(pp,
                    p -> computeTransients(pp, r -> WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, r)));
            double turningR = getTurningR(pp);
            double startR = Double.isNaN(turningR) ? mesh.getEntry(0) : turningR;
            //TODO diffraction
            deltaMap.put(pp, computeDelta(pp, startR, mesh.getEntry(mesh.getDimension() - 1)));
            if (RAY_PARAMETER == 0 && (pp == PhasePart.I || pp == PhasePart.JV))
                deltaMap.put(pp, dThetaMap.get(pp)[0] = Math.PI / 2);
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////// TODO
//////////////////////////////////////////////////<----------------------------tau develop
///////////////////////////////////////////////////////////////////////////////////////
    /**
     * &tau; for each phase part (pp)
     */
    private transient Map<PhasePart, Double> tauMap;
    /**
     * &delta;&tau; in each layer for each pp
     */
    private transient Map<PhasePart, double[]> dTauMap;

    double computeTau(Phase phase, double eventR) {
        if (getStructure().earthRadius() < eventR || eventR < getStructure().coreMantleBoundary())
            throw new IllegalArgumentException("Event radius (" + eventR + ") must be in the mantle.");
        PathPart[] parts = phase.getPassParts();
        double tau = 0;
        for (PathPart part : parts)
            tau += computeTau(part, eventR);
        return tau;
    }

    /**
     * @param part   part of path
     * @param eventR [km] radius of the event
     * @return [s] &tau; for the part
     */
    private double computeTau(GeneralPart part, double eventR) {
        PhasePart pp = part.getPhase();
        PassPoint inner = part.getInnerPoint();
        PassPoint outer = part.getOuterPoint();
        boolean innerIsBoundary = PassPoint.isBoundary(inner);
        boolean outerIsBoundary = PassPoint.isBoundary(outer) ||
                (outer == PassPoint.SEISMIC_SOURCE && eventR == getStructure().earthRadius());
        double turningR = getTurningR(pp);
        if (innerIsBoundary && !Double.isNaN(turningR)) return Double.NaN;
        if (outerIsBoundary)
            if (innerIsBoundary || (inner == PassPoint.BOUNCE_POINT && !Double.isNaN(turningR))) return tauMap.get(pp);
        double[] interval = getIntegralInterval(part, eventR);
        if (Double.isNaN(interval[0]) || Double.isNaN(interval[1]) || interval[1] < interval[0]) return Double.NaN;
        return computeTau(pp, interval[0], interval[1]);
    }

    /**
     * @param part   to compute for
     * @param eventR [km] radius at the event
     * @return &tau; for the part
     */
    private double computeTau(PathPart part, double eventR) {
        if (part.isDiffraction()) throw new RuntimeException("madamadamada"); //TODO
        else if (part.isPropagation()) return computeTau((GeneralPart) part, eventR);
        else return 0;
    }

    /**
     * Computes an epicentral distance &Delta; for a raypath with two raypaths via &tau;.
     *
     * @param phase         target phase
     * @param eventR        [km] event radius
     * @param ray0          raypath for computation, ray parameter must be smaller than that of a target raypath.
     * @param targetRaypath target raypath for the &Delta
     * @param ray2          raypath for computation, ray parameter must be larger than that of a target raypath.
     * @return [rad] &Delta; computed using input three raypaths.
     */
    static double computeDelta(Phase phase, double eventR, Raypath ray0, Raypath targetRaypath, Raypath ray2) {
        if (targetRaypath.getRayParameter() <= ray0.RAY_PARAMETER || ray2.RAY_PARAMETER <= targetRaypath.RAY_PARAMETER)
            throw new IllegalArgumentException(
                    "Ray parameters must be p0<p1<p2. Input parameters are " + ray0 + " " + targetRaypath + " " + ray2);
        if (phase.isDiffracted()) throw new RuntimeException("Diffraction waves meibangfa");
        double tau0 = ray0.computeTau(phase, eventR);
        double tau1 = targetRaypath.computeTau(phase, eventR);
        double tau2 = ray2.computeTau(phase, eventR);
        double dTaudP10 = -(tau1 - tau0) / (targetRaypath.RAY_PARAMETER - ray0.RAY_PARAMETER);
        double dTaudP21 = -(tau2 - tau1) / (ray2.RAY_PARAMETER - targetRaypath.RAY_PARAMETER);
//        if (RaypathCatalog.DEFAULT_MAXIMUM_D_DELTA < Math.abs(dTaudP10 - dTaudP21)) throw new RuntimeException(
//                "Delta from \u03C4 may have problem, probably because of a velocity jump. The gap of d\u03C4/dP: " +
//                        Math.toDegrees(Math.abs(dTaudP10 - dTaudP21)));
        return (dTaudP10 + dTaudP21) / 2;
    }

    /**
     * Computes a epicentral distance &Deleta; for a raypath with two raypaths via &tau;.
     *
     * @param phase         target phase
     * @param eventR        [km] event radius
     * @param ray0          raypath for computation, ray parameter must be smaller than that of a target raypath.
     * @param targetRaypath target raypath for the &Delta
     * @param ray2          raypath for computation, ray parameter must be larger than that of a target raypath.
     * @return [s] T computed using input three raypaths.
     */
    static double computeT(Phase phase, double eventR, Raypath ray0, Raypath targetRaypath, Raypath ray2) {
        if (targetRaypath.getRayParameter() <= ray0.RAY_PARAMETER || ray2.RAY_PARAMETER <= targetRaypath.RAY_PARAMETER)
            throw new IllegalArgumentException(
                    "ray parameters must be p0 < p1 < p2. Input parameters are " + ray0 + " " + targetRaypath + " " +
                            ray2);
        return targetRaypath.computeTau(phase, eventR) +
                targetRaypath.RAY_PARAMETER * computeDelta(phase, eventR, ray0, targetRaypath, ray2);
    }

    /**
     * &tau; for critical ranges.
     * a critical range for a phase part is from a bounce point to next mesh grid.
     * If there is no bounce point for a phase part, the value for the pp is NaN.
     */
    private transient Map<PhasePart, Double> criticalTauMap;

    private void computeTau() {
        Arrays.stream(PhasePart.values()).parallel().forEach(pp -> {
            RealVector mesh = MESH.getMesh(pp.whichPartition());
            double turningR = getTurningR(pp);
            int startIndex = Double.isNaN(turningR) ? 0 : MESH.getNextIndexOf(turningR, pp.whichPartition()) + 1;
            DoubleUnaryOperator computeQTau = r -> WOODHOUSE.computeQTau(pp, RAY_PARAMETER, r);
            if (!Double.isNaN(turningR) && !Double.isNaN(computeQTau.applyAsDouble(mesh.getEntry(startIndex)))) {
                double minimumR = turningR;
                if (mesh.getEntry(startIndex) - turningR < ComputationalMesh.EPS) criticalTauMap.put(pp, 0d);
                else {
                    while (Double.isNaN(computeQTau.applyAsDouble(minimumR))) minimumR += ComputationalMesh.EPS; //TODO
                    double cut = (mesh.getEntry(startIndex) - minimumR) / 2; //TODO
                    criticalTauMap.put(pp, simpson(r -> WOODHOUSE.computeQTau(pp, RAY_PARAMETER, r), minimumR + cut,
                            mesh.getEntry(startIndex)));
                }
            } else criticalTauMap.put(pp, Double.NaN);
            dTauMap.computeIfAbsent(pp, this::computeTransientTau);
//            tauMap.put(pp, computeTau(pp, Double.isNaN(turningR) ? mesh.getEntry(0) : turningR,
//                    mesh.getEntry(mesh.getDimension() - 1))); TODO TAU
            tauMap.put(pp,Double.NaN);
        });
    }

    /**
     * Computed transient maps of d&Delta; or dT. (not for d&tau;)
     */
    private double[] computeTransientTau(PhasePart pp) {
        RealVector mesh = MESH.getMesh(pp.whichPartition());
        double turningR = getTurningR(pp);
        int startIndex = Double.isNaN(turningR) ? 0 : MESH.getNextIndexOf(turningR, pp.whichPartition()) + 1;
        double[] dTau = new double[mesh.getDimension() - 1];
        for (int i = startIndex; i < dTau.length; i++)
            dTau[i] = simpson(r -> WOODHOUSE.computeQTau(pp, RAY_PARAMETER, r), mesh.getEntry(i), mesh.getEntry(i + 1));
        return dTau;
    }

/////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////<----------------------------tau develop
///////////////////////////////////////////////////////////////////////////////////////

    /**
     * computes turning radius
     */
    private void setTurningRs() {
        turningRMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        if (RAY_PARAMETER == 0) {
            Arrays.stream(PhasePart.values()).forEach(pp -> turningRMap.put(pp, Double.NaN));
            turningRMap.put(PhasePart.I, 0d);
            turningRMap.put(PhasePart.JV, 0d);
            return;
        }
        Arrays.stream(PhasePart.values())
                .forEach(pp -> turningRMap.put(pp, getStructure().getTurningR(pp, RAY_PARAMETER)));
    }

    /**
     * Information to Standard write. Note that if the method is called before
     * this is computed, an exception happens.
     */
    public void printInfo() {
        System.out.println("#Phase:Turning points[km] Jeffrey boundary[km] Propagation delta[deg] time[s]");
        Arrays.stream(PhasePart.values()).forEach(pp -> System.out.println(
                pp + ": " + Precision.round(turningRMap.get(pp), 3) + " " + jeffreysBoundaryMap.get(pp) + " " +
                        Precision.round(Math.toDegrees(deltaMap.get(pp)), 3) + " " +
                        Precision.round(timeMap.get(pp), 3)));
    }

    /**
     * @param pp target phase part
     * @return [km] radius of turning point
     */
    public double getTurningR(PhasePart pp) {
        return turningRMap.get(pp);
    }

    /**
     * When dXdr is for q<sub>&Delta;</sub>, this method returns &Delta;.
     * When dXdr is for q<sub>T</sub>, this method returns T (travel time).
     *
     * @param dXdr   {@link DoubleUnaryOperator} to compute q<sub>&Delta;</sub> or q<sub>T</sub> for integration
     * @param startR [km]
     * @param endR   [km]
     * @return [rad] &Delta; or [s] T (travel time) for startR &le; r &le; endR
     * @see Integrand#bySimpsonRule(double, double, double, double)
     */
    private double simpson(DoubleUnaryOperator dXdr, double startR, double endR) {
        double deltaX = endR - startR;
        if (deltaX < ComputationalMesh.EPS) return 0;
        double a = dXdr.applyAsDouble(startR);
        double b = dXdr.applyAsDouble(startR + 0.5 * deltaX);
        double c = dXdr.applyAsDouble(endR);
        if (Double.isNaN(a + c)) return Double.isNaN(b) ? Double.NaN : 0; //TODO
        double ratio = a < c ? a / c : c / a;
        if (INTEGRAL_THRESHOLD < ratio) return bySimpsonRule(a, b, c, deltaX);
        if (a + b + c == 0) return 0;
        double width = 0.01 * deltaX;
        return simpsonInCriticalRange(dXdr, startR, startR + width) +
                simpsonInCriticalRange(dXdr, startR + width, endR);
//        return simpsonInCriticalRange(dXdr, startR, endR);
    }

    /**
     * Threshold for the integration. This value (ratio) must be positive and
     * less than 1. If it is a, the difference between two Q<sub>T</sub> at
     * adjacent points must be with in a. a &lt; Q<sub>T</sub> (i)/Q<sub>T</sub>
     * (i+1) &lt; 1/a
     */
    private static final double INTEGRAL_THRESHOLD = 0.9;

    /**
     * Computes diffraction on a boundary at r. The velocity is considered as
     * the one at r &pm; {@link ComputationalMesh#EPS}
     *
     * @param pp              phase part for the diffraction
     * @param boundaryR       [km]
     * @param deltaOnBoundary [rad]
     * @param shallower       The diffraction happens on the isShallower(true) or
     *                        deeper(false) side of the boundary. Shallower means larger
     *                        radius.
     * @return [s] travel time along the boundary.
     * @throws RuntimeException If the structure has no boundary at the input boundaryR.
     */
    private double computeTAlongBoundary(PhasePart pp, double boundaryR, double deltaOnBoundary, boolean shallower) {
        if (ComputationalMesh.EPS < Math.abs(getStructure().coreMantleBoundary() - boundaryR) &&
                ComputationalMesh.EPS < Math.abs(getStructure().innerCoreBoundary() - boundaryR) &&
                Arrays.stream(getStructure().velocityBoundaries())
                        .allMatch(b -> ComputationalMesh.EPS < Math.abs(b - boundaryR)))
            throw new IllegalArgumentException("Input radius " + boundaryR + " is not a boundary.");
        double r = boundaryR + (shallower ? ComputationalMesh.EPS : -ComputationalMesh.EPS);
        double s = boundaryR * deltaOnBoundary;
        double numerator;
        switch (pp) {
            case I:
            case K:
            case P:
                numerator = getStructure().getA(r);
                break;
//            case JH:
            case SH:
                numerator = getStructure().getN(r);
                break;
            case SV:
            case JV:
                numerator = getStructure().getL(r);
                break;
            default:
                throw new RuntimeException("UNEXPECTED");
        }
        double velocity = Math.sqrt(numerator / getStructure().getRho(r));
        return s / velocity;
    }

    @Override
    public int compareTo(Raypath o) {
        return Double.compare(RAY_PARAMETER, o.RAY_PARAMETER);
    }
    
    /**
     * Compute the angle (in radian) between the raypath and the local vertical.
     * @param pp
     * @param rayParameter
     * @param r
     * @return angle in radian
     */
    public double computeIncidentAngle(PhasePart pp, double rayParameter, double r) {
    	double qDelta = WOODHOUSE.computeQDelta(pp, rayParameter, r);
    	return Math.atan(r * qDelta);
    }
}
