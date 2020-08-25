package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kensuke Konishi
 * @version 0.1.3
 */
class EULA extends JDialog {

    private EULA(Frame parent, boolean modal) {
        super(parent, "End user license agreement", modal);
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        eulaScroll = new JScrollPane();
        eulaArea = new JTextArea(EULA, 5, 20);
        jPanel1 = new JPanel();
        ButtonGroup acceptDecline = new ButtonGroup();
        acceptButton = new JRadioButton("Accept");
        declineButton = new JRadioButton("Decline");
        acceptDecline.add(acceptButton);
        acceptDecline.add(declineButton);
        okButton = new JToggleButton("OK");
        declineButton.setSelected(true);

        eulaArea.setLineWrap(true);
        eulaArea.setEditable(false);
        eulaArea.setCaretPosition(0);
        eulaScroll.setViewportView(eulaArea);

        okButton.addActionListener(e -> {
            if (declineButton.isSelected()) System.exit(71);
            try {
                Files.createFile(ANISOtime.AGREEMENT_PATH);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            setVisible(false);
            dispose();
        });

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1Layout.setAutoCreateGaps(true);
        jPanel1Layout.setAutoCreateContainerGaps(true);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(acceptButton)
                                        .addComponent(declineButton)
                                        .addComponent(okButton))
                                .addGap(156, 156, 156))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(acceptButton)
                                .addComponent(declineButton)
                                .addComponent(okButton)
                                .addGap(24, 24, 24))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(eulaScroll, GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE)
                                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        )
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(eulaScroll, GroupLayout.PREFERRED_SIZE, 206, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
        );
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(io.github.kensuke1984.anisotime.EULA.class.getName()).log(Level.SEVERE, null, ex);
        }

        EventQueue.invokeLater(() -> {
            io.github.kensuke1984.anisotime.EULA dialog = new EULA(new JFrame(), true);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            dialog.setVisible(true);
        });
    }

    private JRadioButton acceptButton;
    private JRadioButton declineButton;
    private JPanel jPanel1;
    private JScrollPane eulaScroll;
    private JTextArea eulaArea;
    private JToggleButton okButton;

    static boolean isAccepted() {
        if (Files.exists(ANISOtime.AGREEMENT_PATH)) return true;
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(EULA);
            System.err.println("If you accept the terms of the agreement, type \"yes\". (yes/No)");
            try {
                String input = Utilities.readInputLine();
                if (input.equals("yes") || input.equals("Yes"))
                    return true;
                System.exit(71);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(88);
        }
        EULA e = new EULA(new JFrame(), true);
        e.setVisible(true);
        return Files.exists(ANISOtime.AGREEMENT_PATH);
    }

    private static final String EULA =
            "This software (ANISOtime) is made available at no cost" +
                    " under the GNU General Public License Version 3, 29 June 2007 " +
                    "(https://www.gnu.org/licenses/)." +
                    " The GNU General Public License, gives you specific rights to use," +
                    " modify and redistribute this software." +
                    " Please be aware of section 2 of the license which specifically prevents you" +
                    " from redistributing this software incorporated, in whole or on part, into to a work," +
                    " unless that work is also covered by the GNU General Public License." +
                    " Please see the Free Software Foundation's web site for more information.\n\n" +
                    "Disclaimer: Portions of this software are copyrighted by the \"The Developers\"" +
                    " (Kensuke Konishi, Anselme F.E. Borgeaud, Kenji Kawai and Robert J. Geller, and their associates), " +
                    "and portions are also copyrighted by Sun Microsystems, Inc., Oracle Corporation, and other parties.\n\n" +
                    "The Developers have made their best efforts to produce error-free software." +
                    " If possible, the Developers will respond to questions and error reports." +
                    " HOWEVER, IN NO EVENT SHALL THE DEVELOPERS BE LIABLE TO ANY PARTY" +
                    " FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES" +
                    " ARISING OUT OF THE USE OF THIS SOFTWARE, ITS DOCUMENTATION, OR ANY DERIVATIVES THEREOF," +
                    " EVEN IF THE DEVELOPERS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE." +
                    " THE DEVELOPERS DO NOT PROMISE TO RESPOND TO QUESTIONS AND/OR ERROR REPORTS," +
                    " AND ACCEPT NO LIABILITY OR RESPONSIBILITY FOR FAILING TO DO SO.\n\n" +
                    "THE DEVELOPERS SPECIFICALLY DISCLAIM ANY WARRANTIES, INCLUDING," +
                    " BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY," +
                    " FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT." +
                    " THIS SOFTWARE IS PROVIDED ON AN \"AS IS\" BASIS," +
                    " AND THE DEVELOPERS HAVE NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS," +
                    " OR MODIFICATIONS.\n\n" +
                    "Acknowledgement: Portions of the wording of this license are modified" +
                    " from the license for the Tau-P Toolkit.\n";

}