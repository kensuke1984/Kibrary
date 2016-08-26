/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.util.ArrayList;

/**
 * Rayparameter mode
 * 
 * @author Kensuke Konishi
 * @version 0.2.2.1b
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
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		raypath.compute();
		raypaths = new ArrayList<>();
		phases = new ArrayList<>(gui.getSelectedPhaseSet());

		for (int i = 0; i < phases.size(); i++)
			raypaths.add(raypath);

		gui.computed(raypath);
		showResult(raypaths, phases);
	}
}
