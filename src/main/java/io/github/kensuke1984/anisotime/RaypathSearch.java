package io.github.kensuke1984.anisotime;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import javax.swing.SwingUtilities;

import io.github.kensuke1984.kibrary.util.Trace;

/**
 * TODO Searching utilities for raypath.
 * 
 * @author Kensuke Konishi
 * 
 * @version 0.1.1b
 * 
 */
public final class RaypathSearch {

	private RaypathSearch() {
	}

	private static final boolean isDisplayable = !GraphicsEnvironment.isHeadless();

	/**
	 * Create a {@link Raypath} where P wave turns at eventR.
	 * 
	 * @param structure
	 *            {@link VelocityStructure}
	 * @param turningR
	 *            radius[km] you want to set.
	 * @param permissibleRGap
	 *            [km] gap between turningR of obtained Raypath and input R
	 * @param eventR
	 *            [km] source radius
	 * @return null if the gap exceeds permissibleRGap
	 */
	public static Raypath raypathByPTurningR(VelocityStructure structure, double turningR, double permissibleRGap,
			double eventR) {
		double p = toPRayParameter(turningR, structure);
		Raypath raypath = new Raypath(p, structure);
		double residual = turningR - raypath.getTurningR(PhasePart.P);
		return Math.abs(residual) < permissibleRGap ? raypath : null;
	}

	/**
	 * Create a {@link Raypath} where V wave turns at eventR
	 * 
	 * @param structure
	 *            {@link VelocityStructure}
	 * @param turningR
	 *            radius[km] you want to set.
	 * @param permissibleRGap
	 *            [km] gap between turningR of obtained Raypath and input R
	 * @param eventR
	 *            [km] source radius
	 * @param sv
	 *            true: SV, false:SH
	 * @return null if the gap exceeds permissibleRGap
	 */
	public static Raypath raypathBySTurningR(VelocityStructure structure, double turningR, double permissibleRGap,
			double eventR, boolean sv) {
		Raypath raypath;
		double p = toSRayParameter(turningR, structure, sv);
		try {
			raypath = new Raypath(p, structure);
		} catch (Exception e) {
			return null;
		}
		double residual = turningR - raypath.getTurningR(sv ? PhasePart.SV : PhasePart.SH);
		return Math.abs(residual) < permissibleRGap ? raypath : null;
	}

	/**
	 * Create {@link Raypath} for Pdiff phase. The raypath is supposed to have a
	 * turning depth at the core mantle boundary.
	 * 
	 * @param structure
	 *            {@link VelocityStructure}
	 * @param eventR
	 *            [km] source radius
	 * @return raypath which has pturning R on the CMB
	 */
	public static Raypath pDiffRaypath(VelocityStructure structure, double eventR) {
		return raypathByPTurningR(structure, structure.coreMantleBoundary() + Raypath.permissibleGapForDiff / 100,
				Raypath.permissibleGapForDiff, eventR);
	}

	/**
	 * Create {@link Raypath} for SV or SHdiff phase. The raypath is supposed to
	 * have a turning depth at the core mantle boundary.
	 * 
	 * @param structure
	 *            {@link VelocityStructure}
	 * @param eventR
	 *            [km] source radius
	 * @param sv
	 *            true:SV, false:SH
	 * @return raypath which has svturning R on the CMB
	 */
	public static Raypath sDiffRaypath(VelocityStructure structure, double eventR, boolean sv) {
		return raypathBySTurningR(structure, structure.coreMantleBoundary() + Raypath.permissibleGapForDiff,
				100 * Raypath.permissibleGapForDiff, eventR, sv);
	}

	/**
	 * Eq. (7) qt Woodhouse 1981
	 * 
	 * @param turningR
	 * @param vs
	 * @return p where qt(turningR)=0
	 */
	private static double toPRayParameter(double turningR, VelocityStructure vs) {
		return Math.sqrt(vs.getRho(turningR) / vs.getA(turningR)) * turningR;
	}

