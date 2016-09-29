/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import org.apache.commons.io.IOUtils;

/**
 * Menubar for GUI
 * 
 * @author Kensuke Konishi
 * @version 0.1.5
 * 
 */
final class MenuBar extends JMenuBar {

	private static final long serialVersionUID = -3885037230307922628L;

	MenuBar(ANISOtimeGUI gui) {
		this.gui = gui;
		initComponents();
	}

	private final ANISOtimeGUI gui;

	private ButtonGroup buttonGroupModes = new ButtonGroup();
	private ButtonGroup buttonGroupPolarization = new ButtonGroup();

	private void initComponents() {
		jMenuFile = new JMenu("File");
		jMenuHelp = new JMenu("Help");
		jMenuSettings = new JMenu("Settings");
		jMenuModes = new JMenu("Switch Mode");
		jMenuPolarization = new JMenu("Switch Polarization");
		jMenuItemExit = new JMenuItem("Exit");
		jMenuItemParameterDescription = new JMenuItem("Parameter description");
		jMenuItemRayparameterMode = new JRadioButtonMenuItem("Ray parameter Mode");
		jMenuItemEpicentralDistanceMode = new JRadioButtonMenuItem("Epicentral Distance Mode");
		jMenuItemPreferences = new JMenuItem("Preferences");

		jMenuItemAbout = new JMenuItem("About");
		jMenuItemMail = new JMenuItem("Feedback");

		jMenuItemPSVSH = new JRadioButtonMenuItem("All");
		jMenuItemPSV = new JRadioButtonMenuItem("P-SV");
		jMenuItemSH = new JRadioButtonMenuItem("SH");
		buttonGroupPolarization.add(jMenuItemPSVSH);
		buttonGroupPolarization.add(jMenuItemPSV);
		buttonGroupPolarization.add(jMenuItemSH);

		buttonGroupModes.add(jMenuItemEpicentralDistanceMode);
		buttonGroupModes.add(jMenuItemRayparameterMode);

		


		// first is normal mode
		((JRadioButtonMenuItem) jMenuItemEpicentralDistanceMode).setSelected(true);
		((JRadioButtonMenuItem) jMenuItemPSVSH).setSelected(true);
		jMenuSettings.add(jMenuModes);
		jMenuSettings.add(jMenuPolarization);
		jMenuSettings.add(jMenuItemPreferences);
		jMenuPolarization.add(jMenuItemPSVSH);
		jMenuPolarization.add(jMenuItemPSV);
		jMenuPolarization.add(jMenuItemSH);
		jMenuHelp.add(jMenuItemAbout);
		jMenuHelp.add(jMenuItemParameterDescription);
		jMenuHelp.add(jMenuItemMail);
		jMenuModes.add(jMenuItemEpicentralDistanceMode);
		jMenuModes.add(jMenuItemRayparameterMode);
		jMenuFile.add(jMenuItemExit);
		add(jMenuFile);
		add(jMenuSettings);
		add(jMenuHelp);
		setListeners();;
	}

	private void setListeners() {
		jMenuItemPSVSH.addActionListener(e -> gui.setPolarity(0));
		jMenuItemPSV.addActionListener(e -> gui.setPolarity(1));
		jMenuItemSH.addActionListener(e -> gui.setPolarity(2));
		
		jMenuItemRayparameterMode.addActionListener(e -> gui.setMode(ComputationMode.RAY_PARAMETER));
		jMenuItemEpicentralDistanceMode.addActionListener(e -> gui.setMode(ComputationMode.EPICENTRAL_DISTANCE));
		jMenuItemPreferences.addActionListener(e -> gui.changePropertiesVisible());

	
	
		jMenuItemExit.addActionListener(evt -> System.exit(0));

		jMenuItemParameterDescription.addActionListener(e -> {
			URL descriptionPdf = getClass().getClassLoader().getResource("description.pdf");
			String tmpPDFname = ".description"
					+ Integer.toString((int) (Math.random() * Math.random() * Math.random() * 1000)) + ".pdf";
			Path pdfFile = Paths.get("/var/tmp", tmpPDFname);
			try {
				pdfFile = Files.createFile(pdfFile);
			} catch (Exception e2) {
				pdfFile = Paths.get(System.getProperty("user.dir"), tmpPDFname);
			}
			try (BufferedOutputStream pdfOutStream = new BufferedOutputStream(Files.newOutputStream(pdfFile));
					InputStream pdfStream = descriptionPdf.openStream()) {
				IOUtils.copy(pdfStream, pdfOutStream);
			} catch (Exception e2) {
				e2.printStackTrace();
				return;
			}
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().open(pdfFile.toFile());
					pdfFile.toFile().deleteOnExit();
					return;
				} catch (Exception ex) {
					ex.printStackTrace();
					// JOptionPane.showMessageDialog(null,
					// "미안 해요. PDF를 열 수 없습니다");
				}
			}
			JOptionPane.showMessageDialog(null,
					"Can't open a pdf file. Look at " + pdfFile + " and delete by yourself.");
		});
		jMenuItemAbout.addActionListener(e -> About.main(null));
		jMenuItemMail.addActionListener(e -> {
			try {
				Desktop.getDesktop().mail(new URI("mailto:bob@eps.s.u-tokyo.ac.jp"));
			} catch (Exception e2) {
				JOptionPane.showMessageDialog(null,
						"<html>Can't launch a mailer. Please send Email to <a href>traveltimereport@outlook.com</a>.");
			}
		});
	
	
	
	
	
	
	
	
	
	}

	private JMenu jMenuFile;
	private JMenu jMenuSettings;
	private JMenu jMenuModes;
	private JMenu jMenuHelp;
	private JMenu jMenuPolarization;
	// private javax.swing.JMenu jMenu2;
	private JMenuItem jMenuItemExit;
	private JMenuItem jMenuItemRayparameterMode;
	private JMenuItem jMenuItemEpicentralDistanceMode;
	private JMenuItem jMenuItemParameterDescription;
	private JMenuItem jMenuItemAbout;
	private JMenuItem jMenuItemMail;
	private JMenuItem jMenuItemPreferences;
	private JMenuItem jMenuItemPSVSH;
	private JMenuItem jMenuItemPSV;
	private JMenuItem jMenuItemSH;
}
