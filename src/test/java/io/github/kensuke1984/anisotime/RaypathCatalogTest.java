package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.cli.ParseException;

import java.io.IOException;

/**
 * @author Kensuke Konishi
 * @version 0.0.2
 */
class RaypathCatalogTest {
    private static void createCheck() throws IOException, ParseException {
        RaypathCatalog prem = RaypathCatalog.iprem();
        for (Raypath raypath : prem.getRaypaths()) {
//            double eventR = 6371 - 100;
//            System.out.println(raypath.getRayParameter()+" "+Math.toDegrees(raypath.computeDelta(Phase.P,eventR))+" "+
//                    raypath.computeT(Phase.P,eventR));

        }
        double eventR = 6371 - 10;
        Phase phase = Phase.create("P");
        Phase phase1 = Phase.create("Pv220P");
        Phase phase2 = Phase.create("Pv400P");
        Phase phase3 = Phase.create("Pv670P");
        long t = System.nanoTime();
        Raypath[] candidates = prem.searchPath(phase, 6361, Math.toRadians(20), false);
        System.out.println(Utilities.toTimeString(System.nanoTime() - t));
        System.out.println(candidates.length);
        for (Raypath candidate : candidates) {
//            double d = candidate.computeDelta(phase, 6271);
//            if (!Double.isNaN(d))
            System.out.println("y");
            System.out.println(
                    Math.toDegrees(candidate.computeDelta(phase, eventR)) + " " + candidate.computeT(phase, eventR));
            System.out.println(
                    Math.toDegrees(candidate.computeDelta(phase1, eventR)) + " " + candidate.computeT(phase1, eventR));
            System.out.println(
                    Math.toDegrees(candidate.computeDelta(phase2, eventR)) + " " + candidate.computeT(phase2, eventR));
            System.out.println(
                    Math.toDegrees(candidate.computeDelta(phase3, eventR)) + " " + candidate.computeT(phase3, eventR));
        }
//        System.out.println((prem.searchPath(phase, 6371, Math.toRadians(20), false)[0].computeT(phase, 6371)));
//        System.exit(0);w
        //261.6192175422948 94.01490821552146 798.1703394543351
        // 337.7542692276028 73.18767642655352 691.0307993093318
//478.85555933255523 39.05726961249951 431.16559569122245
        RaypathCatalog isoPrem = RaypathCatalog.prem();
        RaypathCatalog ak135 = RaypathCatalog.ak135();
    }


    private RaypathCatalogTest() {
    }

    private static void kennetFig2() throws ParseException {
//        Raypath raypath = new Raypath(100)
        ANISOtimeCLI.main("-deg 50 -mod iprem".split("\\s+"));
    }

    public static void main(String[] args) throws IOException, ParseException {
        createCheck();
//        kennetFig2();
//        test1();
        System.exit(0);
    }

    private static void test1() throws ParseException {
double delta = Math.toRadians(108.5);
double r = 6371-50.2;
ANISOtimeCLI.main("-deg 108.5 -h 50.2 -mod iprem".split("\\s+"));
//RaypathCatalog.iprem().searchPath(Phase.S)
    }



}