	/**
	 * Eq. (7) qt Woodhouse 1981
	 * 
	 * @param turningR
	 *            radius of turning point
	 * @param structure
	 *            {@link VelocityStructure}
	 * @param sv
	 *            true:SV, false:SH
	 * @return p where qt(turningR)=0
	 */
	private static double toSRayParameter(double turningR, VelocityStructure structure, boolean sv) {
		double v = Math.sqrt((sv ? structure.getL(turningR) : structure.getN(turningR)) / structure.getRho(turningR));
		return turningR / v;
	}

	/**
	 * 
	 * TODO By input parameters, make a list of possible P
	 * 
	 * @param targetPhase
	 *            the phase for search
	 * @param structure
	 *            of the earth
	 * @param sv
	 *            SV or not
	 * @param eventR
	 *            [km]
	 * @param targetDelta
	 *            [rad]
	 * @param deltaR
	 *            [km] look for the ray parameter by changing turning depth by
	 *            deltaR
	 * @return list of possible {@link Raypath}
	 */
	private static List<Raypath> makeRaypathCandidates(Phase targetPhase, VelocityStructure structure, boolean sv,
			double eventR, double targetDelta, double deltaR) {
		Partition pTurning = targetPhase.pReaches();
		Partition sTurning = targetPhase.sReaches();
		// System.out.println(pTurning+" "+sTurning);
		double rStart = 0;
		double rEnd = 0;
		List<Raypath> pathList = new ArrayList<>();
		if (sTurning != null) {
			switch (sTurning) {
			case MANTLE:
				rStart = structure.coreMantleBoundary();
				rEnd = eventR - deltaR * 0.01;
				break;
			case CORE_MANTLE_BOUNDARY:
			case INNERCORE:
				rStart = 0;
				rEnd = structure.innerCoreBoundary();
				break;
			default:
				throw new RuntimeException("UNEXPECTED");
			}
			pathList.addAll(DoubleStream.iterate(rStart, r -> r + deltaR).limit((int) ((rEnd - rStart) / deltaR) + 2)
					.filter(r -> 0 < r && r <= structure.earthRadius()).parallel()
					.mapToObj(r -> RaypathSearch.raypathBySTurningR(structure, r, 0.01, eventR, sv))
					.filter(Objects::nonNull).collect(Collectors.toList()));

			// assume p for S with turning point is within cmb and icb.
			if (sTurning == Partition.CORE_MANTLE_BOUNDARY) {
				double pdelta = Math.abs(pathList.get(pathList.size() - 1).getRayParameter()
						- pathList.get(pathList.size() - 2).getRayParameter());
				double pStart = pathList.get(pathList.size() - 1).getRayParameter();
				double pEnd = toSRayParameter(structure.coreMantleBoundary() + 0.0001, structure, sv);
				while (0.001 < pdelta) {
					for (double p = pStart + pdelta; p < pEnd; p += pdelta)
						pathList.add(new Raypath(p, structure));
					pStart = pEnd - pdelta;
					pdelta *= 0.1;
				}

			}
		} else if (pTurning != null) {
			switch (pTurning) {
			case MANTLE:
				rStart = structure.coreMantleBoundary();
				rEnd = eventR - 0.01 * deltaR;
				break;
			case OUTERCORE:
				rStart = structure.innerCoreBoundary();
				rEnd = structure.coreMantleBoundary();
				break;
			case CORE_MANTLE_BOUNDARY:
				rStart = 0;
				rEnd = structure.coreMantleBoundary();
				break;
			case INNER_CORE_BOUNDARY:
			case INNERCORE:
				rStart = 0;
				rEnd = structure.innerCoreBoundary();
				break;
			default:
				throw new RuntimeException("UNEXPECTED");
			}
			for (double r = rStart; r <= rEnd; r += deltaR) {
				Raypath raypath = RaypathSearch.raypathByPTurningR(structure, r, 0.01, eventR);
				if (raypath != null)
					pathList.add(raypath);
			}
		} else if (targetPhase.toString().equals("p") || targetPhase.toString().equals("s")) {
			rStart = 0;
			rEnd = eventR - 0.01 * deltaR;
			if (targetPhase.toString().equals("p"))
				for (double r = rStart; r <= rEnd; r += deltaR) {
					Raypath raypath = RaypathSearch.raypathByPTurningR(structure, r, 0.01, eventR);
					if (raypath != null)
						pathList.add(raypath);
				}
			else
				for (double r = rStart; r <= rEnd; r += deltaR) {
					if (r < structure.coreMantleBoundary() && structure.innerCoreBoundary() < r)
						continue;
					Raypath raypath = RaypathSearch.raypathBySTurningR(structure, r, 0.01, eventR, sv);
					if (raypath != null)
						pathList.add(raypath);
				}
		} else
			throw new RuntimeException("Unexpected phase.");

		return pathList;
	}

