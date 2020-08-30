package io.github.kensuke1984.kibrary;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * About Kibrary.
 *
 * @author Kensuke Konishi
 * @version {@value VERSION}
 * @see <a href=https://github.com/kensuke1984/Kibrary>GitHub</a>
 * @see <a href=https://kensuke1984.github.io/Kibrary>Javadoc</a>
 */
public final class About extends javax.swing.JFrame {
    public static final String EMAIL_ADDRESS = "kensuke1984@gmail.com";
    public static final String CODENAME = "Shiva";
    public static final String VERSION = "0.4.9.26";
    private static final String LINE = "Kibrary " + VERSION + " (" + CODENAME + ")\n" +
            "Copyright \u00a9 2015-2020 Kensuke Konishi and Anselme F.E. Borgeaud.\n\n" +
            "This software is licensed under the GNU General Public License Version 3, 29 June 2007 (https://www.gnu.org/licenses/).\n";

    // kibrary.jar
    private static final String KIBRARY_JAR_URL = "https://bit.ly/31FkTrh";

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
        else System.err.println(LINE);
    }

    private void initComponents() {
        JTextArea jTextArea1 = new JTextArea(LINE, 5, 20);
        JScrollPane jScrollPane1 = new JScrollPane(jTextArea1);
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

}
