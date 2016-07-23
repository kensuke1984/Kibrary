/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mode for diffracted waves such as Pdiff, Sdiff...
 * 
 * @author Kensuke Konishi
 * @version 0.2.2b
 */
class DiffractionMode extends Computation {

	private VelocityStructure structure;

	DiffractionMode(ANISOtimeGUI gui, VelocityStructure structure) {
		super(gui);
		this.structure = structure;
	}

	@Override
	public void run() {
		Set<Phase> targetPhases = gui.getSelectedPhaseSet();
		List<Raypath> raypathList = new ArrayList<>();
		List<Phase> phaseList = new ArrayList<>();
		double angleOnCMB = gui.getMostImportant();
		for (Phase phase : targetPhases) {
			if (!phase.isDiffracted())
				continue;
			Raypath raypath = phase.toString().contains("Pdiff")
					? structure.raypathByTurningR(PhasePart.P, true, structure.coreMantleBoundary())
					: (phase.isPSV() ? structure.raypathByTurningR(PhasePart.SV, true, structure.coreMantleBoundary())
							: structure.raypathByTurningR(PhasePart.SH, true, structure.coreMantleBoundary()));
			raypath.compute();
			Phase anglePhase = Phase.create(phase.toString() + angleOnCMB, phase.isPSV());
			raypathList.add(raypath);
			phaseList.add(anglePhase);
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