	/**
	 * @param raypathList
	 *            {@link List} of {@link Raypath} to compute
	 * @return true if the computation is not canceled.
	 */
	private static boolean computeRaypath(final List<Raypath> raypathList, Phase phase) {
		final ProgressWindow window = isDisplayable && 50 < raypathList.size()
				? new ProgressWindow(0, raypathList.size()) : null;
		String title = phase.isPSV() ? phase.toString() + " (P-SV)" : phase.toString() + " (SH)";
		if (window != null)
			SwingUtilities.invokeLater(() -> {
				window.setVisible(true);
				window.setTitle(title);
			});
		raypathList.parallelStream().forEach(raypath -> {
			if (window != null && window.isCanceled())
				return;
			raypath.compute();
			if (window != null)
				window.addProgress();
		});
		if (window != null)
			SwingUtilities.invokeLater(window::dispose);
		if (window == null)
			return true;
		return !window.isCanceled();

	}

	/**
	 * For the structure, search a rayparameter for the input epicentral
	 * distance by deltaR
	 * 
	 * @param targetPhase
	 *            {@link Phase} to compute for
	 * @param structure
	 *            {@link VelocityStructure}
	 * @param eventR
	 *            [km]
	 * @param targetDelta
	 *            [rad]
	 * @param deltaR
	 *            [km] look for the ray parameter by changing turning depth by
	 *            deltaR
	 * @return array of rayparameters which have epicentral distance near the
	 *         input distance, empty list will return if no phases
	 */
	public static List<Raypath> lookFor(Phase targetPhase, VelocityStructure structure, double eventR,
			double targetDelta, double deltaR) {
		if (targetPhase.isDiffracted()) {
			Raypath diffRay = targetPhase.toString().contains("Pdiff") ? pDiffRaypath(structure, eventR)
					: sDiffRaypath(structure, eventR, targetPhase.isPSV());
			diffRay.compute();
			if (targetDelta < diffRay.computeDelta(eventR, targetPhase))
				return Collections.emptyList();
			diffRay.compute();
			return new ArrayList<>(Arrays.asList(diffRay));
		}
		// long startTime = System.nanoTime();
		final List<Raypath> searchPathlist = makeRaypathCandidates(targetPhase, structure, targetPhase.isPSV(), eventR,
				targetDelta, deltaR);

		// compute all raypath
		if (!computeRaypath(searchPathlist, targetPhase))
			return Collections.emptyList();

		// compute deltas and save
		double[] delta = searchPathlist.stream().mapToDouble(searchPath -> searchPath.computeDelta(eventR, targetPhase))
				.toArray();

		// pick up raypaths with delta near the target
		List<Raypath> possibleRaypathList = new ArrayList<>();
		for (int i = 0; i < searchPathlist.size() - 1; i++) {
			double relative0 = delta[i] - targetDelta;
			if (relative0 == 0) {
				possibleRaypathList.add(searchPathlist.get(i));
				continue;
			}
			double relative1 = delta[i + 1] - targetDelta;
			if (0 <= relative0 * relative1)
				continue;
			if (Double.isNaN(relative0) && Double.isNaN(relative1))
				continue;

			if (relative0 * relative1 < 0)
				possibleRaypathList.add(
						pickup(searchPathlist.get(i), searchPathlist.get(i + 1), eventR, targetPhase, targetDelta));
			else {
				Raypath raypathNaN;
				Raypath raypathNonNaN;
				if (Double.isNaN(delta[i])) {
					raypathNaN = searchPathlist.get(i); // NAN
					raypathNonNaN = searchPathlist.get(i + 1);
				} else {
					raypathNaN = searchPathlist.get(i + 1); // NAN
					raypathNonNaN = searchPathlist.get(i);
				}
				for (;;) {
					double relativeNonNan = Math.abs(raypathNonNaN.computeDelta(eventR, targetPhase) - targetDelta);
					Raypath candidateRaypath = search(raypathNaN, raypathNonNaN, eventR, targetPhase, targetDelta);
					if (candidateRaypath != null) {
						if (Math.abs(candidateRaypath.computeDelta(eventR, targetPhase) - targetDelta) < 0.01) {
							possibleRaypathList.add(candidateRaypath);
							break;
						}
						double relative = Math.abs(candidateRaypath.computeDelta(eventR, targetPhase) - targetDelta);
						if (relativeNonNan < relative)
							break;
						raypathNonNaN = candidateRaypath;
					} else
						break;
				}
			}
		}
		// System.out.println(possibleRaypathList.size() + " are found");
		if (possibleRaypathList.size() == 0)
			return Collections.emptyList();
		possibleRaypathList.forEach(Raypath::compute);
		return possibleRaypathList;
	}

