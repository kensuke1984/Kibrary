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
import java.util.stream.Stream;

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
 * <p>
 * calculation of a travel time and a epicentral distance region 0 near turning
 * depth (eps +turningDepth) will be calculated by Geffreys region 1 deeper part
 * than event depth but not including region 0 region 2 shallower part than
 * event depth to the surface
 * <p>
 * &Delta; (delta) denotes epicentral distance.<br>
 * T (time) denotes travel time. &tau; (tau) denotes tau. Q<sub>T</sub>&ne;Q
 * <sub>&tau;</sub>
 * <p>
 * <p>
 * P: P wave in the mantle<br>
 * SV,SH: S wave (SV, SH) in the mantle<br>
 * K: P(K) wave in the outer-core<br>
 * I: P(I) wave in the inner-core<br>
 * JV,JH: SV, SH(J) wave in the inner-core<br>
 * <p>
 * TODO reflection
 *
 * @author Kensuke Konishi
 * @version 0.4.4b
 * @see "Woodhouse, 1981"
 */
public class Raypath implements Serializable, Comparable<Raypath> {

    /**
     * If the gap between the CMB and the turning r is under this value, then
     * diffracted phase can be computed.
     */
    static final double permissibleGapForDiff = 1e-5;
    /**
     * 2016/12/16
     */
    private static final long serialVersionUID = 8243323152285239494L;
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
     */
    private transient Map<PhasePart, Double> turningRMap;
    /**
     * &Delta; of phase parts
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
    private transient Map<PhasePart, Propagation> propagationMap;

    /**
     * ray parameter p the source is on the surface PREM structure
     *
     * @param rayParameter a ray parameter
     */
    public Raypath(double rayParameter) {
        this(rayParameter, PolynomialStructure.PREM);
    }

    /**
     * @param rayParameter the ray parameter
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
     * @param phase the target phase
     * @return bottom Radius of the input phase[km]
     */

    double bottomingR(Phase phase) {
        Partition pReach = phase.pReaches();
        Partition sReach = phase.sReaches();
        double sBottom = earthRadius();
        double pBottom = earthRadius();
        if (sReach != null) switch (sReach) {
            case CORE_MANTLE_BOUNDARY:
                sBottom = coreMantleBoundary();
                break;
            case INNER_CORE_BOUNDARY:
                sBottom = innerCoreBoundary();
                break;
            case MANTLE:
                sBottom = phase.isPSV() ? turningRMap.get(PhasePart.SV) : turningRMap.get(PhasePart.SH);
                break;
            case INNERCORE:
                sBottom = phase.isPSV() ? turningRMap.get(PhasePart.JV) : turningRMap.get(PhasePart.JH);
                break;
            default:
                throw new RuntimeException("UNEXPECTED");
        }
        if (pReach != null) switch (pReach) {
            case CORE_MANTLE_BOUNDARY:
                pBottom = coreMantleBoundary();
                break;
            case INNER_CORE_BOUNDARY:
                pBottom = innerCoreBoundary();
                break;
            case INNERCORE:
            case MANTLE:
            case OUTERCORE:
                pBottom = turningRMap.get(PhasePart.P);
                break;
            default:
                throw new RuntimeException("UNEXPECTED");
        }
        return pBottom < sBottom ? pBottom : sBottom;
    }

