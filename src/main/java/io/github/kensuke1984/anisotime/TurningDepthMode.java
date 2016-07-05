/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.math3.util.Precision;

/**
 * Turning depth mode
 * 
 * @author Kensuke Konishi
 * 
 * @version 0.1.1.3
 * 
 */
class TurningDepthMode extends Computation {

	private double eventR;
	private VelocityStructure structure;
	private static final double PERMISSIBLE_GAP = 0.0001;

	TurningDepthMode(ANISOtime travelTimeTool, VelocityStructure structure, double eventR) {
		super(travelTimeTool);
		this.eventR = eventR;
		this.structure = structure;
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

		final double turningR = structure.earthRadius() - travelTimeTool.getMostImportant();
		if (turningR < 0) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
					"Input turning depth " + Precision.round(travelTimeTool.getMostImportant(), 2) + " is invalid."));
			return;
		}
		Phase[] samplePhase = travelTimeTool.getSelectedPhases();
		List<Raypath> raypathList = new ArrayList<>();
		List<Phase> phaseList = new ArrayList<>();
		for (Phase phase : samplePhase) {
			if (!(phase.equals(Phase.create("P")) || phase.equals(Phase.create("S")))) {
				SwingUtilities.invokeLater(
						() -> JOptionPane.showMessageDialog(null, "TurningDepthMode is only for P or S wave now."));
				return;
			}
			if (phase.equals(Phase.create("P"))) {
				if (!psv)
					continue;
				Raypath raypath = RaypathSearch.raypathByPTurningR(structure, turningR, PERMISSIBLE_GAP, eventR);
				if (raypath != null) {
					switch (structure.whichPartition(turningR)) {
					case MANTLE:
						break;
					case CORE_MANTLE_BOUNDARY:
						phase = Phase.create("PcP");
						break;
					case OUTERCORE:
						phase = Phase.create("PKP");
						break;
					case INNER_CORE_BOUNDARY:
						phase = Phase.create("PKiKP");
						break;
					case INNERCORE:
						phase = Phase.create("PKIKP");
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
					Raypath raypath = RaypathSearch.raypathBySTurningR(structure, turningR, PERMISSIBLE_GAP, eventR,
							true);
					if (raypath != null) {
						switch (structure.whichPartition(turningR)) {
						case MANTLE:
							phase = Phase.create("S");
							break;
						case CORE_MANTLE_BOUNDARY:
							phase = Phase.create("ScS");
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
					Raypath raypath = RaypathSearch.raypathBySTurningR(structure, turningR, PERMISSIBLE_GAP, eventR,
							false);
					if (raypath != null) {
						switch (structure.whichPartition(turningR)) {
						case MANTLE:
							phase = Phase.create("S");
							break;
						case CORE_MANTLE_BOUNDARY:
							phase = Phase.create("ScS");
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
