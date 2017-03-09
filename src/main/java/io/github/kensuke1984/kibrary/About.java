package io.github.kensuke1984.kibrary;

import java.awt.GraphicsEnvironment;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import javax.swing.*;

/**
 * About Kibrary (this library).
 * <p>
 * Library for waveform inversion.
 * <p>
 * <br>
 *
 * @author Kensuke Konishi
 * @version {@value version}
 * @see <a href=https://github.com/kensuke1984/Kibrary>GitHub</a>
 * @see <a href=https://kensuke1984.github.io/Kibrary>Javadoc</a>
 */
final class About extends javax.swing.JFrame {
    private static final long serialVersionUID = -2485772755944862822L;

    public static final String codename = "Sahagin-yuki";

    public static final String version = "0.1.0";

    private About() {
        super("About Kibrary");
        initComponents();
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

    private static final String line;

    static {
        try {
            line = String.join("\n", IOUtils.readLines(About.class.getClassLoader().getResourceAsStream("LICENSE"),
                    Charset.defaultCharset()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
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
        // </editor-fold>
        /* Create and display the form */
        if (!GraphicsEnvironment.isHeadless()) SwingUtilities.invokeLater(() -> new About().setVisible(true));
        else System.out.println(line);
    }

    private JScrollPane jScrollPane1;
}
