package io.github.kensuke1984.kibrary.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

/**
 * This class is based on a class provided as a Sample by Oracle. The copyright
 * is stated in the Kibrary License statement.
 *
 * @author Kensuke Konishi
 * @version 0.0.2
 */
class PasswordInput extends JPanel implements ActionListener {
    /**
     * 2018/2/1
     */
    private static final long serialVersionUID = -5714596316649848840L;
    private static String OK = "ok";
    private static String HELP = "help";

    private JFrame controllingFrame; // needed for dialogs
    private JPasswordField passwordField;
    private volatile String password;

    private PasswordInput(JFrame f, String phrase) {
        // Use the default FlowLayout.
        controllingFrame = f;

        // Create everything.
        passwordField = new JPasswordField(10);
        passwordField.setActionCommand(OK);
        passwordField.addActionListener(this);

        JLabel label = new JLabel(phrase + ": ");
        label.setLabelFor(passwordField);

        JComponent buttonPane = createButtonPanel();

        // Lay out everything.
        JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        textPane.add(label);
        textPane.add(passwordField);

        add(textPane);
        add(buttonPane);
    }

    /*
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event dispatch thread.
     */
    private static PasswordInput createAndShowGUI(String phrase) {
        // Create and set up the window.
        JFrame frame = new JFrame();
//        frame.setUndecorated(true);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Create and set up the content pane.
        PasswordInput newContentPane = new PasswordInput(frame, phrase);
        newContentPane.setOpaque(true); // content panes must be opaque
        frame.setContentPane(newContentPane);

        // Make sure the focus goes to the right component
        // whenever the frame is initially given the focus.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                newContentPane.resetFocus();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                System.err.println("OK. The secret is secret. But I guess you must move forward sometime.");
                newContentPane.password = "";
            }
        });

        // Display the window.
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
        return newContentPane;
    }

    private JComponent createButtonPanel() {
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

    public String getPassword() throws InterruptedException {
        while (password == null) Thread.sleep(10);
        return password;
    }

    public static String getPassword(String phrase) throws InterruptedException {
        return createAndShowGUI(phrase).getPassword();
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
    private void resetFocus() {
        passwordField.requestFocusInWindow();
    }

}
