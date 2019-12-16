package io.github.kensuke1984.anisotime;

import org.apache.commons.cli.ParseException;

import java.io.*;

/**
 * @author Kensuke Konishi
 * @version 0.0.2
 */
class RaypathCatalogTest {
    private static void createCheck() throws IOException, ParseException {
//        RaypathCatalog prem = RaypathCatalog.prem();
//        RaypathCatalog isoPrem = RaypathCatalog.iprem();
        RaypathCatalog ak135 = RaypathCatalog.ak135();
    }



    private RaypathCatalogTest() {
    }

    private static void kennetFig2() {
//        Raypath raypath = new Raypath(100)
    }

    public static void main(String[] args) throws IOException, ParseException {
        createCheck();
        System.exit(0);
    }

}
