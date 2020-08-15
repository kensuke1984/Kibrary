package io.github.kensuke1984.anisotime;


import io.github.kensuke1984.kibrary.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
public class EULA extends JDialog {
    private JPanel panel1;
    private JTextArea eulaArea;
    private JButton acceptButton;
    private JButton declineButton;

    private boolean acceptClicked;

    private EULA() {
        eulaArea.setText(EULA);
        setTitle("End User Licensing Agreement for ANISOtime");
        pack();
        setSize(500, 300);
        setLocationRelativeTo(null);
        setContentPane(panel1);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        acceptButton.addActionListener(e -> {
            try {
                Files.createFile(ANISOtime.AGREEMENT_PATH);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            acceptClicked = true;
            setVisible(false);
            dispose();
        });
        declineButton.addActionListener(e -> System.exit(71));
        setModal(true);
        setAlwaysOnTop(true);
        setVisible(true);
    }

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
        EULA e = new EULA();
        return e.acceptClicked;
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
