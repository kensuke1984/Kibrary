package io.github.kensuke1984.anisotime;

import org.apache.commons.cli.ParseException;

import java.io.*;

/**
 * @author Kensuke Konishi
 * @version 0.0.2
 */
class RaypathCatalogTest {
    private static void createCheck() throws IOException, ParseException {
        RaypathCatalog prem = RaypathCatalog.prem();
        for (Raypath raypath : prem.getRaypaths()) {
            double eventR = 6371-100;
//            System.out.println(raypath.getRayParameter()+" "+Math.toDegrees(raypath.computeDelta(Phase.P,eventR))+" "+
//                    raypath.computeT(Phase.P,eventR));

        }
        System.out.println( (prem.searchPath(Phase.P, 6371, Math.toRadians(50), false)[0].computeT(Phase.P, 6371)));
//        System.exit(0);
        //261.6192175422948 94.01490821552146 798.1703394543351
        // 337.7542692276028 73.18767642655352 691.0307993093318
//478.85555933255523 39.05726961249951 431.16559569122245
        RaypathCatalog isoPrem = RaypathCatalog.iprem();
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
