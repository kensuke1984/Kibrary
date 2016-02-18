/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.util.Arrays;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Set of several GUI input components used in ANISOtime.
 * 
 * @version 0.0.2.1
 * @author Kensuke Konishi
 * 
 */
final class GUIInputComponents {

	/**
	 * @return {@link JTextField} only accepting positive double
	 */
	static JTextField createPositiveNumberField(String text) {
		JTextField field = new JTextField(new NumberDocument(), text, 0);
		field.setInputVerifier(positiveInputVerifier);
		field.setHorizontalAlignment(SwingConstants.CENTER);
		return field;
	}

	/**
	 * @return {@link JTextField} only accepting positive double
	 */
	static JTextField createPhaseField() {
		JTextField field = new JTextField();
		field.setInputVerifier(phaseVerifier);
		field.setHorizontalAlignment(SwingConstants.CENTER);
		return field;
	}

	/**
	 * verifier for doubles check if its positive
	 */
	private static InputVerifier positiveInputVerifier = new InputVerifier() {
		@Override
		public boolean verify(JComponent input) {
			try {
				JTextField textField = (JTextField) input;
				return 0 <= Double.parseDouble(textField.getText());
			} catch (Exception e) {
				return false;
			}
		}
	};

	private static InputVerifier phaseVerifier = new InputVerifier() {
		@Override
		public boolean verify(JComponent input) {
			JTextField field = (JTextField) input;
			String inputString = field.getText();
			if (inputString.length() == 0)
				return true;
			String[] phaseNames = inputString.trim().split(",");
			return Arrays.stream(phaseNames).map(String::trim).allMatch(Phase::isValid);
		}
	};

}
