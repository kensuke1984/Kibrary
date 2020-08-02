package io.github.kensuke1984.anisotime;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauPException;
import io.github.kensuke1984.kibrary.external.TauPPhase;
import io.github.kensuke1984.kibrary.external.TauP_Time;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class RaypathTest {
    private RaypathTest() {
    }


    static final VelocityStructure prem = VelocityStructure.prem();

    // compare ANISOtime value with TauP time's  (aniso-taup)
    private static double compareTauP(double eventR, Phase phase, Raypath raypath) {
        double anisoDelta = raypath.computeDelta(phase, eventR);
        if (Double.isNaN(anisoDelta)) return Double.NaN;
        Set<Phase> phaseSet = new HashSet<>();
        phaseSet.add(phase);
        try {
            Set<TauPPhase> tauPPhase = TauP_Time.
                    getTauPPhase(eventR, Math.toDegrees(anisoDelta), phaseSet);
            if (tauPPhase.isEmpty()) return Double.NaN;
            double anisoTime = raypath.computeT(phase, eventR);
            double[] tauptimes = tauPPhase.stream().mapToDouble(TauPPhase::getTravelTime).toArray();
            double difference = anisoTime;
            edu.sc.seis.TauP.TauP_Time timeTool = new edu.sc.seis.TauP.TauP_Time(
                    "/mnt/ntfs/kensuke/workspace/kibrary/anisotime/aniso_taup_comp/PREM_1000.taup");
            timeTool.parsePhaseList(phase.toString());
            timeTool.calculate(Math.toDegrees(anisoDelta));
            List<Arrival> tlist = timeTool.getArrivals();
            for (Arrival arrival : tlist) {
                double taupt = arrival.getTime();
//                difference = Math.min(Math.abs(anisoTime - taupt), difference);
            }

            for (double tauptime : tauptimes) {
                difference = Math.min(Math.abs(anisoTime - tauptime), difference);
//                System.out.println(tauptime+" "+anisoTime);
            }


//            if (difference < 0.08) return 0;
//            System.out.println(difference);
            System.out.println(Math.toDegrees(anisoDelta) + " " + difference + " " + raypath.computeT(phase, eventR));

            return difference;

        } catch (Exception e) {
            return Double.NaN;
        }

    }

    private static void compareTauP_P() {

        double rayp3481 = 3481 / iprem.computeVph(3481);
        double rayp6370 = 6370 / iprem.computeVph(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = rayp3481; rayp < rayp6370; rayp += 1) {
            Raypath raypath = new Raypath(rayp, iprem);
            compareTauP(6371, Phase.P, raypath);
        }
    }

    private static void compareTauP_PKIKP() {

        double rayp3481 = 3481 / iprem.computeVph(3481);
        double rayp6370 = 6370 / iprem.computeVph(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = 0; rayp < rayp3481; rayp += 1) {
            Raypath raypath = new Raypath(rayp, iprem);
            compareTauP(6371, Phase.PKIKP, raypath);
        }


    }

    private static void compareTauP_P1() {

        NamedDiscontinuityStructure premND = NamedDiscontinuityStructure.prem();
        double rayp3481 = 3481 / premND.computeVph(3481);
        double rayp6370 = 6370 / premND.computeVph(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = rayp3481; rayp < rayp6370; rayp += 1) {
            Raypath raypath = new Raypath(rayp, premND);
            compareTauP(6371, Phase.P, raypath);
        }
    }

    private static void compareTauP_S() {

        double rayp3481 = 3481 / iprem.computeVsh(3481);
        double rayp6370 = 6370 / iprem.computeVsh(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = rayp3481; rayp < rayp6370; rayp += 1) {
            Raypath raypath = new Raypath(rayp, iprem);
            compareTauP(6371, Phase.S, raypath);
        }
    }

    private static void compareTauP_S1() {
        NamedDiscontinuityStructure premND = NamedDiscontinuityStructure.prem();
        double rayp3481 = 3481 / premND.computeVsh(3481);
        double rayp6370 = 6370 / premND.computeVsh(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = rayp3481; rayp < rayp6370; rayp += 1) {
            Raypath raypath = new Raypath(rayp, premND);
            compareTauP(6371, Phase.S, raypath);
        }
    }

    private static void compareTauP_PcP() {

        double rayp3481 = 3481 / iprem.computeVph(3481);
        double rayp6370 = 6370 / iprem.computeVph(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = 0; rayp < rayp3481; rayp += 1) {
            Raypath raypath = new Raypath(rayp, iprem);
            compareTauP(6371, Phase.PcP, raypath);
        }
    }

    private static void compareTauP_ScS() {

        double rayp3481 = 3481 / iprem.computeVsh(3481);
        double rayp6370 = 6370 / iprem.computeVsh(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = 0; rayp < rayp3481; rayp += 1) {
            Raypath raypath = new Raypath(rayp, iprem);
            compareTauP(6371, Phase.ScS, raypath);
        }
    }

    private static void compareTauP_SKIKS() {

        double rayp3481 = 3481 / iprem.computeVsh(3481);
        double rayp6370 = 6370 / iprem.computeVsh(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = 0; rayp < rayp3481; rayp += 1) {
            Raypath raypath = new Raypath(rayp, iprem);
            compareTauP(6371, Phase.SKIKS, raypath);
        }
    }

    //start TODO
    private static void compareTauP_PKP() {

        double rayp3481 = 3481 / iprem.computeVph(3481);
        double rayp6370 = 6370 / iprem.computeVph(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = 0; rayp < rayp3481; rayp += 1) {
            Raypath raypath = new Raypath(rayp, iprem);
            compareTauP(6371, Phase.PKP, raypath);
        }
    }

    private static void compareTauP_SKS() {

        double rayp3481 = 3481 / iprem.computeVsv(3481);
        double rayp6370 = 6370 / iprem.computeVsv(6370);
//        System.out.println(rayp3481+" "+rayp6370);
        double deltaP = 1;

        for (double rayp = 0; rayp < rayp3481; rayp += 1) {
            Raypath raypath = new Raypath(rayp, iprem);
            compareTauP(6371, Phase.SKS, raypath);
        }
    }

    private static Woodhouse1981 ipremW = new Woodhouse1981(VelocityStructure.iprem());
    private static ComputationalMesh mesh = new ComputationalMesh(VelocityStructure.iprem(), 1, 1, 1);

    private static void compareTAUDELTA() throws TauPException, IOException, TauModelException {

        double rayP = 300;
//        double bounceR = 3480;
        List<String> linePKIKP = new ArrayList<>();
        List<String> lineSKIKS = new ArrayList<>();
        List<String> lineSKiKS = new ArrayList<>();
        List<String> lineScS = new ArrayList<>();
        List<String> lineSKS = new ArrayList<>();
        List<String> linePKiKP = new ArrayList<>();
        List<String> linePcP = new ArrayList<>();
        List<String> linePKP = new ArrayList<>();
        rayP = 180;
//        rayP = 3500/VelocityStructure.iprem().computeVph(3500);
//        System.out.println(rayP);

        while (true) {
//            bounceR+=1;
//            if (bounceR>4000)
            if (rayP > 220) break;
            rayP += 0.05;
            System.out.println("rayparameter " + rayP);
//            rayP = bounceR / VelocityStructure.iprem().computeVph(bounceR);
            double deltaP = 0.5;
            Raypath okRay1 = new Raypath(rayP, ipremW, mesh);
            if (3480 < okRay1.getTurningR(PhasePart.P)) continue;
            Raypath okRayM = new Raypath(rayP - deltaP, ipremW, mesh);
            Raypath okRayP = new Raypath(rayP + deltaP, ipremW, mesh);
//            raypath2Ratio(Phase.PcP,okRayM,okRay1,okRayP);
            if (okRay1.getTurningR(PhasePart.I) < 1220) {
                linePKIKP.add(raypath2Ratio2(Phase.PKIKP, okRayM, okRay1, okRayP));
                linePKiKP.add(raypath2Ratio2(Phase.PKiKP, okRayM, okRay1, okRayP));
            }
//            linePcP.add(raypath2Ratio(Phase.PcP, rayP, deltaP));
            linePcP.add(raypath2Ratio2(Phase.PcP, okRayM, okRay1, okRayP));
//            System.exit(0);
            if (1225 < okRay1.getTurningR(PhasePart.K)) linePKP.add(raypath2Ratio2(Phase.PKP, okRayM, okRay1, okRayP));
//            linePKP.add(raypath2Ratio(Phase.PKP, rayP, deltaP));
//            lineSKIKS.add(raypath2Ratio(Phase.SKIKS, rayP, deltaP));
//            lineScS.add(raypath2Ratio(Phase.ScS, rayP, deltaP));
            if (3480 < okRay1.getTurningR(PhasePart.SV)) continue;
            lineScS.add(raypath2Ratio2(Phase.ScS, okRayM, okRay1, okRayP));
//            lineSKiKS.add(raypath2Ratio(Phase.SKiKS, rayP, deltaP));
            if (1225 < okRay1.getTurningR(PhasePart.K)) lineSKS.add(raypath2Ratio2(Phase.SKS, okRayM, okRay1, okRayP));
            if (okRay1.getTurningR(PhasePart.I) < 1220) {
                lineSKiKS.add(raypath2Ratio2(Phase.SKiKS, okRayM, okRay1, okRayP));
                lineSKIKS.add(raypath2Ratio2(Phase.SKIKS, okRayM, okRay1, okRayP));
            }
//            lineSKS.add(raypath2Ratio(Phase.SKS, rayP, deltaP));
        }

    }


    private static String raypath2Ratio2(Phase phase, Raypath okRayM, Raypath okRay1, Raypath okRayP)
            throws TauPException, IOException, TauModelException {
        double bounceR = okRay1.getTurningR(PhasePart.K);
        double delta1 = Math.toDegrees(okRay1.computeDelta(phase, 6371));
        double time1 = okRay1.computeT(phase, 6371);
//            double t = raypath.computeT(p, 6371);0.9811088004940575 0.9860402275788196 0.9909716546635817 0.9860402153491439
        double dtaudpCenter = Raypath.computeDelta(phase, 6371, okRayM, okRay1, okRayP);
        dtaudpCenter = Math.toDegrees(dtaudpCenter);
        Set<Phase> tauPPhases = new HashSet<>();
        tauPPhases.add(phase);
        Set<TauPPhase> tauPPhase = TauP_Time.getTauPPhase(6371, dtaudpCenter, tauPPhases);
        double deltani = dtaudpCenter;
//        System.out.println(deltani + " " + okRay1.getTurningR(PhasePart.K));
        double time5 = Raypath.computeT(phase, 6371, okRayM, okRay1, okRayP);
        System.out.println(deltani + " a " + time5);
        TauPPhase next = tauPPhase.stream().filter(tp -> Math.abs(tp.getPuristDistance() - deltani) < 0.1)
                .sorted(Comparator.comparingDouble(tp -> Math.abs(tp.getTravelTime() - time5))).findFirst().get();
        double taupDelta = next.getPuristDistance();
        double taupT = next.getTravelTime();
        System.out.println("dtaudpBefore  dtaudpCenter  dtaudpAhead  deltabyOLD");
//        System.out.println(dtaudpBefore + " " + dtaudpCenter + " " + dtaudpAhead + " " + delta1);
        double deltaRatio = 100 * Math.abs(1 - dtaudpCenter / taupDelta);
        double tRatio = Math.abs(1 - time5 / taupT) * 100;
        System.out.println(
                dtaudpCenter + " " + delta1 + " " + dtaudpCenter / delta1 + " " + taupDelta + " " + deltaRatio + " " +
                        tRatio);

        return okRay1.getRayParameter() + " " + bounceR + " " + deltaRatio + " " + tRatio;
    }

    private static void recordSection() throws ParseException {
        String[] cmd = "-rs 30,40,0.1 -ph S -SV -mod prem -o /tmp/rs.txt".split("\\s+");
        ANISOtimeCLI.main(cmd);

    }


    private static void edgeCheck() {
        VelocityStructure structure = ipremW.getStructure();
        double icb = structure.innerCoreBoundary() - ComputationalMesh.EPS;
        double pkikpMax = icb / structure.computeVph(icb);
        // 348.5706287454159
        double skiksMax = icb / structure.computeVsv(icb);
        //431.5054502232356 117.95418336407785
        double pkpMin = (structure.innerCoreBoundary() + ComputationalMesh.EPS) /
                structure.computeVph(structure.innerCoreBoundary() + ComputationalMesh.EPS);
        double pkpMax = (structure.coreMantleBoundary() - ComputationalMesh.EPS) /
                structure.computeVph(structure.coreMantleBoundary() - ComputationalMesh.EPS);
        double pMin = (structure.coreMantleBoundary() + ComputationalMesh.EPS) /
                structure.computeVph(structure.coreMantleBoundary() + ComputationalMesh.EPS);
        double sksMin = (structure.innerCoreBoundary() + ComputationalMesh.EPS) /
                structure.computeVph(structure.innerCoreBoundary() + ComputationalMesh.EPS);
        double sksMax = (structure.coreMantleBoundary() - ComputationalMesh.EPS) /
                structure.computeVph(structure.coreMantleBoundary() - ComputationalMesh.EPS);
        double sMin = (structure.coreMantleBoundary() + ComputationalMesh.EPS) /
                structure.computeVph(structure.coreMantleBoundary() + ComputationalMesh.EPS);
        System.out.println(sksMax + " " + sksMin + " " + skiksMax);
    }


    static final VelocityStructure iprem = VelocityStructure.iprem();

    private static void write(Raypath ray, Path path, OpenOption... options) throws IOException {
        try (ObjectOutputStream o = new ObjectOutputStream(Files.newOutputStream(path, options))) {
            o.writeObject(ray);
        }
    }

    private static Raypath read(Path path, OpenOption... options) throws IOException, ClassNotFoundException {
        try (ObjectInputStream oi = new ObjectInputStream(Files.newInputStream(path, options))) {
            return (Raypath) oi.readObject();
        }
    }

    public static void main(String[] args)
            throws IOException, ParseException, TauModelException, TauPException, ClassNotFoundException {
//        compareTauP();
        debugPKJKP();
    }


    private static void debugPKJKP() throws IOException {
//        Raypath good = new Raypath(45.29);
        Raypath good = new Raypath(68.29);
        Raypath bad = new Raypath(79.29);
        Phase pkjkp = Phase.create("PKJKP");
        double eventR = 6371;
        double goodtheta = good.computeDelta(pkjkp, eventR);
        double badtheta = bad.computeDelta(pkjkp, eventR);
        System.out.println(Math.toDegrees(goodtheta));
        System.out.println(Math.toDegrees(badtheta));
//        bad.debug();
//        System.exit(0);
        double[][] badXY = bad.getRoute(pkjkp, eventR);
//        System.exit(0);
        double[][] goodXY = good.getRoute(pkjkp, eventR);
        System.out.println(goodXY.length + " " + badXY.length);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 12000; i++) {
            double goodX = goodXY[i][0];
            double goodY = goodXY[i][1];
            double badX = badXY[i][0];
            double badY = badXY[i][1];
            lines.add(goodX + " " + goodY + " " + badX + " " + badY);
        }


        Files.write(Paths.get("/tmp/paths.txt"), lines);
    }

    private static double computeTanalyticalHomogenSH(double rayparameter, double rmin, double rmax) {
        PolynomialStructure homogen = PolynomialStructure.HOMOGEN;
        double r0 = 5000.;
        double rho0 = homogen.getRho(r0);
        double L0 = homogen.getL(1);
        double N0 = homogen.getN(1);
        double qtau0 = Math.sqrt(rho0 / L0 - N0 / L0 * rayparameter * rayparameter);
        double qt0 = rho0 / (qtau0 * L0);

        return qt0 * Math.log(rmax / rmin);
    }

    public static double computeTanalyticalHomogenSV(double rayparameter, double rmin, double rmax) {
        PolynomialStructure homogen = PolynomialStructure.HOMOGEN;
        double r0 = 5000.;

        double rho0 = homogen.getRho(r0);
        double L0 = homogen.getL(1);
        double A0 = homogen.getA(1);
        double C0 = homogen.getC(1);
        double F0 = homogen.getF(1);

        double s10 = rho0 / 2. * (1. / L0 + 1. / C0);
        double s20 = rho0 / 2. * (1. / L0 - 1. / C0);
        double s30 = 1. / (2 * L0 * C0) * (A0 * C0 - F0 * F0 - 2 * L0 * F0);
        double s40 = s30 * s30 - A0 / C0;
        double s50 = rho0 / (2 * C0) * (1 + A0 / L0) - s10 * s30;
        double R0 = Math.sqrt(s40 * Math.pow(rayparameter, 4) + 2 * s50 * Math.pow(rayparameter, 2) + s20 * s20);

        double qtau0 = Math.sqrt(s10 - s30 * rayparameter * rayparameter + R0);
        double qt0 = 1. / qtau0 * (s10 + (s50 * rayparameter * rayparameter + s20 * s20) / R0);

        return qt0 * Math.log(rmax / rmin);
    }

    public static double computeTanalyticalHomogenP(double rayparameter, double rmin, double rmax) {
        PolynomialStructure homogen = PolynomialStructure.HOMOGEN;
        double r0 = 5000.;

        double rho0 = homogen.getRho(r0);
        double L0 = homogen.getL(1);
        double A0 = homogen.getA(1);
        double C0 = homogen.getC(1);
        double F0 = homogen.getF(1);

        double s10 = rho0 / 2. * (1. / L0 + 1. / C0);
        double s20 = rho0 / 2. * (1. / L0 - 1. / C0);
        double s30 = 1. / (2 * L0 * C0) * (A0 * C0 - F0 * F0 - 2 * L0 * F0);
        double s40 = s30 * s30 - A0 / C0;
        double s50 = rho0 / (2 * C0) * (1 + A0 / L0) - s10 * s30;
        double R0 = Math.sqrt(s40 * Math.pow(rayparameter, 4) + 2 * s50 * Math.pow(rayparameter, 2) + s20 * s20);

        double qtau0 = Math.sqrt(s10 - s30 * rayparameter * rayparameter - R0);
        double qt0 = 1. / qtau0 * (s10 - (s50 * rayparameter * rayparameter + s20 * s20) / R0);

        return qt0 * Math.log(rmax / rmin);
    }
}
