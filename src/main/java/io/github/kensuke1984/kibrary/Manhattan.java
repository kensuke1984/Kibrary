package io.github.kensuke1984.kibrary;

import java.util.Arrays;

/**
 * 
 * The list of names of manhattan (operation)
 * 
 * @author kensuke
 * @version 0.0.1
 */
public enum Manhattan {
	SpcSAC(0), FilterDivider(1), TimewindowMaker(2), SyntheticDSMInformationFileMaker(3),;

	private int value;

	private Manhattan(int n) {
		value = n;
	}

	static Manhattan valueOf(int n) {
		return Arrays.stream(values()).filter(m -> m.value == n).findAny().get();
	}

	public static void printList() {
		for(int i=0;i<values().length;i++)
			System.out.println(i+": "+valueOf(i));
	}

	private void createDefaultPropertiesFile() {
		switch (this) {
		case FilterDivider:

			break;
		case SpcSAC:
			break;
		case SyntheticDSMInformationFileMaker:
			break;
		case TimewindowMaker:
			break;
		default:
			break;

		}
	}
}
