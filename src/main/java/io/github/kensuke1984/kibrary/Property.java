package io.github.kensuke1984.kibrary;

import java.io.IOException;

import io.github.kensuke1984.kibrary.dsminformation.SshDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.dsminformation.SyntheticDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.selection.FilterDivider;
import io.github.kensuke1984.kibrary.timewindow.TimewindowMaker;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.spc.SpcSAC;

/**
 * 
 * This class will create a default property for a procedure in Kibrary.
 * 
 * 
 * @author kensuke
 * @version 0.0.1.1
 * 
 */
class Property {


	public static void main(String[] args) throws IOException {
		Manhattan.printList();
		System.out.print(
				"For which one do you want to create a property file? [0-" + (Manhattan.values().length - 1) + "]");
		output(Manhattan.valueOf(Integer.parseInt(Utilities.readInputLine())));
		return;
	}

	
	private static void output(Manhattan manhattan) throws IOException {
		switch (manhattan) {
		case SpcSAC:
			SpcSAC.writeDefaultPropertiesFile();
			break;
		case TimewindowMaker:
			TimewindowMaker.writeDefaultPropertiesFile();
			break;
		case FilterDivider:
			FilterDivider.writeDefaultPropertiesFile();
			break;
		case SyntheticDSMInformationFileMaker:
			SyntheticDSMInformationFileMaker.writeDefaultPropertiesFile();
			break;
		case SshDSMInformationFileMaker:
			SshDSMInformationFileMaker.writeDefaultPropertiesFile();
			break;
		default:
			break;
		}
	}

}