	/**
	 * If a return value is raypath0 or raypath1, it almost means the raypath
	 * computation is related to triplication range's.
	 * 
	 * Should consider more TODO
	 * 
	 * 
	 * @param raypath0
	 *            in which {@link Raypath#compute()} is done
	 * @param raypath1
	 *            in which {@link Raypath#compute()} is done
	 * @param eventR
	 *            radius of the source [km]
	 * @param phase
	 *            target phase
	 * @param targetDelta
	 *            [rad] target delta
	 * @return preferable {@link Raypath} between raypath0 and raypath1 or null
	 *         if one does not exist
	 */
	private static Raypath pickup(Raypath raypath0, Raypath raypath1, double eventR, Phase phase, double targetDelta) {
		double delta0 = raypath0.computeDelta(eventR, phase);
		double delta1 = raypath1.computeDelta(eventR, phase);
		double relative0 = delta0 - targetDelta;
		if (relative0 == 0)
			return raypath0;
		double relative1 = delta1 - targetDelta;
		if (0 <= relative0 * relative1 || Double.isNaN(relative0) || Double.isNaN(relative1))
			return null;

		relative0 = Math.abs(relative0);
		relative1 = Math.abs(relative1);
		double p = (relative1 * raypath0.getRayParameter() + relative0 * raypath1.getRayParameter())
				/ (relative0 + relative1);
		Raypath raypath = new Raypath(p, raypath0.getVelocityStructure());
		raypath.compute();
		double centerDelta = raypath.computeDelta(eventR, phase);
		double relative = Math.abs(centerDelta - targetDelta);
		if (relative <= relative0 && relative <= relative1)
			return raypath;
		return relative0 < relative1 ? raypath0 : raypath1;
	}

	private static List<Raypath> makeRaypathList(Raypath raypathNAN, Raypath raypath1, double eventR, Phase targetPhase,
			double targetDelta) {
		double rayParameterNAN = raypathNAN.getRayParameter();
		double rayParameter1 = raypath1.getRayParameter();
		final double pStart = rayParameterNAN < rayParameter1 ? rayParameterNAN : rayParameter1;
		final double pEnd = rayParameterNAN < rayParameter1 ? rayParameter1 : rayParameterNAN;
		final VelocityStructure structure = raypathNAN.getVelocityStructure();
		int number = 10;
		double pDelta = (pEnd - pStart) / number;
		// avoid pend
		return DoubleStream.iterate(pStart, p -> p += pDelta).limit(number).mapToObj(p -> new Raypath(p, structure))
				.collect(Collectors.toList());
	}

