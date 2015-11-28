/**
 * 
 */
package anisotime;

import java.util.ArrayList;
import java.util.List;

/**
 * Mode for diffracted waves such as Pdiff, Sdiff...
 * 
 * @author kensuke
 * @version 0.1.5
 */
class DiffractionMode extends Computation {

	private VelocityStructure structure;
	private double eventR;

	/**
	 * @param travelTimeTool
	 */
	DiffractionMode(ANISOtime travelTimeTool, VelocityStructure vs, double eventR) {
		super(travelTimeTool);
		this.eventR = eventR;
		this.structure = vs;
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
		Phase[] targetPhases = travelTimeTool.getSelectedPhases();
		List<Raypath> raypathList = new ArrayList<>();
		List<Phase> phaseList = new ArrayList<>();
		Raypath pRaypath = RaypathSearch.pDiffRaypath(structure, eventR);
		Raypath svRaypath = RaypathSearch.sDiffRaypath(structure, eventR, true);
		Raypath shRaypath = RaypathSearch.sDiffRaypath(structure, eventR, false);
		pRaypath.compute();
		svRaypath.compute();
		shRaypath.compute();
		double angleOnCMB = travelTimeTool.getMostImportant();
		for (Phase phase : targetPhases) {
			if (!phase.isDiffracted())
				continue;
			Phase anglePhase = Phase.create(phase.toString() + angleOnCMB);
			if (psv)
				if (phase.pReaches() != null) {
					raypathList.add(pRaypath);
					phaseList.add(anglePhase);
				} else {
					raypathList.add(svRaypath);
					phaseList.add(anglePhase);
				}
			if (sh) {
				if (phase.pReaches() != null)
					continue;
				raypathList.add(shRaypath);
				phaseList.add(anglePhase);
			}

		}
		raypaths = raypathList;
		phases = phaseList;
		showResult(raypaths, phases);
	}

	@Override
	ComputationMode getMode() {
		return ComputationMode.DIFFRACTION;
	}

}
