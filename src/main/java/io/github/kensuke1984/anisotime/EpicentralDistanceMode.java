/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Epicentral distance mode.
 * 
 * @author Kensuke Konishi
 * @version 0.1.2.1
 * 
 * 
 */
class EpicentralDistanceMode extends Computation {

	private VelocityStructure structure;
	private double eventR;
	private Phase[] targetPhases;
	/**
	 * [rad]
	 */
	private double epicentralDistance;

	/**
	 * @param travelTimeTool
	 *            parent
	 * @param targetPhases
	 *            Array of PhaseNames
	 * @param epicentralDistance
	 *            target delta [rad]
	 * @param structure
	 *            structure
	 * @param eventR
	 *            radius (not depth) [km]
	 */
	EpicentralDistanceMode(ANISOtime travelTimeTool, Phase[] targetPhases, double epicentralDistance, VelocityStructure structure,
			double eventR) {
		super(travelTimeTool);
		this.structure = structure;
		this.eventR = eventR;
		this.epicentralDistance = epicentralDistance;
		this.targetPhases = targetPhases;
	}

	@Override
	public void run() {
		int polarization = travelTimeTool.getPolarization();
		boolean psv = false;
		boolean sh = false;
		switch (polarization) {
		case 0:
			psv = true;
			sh = true;
			break;
		case 1:
			psv = true;
			break;
		case 2:
			sh = true;
			break;
		default:
			throw new RuntimeException("Unexpected happens");
		}
		raypaths = new ArrayList<>();
		phases = new ArrayList<>();

		double deltaR = 10; // TODO
		for (Phase phase : targetPhases) {
			if (psv) {
				List<Raypath> possibleRaypathArray = RaypathSearch.lookFor(phase, structure, eventR, epicentralDistance,
						deltaR, true);
				if (possibleRaypathArray.isEmpty())
					continue;
				raypaths.addAll(possibleRaypathArray);
				IntStream.range(0, possibleRaypathArray.size()).forEach(i -> phases.add(phase));
			}
			if (sh && phase.pReaches() == null) {
				List<Raypath> possibleRaypathArray = RaypathSearch.lookFor(phase, structure, eventR, epicentralDistance,
						deltaR, false);
				if (possibleRaypathArray.isEmpty())
					continue;
				raypaths.addAll(possibleRaypathArray);
				IntStream.range(0, possibleRaypathArray.size()).forEach(i ->  phases.add(phase));
			}
		}
		for (int i = 0; i <  phases.size(); i++) {
			Phase phase = phases.get(i);
			if (!phase.isDiffracted())
				continue;
			Raypath raypath = raypaths.get(i);
			double delta = raypath.computeDelta(phase);
			double dDelta = Math.toDegrees(epicentralDistance - delta);
			phases.set(i, Phase.create(phase.toString() + dDelta));
		}
		int n = raypaths.size();
		// System.out.println("Whats done is done");
		double[] delta = new double[n];
		Arrays.fill(delta, epicentralDistance);
		showResult(delta, raypaths,  phases);

	}

	@Override
	ComputationMode getMode() {
		return ComputationMode.EPICENTRAL_DISTANCE;
	}

}
