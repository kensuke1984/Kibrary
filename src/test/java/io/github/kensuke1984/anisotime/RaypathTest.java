package io.github.kensuke1984.anisotime;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauPException;
import io.github.kensuke1984.kibrary.external.TauPPhase;
import io.github.kensuke1984.kibrary.external.TauP_Time;
import io.github.kensuke1984.kibrary.util.Utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class RaypathTest {
    private RaypathTest() {
    }

    private static void current() {
//        double p = 116.77464153043707+1.17;   117.95356393417516
        VelocityStructure structure = VelocityStructure.iprem();
//        Raypath raypath = new Raypath(p,structure);
//        raypath.compute();
        double icb = VelocityStructure.iprem().innerCoreBoundary();
        double loweMostOutercore = icb + ComputationalMesh.EPS;
        double pPKiKP = loweMostOutercore / structure.computeVph(loweMostOutercore);
        double startP = 0;
        double endP = pPKiKP;
        System.out.println(pPKiKP);
        double ap = 117.71784869495163;
        System.out.println(ap);
        Raypath a = new Raypath(ap, structure);
//        System.out.println(pPKiKP);
//        double delta = Math.toDegrees(raypath.computeDelta(Phase.PKiKP, 6371));
//        System.out.println(delta);
    }

    private static void checkTauP() {
//        Woodhouse1981 w = new Woodhouse1981(VelocityStructure.prem());
//        ComputationalMesh mesh = ComputationalMesh.simple(VelocityStructure.prem());
//        double pstart = 300;
//        double pend = 305;
//        double deltaP = 0.01;
//        Raypath r0 = new Raypath(pstart, w, mesh);
//        Raypath r1 = new Raypath(pstart + deltaP, w, mesh);
//        r0.compute();
//        r1.compute();
//        double tau0 = r0.computeTau();
//        double tau1 = r1.computeTau();
//        long t = System.nanoTime();
//        for (double rayP = pstart + 2 * deltaP; rayP < pend; rayP += deltaP) {
////302.88999999999737 153.76009141344358 81.4055331240347  4008.8120937050676 3 min and 0.799488915 s
//            //301.4299999999987 154.7994094968611 81.74065885603943  3994.462261074104 3 min and 0.473228064 s
//            //
//            /////Delta 82.00940094497804 1.4313340640780159
////        double rayP = 300;301.929999
//            Raypath raypath = new Raypath(rayP, w, mesh);
//            raypath.compute();
//            double delta = Math.toDegrees(raypath.computeDelta(Phase.P, 6371));
//            Phase p = Phase.P;
////            double t = raypath.computeT(p, 6371);
//            double tau = raypath.computeTau();
//            double dtaudpAhead = -(tau - tau1) / deltaP;
//            double dtaudpBefore = -(tau1 - tau0) / deltaP;
//            double dtaudpCenter = -(tau - tau0) / 2 / deltaP;
//            System.out.println((rayP - deltaP) + " " + 2 * Math.toDegrees(dtaudpBefore) + " " + delta + " " +
//                    Math.toDegrees(dtaudpCenter * 2) + " " + Utilities.toTimeString(System.nanoTime() - t));
//            r0 = r1;
//            tau0 = tau1;
//            tau1 = tau;
//            r1 = raypath;
////            System.out.println(delta + " " + Math.toRadians(delta));
//        }


    }

    private static void check3005() {
        Woodhouse1981 w = new Woodhouse1981(VelocityStructure.prem());
        ComputationalMesh mesh = ComputationalMesh.simple(VelocityStructure.prem());
        Raypath raypath = new Raypath(300.5, w, mesh);
//            raypath.compute();
//        raypath.computeTau();
    }

    private static void compareTAUDELTA() throws TauPException, IOException, TauModelException {
        Woodhouse1981 w = new Woodhouse1981(VelocityStructure.iprem());
        ComputationalMesh mesh = new ComputationalMesh(VelocityStructure.prem(), 1, 1, 1);

        double rayP = 300;
//        double bounceR = 3480;
        List<String> line = new ArrayList<>();
        rayP = 187;
//        rayP = 3500/VelocityStructure.iprem().computeVph(3500);
//        System.out.println(rayP);

        while(true) {
//            bounceR+=1;
//            if (bounceR>4000)
            if (rayP > 192.01) break;
            rayP += 0.001;
//            rayP = bounceR / VelocityStructure.iprem().computeVph(bounceR);
            double deltaP = 1;
            Raypath raypath0 = new Raypath(rayP - deltaP, w, mesh);
            Raypath raypath1 = new Raypath(rayP, w, mesh);
            Raypath raypath2 = new Raypath(rayP + deltaP, w, mesh);
            System.out.println("rayP, deltaP: " + rayP + " " + deltaP);
            raypath0.compute();
            raypath1.compute();
            raypath2.compute();
            Phase phase = Phase.PKP;
            double bounceR = raypath1.getTurningR(PhasePart.K);
//            PhasePart pp = PhasePart.P;
//            System.out.println(
//                    raypath0.getTurningR(pp) + " " + raypath1.getTurningR(pp) + " " + raypath2.getTurningR(pp));
            double delta0 = Math.toDegrees(raypath0.computeDelta(phase, 6371));
            double delta1 = Math.toDegrees(raypath1.computeDelta(phase, 6371));
            double time1 = raypath1.computeT(phase, 6371);
            double delta2 = Math.toDegrees(raypath2.computeDelta(phase, 6371));
//            double t = raypath.computeT(p, 6371);0.9811088004940575 0.9860402275788196 0.9909716546635817 0.9860402153491439
            PhasePart[] op = new PhasePart[]{PhasePart.P};
            PhasePart[] pki = new PhasePart[]{PhasePart.P, PhasePart.K, PhasePart.I};
            PhasePart[] pk = new PhasePart[]{PhasePart.P, PhasePart.K};

            double tau0 = Arrays.stream(pk).mapToDouble(raypath0::getTau).sum();
            double tau1 = Arrays.stream(pk).mapToDouble(raypath1::getTau).sum();
            double tau2 = Arrays.stream(pk).mapToDouble(raypath2::getTau).sum();
            System.out.println("tau " + tau0 + " " + tau1 + " " + tau2);
            double dtaudpAhead = -(tau2 - tau1) / deltaP;
            double dtaudpBefore = -(tau1 - tau0) / deltaP;
            double dtaudpCenter = -(tau2 - tau0) / 2 / deltaP;
            dtaudpAhead = 2 * Math.toDegrees(dtaudpAhead);
            dtaudpCenter = 2 * Math.toDegrees(dtaudpCenter);
            dtaudpBefore = 2 * Math.toDegrees(dtaudpBefore);
            Set<Phase> tauPPhases = new HashSet<>();
            tauPPhases.add(phase);
            Set<TauPPhase> tauPPhase = TauP_Time.getTauPPhase(6371, dtaudpCenter, tauPPhases);
            double deltani=dtaudpCenter;
            System.out.println(deltani+" "+raypath1.getTurningR(PhasePart.K));
            TauPPhase next =
                    tauPPhase.stream().filter(tp -> Math.abs(tp.getPuristDistance() - deltani) < 0.1).findAny()
                            .get();
            double taupDelta = next.getPuristDistance();
            double taupT = next.getTravelTime();
            System.out.println("dtaudpBefore  dtaudpCenter  dtaudpAhead  deltabyOLD");
//        System.out.println(dtaudpBefore + " " + dtaudpCenter + " " + dtaudpAhead + " " + delta1);
            double deltaRatio = 100 * Math.abs(1 - dtaudpCenter / taupDelta);
            System.out.println(
                    dtaudpCenter + " " + delta1 + " " + dtaudpCenter / delta1 + " " + taupDelta + " " + deltaRatio);
            double time5 = tau1 * 2 + (rayP) * Math.toRadians(dtaudpCenter);
            double tRatio = Math.abs(1 - time5 / taupT) * 100;

            System.out.println(time5 + " " + time1 + " " + time5 / time1 + " " + taupT + " " + tRatio+" "+tau0+" "+tau1+" "+tau2);
            line.add(rayP + " " + bounceR + " " + deltaRatio + " " + tRatio+ " "+tau0+" "+tau1+" "+tau2);
        }
        Files.write(Paths.get("/home/kensuke/workspace/kibrary/anisotime/taup/tmp.txt"), line);

    }

    public static void main(String[] args) throws IOException, TauModelException, TauPException {
//        current();
//        checkTauP();
        compareTAUDELTA();
//        check3005();
    }
}
