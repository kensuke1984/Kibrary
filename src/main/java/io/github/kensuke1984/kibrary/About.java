package io.github.kensuke1984.kibrary;

import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * About Kibrary (this library).
 * <p>
 * Library for waveform inversion.
 * <p>
 * <br>
 *
 * @author Kensuke Konishi
 * @version {@value VERSION}
 * @see <a href=https://github.com/kensuke1984/Kibrary>GitHub</a>
 * @see <a href=https://kensuke1984.github.io/Kibrary>Javadoc</a>
 */
public final class About extends javax.swing.JFrame {
    public static final String EMAIL_ADDRESS = "kensuke@earth.sinica.edu.tw";
    public static final String CODENAME = "Shiva";
    public static final String VERSION = "0.4.9.10";
    private static final String line;

    //kibrary.jar
    private static final String KIBRARY_JAR_URL = "https://bit.ly/305JHrE";

    /**
     * 2020/7/27
     */
    private static final long serialVersionUID = 5648703402071570271L;

    static {
        try {
            line = String.join("\n", IOUtils.readLines(About.class.getClassLoader().getResourceAsStream("LICENSE"),
                    Charset.defaultCharset()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JScrollPane jScrollPane1;

    private About() {
        super("About Kibrary");
        initComponents();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            Logger.getLogger(About.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!GraphicsEnvironment.isHeadless()) SwingUtilities.invokeLater(() -> new About().setVisible(true));
        else System.out.println(line);
    }

    private void initComponents() {
        JTextArea jTextArea1 = new JTextArea(line, 5, 20);
        jScrollPane1 = new JScrollPane(jTextArea1);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        jTextArea1.setLineWrap(true);
        jTextArea1.setEditable(false);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap()
                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap()
                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE).addContainerGap()));
        pack();
        SwingUtilities.invokeLater(() -> jScrollPane1.getVerticalScrollBar().setValue(0));
    }

    private static void downloadKibrary() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        Path localPath = Paths.get(About.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(localPath) || localPath.toString().contains("anisotime")) return;
        String localSum = Utilities.checksum(localPath, "SHA-256");
        Path path = Utilities.download(new URL(KIBRARY_JAR_URL));
        String cloudSum = Utilities.checksum(path, "SHA-256");
        if (localSum.equals(cloudSum)) return;
        Path latest = localPath.resolveSibling("latest_kibrary");
        Files.move(path, latest, StandardCopyOption.REPLACE_EXISTING);
        try {
            JOptionPane.showMessageDialog(null, "Software update is found. The latest version is " + latest);
        } catch (HeadlessException e) {
            System.err.println("Software update is found. The latest version is " + latest);
        }
        System.exit(55);
    }
}
