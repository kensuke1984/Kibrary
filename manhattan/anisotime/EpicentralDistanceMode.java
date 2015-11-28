/**
 * 
 */
package anisotime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Epicentral distance mode.
 * 
 * @author kensuke
 * @version 0.1.2
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
	 * @param phases
	 *            Array of {@link PhaseName}s
	 * @param epicentralDistance
	 *            target delta [rad]
	 * @param vs
	 *            structure
	 * @param eventR
	 *            radius (not depth) [km]
	 * @param sv
	 *            if true, p-SV mode
	 */
	EpicentralDistanceMode(ANISOtime travelTimeTool, Phase[] phases, double epicentralDistance, VelocityStructure vs,
			double eventR) {
		super(travelTimeTool);
		structure = vs;
		this.eventR = eventR;
		this.epicentralDistance = epicentralDistance;
		this.targetPhases = phases;
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
