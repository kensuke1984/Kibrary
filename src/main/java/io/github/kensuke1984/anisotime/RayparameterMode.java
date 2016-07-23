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
 * @version 0.2.2b
 * 
 */
final class RayparameterMode extends Computation {

	final Raypath raypath;

	RayparameterMode(ANISOtimeGUI parentTravelTimeTool, Raypath raypath) {
		super(parentTravelTimeTool);
		this.raypath = raypath;
	}

	@Override
	ComputationMode getMode() {
		return ComputationMode.RAYPARAMETER;
	}

	/** 
	 * Computes for parameters when compute button is pushed
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		raypath.compute();
		List<Raypath> raypathList = new ArrayList<>();
		List<Phase> phaseList = new ArrayList<>(gui.getSelectedPhaseSet());
		
		for(int i=0;i<phaseList.size();i++)
			raypathList.add(raypath);
		
		raypaths = raypathList;
		phases = phaseList;
		gui.computed(raypath);
		showResult(raypaths, phases);
	}
}
