/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * @author Kensuke Konishi
 * @version 0.1.1
 * 
 */
abstract class Computation implements Runnable {

	List<Raypath> raypaths;
	List<Phase> phases;

	abstract ComputationMode getMode();

	final ANISOtimeGUI gui;

	Computation(ANISOtimeGUI travelTimeTool) {
		this.gui = travelTimeTool;
	}

	private static void showRayPath(final ANISOtimeGUI travelTimeGUI, final Raypath raypath, final Phase phase) {
		if (!raypath.exists(travelTimeGUI.getEventR(), phase))
			return;
		double[][] points = raypath.getRouteXY(travelTimeGUI.getEventR(), phase);
		if (points != null) {
			double[] x = new double[points.length];
			double[] y = new double[points.length];
			for (int i = 0; i < points.length; i++) {
				x[i] = points[i][0];
				y[i] = points[i][1];
			}
			try {
				SwingUtilities.invokeAndWait(() -> travelTimeGUI.addPath(x, y));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return;
	}

	void outputPath() {
		double eventR = gui.getEventR();
		Runnable output = new Runnable() {
			Path outputDirectory;

			@Override
			public void run() {
				try {
					SwingUtilities.invokeAndWait(() -> {
						JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
						fileChooser.setDialogTitle("Output the path?");
						fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						int action = fileChooser.showOpenDialog(null);
						if (action == JFileChooser.CANCEL_OPTION || action == JFileChooser.ERROR_OPTION)
							return;
						outputDirectory = fileChooser.getSelectedFile().toPath();
					});
					if (outputDirectory == null)
						return;
					if (raypaths.size() != phases.size())
						throw new RuntimeException("UNEXPECTED");
					for (int i = 0; i < raypaths.size(); i++) {
						String name = phases.get(i).isPSV() ? phases.get(i) + "_PSV" : phases.get(i) + "_SH";
						Path outEPSFile = outputDirectory.resolve(name + ".eps");
						Path outInfoFile = outputDirectory.resolve(name + ".inf");
						Path outDataFile = outputDirectory.resolve(name + ".dat");
						raypaths.get(i).outputEPS(eventR, outEPSFile, phases.get(i));
						raypaths.get(i).outputInfo(outInfoFile, eventR, phases.get(i));
						raypaths.get(i).outputDat(outDataFile, eventR, phases.get(i));
					}
				} catch (Exception e) {
					e.printStackTrace();
					SwingUtilities.invokeLater(
							() -> JOptionPane.showMessageDialog(null, "Cannot output files about the path"));
				}
			}
		};
		new Thread(output).start();
	}

	/**
	 * This method shows results containing ith phase of ith raypath
	 * 
	 * @param raypath
	 *            List of {@link Raypath}
	 * @param phase
	 *            List of {@link Phase}
	 */
	synchronized public void showResult(final List<Raypath> raypath, final List<Phase> phase) {
		showResult(null, raypath, phase);
	}

	/**
	 * This method shows results containing ith phase of ith raypath
	 * 
	 * @param delta
	 *            Array of epicentral distance
	 * @param raypathList
	 *            List of {@link Raypath}
	 * @param phaseList
	 *            List of {@link Phase}
	 */
	synchronized public void showResult(final double[] delta, final List<Raypath> raypathList,
			final List<Phase> phaseList) {
		Objects.requireNonNull(raypathList);
		Objects.requireNonNull(phaseList);
		if (raypathList.size() != phaseList.size())
			throw new RuntimeException("UNEXPECTED");
		// System.out.println(SwingUtilities.isEventDispatchThread());
		double eventR =gui.getEventR();
		for (int i = 0; i < phaseList.size(); i++) {
			Raypath raypath = raypathList.get(i);
			Phase phase = phaseList.get(i);
			if (!raypath.exists(eventR, phase))
				continue;
			// travelTimeTool.addPanels(panel);
			double epicentralDistance = Math.toDegrees(raypath.computeDelta(eventR,phase));
			double travelTime = raypath.computeT(eventR,phase);
			// System.out.println(epicentralDistance+" "+travelTime);
			String title = phase.isPSV() ? phase + " (P-SV)" : phase + " (SH)";
			double depth = raypath.earthRadius() - gui.getEventR();

			if (delta == null) {
				gui.addResult(epicentralDistance, depth, title, travelTime, raypath.getRayParameter());
				showRayPath(gui, raypath, phase);
			} else {
				double time = travelTime;
				double interval = 0.1;
				double targetDelta = Math.toDegrees(delta[i]);
				if (!phase.isDiffracted())
					try {
						while ((time = RaypathCatalog.travelTimeByThreePointInterpolate(targetDelta, raypath,
								gui.getEventR(), phase, interval)) < 0)
							interval *= 10;
					} catch (Exception e) {
						// e.printStackTrace();
						gui.addResult(epicentralDistance, depth, title, travelTime,
								raypath.getRayParameter());
						showRayPath(gui, raypath, phase);

						continue;
					}
				if (!Double.isNaN(time)) {
					gui.addResult(targetDelta, depth, title, time, raypath.getRayParameter());
					showRayPath(gui, raypath, phase);
				}

			}

		}
		try {
			if (0 < gui.getNumberOfRaypath())
				SwingUtilities.invokeLater(() -> {
					gui.setRaypathVisible(true);
					gui.setResult(0);
					gui.selectRaypath(0);
				});
		} catch (Exception e) {
		}

	}

}
