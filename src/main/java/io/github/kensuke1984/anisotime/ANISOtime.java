package io.github.kensuke1984.anisotime;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.apache.commons.cli.ParseException;

/**
 * 
 * ANISOtime launcher.
 * 
 * @author Kensuke Konishi
 * @version 1.0.1.1b
 * 
 */
final class ANISOtime {

	static final String codename = "Tavnazia";

	static final String version = "1.0.1b";

	/**
	 * Creates new form TravelTimeGUI
	 */
	private ANISOtime() {
	}

	/**
	 * @param args
	 *            the command line arguments
	 * @throws ParseException
	 */
	public static void main(String args[]) {
		if (args.length != 0)
			try {
				ANISOtimeCLI.main(args);
				return;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		else if (GraphicsEnvironment.isHeadless()) {
			System.err.println("No graphical environment.. please use CLI.");
			ANISOtimeCLI.printHelp();
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
		SwingUtilities.invokeLater(() -> new ANISOtimeGUI().setVisible(true));
	}

}
