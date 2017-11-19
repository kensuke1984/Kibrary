package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.math.Integrand;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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
 * <p>
 * TODO when the path partially exists. I have to change drastically the structure of dealing with layers each layer has a phase or not
 * <p>
 * <p>
 * TODO cache eventR phase
 *
 * @author Kensuke Konishi
 * @version 0.5.0b
 * @see "Woodhouse, 1981"
 */
public class Raypath implements Serializable, Comparable<Raypath> {
    /**
     * If the gap between the CMB and the turning r is under this value, then
     * diffracted phase can be computed.
     */
    static final double permissibleGapForDiff = 1e-5;
    /**
     * 2017/11/19
     */
    private static final long serialVersionUID = 3501567256448143684L;


    private final double RAY_PARAMETER; // ray parameter p = (r * sin(t) )/ v(r)
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
     * &Delta; of phase parts. Each part is only half way, not including election, bouncing.
     */
    private transient Map<PhasePart, Double> deltaMap;

    /**
     * T of phase parts
     */
    private transient Map<PhasePart, Double> timeMap;

    private transient Map<PhasePart, Double> jeffreysDeltaMap;

    private transient Map<PhasePart, Double> jeffreysTMap;

    /**
     * Jeffreys boundary for each phase part.
     * The boundary is on mesh. If the phase penetrates a part, the value is {@link Double#NaN}
     */
    private transient Map<PhasePart, Double> jeffreysBoundaryMap;
    /**
     * If this method has &Delta; and T for partitions.
     */
    private boolean isComputed;
    /**
     * If this has transient values the route theta and time.
     */
    private transient boolean hasTransients;

    /**
     * ray parameter p the source is on the surface PREM structure
     *
     * @param rayParameter a ray parameter
     */
    public Raypath(double rayParameter) {
        this(rayParameter, PolynomialStructure.PREM);
    }

    /**
     * @param rayParameter ray parameter P
     * @param structure    {@link VelocityStructure}
     */
    public Raypath(double rayParameter, VelocityStructure structure) {
        this(rayParameter, structure, null);
    }

    /**
     * @param rayParameter ray parameter P
     * @param structure    {@link VelocityStructure}
     * @param mesh         {@link ComputationalMesh}
     */
    public Raypath(double rayParameter, VelocityStructure structure, ComputationalMesh mesh) {
        this(rayParameter, new Woodhouse1981(structure), mesh);
    }

