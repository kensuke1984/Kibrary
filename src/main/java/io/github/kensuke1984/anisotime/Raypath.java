package io.github.kensuke1984.anisotime;

import static io.github.kensuke1984.kibrary.math.Integrand.bySimpsonRule;
import static io.github.kensuke1984.kibrary.math.Integrand.jeffreysMethod3;

import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Every depth is written as <b>radius [km]</b>. The raypath is of a given ray
 * parameter p.
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
 * 
 * @author Kensuke Konishi
 * 
 * 
 * @version 0.3.9.2
 * 
 * 
 */
public class Raypath {

	/**
	 * when integrate values on boundaries, use the value at point very close to
	 * the boundaries by eps. Default value is 1e-7
	 */
	private final double eps = 0.0000001;

	private final double eventR; // as the initial value

	public double getEventR() {
		return eventR;
	}

	VelocityStructure getStructure() {
		return structure;
	}

	private double innerCorePDelta;

	/**
	 * R of innercore for Pwave [layer] ={r1, r2, ....} r1 < r2
	 */
	private double[] innerCorePR;
	/**
	 * 
	 * each point is R of {@link #innerCorePR} theta from the bottom point of P
	 * inner core [layer] = { theta1, theta2, ... }theta1 < theta2
	 */
	private double[] innerCorePTheta;
	private double innerCorePTime;
	private double innerCoreSDelta;
	/**
	 * R of innercore for S [layer] ={r1, r2, ....} r1 < r2
	 */
	private double[] innerCoreSR;
	/**
	 * each point is R of {@link #innerCorePR} theta from the bottom point of P
	 * mantle [layer] = { theta1, theta2, ... }theta1 < theta2
	 */
	private double[] innerCoreSTheta;
	private double innerCoreSTime;

	/**
	 * the interval of calculation points (/km)
	 */
	private double interval = 1;

	/**
	 * the region near the turning point. the integral for this region is
	 * obtained by Jeffereys eps cannot be big. If it if big... might be ignored
	 * like the case if turningR is closer to boundaries
	 */
	private double jeffereysEPS = 10;
	private double mantlePDelta;

	/**
	 * R of mantle [layer] ={r1, r2, ....} r1 < r2
	 */
	private double[] mantlePR;

	/**
	 * each point is R of {@link #mantlePR} theta from the bottom point of P
	 * mantle [layer] = { theta1, theta2, ... }theta1 < theta2
	 */
	private double[] mantlePTheta;
	private double mantlePTime;
	private double mantleSDelta;

	/**
	 * R of mantle [layer] ={r1, r2, ....} r1 < r2
	 */
	private double[] mantleSR;

	/**
	 * each point is R of {@link #mantlePR} theta from the bottom point of S
	 * mantle [layer] = { theta1, theta2, ... }theta1 < theta2
	 */
	private double[] mantleSTheta;

	private double mantleSTime;

	private double outerCoreDelta;

	/**
	 * R of outercore [layer] ={r1, r2, ....} r1 < r2
	 */
	private double[] outerCoreR;

	/**
	 * each point is R of {@link #outerCoreR} theta from the bottom point of P
	 * outer core [layer] = { theta1, theta2, ... }theta1 < theta2
	 * 
	 */
	private double[] outerCoreTheta;

	private double outerCoreTime;

	/**
	 * if the gap between the CMB and the turning r is under this value, then
	 * diffracted phase can be computed
	 */
	private static double permissibleGapForDiff = 10e-9;

	private Partition pTurning;

	private double pTurningR;

	private final double rayParameter; // rayparameter p = (r * sin(t) )/ v(r)
	private Partition shTurning;
	private double shTurningR;
	private final VelocityStructure structure;

	/**
	 * compute SV(true) or SH(false) default SH
	 */
	private boolean sv;

	private Partition svTurning;

	private double svTurningR;

	private double upperPDelta;

	private double upperPTime;

	private double upperSDelta;

	private double upperSTime;

	/**
	 * ray parameter p the source is on the surface PREM structure
	 * 
	 * @param rayParameterP
	 *            a ray parameter
	 */
	public Raypath(double rayParameterP) {
		this(rayParameterP, 0, PolynomialStructure.PREM, false);
	}

	/**
	 * ray parameter p PREM structure
	 * 
	 * @param rayParameterP
	 *            a ray parameter
	 * @param eventR
	 *            the radius of the hypocenter
	 */
	public Raypath(double rayParameterP, double eventR) {
		this(rayParameterP, eventR, PolynomialStructure.PREM, false);
	}

	/**
	 * @param rayParameterP
	 *            the ray parameter
	 * @param eventR
	 *            the radius of the hypocenter
	 * @param structure
	 *            {@link VelocityStructure}
	 */
	public Raypath(double rayParameterP, double eventR, VelocityStructure structure) {
		this(rayParameterP, eventR, structure, false);
	}

	/**
	 * @param rayParameterP
	 *            the ray parameter
	 * @param eventR
	 *            the radius of the hypocenter
	 * @param structure
	 *            {@link VelocityStructure}
	 * @param sv
	 *            if true computes SV, else SH
	 */
	public Raypath(double rayParameterP, double eventR, VelocityStructure structure, boolean sv) {
		this.sv = sv;
		this.rayParameter = rayParameterP;
		this.eventR = 0 < eventR ? eventR : structure.earthRadius();
		this.structure = structure;

		if (!checkPValidity())
			throw new RuntimeException("Input p is invalid.");

		setTurningRs();
		// compute();
	}

	public boolean isSv() {
		return sv;
	}

