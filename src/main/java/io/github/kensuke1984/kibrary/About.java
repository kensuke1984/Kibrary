package io.github.kensuke1984.kibrary;

import java.awt.GraphicsEnvironment;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

/**
 * About Kibrary (this library).
 * 
 * Library for waveform inversion.
 * 
 * <br>
 * 
 * @see <a href=https://github.com/kensuke1984/Kibrary>GitHub</a>
 * @see <a href=https://kensuke1984.github.io/Kibrary>Javadoc</a>
 * 
 * @author Kensuke Konishi
 * @version 0.4.1
 * 
 */
final class About extends javax.swing.JFrame {
	private static final long serialVersionUID = -2485772755944862822L;

	public static final String codename = "Sahagin";

	public static final String version = "0.4.1";

	private About() {
		super("About Kibrary");
		initComponents();
	}

	private void initComponents() {
		jTextArea1 = new javax.swing.JTextArea(line, 5, 20);
		jScrollPane1 = new javax.swing.JScrollPane(jTextArea1);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		jTextArea1.setLineWrap(true);
		jTextArea1.setEditable(false);
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
						.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
						.addContainerGap()));
		pack();
		java.awt.EventQueue.invokeLater(() -> jScrollPane1.getVerticalScrollBar().setValue(0));
	}

	private static final String line;

	static {
		try {
			line = String.join("\n", IOUtils.readLines(About.class.getClassLoader().getResourceAsStream("LICENSE"),
					Charset.defaultCharset()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
			java.util.logging.Logger.getLogger(About.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(About.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(About.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(About.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		// </editor-fold>
		/* Create and display the form */
		if (!GraphicsEnvironment.isHeadless())
			java.awt.EventQueue.invokeLater(() -> new About().setVisible(true));
		else
			System.out.println(line);
	}

	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JTextArea jTextArea1;
}
