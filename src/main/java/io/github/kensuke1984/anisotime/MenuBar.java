package io.github.kensuke1984.anisotime;

import javax.swing.*;

/**
 * Menu bar for GUI
 *
 * @author Kensuke Konishi
 * @version 0.1.6
 */
final class MenuBar extends JMenuBar {

    /**
     * 2020/7/26
     */
    private static final long serialVersionUID = -3385046821286061454L;
    private final ANISOtimeGUI GUI;

    private JMenuItem jMenuItemRayparameterMode;
    private JMenuItem jMenuItemEpicentralDistanceMode;
    private JMenuItem jMenuItemAbout;
    private JMenuItem jMenuItemMail;
    private JMenuItem jMenuItemPSVSH;
    private JMenuItem jMenuItemPSV;
    private JMenuItem jMenuItemSH;

    MenuBar(ANISOtimeGUI gui) {
        GUI = gui;
        initComponents();
    }

    private void initComponents() {
        JMenu jMenuFile = new JMenu("File");
        JMenuItem jMenuItemExit = new JMenuItem("Exit");

        JMenu jMenuSettings = new JMenu("Settings");
        JMenu jMenuModes = new JMenu("Switch Mode");
        jMenuItemEpicentralDistanceMode = new JRadioButtonMenuItem("Epicentral Distance Mode");
        jMenuItemRayparameterMode = new JRadioButtonMenuItem("Ray parameter Mode");

        JMenu jMenuPolarization = new JMenu("Switch Polarization");
        jMenuItemPSVSH = new JRadioButtonMenuItem("All");
        jMenuItemPSV = new JRadioButtonMenuItem("P-SV");
        jMenuItemSH = new JRadioButtonMenuItem("SH");

        ButtonGroup buttonGroupPolarization = new ButtonGroup();
        buttonGroupPolarization.add(jMenuItemPSVSH);
        buttonGroupPolarization.add(jMenuItemPSV);
        buttonGroupPolarization.add(jMenuItemSH);

        JMenu jMenuHelp = new JMenu("Help");
        jMenuItemAbout = new JMenuItem("About");
        jMenuItemMail = new JMenuItem("Feedback");

        ButtonGroup buttonGroupModes = new ButtonGroup();
        buttonGroupModes.add(jMenuItemEpicentralDistanceMode);
        buttonGroupModes.add(jMenuItemRayparameterMode);

        // first is normal mode
        jMenuItemEpicentralDistanceMode.setSelected(true);
        jMenuItemPSVSH.setSelected(true);
        jMenuSettings.add(jMenuModes);
        jMenuSettings.add(jMenuPolarization);
        jMenuPolarization.add(jMenuItemPSVSH);
        jMenuPolarization.add(jMenuItemPSV);
        jMenuPolarization.add(jMenuItemSH);
        jMenuHelp.add(jMenuItemAbout);
        jMenuHelp.add(jMenuItemMail);
        jMenuModes.add(jMenuItemEpicentralDistanceMode);
        jMenuModes.add(jMenuItemRayparameterMode);
        jMenuFile.add(jMenuItemExit);
        jMenuItemExit.addActionListener(e -> System.exit(0));
        add(jMenuFile);
        add(jMenuSettings);
        add(jMenuHelp);
        setListeners();
    }

    private void setListeners() {
        jMenuItemPSVSH.addActionListener(e -> GUI.setPolarity(0));
        jMenuItemPSV.addActionListener(e -> GUI.setPolarity(1));
        jMenuItemSH.addActionListener(e -> GUI.setPolarity(2));

        jMenuItemRayparameterMode.addActionListener(e -> GUI.setMode(ComputationMode.RAY_PARAMETER));
        jMenuItemEpicentralDistanceMode.addActionListener(e -> GUI.setMode(ComputationMode.EPICENTRAL_DISTANCE));

        jMenuItemAbout.addActionListener(e -> About.main(null));
        jMenuItemMail.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "<html>Please send an Email to <a href>" + io.github.kensuke1984.kibrary.About.EMAIL_ADDRESS +
                        "</a>."));

    }
}
