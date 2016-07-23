package io.github.kensuke1984.anisotime;

import javax.swing.SwingUtilities;

import org.apache.commons.cli.ParseException;

/**
 * 
 * ANISOtime launcher.
 * 
 * @author Kensuke Konishi
 * @version 1.0b
 * 
 */
final class ANISOtime {

	static final String codename = "Tavnazia";

	static final String version = "1.0b";

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
		if (args.length != 0) {
			try {
				ANISOTimeCLI.main(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
