package io.github.kensuke1984.anisotime;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.swing.UIManager.*;

/**
 * Information about ANISOtime package.
 *
 * @author Kensuke Konishi
 * @version 0.0.5.1
 * @see <a href=http://www-solid.eps.s.u-tokyo.ac.jp/~dsm/anisotime.htm>web</a>
 */
final class About extends javax.swing.JFrame {

    /**
     * 2020/7/27
     */
    private static final long serialVersionUID = -5905860950780042944L;
    private static final String line = "ANISOtime " + ANISOtime.VERSION + " (" + ANISOtime.CODENAME +
            ")\nCopyright \u00a9 2015-2020 Kensuke Konishi, Anselme F.E. Borgeaud, Kenji Kawai and Robert J. Geller.\n\n" +
            "This software is licensed under the GNU General Public License Version 3, 29 June 2007 (https://www.gnu.org/licenses/).\n";
    private JScrollPane jScrollPane1;

    private About() {
        super("About ANISOtime");
        initComponents();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            for (LookAndFeelInfo info : getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) {
                    setLookAndFeel(info.getClassName());
                    break;
                }
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(About.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!GraphicsEnvironment.isHeadless()) SwingUtilities.invokeLater(() -> new About().setVisible(true));
        else System.out.println(line);
    }

    private void initComponents() {
        jScrollPane1 = new JScrollPane();
        JTextArea jTextArea1 = new JTextArea(line, 5, 20);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        jScrollPane1.setViewportView(jTextArea1);
        jTextArea1.setLineWrap(true);
        jTextArea1.setEditable(false);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap()
                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap()
                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE).addContainerGap()));
        pack();
        SwingUtilities.invokeLater(() -> jScrollPane1.getVerticalScrollBar().setValue(0));
    }
}