	/**
	 * @param phase
	 * @return bottom Radius of the input phase[km]
	 */
	double bottomingR(Phase phase) {
		Partition pReach = phase.pReaches();
		Partition sReach = phase.sReaches();
		double sBottom = structure.earthRadius();
		double pBottom = structure.earthRadius();
		if (sReach != null)
			switch (sReach) {
			case CORE_MANTLE_BOUNDARY:
				sBottom = structure.coreMantleBoundary();
				break;
			case INNER_CORE_BAUNDARY:
				sBottom = structure.innerCoreBoundary();
				break;
			case MANTLE:
			case INNERCORE:
				sBottom = getSTurningR();
				break;
			default:
				throw new RuntimeException("UNEXPECTED");
			}
		if (pReach != null)
			switch (pReach) {
			case CORE_MANTLE_BOUNDARY:
				pBottom = structure.coreMantleBoundary();
				break;
			case INNER_CORE_BAUNDARY:
				pBottom = structure.innerCoreBoundary();
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

	private double calcQDeltaP(double r) {
		double r2 = r * r;
		return rayParameter / r2 / calcQTauP(r)
				* (calcS3(r) + (calcS4(r) * rayParameter * rayParameter / r2 + calcS5(r)) / calcR(r));
	}

	private double calcQDeltaS(double r) {
		double r2 = r * r;
		return sv
				? rayParameter / r2 / calcQTauS(r)
						* (calcS3(r) - (calcS4(r) * rayParameter * rayParameter / r2 + calcS5(r)) / calcR(r))
				: rayParameter * structure.getN(r) / structure.getL(r) / calcQTauS(r) / r2;
	}

	private double calcQTauP(double r) {
		return Math.sqrt(calcS1(r) - calcS3(r) * rayParameter * rayParameter / r / r - calcR(r));
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
	public void outputInfo(Path informationFile, Phase phase) {
		try (PrintWriter pw = new PrintWriter(informationFile.toFile())) {
			pw.println("Phase: " + phase);
			pw.println("Ray parameter: " + rayParameter);
			pw.println("Epicentral distance[deg]: " + Math.toDegrees(computeDelta(phase)));
			pw.println("Travel time[s]: " + computeTraveltime(phase));
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
	public void outputDat(Path dataFile, Phase phase) {
		if (!exists(phase))
			return;
		try (PrintWriter os = new PrintWriter(dataFile.toFile())) {
			double[][] points = getRoute(phase);
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
	public void outputEPS(Path epsFile, Phase phase) {
		if (!exists(phase))
			return;
		try (BufferedOutputStream os = new BufferedOutputStream(Files.newOutputStream(epsFile))) {
			RaypathPanel panel = new RaypathPanel(earthRadius(), coreMantleBoundary(), innerCoreBoundary());
			// panel.setResults(rayParameter, computeDelta(phase),
			// computeTraveltime(phase));
			double[][] points = getRouteXY(phase);
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
			panel.toEPS(os, phase, rayParameter, computeDelta(phase), computeTraveltime(phase), eventR);
			// panel.setFrameVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * q=(Rho/L-NP<sup>2</sup>/(Lr<sup>2</sup>))<sup>0.5</sup>
	 * 
	 * @param r
	 *            radius [km]
	 * @return Q<sub>&tau;</sub>S
	 */
	private double calcQTauS(double r) {
		double r2 = r * r;
		if (sv)
			return Math.sqrt(calcS1(r) - calcS3(r) * rayParameter * rayParameter / r2 + calcR(r));
		double L = structure.getL(r);
		return Math.sqrt(structure.getRho(r) / L - structure.getN(r) * rayParameter * rayParameter / L / r2);
	}

	private double calcQTP(double r) {
		double s2 = calcS2(r);
		// System.out.println(s2);
		return (calcS1(r) - (calcS5(r) * rayParameter * rayParameter / r / r + s2 * s2) / calcR(r)) / calcQTauP(r);
	}

	private double calcQTS(double r) {
		if (sv) {
			double s2 = calcS2(r);
			return (calcS1(r) + (calcS5(r) * rayParameter * rayParameter / r / r + s2 * s2) / calcR(r)) / calcQTauS(r);
		}
		return structure.getRho(r) / structure.getL(r) / calcQTauS(r);
	}

	private double calcR(double r) {
		double s2 = calcS2(r);
		double por = rayParameter / r;
		double por2 = por * por;
		return Math.sqrt(calcS4(r) * por2 * por2 + 2 * calcS5(r) * por2 + s2 * s2);
	}

	private double calcS1(double r) {
		return 0.5 * structure.getRho(r) * (1 / structure.getL(r) + 1 / structure.getC(r));
	}

	private double calcS2(double r) {
		return 0.5 * structure.getRho(r) * (1 / structure.getL(r) - 1 / structure.getC(r));
	}

	private double calcS3(double r) {
		double c = structure.getC(r);
		double f = structure.getF(r);
		double l = structure.getL(r);
		return 0.5 / l / c * (structure.getA(r) * c - f * f - 2 * l * f);
	}

	private double calcS4(double r) {
		double s3 = calcS3(r);
		return s3 * s3 - structure.getA(r) / structure.getC(r);
	}

	private double calcS5(double r) {
		return 0.5 * structure.getRho(r) / structure.getC(r) * (1 + structure.getA(r) / structure.getL(r))
				- calcS1(r) * calcS3(r);
	}

	/**
	 * Check if the input ray parameter is valid 0<= p <= r/vs(at surface) vs is
	 * lower one of Vsv and Vsh
	 * 
	 * @return
	 */
	private boolean checkPValidity() {
		if (rayParameter < 0)
			return false;
		double rho = structure.getRho(structure.earthRadius());
		double vsv = Math.sqrt(structure.getL(structure.earthRadius()) / rho);
		double vsh = Math.sqrt(structure.getN(structure.earthRadius()) / rho);
		double pMax = structure.earthRadius() / (vsv < vsh ? vsv : vsh);
		return rayParameter < pMax;
	}

	/**
	 * Compute travel times and deltas in layers. This method must be done
	 * before {@link #computeDelta(Phase)} or {@link #computeTraveltime(Phase)}.
	 */
	public void compute() {
		computeDelta();
		computeTraveltime();
	}

	void computeDelta() {
		mantlePDelta = mantlePDelta();
		outerCoreDelta = outerCoreDelta();
		innerCorePDelta = innerCorePDelta();
		mantleSDelta = mantleSDelta();
		innerCoreSDelta = innerCoreSDelta();
		upperPDelta = 0;
		upperSDelta = 0;
		double[] upperR = point(eventR, structure.earthRadius(), interval);
		for (int i = 0; i < upperR.length - 1; i++) {
			upperPDelta += simpsonPDelta(upperR[i], upperR[i + 1]);
			upperSDelta += simpsonSDelta(upperR[i], upperR[i + 1]);
		}
	}

	void computeTraveltime() {
		mantlePTime = mantlePTau();
		outerCoreTime = outerCoreTau();
		innerCorePTime = innerCorePTau();
		mantleSTime = mantleSTau();
		innerCoreSTime = innerCoreSTau();
		double[] upperR = point(eventR, structure.earthRadius(), interval);
		upperPTime = 0;
		upperSTime = 0;
		for (int i = 0; i < upperR.length - 1; i++) {
			upperPTime += simpsonPTau(upperR[i], upperR[i + 1]);
			upperSTime += simpsonSTau(upperR[i], upperR[i + 1]);
		}
	}

	/**
	 * Computes delta for an input phase. The source for the raypath is assumed
	 * to be on the surface. If, for instance, an input phase is a depth phase,
	 * then ignore the first upper going part and else if uppergoing part is
	 * added.
	 * 
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return delta for phase &pm; upper going delta
	 */
	public double computeExtendedDelta(Phase phase) {
		double delta = computeDelta(phase);
		char first = phase.toString().charAt(0);
		switch (first) {
		case 'p':
			return delta - upperPDelta;
		case 'P':
			return delta + upperPDelta;
		case 's':
			return delta - upperSDelta;
		case 'S':
			return delta + upperSDelta;
		default:
			return 0;
		}
	}

	/**
	 * Computes traveltime for an input phase. The source for the raypath is
	 * assumed to be on the surface. If, for instance, an input phase is a depth
	 * phase, then ignore the first upper going part and else if uppergoing part
	 * is added.
	 * 
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return travel time for phase &pm; upper going traveltime
	 */
	public double computeExtendedTraveltime(Phase phase) {
		double traveltime = computeTraveltime(phase);

		char first = phase.toString().charAt(0);
		switch (first) {
		case 'p':
			return traveltime - upperPTime;
		case 'P':
			return traveltime + upperPTime;
		case 's':
			return traveltime - upperSTime;
		case 'S':
			return traveltime + upperSTime;
		default:
			return 0;
		}
	}

	/**
	 * Compute delta for a input {@link Phase}
	 * 
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return Epicentral distance[rad] for the phase if the phase does not
	 *         exist or anything wrong, returns Double.NaN
	 */
	public double computeDelta(Phase phase) {
		if (!exists(phase))
			return Double.NaN;

		double delta = 0;
		if (phase.isDiffracted()) {
			double diffDelta = phase.getDiffractionAngle();
			delta = phase.toString().contains("P") ? pDiffDelta(diffDelta) : sDiffDelta(diffDelta);
			if (phase.toString().startsWith("p"))
				delta += upperPDelta + (phase.toString().contains("P") ? upperPDelta : upperSDelta);
			else if (phase.toString().startsWith("s"))
				delta += upperSDelta + (phase.toString().contains("P") ? upperPDelta : upperSDelta);
			return delta;
		}

		double mp = phase.mantleP();
		double ms = phase.mantleS();
		double oc = phase.outerCore();
		double icp = phase.innerCoreP();
		double ics = phase.innerCoreS();
		// System.out.println(ms);

		double mantleP = 0 < mp ? mantlePDelta * mp * 2 : 0;
		double mantleS = 0 < ms ? mantleSDelta * ms * 2 : 0;
		double outerCore = 0 < oc ? outerCoreDelta * oc * 2 : 0;
		double innerCoreP = 0 < icp ? innerCorePDelta * icp * 2 : 0;
		double innerCoreS = 0 < ics ? innerCoreSDelta * ics * 2 : 0;
		delta += mantleP + mantleS + outerCore + innerCoreP + innerCoreS;

		char first = phase.toString().charAt(0);
		switch (first) {
		case 'p':
			return delta + upperPDelta;
		case 'P':
			return delta - upperPDelta;
		case 's':
			return delta + upperSDelta;
		case 'S':
			return delta - upperSDelta;
		}
		return delta;
	}

	/**
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return Travel time[s] for the phase
	 */
	public double computeTraveltime(Phase phase) {
		if (!exists(phase))
			return Double.NaN;

		double traveltime = 0;
		if (phase.isDiffracted()) {
			double delta = phase.getDiffractionAngle();
			traveltime = phase.toString().contains("P") ? pDiffTraveltime(delta) : sDiffTraveltime(delta);
			// System.out.println(delta + " " + traveltime);
			if (phase.toString().startsWith("p"))
				traveltime += upperPTime + (phase.toString().contains("P") ? upperPTime : upperSTime);
			else if (phase.toString().startsWith("s"))
				traveltime += upperSTime + (phase.toString().contains("P") ? upperPTime : upperSTime);
			return traveltime;
		}

		double mp = phase.mantleP();
		double ms = phase.mantleS();
		double oc = phase.outerCore();
		double icp = phase.innerCoreP();
		double ics = phase.innerCoreS();

		double mantleP = 0 < mp ? mantlePTime * mp * 2 : 0;
		double mantleS = 0 < ms ? mantleSTime * ms * 2 : 0;
		double outerCore = 0 < oc ? outerCoreTime * oc * 2 : 0;
		double innerCoreP = 0 < icp ? innerCorePTime * icp * 2 : 0;
		double innerCoreS = 0 < ics ? innerCoreSTime * ics * 2 : 0;
		traveltime += mantleP + mantleS + outerCore + innerCoreP + innerCoreS;
		char first = phase.toString().charAt(0);
		switch (first) {
		case 'p':
			return traveltime + upperPTime;
		case 'P':
			return traveltime - upperPTime;
		case 's':
			return traveltime + upperSTime;
		case 'S':
			return traveltime - upperSTime;
		}

		return traveltime;
	}

	/**
	 * @return Radius of the core mantle boundary[km]
	 */
	double coreMantleBoundary() {
		return structure.coreMantleBoundary();
	}

	/**
	 * @return Earth radius [km]
	 */
	double earthRadius() {
		return structure.earthRadius();
	}

	/**
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return if name exists for the rayparameter
	 */
	public boolean exists(Phase phase) {
		String nameStr = phase.toString();
		if (nameStr.contains("Pdiff"))
			return Math.abs(getPTurningR() - structure.coreMantleBoundary()) < permissibleGapForDiff;
		if (nameStr.contains("Sdiff"))
			return Math.abs(getSTurningR() - structure.coreMantleBoundary()) < permissibleGapForDiff;
		// System.out.println("hi 218" +shTurning);
		if (nameStr.startsWith("p") || nameStr.startsWith("s"))
			if (eventR == structure.earthRadius())
				return false;
		if (pTurning == null || pTurning.equals(Partition.MANTLE))
			if (nameStr.contains("K"))
				return false;
		if (nameStr.contains("P"))
			if (eventR < pTurningR)
				return false;
		if (nameStr.contains("S"))
			if (eventR < (sv ? svTurningR : shTurningR)) //
				return false;
		// System.out.println("Hi"+ pTurning+" "+svTurning);
		if (null == (sv ? svTurning : shTurning)) {
			return phase.turningRValidity(pTurning, Partition.CORE_MANTLE_BOUNDARY);
		}
		return phase.turningRValidity(pTurning, sv ? svTurning : shTurning);
		// return false;
	}

	/**
	 * @return {@link Partition} where P wave turns
	 */
	public Partition getPTurning() {
		return pTurning;
	}

	/**
	 * @return radius of P wave turning point
	 */
	public double getPTurningR() {
		return pTurningR;
	}

	/**
	 * @return ray parameter
	 */
	public double getRayParameter() {
		return rayParameter;
	}

	/**
	 * 
	 * @param phase
	 *            Seismic {@link Phase}
	 * @return [point]{r, theta}
	 */
	public double[][] getRoute(Phase phase) {
		if (!exists(phase)) {
			System.err.println(phase + " does not exist.");
			return null;
		}

		if (mantlePR == null && mantleSR == null)
			throw new RuntimeException("It looks like the Raypath is not computed yet");

		// System.out.println(phase+" "+ shTurningR);
		double[][] points = null;
		List<Double> thetaList = new ArrayList<>();
		List<Double> rList = new ArrayList<>();
		double currentTheta = 0;

		if (phase.isDiffracted()) {
			boolean isDepthPhase = phase.getNPart() == 3;
			// upper part of depth phase
			if (isDepthPhase) {
				boolean isP = phase.partIsP(0);
				int nR = isP ? mantlePR.length : mantleSR.length;
				for (int iR = 0; iR < nR; iR++) {
					double r = isP ? mantlePR[iR] : mantleSR[iR];
					if (r < eventR)
						continue;
					rList.add(r);
					thetaList.add(currentTheta);
					currentTheta += isP ? mantlePTheta[iR] : mantleSTheta[iR];
				}
			}

			boolean p = phase.partIsP(0);
			int nR = p ? mantlePR.length : mantleSR.length;
			double diffractionDelta = phase.getDiffractionAngle();
			// former half of Pdiff or Sdiff
			for (int iR = 0; iR < nR; iR++) {
				int j = nR - 1 - iR;
				double r = p ? mantlePR[j] : mantleSR[j];
				if (!isDepthPhase && eventR < r)
					continue;
				// System.out.println(name.partIsDownGoing(i)+" "+eventR+" "+r);
				rList.add(r);
				currentTheta += p ? mantlePTheta[j] : mantleSTheta[j];
				thetaList.add(currentTheta);
			}
			// System.out.println(diffractionDelta);
			// double startTheta = currentTheta;
			double endTheta = currentTheta + diffractionDelta;
			double dTheta = Math.toRadians(0.5);
			while (currentTheta < endTheta) {
				rList.add(coreMantleBoundary());
				thetaList.add(currentTheta);
				currentTheta += dTheta;
			}
			for (int iR = 0; iR < nR; iR++) {
				int j = iR;
				double r = p ? mantlePR[j] : mantleSR[j];
				// System.out.println(name.partIsDownGoing(i)+" "+eventR+" "+r);
				rList.add(r);
				thetaList.add(currentTheta);
				currentTheta += p ? mantlePTheta[j] : mantleSTheta[j];
			}

		} else {
			// not diffracted
			// p s first exception
			double[] firstR = null;
			double[] firstTheta = null;
			if (phase.partIsP(0)) {
				firstR = mantlePR;
				firstTheta = mantlePTheta;
			} else {
				firstR = mantleSR;
				firstTheta = mantleSTheta;
			}
			for (int iR = 0; iR < firstR.length; iR++) {
				int j = phase.partIsDownGoing(0) ? firstR.length - iR - 1 : iR;
				double r = firstR[j];
				if (eventR < r && phase.partIsDownGoing(0))
					continue;
				if (r < eventR && !phase.partIsDownGoing(0))
					continue;
				// System.out.println(name.partIsDownGoing(i)+" "+eventR+" "+r);
				rList.add(r);
				if (phase.partIsDownGoing(0)) {
					currentTheta += firstTheta[j];
					thetaList.add(currentTheta);
				} else {
					thetaList.add(currentTheta);
					currentTheta += firstTheta[j];
				}
			}

			for (int i = 1; i < phase.getNPart(); i++) {
				// System.out.println("hi" + i);
				switch (phase.partIs(i)) {
				case MANTLE: {
					double[] mantleR = null;
					double[] mantleTheta = null;
					if (phase.partIsP(i)) {
						mantleR = mantlePR;
						mantleTheta = mantlePTheta;
					} else {
						mantleR = mantleSR;
						mantleTheta = mantleSTheta;
					}
					currentTheta += mantleTheta[phase.partIsDownGoing(i) ? mantleR.length - 1 : 0];
					for (int iR = 1; iR < mantleR.length; iR++) {
						int j = phase.partIsDownGoing(i) ? mantleR.length - 1 - iR : iR;
						rList.add(mantleR[j]);
						if (phase.partIsDownGoing(i)) {
							currentTheta += mantleTheta[j];
							thetaList.add(currentTheta);
						} else {
							thetaList.add(currentTheta);
							currentTheta += mantleTheta[j];
						}

					}
				}
					break;
				case OUTERCORE:
					currentTheta += outerCoreTheta[phase.partIsDownGoing(i) ? outerCoreR.length - 1 : 0];
					for (int iR = 1; iR < outerCoreR.length; iR++) {
						int j = phase.partIsDownGoing(i) ? outerCoreR.length - 1 - iR : iR;
						rList.add(outerCoreR[j]);
						if (phase.partIsDownGoing(i)) {
							currentTheta += outerCoreTheta[j];
							thetaList.add(currentTheta);
						} else {
							thetaList.add(currentTheta);
							currentTheta += outerCoreTheta[j];
						}
					}
					break;
				case INNERCORE: {
					double[] innerCoreR = null;
					double[] innerCoreTheta = null;
					if (phase.partIsP(i)) {
						innerCoreR = innerCorePR;
						innerCoreTheta = innerCorePTheta;
					} else {
						innerCoreR = innerCoreSR;
						innerCoreTheta = innerCoreSTheta;
					}
					currentTheta += innerCoreTheta[phase.partIsDownGoing(i) ? innerCoreTheta.length - 1 : 0];
					for (int iR = 1; iR < innerCoreR.length; iR++) {
						if (rayParameter == 0 && iR == 0 && !phase.partIsDownGoing(i))
							currentTheta += Math.PI;
						int j = phase.partIsDownGoing(i) ? innerCoreR.length - iR - 1 : iR;
						rList.add(innerCoreR[j]);
						if (phase.partIsDownGoing(0)) {
							currentTheta += innerCoreTheta[j];
							thetaList.add(currentTheta);
						} else {
							thetaList.add(currentTheta);
							currentTheta += innerCoreTheta[j];
						}
					}
				}
					break;
				default:
					throw new RuntimeException("souteigai");
				}
			}
		}
		points = new double[thetaList.size()][2];
		for (int i = 0; i < points.length; i++) {
			double r = rList.get(i);
			double theta = thetaList.get(i);
			points[i] = new double[] { r, theta };
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
	public double[][] getRouteXY(Phase phase) {
		if (!exists(phase)) {
			System.err.println(phase + " does not exist.");
			return null;
		}
		// System.out.println(phase+" "+ shTurningR);
		double[][] rTheta = getRoute(phase);
		double[][] points = new double[rTheta.length][2];
		for (int i = 0; i < points.length; i++) {
			double x = rTheta[i][0] * Math.sin(rTheta[i][1]);
			double y = rTheta[i][0] * Math.cos(rTheta[i][1]);
			points[i] = new double[] { x, y };
		}

		return points;
	}

	Partition getSHTurning() {
		return shTurning;
	}

	double getSHTurningR() {
		return shTurningR;
	}

	/**
	 * @return {@link Partition} where S wave turns
	 */
	public Partition getSTurning() {
		return sv ? svTurning : shTurning;
	}

	/**
	 * @return [km] radius of a S wave turning point
	 */
	public double getSTurningR() {
		return sv ? svTurningR : shTurningR;
	}

	Partition getSVTurning() {
		return svTurning;
	}

	double getSVTurningR() {
		return svTurningR;
	}

	VelocityStructure getVelocityStructure() {
		return structure;
	}

	/**
	 * @return Radius of the inner core boundary [km]
	 */
	double innerCoreBoundary() {
		return structure.innerCoreBoundary();
	}

	/**
	 * (Half)Delta in inner core
	 * 
	 * @return
	 */
	private double innerCorePDelta() {
		double delta = 0;
		double icbR = structure.innerCoreBoundary();
		if (icbR < pTurningR || pTurningR < 0)
			return 0;
		double startR = pTurningR + jeffereysEPS;

		if (icbR <= startR) {
			double jeff = jeffreysPDelta(pTurningR, icbR);
			innerCorePR = new double[2];
			innerCorePTheta = new double[2];
			innerCorePR[0] = pTurningR;
			innerCorePTheta[0] = jeff;
			innerCorePR[1] = icbR;
			innerCorePTheta[1] = 0;
			return jeff;
		}
		// throw new RuntimeException("eps is too big " + jeffereysEPS);

		// delta = simpsonPDelta(startR, icbR - eps); //
		double[] x = point(startR, icbR - eps, interval);
		double jeff = jeffreysPDelta(pTurningR, startR);
		delta += jeff;
		innerCorePR = new double[x.length + 1];
		innerCorePTheta = new double[x.length + 1];
		innerCorePR[0] = pTurningR;
		innerCorePTheta[0] = jeff;
		for (int i = 0; i < x.length - 1; i++) {
			innerCorePR[i + 1] = x[i];
			double simpson = simpsonPDelta(x[i], x[i + 1]);
			innerCorePTheta[i + 1] = simpson;
			// innerCorePRoute[i + 1] = new double[] { x[i], delta };
			delta += simpson;
		}
		innerCorePR[innerCorePR.length - 1] = icbR - eps;
		innerCorePTheta[innerCorePR.length - 1] = 0;
		// System.out.println(Math.toDegrees(delta));
		// delta *= 2;
		// System.out.println("inner core delta :" + Math.toDegrees(delta));
		return delta;
	}

	/**
	 * (Half)travel time in inner core
	 * 
	 * @return
	 */
	private double innerCorePTau() {
		double icbR = structure.innerCoreBoundary();
		if (pTurningR > icbR || pTurningR < 0)
			return 0;
		double startR = pTurningR + jeffereysEPS;
		if (icbR <= startR)
			// throw new RuntimeException("eps is too big " + jeffereysEPS);
			return jeffreysPTau(pTurningR, icbR - eps);
		double tau = 0;
		// delta = simpsonPDelta(startR, icbR - eps); //
		double[] x = point(startR, icbR - eps, interval);
		for (int i = 0; i < x.length - 1; i++)
			tau += simpsonPTau(x[i], x[i + 1]);
		// tau = simpsonPTau(startR, icbR - eps); //
		tau += jeffreysPTau(pTurningR, startR);

		// tau *= 2;
		// System.out.println("inner core " + tau + " sec");
		return tau;
	}

	/**
	 * (Half) Delta in inner core
	 * 
	 * @return
	 */
	private double innerCoreSDelta() {
		double icbR = structure.innerCoreBoundary();
		double turningR = sv ? svTurningR : shTurningR;
		if (turningR > icbR || turningR < 0)
			return 0;
		double delta = 0;
		double startR = turningR + jeffereysEPS;
		if (icbR <= startR) {
			double jeff = jeffreysSDelta(turningR, icbR);
			innerCoreSR = new double[2];
			innerCoreSTheta = new double[2];
			innerCoreSR[0] = turningR;
			innerCoreSTheta[0] = jeff;
			innerCoreSR[1] = icbR;
			innerCoreSTheta[1] = 0;
			return jeff;
		}
		// throw new RuntimeException("eps is too big " + jeffereysEPS);
		// tau = simpsonSDelta(startR, icbR - eps); //
		double[] x = point(startR, icbR - eps, interval);
		// innerCoreSRoute = new double[x.length][];
		innerCoreSR = new double[x.length + 1];
		innerCoreSTheta = new double[x.length + 1];
		innerCoreSR[0] = turningR;
		double jeff = jeffreysSDelta(turningR, startR);
		innerCoreSTheta[0] = jeff;
		delta += jeff;
		for (int i = 0; i < x.length - 1; i++) {
			double simpson = simpsonSDelta(x[i], x[i + 1]);
			innerCoreSR[i + 1] = x[i];
			innerCoreSTheta[i + 1] = simpson;
			// innerCoreSRoute[i] = new double[] { x[i], delta };
			delta += simpson;
		}
		innerCoreSR[innerCoreSR.length - 1] = icbR - eps;
		innerCoreSTheta[innerCoreSR.length - 1] = 0;
		// tau *= 2;
		// System.out.println("inner core " + tau + " sec");
		return delta;
	}

	private double innerCoreSTau() {
		double tau = 0;
		double icbR = structure.innerCoreBoundary();
		double turningR = sv ? svTurningR : shTurningR;
		if (turningR > icbR || turningR < 0)
			return 0;
		double startR = turningR + jeffereysEPS;
		if (startR > icbR)
			return jeffreysSTau(turningR, icbR);
		// throw new RuntimeException("eps is too big " + jeffereysEPS);
		// tau = simpsonSTau(startR, structure.innerCoreBoundary() - eps); //
		double[] x = point(startR, icbR - eps, interval);
		for (int i = 0; i < x.length - 1; i++)
			tau += simpsonSTau(x[i], x[i + 1]);
		tau += jeffreysSTau(turningR, startR);
		// tau *= 2;
		// System.out.println("inner core " + tau + " sec");
		return tau;
	}

	/**
	 * computes delta by Jeffreys from rStart to rEnd at r= rStart qdeltap is
	 * singular
	 * 
	 * @param rStart
	 * @param rEnd
	 * @return
	 */
	private double jeffreysOuterCoreDelta(double turningR, double rEnd) {
		double[] x = new double[3];
		double dr = (rEnd - turningR) / 3;
		for (int i = 0; i < 3; i++) {
			double r = turningR + dr * (i + 1);
			x[i] = outerCoreQDelta(r);
		}
		// System.out.println("Geffreys");
		return jeffreysMethod3(x) * dr * 3;
	}

	/**
	 * computes traveltime by Jeffreys from rStart to rEnd at r= rStart qtp is
	 * singular
	 * 
	 * @param rStart
	 * @param rEnd
	 * @return
	 */
	private double jeffreysOuterCoreTau(double turningR, double rEnd) {
		double[] x = new double[3];
		double dr = (rEnd - turningR) / 3;
		for (int i = 0; i < 3; i++) {
			double r = turningR + dr * (i + 1);
			x[i] = outerCoreQT(r);
		}
		return jeffreysMethod3(x) * dr * 3;
	}

	/**
	 * computes delta by Jeffreys from rStart to rEnd at r= rStart qdeltap is
	 * singular
	 * 
	 * @param rStart
	 * @param rEnd
	 * @return
	 */
	private double jeffreysPDelta(double rStart, double rEnd) {
		double[] x = new double[3];
		double dr = (rEnd - rStart) / 3;
		for (int i = 0; i < 3; i++) {
			double r = rStart + dr * (i + 1);
			x[i] = calcQDeltaP(r);
		}
		// System.out.println("Geffreys");
		return jeffreysMethod3(x) * dr * 3;
	}

	/**
	 * computes traveltime by Jeffreys from rStart to rEnd at r= rStart qtp is
	 * singular
	 * 
	 * @param rStart
	 * @param rEnd
	 * @return
	 */
	private double jeffreysPTau(double rStart, double rEnd) {
		double[] x = new double[3];
		final double dr = (rEnd - rStart) / 3;
		for (int i = 0; i < 3; i++) {
			double r = rStart + dr * (i + 1);
			x[i] = calcQTP(r);
		}
		return jeffreysMethod3(x) * dr * 3;
	}

	/**
	 * computes delta by Jeffreys from rStart to rEnd at r= rStart qdeltaS is
	 * singular
	 * 
	 * @param rStart
	 * @param rEnd
	 * @return
	 */
	private double jeffreysSDelta(double rStart, double rEnd) {
		double[] x = new double[3];
		final double dr = (rEnd - rStart) / 3;
		for (int i = 0; i < 3; i++) {
			double r = rStart + dr * (i + 1);
			x[i] = calcQDeltaS(r);
		}
		return jeffreysMethod3(x) * dr * 3;
	}

	/**
	 * computes travel time by Jeffreys from rStart to rEnd at r= rStart qts is
	 * singular
	 * 
	 * @param rStart
	 * @param rEnd
	 * @return
	 */
	private double jeffreysSTau(double rStart, double rEnd) {
		double[] x = new double[3];
		final double dr = (rEnd - rStart) / 3;
		for (int i = 0; i < 3; i++) {
			double r = rStart + dr * (i + 1);
			x[i] = calcQTS(r);
		}
		return jeffreysMethod3(x) * dr * 3;
	}

	/**
	 * (Half) Delta of mantle part
	 * 
	 * @return
	 */
	private double mantlePDelta() {
		if (pTurningR < 0)
			return Double.NaN;
		if (eventR < pTurningR)
			return Double.NaN;

		double delta = 0;
		double cmb = structure.coreMantleBoundary();
		double startR = pTurningR < cmb ? cmb + eps : pTurningR + jeffereysEPS; //
		// delta = simpsonPDelta(startR, structure.earthRadius());
		if (structure.earthRadius() <= startR) {
			double jeff = jeffreysPDelta(pTurningR, structure.earthRadius());
			mantlePR = new double[2];
			mantlePTheta = new double[2];
			mantlePR[0] = pTurningR;
			mantlePR[1] = structure.earthRadius();
			mantlePTheta[0] = jeff;
			mantlePTheta[1] = 0;
			return jeff;
		}
		double[] x = point(startR, structure.earthRadius(), interval);
		if (cmb <= pTurningR) {
			double jeff = jeffreysPDelta(pTurningR, startR);
			delta += jeff;
			mantlePR = new double[x.length + 1];
			mantlePTheta = new double[x.length + 1];
			mantlePR[0] = pTurningR;
			mantlePTheta[0] = jeff;
		} else {
			mantlePR = new double[x.length];
			mantlePTheta = new double[x.length];
		}
		for (int i = 0; i < x.length - 1; i++) {
			double dDelta = simpsonPDelta(x[i], x[i + 1]);
			if (cmb <= pTurningR) {
				mantlePR[i + 1] = x[i];
				mantlePTheta[i + 1] = dDelta;
			} else {
				mantlePR[i] = x[i];
				mantlePTheta[i] = dDelta;
			}
			delta += dDelta;
		}
		mantlePR[mantlePR.length - 1] = structure.earthRadius();
		mantlePTheta[mantlePR.length - 1] = 0;

		// delta *= 2;
		return delta;
	}

	/**
	 * (Half) travel time in mantle
	 * 
	 * @return
	 */
	private double mantlePTau() {
		if (pTurningR < 0)
			return Double.NaN;
		if (eventR < pTurningR)
			return Double.NaN;

		double tau = 0;
		double cmb = structure.coreMantleBoundary();
		double startR = pTurningR < cmb ? cmb + eps : pTurningR + jeffereysEPS;//
		if (structure.earthRadius() <= startR)
			return jeffreysPTau(pTurningR, structure.earthRadius());
		// delta = simpsonPDelta(startR, icbR - eps); //
		double[] x = point(startR, structure.earthRadius(), interval);
		for (int i = 0; i < x.length - 1; i++)
			tau += simpsonPTau(x[i], x[i + 1]);
		// tau = simpsonPTau(startR, structure.earthRadius());
		if (cmb <= pTurningR)
			tau += jeffreysPTau(pTurningR, startR);

		// tau *= 2;
		return tau;
	}

	/**
	 * @return (Half) Delta of delta in mantle part
	 */
	private double mantleSDelta() {
		double delta = 0;
		double cmb = structure.coreMantleBoundary();
		double turningR = sv ? svTurningR : shTurningR;
		double startR = turningR < cmb ? cmb + eps : turningR + jeffereysEPS; //
		if (eventR < turningR)
			return Double.NaN;

		if (structure.earthRadius() <= startR) {
			double jeff = jeffreysSDelta(turningR, structure.earthRadius());
			mantleSR = new double[2];
			mantleSTheta = new double[2];
			mantleSR[0] = turningR;
			mantleSR[1] = structure.earthRadius();
			mantleSTheta[0] = jeff;
			mantleSTheta[1] = 0;
			return jeff;
		}
		if (turningR < 0)
			startR = cmb + permissibleGapForDiff;

		double[] x = point(startR, structure.earthRadius(), interval);
		if (cmb <= turningR) {
			double jeff = jeffreysSDelta(turningR, startR);
			delta += jeff;
			mantleSTheta = new double[x.length];
			mantleSR = new double[x.length];
			mantleSTheta[0] = jeff;
			mantleSR[0] = turningR;
		} else {
			mantleSTheta = new double[x.length];
			mantleSR = new double[x.length];
		}
		// System.out.println(delta);
		for (int i = 0; i < x.length - 1; i++) {
			double simpson = simpsonSDelta(x[i], x[i + 1]);
			if (cmb <= turningR) {
				mantleSTheta[i + 1] = simpson;
				mantleSR[i + 1] = x[i];
			} else {
				mantleSTheta[i] = simpson;
				mantleSR[i] = x[i];
			}
			delta += simpson;
		}
		mantleSR[mantleSTheta.length - 1] = structure.earthRadius();
		mantleSTheta[mantleSTheta.length - 1] = 0;
		// System.out.println("Mantle S Delta " + delta);
		return delta;

	}

	/**
	 * @return (Half)travel time in mantle
	 */
	private double mantleSTau() {
		double tau = 0;
		double cmb = structure.coreMantleBoundary();
		double startR = 0;
		double turningR = sv ? svTurningR : shTurningR;
		startR = turningR < cmb ? cmb + eps : turningR + jeffereysEPS;//
		// tau = simpsonSTau(startR, structure.earthRadius());
		if (eventR < turningR)
			return Double.NaN;
		if (turningR < 0)
			startR = cmb + permissibleGapForDiff;
		if (structure.earthRadius() <= startR)
			return jeffreysSTau(turningR, structure.earthRadius());
		// System.out.println("ho");
		double[] x = point(startR, structure.earthRadius(), interval);
		for (int i = 0; i < x.length - 1; i++)
			tau += simpsonSTau(x[i], x[i + 1]);
		if (cmb <= turningR)
			tau += jeffreysSTau(turningR, startR);
		// tau *= 2;
		return tau;
	}

	/**
	 * 
	 * 
	 * @return &Delta; in outer core
	 */
	private double outerCoreDelta() {
		if (pTurningR < 0)
			return 0;
		if (structure.coreMantleBoundary() <= pTurningR)
			return 0;
		double icb = structure.innerCoreBoundary();
		double startR = pTurningR < icb ? icb + eps : pTurningR + jeffereysEPS; //
		double cmbR = structure.coreMantleBoundary();
		if (cmbR <= startR) {
			double jeff = jeffreysOuterCoreDelta(pTurningR, cmbR);
			outerCoreR = new double[2];
			outerCoreTheta = new double[2];
			outerCoreR[0] = pTurningR;
			outerCoreR[1] = cmbR;
			outerCoreTheta[0] = jeff;
			outerCoreTheta[1] = 0;
			return jeff;
		}
		// System.out.println("startR " + startR);
		double delta = 0;
		double[] x = point(startR, cmbR, interval);
		if (icb <= pTurningR) {
			double jeff = jeffreysOuterCoreDelta(pTurningR, startR);
			delta += jeff;
			outerCoreR = new double[x.length + 1];
			outerCoreTheta = new double[x.length + 1];
			outerCoreR[0] = pTurningR;
			outerCoreTheta[0] = jeff;
		} else {
			outerCoreR = new double[x.length];
			outerCoreTheta = new double[x.length];
		}
		for (int i = 0; i < x.length - 1; i++) {
			double deltax = x[i + 1] - x[i];
			double a = outerCoreQDelta(x[i]);
			double b = outerCoreQDelta(x[i] + 0.5 * deltax);
			double c = outerCoreQDelta(x[i + 1]);
			double simpson = bySimpsonRule(a, b, c) * deltax;
			if (icb <= pTurningR) {
				outerCoreR[i + 1] = x[i];
				outerCoreTheta[i + 1] = simpson;
			} else {
				outerCoreR[i] = x[i];
				outerCoreTheta[i] = simpson;
			}
			delta += simpson;
		}
		outerCoreR[outerCoreR.length - 1] = cmbR;
		outerCoreTheta[outerCoreR.length - 1] = 0;
		// delta *= 2;
		// System.out.println("outer core delta is " + Math.toDegrees(delta));
		return delta;
	}

	private double outerCoreQDelta(double r) {
		double v = Math.sqrt(structure.getA(r) / structure.getRho(r));
		double sin = rayParameter * v / r;
		double cos = Math.sqrt(1 - sin * sin);
		double qdelta = sin / cos / r;
		return qdelta;
	}

	private double outerCoreQT(double r) {
		double v = Math.sqrt(structure.getA(r) / structure.getRho(r));
		double sin = rayParameter * v / r;
		double cos = Math.sqrt(1 - sin * sin);
		double tau = 1 / v / cos;
		return tau;
	}

	/**
	 * (Half)travel time in outer core
	 * 
	 * @return
	 */
	private double outerCoreTau() {
		if (pTurningR < 0)
			return 0;
		double cmbR = structure.coreMantleBoundary();
		if (cmbR <= pTurningR)
			return 0;
		double tau = 0;
		double icb = structure.innerCoreBoundary();
		// System.out.println(startR + " " + endR);
		double startR = pTurningR < icb ? icb + eps : pTurningR + jeffereysEPS; //
		// System.out.println(startR + " dd ");
		if (cmbR <= startR)
			return jeffreysOuterCoreTau(pTurningR, cmbR);

		double[] x = point(startR, cmbR, interval);
		for (int i = 0; i < x.length - 1; i++) {
			double deltax = x[i + 1] - x[i];
			double a = outerCoreQT(x[i]);
			double b = outerCoreQT(x[i] + 0.5 * deltax);
			double c = outerCoreQT(x[i + 1]);
			// System.out.println(x[i]+" "+a+" "+b+" "+c);
			tau = tau + bySimpsonRule(a, b, c) * deltax;
			// if (Double.isNaN(tau))
			// System.exit(0);
		}

		if (icb <= pTurningR)
			tau += jeffreysOuterCoreTau(pTurningR, startR);
		// tau *= 2;
		// System.out.println("Outer core: " + tau + " sec");
		return tau;
	}

	/**
	 * Epicentral distance(delta) for the diffracted phase which travels
	 * deltaOnCMB on the CMB If and only if P wave of this {@link Raypath} has
	 * turning depth sufficiently near the core mantle boundary. Permissible gap
	 * is {@link #permissibleGapForDiff}.
	 * 
	 * @param deltaOnCMB
	 *            [rad]
	 * @return delta [rad] or -1 if the diffracted phase is not computable due
	 *         to turning R
	 */
	private double pDiffDelta(double deltaOnCMB) {
		double gap = Math.abs(pTurningR - structure.coreMantleBoundary());
		if (permissibleGapForDiff < gap)
			return -1;
		double delta = 2 * mantlePDelta - upperPDelta + deltaOnCMB;
		return delta;
	}

	/**
	 * Travel time for the diffracted phase which travels deltaOnCMB on the CMB
	 * 
	 * @param deltaOnCMB
	 *            [rad]
	 * @return [s] travel time for the path of -1 if the diffracted phase is not
	 *         computable due to Turning R
	 */
	private double pDiffTraveltime(double deltaOnCMB) {
		double gap = Math.abs(pTurningR - structure.coreMantleBoundary());
		if (permissibleGapForDiff < gap)
			return -1;
		double time = 2 * mantlePTime - upperPTime + pTimeAlongCMB(deltaOnCMB);
		return time;
	}

	/**
	 * Interval of integration startR to endR is divided by deltaR
	 * 
	 * @param startR
	 *            [km]
	 * @param endR
	 *            [km]
	 * @param deltaR
	 *            [km]
	 * @return Array of radius
	 */
	private static double[] point(double startR, double endR, double deltaR) {
		double length = endR - startR;
		// System.out.println(start+" "+end+" "+delta);
		int n = (int) (length / deltaR);
		double[] x = new double[n + 1];
		for (int i = 0; i < n; i++)
			x[i] = startR + i * deltaR;
		x[n] = endR;
		return x;

	}

	/**
	 * Epicentral distance(delta) for diffracted phase which travels deltaOnCMB
	 * on the CMB
	 * 
	 * @param deltaOnCMB
	 *            [rad]
	 * @return delta [rad] or -1 if the diffracted phase is not computable due
	 *         to turning R
	 */
	private double sDiffDelta(double deltaOnCMB) {
		double gap = Math.abs(getSTurningR() - structure.coreMantleBoundary());
		// System.out.println(gap+" "+permissibleGapForDiff);
		if (permissibleGapForDiff < gap)
			return -1;
		double delta = 2 * mantleSDelta - upperSDelta + deltaOnCMB;
		// System.out.println(mantleSDelta + " " + upperSDelta);
		return delta;
	}

	/**
	 * Travel time for the diffracted phase which travels deltaOnCMB on the CMB
	 * 
	 * @param deltaOnCMB
	 *            [rad]
	 * @return [s] travel time for the path of -1 if the diffracted phase is not
	 *         computable due to Turning R
	 */
	private double sDiffTraveltime(double deltaOnCMB) {
		double gap = Math.abs(getSTurningR() - structure.coreMantleBoundary());
		if (permissibleGapForDiff < gap)
			return -1;
		double time = 2 * mantleSTime - upperSTime + sTimeAlongCMB(deltaOnCMB);
		return time;
	}

	/**
	 * Set integral interval grid.
	 * 
	 * @param interval
	 *            [km] integral interval
	 */
	public void setInterval(double interval) {
		this.interval = interval;
	}

	/**
	 * Set radius region of Jeffrey &amp; Jeffrey computation.
	 * 
	 * @param jeffreysEPS
	 *            [km] radius range
	 */
	public void setJeffreysEPS(double jeffreysEPS) {
		this.jeffereysEPS = jeffreysEPS;
	}

	/**
	 * computes turning radius
	 */
	private void setTurningRs() {

		pTurningR = structure.pTurningR(rayParameter);
		shTurningR = structure.shTurningR(rayParameter);
		svTurningR = structure.svTurningR(rayParameter);

		pTurning = whichPartition(pTurningR);
		shTurning = whichPartition(shTurningR);
		svTurning = whichPartition(svTurningR);
		// System.out.println("Turning Rs P:" + pTurning + " " + pTurningR
		// + ", SV:" + svTurning + " " + svTurningR + ", SH:" + shTurning
		// + " " + shTurningR);
	}

	/**
	 * integrate delta from startR to end R by Simpson Rule
	 * 
	 * @param startR
	 * @param endR
	 * @return
	 */
	private double simpsonPDelta(double startR, double endR) {
		double deltax = endR - startR;
		if (deltax <= 0)
			throw new RuntimeException("integral interval is invalid.");
		double a = calcQDeltaP(startR);
		double b = calcQDeltaP(startR + 0.5 * deltax);
		double c = calcQDeltaP(endR);
		// System.out.println(a+" "+b+" "+c);
		// System.out.println(x[i]*Math.sin(delta)+" "+x[i]*Math.cos(delta));
		double delta = bySimpsonRule(a, b, c) * deltax;
		// System.out.println(x[i+1]);
		// if(Double.isNaN(delta))
		// System.exit(0);
		return delta;
	}

	/**
	 * integrate traveltime from startR to end R by Simpson Rule
	 * 
	 * @param startR
	 * @param endR
	 * @return
	 */
	private double simpsonPTau(double startR, double endR) {
		double deltax = endR - startR;
		double a = calcQTP(startR);
		double b = calcQTP(startR + 0.5 * deltax);
		double c = calcQTP(endR);
		double tau = bySimpsonRule(a, b, c) * deltax;
		return tau;
	}

	/**
	 * integrate delta from startR to end R by Simpson Rule
	 * 
	 * @param startR
	 * @param endR
	 * @return
	 */
	private double simpsonSDelta(double startR, double endR) {
		double deltax = endR - startR;
		double a = calcQDeltaS(startR);
		double b = calcQDeltaS(startR + 0.5 * deltax);
		double c = calcQDeltaS(endR);
		double delta = bySimpsonRule(a, b, c) * deltax;
		return delta;
	}

	/**
	 * integral SH Q_T from the startR to the endR startR < endR
	 * 
	 * @param startR
	 * @param endR
	 * @return
	 */
	private double simpsonSTau(double startR, double endR) {
		double tau = 0;
		if (startR == endR)
			return 0;
		if (startR > endR)
			throw new RuntimeException("Invalid Rs");
		double deltax = endR - startR;
		double a = calcQTS(startR);
		double b = calcQTS(startR + 0.5 * deltax);
		double c = calcQTS(endR);
		tau += bySimpsonRule(a, b, c) * deltax;
		return tau;
	}

	/**
	 * Computation for diffracted waves.
	 * 
	 * @param deltaOnCMB
	 *            [rad]
	 * @return P wave traveltime along the CMB
	 */
	private double pTimeAlongCMB(double deltaOnCMB) {
		double time = 0;
		// radius which avoids 0 on cmb
		double r = structure.coreMantleBoundary() + eps;
		double s = r * deltaOnCMB;
		double velocity = Math.sqrt(structure.getA(r) / structure.getRho(r));
		time = s / velocity;
		// System.out.println("pTimeAlongCMB = "+time);
		return time;
	}

	/**
	 * Computation for diffracted waves. TODO AN
	 * 
	 * @param r
	 *            [km] radius
	 * @param delta
	 *            [rad]
	 * @return P wave traveltime along the CMB
	 */
	private double pDiffractionTime(double r, double delta) {
		double time = 0;
		// radius which avoids 0 on cmb
		double s = r * delta;
		double velocity = Math.sqrt(structure.getA(r) / structure.getRho(r));
		time = s / velocity;
		// System.out.println("pTimeAlongCMB = "+time);
		return time;
	}

	/**
	 * Computation for diffracted waves. TODO
	 * 
	 * @param r
	 *            [km] radius where raypath diffracts
	 * @param deltaOnBoundary
	 *            [rad]
	 * @return S wave traveltime along the CMB
	 */
	public double sDiffractionTime(double r, double deltaOnBoundary) {
		double startR = r;
		double turningR = sv ? svTurningR : shTurningR;
		if (eventR < r || r <= turningR + jeffereysEPS)
			throw new RuntimeException("Unexpected");
		if (r <= structure.coreMantleBoundary() + permissibleGapForDiff)
			throw new RuntimeException("Not yet...");
		// tau = simpsonSTau(startR, structure.earthRadius());
		// System.out.println("ho");
		double tau = 0;
		double[] x = point(startR, structure.earthRadius(), interval);
		for (int i = 0; i < x.length - 1; i++)
			tau += simpsonSTau(x[i], x[i + 1]);
		// tau *= 2;
		// radius which avoids 0 on cmb
		double s = r * deltaOnBoundary;
		double velocity = Math.sqrt((sv ? structure.getL(r) : structure.getN(r)) / structure.getRho(r));
		double timeOnDiffraction = s / velocity;
		return tau * 2 + timeOnDiffraction - upperSTime;
	}

	/**
	 * TODO
	 * 
	 * @param r
	 *            [km] radius for boundary
	 * @param deltaOnBoundary
	 *            [rad]
	 * @return delta [rad]
	 */
	public double sDiffractionDelta(double r, double deltaOnBoundary) {
		double startR = r;
		double turningR = sv ? svTurningR : shTurningR;
		if (eventR < r || r <= turningR + jeffereysEPS)
			throw new RuntimeException("Unexpected");
		if (r <= structure.coreMantleBoundary() + permissibleGapForDiff)
			throw new RuntimeException("Not yet...");
		double delta = 0;
		double[] x = point(startR, structure.earthRadius(), interval);
		for (int i = 0; i < x.length - 1; i++)
			delta += simpsonSDelta(x[i], x[i + 1]);
		return delta * 2 + deltaOnBoundary - upperSDelta;
	}

	/**
	 * Computation for diffracted waves.
	 * 
	 * @param deltaOnCMB
	 *            [rad]
	 * @return S wave traveltime along the CMB
	 */
	private double sTimeAlongCMB(double deltaOnCMB) {
		double time = 0;
		// radius which avoids 0 on cmb
		double r = structure.coreMantleBoundary() + eps;
		double s = r * deltaOnCMB;
		double velocity = Math.sqrt((sv ? structure.getL(r) : structure.getN(r)) / structure.getRho(r));
		time = s / velocity;
		return time;
	}

	/**
	 * switch system to compute SH
	 */
	public void switchSH() {
		sv = false;
	}

	/**
	 * switch system to compute SV
	 */
	public void switchSV() {
		sv = true;
	}

	/**
	 * @param r
	 *            radius [km]
	 * @return which part r belong to
	 */
	private Partition whichPartition(double r) {
		if (r < 0)
			return null;
		else if (r < structure.innerCoreBoundary())
			return Partition.INNERCORE;
		else if (r == structure.innerCoreBoundary())
			return Partition.INNER_CORE_BAUNDARY;
		else if (r < structure.coreMantleBoundary())
			return Partition.OUTERCORE;
		else if (r == structure.coreMantleBoundary())
			return Partition.CORE_MANTLE_BOUNDARY;
		else
			return Partition.MANTLE;
	}
}
