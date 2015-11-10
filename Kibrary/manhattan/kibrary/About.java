package kibrary;

import java.awt.GraphicsEnvironment;


/**
 * About Kibrary (this library).
 * 
 * @since 2015/1/28
 * 
 * 
 * @version 0.2
 * 
 * 
 */
final class About extends javax.swing.JFrame {
	private static final long serialVersionUID = -2485772755944862822L;

	private static final String codeName = "Goblin";
	private static final String version = "0.2.4rc";

	/**
	 * Creates new form AboutANISOtime
	 */
	About() {
		initComponents();
	}

	static final void printLicense() {

		System.out.print("Kibrary " + version + " (" + codeName);
		System.out.println(") Copyright \u00a9 2015 Kensuke Konishi");
		// Copyright [yyyy] [name of copyright owner]
		System.out.println();
		System.out.println("Licensed under the Apache License, Version 2.0 (the \"License\")");
		System.out.println("you may not use this file except in compliance with the License.");
		System.out.println("You may obtain a copy of the License at");

		System.out.println("\n\thttp://www.apache.org/licenses/LICENSE-2.0\n");

		System.out.println("Unless required by applicable law or agreed to in writing, software "
				+ "distributed under the License is distributed on an \"AS IS\" BASIS, "
				+ "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.");
		System.out.println("See the License for the specific language governing permissions and "
				+ "limitations under the License.");
	}

	static final void showLicense() {
		About.main(null);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">
	private void initComponents() {
		setTitle("About Kibrary");
		jScrollPane1 = new javax.swing.JScrollPane();
		jTextArea1 = new javax.swing.JTextArea();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		jTextArea1.setColumns(20);
		jTextArea1.setRows(5);
		jScrollPane1.setViewportView(jTextArea1);
		setLicenseLines();
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
		setSize(300, 300);
		java.awt.EventQueue.invokeLater(() -> jScrollPane1.getVerticalScrollBar().setValue(0));
	}// </editor-fold>

	private void setLicenseLines() {
		String line = "  Kibrary " + version + " (" + codeName + ") \n\nCopyright © 2015 Kensuke Konishi\n"
				+ "Licensed under the Apache License, Version 2.0 (the \"License\")\n"
				+ "You may not use this file except in compliance with the License.\n"
				+ "You may obtain a copy of the License at\n\n" + "http://www.apache.org/licenses/LICENSE-2.0\n\n"
				+ "Unless required by applicable law or agreed to in writing, "
				+ "software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
				+ "See the License for the specific language governing permissions and limitations under the License.";
		jTextArea1.setText(line);

	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		printLicense();
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed" desc=" Look and feel setting
		// code (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the
		 * default look and feel. For details see
		 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.
		 * html
		 */
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
	}

	// Variables declaration - do not modify
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JTextArea jTextArea1;
	// End of variables declaration
}