    /**
     * @param rayParameter ray parameter P
     * @param woodhouse    {@link Woodhouse1981}
     * @param mesh         {@link ComputationalMesh}
     */
    Raypath(double rayParameter, Woodhouse1981 woodhouse, ComputationalMesh mesh) {
        if (rayParameter < 0)
            throw new RuntimeException("Input ray parameter must be a non-negative number. " + rayParameter);
        RAY_PARAMETER = rayParameter;
        WOODHOUSE = woodhouse;
        MESH = mesh == null ? ComputationalMesh.simple(woodhouse.getStructure()) : mesh;
        setTurningRs();
        computeJeffreysRange();
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
    public void outputInfo(Path informationFile, double eventR, Phase phase) {
        try (PrintWriter pw = new PrintWriter(informationFile.toFile())) {
            pw.println("Phase: " + phase);
            pw.println("Ray parameter: " + RAY_PARAMETER);
            pw.println("Epicentral distance[deg]: " + Math.toDegrees(computeDelta(eventR, phase)));
            pw.println("Travel time[s]: " + computeT(eventR, phase));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param dataFile Path of a dataFile
     * @param eventR   [km] radius of event
     * @param phase    to write
     */
    void outputDat(Path dataFile, double eventR, Phase phase) {
        if (Double.isNaN(computeDelta(eventR, phase))) return;
        try (PrintWriter os = new PrintWriter(dataFile.toFile())) {
            double[][] points = getRoute(eventR, phase);
            os.println("#Radius[km] Theta[deg]");
            if (points != null) for (double[] point : points) os.println(point[0] + " " + Math.toDegrees(point[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create an EPS file of {@link Phase}
     *
     * @param eventR  [km] radius of event
     * @param phase   Seismic {@link Phase}
     * @param epsFile Path of an eps file name
     * @param options open options
     * @throws IOException if any
     */
    public void outputEPS(double eventR, Phase phase, Path epsFile, OpenOption... options) throws IOException {
        double delta = computeDelta(eventR, phase);
        double t = computeT(eventR, phase);
        if (Double.isNaN(delta)) return;
        try (BufferedOutputStream os = new BufferedOutputStream(Files.newOutputStream(epsFile, options))) {
            createPanel(eventR, phase).toEPS(os, phase, RAY_PARAMETER, delta, t, eventR);
        }
    }

    /**
     * @param eventR [km] radius of the event
     * @param phase  target phase
     * @return RaypathPanel for the input values
     */
    RaypathPanel createPanel(double eventR, Phase phase) {
        RaypathPanel panel = new RaypathPanel(getStructure());
        double[][] points = getRouteXY(eventR, phase);
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
     * Compute travel times and deltas in layers. This method must be done
     * before {@link #computeDelta(double, Phase)} or
     * {@link #computeT(double, Phase)}. If once this method is called, it does
     * not compute anymore in the future.
     */
    public void compute() {
        if (isComputed) return;
        synchronized (this) {
            if (isComputed) return;
            computeTransients();
            isComputed = true;
            hasTransients = true;
        }
    }

    private void computeTransients() {
        if (hasTransients) return;
        synchronized (this) {
            if (hasTransients) return;
            computeDelta();
            computeT();
            isComputed = true;
            hasTransients = true;
        }
    }

    /**
     * When the instance is deserialized, turning Rs and Jeffreys range must be
     * computed. They are done here.
     *
     * @param stream to be read
     * @throws ClassNotFoundException if happens
     * @throws IOException            if any
     * @serialData
     */
    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        int flag = stream.readByte();
        deltaMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        timeMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        for (PhasePart pp : PhasePart.values())
            if ((flag & pp.getFlag()) == 0) {
                deltaMap.put(pp, Double.NaN);
                timeMap.put(pp, Double.NaN);
            } else {
                deltaMap.put(pp, stream.readDouble());
                timeMap.put(pp, stream.readDouble());
            }

        setTurningRs();
        computeJeffreysRange();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        //flags of phases inside
        AtomicInteger ai = new AtomicInteger();
        List<Double> outputList = new ArrayList<>();
        Arrays.stream(PhasePart.values()).filter(pp -> !Double.isNaN(deltaMap.get(pp) + timeMap.get(pp)))
                .forEach(pp -> {
                    ai.set(ai.get() | pp.getFlag());
                    outputList.add(deltaMap.get(pp));
                    outputList.add(timeMap.get(pp));
                });
        stream.writeByte(ai.get());
        for (double value : outputList)
            stream.writeDouble(value);
    }

    /**
     * TODO Range is from the turning point to a radius which is good enough for
     * a given mesh threshold ({@link ComputationalMesh#INTEGRAL_THRESHOLD}).
     * <p>
     * Each boundary is one of the radius set in {@link #MESH}.
     */
    private void computeJeffreysRange() {
        jeffreysBoundaryMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        jeffreysDeltaMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        jeffreysTMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));

        Arrays.stream(PhasePart.values()).forEach(pp -> {
            jeffreysBoundaryMap.put(pp, Double.NaN);
            jeffreysDeltaMap.put(pp, Double.NaN);
            jeffreysTMap.put(pp, Double.NaN);
        });

        Consumer<PhasePart> compute = pp -> {
            if (Double.isNaN(turningRMap.get(pp))) return;
            int index = MESH.getNextIndexOf(turningRMap.get(pp), pp.whichPartition());
            RealVector mesh = MESH.getMesh(pp.whichPartition());
            double boundary = mesh.getEntry(index);
            while (++index < mesh.getDimension()) {
                double next = mesh.getEntry(index);
                double q = WOODHOUSE.computeQT(pp, RAY_PARAMETER, boundary);
                double qNext = WOODHOUSE.computeQT(pp, RAY_PARAMETER, next);
                double ratio = q < qNext ? q / qNext : qNext / q;
                if (MESH.INTEGRAL_THRESHOLD < ratio) break;
                boundary = next;
            }
            jeffreysBoundaryMap.put(pp, boundary);
            jeffreysDeltaMap.put(pp, jeffreysDelta(pp, boundary));
            jeffreysTMap.put(pp, jeffreysT(pp, boundary));
        };
        Arrays.stream(PhasePart.values()).forEach(compute);
    }

    /**
     * Computes T (travel time) for the mantle, outer-core and inner-core. It
     * also computes transient arrays of &delta;&Delta; and &delta;T.
     */
    private void computeT() {
        timeMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        Arrays.stream(PhasePart.values()).forEach(pp -> timeMap.put(pp, Double.NaN));
        Function<PhasePart, Thread> createThread = pp -> {
            RealVector mesh = MESH.getMesh(pp.whichPartition());
            double[] dT = new double[mesh.getDimension() - 1];
            dTMap.put(pp, dT);
            return new Thread(() -> {
                double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
                for (int i = 0; i < dT.length; i++) {
                    if (mesh.getEntry(i) < jeffreysBoundary) continue;
                    dT[i] = simpsonT(pp, mesh.getEntry(i), mesh.getEntry(i + 1));
                }
                double turningR = getTurningR(pp);
                double startR = Double.isNaN(turningR) ? mesh.getEntry(0) : turningR;
                timeMap.put(pp, computeT(pp, startR, mesh.getEntry(dT.length)));
            });
        };

        dTMap = new EnumMap<>(PhasePart.class);
        List<Thread> runningThread =
                Arrays.stream(PhasePart.values())//.filter(pp -> propagationMap.get(pp) != Propagation.NOEXIST)
                        .map(createThread).collect(Collectors.toList());

        runningThread.forEach(Thread::start);

        try {
            for (Thread t : runningThread)
                t.join();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not compute travel time.");
        }
    }


    /**
     * @param depth [km] distance from the Earth surface
     * @return [km] radius of the depth
     */
    private double toRadius(double depth) {
        return WOODHOUSE.getStructure().earthRadius() - depth;
    }

    /**
     * @param eventR [km] radius of the event
     * @param part   part of path
     * @return [rad] delta for the part
     */
    private double computeDelta(double eventR, GeneralPart part) {
        PhasePart phase = part.getPhase();
        PassPoint inner = part.getInnerPoint();
        PassPoint outer = part.getOuterPoint();
        boolean innerIsBoundary = PassPoint.isBoundary(inner);
        boolean outerIsBoundary = PassPoint.isBoundary(outer);
        double delta = deltaMap.get(phase);
        if (innerIsBoundary && outerIsBoundary) return delta;
        double turningR = getTurningR(phase);
        double innerR;
        switch (inner) {
            case OTHER:
                innerR = toRadius(part.getInnerDepth());
                break;
            case BOUNCE_POINT:
                innerR = turningR;
                if (Double.isNaN(innerR)) return Double.NaN;
                break;
            case SEISMIC_SOURCE:
                innerR = eventR;
                break;
            case CMB:
                innerR = getStructure().coreMantleBoundary();
                //TODO
                if (Math.abs(turningR - innerR) < permissibleGapForDiff) innerR = turningR;
                break;
            case ICB:
                innerR = getStructure().innerCoreBoundary();
                break;
            case EARTH_SURFACE:
            default:
                throw new RuntimeException("soteigai");
        }
        double outerR;
        switch (outer) {
            case OTHER:
                outerR = toRadius(part.getOuterDepth());
                break;
            case BOUNCE_POINT:
                outerR = turningR;
                if (Double.isNaN(outerR)) return Double.NaN;
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
                throw new RuntimeException("soteigai");
        }
        return computeDelta(phase, innerR, outerR);
    }

    /**
     * @param eventR [km] radius of the event
     * @param part   part of path
     * @return [s] travel time for the part
     */
    private double computeT(double eventR, GeneralPart part) {
        PhasePart phase = part.getPhase();
        PassPoint inner = part.getInnerPoint();
        PassPoint outer = part.getOuterPoint();
        boolean innerIsBoundary = PassPoint.isBoundary(inner);
        boolean outerIsBoundary = PassPoint.isBoundary(outer);


        double time = timeMap.get(phase);
        if (innerIsBoundary && outerIsBoundary) return time;
        double turningR = getTurningR(phase);
        double innerR;
        switch (inner) {
            case OTHER:
                innerR = toRadius(part.getInnerDepth());
                break;
            case BOUNCE_POINT:
                innerR = turningR;
                if (Double.isNaN(innerR)) return Double.NaN;
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
                throw new RuntimeException("soteigai");
        }
        double outerR;
        switch (outer) {
            case OTHER:
                outerR = toRadius(part.getOuterDepth());
                break;
            case BOUNCE_POINT:
                outerR = turningR;
                if (Double.isNaN(outerR)) return Double.NaN;
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
                throw new RuntimeException("soteigai");
        }
        return computeT(phase, innerR, outerR);
    }


    /**
     * @param eventR [km] radius at the event
     * @param part   to compute for
     * @return [rad] delta for the part
     */
    private double computeDelta(double eventR, PathPart part) {
        if (part.isDiffraction()) return ((Diffracted) part).getAngle();
        else if (part.isPropagation()) return computeDelta(eventR, (GeneralPart) part);
        else return 0;
    }

    /**
     * @param eventR [km] radius at the event
     * @param part   to compute for
     * @return [s] travel time for the part
     */
    private double computeT(double eventR, PathPart part) {
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
                    throw new RuntimeException("Something wrong related to diffraction");
                default:
                case EARTH_SURFACE:
                case BOUNCE_POINT:
                case SEISMIC_SOURCE:
                    throw new RuntimeException("Something wrong related to diffraction");
            }
            else throw new RuntimeException("Something wrong related to diffraction");
            return computeTAlongBoundary(diffracted.getPhase(), r, angle, true);
        } else if (part.isPropagation()) {
            return computeT(eventR, (GeneralPart) part);
        } else return 0;
    }

    /**
     * Compute delta for the input {@link Phase}
     *
     * @param eventR [km] must be in the mantle
     * @param phase  Seismic {@link Phase}
     * @return [rad] Epicentral distance(&Delta;) for the phase if the phase does not
     * exist or anything wrong, returns Double.NaN
     */
    public double computeDelta(double eventR, Phase phase) {
        if (!isComputed) throw new RuntimeException("Not computed yet.");
        if (getStructure().earthRadius() < eventR || eventR < getStructure().coreMantleBoundary())
            throw new IllegalArgumentException("Event radius (" + eventR + ") must be in the mantle.");
        PathPart[] parts = phase.getPassParts();
        double delta = 0;
        for (PathPart part : parts)
            delta += computeDelta(eventR, part);
        return delta;
    }

    /**
     * Compute T for the input.
     *
     * @param eventR [km] must be in the mantle
     * @param phase  Seismic {@link Phase}
     * @return [s] travel time for the phase
     */
    public double computeT(double eventR, Phase phase) {
        if (!isComputed) throw new RuntimeException("Not computed yet.");
        if (getStructure().earthRadius() < eventR || eventR < getStructure().coreMantleBoundary())
            throw new IllegalArgumentException("Event radius (" + eventR + ") must be in the mantle.");
//        if (!exists(eventR, phase)) return Double.NaN;
        double time = 0;
        PathPart[] parts = phase.getPassParts();
        for (PathPart part : parts)
            time += computeT(eventR, part);
        return time;
    }

    /**
     * @return ray parameter
     */
    public double getRayParameter() {
        return RAY_PARAMETER;
    }

    /**
     * rList must not be empty. &Delta; and T is computed for range rStart &le;
     * r &le; nextR. rStart is the last entry of rList. (The $Delta; + the last
     * entry of thetaList) is added to the thetaList. T is same. The nextR and
     * the last entry of rList must be in a same partition.
     *
     * @param nextR          [km]
     * @param rList          to add nextR
     * @param thetaList      to add &Delta;
     * @param travelTimeList to add T
     */
    private void addRThetaTime(double nextR, PhasePart pp, LinkedList<Double> rList, LinkedList<Double> thetaList,
                               LinkedList<Double> travelTimeList) {
        double beforeR = rList.getLast();
        double cmbR = getStructure().coreMantleBoundary();
        double earthR = getStructure().earthRadius();
        double icbR = getStructure().innerCoreBoundary();
        if (Math.abs(nextR - turningRMap.get(pp)) < ComputationalMesh.eps)
            nextR = turningRMap.get(pp) + ComputationalMesh.eps;
        else if (Math.abs(nextR - icbR) < permissibleGapForDiff)
            nextR = icbR + ComputationalMesh.eps * (nextR < icbR ? -1 : 1);
        else if (Math.abs(nextR - cmbR) < permissibleGapForDiff)
            nextR = cmbR + ComputationalMesh.eps * (nextR < cmbR ? -1 : 1);
        else if (Math.abs(nextR - earthR) < permissibleGapForDiff) nextR = earthR - ComputationalMesh.eps;
        else if (nextR < permissibleGapForDiff) nextR = ComputationalMesh.eps;

        if (Math.abs(beforeR - icbR) < permissibleGapForDiff)
            beforeR = icbR + permissibleGapForDiff * (nextR < icbR ? -1 : 1);
        else if (Math.abs(beforeR - cmbR) < permissibleGapForDiff)
            beforeR = cmbR + permissibleGapForDiff * (nextR < cmbR ? -1 : 1);
        else if (Math.abs(beforeR - earthR) < permissibleGapForDiff) beforeR = earthR - ComputationalMesh.eps;

        double smallerR = Math.min(beforeR, nextR);
        double biggerR = Math.max(beforeR, nextR);
        double theta = computeDelta(pp, smallerR, biggerR);
        if (beforeR <= ComputationalMesh.eps) theta += Math.toRadians(180);
        double time = computeT(pp, smallerR, biggerR);
        rList.add(nextR);
        thetaList.add(theta + thetaList.getLast());
        travelTimeList.add(time + travelTimeList.getLast());
    }

    /**
     * <p>
     * route [i] is a set of radius[i][0], theta[i][1], T[i][2].<br>
     * Note that the i-th theta indicates the &Delta; [rad] between the i-th point and
     * epicenter. T is the same. 0 th point is epicenter.
     * radius, theta and T are in [km], [rad] and [s], respectively.
     *
     * @param eventR [km] must be in the mantle
     * @param phase  Seismic {@link Phase}
     * @return route[point][]{r, theta, T}
     */
    private double[][] getRoute(double eventR, Phase phase) {
        if (getStructure().earthRadius() < eventR || eventR <= getStructure().coreMantleBoundary())
            throw new IllegalArgumentException("Input eventR:" + eventR + " is out of the mantle.");
        if (Double.isNaN(computeDelta(eventR, phase))) throw new RuntimeException(phase + " does not exist.");
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
                } else throw new RuntimeException("wow its not expected.");
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
                GeneralPart g = ((GeneralPart) part);
                PhasePart pp = g.getPhase();
                Partition partition = g.getPhase().whichPartition();
                double startR = getROf(!g.isDownward(), eventR, g) + (g.isDownward() ? -1 : 1) * ComputationalMesh.eps;
                double endR = getROf(g.isDownward(), eventR, g) + (g.isDownward() ? 1 : -1) * ComputationalMesh.eps;
                int startIndex = MESH.getNextIndexOf(startR, partition);
                int endIndex = MESH.getNextIndexOf(endR, partition);
                if (!g.isDownward()) startIndex++;

                RealVector mesh = MESH.getMesh(partition);
                for (int j = startIndex; j != endIndex; ) {
                    double r = mesh.getEntry(j);
                    addRThetaTime(r, pp, rList, thetaList, tList);
                    if (g.isDownward()) j--;
                    else j++;
                }
                addRThetaTime(endR, pp, rList, thetaList, tList);
            } else if (part.isBottomsideReflection() || part.isTransmission() || part.isPenetration() ||
                    part.isBounce() || part.isTopsideReflection()) {
            } else throw new RuntimeException("an unexpected part");

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
     * @param inner  inner(true) or outer (false)
     * @param eventR radius [km] of the event
     * @param part   for the innerR.
     * @return radius [km] of the inner(true) or outer (false) point of the part
     */
    private double getROf(boolean inner, double eventR, GeneralPart part) {
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
     * @param eventR [km] radius of event
     * @param phase  Seismic {@link Phase}
     * @return [point]{x, y} coordinates of the path
     */
    double[][] getRouteXY(double eventR, Phase phase) {
        if (Double.isNaN(computeDelta(eventR, phase))) {
            System.err.println(phase + " does not exist.");
            return null;
        }
        double[][] rTheta = getRoute(eventR, phase);
        double[][] points = new double[rTheta.length][2];
        for (int i = 0; i < points.length; i++) {
            points[i][0] = rTheta[i][0] * Math.sin(rTheta[i][1]);
            points[i][1] = rTheta[i][0] * Math.cos(rTheta[i][1]);
        }
        return points;
    }

    /**
     * Compute &Delta; for turningR &le; r &le; rEnd by a device of Jeffreys and Jeffreys
     *
     * @param pp   to compute &Delta; for
     * @param rEnd [km]
     * @return &Delta; for radius range between turningR and rEnd.
     */
    private double jeffreysDelta(PhasePart pp, double rEnd) {
        double turningR = turningRMap.get(pp);
        if (Math.abs(rEnd - turningR) <= ComputationalMesh.eps) return 0;
        DoubleFunction<Double> rToY = r -> WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, r) * drdx(pp, r);
        double rCenter = (rEnd + turningR) / 2;
        double modifiedR = rCenter + (toX(pp, rEnd) / 2 - toX(pp, rCenter)) * drdx(pp, rCenter);
        if (Double.isNaN(modifiedR) || modifiedR <= turningR || rEnd <= modifiedR) modifiedR = rCenter;
        return jeffreysMethod1(toX(pp, modifiedR), rToY.apply(modifiedR), rToY.apply(rEnd));
    }

    private double jeffreysT(PhasePart pp, double rEnd) {
        double turningR = turningRMap.get(pp);
        if (Math.abs(rEnd - turningR) <= ComputationalMesh.eps) return 0;
        DoubleFunction<Double> rToY = r -> WOODHOUSE.computeQT(pp, RAY_PARAMETER, r) * drdx(pp, r);
        double rCenter = (rEnd + turningR) / 2;
        double modifiedR = rCenter + (toX(pp, rEnd) / 2 - toX(pp, rCenter)) * drdx(pp, rCenter);
        if (Double.isNaN(modifiedR) || modifiedR <= turningR || rEnd <= modifiedR) modifiedR = rCenter;
        return jeffreysMethod1(toX(pp, modifiedR), rToY.apply(modifiedR), rToY.apply(rEnd));
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
            case JH:
                return Math.pow(WOODHOUSE.computeQTau(pp, RAY_PARAMETER, r), 2);
            case K:
                return 1 -
                        Math.pow(RAY_PARAMETER * Math.sqrt(getStructure().getA(r) / getStructure().getRho(r)) / r, 2);
        }
        throw new RuntimeException("unEXPeCteD");
    }

    private double drdx(PhasePart pp, double r) {
        return ComputationalMesh.eps / (toX(pp, r + ComputationalMesh.eps) - toX(pp, r));
    }

    /**
     * This method computes &Delta; by precomputed values. The startR and endR
     * must be in the section (inner-core, outer-core or mantle). If the endR
     *
     * @param pp     to compute
     * @param startR [km] must be a positive number
     * @param endR   [km] must be a positive number
     * @return &Delta; for rStart &le; r &le; rEnd
     */
    private double computeDelta(PhasePart pp, double startR, double endR) {
        if (Double.isNaN(startR + endR) || startR < 0 || endR < 0)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        if (endR - startR < ComputationalMesh.eps) return 0;
        Partition partition = pp.whichPartition();
        RealVector radii = MESH.getMesh(partition);
        double minR = radii.getEntry(0);
        double maxR = radii.getEntry(radii.getDimension() - 1);
        if (startR < minR - ComputationalMesh.eps || endR < startR || maxR + ComputationalMesh.eps < endR)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        double turningR = getTurningR(pp);
        //Integral interval contains a singular(bounce) point.
        if (startR < turningR && turningR < endR) return Double.NaN;

        startR = Math.max(startR, minR);
        endR = Math.min(endR, maxR);
        double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
        double jeffreysDelta = jeffreysDeltaMap.get(pp);

        int firstIndexForMemory = MESH.getNextIndexOf(startR, partition) + 1;
        int endIndexForMemory = MESH.getNextIndexOf(endR, partition);
        // the case where the integral interval is inside the jeffrey interval TODO
        if (turningR < endR && endR <= jeffreysBoundary) return jeffreysDelta(pp, endR) - jeffreysDelta(pp, startR);
        // the case where integral interval is shorter than mesh grid
        if (endIndexForMemory < firstIndexForMemory) {
            if (turningR < startR && startR <= jeffreysBoundary) {
                double delta = simpsonDelta(pp, jeffreysBoundary, endR);
                double jeff = jeffreysDelta - jeffreysDelta(pp, startR);
                if (Double.isNaN(jeff)) throw new RuntimeException("YoCHECK");
                return delta + jeff;
            }
            return simpsonDelta(pp, startR, endR);
        }
        double nextREnd = radii.getEntry(endIndexForMemory);
        //outside the nextREnd, if it is inside the jeffreys region, outside the region.
        double delta = simpsonDelta(pp, nextREnd < jeffreysBoundary ? jeffreysBoundary : nextREnd, endR);
        if (dThetaMap != null) {
            double[] theta = dThetaMap.get(pp);
            for (int i = firstIndexForMemory; i < endIndexForMemory; i++)
                delta += theta[i];
        } else {
            for (int i = firstIndexForMemory; i < endIndexForMemory; i++) {
                if (radii.getEntry(i) < jeffreysBoundary) continue;
                delta += simpsonDelta(pp, radii.getEntry(i), radii.getEntry(i + 1));
            }
        }
        //the case where startR is inside the jeffreys interval.
        if (turningR <= startR && startR < jeffreysBoundary) {
            double jeffreys = jeffreysDelta - jeffreysDelta(pp, startR);
//            if (Double.isNaN(jeffreys)) throw new RuntimeException("youtcheckya");
            return delta + jeffreys;
        }
        return delta + simpsonDelta(pp, startR, radii.getEntry(firstIndexForMemory));
    }

    /**
     * This method computes travel time by precomputed values. The startR and endR must be in the same partition
     * (mantle or inner-core).
     *
     * @param startR [km] start value of integration
     * @param endR   [km] end value of integration
     * @return [s] travel time for startR &le; r &le; endR
     */
    private double computeT(PhasePart pp, double startR, double endR) {
        if (Double.isNaN(startR + endR) || startR < 0 || endR < 0)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        if (endR - startR < ComputationalMesh.eps) return 0;
        Partition partition = pp.whichPartition();
        RealVector radii = MESH.getMesh(partition);
        double minR = radii.getEntry(0);
        double maxR = radii.getEntry(radii.getDimension() - 1);
        if (startR < minR - ComputationalMesh.eps || endR < startR || maxR + ComputationalMesh.eps < endR)
            throw new IllegalArgumentException("Invalid input (startR, endR)=(" + startR + ", " + endR + ")");
        double turningR = getTurningR(pp);
        //Integral interval contains a singular(bounce) point.
        if (startR < turningR && turningR < endR) return Double.NaN;


        startR = Math.max(startR, minR);
        endR = Math.min(endR, maxR);

        double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
        double jeffreysT = jeffreysTMap.get(pp);
        int firstIndexForMemory = MESH.getNextIndexOf(startR, partition) + 1;
        int endIndexForMemory = MESH.getNextIndexOf(endR, partition);
        // the case where the integral interval is inside the jeffrey interval TODO
        if (turningR < endR && endR <= jeffreysBoundary) return jeffreysT(pp, endR) - jeffreysT(pp, startR);
        // the case where integral interval is shorter than mesh grid
        if (endIndexForMemory < firstIndexForMemory) {
            if (turningR < startR && startR <= jeffreysBoundary) {
                double delta = simpsonT(pp, jeffreysBoundary, endR);
                double jeff = jeffreysT - jeffreysT(pp, startR);
                if (Double.isNaN(jeff)) throw new RuntimeException("YoCHECK");
                return delta + jeff;
            }
            return simpsonT(pp, startR, endR);
        }
        double nextREnd = radii.getEntry(endIndexForMemory);
        //outside the nextREnd, if it is inside the jeffreys region, outside the region.
        double time = simpsonT(pp, nextREnd < jeffreysBoundary ? jeffreysBoundary : nextREnd, endR);
        if (dTMap != null) {
            double[] t = dTMap.get(pp);
            for (int i = firstIndexForMemory; i < endIndexForMemory; i++)
                time += t[i];
        } else {
            for (int i = firstIndexForMemory; i < endIndexForMemory; i++) {
                if (radii.getEntry(i) < jeffreysBoundary) continue;
                time += simpsonT(pp, radii.getEntry(i), radii.getEntry(i + 1));
            }
        }
        //the case where startR is inside the jeffreys interval.
        if (turningR <= startR && startR < jeffreysBoundary) {
            double jeffreys = jeffreysT - jeffreysT(pp, startR);
//            if (Double.isNaN(jeffreys)) throw new RuntimeException("An error on ray parameter "+RAY_PARAMETER);
            return time + jeffreys;
        }
        return time + simpsonDelta(pp, startR, radii.getEntry(firstIndexForMemory));

    }

    /**
     * Computes &Delta; for the mesh and compute the ones for the mantle,
     * outer-core and inner-core. If one does not exist, the value is default
     * ({@link Double#NaN}).
     * <p>
     * TODO boundary value
     */
    private void computeDelta() {
        deltaMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        Arrays.stream(PhasePart.values()).forEach(pp -> deltaMap.put(pp, Double.NaN));
        Function<PhasePart, Thread> createThread = pp -> {
            RealVector mesh = MESH.getMesh(pp.whichPartition());
            double[] dTheta = new double[mesh.getDimension() - 1];
            dThetaMap.put(pp, dTheta);
            return new Thread(() -> {
                double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
                for (int i = 0; i < dTheta.length; i++) {
                    if (mesh.getEntry(i) < jeffreysBoundary) continue;
                    dTheta[i] = simpsonDelta(pp, mesh.getEntry(i), mesh.getEntry(i + 1));
                }
                double turningR = getTurningR(pp);
                double startR = Double.isNaN(turningR) ? mesh.getEntry(0) : turningR;
                //TODO diffraction
                deltaMap.put(pp, computeDelta(pp, startR, mesh.getEntry(dTheta.length)));
            });
        };

        dThetaMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        List<Thread> runningThread = Arrays.stream(PhasePart.values()).map(createThread).collect(Collectors.toList());

        runningThread.forEach(Thread::start);
        try {
            for (Thread t : runningThread)
                t.join();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not compute epicentral distances.");
        }
    }

    /**
     * computes turning radius
     */
    private void setTurningRs() {
        turningRMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        if (RAY_PARAMETER == 0) {
            Arrays.stream(PhasePart.values()).forEach(pp -> {
                turningRMap.put(pp, Double.NaN);
            });
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
        if (!isComputed)
            throw new RuntimeException("Not computed yet. It must be computed before printing information.");
        System.out.println("#Phase:Turning points[km] Jeffrey boundary[km] Propagation delta[deg] time[s]");
        Arrays.stream(PhasePart.values()).forEach(pp -> System.out.println(
                pp + ": " + Precision.round(turningRMap.get(pp), 3) + " " + jeffreysBoundaryMap.get(pp) + " " +
                        Precision.round(Math.toDegrees(deltaMap.get(pp)), 3) + " " +
                        Precision.round(timeMap.get(pp), 3)));
    }

    public double getTurningR(PhasePart pp) {
        return turningRMap.get(pp);
    }

    /**
     * Integrates &Delta; by Simpson's Rule
     *
     * @param pp     target phase
     * @param startR [km]
     * @param endR   [km]
     * @return [rad] &Delta; for startR &le; r &le; endR
     * @see Integrand#bySimpsonRule(double, double, double, double)
     */
    private double simpsonDelta(PhasePart pp, double startR, double endR) {
        double deltaX = endR - startR;
        double a = WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, startR);
        double b = WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, startR + 0.5 * deltaX);
        double c = WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, endR);
        return bySimpsonRule(a, b, c, deltaX);
    }

    /**
     * Integrates traveltime by Simpson's Rule
     *
     * @param pp     target phase
     * @param startR [km]
     * @param endR   [km]
     * @return [s] travel time for startR &le; r &le; endR
     * @see Integrand#bySimpsonRule(double, double, double, double)
     */
    private double simpsonT(PhasePart pp, double startR, double endR) {
        double deltaX = endR - startR;
        double a = WOODHOUSE.computeQT(pp, RAY_PARAMETER, startR);
        double b = WOODHOUSE.computeQT(pp, RAY_PARAMETER, startR + 0.5 * deltaX);
        double c = WOODHOUSE.computeQT(pp, RAY_PARAMETER, endR);
        return bySimpsonRule(a, b, c, deltaX);
    }

    /**
     * Computes diffraction on a boundary at r. The velocity is considered as
     * the one at r &pm; {@link ComputationalMesh#eps}
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
        if (ComputationalMesh.eps < Math.abs(getStructure().coreMantleBoundary() - boundaryR) &&
                ComputationalMesh.eps < Math.abs(getStructure().innerCoreBoundary() - boundaryR) &&
                Arrays.stream(WOODHOUSE.getStructure().additionalBoundaries())
                        .allMatch(b -> ComputationalMesh.eps < Math.abs(b - boundaryR)))
            throw new RuntimeException("The input radius " + boundaryR + " is not a boundary.");
        double r = boundaryR + (shallower ? ComputationalMesh.eps : -ComputationalMesh.eps);
        double s = boundaryR * deltaOnBoundary;
        double numerator;
        switch (pp) {
            case I:
            case K:
            case P:
                numerator = WOODHOUSE.getStructure().getA(r);
                break;
            case JH:
            case SH:
                numerator = WOODHOUSE.getStructure().getN(r);
                break;
            case SV:
            case JV:
                numerator = WOODHOUSE.getStructure().getL(r);
                break;
            default:
                throw new RuntimeException("unikuspected");
        }
        double velocity = Math.sqrt(numerator / WOODHOUSE.getStructure().getRho(r));
        return s / velocity;
    }

    @Override
    public int compareTo(Raypath o) {
        return Double.compare(RAY_PARAMETER, o.RAY_PARAMETER);
    }
}
