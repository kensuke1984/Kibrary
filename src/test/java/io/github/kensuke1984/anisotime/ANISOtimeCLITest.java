package io.github.kensuke1984.anisotime;

import org.apache.commons.cli.ParseException;

/**
 * @author Kensuke Konishi
 * @author anselme
 * @version 0.0.1
 */
class ANISOtimeCLITest {
    private ANISOtimeCLITest(){}

    public static void main(String[] args) {
    	testUserManual();
//    	testNonDefaultPhases();
    }
    
    public static void testUserManual() {
    	String line = "-u";
    	try {
    		ANISOtimeCLI.main(line.split("\\s+"));
    	} catch(ParseException e) {
    		e.printStackTrace();
    	}
    }
    
    public static void testNonDefaultPhases() {
    	String line = "-mod prem -ph ScP -deg 30";
    	try {
    		ANISOtimeCLI.main(line.split("\\s+"));
    	} catch(ParseException e) {
    		e.printStackTrace();
    	}
    }
}
