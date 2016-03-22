/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.util.ArrayList;
import java.util.List;

/**
 * Rayparameter mode
 * 
 * @author Kensuke Konishi
 * @version 0.1.2.1
 * 
 */
final class RayparameterMode extends Computation {

	final Raypath raypath;

	RayparameterMode(ANISOtime parentTravelTimeTool, Raypath raypath) {
		super(parentTravelTimeTool);
		this.raypath = raypath;
	}

	@Override
	ComputationMode getMode() {
		return ComputationMode.RAYPARAMETER;
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
		Raypath psvPath = new Raypath(raypath.getRayParameter(), raypath.getEventR(), raypath.getVelocityStructure(),
				true);
		psvPath.compute();
		Raypath shPath = new Raypath(raypath.getRayParameter(), raypath.getEventR(), raypath.getVelocityStructure(),
				false);
		shPath.compute();
		Phase[] selectedPhases = travelTimeTool.getSelectedPhases();
		List<Raypath> raypathList = new ArrayList<>();
		List<Phase> phaseList = new ArrayList<>();
		for (Phase phase : selectedPhases) {
			if (psv && psvPath.exists(phase)) {
				raypathList.add(psvPath);
				phaseList.add(phase);
			}
			if (sh && phase.pReaches() == null && shPath.exists(phase)) {
				raypathList.add(shPath);
				phaseList.add(phase);
			}

		}
		raypaths = raypathList;
		phases = phaseList;
		travelTimeTool.computed(raypath);
		showResult(raypaths, phases);
	}
}
