package io.github.kensuke1984.kibrary;

import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.EnumUtils;

import io.github.kensuke1984.kibrary.dsminformation.SyntheticDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.selection.FilterDivider;
import io.github.kensuke1984.kibrary.timewindow.TimewindowMaker;
import io.github.kensuke1984.kibrary.util.spc.SpcSAC;

/**
 * 
 * This class will create a default property for a procedure in Kibrary.
 * 
 * 
 * @author kensuke
 * @version 0.0.1
 */
class Property {

	private static final Options options = new Options();

	public static void main(String[] args) throws IOException {
		if (args.length == 1 && args[0].equals("-l"))
			Manhattan.printList();
		else if (args.length == 2 && args[0].equals("-c") && EnumUtils.isValidEnum(Manhattan.class, args[1]))
			output(Manhattan.valueOf(args[1]));
		else
			new HelpFormatter().printHelp("PropertyMaker", options);
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
		}
	}

	static {
		options.addOption("l", "list the procedures in Kibrary");
		options.addOption("c", true,
				"creates a default property file for a procedure which name is <arg> (e.g. SpcSAC)");
	}

}
