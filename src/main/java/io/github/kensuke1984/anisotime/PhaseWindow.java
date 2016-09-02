/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.HashSet;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;

/**
 * 
 * Window for choosing phases.
 * 
 * @author Kensuke Konishi
 * 
 * @version 0.0.3
 * 
 */
class PhaseWindow extends javax.swing.JFrame {

	private static final long serialVersionUID = 467880519968141950L;

	private final ANISOtimeGUI gui;

	/**
	 * Creates new form NewJFrame
	 */
	PhaseWindow(ANISOtimeGUI gui) {
		super("phase");
		this.gui = gui;
		initComponents();
	}

	/**
	 * @param i
	 *            0:All, 1:P-SV, 2:SH
	 */
	void setPolarity(int i) {
		final ListCellRenderer<? super String> r = jList1.getCellRenderer();
		jList1.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
			Phase p = Phase.create((String) jList1.getModel().getElementAt(index));
			boolean sh = p.pReaches() == null;
			Component c;
			if (i == 2 && !sh) {
				c = r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				c.setEnabled(false);
			} else {
				c = r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				c.setEnabled(true);
			}
			return c;
		});
		gui.setPhaseSet(new HashSet<>(jList1.getSelectedValuesList()));
	}

	private void initComponents() {
		setLocationRelativeTo(null);
		jScrollPane1 = new javax.swing.JScrollPane();
		jList1 = new javax.swing.JList<>();

		jTextFieldPhaseAdd = GUIInputComponents.createPhaseField();
		jTextFieldPhaseAdd.setText("pP, sS");

		jButtonAdd = new javax.swing.JButton("Add");
		setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		setResizable(false);
		jList1.setBorder(null);

		phaseLists = new DefaultListModel<>();
		jList1.setModel(phaseLists);
		phaseLists.addElement("p");
		phaseLists.addElement("s");
		phaseLists.addElement("P");
		phaseLists.addElement("S");
		phaseLists.addElement("PcP");
		phaseLists.addElement("ScS");
		phaseLists.addElement("Pdiff");
		phaseLists.addElement("Sdiff");
		phaseLists.addElement("PKP");
		phaseLists.addElement("SKS");
		phaseLists.addElement("PKiKP");
		phaseLists.addElement("PKIKP");
		phaseLists.addElement("SKIKS");
		jScrollPane1.setViewportView(jList1);

		jButtonAdd.setToolTipText("");
		jList1.setSelectedIndices(new int[] { 2, 3 });
		jButtonAdd.addActionListener(evt -> {
			String line = jTextFieldPhaseAdd.getText();
			Arrays.stream(line.split(",")).map(String::trim).filter(str -> phaseLists.indexOf(str) == -1)
					.forEach(phaseLists::addElement);
		});
		JLabel jLabelShift = new JLabel("Hold SHIFT to select range");
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
								.addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 194,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jLabelShift, javax.swing.GroupLayout.PREFERRED_SIZE, 194,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGroup(layout.createSequentialGroup()
										.addComponent(jTextFieldPhaseAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 139,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jButtonAdd, javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabelShift, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jTextFieldPhaseAdd, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jButtonAdd))
						.addContainerGap(20, Short.MAX_VALUE)));

		jList1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				gui.setPhaseSet(new HashSet<>(jList1.getSelectedValuesList()));
			}
		});

		pack();
	}// </editor-fold>

	private DefaultListModel<String> phaseLists;
	private javax.swing.JButton jButtonAdd;
	private javax.swing.JList<String> jList1;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JTextField jTextFieldPhaseAdd;
}
