package io.github.kensuke1984.anisotime;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauPException;
import io.github.kensuke1984.kibrary.external.TauPPhase;
import io.github.kensuke1984.kibrary.external.TauP_Time;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kensuke Konishi
 * @version 0.0.3
 */
class RaypathCatalogTest {
    private static void readCheck() {
        RaypathCatalog prem = RaypathCatalog.prem();
        RaypathCatalog isoPrem = RaypathCatalog.iprem();
        RaypathCatalog ak135 = RaypathCatalog.ak135();
    }

    private static void createCheck() {
        try {
            RaypathCatalog.main("prem".split(" "));
            RaypathCatalog.main("iprem".split(" "));
            RaypathCatalog.main("ak135".split(" "));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private RaypathCatalogTest() {
    }


    private static void checkPhases() throws TauPException, IOException, TauModelException {
        double[] eventRs = {6371, 6271, 6171, 6071, 5971, 5871, 5771, 5671};
        double[] deltas = {5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180};
        Phase[] phases =
                {Phase.p, Phase.s, Phase.pP, Phase.sS, Phase.P, Phase.S, Phase.create("PS"), Phase.create("SP"),
                        Phase.create("PP"), Phase.create("SS"), Phase.PcP, Phase.ScS, Phase.PKP, Phase.create("PKKP"),
                        Phase.SKS, Phase.create("SKKS"), Phase.PKiKP, Phase.SKiKS, Phase.PKIKP, Phase.SKIKS,};
        for (Phase phase : phases) {
            System.out.println("\r" + phase);
            for (double eventR : eventRs) {
                System.out.print("\r" + eventR);
                for (double delta : deltas) {
                    System.out.print(" " + delta);
                    ipremCheck(phase, eventR, delta);
                }
            }
        }
    }

    private static void ipremCheck(Phase phase, double eventR, double delta)
            throws TauPException, IOException, TauModelException {
        Raypath[] raypaths = inIPREM(phase, eventR, delta);
        Set<TauPPhase> taup = taup(phase, eventR, delta);
        taup.removeIf(t -> t.getPuristDistance() != delta);
        if (raypaths.length != taup.size())
            System.err.println("Different number of paths: " + phase + " " + eventR + " " + delta);
    }


    // TODO PKKKKP PCPSCS PS PPPP
    private static void extremeCheck() throws TauPException, IOException, TauModelException {
        Phase phase = Phase.create("pP");
        double eventR = 6371;
        double delta = 50;
        ipremCheck(phase, eventR, delta);
    }

    /**
     * @param phase
     * @param eventR
     * @param delta  [deg]
     */
    private static void showDeltaTime(Raypath raypath, Phase phase, double eventR, double delta) {
        Phase p = RaypathCatalog.getActualTargetPhase(raypath, phase, eventR, Math.toRadians(delta), false);
        double d = Math.toDegrees(raypath.computeDelta(p, eventR));
        double rayP = raypath.getRayParameter() / 180 * Math.PI;
        double t = raypath.computeT(p, eventR);
        System.out.println(p + " " + rayP + " " + d + " " + t);
    }


    /**
     * @param phase  target phase
     * @param eventR [km]
     * @param delta  [deg]
     * @return raypaths in the iprem catalog
     */
    private static Raypath[] inIPREM(Phase phase, double eventR, double delta) {
//        RaypathCatalog.iprem().debugSearch(phase, eventR, Math.toRadians(delta), false);
//        System.exit(0);
        return RaypathCatalog.iprem().searchPath(phase, eventR, Math.toRadians(delta), false);
    }

    /**
     * @param phase  target phase
     * @param eventR [km]
     * @param delta  [deg]
     * @return raypaths in the prem catalog
     */
    private static Raypath[] inPREM(Phase phase, double eventR, double delta) {
        return RaypathCatalog.prem().searchPath(phase, eventR, Math.toRadians(delta), false);
    }

    /**
     * @param phase  target phase
     * @param eventR [km]
     * @param delta  [deg]
     * @return raypaths in the ak135 catalog
     */
    private static Raypath[] inAK135(Phase phase, double eventR, double delta) {
        return RaypathCatalog.ak135().searchPath(phase, eventR, Math.toRadians(delta), false);
    }

    private static Set<TauPPhase> taup(Phase phase, double eventR, double epicentralDistance)
            throws TauPException, IOException, TauModelException {
        return TauP_Time.getTauPPhase(eventR, epicentralDistance, new HashSet<>(Arrays.asList(phase)));
    }

    private static Set<TauPPhase> taup(double eventR, double epicentralDistance, Set<Phase> phaseSet)
            throws TauPException, IOException, TauModelException {
        return TauP_Time.getTauPPhase(eventR, epicentralDistance, phaseSet);
    }

    private static void test() throws TauPException, IOException, TauModelException {
        double[] eventRs = {5671};
        double[] deltas = {20,};
        Phase[] phases = {Phase.create("SP"),};
        for (Phase phase : phases) {
            System.out.println("\r" + phase);
            for (double eventR : eventRs) {
                System.out.print("\r" + eventR);
                for (double delta : deltas) {
                    System.out.print(" " + delta);
                    ipremCheck(phase, eventR, delta);
                }
            }
        }
        System.exit(0);
    }

    private static void debugPKJKP() {
        RaypathCatalog iprem = RaypathCatalog.iprem();
        Phase pkjkp = Phase.create("PKJKP");
        double delta = Math.toRadians(170);
    }


    public static void main(String[] args) throws TauPException, IOException, TauModelException {
//        debugPKJKP();
//        readCheck();
        checkPhases();
    }

    //TODO K J I
    private static String addDepth(String name, double[] pvRadii, double[] svRadii) {
        if (name.contains("v") || name.contains("c") || name.contains("K"))
            throw new RuntimeException("UNIKUSPEKUTEDDO");
        int ip = 0;
        int is = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == 'P') {
                double r = pvRadii[ip++];
                if (r == 6371) sb.append("P");
                else sb.append("Pv").append(r).append("P");
            }
            if (c == 'S') {
                double r = svRadii[is++];
                if (r == 6371) sb.append("S");
                else sb.append("Sv").append(r).append("S");
            }
        }
        return sb.toString();
    }


}
