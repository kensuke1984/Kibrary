package io.github.kensuke1984.anisotime;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 
 * ANISOtime launcher.
 * 
 * @author Kensuke Konishi
 * @version 0.3.8
 * 
 */
final class ANISOtime extends TravelTimeGUI {
	
	static final String codename = "Promyvion";
	
	static final String version ="0.3.7.1b";
	
	private static final long serialVersionUID = -4093263118460123169L;

	/** 
	 * Creates new form TravelTimeGUI
	 */
	private ANISOtime() {
		setLocationRelativeTo(null);
	}

	private Computation currentComputationMode;

	@Override
	void save() {
		if(currentComputationMode==null){
			JOptionPane.showMessageDialog(null, "Compute first.");
			return;
		}
		currentComputationMode.outputPath();
	}

	/**
	 * when the button "Compute" is clicked.
	 */
	@Override
	void compute() {
		createNewRaypathTabs();
		switch (selectedMode()) {
		case RAYPARAMETER:
			Raypath raypath = new Raypath(getMostImportant(), getEventR(), getStructure(), false);
			RayparameterMode normalMode = new RayparameterMode(this, raypath);
			currentComputationMode = normalMode;
			break;
		case TURNING_DEPTH:
			TurningDepthMode tdm = new TurningDepthMode(this, getStructure(), getEventR());
			currentComputationMode = tdm;
			break;
		case DIFFRACTION:
			DiffractionMode dMode = new DiffractionMode(this, getStructure(), getEventR());
			currentComputationMode = dMode;
			break;
		case EPICENTRAL_DISTANCE:
			EpicentralDistanceMode eMode = new EpicentralDistanceMode(this, getSelectedPhases(),
					Math.toRadians(getMostImportant()), getStructure(), getEventR());
			currentComputationMode = eMode;
			break;
		default:
			JOptionPane.showMessageDialog(null, "sorry not yet.");
			return;
		}
		new Thread(currentComputationMode).start();

	}

	private Raypath currentRaypath;

	synchronized void computed(Raypath raypath) {
		currentRaypath = raypath;
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		if (args.length != 0) {
			TravelTimeCLI.main(args);
			return;
		}
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) 
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(ANISOtime.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(ANISOtime.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(ANISOtime.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(ANISOtime.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		// </editor-fold>
		// System.out.println(SwingUtilities.isEventDispatchThread() + " "
		// + Thread.currentThread().getName());
		/* Create and display the form */
		SwingUtilities.invokeLater(() -> new ANISOtime().setVisible(true));
	}

}
