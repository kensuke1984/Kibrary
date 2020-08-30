package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.Environment;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/**
 * Menu bar for GUI
 *
 * @author Kensuke Konishi
 * @version 0.1.10
 */
final class MenuBar extends JMenuBar {

    private final ANISOtimeGUI GUI;

    private JMenuItem jMenuItemRayparameterMode;
    private JMenuItem jMenuItemEpicentralDistanceMode;
    private JMenuItem jMenuItemAbout;
    private JMenuItem jMenuItemMail;
    private JMenuItem jMenuItemLicense;
    private JMenuItem jMenuItemGuide;
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
        jMenuItemLicense = new JMenuItem("License");
        jMenuItemGuide = new JMenuItem("User guide");
        jMenuItemMail = new JMenuItem("Feedback");

        ButtonGroup buttonGroupModes = new ButtonGroup();
        buttonGroupModes.add(jMenuItemEpicentralDistanceMode);
        buttonGroupModes.add(jMenuItemRayparameterMode);

        jMenuItemEpicentralDistanceMode.setSelected(true);
        jMenuItemPSVSH.setSelected(true);
        jMenuSettings.add(jMenuModes);
        jMenuSettings.add(jMenuPolarization);
        jMenuPolarization.add(jMenuItemPSVSH);
        jMenuPolarization.add(jMenuItemPSV);
        jMenuPolarization.add(jMenuItemSH);
        jMenuHelp.add(jMenuItemAbout);
        jMenuHelp.add(jMenuItemLicense);
        jMenuHelp.add(jMenuItemGuide);
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
                "<html>Please send an Email to <a href>" + ANISOtime.EMAIL_ADDRESS + "</a>."));
        jMenuItemGuide.addActionListener(e -> {
            Path manual = Environment.KIBRARY_SHARE.resolve("user_guide.pdf");
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(manual.toFile());
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            JOptionPane
                    .showMessageDialog(null, "Can't open the manual file. Look at " + manual + ".");
        });
        jMenuItemLicense.addActionListener(e->{
            EULA.showInWindow();
        });
    }
}
