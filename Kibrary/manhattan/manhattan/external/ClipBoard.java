package manhattan.external;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

public class ClipBoard {

	public static void main(String[] args) {

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		try {
			System.out.println(clipboard.getContents(null).getTransferData(
					DataFlavor.stringFlavor));
		} catch (Exception e) {
		}

	}

}
