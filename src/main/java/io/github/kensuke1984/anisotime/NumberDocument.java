package io.github.kensuke1984.anisotime;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Document containing only numbers (double value) used in ANISOtime.
 *
 * @author Kensuke Konishi
 * @version 0.0.1.3
 */
final class NumberDocument extends PlainDocument {


    private static final long serialVersionUID = 5550233953139814272L;

    NumberDocument() {
    }

    private static void checkInput(String proposedValue, int offset) throws BadLocationException {
        if (!proposedValue.isEmpty()) {
            if (proposedValue.equals("+") || proposedValue.equals("-")) return;
            try {
                Double.parseDouble(proposedValue);
            } catch (NumberFormatException e) {
                throw new BadLocationException(proposedValue, offset);
            }
        }
    }

    @Override
    public void insertString(int offset, String str, AttributeSet attributes) throws BadLocationException {
        if (str == null) return;
        String newValue;
        int length = getLength();
        if (length == 0) newValue = str;
        else {
            String currentContent = getText(0, length);
            StringBuilder currentBuffer = new StringBuilder(currentContent);
            currentBuffer.insert(offset, str);
            newValue = currentBuffer.toString();
        }
        checkInput(newValue, offset);
        super.insertString(offset, str, attributes);
    }

    @Override
    public void remove(int offset, int length) throws BadLocationException {
        int currentLength = getLength();
        String currentContent = getText(0, currentLength);
        String before = currentContent.substring(0, offset);
        String after = currentContent.substring(length + offset, currentLength);
        String newValue = before + after;
        checkInput(newValue, offset);
        super.remove(offset, length);
    }

}
