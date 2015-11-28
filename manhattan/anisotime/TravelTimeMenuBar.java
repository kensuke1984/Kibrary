/**
 * 
 */
package anisotime;

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
 * @author kensuke
 * @version 0.1.3
 * 
 */
final class TravelTimeMenuBar extends JMenuBar {

	private static final long serialVersionUID = -3885037230307922628L;

	TravelTimeMenuBar(TravelTimeGUI gui) {
		super();
		this.gui = gui;
		initComponents();
	}

	private TravelTimeGUI gui;

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
		jMenuItemTurningDepthMode = new JRadioButtonMenuItem("Turning Depth Mode");
		jMenuItemDiffractionMode = new JRadioButtonMenuItem("Diffraction Mode");
		jMenuItemRayparameterMode = new JRadioButtonMenuItem("Rayparameter Mode");
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

		buttonGroupModes.add(jMenuItemDiffractionMode);
		buttonGroupModes.add(jMenuItemTurningDepthMode);
		buttonGroupModes.add(jMenuItemEpicentralDistanceMode);
		buttonGroupModes.add(jMenuItemRayparameterMode);

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
		jMenuItemAbout.addActionListener(e -> {
			new About().setVisible(true);
		});
		jMenuItemMail.addActionListener(e -> {
			try {
				Desktop.getDesktop().mail(new URI("mailto:bob@eps.s.u-tokyo.ac.jp"));
			} catch (Exception e2) {
				JOptionPane.showMessageDialog(null,
						"<html>Can't launch a mailer. Please send Email to <a href>traveltimereport@outlook.com</a>.");
			}
		});

		setModeSelect();

		// first is normal mode
		mode = ComputationMode.EPICENTRAL_DISTANCE;
		((JRadioButtonMenuItem) jMenuItemEpicentralDistanceMode).setSelected(true);
		((JRadioButtonMenuItem) jMenuItemPSVSH).setSelected(true);
		polarization = 0;
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
		jMenuModes.add(jMenuItemTurningDepthMode);
		jMenuModes.add(jMenuItemDiffractionMode);
		jMenuFile.add(jMenuItemExit);
		add(jMenuFile);
		add(jMenuSettings);
		add(jMenuHelp);
		setPolarization();
	}

	private int polarization;

	/**
	 * @return 0(default): All, 1: P-SV, 2: SH
	 */
	int getPolarization() {
		return polarization;
	}

	/**
	 * 0(default): All, 1: P-SV, 2: SH
	 */
	void setPolarity(int i) {
		polarization = i;
	}

	private void setPolarization() {
		jMenuItemPSVSH.addActionListener(e -> {
			gui.setPolarity(0);
		});
		jMenuItemPSV.addActionListener(e -> {
			gui.setPolarity(1);
		});
		jMenuItemSH.addActionListener(e -> {
			gui.setPolarity(2);
		});
	}

	String getPoleString() {
		String pole = null;
		switch (polarization) {
		case 0:
			pole = "Polarity:All";
			break;
		case 1:
			pole = "Polarity:P-SV";
			break;
		case 2:
			pole = "Polarity:SH";
			break;
		default:
			throw new RuntimeException("Unexpected");
		}
		return pole;
	}

	String getModeName() {
		switch (mode) {
		case TURNING_DEPTH:
			return "Mode:Turning Depth";
		case DIFFRACTION:
			return "Mode:Diffraction";
		case EPICENTRAL_DISTANCE:
			return "Mode:Epicentral Distance";
		case RAYPARAMETER:
			return "Mode:Rayparameter";
		default:
			throw new RuntimeException("Error");
		}
	}

	private void setModeSelect() {
		jMenuItemTurningDepthMode.addActionListener(e -> {
			mode = ComputationMode.TURNING_DEPTH;
			gui.setMode(mode);
			gui.setPolarity(polarization);
		});
		jMenuItemDiffractionMode.addActionListener(e -> {
			mode = ComputationMode.DIFFRACTION;
			gui.setMode(mode);
			gui.setPolarity(polarization);
		});
		jMenuItemRayparameterMode.addActionListener(e -> {
			mode = ComputationMode.RAYPARAMETER;
			gui.setMode(mode);
			gui.setPolarity(polarization);
		});
		jMenuItemEpicentralDistanceMode.addActionListener(e -> {
			mode = ComputationMode.EPICENTRAL_DISTANCE;
			gui.setMode(mode);
			gui.setPolarity(polarization);
		});
		jMenuItemPreferences.addActionListener(e -> {
			gui.changePropertiesVisible();
		});
	}

	ComputationMode selectedMode() {
		return mode;
	}

	private ComputationMode mode;

	private JMenu jMenuFile;
	private JMenu jMenuSettings;
	private JMenu jMenuModes;
	private JMenu jMenuHelp;
	private JMenu jMenuPolarization;
	// private javax.swing.JMenu jMenu2;
	private JMenuItem jMenuItemExit;
	private JMenuItem jMenuItemTurningDepthMode;
	private JMenuItem jMenuItemDiffractionMode;
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