    /**
     * Create an information file of {@link Phase}
     *
     * @param informationFile Path of an informationFile
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
     * @param phase    to output
     */
    public void outputDat(Path dataFile, double eventR, Phase phase) {
        if (!exists(eventR, phase)) return;
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
     * @param phase   Seismic {@link Phase}
     * @param epsFile Path of an eps file name
     * @param options open options
     * @throws IOException if any
     */
    public void outputEPS(double eventR, Phase phase, Path epsFile, OpenOption... options) throws IOException {
        if (!exists(eventR, phase)) return;
        try (BufferedOutputStream os = new BufferedOutputStream(Files.newOutputStream(epsFile, options))) {
            createPanel(eventR, phase)
                    .toEPS(os, phase, RAY_PARAMETER, computeDelta(eventR, phase), computeT(eventR, phase), eventR);
        }
    }

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
        AtomicInteger ai = new AtomicInteger();
        List<Double> outputList = new ArrayList<>();
        Arrays.stream(PhasePart.values()).filter(pp -> propagationMap.get(pp) != Propagation.NOEXIST).forEach(pp -> {
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
     * a given mesh threshold ({@link ComputationalMesh#integralThreshold}).
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
            if (Double.isNaN(turningRMap.get(pp)) || propagationMap.get(pp) == Propagation.PENETRATING) return;
            int index = MESH.getNextIndexOf(turningRMap.get(pp), pp.whichPartition());
            RealVector mesh = MESH.getMesh(pp.whichPartition());
            double boundary = mesh.getEntry(index);
            while (++index < mesh.getDimension()) {
                double next = mesh.getEntry(index);
                double q = WOODHOUSE.computeQT(pp, RAY_PARAMETER, boundary);
                double qNext = WOODHOUSE.computeQT(pp, RAY_PARAMETER, next);
                double ratio = q < qNext ? q / qNext : qNext / q;
                if (MESH.integralThreshold < ratio) break;
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
                double startR;
                switch (getPropagation(pp)) {
                    case PENETRATING:
                        startR = mesh.getEntry(0) + ComputationalMesh.eps;
                        break;
                    case DIFFRACTION:
                        startR = mesh.getEntry(0) + permissibleGapForDiff;
                        break;
                    case BOUNCING:
                        startR = turningRMap.get(pp);
                        break;
                    default:
                        throw new RuntimeException("UNEXPECTED");
                }
                timeMap.put(pp, computeT(pp, startR, mesh.getEntry(dT.length)));
            });
        };

        dTMap = new EnumMap<>(PhasePart.class);
        List<Thread> runningThread =
                Arrays.stream(PhasePart.values()).filter(pp -> propagationMap.get(pp) != Propagation.NOEXIST)
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
     * This method computes T. The rStart and rEnd must be in the same partition
     * (mantle or inner-core).
     *
     * @param startR start value of integration
     * @param endR   end value of integration
     * @return T for startR &le; r &le; endR
     */
    private double computeT(PhasePart pp, double startR, double endR) {
        Partition partition = pp.whichPartition();
        RealVector radii = MESH.getMesh(partition);
        double minR = radii.getEntry(0);
        double maxR = radii.getEntry(radii.getDimension() - 1);
        if (startR < minR - ComputationalMesh.eps || endR < startR || maxR + ComputationalMesh.eps < endR)
            throw new IllegalArgumentException("Input rStart and rEnd are invalid.");

        if (startR < turningRMap.get(pp))
            throw new IllegalArgumentException("Input rStart is deeper than the boucing point.");

        if (getPropagation(pp) == Propagation.NOEXIST) return Double.NaN;

        if (endR - startR < ComputationalMesh.eps) return 0;

        startR = Math.max(startR, minR);
        endR = Math.min(endR, maxR);

        double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
        double jeffreysT = jeffreysTMap.get(pp);

        // might be NaN
        if (endR <= jeffreysBoundary) return jeffreysT(pp, endR) - jeffreysT(pp, startR);

        int beginIndex = MESH.getNextIndexOf(startR, partition) + 1;
        int endIndex = MESH.getNextIndexOf(endR, partition);
        if (endIndex < beginIndex) {
            if (getPropagation(pp) == Propagation.PENETRATING || jeffreysBoundary <= startR)
                return simpsonT(pp, startR, endR);
            double delta = simpsonT(pp, jeffreysBoundary, endR);
            double jeff = jeffreysT - jeffreysT(pp, startR);
            if (Double.isNaN(jeff)) throw new RuntimeException("YoCHECK");
            return delta + jeff;
        }

        double nextREnd = radii.getEntry(endIndex);
        double time = simpsonT(pp, nextREnd < jeffreysBoundary ? jeffreysBoundary : nextREnd, endR);
        for (int i = beginIndex; i < endIndex; i++) {
            if (radii.getEntry(i) < jeffreysBoundary) continue;
            time += simpsonT(pp, radii.getEntry(i), radii.getEntry(i + 1));
        }

        if (Double.isNaN(jeffreysBoundary) || jeffreysBoundary <= startR)
            return time + simpsonT(pp, startR, radii.getEntry(beginIndex));

//        int indexJeffreyNext = MESH.getNextIndexOf(jeffreysBoundary, partition) + 1;
//        time += simpsonT(pp, jeffreysBoundary, radii.getEntry(indexJeffreyNext));

        double jeffreys = jeffreysT - jeffreysT(pp, startR);
        if (Double.isNaN(jeffreys)) throw new RuntimeException("youcheckya");
        return time + jeffreys;
    }

    /**
     * Compute delta for a input {@link Phase}
     *
     * @param eventR [km] must be in the mantle
     * @param phase  Seismic {@link Phase}
     * @return Epicentral distance[rad] for the phase if the phase does not
     * exist or anything wrong, returns Double.NaN
     */
    public double computeDelta(double eventR, Phase phase) {
        if (getStructure().earthRadius() < eventR || eventR < getStructure().coreMantleBoundary())
            throw new IllegalArgumentException("Event radius (" + eventR + ") must be in the mantle.");
        if (!exists(eventR, phase)) return Double.NaN;

        double mp = phase.getCountOf(PhasePart.P);
        double ms = phase.getCountOf((phase.isPSV() ? PhasePart.SV : PhasePart.SH));
        double oc = phase.getCountOf(PhasePart.K);
        double icp = phase.getCountOf(PhasePart.I);
        double ics = phase.getCountOf((phase.isPSV() ? PhasePart.JV : PhasePart.JH));

        // System.out.println(mp+" "+ms+" "+oc+" "+icp+" "+ics);
        double p = 0 < mp ? deltaMap.get(PhasePart.P) * mp * 2 : 0;
        double s = 0 < ms ? deltaMap.get((phase.isPSV() ? PhasePart.SV : PhasePart.SH)) * ms * 2 : 0;
        double k = 0 < oc ? deltaMap.get(PhasePart.K) * oc * 2 : 0;
        double i = 0 < icp ? deltaMap.get(PhasePart.I) * icp * 2 : 0;
        double j = 0 < ics ? deltaMap.get((phase.isPSV() ? PhasePart.JV : PhasePart.JH)) * ics * 2 : 0;
        double delta = p + s + k + i + j;

        if (phase.isDiffracted()) delta += phase.getDiffractionAngle();

        if (Math.abs(eventR - earthRadius()) < ComputationalMesh.eps) return delta;

        switch (phase.toString().charAt(0)) {
            case 'p':
                return delta + computeDelta(PhasePart.P, eventR, earthRadius());
            case 'P':
                return delta - computeDelta(PhasePart.P, eventR, earthRadius());
            case 's':
                return delta + computeDelta(phase.isPSV() ? PhasePart.SV : PhasePart.SH, eventR, earthRadius());
            case 'S':
                return delta - computeDelta(phase.isPSV() ? PhasePart.SV : PhasePart.SH, eventR, earthRadius());
        }
        return delta;
    }

    /**
     * @param phase Seismic {@link Phase}
     * @return Travel time[s] for the phase
     */
    public double computeT(double eventR, Phase phase) {
        if (!exists(eventR, phase)) return Double.NaN;
        double mp = phase.getCountOf(PhasePart.P);
        double ms = phase.getCountOf((phase.isPSV() ? PhasePart.SV : PhasePart.SH));
        double oc = phase.getCountOf(PhasePart.K);
        double icp = phase.getCountOf(PhasePart.I);
        double ics = phase.getCountOf((phase.isPSV() ? PhasePart.JV : PhasePart.JH));

        double p = 0 < mp ? timeMap.get(PhasePart.P) * mp * 2 : 0;
        double s = 0 < ms ? timeMap.get((phase.isPSV() ? PhasePart.SV : PhasePart.SH)) * ms * 2 : 0;
        double k = 0 < oc ? timeMap.get(PhasePart.K) * oc * 2 : 0;
        double i = 0 < icp ? timeMap.get(PhasePart.I) * icp * 2 : 0;
        double j = 0 < ics ? timeMap.get((phase.isPSV() ? PhasePart.JV : PhasePart.JH)) * ics * 2 : 0;

        double time = p + s + k + i + j;
        if (phase.isDiffracted()) {
            double deltaOnCMB = phase.getDiffractionAngle();
            time += phase.toString().contains("Pdiff")// TODO
                    ? computeTAlongBoundary(PhasePart.P, coreMantleBoundary(), deltaOnCMB, true) :
                    computeTAlongBoundary(phase.isPSV() ? PhasePart.SV : PhasePart.SH, coreMantleBoundary(), deltaOnCMB,
                            true);
        }

        if (Math.abs(eventR - earthRadius()) <= ComputationalMesh.eps) return time;

        switch (phase.toString().charAt(0)) {
            case 'p':
                return time + computeT(PhasePart.P, eventR, earthRadius());
            case 'P':
                return time - computeT(PhasePart.P, eventR, earthRadius());
            case 's':
                return time + computeT(phase.isPSV() ? PhasePart.SV : PhasePart.SH, eventR, earthRadius());
            case 'S':
                return time - computeT(phase.isPSV() ? PhasePart.SV : PhasePart.SH, eventR, earthRadius());
        }

        return time;
    }

    /**
     * @return Radius of the core mantle boundary[km]
     */
    double coreMantleBoundary() {
        return WOODHOUSE.getStructure().coreMantleBoundary();
    }

    /**
     * @return Earth radius [km]
     */
    double earthRadius() {
        return WOODHOUSE.getStructure().earthRadius();
    }

    /**
     * @param phase Seismic {@link Phase}
     * @return if name exists for the rayparameter
     */
    public boolean exists(double eventR, Phase phase) {
        String nameStr = phase.toString();
        if (nameStr.contains("Pdiff")) return getPropagation(PhasePart.P) == Propagation.DIFFRACTION;
        if (nameStr.contains("Sdiff"))
            return getPropagation(phase.isPSV() ? PhasePart.SV : PhasePart.SH) == Propagation.DIFFRACTION;
        if (nameStr.startsWith("p") || nameStr.startsWith("s"))
            if (Math.abs(eventR - earthRadius()) < ComputationalMesh.eps) return false;
        if (nameStr.startsWith("P") || nameStr.startsWith("p")) if (eventR < turningRMap.get(PhasePart.P)) return false;
        if (nameStr.startsWith("S") || nameStr.startsWith("s"))
            if (eventR < (phase.isPSV() ? turningRMap.get(PhasePart.SV) : turningRMap.get(PhasePart.SH))) //
                return false;
        return phase.exists(this);
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
        if (Math.abs(nextR - turningRMap.get(pp)) < ComputationalMesh.eps)
            nextR = turningRMap.get(pp) + ComputationalMesh.eps;
        else if (Math.abs(nextR - innerCoreBoundary()) < permissibleGapForDiff)
            nextR = innerCoreBoundary() - ComputationalMesh.eps * (nextR < innerCoreBoundary() ? 1 : -1);
        else if (Math.abs(nextR - coreMantleBoundary()) < permissibleGapForDiff)
            nextR = coreMantleBoundary() - ComputationalMesh.eps * (nextR < coreMantleBoundary() ? 1 : -1);
        else if (Math.abs(nextR - earthRadius()) < permissibleGapForDiff) nextR = earthRadius() - ComputationalMesh.eps;
        else if (nextR < permissibleGapForDiff) nextR = ComputationalMesh.eps;

        if (Math.abs(beforeR - innerCoreBoundary()) < permissibleGapForDiff) beforeR = innerCoreBoundary();
        else if (Math.abs(beforeR - coreMantleBoundary()) < permissibleGapForDiff) beforeR = coreMantleBoundary();
        else if (Math.abs(beforeR - earthRadius()) < permissibleGapForDiff) beforeR = earthRadius();

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
     * TODO arbitrary boundaries
     *
     * @param eventR [km] must be in the mantle
     * @param phase  Seismic {@link Phase}
     * @return route[point]{r, theta} <br>
     * r[km], theta[rad]<br>
     * route [i] is a set of radius([i][0]), theta([i][1]), travel
     * time([i][2]).<br>
     * theta of ith point indicates the $Delta; between i th point and
     * epicenter. trravel time is same. 0 th point is epicenter.
     */
    public double[][] getRoute(double eventR, Phase phase) {
        if (earthRadius() < eventR || eventR <= coreMantleBoundary())
            throw new IllegalArgumentException("Input eventR:" + eventR + " is out of the mantle.");
        if (!exists(eventR, phase)) throw new RuntimeException(phase + " does not exist.");
        // radius [km]
        LinkedList<Double> rList = new LinkedList<>();
        // theta [rad]
        LinkedList<Double> thetaList = new LinkedList<>();
        // time [s]
        LinkedList<Double> travelTimeList = new LinkedList<>();
        RealVector mantleMesh = MESH.getMesh(Partition.MANTLE);
        // First phase because we must take care of the eventR
        // [eventR, bottomR (turningR or CMB)]
        rList.add(eventR);
        thetaList.add(0d);
        travelTimeList.add(0d);
        if (phase.partIsDownGoing(0)) {
            int indexEventR = MESH.getNextIndexOf(eventR, Partition.MANTLE);
            if (eventR - mantleMesh.getEntry(indexEventR) < ComputationalMesh.eps) indexEventR--;
            PhasePart pp = phase.phasePartOf(0);
            if (getPropagation(pp) == Propagation.PENETRATING) for (int iR = indexEventR; 0 <= iR; iR--)
                addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
            else {
                double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
                int bottomIndex = MESH.getNextIndexOf(jeffreysBoundary, Partition.MANTLE);
                for (int iR = indexEventR; bottomIndex < iR; iR--)
                    addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                addRThetaTime(jeffreysBoundary, pp, rList, thetaList, travelTimeList);
                addRThetaTime(turningRMap.get(pp), pp, rList, thetaList, travelTimeList);
            }
        } else for (int iR = MESH.getNextIndexOf(eventR, Partition.MANTLE) + 1, n = mantleMesh.getDimension(); iR < n;
                    iR++)
            addRThetaTime(mantleMesh.getEntry(iR), phase.phasePartOf(0), rList, thetaList, travelTimeList);

        for (int i = 1; i < phase.getNPart(); i++) {
            boolean isDownGoing = phase.partIsDownGoing(i);
            PhasePart pp = phase.phasePartOf(i);
            switch (phase.partitionOf(i)) {
                case CORE_MANTLE_BOUNDARY:
                    double angle = phase.getDiffractionAngle();
                    double finalAngle = thetaList.getLast() + phase.getDiffractionAngle();
                    double finalTime =
                            computeTAlongBoundary(pp, coreMantleBoundary(), phase.getDiffractionAngle(), true) +
                                    travelTimeList.getLast();
                    for (double tic = Math.toRadians(1); tic < angle; tic += Math.toRadians(1)) {
                        rList.add(coreMantleBoundary() + permissibleGapForDiff);
                        thetaList.add(thetaList.getLast() + Math.toRadians(1));
                        travelTimeList.add(computeTAlongBoundary(pp, coreMantleBoundary(), Math.toRadians(1), true) +
                                travelTimeList.getLast());
                    }
                    rList.add(coreMantleBoundary() + permissibleGapForDiff);
                    thetaList.add(finalAngle);
                    travelTimeList.add(finalTime);
                    break;
                case MANTLE:
                    if (getPropagation(pp) == Propagation.PENETRATING)
                        if (isDownGoing) for (int iR = mantleMesh.getDimension() - 2; 0 <= iR; iR--)
                            addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                        else for (int iR = 1, n = mantleMesh.getDimension(); iR < n; iR++)
                            addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                    else {
                        double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
                        int bottomIndex = MESH.getNextIndexOf(jeffreysBoundary, Partition.MANTLE);
                        if (isDownGoing) {
                            for (int iR = mantleMesh.getDimension() - 2; bottomIndex < iR; iR--)
                                addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                            addRThetaTime(jeffreysBoundary, pp, rList, thetaList, travelTimeList);
                            addRThetaTime(turningRMap.get(pp), pp, rList, thetaList, travelTimeList);
                        } else {
                            addRThetaTime(jeffreysBoundary, pp, rList, thetaList, travelTimeList);
                            for (int iR = bottomIndex + 1, n = mantleMesh.getDimension(); iR < n; iR++)
                                addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                        }
                    }
                    break;
                case OUTERCORE:
                    RealVector outerCoreMesh = MESH.getMesh(Partition.OUTERCORE);
                    if (propagationMap.get(PhasePart.K) == Propagation.PENETRATING) {
                        if (isDownGoing) for (int iR = outerCoreMesh.getDimension() - 2; 0 <= iR; iR--)
                            addRThetaTime(outerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                        else for (int iR = 1, n = outerCoreMesh.getDimension(); iR < n; iR++)
                            addRThetaTime(outerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                    } else {
                        double kBoundary = jeffreysBoundaryMap.get(PhasePart.K);
                        int bottomIndex = MESH.getNextIndexOf(kBoundary, Partition.OUTERCORE);
                        if (isDownGoing) {
                            for (int iR = outerCoreMesh.getDimension() - 2; bottomIndex < iR; iR--)
                                addRThetaTime(outerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                            addRThetaTime(kBoundary, pp, rList, thetaList, travelTimeList);
                            addRThetaTime(turningRMap.get(PhasePart.K), pp, rList, thetaList, travelTimeList);
                        } else {
                            addRThetaTime(kBoundary, pp, rList, thetaList, travelTimeList);
                            for (int iR = bottomIndex + 1, n = outerCoreMesh.getDimension(); iR < n; iR++)
                                addRThetaTime(outerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                        }
                    }
                    break;
                case INNERCORE:
                    RealVector innerCoreMesh = MESH.getMesh(Partition.INNERCORE);
                    if (RAY_PARAMETER == 0) {
                        if (isDownGoing) for (int iR = innerCoreMesh.getDimension() - 2; 0 <= iR; iR--)
                            addRThetaTime(innerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                        else for (int iR = 1, n = innerCoreMesh.getDimension(); iR < n; iR++)
                            addRThetaTime(innerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                        continue;
                    }
                    double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
                    int bottomIndex = MESH.getNextIndexOf(jeffreysBoundary, Partition.INNERCORE);
                    if (isDownGoing) {
                        for (int iR = innerCoreMesh.getDimension() - 2; bottomIndex < iR; iR--)
                            addRThetaTime(innerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                        addRThetaTime(jeffreysBoundary, pp, rList, thetaList, travelTimeList);
                        addRThetaTime(turningRMap.get(pp), pp, rList, thetaList, travelTimeList);
                    } else {
                        addRThetaTime(jeffreysBoundary, pp, rList, thetaList, travelTimeList);
                        for (int iR = bottomIndex + 1, n = innerCoreMesh.getDimension(); iR < n; iR++)
                            addRThetaTime(innerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
                    }
                    break;
                default:
                    throw new RuntimeException("souteigai");
            }
        }
        double[][] points = new double[rList.size()][3];
        points[0][0] = eventR;
        for (int i = 1; i < points.length; i++) {
            points[i][0] = rList.get(i);
            points[i][1] = thetaList.get(i);
            points[i][2] = travelTimeList.get(i);
        }

        return points;
    }

    /**
     * @return false returns if and only if {@link PhasePart#P},{@link PhasePart#SV} and {@link PhasePart#SH}
     * are all {@link Propagation#NOEXIST}.
     */
    boolean exists() {
        return Stream.of(PhasePart.P, PhasePart.SV, PhasePart.SH)
                .anyMatch(phasePart -> propagationMap.get(phasePart) != Propagation.NOEXIST);
    }

    /**
     * The center of the Earth is (0, 0) Starting point is (0, eventR)
     *
     * @param phase Seismic {@link Phase}
     * @return [point]{x, y} coordinates of the path
     */
    public double[][] getRouteXY(double eventR, Phase phase) {
        if (!exists(eventR, phase)) {
            System.err.println(phase + " does not exist.");
            return null;
        }
        // System.out.println(phase+" "+ shTurningR);
        double[][] rTheta = getRoute(eventR, phase);
        double[][] points = new double[rTheta.length][2];
        for (int i = 0; i < points.length; i++) {
            points[i][0] = rTheta[i][0] * Math.sin(rTheta[i][1]);
            points[i][1] = rTheta[i][0] * Math.cos(rTheta[i][1]);
        }

        return points;
    }

    /**
     * @return Radius of the inner core boundary [km]
     */
    double innerCoreBoundary() {
        return WOODHOUSE.getStructure().innerCoreBoundary();
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
     * @param startR [km]
     * @param endR   [km]
     * @return &Delta; for rStart &le; r &le; rEnd
     */
    private double computeDelta(PhasePart pp, double startR, double endR) {
        Partition partition = pp.whichPartition();
        RealVector radii = MESH.getMesh(partition);
        double minR = radii.getEntry(0);
        double maxR = radii.getEntry(radii.getDimension() - 1);
        if (startR < minR - ComputationalMesh.eps || endR < startR || maxR + ComputationalMesh.eps < endR)
            throw new IllegalArgumentException("Input rStart and rEnd are invalid.");

        if (startR < turningRMap.get(pp))
            throw new IllegalArgumentException("Input rStart is deeper than the boucing point.");

        if (getPropagation(pp) == Propagation.NOEXIST) return Double.NaN;

        if (endR - startR < ComputationalMesh.eps) return 0;

        startR = Math.max(startR, minR);
        endR = Math.min(endR, maxR);
        double jeffreysBoundary = jeffreysBoundaryMap.get(pp);
        double jeffreysDelta = jeffreysDeltaMap.get(pp);
        // might be NaN
        if (endR <= jeffreysBoundary) return jeffreysDelta(pp, endR) - jeffreysDelta(pp, startR);

        int firstIndexForMemory = MESH.getNextIndexOf(startR, partition) + 1;
        int endIndexForMemory = MESH.getNextIndexOf(endR, partition);
        if (endIndexForMemory < firstIndexForMemory) {
            if (getPropagation(pp) == Propagation.PENETRATING || jeffreysBoundary <= startR)
                return simpsonDelta(pp, startR, endR);

            double delta = simpsonDelta(pp, jeffreysBoundary, endR);
            double jeff = jeffreysDelta - jeffreysDelta(pp, startR);
            if (Double.isNaN(jeff)) throw new RuntimeException("YoCHECK");

            return delta + jeff;
        }
        double nextREnd = radii.getEntry(endIndexForMemory);
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
        if (Double.isNaN(jeffreysBoundary) || jeffreysBoundary <= startR)
            return delta + simpsonDelta(pp, startR, radii.getEntry(firstIndexForMemory));
//        int indexJeffreyNext = MESH.getNextIndexOf(jeffreysBoundary, partition) + 1;
//        delta += simpsonDelta(pp, jeffreysBoundary, radii.getEntry(indexJeffreyNext));
        double jeffreys = jeffreysDelta - jeffreysDelta(pp, startR);
        if (Double.isNaN(jeffreys)) throw new RuntimeException("youtcheckya");
        return delta + jeffreys;
    }

    /**
     * Computes &Delta; for the mesh and compute the ones for the mantle,
     * outer-core and inner-core. If one does not exist, the value is default (
     * {@link Double#NaN}).
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

                double startR;
                switch (getPropagation(pp)) {
                    case PENETRATING:
                        startR = mesh.getEntry(0) + ComputationalMesh.eps;
                        break;
                    case DIFFRACTION:
                        startR = mesh.getEntry(0) + permissibleGapForDiff;
                        break;
                    case BOUNCING:
                        startR = turningRMap.get(pp);
                        break;
                    default:
                        throw new RuntimeException("UNEXPECTED");
                }
                deltaMap.put(pp, computeDelta(pp, startR, mesh.getEntry(dTheta.length)));
            });
        };

        dThetaMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        List<Thread> runningThread =
                Arrays.stream(PhasePart.values()).filter(pp -> propagationMap.get(pp) != Propagation.NOEXIST)
                        .map(createThread).collect(Collectors.toList());

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
     * @param pp target phase part
     * @return propagation of the pp
     */
    Propagation getPropagation(PhasePart pp) {
        return propagationMap.get(pp);
    }

    /**
     * computes turning radius
     */
    private void setTurningRs() {
        turningRMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        propagationMap = Collections.synchronizedMap(new EnumMap<>(PhasePart.class));
        if (RAY_PARAMETER == 0) {
            Arrays.stream(PhasePart.values()).forEach(pp -> {
                turningRMap.put(pp, Double.NaN);
                propagationMap.put(pp, Propagation.PENETRATING);
            });
            return;
        }

        Arrays.stream(PhasePart.values())
                .forEach(pp -> turningRMap.put(pp, getStructure().getTurningR(pp, RAY_PARAMETER)));

        if (!Double.isNaN(turningRMap.get(PhasePart.K))) propagationMap.put(PhasePart.K, Propagation.BOUNCING);
        else propagationMap.put(PhasePart.K, Double.isNaN(
                WOODHOUSE.computeQT(PhasePart.K, RAY_PARAMETER, innerCoreBoundary() + ComputationalMesh.eps)) ?
                Propagation.NOEXIST : Propagation.PENETRATING);

        Stream.of(PhasePart.P, PhasePart.SV, PhasePart.SH).forEach(pp -> {
            Propagation propagation =
                    Double.isNaN(WOODHOUSE.computeQT(pp, RAY_PARAMETER, coreMantleBoundary() + ComputationalMesh.eps)) ?
                            Propagation.NOEXIST : Propagation.PENETRATING;
            double turningR = turningRMap.get(pp);
            if (coreMantleBoundary() + permissibleGapForDiff < turningR) propagation = Propagation.BOUNCING;
            else if (coreMantleBoundary() <= turningR) propagation = Propagation.DIFFRACTION;
            if (propagation == Propagation.BOUNCING &&
                    Double.isNaN(WOODHOUSE.computeQT(pp, RAY_PARAMETER, turningR + ComputationalMesh.eps)))
                propagation = Propagation.NOEXIST;
            propagationMap.put(pp, propagation);
        });

        Stream.of(PhasePart.I, PhasePart.JV, PhasePart.JH).forEach(pp -> {
            Propagation propagation =
                    Double.isNaN(WOODHOUSE.computeQT(pp, RAY_PARAMETER, ComputationalMesh.eps)) ? Propagation.NOEXIST :
                            Propagation.PENETRATING;
            double turningR = turningRMap.get(pp);
            if (!Double.isNaN(turningR)) propagation = Propagation.BOUNCING;
            if (propagation == Propagation.BOUNCING &&
                    Double.isNaN(WOODHOUSE.computeQT(pp, RAY_PARAMETER, turningR + ComputationalMesh.eps)))
                propagation = Propagation.NOEXIST;
            propagationMap.put(pp, propagation);
        });

    }

    /**
     * Information to Standard output. Note that if the method is called before
     * this is computed, an exception happens.
     */
    public void printInfo() {
        if (!isComputed)
            throw new RuntimeException("Not computed yet. It must be computed before printing information.");
        System.out.println("#Phase:Turning points[km] Jeffrey boundary[km] Propagation delta[deg] time[s]");
        Arrays.stream(PhasePart.values()).forEach(pp -> System.out.println(
                pp + ": " + Precision.round(turningRMap.get(pp), 3) + " " + jeffreysBoundaryMap.get(pp) + " " +
                        propagationMap.get(pp) + " " + Precision.round(Math.toDegrees(deltaMap.get(pp)), 3) + " " +
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
     * @return &Delta; for startR &le; r &le; endR
     * @see Integrand#bySimpsonRule(double, double, double, double)
     */
    private double simpsonDelta(PhasePart pp, double startR, double endR) {
        double deltax = endR - startR;
        double a = WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, startR);
        double b = WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, startR + 0.5 * deltax);
        double c = WOODHOUSE.computeQDelta(pp, RAY_PARAMETER, endR);
        return bySimpsonRule(a, b, c, deltax);
    }

    /**
     * Integrates traveltime by Simpson's Rule
     *
     * @param pp     target phase
     * @param startR [km]
     * @param endR   [km]
     * @return traveltime for startR &le; r &le; endR
     * @see Integrand#bySimpsonRule(double, double, double, double)
     */
    private double simpsonT(PhasePart pp, double startR, double endR) {
        double deltax = endR - startR;
        double a = WOODHOUSE.computeQT(pp, RAY_PARAMETER, startR);
        double b = WOODHOUSE.computeQT(pp, RAY_PARAMETER, startR + 0.5 * deltax);
        double c = WOODHOUSE.computeQT(pp, RAY_PARAMETER, endR);
        return bySimpsonRule(a, b, c, deltax);
    }

    /**
     * Computes diffraction on a boundary at r. The velocity is considered as
     * the one at r &pm; {@link ComputationalMesh#eps}
     *
     * @param pp              phase part for the diffraction
     * @param boundaryR       [km]
     * @param deltaOnBoundary [rad]
     * @param shallower       The diffraction happens on the shallower(true) or
     *                        deeper(false) side of the boundary. Shallower means larger
     *                        radius.
     * @return T[s] along the boundary.
     * @throws RuntimeException If the structure has no boundary at the input boundaryR.
     */
    private double computeTAlongBoundary(PhasePart pp, double boundaryR, double deltaOnBoundary, boolean shallower) {
        if (ComputationalMesh.eps < Math.abs(coreMantleBoundary() - boundaryR) &&
                ComputationalMesh.eps < Math.abs(innerCoreBoundary() - boundaryR) &&
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
