/**
 * 
 */
package anisotime;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kensuke
 * @since 2014/08/03
 * @version 0.0.1
 * 
 * @version 0.1.0
 * @since 2015/4/29 Robert J. Geller request
 * 
 * @version 0.1.1
 * @since 2015/5/4 Polarization
 * 
 * @version 0.1.2
 * @since 2015/5/6 multiple phases ok
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
		// travelTimeTool.jPanelTurningInformation.setTurningInformation(
		// raypath.getPTurningR(), raypath.getSTurningR(),
		// raypath.getPTurning(), raypath.getSTurning());
		travelTimeTool.computed(raypath);

		showResult(raypaths, phases);
	}
}
