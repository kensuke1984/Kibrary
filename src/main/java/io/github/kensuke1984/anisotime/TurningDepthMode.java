/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.math3.util.Precision;

/**
 * Turning depth mode
 * 
 * @author Kensuke Konishi
 * 
 * @version 0.2.2b
 * 
 */
class TurningDepthMode extends Computation {

	private VelocityStructure structure;

	TurningDepthMode(ANISOtimeGUI gui, VelocityStructure structure) {
		super(gui);
		this.structure = structure;
	}

	@Override
	public void run() {
		int polarization = gui.getPolarity();
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

		final double turningR = structure.earthRadius() - gui.getMostImportant();
		if (turningR < 0) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
					"Input turning depth " + Precision.round(gui.getMostImportant(), 2) + " is invalid."));
			return;
		}
		Set<Phase> samplePhase = gui.getSelectedPhaseSet();
		List<Raypath> raypathList = new ArrayList<>();
		List<Phase> phaseList = new ArrayList<>();
		for (Phase phase : samplePhase) {
			if (!(phase.equals(Phase.P) || phase.equals(Phase.S))) {
				SwingUtilities.invokeLater(
						() -> JOptionPane.showMessageDialog(null, "TurningDepthMode is only for P or S wave now."));
				return;
			}
			if (phase.equals(Phase.P)) {
				if (!psv)
					continue;
				Raypath raypath = structure.raypathByTurningR(PhasePart.P, true, turningR);
				if (raypath != null) {
					switch (structure.whichPartition(turningR)) {
					case MANTLE:
						break;
					case CORE_MANTLE_BOUNDARY:
						phase = Phase.PcP;
						break;
					case OUTERCORE:
						phase = Phase.PKP;
						break;
					case INNER_CORE_BOUNDARY:
						phase = Phase.PKiKP;
						break;
					case INNERCORE:
						phase = Phase.PKIKP;
						break;
					default:
						continue;
					}
					raypath.compute();
					raypathList.add(raypath);
					phaseList.add(phase);
				}
			} else {
				if (psv) {
					Raypath raypath = structure.raypathByTurningR(PhasePart.SV, true, turningR);
					if (raypath != null) {
						switch (structure.whichPartition(turningR)) {
						case MANTLE:
							phase = Phase.S;
							break;
						case CORE_MANTLE_BOUNDARY:
							phase = Phase.ScS;
							break;
						case INNERCORE:
							phase = Phase.create("SKJKS");
							break;
						default:
							continue;
						}
						raypath.compute();
						raypathList.add(raypath);
						phaseList.add(phase);
					}
				}
				if (sh) {
					Raypath raypath = structure.raypathByTurningR(PhasePart.SH, true, turningR);
					if (raypath != null) {
						switch (structure.whichPartition(turningR)) {
						case MANTLE:
							phase = Phase.S;
							break;
						case CORE_MANTLE_BOUNDARY:
							phase = Phase.ScS;
							break;
						case INNERCORE:
						default:
							continue;
						}
						raypath.compute();
						raypathList.add(raypath);
						phaseList.add(phase);
					}
				}
			}
		}
		raypaths = raypathList;
		phases = phaseList;
		showResult(raypaths, phases);
	}

	@Override
	ComputationMode getMode() {
		return ComputationMode.TURNING_DEPTH;
	}

}
