package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.util.Utilities;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Kensuke Konishi
 * @version 0.0.6.3
 */
class ResultWindow extends javax.swing.JPanel {
    /**
     * 2017/11/4
     */
    private static final long serialVersionUID = 4316137174413958628L;
    private final ANISOtimeGUI GUI;
    private SampleTableCellRenderer02 render;
    private javax.swing.JTable jTable1;

    ResultWindow(ANISOtimeGUI gui) {
        GUI = gui;
        initComponents();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | javax.swing.UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException ex) {
            java.util.logging.Logger.getLogger(ResultWindow.class.getName())
                    .log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new ResultWindow(null).setVisible(true));
    }

    private void initComponents() {
        JScrollPane jScrollPane1 = new JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane1.setViewportView(jTable1);
        render = new SampleTableCellRenderer02();
        jTable1.setDefaultRenderer(Object.class, render);
        // setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        // jTable1.setEnabled(false);
        jTable1.setDefaultEditor(Object.class, null);
        jTable1.setRowSelectionAllowed(true);
        jTable1.setColumnSelectionAllowed(false);
        jTable1.setShowGrid(true);
        jTable1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = jTable1.getSelectedRow();
                setColor(row);
                GUI.selectRaypath(row);
            }
        });
        jTable1.setModel(new DefaultTableModel(new Object[][]{},
                new String[]{"\u0394 [deg]", "Depth [km]", "Phase", "Time [s]", "Ray parameter"}));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE));
        // setPreferredSize(getMinimumSize());
    }
    void clearRows() {
        ((DefaultTableModel) jTable1.getModel()).setRowCount(0);
    }

    /**
     * @param epicentralDistance [deg]
     * @param depth              [km]
     * @param phase              seismic phase
     * @param travelTime         [s]
     */
    void addRow(double epicentralDistance, double depth, String phase, double travelTime, double rayparameter) {
        String delta = Utilities.fixDecimalPlaces(2, epicentralDistance);
        String depthS = Utilities.fixDecimalPlaces(2, depth);
        String p = Utilities.fixDecimalPlaces(2, rayparameter);
        String time = Utilities.fixDecimalPlaces(2, travelTime);
        try {
            SwingUtilities.invokeAndWait(() -> ((DefaultTableModel) (jTable1.getModel()))
                    .addRow(new String[]{delta, depthS, phase, time, p}));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * the row will be featured.
     *
     * @param i number for the row
     */
    void setColor(int i) {
        render.featured = i;
        repaint();
    }

    private class SampleTableCellRenderer02 extends DefaultTableCellRenderer {

        private static final long serialVersionUID = -4100672856859272722L;
        private int featured;

        SampleTableCellRenderer02() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			/*
             * どういう呼ばれ方をしてるのか確認するための出力文 目に見えるセルだけ毎度、描画しているらしい スクロールさせると、それがわかる
			 */
            // System.out.println("row:" + row + " /column:" + column +
            // " /selected:" + isSelected + " /focus:" + hasFocus + " /value:" +
            // value);
            if (row == featured) {
                setForeground(Color.RED);
                setBackground(Color.white);
            } else {
                setForeground(Color.BLACK);
                setBackground(Color.white);
            }
            // System.out.println(featured+" fe");
            // // 選択されている行を赤色にする
            // if(isSelected) {
            // this.setBackground(Color.RED);
            // }
            // else {
            // this.setBackground(table.getBackground());
            // }

            // // フォーカスが当たっているセルを黄色にする
            // if(hasFocus) {
            // this.setBackground(Color.yellow);
            // }

            // // 行番号=1/列番号=1のセルを青色にする
            // if((row == 1) && (column == 1)) {
            // this.setBackground(Color.BLUE);
            // }

            return this;
        }
    }
}
