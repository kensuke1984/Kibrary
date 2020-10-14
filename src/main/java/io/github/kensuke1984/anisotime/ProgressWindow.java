/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.github.kensuke1984.anisotime;

import java.awt.*;

/**
 * Progress window for computation
 *
 * @author Kensuke Konishi
 * @version 0.0.2.1
 */
final class ProgressWindow extends javax.swing.JFrame {

    private static final long serialVersionUID = 4174550805475268586L;
    /**
     * if "cancel" button is pushed.
     */
    private boolean canceled;
    private int iProgress;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JLabel jLabelComputing;
    private javax.swing.JProgressBar jProgressBar;

    /**
     * @param min of progressbar
     * @param max of progressbar
     */
    ProgressWindow(int min, int max) {
        jProgressBar = new javax.swing.JProgressBar(min, max);
        initComponents();
        setFocusableWindowState(false);
        setState(Frame.ICONIFIED);
    }

    boolean isCanceled() {
        return canceled;
    }


    private void initComponents() {

        jLabelComputing = new javax.swing.JLabel("Computing");
        jButtonCancel = new javax.swing.JButton("Cancel");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        jButtonCancel.addActionListener(e -> {
            canceled = true;
            dispose();
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addGap(31, 31, 31).addGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                .addComponent(jProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createSequentialGroup().addComponent(jLabelComputing)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButtonCancel))).addContainerGap(31, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addGap(24, 24, 24).addGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabelComputing).addComponent(jButtonCancel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(22, Short.MAX_VALUE)));

        pack();

    }

    synchronized void addProgress() {
        jProgressBar.setValue(++iProgress);
    }
}
