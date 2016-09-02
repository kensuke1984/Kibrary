package io.github.kensuke1984.anisotime;

import java.awt.GraphicsEnvironment;

/**
 * Information about ANISO time package.
 * 
 * @see <a href=http://www-solid.eps.s.u-tokyo.ac.jp/~dsm/anisotime.htm>web</a>
 * @author Kensuke Konishi
 * 
 * @version 0.0.3
 * 
 */
final class About extends javax.swing.JFrame {

	private static final long serialVersionUID = -8583520488520194322L;

	private About() {
		super("About ANISOtime");
		initComponents();
	}

	private void initComponents() {
		jScrollPane1 = new javax.swing.JScrollPane();
		jTextArea1 = new javax.swing.JTextArea(line, 5, 20);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		jScrollPane1.setViewportView(jTextArea1);
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

	private static final String line = "ANISOtime " + ANISOtime.version + " (" + ANISOtime.codename
			+ ") Copyright \u00a9 2015 Kensuke Konishi\n\n"
			+ "Licensed under the Apache License, Version 2.0 (the \"License\")\n"
			+ "You may not use this file except in compliance with the License.\n"
			+ "You may obtain a copy of the License at\n\n" + "\thttp://www.apache.org/licenses/LICENSE-2.0\n\n"
			+ "Unless required by applicable law or agreed to in writing, "
			+ "software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
			+ "See the License for the specific language governing permissions and limitations under the License.";

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
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

	// Variables declaration - do not modify
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JTextArea jTextArea1;
	// End of variables declaration
}
