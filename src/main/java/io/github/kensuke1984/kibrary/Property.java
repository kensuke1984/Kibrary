package io.github.kensuke1984.kibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * 
 * This class will create a default property for a procedure in Kibrary.
 * 
 * 
 * @author Kensuke Konishi
 * @version 0.0.2.1
 * 
 */
public class Property {

	public static void main(String[] args) throws Exception {
		Manhattan.printList();
		System.out.print("For which one do you want to create a property file? [1-" + Manhattan.values().length + "]");
		Manhattan.valueOf(Integer.parseInt(Utilities.readInputLine())).writeDefaultPropertiesFile();
	}

	public static Properties parse(String[] args) throws IOException {
		Properties property = new Properties();
		if (args.length == 0)
			property.load(Files.newBufferedReader(Operation.findPath()));
		else if (args.length == 1)
			property.load(Files.newBufferedReader(Paths.get(args[0])));
		else
			throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");
		return property;
	}

}
