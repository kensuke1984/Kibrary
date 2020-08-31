package io.github.kensuke1984.anisotime;

import org.apache.commons.cli.ParseException;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class ANISOtimeCLITest {
    private ANISOtimeCLITest(){}

    public static void main(String[] args) throws ParseException {
        String line = "-help -k -o";
        ANISOtimeCLI.main(line.split("\\s+"));
    }
}
