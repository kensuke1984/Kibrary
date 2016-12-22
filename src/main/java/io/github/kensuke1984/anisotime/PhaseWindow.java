package io.github.kensuke1984.anisotime;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.HashSet;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;
import javax.swing.WindowConstants;

/**
 * Window for choosing phases.
 *
 * @author Kensuke Konishi
 * @version 0.0.3.1
 */
class PhaseWindow extends javax.swing.JFrame {

    /**
     * 2016/12/3
     */
    private static final long serialVersionUID = -8346038785047175817L;

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
     * @param i 0:All, 1:P-SV, 2:SH
     */
    void setPolarity(int i) {
        ListCellRenderer<? super String> r = jList1.getCellRenderer();
        jList1.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            Phase p = Phase.create(jList1.getModel().getElementAt(index));
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
        JScrollPane jScrollPane1 = new JScrollPane();
        jList1 = new javax.swing.JList<>();

        jTextFieldPhaseAdd = GUIInputComponents.createPhaseField();
        jTextFieldPhaseAdd.setText("pP, sS");

        JButton jButtonAdd = new JButton("Add");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
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
        jList1.setSelectedIndices(new int[]{2, 3});
        jButtonAdd.addActionListener(evt -> {
            String line = jTextFieldPhaseAdd.getText();
            Arrays.stream(line.split(",")).map(String::trim).filter(str -> phaseLists.indexOf(str) == -1)
                    .forEach(phaseLists::addElement);
        });
        JLabel jLabelShift = new JLabel("Hold SHIFT to select range");
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap().addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 194, GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabelShift, GroupLayout.PREFERRED_SIZE, 194, GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(jTextFieldPhaseAdd, GroupLayout.PREFERRED_SIZE, 139,
                                                GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(jButtonAdd, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)))
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap()
                        .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabelShift, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextFieldPhaseAdd, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE).addComponent(jButtonAdd))
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
    private JList<String> jList1;
    private JTextField jTextFieldPhaseAdd;
}
