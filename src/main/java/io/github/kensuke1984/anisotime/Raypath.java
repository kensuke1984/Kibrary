package io.github.kensuke1984.anisotime;

import static io.github.kensuke1984.kibrary.math.Integrand.bySimpsonRule;
import static io.github.kensuke1984.kibrary.math.Integrand.jeffreysMethod1;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.Function;

import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;

import io.github.kensuke1984.kibrary.math.Integrand;
import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * 
 * Every depth is written as <b>radius [km]</b>. Every angle value returns in
 * [rad]. The raypath is of a given ray parameter p.
 * 
 * Cannot compute travel time for structures that have layers with 0 velocity
 * and non zero velocity. Layers must have only non zero velocity or zero
 * velocity.
 * 
 * now diffraction phase is computable but only for the raypath which turning R
 * is near the CMB by {@link #permissibleGapForDiff}
 * 
 * 
 * calculation of a travel time and a epicentral distance region 0 near turning
 * depth (eps +turningDepth) will be calculated by Geffreys region 1 deeper part
 * than event depth but not including region 0 region 2 shallower part than
 * event depth to the surface
 * <p>
 * &Delta; (delta) denotes epicentral distance.<br>
 * T (time) denotes travel time. &tau; (tau) denotes tau. Q<sub>T</sub>&ne;Q
 * <sub>&tau;</sub>
 * 
 * TODO if rstart and rend is too close, ...
 * <p>
 * P: P wave in the mantle<br>
 * SV,SH: S wave (SV, SH) in the mantle<br>
 * K: P(K) wave in the outer-core<br>
 * I: P(I) wave in the inner-core<br>
 * JV,JH: SV, SH(J) wave in the inner-core<br>
 * 
 * 
 * @author Kensuke Konishi
 * 
 * @version 0.4.1b
 * @see Woodhouse, 1981
 */
public class Raypath implements Serializable, Comparable<Raypath> {

	/**
	 * generated on 2016/5/11
	 */
	private static final long serialVersionUID = -4590397277743992353L;

	/**
	 * If the gap between the CMB and the turning r is under this value, then
	 * diffracted phase can be computed.
	 */
	static final double permissibleGapForDiff = 1e-5;

	/**
	 * @return {@link VelocityStructure} of this raypath.
	 */
	VelocityStructure getStructure() {
		return WOODHOUSE.getStructure();
	}

	/**
	 * &delta;&Delta;<sub>i</sub> at r<sub>i</sub> &le; r &le; r<sub>i+1</sub>
	 * (i = 0, 1, ..., n-1)
	 * <p>
	 * r<sub>i</sub> is in {@link ComputationalMesh}<br>
	 * r<sub>0</sub> = core-mantle boundary, r<sub>n</sub> = surface. <u>The
	 * number of &delta;$Delta; is 1 less than the number of the mesh.</u>
	 * <p>
	 * &delta;&Delta; = 0 where the raypath does not reach or the range is in
	 * the Jefferey's range.
	 */
	private transient double[] thetaP;

	/**
	 * Same as {@link #thetaP}
	 */
	private transient double[] thetaSV;

	/**
	 * Same as {@link #thetaP}
	 */
	private transient double[] thetaSH;

	/**
	 * Same as {@link #thetaP}
	 */
	private transient double[] thetaK;

	/**
	 * Same as {@link #thetaP}
	 */
	private transient double[] thetaI;

	/**
	 * Same as {@link #thetaP}
	 */
	private transient double[] thetaJH;

	/**
	 * Same as {@link #thetaP}
	 */
	private transient double[] thetaJV;

	/**
	 * 
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
	private transient double[] dTP;

	/**
	 * Same as {@link #dTP}
	 */
	private transient double[] dTSV;

	/**
	 * Same as {@link #dTP}
	 */
	private transient double[] dTSH;

	/**
	 * Same as {@link #dTP}
	 */
	private transient double[] dTK;

	/**
	 * Same as {@link #dTP}
	 */
	private transient double[] dTI;

	/**
	 * Same as {@link #dTP}
	 */
	private transient double[] dTJV;

	/**
	 * Same as {@link #dTP}
	 */
	private transient double[] dTJH;

	private final double RAYPARAMETER; // rayparameter p = (r * sin(t) )/ v(r)

	/**
	 * Radius [km] at which P bounces. The value can be only in mantle. If there
	 * is no bouncing point, the value is {@link Double#NaN}
	 */
	private transient double pTurningR;

	/**
	 * Turning radius for SV (mantle). Same as {@link #pTurningR}
	 */
	private transient double svTurningR;

	/**
	 * Turning radius for SH (mantle). Same as {@link #pTurningR}
	 */
	private transient double shTurningR;

	/**
	 * Radius [km] at which K bounces. The value can be in the outer-core. If
	 * there is no bouncing point, the value is {@link Double#NaN}
	 */
	private transient double kTurningR;

	/**
	 * Same as {@link #pTurningR}. Turning radius for I.
	 */
	private transient double iTurningR;
	/**
	 * Turning radius for JV. Same as {@link #pTurningR}
	 */
	private transient double jvTurningR;
	/**
	 * Turning radius for JH. Same as {@link #pTurningR}
	 */
	private transient double jhTurningR;

	private final Woodhouse1981 WOODHOUSE;

	/**
	 * &Delta; of P in the mantle
	 */
	private transient double deltaP = Double.NaN;
	/**
	 * &Delta; of SV in the mantle
	 */
	private transient double deltaSV = Double.NaN;
	/**
	 * &Delta; of SH in the mantle
	 */
	private transient double deltaSH = Double.NaN;
	/**
	 * &Delta; of K in the outer-core
	 */
	private transient double deltaK = Double.NaN;
	/**
	 * &Delta; of I in the inner-core
	 */
	private transient double deltaI = Double.NaN;
	/**
	 * &Delta; of JV in the inner-core
	 */
	private transient double deltaJV = Double.NaN;
	/**
	 * &Delta; of JH in the inner-core
	 */
	private transient double deltaJH = Double.NaN;

	/**
	 * T of P in the mantle
	 */
	private transient double timeP = Double.NaN;
	/**
	 * T of SV in the mantle
	 */
	private transient double timeSV = Double.NaN;
	/**
	 * T of SH in the mantle
	 */
	private transient double timeSH = Double.NaN;
	/**
	 * T of K in the outer-core
	 */
	private transient double timeK = Double.NaN;
	/**
	 * T of I in the inner-core
	 */
	private transient double timeI = Double.NaN;
	/**
	 * T of JV in the inner-core
	 */
	private transient double timeJV = Double.NaN;
	/**
	 * T of JH in the inner-core
	 */
	private transient double timeJH = Double.NaN;

	private transient double jeffreysPDelta = Double.NaN;
	private transient double jeffreysSVDelta = Double.NaN;
	private transient double jeffreysSHDelta = Double.NaN;
	private transient double jeffreysKDelta = Double.NaN;
	private transient double jeffreysIDelta = Double.NaN;
	private transient double jeffreysJVDelta = Double.NaN;
	private transient double jeffreysJHDelta = Double.NaN;

	private transient double jeffreysPT = Double.NaN;
	private transient double jeffreysSVT = Double.NaN;
	private transient double jeffreysSHT = Double.NaN;
	private transient double jeffreysKT = Double.NaN;
	private transient double jeffreysIT = Double.NaN;
	private transient double jeffreysJVT = Double.NaN;
	private transient double jeffreysJHT = Double.NaN;

	/**
	 * Maximum radius of the Jeffrey's range for P integration. (
	 * {@link #pTurningR} + {@link #JEFFREYS_EPS})
	 * <p>
	 * If either CMB or ICB is in the Jeffrey's range, the boundary is it.
	 */
	private transient double jeffreysPBoundary;

	/**
	 * Same as {@link #jeffreysPBoundary}
	 */
	private transient double jeffreysSVBoundary;

	/**
	 * Same as {@link #jeffreysPBoundary}
	 */
	private transient double jeffreysSHBoundary;

	/**
	 * Same as {@link #jeffreysPBoundary}
	 */
	private transient double jeffreysKBoundary;

	/**
	 * Same as {@link #jeffreysPBoundary}
	 */
	private transient double jeffreysIBoundary;

	/**
	 * Same as {@link #jeffreysPBoundary}
	 */
	private transient double jeffreysJVBoundary;

	/**
	 * Same as {@link #jeffreysPBoundary}
	 */
	private transient double jeffreysJHBoundary;

	/**
	 * Mesh for integration
	 */
	private final ComputationalMesh MESH;

	/**
	 * ray parameter p the source is on the surface PREM structure
	 * 
	 * @param rayParameterP
	 *            a ray parameter
	 */
	public Raypath(double rayParameterP) {
		this(rayParameterP, PolynomialStructure.PREM);
	}

	/**
	 * @param rayParameterP
	 *            the ray parameter
	 * @param structure
	 *            {@link VelocityStructure}
	 */
	public Raypath(double rayParameterP, VelocityStructure structure) {
		this(rayParameterP, structure, null);
	}

	/**
	 * @param rayParameterP
	 *            the ray parameter
	 * @param structure
	 *            {@link VelocityStructure}
	 * @param interval
	 *            [km] interval for computation points
	 * @param jeffereysEPS
	 *            [km] range for jeffreys computation
	 */
	public Raypath(double rayParameter, VelocityStructure structure, ComputationalMesh mesh) {
		this(rayParameter, new Woodhouse1981(structure), mesh);
	}

	Raypath(double rayParameter, Woodhouse1981 woodhouse, ComputationalMesh mesh) {
		RAYPARAMETER = rayParameter;
		WOODHOUSE = woodhouse;
		MESH = mesh == null ? ComputationalMesh.simple() : mesh;
		setTurningRs();
		computeJeffreysRange();
	}

	/**
	 * @param phase
	 *            the target phase
	 * @return bottom Radius of the input phase[km]
	 */
	double bottomingR(Phase phase) {
		Partition pReach = phase.pReaches();
		Partition sReach = phase.sReaches();
		double sBottom = earthRadius();
		double pBottom = earthRadius();
		if (sReach != null)
			switch (sReach) {
			case CORE_MANTLE_BOUNDARY:
				sBottom = coreMantleBoundary();
				break;
			case INNER_CORE_BOUNDARY:
				sBottom = innerCoreBoundary();
				break;
			case MANTLE:
				sBottom = phase.isPSV() ? svTurningR : shTurningR;
				break;
			case INNERCORE:
				sBottom = phase.isPSV() ? jvTurningR : jhTurningR;
				break;
			default:
				throw new RuntimeException("UNEXPECTED");
			}
		if (pReach != null)
			switch (pReach) {
			case CORE_MANTLE_BOUNDARY:
				pBottom = coreMantleBoundary();
				break;
			case INNER_CORE_BOUNDARY:
				pBottom = innerCoreBoundary();
				break;
			case INNERCORE:
			case MANTLE:
			case OUTERCORE:
				pBottom = pTurningR;
				break;
			default:
				throw new RuntimeException("UNEXPECTED");
			}
		return pBottom < sBottom ? pBottom : sBottom;
	}

	/**
	 * Create an information file of {@link Phase}
	 * 
	 * @param informationFile
	 *            Path of an informationFile
	 * 
	 * @param phase
	 *            Seismic {@link Phase}
	 */
	public void outputInfo(Path informationFile, double eventR, Phase phase) {
		try (PrintWriter pw = new PrintWriter(informationFile.toFile())) {
			pw.println("Phase: " + phase);
			pw.println("Ray parameter: " + RAYPARAMETER);
			pw.println("Epicentral distance[deg]: " + Math.toDegrees(computeDelta(eventR, phase)));
			pw.println("Travel time[s]: " + computeT(eventR, phase));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param dataFile
	 *            Path of a dataFile
	 * @param phase
	 *            to output
	 */
	public void outputDat(Path dataFile, double eventR, Phase phase) {
		if (!exists(eventR, phase))
			return;
		try (PrintWriter os = new PrintWriter(dataFile.toFile())) {
			double[][] points = getRoute(eventR, phase);
			os.println("#Radius[km] Theta[deg]");
			if (points != null)
				for (int i = 0; i < points.length; i++)
					os.println(points[i][0] + " " + Math.toDegrees(points[i][1]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create an EPS file of {@link Phase}
	 * 
	 * @param epsFile
	 *            Path of an eps file name
	 * @param phase
	 *            Seismic {@link Phase}
	 */
	public void outputEPS(double eventR, Path epsFile, Phase phase) {
		if (!exists(eventR, phase))
			return;
		try (BufferedOutputStream os = new BufferedOutputStream(Files.newOutputStream(epsFile))) {
			RaypathPanel panel = new RaypathPanel(earthRadius(), coreMantleBoundary(), innerCoreBoundary());
			// panel.setResults(rayParameter, computeDelta(phase),
			// computeTraveltime(phase));
			double[][] points = getRouteXY(eventR, phase);
			if (points != null) {
				double[] x = new double[points.length];
				double[] y = new double[points.length];
				for (int i = 0; i < points.length; i++) {
					// lines.add(points[i][0] + " " + points[i][1]);
					// System.out.println(points[i][0]+" "+points[i][1]);
					x[i] = points[i][0];
					y[i] = points[i][1];
				}
				panel.addPath(x, y);
			}
			panel.toEPS(os, phase, RAYPARAMETER, computeDelta(eventR, phase), computeT(eventR, phase), eventR);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Compute travel times and deltas in layers. This method must be done
	 * before {@link #computeDelta(Phase)} or {@link #computeTraveltime(Phase)}.
	 * If once this method is called, it does not compute anymore in the future.
	 */
	public void compute() {
		if (isComputed)
			return;
		synchronized (this) {
			if (isComputed)
				return;
			computeTransients();
			isComputed = true;
			hasTransients = true;
		}
	}

	public static void main(String[] args) throws Exception { // 0
																// PKIKP
		// TODO p小さい時
		long t = System.nanoTime(); // TODO
		Raypath r = new Raypath(3, VelocityStructure.prem(), new ComputationalMesh(VelocityStructure.prem(), 1, 1, 1));
		// System.exit(0);
		r.compute();
		r.printTurningPoints();
		r.printDelta();
		r.printT();
		Phase phase = Phase.PKIKP;
		System.out.println(phase + " " + Math.toDegrees(r.computeDelta(6371, phase)) + " " + r.computeT(6371, phase));
		// writeTest();
		// readTest();
		System.out.println(Math.toDegrees(r.jeffreysIDelta));
		System.out.println(Utilities.toTimeString(System.nanoTime() - t));
	}

	private void computeTransients() {
		if (hasTransients)
			return;
		synchronized (this) {
			if (hasTransients)
				return;
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
	 * @serialData
	 * @param stream
	 *            to be read
	 * @throws ClassNotFoundException
	 *             if happens
	 * @throws IOException
	 *             if any
	 */
	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();
		int flag = stream.readByte();
		deltaP = Double.NaN;
		timeP = Double.NaN;
		deltaSV = Double.NaN;
		timeSV = Double.NaN;
		deltaSH = Double.NaN;
		timeSH = Double.NaN;
		deltaK = Double.NaN;
		timeK = Double.NaN;
		deltaI = Double.NaN;
		timeI = Double.NaN;
		deltaJV = Double.NaN;
		timeJV = Double.NaN;
		deltaJH = Double.NaN;
		timeJH = Double.NaN;
		if ((flag & 1) != 0) {
			deltaP = stream.readDouble();
			timeP = stream.readDouble();
		}
		if ((flag & 2) != 0) {
			deltaSV = stream.readDouble();
			timeSV = stream.readDouble();
		}
		if ((flag & 4) != 0) {
			deltaSH = stream.readDouble();
			timeSH = stream.readDouble();
		}
		if ((flag & 8) != 0) {
			deltaK = stream.readDouble();
			timeK = stream.readDouble();
		}
		if ((flag & 16) != 0) {
			deltaI = stream.readDouble();
			timeI = stream.readDouble();
		}
		if ((flag & 32) != 0) {
			deltaJV = stream.readDouble();
			timeJV = stream.readDouble();
		}
		if ((flag & 64) != 0) {
			deltaJH = stream.readDouble();
			timeJH = stream.readDouble();
		}
		setTurningRs();
		computeJeffreysRange();
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		byte flag = 0;
		List<Double> outputList = new ArrayList<>();
		if (propagationP != Propagation.NOEXIST) {
			flag |= 1;
			outputList.add(deltaP);
			outputList.add(timeP);
		}
		if (propagationSV != Propagation.NOEXIST) {
			flag |= 2;
			outputList.add(deltaSV);
			outputList.add(timeSV);
		}
		if (propagationSH != Propagation.NOEXIST) {
			flag |= 4;
			outputList.add(deltaSH);
			outputList.add(timeSH);
		}
		if (propagationK != Propagation.NOEXIST) {
			flag |= 8;
			outputList.add(deltaK);
			outputList.add(timeK);
		}
		if (propagationI != Propagation.NOEXIST) {
			flag |= 16;
			outputList.add(deltaI);
			outputList.add(timeI);
		}
		if (propagationJV != Propagation.NOEXIST) {
			flag |= 32;
			outputList.add(deltaJV);
			outputList.add(timeJV);
		}
		if (propagationJH != Propagation.NOEXIST) {
			flag |= 64;
			outputList.add(deltaJH);
			outputList.add(timeJH);
		}
		stream.writeByte(flag);
		for (double value : outputList)
			stream.writeDouble(value);
	}

	/**
	 * If this method has &Delta; and T for partitions.
	 */
	private boolean isComputed;

	/**
	 * If this has transient values the route theta and time.
	 */
	private transient boolean hasTransients;

	/**
	 * TODO Range is from the turning point to a radius which is good enough for
	 * a given mesh threshold ({@link ComputationalMesh#integralThreshold}).
	 * 
	 * Each boundary is one of the radius set in {@link #MESH}.
	 */
	private void computeJeffreysRange() {
		jeffreysPBoundary = Double.NaN;
		jeffreysSVBoundary = Double.NaN;
		jeffreysSHBoundary = Double.NaN;
		jeffreysKBoundary = Double.NaN;
		jeffreysIBoundary = Double.NaN;
		jeffreysJVBoundary = Double.NaN;
		jeffreysJHBoundary = Double.NaN;

		Function<PhasePart, Double> candidate = pp -> {
			int index = MESH.getNextIndexOf(turningROf(pp), pp.whichPartition());
			RealVector mesh = MESH.getMesh(pp.whichPartition());
			double boundary = mesh.getEntry(index);
			while (++index < mesh.getDimension()) {
				double next = mesh.getEntry(index);
				double q = WOODHOUSE.computeQT(pp, RAYPARAMETER, boundary);
				double qNext = WOODHOUSE.computeQT(pp, RAYPARAMETER, next);
				double ratio = q < qNext ? q / qNext : qNext / q;
				if (MESH.integralThreshold < ratio)
					break;
				boundary = next;
			}
			return boundary;
		};

		// P
		if (!Double.isNaN(pTurningR)) {
			jeffreysPBoundary = candidate.apply(PhasePart.P);
			jeffreysPDelta = jeffreysDelta(PhasePart.P, jeffreysPBoundary);
			jeffreysPT = jeffreysT(PhasePart.P, jeffreysPBoundary);
		}

		// SV
		if (!Double.isNaN(svTurningR)) {
			jeffreysSVBoundary = candidate.apply(PhasePart.SV);
			jeffreysSVDelta = jeffreysDelta(PhasePart.SV, jeffreysSVBoundary);
			jeffreysSVT = jeffreysT(PhasePart.SV, jeffreysSVBoundary);
		}

		// SH
		if (!Double.isNaN(shTurningR)) {
			jeffreysSHBoundary = candidate.apply(PhasePart.SH);
			jeffreysSHDelta = jeffreysDelta(PhasePart.SH, jeffreysSHBoundary);
			jeffreysSHT = jeffreysT(PhasePart.SH, jeffreysSHBoundary);
		}

		// K
		if (!Double.isNaN(kTurningR)) {
			jeffreysKBoundary = candidate.apply(PhasePart.K);
			jeffreysKDelta = jeffreysDelta(PhasePart.K, jeffreysKBoundary);
			jeffreysKT = jeffreysT(PhasePart.K, jeffreysKBoundary);
		}

		// I
		if (!Double.isNaN(iTurningR) && propagationI != Propagation.PENETRATING) {
			jeffreysIBoundary = candidate.apply(PhasePart.I);
			jeffreysIDelta = jeffreysDelta(PhasePart.I, jeffreysIBoundary);
			jeffreysIT = jeffreysT(PhasePart.I, jeffreysIBoundary);
		}

		// JV
		if (!Double.isNaN(jvTurningR) && propagationJV != Propagation.PENETRATING) {
			jeffreysJVBoundary = candidate.apply(PhasePart.JV);
			jeffreysJVDelta = jeffreysDelta(PhasePart.JV, jeffreysJVBoundary);
			jeffreysJVT = jeffreysT(PhasePart.JV, jeffreysJVBoundary);
		}

		// JH
		if (!Double.isNaN(jhTurningR) && propagationJH != Propagation.PENETRATING) {
			jeffreysJHBoundary = candidate.apply(PhasePart.JH);
			jeffreysJHDelta = jeffreysDelta(PhasePart.JH, jeffreysJHBoundary);
			jeffreysJHT = jeffreysT(PhasePart.JH, jeffreysJHBoundary);
		}
	}

	private double[] getDT(PhasePart pp) {
		switch (pp) {
		case I:
			return dTI;
		case JH:
			return dTJH;
		case JV:
			return dTJV;
		case K:
			return dTK;
		case P:
			return dTP;
		case SH:
			return dTSH;
		case SV:
			return dTSV;
		default:
			throw new RuntimeException("uNexpected");
		}
	}

	private double[] getDTheta(PhasePart pp) {
		switch (pp) {
		case I:
			return thetaI;
		case JH:
			return thetaJH;
		case JV:
			return thetaJV;
		case K:
			return thetaK;
		case P:
			return thetaP;
		case SH:
			return thetaSH;
		case SV:
			return thetaSV;
		default:
			throw new RuntimeException("uNexpected");
		}
	}

	private void setT(PhasePart pp, double t) {
		switch (pp) {
		case I:
			timeI = t;
			return;
		case JH:
			timeJH = t;
			return;
		case JV:
			timeJV = t;
			return;
		case K:
			timeK = t;
			return;
		case P:
			timeP = t;
			return;
		case SH:
			timeSH = t;
			return;
		case SV:
			timeSV = t;
			return;
		default:
			throw new RuntimeException("unexpecteD");
		}
	}

	private void setDelta(PhasePart pp, double t) {
		switch (pp) {
		case I:
			deltaI = t;
			return;
		case JH:
			deltaJH = t;
			return;
		case JV:
			deltaJV = t;
			return;
		case K:
			deltaK = t;
			return;
		case P:
			deltaP = t;
			return;
		case SH:
			deltaSH = t;
			return;
		case SV:
			deltaSV = t;
			return;
		default:
			throw new RuntimeException("unexpecteD");
		}
	}

	/**
	 * Computes T (travel time) for the mantle, outer-core and inner-core. It
	 * also computes transient arrays of &delta;&Delta; and &delta;T.
	 */
	private void computeT() {
		RealVector mantleMesh = MESH.getMesh(Partition.MANTLE);
		RealVector outerCoreMesh = MESH.getMesh(Partition.OUTERCORE);
		RealVector innerCoreMesh = MESH.getMesh(Partition.INNERCORE);
		int mantle = mantleMesh.getDimension() - 1;
		int outerCore = outerCoreMesh.getDimension() - 1;
		int innerCore = innerCoreMesh.getDimension() - 1;

		Function<PhasePart, Thread> createThread = pp -> {
			RealVector mesh = MESH.getMesh(pp.whichPartition());
			double[] dT = getDT(pp);
			return new Thread(() -> {
				double jeffreysBoundary = getJeffreysBoundary(pp);
				for (int i = 0; i < dT.length; i++) {
					if (mesh.getEntry(i) < jeffreysBoundary)
						continue;
					dT[i] = simpsonT(pp, mesh.getEntry(i), mesh.getEntry(i + 1));
				}
				setT(pp, computeT(pp, getPropagation(pp) == Propagation.PENETRATING ? mesh.getEntry(0) : turningROf(pp),
						mesh.getEntry(dT.length)));
			});
		};

		List<Thread> runningThread = new ArrayList<>(7);
		// mantle P
		Thread pThread;
		if (propagationP != Propagation.NOEXIST) {
			dTP = new double[mantle];
			pThread = createThread.apply(PhasePart.P);
			runningThread.add(pThread);
		}

		// mantle S
		Thread svThread;
		if (propagationSV != Propagation.NOEXIST) {
			dTSV = new double[mantle];
			svThread = createThread.apply(PhasePart.SV);
			runningThread.add(svThread);
		}

		Thread shThread;
		if (propagationSH != Propagation.NOEXIST) {
			dTSH = new double[mantle];
			shThread = createThread.apply(PhasePart.SH);
			runningThread.add(shThread);
		}

		// outer-core
		Thread kThread;
		if (propagationK != Propagation.NOEXIST) {
			dTK = new double[outerCore];
			kThread = createThread.apply(PhasePart.K);
			runningThread.add(kThread);
		}

		// inner-core P
		Thread iThread;
		if (propagationI != Propagation.NOEXIST) {
			dTI = new double[innerCore];
			iThread = createThread.apply(PhasePart.I);
			runningThread.add(iThread);
		}

		// inner-core S
		Thread jvThread;
		if (propagationJV != Propagation.NOEXIST) {
			dTJV = new double[innerCore];
			jvThread = createThread.apply(PhasePart.JV);
			runningThread.add(jvThread);
		}

		Thread jhThread;
		if (propagationJH != Propagation.NOEXIST) {
			dTJH = new double[innerCore];
			jhThread = createThread.apply(PhasePart.JH);
			runningThread.add(jhThread);
		}
		runningThread.forEach(Thread::start);

		try {
			for (Thread t : runningThread)
				t.join();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not compute travel time.");
		}
	}

	private double getJeffreysBoundary(PhasePart pp) {
		switch (pp) {
		case I:
			return jeffreysIBoundary;
		case JH:
			return jeffreysJHBoundary;
		case JV:
			return jeffreysJVBoundary;
		case K:
			return jeffreysKBoundary;
		case P:
			return jeffreysPBoundary;
		case SH:
			return jeffreysSHBoundary;
		case SV:
			return jeffreysSVBoundary;
		default:
			throw new RuntimeException("UNEXpected");
		}
	}

	private double getJeffreysDelta(PhasePart pp) {
		switch (pp) {
		case I:
			return jeffreysIDelta;
		case JH:
			return jeffreysJHDelta;
		case JV:
			return jeffreysJVDelta;
		case K:
			return jeffreysKDelta;
		case P:
			return jeffreysPDelta;
		case SH:
			return jeffreysSHDelta;
		case SV:
			return jeffreysSVDelta;
		default:
			throw new RuntimeException("UNEXpected");
		}
	}

	private double getJeffreysT(PhasePart pp) {
		switch (pp) {
		case I:
			return jeffreysIT;
		case JH:
			return jeffreysJHT;
		case JV:
			return jeffreysJVT;
		case K:
			return jeffreysKT;
		case P:
			return jeffreysPT;
		case SH:
			return jeffreysSHT;
		case SV:
			return jeffreysSVT;
		default:
			throw new RuntimeException("UNEXpected");
		}
	}

	/**
	 * This method computes T. The rStart and rEnd must be in the same partition
	 * (mantle or inner-core).
	 * 
	 * @param startR
	 *            start value of integration
	 * @param endR
	 *            end value of integration
	 * @return T for startR &le; r &le; endR
	 */
	private double computeT(PhasePart pp, double startR, double endR) {
		Partition partition = pp.whichPartition();
		RealVector radii = MESH.getMesh(partition);
		double minR = radii.getEntry(0);
		double maxR = radii.getEntry(radii.getDimension() - 1);
		if (startR < minR || endR < startR || maxR + ComputationalMesh.eps < endR)
			throw new IllegalArgumentException("Input rStart and rEnd are invalid.");

		if (startR < turningROf(pp))
			throw new IllegalArgumentException("Input rStart is deeper than the boucing point.");

		if (getPropagation(pp) == Propagation.NOEXIST)
			return Double.NaN;

		if (endR - startR < ComputationalMesh.eps)
			return 0;

		startR = Math.max(startR, minR);
		endR = Math.min(endR, maxR);

		double jeffreysBoundary = getJeffreysBoundary(pp);
		double jeffreysT = getJeffreysT(pp);

		// might be NaN
		if (endR <= jeffreysBoundary)
			return jeffreysT(pp, endR) - jeffreysT(pp, startR);

		int beginIndex = MESH.getNextIndexOf(startR, partition) + 1;
		int endIndex = MESH.getNextIndexOf(endR, partition);
		if (endIndex < beginIndex) {
			if (jeffreysBoundary <= startR)
				return simpsonT(pp, startR, endR);
			double delta = simpsonT(pp, jeffreysBoundary, endR);
			double jeff = jeffreysT - jeffreysT(pp, startR);
			if (Double.isNaN(jeff))
				throw new RuntimeException("YoCHECK");
			return delta + jeff;
		}

		double nextREnd = radii.getEntry(endIndex);
		double time = simpsonT(pp, nextREnd < jeffreysBoundary ? jeffreysBoundary : nextREnd, endR);
		for (int i = beginIndex; i < endIndex; i++) {
			if (radii.getEntry(i) < jeffreysBoundary)
				continue;
			time += simpsonT(pp, radii.getEntry(i), radii.getEntry(i + 1));
		}

		if (Double.isNaN(jeffreysBoundary) || jeffreysBoundary <= startR)
			return time + simpsonT(pp, startR, radii.getEntry(beginIndex));

		int indexJeffreyNext = MESH.getNextIndexOf(jeffreysBoundary, partition) + 1;
		time += simpsonT(pp, jeffreysBoundary, radii.getEntry(indexJeffreyNext));

		double jeffreys = jeffreysT - jeffreysT(pp, startR);
		if (Double.isNaN(jeffreys))
			throw new RuntimeException("youcheckya");
		return time + jeffreys;
	}

	public void printDelta() {
		System.out.println("Delta[deg] P:" + Precision.round(Math.toDegrees(deltaP), 3) + " SV:"
				+ Precision.round(Math.toDegrees(deltaSV), 3) + " SH:" + Precision.round(Math.toDegrees(deltaSH), 3)
				+ " K:" + Precision.round(Math.toDegrees(deltaK), 3) + " I:"
				+ Precision.round(Math.toDegrees(deltaI), 3) + " JV:" + Precision.round(Math.toDegrees(deltaJV), 3)
				+ " JH:" + Precision.round(Math.toDegrees(deltaJH), 3));
	}

	public void printT() {
		System.out.println("T[s] P:" + Precision.round(timeP, 3) + " SV:" + Precision.round(timeSV, 3) + " SH:"
				+ Precision.round(timeSH, 3) + " K:" + Precision.round(timeK, 3) + " I:" + Precision.round(timeI, 3)
				+ " JV:" + Precision.round(timeJV, 3) + " JH:" + Precision.round(timeJH, 3));
	}

	/**
	 * Compute delta for a input {@link Phase}
	 * 
	 * @param eventR
	 *            [km] must be in the mantle
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return Epicentral distance[rad] for the phase if the phase does not
	 *         exist or anything wrong, returns Double.NaN
	 */
	public double computeDelta(double eventR, Phase phase) {
		if (!exists(eventR, phase))
			return Double.NaN;

		double mp = phase.getCountOfP();
		double ms = phase.getCountOfS();
		double oc = phase.getCountOfK();
		double icp = phase.getCountOfI();
		double ics = phase.getCountOfJ();

		// System.out.println(mp+" "+ms+" "+oc+" "+icp+" "+ics);
		double p = 0 < mp ? deltaP * mp * 2 : 0;
		double s = 0 < ms ? (phase.isPSV() ? deltaSV : deltaSH) * ms * 2 : 0;
		double k = 0 < oc ? deltaK * oc * 2 : 0;
		double i = 0 < icp ? deltaI * icp * 2 : 0;
		double j = 0 < ics ? (phase.isPSV() ? deltaJV : deltaJH) * ics * 2 : 0;
		double delta = p + s + k + i + j;

		if (phase.isDiffracted())
			delta += phase.getDiffractionAngle();

		if (Math.abs(eventR - earthRadius()) < ComputationalMesh.eps)
			return delta;

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
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return Travel time[s] for the phase
	 */
	public double computeT(double eventR, Phase phase) {
		if (!exists(eventR, phase))
			return Double.NaN;

		double mp = phase.getCountOfP();
		double ms = phase.getCountOfS();
		double oc = phase.getCountOfK();
		double icp = phase.getCountOfI();
		double ics = phase.getCountOfJ();

		double p = 0 < mp ? timeP * mp * 2 : 0;
		double s = 0 < ms ? (phase.isPSV() ? timeSV : timeSH) * ms * 2 : 0;
		double k = 0 < oc ? timeK * oc * 2 : 0;
		double i = 0 < icp ? timeI * icp * 2 : 0;
		double j = 0 < ics ? (phase.isPSV() ? timeJV : timeJH) * ics * 2 : 0;
		double time = p + s + k + i + j;
		if (phase.isDiffracted()) {
			double deltaOnCMB = phase.getDiffractionAngle();
			time += phase.toString().contains("Pdiff")
					? computeTimePAlongBoundary(coreMantleBoundary(), deltaOnCMB, true)
					: computeTimeSAlongBoundary(phase.isPSV(), coreMantleBoundary(), deltaOnCMB, true);
		}

		if (Math.abs(eventR - earthRadius()) <= ComputationalMesh.eps)
			return time;

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
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return if name exists for the rayparameter
	 */
	public boolean exists(double eventR, Phase phase) {
		String nameStr = phase.toString();
		if (nameStr.contains("Pdiff"))
			return getPropagation(PhasePart.P) == Propagation.DIFFRACTION;
		if (nameStr.contains("Sdiff"))
			return getPropagation(phase.isPSV() ? PhasePart.SV : PhasePart.SH) == Propagation.DIFFRACTION;

		if (nameStr.startsWith("p") || nameStr.startsWith("s"))
			if (Math.abs(eventR - earthRadius()) < ComputationalMesh.eps)
				return false;
		if (nameStr.contains("P"))
			if (eventR < pTurningR)
				return false;
		if (nameStr.contains("S"))
			if (eventR < (phase.isPSV() ? svTurningR : shTurningR)) //
				return false;
		return phase.exists(this);
	}

	/**
	 * @return radius of turning point
	 */
	public double getTurningR(PhasePart pp) {
		switch (pp) {
		case I:
			return iTurningR;
		case JH:
			return jhTurningR;
		case JV:
			return jvTurningR;
		case K:
			return kTurningR;
		case P:
			return pTurningR;
		case SH:
			return shTurningR;
		case SV:
			return svTurningR;
		default:
			throw new RuntimeException("uNeXpected");
		}
	}

	/**
	 * @return ray parameter
	 */
	public double getRayParameter() {
		return RAYPARAMETER;
	}

	/**
	 * 
	 * rList must not be empty. &Delta; and T is computed for range rStart &le;
	 * r &le; nextR. rStart is the last entry of rList. (The $Delta; + the last
	 * entry of thetaList) is added to the thetaList. T is same. The nextR and
	 * the last entry of rList must be in a same partition.
	 * 
	 * 
	 * @param nextR
	 *            [km]
	 * @param rList
	 *            to add nextR
	 * @param thetaList
	 *            to add &Delta;
	 * @param travelTimeList
	 *            to add T
	 */
	private void addRThetaTime(double nextR, PhasePart pp, List<Double> rList, List<Double> thetaList,
			List<Double> travelTimeList) {
		double beforeR = rList.get(rList.size() - 1);
		if (Math.abs(beforeR - coreMantleBoundary()) < ComputationalMesh.eps
				&& Math.abs(nextR - coreMantleBoundary()) < ComputationalMesh.eps)
			throw new RuntimeException("korekarayaruo");

		double smallerR = Math.min(beforeR, nextR);
		double biggerR = Math.max(beforeR, nextR);
		double theta = computeDelta(pp, smallerR, biggerR);
		double time = computeT(pp, smallerR, biggerR);
		int lastIndex = rList.size() - 1;
		rList.add(nextR);
		thetaList.add(theta + thetaList.get(lastIndex));
		travelTimeList.add(time + travelTimeList.get(lastIndex));
	}

	/**
	 * 
	 * @param eventR
	 *            [km] must be in the mantle
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return route[point]{r, theta} <br>
	 *         r[km], theta[rad]<br>
	 *         route [i] is a set of radius([i][0]), theta([i][1]), travel
	 *         time([i][2]).<br>
	 *         theta of ith point indicates the $Delta; between i th point and
	 *         epicenter. trravel time is same. 0 th point is epicenter.
	 */
	public double[][] getRoute(double eventR, Phase phase) {
		if (earthRadius() < eventR || eventR <= coreMantleBoundary())
			throw new IllegalArgumentException("Input eventR:" + eventR + " is out of the mantle.");
		if (!exists(eventR, phase))
			throw new RuntimeException(phase + " does not exist.");

		List<Double> rList = new ArrayList<>();
		List<Double> thetaList = new ArrayList<>();
		List<Double> travelTimeList = new ArrayList<>();
		RealVector mantleMesh = MESH.getMesh(Partition.MANTLE);

		// First phase because we must take care of the eventR
		// [eventR, bottomR (turningR or CMB)]
		rList.add(eventR);
		thetaList.add(0d);
		travelTimeList.add(0d);
		if (phase.partIsDownGoing(0)) {
			int indexEventR = MESH.getNextIndexOf(eventR, Partition.MANTLE);
			if (mantleMesh.getEntry(indexEventR) == eventR)
				indexEventR--;
			PhasePart pp = phase.phasePartOf(0);
			if (getPropagation(pp) == Propagation.PENETRATING)
				for (int iR = indexEventR; 0 <= iR; iR--)
					addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
			else {
				double jeffreysBoundary = getJeffreysBoundary(pp);
				int bottomIndex = MESH.getNextIndexOf(jeffreysBoundary, Partition.MANTLE);
				for (int iR = indexEventR; bottomIndex < iR; iR--)
					addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
				addRThetaTime(jeffreysBoundary, pp, rList, thetaList, travelTimeList);
				addRThetaTime(turningROf(pp), pp, rList, thetaList, travelTimeList);
			}
		} else
			for (int iR = MESH.getNextIndexOf(eventR, Partition.MANTLE) + 1, n = mantleMesh
					.getDimension(); iR < n; iR++)
				addRThetaTime(mantleMesh.getEntry(iR), phase.phasePartOf(0), rList, thetaList, travelTimeList);

		for (int i = 1; i < phase.getNPart(); i++) {
			boolean isDownGoing = phase.partIsDownGoing(i);
			PhasePart pp = phase.phasePartOf(i);
			switch (phase.partitionOf(i)) {
			case CORE_MANTLE_BOUNDARY:
				addRThetaTime(coreMantleBoundary(), pp, rList, thetaList, travelTimeList);
				break;
			case MANTLE:
				if (getPropagation(pp) == Propagation.PENETRATING)
					if (isDownGoing)
						for (int iR = mantleMesh.getDimension() - 2; 0 <= iR; iR--)
							addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
					else
						for (int iR = 1, n = mantleMesh.getDimension(); iR < n; iR++)
							addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
				else {
					double jeffreysBoundary = getJeffreysBoundary(pp);
					int bottomIndex = MESH.getNextIndexOf(jeffreysBoundary, Partition.MANTLE);
					if (isDownGoing) {
						for (int iR = mantleMesh.getDimension() - 2; bottomIndex < iR; iR--)
							addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
						addRThetaTime(jeffreysBoundary, pp, rList, thetaList, travelTimeList);
						addRThetaTime(getTurningR(pp), pp, rList, thetaList, travelTimeList);
					} else {
						addRThetaTime(jeffreysBoundary, pp, rList, thetaList, travelTimeList);
						for (int iR = bottomIndex + 1, n = mantleMesh.getDimension(); iR < n; iR++)
							addRThetaTime(mantleMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
					}
				}
				break;
			case OUTERCORE:
				RealVector outerCoreMesh = MESH.getMesh(Partition.OUTERCORE);
				if (propagationK == Propagation.PENETRATING) {
					if (isDownGoing)
						for (int iR = outerCoreMesh.getDimension() - 2; 0 <= iR; iR--)
							addRThetaTime(outerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
					else
						for (int iR = 1, n = outerCoreMesh.getDimension(); iR < n; iR++)
							addRThetaTime(outerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
				} else {
					int bottomIndex = MESH.getNextIndexOf(jeffreysKBoundary, Partition.OUTERCORE);
					if (isDownGoing) {
						for (int iR = outerCoreMesh.getDimension() - 2; bottomIndex < iR; iR--)
							addRThetaTime(outerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
						addRThetaTime(jeffreysKBoundary, pp, rList, thetaList, travelTimeList);
						addRThetaTime(kTurningR, pp, rList, thetaList, travelTimeList);
					} else {
						addRThetaTime(jeffreysKBoundary, pp, rList, thetaList, travelTimeList);
						for (int iR = bottomIndex + 1, n = outerCoreMesh.getDimension(); iR < n; iR++)
							addRThetaTime(outerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
					}
				}
				break;
			case INNERCORE:
				double jeffreysBoundary = getJeffreysBoundary(pp);
				RealVector innerCoreMesh = MESH.getMesh(Partition.INNERCORE);
				int bottomIndex = MESH.getNextIndexOf(jeffreysBoundary, Partition.INNERCORE);
				if (isDownGoing) {
					for (int iR = innerCoreMesh.getDimension() - 2; bottomIndex < iR; iR--)
						addRThetaTime(innerCoreMesh.getEntry(iR), pp, rList, thetaList, travelTimeList);
					addRThetaTime(jeffreysBoundary, pp, rList, thetaList, travelTimeList);
					addRThetaTime(getTurningR(pp), pp, rList, thetaList, travelTimeList);
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
	 * The center of the Earth is (0, 0) Starting point is (0, {@link #eventR})
	 * 
	 * @param phase
	 *            Seismic {@link Phase}
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
			double x = rTheta[i][0] * Math.sin(rTheta[i][1]);
			double y = rTheta[i][0] * Math.cos(rTheta[i][1]);
			points[i] = new double[] { x, y };
		}

		return points;
	}

	VelocityStructure getVelocityStructure() {
		return WOODHOUSE.getStructure();
	}

	/**
	 * @return Radius of the inner core boundary [km]
	 */
	double innerCoreBoundary() {
		return WOODHOUSE.getStructure().innerCoreBoundary();
	}

	private double turningROf(PhasePart pp) {
		switch (pp) {
		case P:
			return pTurningR;
		case I:
			return iTurningR;
		case JH:
			return jhTurningR;
		case JV:
			return jvTurningR;
		case K:
			return kTurningR;
		case SH:
			return shTurningR;
		case SV:
			return svTurningR;
		default:
			throw new RuntimeException("UNExpECTED");
		}
	}

	private double jeffreysDelta(PhasePart pp, double rEnd) {
		double turningR = turningROf(pp);
		if (Math.abs(rEnd - turningR) <= ComputationalMesh.eps)
			return 0;
		DoubleFunction<Double> rToY = r -> WOODHOUSE.computeQDelta(pp, RAYPARAMETER, r) * drdx(pp, r);
		double rCenter = (rEnd + turningR) / 2;
		double modifiedR = rCenter + (toX(pp, rEnd) / 2 - toX(pp, rCenter)) * drdx(pp, rCenter);
		if (Double.isNaN(modifiedR) || modifiedR <= turningR || rEnd <= modifiedR)
			modifiedR = rCenter;
		return jeffreysMethod1(toX(pp, modifiedR), rToY.apply(modifiedR), rToY.apply(rEnd));
	}

	private double jeffreysT(PhasePart pp, double rEnd) {
		double turningR = turningROf(pp);
		if (Math.abs(rEnd - turningR) <= ComputationalMesh.eps)
			return 0;
		DoubleFunction<Double> rToY = r -> WOODHOUSE.computeQT(pp, RAYPARAMETER, r) * drdx(pp, r);
		double rCenter = (rEnd + turningR) / 2;
		double modifiedR = rCenter + (toX(pp, rEnd) / 2 - toX(pp, rCenter)) * drdx(pp, rCenter);
		if (Double.isNaN(modifiedR) || modifiedR <= turningR || rEnd <= modifiedR)
			modifiedR = rCenter;
		return jeffreysMethod1(toX(pp, modifiedR), rToY.apply(modifiedR), rToY.apply(rEnd));
	}

	private double[] getTheta(PhasePart pp) {
		switch (pp) {
		case I:
			return thetaI;
		case JH:
			return thetaJH;
		case JV:
			return thetaJV;
		case K:
			return thetaK;
		case P:
			return thetaP;
		case SH:
			return thetaSH;
		case SV:
			return thetaSV;
		default:
			throw new RuntimeException("uNeXpected");
		}
	}

	/**
	 * x &equiv; q<sub>&tau;</sub><sup>2</sup>
	 * 
	 * @param r
	 *            [km]
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
			return Math.pow(WOODHOUSE.computeQTau(pp, RAYPARAMETER, r), 2);
		case K:
			return 1 - Math.pow(RAYPARAMETER * Math.sqrt(getStructure().getA(r) / getStructure().getRho(r)) / r, 2);
		}
		throw new RuntimeException("unEXPeCteD");
	}

	private double drdx(PhasePart pp, double r) {
		return ComputationalMesh.eps / (toX(pp, r + ComputationalMesh.eps) - toX(pp, r));
	}

	/**
	 * This method computes &Delta; by precomputed values. The rStart and rEnd
	 * must be in the mantle.
	 * 
	 * @param startR
	 *            [km]
	 * @param endR
	 *            [km]
	 * @return &Delta; for rStart &le; r &le; rEnd
	 */
	private double computeDelta(PhasePart pp, double startR, double endR) {
		Partition partition = pp.whichPartition();
		RealVector radii = MESH.getMesh(partition);
		double minR = radii.getEntry(0);
		double maxR = radii.getEntry(radii.getDimension() - 1);
		if (startR < minR || endR < startR || maxR + ComputationalMesh.eps < endR)
			throw new IllegalArgumentException("Input rStart and rEnd are invalid.");

		if (startR < turningROf(pp))
			throw new IllegalArgumentException("Input rStart is deeper than the boucing point.");

		if (getPropagation(pp) == Propagation.NOEXIST)
			return Double.NaN;

		if (endR - startR < ComputationalMesh.eps)
			return 0;

		startR = Math.max(startR, minR);
		endR = Math.min(endR, maxR);

		double jeffreysBoundary = getJeffreysBoundary(pp);
		double jeffreysDelta = getJeffreysDelta(pp);
		// might be NaN
		if (endR <= jeffreysBoundary)
			return jeffreysDelta(pp, endR) - jeffreysDelta(pp, startR);

		int firstIndexForMemory = MESH.getNextIndexOf(startR, partition) + 1;
		int endIndexForMemory = MESH.getNextIndexOf(endR, partition);
		if (endIndexForMemory < firstIndexForMemory) {
			if (jeffreysBoundary <= startR)
				return simpsonDelta(pp, startR, endR);
			double delta = simpsonDelta(pp, jeffreysBoundary, endR);
			double jeff = jeffreysDelta - jeffreysDelta(pp, startR);
			if (Double.isNaN(jeff))
				throw new RuntimeException("YoCHECK");
			return delta + jeff;
		}
		double nextREnd = radii.getEntry(endIndexForMemory);
		double delta = simpsonDelta(pp, nextREnd < jeffreysBoundary ? jeffreysBoundary : nextREnd, endR);
		double[] theta = getTheta(pp);
		for (int i = firstIndexForMemory; i < endIndexForMemory; i++)
			delta += theta[i];

		if (Double.isNaN(jeffreysBoundary) || jeffreysBoundary <= startR)
			return delta + simpsonDelta(pp, startR, radii.getEntry(firstIndexForMemory));
		int indexJeffreyNext = MESH.getNextIndexOf(jeffreysBoundary, partition) + 1;
		delta += simpsonDelta(pp, jeffreysBoundary, radii.getEntry(indexJeffreyNext));

		double jeffreys = jeffreysDelta - jeffreysDelta(pp, startR);
		if (Double.isNaN(jeffreys))
			throw new RuntimeException("youtcheckya");
		return delta + jeffreys;
	}

	/**
	 * Computes &Delta; for the mesh and compute the ones for the mantle,
	 * outer-core and inner-core. If one does not exist, the value is default (
	 * {@link Double#NaN}).
	 * 
	 * TODO boundary value
	 */
	private void computeDelta() {
		RealVector mantleMesh = MESH.getMesh(Partition.MANTLE);
		RealVector outerCoreMesh = MESH.getMesh(Partition.OUTERCORE);
		RealVector innerCoreMesh = MESH.getMesh(Partition.INNERCORE);
		int mantle = mantleMesh.getDimension() - 1;
		int outerCore = outerCoreMesh.getDimension() - 1;
		int innerCore = innerCoreMesh.getDimension() - 1;
		Function<PhasePart, Thread> createThread = pp -> {
			RealVector mesh = MESH.getMesh(pp.whichPartition());
			double[] dTheta = getDTheta(pp);
			return new Thread(() -> {
				double jeffreysBoundary = getJeffreysBoundary(pp);
				for (int i = 0; i < dTheta.length; i++) {
					if (mesh.getEntry(i) < jeffreysBoundary)
						continue;
					dTheta[i] = simpsonDelta(pp, mesh.getEntry(i), mesh.getEntry(i + 1));
				}
				setDelta(pp,
						computeDelta(pp,
								getPropagation(pp) == Propagation.PENETRATING ? mesh.getEntry(0) : turningROf(pp),
								mesh.getEntry(dTheta.length)));
			});
		};

		List<Thread> runningThread = new ArrayList<>(7);
		// mantle P
		Thread pThread;
		if (propagationP != Propagation.NOEXIST) {
			thetaP = new double[mantle];
			pThread = createThread.apply(PhasePart.P);
			runningThread.add(pThread);
		}

		// mantle S
		Thread svThread;
		if (propagationSV != Propagation.NOEXIST) {
			thetaSV = new double[mantle];
			svThread = createThread.apply(PhasePart.SV);
			runningThread.add(svThread);
		}
		Thread shThread;
		if (propagationSH != Propagation.NOEXIST) {
			thetaSH = new double[mantle];
			shThread = createThread.apply(PhasePart.SH);
			runningThread.add(shThread);
		}

		// outer-core
		Thread kThread;
		if (propagationK != Propagation.NOEXIST) {
			thetaK = new double[outerCore];
			kThread = createThread.apply(PhasePart.K);
			runningThread.add(kThread);
		}

		// inner-core P
		Thread iThread;
		if (propagationI != Propagation.NOEXIST) {
			thetaI = new double[innerCore];
			iThread = createThread.apply(PhasePart.I);
			runningThread.add(iThread);
		}

		// inner-core S
		Thread jvThread;
		if (propagationJV != Propagation.NOEXIST) {
			thetaJV = new double[innerCore];
			jvThread = createThread.apply(PhasePart.JV);
			runningThread.add(jvThread);
		}
		Thread jhThread;
		if (propagationJH != Propagation.NOEXIST) {
			thetaJH = new double[innerCore];
			jhThread = createThread.apply(PhasePart.JH);
			runningThread.add(jhThread);
		}
		runningThread.forEach(Thread::start);
		try {
			for (Thread t : runningThread)
				t.join();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not compute epicentral distances.");
		}
	}

	private transient Propagation propagationP;
	private transient Propagation propagationI;
	private transient Propagation propagationK;

	private transient Propagation propagationSV;
	private transient Propagation propagationSH;
	private transient Propagation propagationJV;
	private transient Propagation propagationJH;

	/**
	 * @param sv
	 *            true:SV, false:SH
	 * @return propagation of S in the inner-core
	 */
	Propagation getPropagation(PhasePart pp) {
		switch (pp) {
		case I:
			return propagationI;
		case JH:
			return propagationJH;
		case JV:
			return propagationJV;
		case K:
			return propagationK;
		case P:
			return propagationP;
		case SH:
			return propagationSH;
		case SV:
			return propagationSV;
		default:
			throw new RuntimeException("UNEXpected");
		}
	}

	/**
	 * computes turning radius
	 * 
	 * TODO turningr is now separated maybe if sentence can be reduced in the
	 * latrepart
	 */
	private void setTurningRs() {
		VelocityStructure vstructure = getStructure();
		pTurningR = vstructure.pTurningR(RAYPARAMETER);
		svTurningR = vstructure.svTurningR(RAYPARAMETER);
		shTurningR = vstructure.shTurningR(RAYPARAMETER);
		kTurningR = vstructure.kTurningR(RAYPARAMETER);
		iTurningR = vstructure.iTurningR(RAYPARAMETER);
		jvTurningR = vstructure.jvTurningR(RAYPARAMETER);
		jhTurningR = vstructure.jhTurningR(RAYPARAMETER);

		if (!Double.isNaN(kTurningR))
			propagationK = Propagation.BOUNCING;
		else
			propagationK = Double
					.isNaN(WOODHOUSE.computeQT(PhasePart.K, RAYPARAMETER, innerCoreBoundary() + ComputationalMesh.eps))
							? Propagation.NOEXIST : Propagation.PENETRATING;

		propagationI = Double.isNaN(WOODHOUSE.computeQT(PhasePart.I, RAYPARAMETER, ComputationalMesh.eps))
				? Propagation.NOEXIST : Propagation.PENETRATING;
		propagationP = Double
				.isNaN(WOODHOUSE.computeQT(PhasePart.P, RAYPARAMETER, coreMantleBoundary() + ComputationalMesh.eps))
						? Propagation.NOEXIST : Propagation.PENETRATING;

		if (coreMantleBoundary() + permissibleGapForDiff < pTurningR)
			propagationP = Propagation.BOUNCING;
		else if (coreMantleBoundary() <= pTurningR)
			propagationP = Propagation.DIFFRACTION;

		if (!Double.isNaN(iTurningR))
			propagationI = Propagation.BOUNCING;

		propagationJV = Double.isNaN(WOODHOUSE.computeQT(PhasePart.JV, RAYPARAMETER, ComputationalMesh.eps))
				? Propagation.NOEXIST : Propagation.PENETRATING;
		propagationSV = Double
				.isNaN(WOODHOUSE.computeQT(PhasePart.SV, RAYPARAMETER, coreMantleBoundary() + ComputationalMesh.eps))
						? Propagation.NOEXIST : Propagation.PENETRATING;

		propagationJH = Double.isNaN(WOODHOUSE.computeQT(PhasePart.JH, RAYPARAMETER, ComputationalMesh.eps))
				? Propagation.NOEXIST : Propagation.PENETRATING;
		propagationSH = Double
				.isNaN(WOODHOUSE.computeQT(PhasePart.SH, RAYPARAMETER, coreMantleBoundary() + ComputationalMesh.eps))
						? Propagation.NOEXIST : Propagation.PENETRATING;

		if (coreMantleBoundary() + permissibleGapForDiff < svTurningR)
			propagationSV = Propagation.BOUNCING;
		else if (coreMantleBoundary() <= svTurningR)
			propagationSV = Propagation.DIFFRACTION;
		if (!Double.isNaN(jvTurningR))
			propagationJV = Propagation.BOUNCING;

		if (coreMantleBoundary() + permissibleGapForDiff < shTurningR)
			propagationSH = Propagation.BOUNCING;
		else if (coreMantleBoundary() <= shTurningR)
			propagationSH = Propagation.DIFFRACTION;
		if (!Double.isNaN(jhTurningR))
			propagationJH = Propagation.BOUNCING;

		if (propagationP == Propagation.BOUNCING
				&& Double.isNaN(WOODHOUSE.computeQT(PhasePart.P, RAYPARAMETER, pTurningR + ComputationalMesh.eps)))
			propagationP = Propagation.NOEXIST;

		if (propagationSV == Propagation.BOUNCING
				&& Double.isNaN(WOODHOUSE.computeQT(PhasePart.SV, RAYPARAMETER, svTurningR + ComputationalMesh.eps)))
			propagationSV = Propagation.NOEXIST;

		if (propagationSH == Propagation.BOUNCING
				&& Double.isNaN(WOODHOUSE.computeQT(PhasePart.SH, RAYPARAMETER, shTurningR + ComputationalMesh.eps)))
			propagationSH = Propagation.NOEXIST;

		if (propagationK == Propagation.BOUNCING
				&& Double.isNaN(WOODHOUSE.computeQT(PhasePart.K, RAYPARAMETER, kTurningR + ComputationalMesh.eps)))
			propagationK = Propagation.NOEXIST;

		if (propagationJV == Propagation.BOUNCING
				&& Double.isNaN(WOODHOUSE.computeQT(PhasePart.JV, RAYPARAMETER, jvTurningR + ComputationalMesh.eps)))
			propagationJV = Propagation.NOEXIST;

		if (propagationJH == Propagation.BOUNCING
				&& Double.isNaN(WOODHOUSE.computeQT(PhasePart.JH, RAYPARAMETER, jhTurningR + ComputationalMesh.eps)))
			propagationJH = Propagation.NOEXIST;
	}

	public void printTurningPoints() {
		System.out.println("#Turning points");
		System.out.println("#Phase radius, Jeffrey boundary");
		System.out.println("P:" + pTurningR + " " + jeffreysPBoundary);
		System.out.println("SV:" + svTurningR + " " + jeffreysSVBoundary);
		System.out.println("SH:" + shTurningR + " " + jeffreysSHBoundary);
		System.out.println("K:" + kTurningR + " " + jeffreysKBoundary);
		System.out.println("I:" + iTurningR + " " + jeffreysIBoundary);
		System.out.println("JV:" + jvTurningR + " " + jeffreysJVBoundary);
		System.out.println("JH:" + jhTurningR + " " + jeffreysJHBoundary);
		System.out.println("#Propagation");
		System.out.println("P:" + propagationP + " SV:" + propagationSV + " SH:" + propagationSH + " K:" + propagationK
				+ " I:" + propagationI + " JV:" + propagationJV + " JH:" + propagationJH);
	}

	/**
	 * Integrates &Delta; by Simpson's Rule
	 * 
	 * @param pp
	 *            target phase
	 * @param startR
	 *            [km]
	 * @param endR
	 *            [km]
	 * @return &Delta; for startR &le; r &le; endR
	 * @see Integrand#bySimpsonRule(double, double, double)
	 */
	private double simpsonDelta(PhasePart pp, double startR, double endR) {
		double deltax = endR - startR;
		double a = WOODHOUSE.computeQDelta(pp, RAYPARAMETER, startR);
		double b = WOODHOUSE.computeQDelta(pp, RAYPARAMETER, startR + 0.5 * deltax);
		double c = WOODHOUSE.computeQDelta(pp, RAYPARAMETER, endR);
		return bySimpsonRule(a, b, c, deltax);
	}

	/**
	 * Integrates traveltime by Simpson's Rule
	 * 
	 * @param pp
	 *            target phase
	 * @param startR
	 *            [km]
	 * @param endR
	 *            [km]
	 * @return traveltime for startR &le; r &le; endR
	 * @see Integrand#bySimpsonRule(double, double, double)
	 */
	private double simpsonT(PhasePart pp, double startR, double endR) {
		double deltax = endR - startR;
		double a = WOODHOUSE.computeQT(pp, RAYPARAMETER, startR);
		double b = WOODHOUSE.computeQT(pp, RAYPARAMETER, startR + 0.5 * deltax);
		double c = WOODHOUSE.computeQT(pp, RAYPARAMETER, endR);
		return bySimpsonRule(a, b, c, deltax);
	}

	/**
	 * Computes diffraction on a boundary at r. The velocity is considered as
	 * the one at r &pm; {@link ComputationalMesh#eps}
	 * 
	 * @param sv
	 *            true:SV, false:SH
	 * @param boundaryR
	 *            [km]
	 * @param deltaOnBoundary
	 *            [rad]
	 * @param shallower
	 *            The diffraction happens true:shallower or false:deeper side.
	 *            Shallower means larger radius.
	 * @return S wave traveltime along the boundary
	 * @throws RuntimeException
	 *             If the structure has no boundary at r.
	 */
	private double computeTimeSAlongBoundary(boolean sv, double boundaryR, double deltaOnBoundary, boolean shallower) {
		if (ComputationalMesh.eps < Math.abs(coreMantleBoundary() - boundaryR)
				&& ComputationalMesh.eps < Math.abs(innerCoreBoundary() - boundaryR)
				&& Arrays.stream(WOODHOUSE.getStructure().additionalBoundaries())
						.allMatch(b -> ComputationalMesh.eps < Math.abs(b - boundaryR)))
			throw new RuntimeException("The input radius " + boundaryR + " is not a boundary.");
		double s = boundaryR * deltaOnBoundary;
		double r = boundaryR + (shallower ? ComputationalMesh.eps : -ComputationalMesh.eps);
		double velocity = Math.sqrt((sv ? WOODHOUSE.getStructure().getL(r) : WOODHOUSE.getStructure().getN(r))
				/ WOODHOUSE.getStructure().getRho(r));
		return s / velocity;
	}

	/**
	 * Computes diffraction on a boundary at r. The velocity is considered as
	 * the one at r &pm; {@link ComputationalMesh#eps}
	 * 
	 * @param boundaryR
	 *            [km]
	 * @param deltaOnBoundary
	 *            [rad]
	 * @param shallower
	 *            The diffraction happens true:shallower or false:deeper side.
	 *            Shallower means larger radius.
	 * @return P wave traveltime along the boundary at the boundaryR
	 * @throws RuntimeException
	 *             If the structure has no boundary at r.
	 */
	private double computeTimePAlongBoundary(double boundaryR, double deltaOnBoundary, boolean shallower) {
		if (ComputationalMesh.eps < Math.abs(coreMantleBoundary() - boundaryR)
				&& ComputationalMesh.eps < Math.abs(innerCoreBoundary() - boundaryR)
				&& Arrays.stream(WOODHOUSE.getStructure().additionalBoundaries())
						.allMatch(b -> ComputationalMesh.eps < Math.abs(b - boundaryR)))
			throw new RuntimeException("The input radius " + boundaryR + " is not a boundary.");
		double r = boundaryR + (shallower ? ComputationalMesh.eps : -ComputationalMesh.eps);
		double s = boundaryR * deltaOnBoundary;
		double velocity = Math.sqrt(WOODHOUSE.getStructure().getA(r) / WOODHOUSE.getStructure().getRho(r));
		return s / velocity;
	}

	@Override
	public int compareTo(Raypath o) {
		return Double.compare(RAYPARAMETER, o.RAYPARAMETER);
	}
}