	/**
	 * @param raypathNAN
	 *            Raypath which delta is NAN
	 * @param raypath1
	 *            Raypath !NAN
	 * @param eventR
	 *            radius of the source [km]
	 * @param phase
	 *            to look for
	 * @param targetDelta
	 *            epicentral distance
	 * @return {@link Raypath}
	 */
	private static Raypath search(Raypath raypathNAN, Raypath raypath1, double eventR, Phase phase,
			double targetDelta) {
		List<Raypath> raypathList = makeRaypathList(raypathNAN, raypath1, eventR, phase, targetDelta);
		computeRaypath(raypathList, phase);
		for (int i = 0; i < raypathList.size() - 1; i++) {
			Raypath raypath = pickup(raypathList.get(i), raypathList.get(i + 1), eventR, phase, targetDelta);
			if (raypath != null)
				return raypath;
		}
		if (Double.isNaN(raypathList.get(0).computeDelta(eventR, phase)))
			for (int i = 1; i < raypathList.size() - 2; i++) {
				if (!Double.isNaN(raypathList.get(i).computeDelta(eventR, phase)))
					return raypathList.get(i);
			}
		else
			for (int i = raypathList.size() - 2; 1 < i; i--)
				if (!Double.isNaN(raypathList.get(i).computeDelta(eventR, phase)))
					return raypathList.get(i);

		return null;
	}

	/**
	 * @param delta
	 *            [deg]
	 * @param raypath0
	 *            source of raypath
	 * @param eventR
	 *            radius of the source [km]
	 * @param phase
	 *            to look for
	 * @param intervalOfP
	 *            density of search
	 * @return travel time for delta [s]
	 */
	static double travelTimeByThreePointInterpolate(double delta, Raypath raypath0, double eventR, Phase phase,
			double intervalOfP) {
		delta = Math.toRadians(delta);
		double delta0 = raypath0.computeDelta(eventR, phase);
		double p0 = raypath0.getRayParameter();
		double p1 = p0 - intervalOfP;
		double p2 = p0 + intervalOfP;
		Raypath raypath1 = new Raypath(p1, raypath0.getStructure());
		Raypath raypath2 = new Raypath(p2, raypath0.getStructure());
		raypath1.compute();
		raypath2.compute();
		double delta1 = raypath1.computeDelta(eventR, phase);
		double delta2 = raypath2.computeDelta(eventR, phase);
		// delta1<delta0<delta2 or delta2<delta0<delta1
		if ((delta1 - delta0) * (delta2 - delta0) > 0)
			return -1;
		if (Math.max(delta1, delta2) < delta || delta < Math.min(delta1, delta2))
			return -1;
		double[] p = new double[] { delta0, delta1, delta2 };
		double[] time = new double[] { raypath0.computeT(eventR, phase), raypath1.computeT(eventR, phase),
				raypath2.computeT(eventR, phase), };
		Trace t = new Trace(p, time);
		return t.toValue(2, delta);
	}

	/**
	 * Assume that there is a regression curve f(&Delta;) = T for the small
	 * range. The function f is assumed to be a polynomial function. The degree
	 * of the function depends on the number of the input raypaths.
	 * 
	 * @param targetDelta
	 *            [rad] epicentral distance to get T for
	 * @param raypaths
	 *            Polynomial interpolation is done with these.
	 * @return travel time [s] for the target Delta estimated by the polynomial
	 *         interpolation with the raypaths.
	 * 
	 */
	double travelTimeInterpolation(double targetDelta, Raypath... raypaths) {

		return 0;
	}

}
