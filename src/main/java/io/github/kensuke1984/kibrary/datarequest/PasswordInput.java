package io.github.kensuke1984.kibrary.datarequest;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * This class is based on a class provided as a Sample by Oracle. The copyright
 * is stated in the Kibrary License statement.
 *
 * @author Kensuke Konishi
 * @version 0.0.1.1
 */
class PasswordInput extends JPanel implements ActionListener {

    private static final long serialVersionUID = 8903201119454585411L;
    private static String OK = "ok";
    private static String HELP = "help";

    private JFrame controllingFrame; // needed for dialogs
    private JPasswordField passwordField;

    PasswordInput(JFrame f) {
        // Use the default FlowLayout.
        controllingFrame = f;

        // Create everything.
        passwordField = new JPasswordField(10);
        passwordField.setActionCommand(OK);
        passwordField.addActionListener(this);

        JLabel label = new JLabel("datarequest.yuki@gmail.com: ");
        label.setLabelFor(passwordField);

        JComponent buttonPane = createButtonPanel();

        // Lay out everything.
        JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        textPane.add(label);
        textPane.add(passwordField);

        add(textPane);
        add(buttonPane);
    }

    protected JComponent createButtonPanel() {
        JPanel p = new JPanel(new GridLayout(0, 1));
        JButton okButton = new JButton("OK");
        JButton helpButton = new JButton("Help");

        okButton.setActionCommand(OK);
        helpButton.setActionCommand(HELP);
        okButton.addActionListener(this);
        helpButton.addActionListener(this);
        p.add(okButton);
        p.add(helpButton);
        return p;
    }

    private String password;

    public String getPassword() throws InterruptedException {
        while (password == null) Thread.sleep(10);
        return password;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (OK.equals(cmd)) { // Process the password.
            char[] input = passwordField.getPassword();
            password = String.copyValueOf(input);
            // Zero out the possible password, for security.
            Arrays.fill(input, '0');

            passwordField.selectAll();
            resetFocus();

            ((Window) SwingUtilities.getRoot(this)).dispose();
        } else { // The user has asked for help.
            JOptionPane.showMessageDialog(controllingFrame, "Recall Geller's group password, would you?");
        }
    }

    // Must be called from the event dispatch thread.
    protected void resetFocus() {
        passwordField.requestFocusInWindow();
    }

    /*
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event dispatch thread.
     */
    public static PasswordInput createAndShowGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("Password for the Gmail");
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Create and set up the content pane.
        PasswordInput newContentPane = new PasswordInput(frame);
        newContentPane.setOpaque(true); // content panes must be opaque
        frame.setContentPane(newContentPane);

        // Make sure the focus goes to the right component
        // whenever the frame is initially given the focus.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                newContentPane.resetFocus();
            }
        });

        // Display the window.
        frame.pack();
        frame.setVisible(true);
        return newContentPane;
    }

}
