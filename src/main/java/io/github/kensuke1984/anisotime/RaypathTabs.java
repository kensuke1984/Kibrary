/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * @author Kensuke Konishi
 * 
 * @version 0.1.0.1
 * 
 * 
 */
class RaypathTabs extends javax.swing.JFrame {

	private static final long serialVersionUID = 406911785152443889L;

	RaypathTabs(ANISOTimeGUI travelTimeGui, RaypathPanel raypathPanel) {
		super("Raypath");
		this.raypathPanel = raypathPanel;
		this.travelTimeGUI = travelTimeGui;
		initComponents();

	}

	void addPath(double[] x, double[] y) {
		raypathPanel.addPath(x, y);
	}

	private RaypathPanel raypathPanel;

	private ANISOTimeGUI travelTimeGUI;

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">
	private void initComponents() {

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		// getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
				javax.swing.GroupLayout.Alignment.TRAILING,
				layout.createSequentialGroup().addContainerGap().addComponent(raypathPanel).addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
				javax.swing.GroupLayout.Alignment.TRAILING,
				layout.createSequentialGroup().addContainerGap().addComponent(raypathPanel).addContainerGap()));

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				int size = getSize().width < getSize().height ? getSize().width : getSize().height;
				setSize(size, size);
			}
		});

		pack();
		setSize(700, 700);
		setLocation(travelTimeGUI.getX() - 700, travelTimeGUI.getY());

	}// </editor-fold>

	void selectTab(int i) {
		raypathPanel.setFeatured(i);
		revalidate();
		repaint();
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(RaypathTabs.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(RaypathTabs.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(RaypathTabs.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(RaypathTabs.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		}
		// </editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(() -> new RaypathTabs(null, null).setVisible(true));
	}

	// Variables declaration - do not modify
	// private javax.swing.JTabbedPane jTabbedPane3;
	// End of variables declaration
}
