package io.github.kensuke1984.anisotime;

import java.awt.GraphicsEnvironment;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.commons.cli.UnrecognizedOptionException;

/**
 * ANISOtime launcher.
 * TODO discontinuity
 *
 * @author Kensuke Konishi
 * @version {@value #version} {@value #codename}
 */
final class ANISOtime {

    static final String codename = "Tavnazia";

    static final String version = "1.1.2b";

    private ANISOtime() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        if (args.length != 0) try {
            ANISOtimeCLI.main(args);
            return;
        } catch (UnrecognizedOptionException e) {
            System.err.println("The command line arguments are invalid.");
            ANISOtimeCLI.printHelp();
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        else if (GraphicsEnvironment.isHeadless()) {
            System.err.println("No graphical environment.. please use CLI.");
            ANISOtimeCLI.printHelp();
            return;
        }

        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(ANISOtime.class.getName()).log(Level.SEVERE, null, ex);
        }
        SwingUtilities.invokeLater(() -> new ANISOtimeGUI().setVisible(true));
    }

}
