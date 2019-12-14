package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.linear.RealVector;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Kensuke Konishi
 * @version 0.0.1.1
 */
class RaypathCatalogTest {
    private static void catalogCheck() throws IOException, ParseException {
//        List<String> strings =
//                Files.readAllLines(Paths.get("/home/kensuke/workspace/kibrary/anisotime/until_PKIKP.txt"));
//
//        List<Raypath> collect =
//                strings.stream().mapToDouble(Double::parseDouble).mapToObj(Raypath::new).collect(Collectors.toList());
        RaypathCatalog prem = RaypathCatalog.prem();
        Raypath[] raypaths = prem.searchPath(Phase.P, 6371, Math.toRadians(30), false);
        for (Raypath raypath : raypaths) {
            System.out.println(Math.toDegrees(
                    raypath.computeDelta(6371,Phase.P)));
        }
        String line = "-deg 30 -mod prem -ph S";
        ANISOtimeCLI.main(line.split("\\s"));
        System.exit(0);
        RaypathCatalog isoPrem = RaypathCatalog.iprem();

        RaypathCatalog ak135 = RaypathCatalog.ak135();
//        System.out.println(isoPrem.getRaypaths().length);
//        System.out.println(prem.getRaypaths().length);
//        System.out.println(ak135.getRaypaths().length);
    }

    private static void diff() {
        RaypathCatalog catalog = RaypathCatalog.prem();
        Phase phase = Phase.create("Sdiff");
        Raypath[] raypaths = catalog.searchPath(phase, 6371 - 50.2, Math.toRadians(108.5), false);
        System.out.println(raypaths[0].computeDelta(6371 - 50.2, phase));
        System.out.println(raypaths[0].computeDelta(6371 - 50.2, phase.S));
    }

    private static void skiks() {
//        RaypathCatalog catalog = RaypathCatalog.PREM;
//        Raypath[] raypaths = catalog.searchPath(phase, 6371-50.2, Math.toRadians(50), false);
//        System.out.println(raypaths[0].computeDelta(6371-50.2,phase));
//110.73 1221.1732409447059
        double eventR = 6371;
        Raypath raypath = new Raypath(110.73, VelocityStructure.prem());
        raypath.compute();
        Phase skiks = Phase.SKIKS;
//        System.out.println("############################");
//        System.out.println(raypath.computeDelta(eventR,skiks));
//        System.out.println("############################");
//        raypath.getRoute(6371,skiks);
//        System.exit(0);
        double[][] pointsSKIKS = raypath.getRouteXY(6371, Phase.SKIKS);
        double[] xSKIKS = new double[pointsSKIKS.length];
        double[] ySKIKS = new double[pointsSKIKS.length];
        for (int i = 0; i < pointsSKIKS.length; i++) {
            xSKIKS[i] = pointsSKIKS[i][0];
            ySKIKS[i] = pointsSKIKS[i][1];
//                System.out.println(xSKIKS[i] + " " + ySKIKS[i]);
        }
//        System.out.println(raypath.getTurningR(PhasePart.I));
    }

    private static void diffBack() throws ParseException {
        String line = "-mod iprem -deg 150.09 -h 0";
        ANISOtimeCLI.main(line.split("\\s+"));
    }

    private static void doubleDiff() {
        RaypathCatalog catalog = RaypathCatalog.iprem();
        Phase targetPhase = Phase.PKiKP;
        double eventR = 6371;
        double targetDelta = Math.toRadians(10.09);

        Raypath[] raypaths = catalog.searchPath(targetPhase, eventR, targetDelta, false);
        Raypath ray1 = raypaths[0];
        Raypath ray2 = raypaths[1];

        System.out.println(ray1.getRayParameter() + " " + Math.toDegrees(ray1.computeDelta(6371, targetPhase)));
        System.out.println(ray2.getRayParameter() + " " + Math.toDegrees(ray2.computeDelta(6371, targetPhase)));

        System.out.println(raypaths.length);
    }

    private static void download() throws IOException {
        URL website = new URL("https://www.dropbox.com/s/l0w1abpfgn1ze38/piac.tar?dl=1");
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream("/tmp/yahoo.tar");
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }

    private static void extract() throws IOException {
        Path zipPath = Paths.get("/home/kensuke/secondDisk/workspace/anisotime/dev/piac.zip");
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
            ZipEntry entry;
            Path outRoot = Paths.get("/tmp");
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = outRoot.resolve(entry.getName());
                try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                        Files.newOutputStream(outPath))) {
                    byte[] buf = new byte[1024];
                    int size = 0;
                    while ((size = zis.read(buf)) != -1) bufferedOutputStream.write(buf, 0, size);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception occurred in reading a SCARDEC catalog.");
        }
    }

    private static void pkikip() throws ParseException {
        String line = "-mod iprem -ph PKIKP -deg 150";
//        ANISOtimeCLI.main(line.split("\\s+"));
        RaypathCatalog catalog = RaypathCatalog.iprem();
        Raypath[] raypaths = catalog.searchPath(Phase.PKIKP, 6371, Math.toRadians(150), false);
        for (Raypath raypath : raypaths) {
            System.out.println(
                    raypath.getRayParameter() + " " + Math.toDegrees(raypath.computeDelta(6371, Phase.PKIKP)) + " " +
                            raypath.computeT(6371, Phase.PKIKP));
        }
    }

    private RaypathCatalogTest() {
    }

    private static void singlePKIKP() {
        Raypath raypath1 = new Raypath(0.13468189275132417, VelocityStructure.prem());
        System.out.println("############");
        raypath1.compute();
        Phase phase = Phase.create("PKIKP");
        double delta = raypath1.computeDelta(6371, phase);
        System.out.println(Math.toDegrees(delta) + " " + raypath1.computeT(6371, phase));
        System.out.println("############");
        System.exit(0);

    }

    private static void manyPKIKP() {
        double pstart = 0;
        double pend = 1;
        double deltaP = 0.01;
        for (double p = pstart; p < pend; p += deltaP) {
            Raypath raypath1 = new Raypath(p, VelocityStructure.prem());
            System.out.println("############");
            raypath1.compute();
            Phase phase = Phase.create("PKIKP");
            double delta = raypath1.computeDelta(6371, phase);
            System.out.println(p + " " + Math.toDegrees(delta) + " " + raypath1.computeT(6371, phase));
            System.out.println("############");
        }
    }

    private static void singleP() {
        Raypath raypath1 = new Raypath(500, VelocityStructure.prem());
        System.out.println("############");
        raypath1.compute();
        Phase phase = Phase.create("P");
        double delta = raypath1.computeDelta(6371, phase);
        System.out.println(
                Math.toDegrees(delta) + " " + raypath1.computeT(6371, phase) + " " + raypath1.getTurningR(PhasePart.P));
        System.out.println("############");
    }

    private static void pTriple() {
        //target boundary

        double minP = 680;
        double maxP = 800;
        double dP = 1;
        Phase p_ = Phase.P;

        Phase p220 = Phase.create("Pv220P");
        for (double p = minP; p < maxP; p += dP) {
            Raypath raypath = new Raypath(p);
            raypath.compute();
            double delta = Math.toDegrees(raypath.computeDelta(6371, Phase.P));
            if (Double.isNaN(delta)) delta = Math.toDegrees(raypath.computeDelta(6371, p220));
            if (Double.isNaN(delta)) continue;
            System.out.println(p + " " + delta + " " + (6371 - raypath.getTurningR(PhasePart.P)));


        }


    }

    private static void showBoundaries() {
        VelocityStructure prem = VelocityStructure.prem();
        for (double v : prem.boundariesInMantle()) {
            System.out.println(v);
        }
    }

    private static void test1986() {
//        Raypath raypath = new Raypath(1986.2500000312498);
        Raypath raypath = new Raypath(1379.0292530838485);
        raypath.compute();
        System.out.println(raypath.getTurningR(PhasePart.SH));
        System.out.println(raypath.computeDelta(6371, Phase.create("Sv15S")));
    }

    private static void check220() {
        PolynomialStructure structure = PolynomialStructure.PREM;
//        RaypathCatalog catalog = RaypathCatalog.prem();


        Phase phasev = Phase.create("Pv220P");
        Phase phase = Phase.create("P");
        double pstart = 700;
        double pend = 800;
        double pdelta = 1;
        for (double p = pstart; p < pend; p += pdelta) {
            Raypath raypath = new Raypath(p);
            raypath.compute();
            double pDelta = Math.toDegrees(raypath.computeDelta(6371, phase));
            double pvdelta = Math.toDegrees(raypath.computeDelta(6371, phasev));
            if (Double.isNaN(pDelta)) pDelta = 0;
            if (Double.isNaN(pvdelta)) pvdelta = 0;
            System.out.println(p + " " + raypath.getTurningR(PhasePart.P) + " " + pDelta + " " + pvdelta);
        }
    }

    private static void recordSection() throws ParseException {
        String[] cmd =
                "-rs 30,40,0.1 -ph S -SV -mod prem -o /home/kensuke/workspace/kibrary/anisotime/record_section/rs.txt"
                        .split("\\s+");
        ANISOtimeCLI.main(cmd);

    }

    private static void singleS1382() {
        Raypath raypath = new Raypath(1382.5332586419352);
        raypath.compute();
        System.out.println("turning " + (6371 - raypath.getTurningR(PhasePart.SH)));
        System.out.println(Math.toDegrees(raypath.computeDelta(6371, Phase.S)));
    }

    private static void single1() {
        final VelocityStructure prem = VelocityStructure.prem();
        double point = 6371 - 400;
        double v = prem.computeVsh(point);
        double pMax = point / v;
//1385.9777587321248
        System.out.println(pMax);
        Raypath raypath = new Raypath(pMax);
        raypath.compute();
        System.out.println(
                raypath.getTurningR(PhasePart.SH) + " " + Math.toDegrees(raypath.computeDelta(6371, Phase.S)) + " " +
                        Math.toDegrees(raypath.computeDelta(6371, Phase.SV)));
    }

    private static void single10() throws ParseException {
        String[] cmd = "-deg 10 -mod prem -ph S -SV".split("\\s+");
        ANISOtimeCLI.main(cmd);
    }

    private static void catalog1() {
        final RaypathCatalog isoPrem = RaypathCatalog.prem();
        final Raypath[] raypaths = isoPrem.getRaypaths();

        final Raypath[] raypaths1 = isoPrem.searchPath(Phase.S, 6371, Math.toRadians(10), false);

        System.out.println(raypaths1.length);

        for (Raypath raypath : raypaths) {
            double p = raypath.getRayParameter();
            double delta = raypath.computeDelta(6371, Phase.S);
            delta = Math.toDegrees(delta);
            double turningR = raypath.getTurningR(PhasePart.SH);
            double time = raypath.computeT(6371, Phase.S);
            if (Double.isNaN(delta)) System.out.println("#" + raypath.getRayParameter() + " " + delta + " " + turningR);
            else System.out.println(raypath.getRayParameter() + " " + delta + " " + time + " " + turningR);
        }
    }

    private static void checkV3630() {
        PolynomialStructure prem = PolynomialStructure.PREM;
        double boundary = 6371 - 220;
        double deltaR = ComputationalMesh.EPS;
        DoubleUnaryOperator get = prem::computeVsh;
        double x = get.applyAsDouble(boundary + deltaR);
        System.out.println(x);
        double x1 = get.applyAsDouble(boundary - deltaR);

        System.out.println((x1 / x - 1) * 100);
    }

    private static void anCheck() throws IOException {
        final PolynomialStructure prem1 = PolynomialStructure.PREM;
        final io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure prem =
                io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.PREM;
        double rstart = 3629;
        double rend = 3631;
        double delta = 0.0001 * (rend - rstart);
        for (double r = rstart; r < rend; r += delta) {
            System.out.println(r + " " + prem.getVshAt(r) + " " + prem1.computeVsh(r));
        }
    }

    private static void anCheck2() {
        double pstart = 8.713 * 180 / 3.14;
        double pend = 8.718 * 180 / 3.14;
        double pdelta = (pend - pstart) / 100;
        for (double p = pstart; p < pend; p += pdelta) {
            Raypath raypath = new Raypath(p);
            raypath.compute();
            double v = raypath.computeDelta(6371, Phase.S);
            v = Math.toDegrees(v);
            System.out.println(p + " " + v);
        }


    }

    private static void anCheck3() {
        /*
        499.59171974521183 NaN
499.5920063694156 NaN
inrange
499.5922929936194 93.88525341783792
         */
        VelocityStructure structure = VelocityStructure.prem();
        System.out.println(structure.shTurningR(499.5922929936194));
    }

    private static void mergeCheck() throws IOException {
        final io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure prem =
                io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.PREM;
        System.out.println(prem.getNzone());
        prem.writePSV(Paths.get("/tmp/hoge0.txt"));

        final io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure polynomialStructure =
                prem.mergeLayer(10);
        polynomialStructure.writePSV(Paths.get("/tmp/hoge.txt"));
    }

    private static void sh220() {
        double pstart = 1324.5332586419352;
        double pend = 1386.527397038334;
//        pend = 1500;
        Phase s = Phase.S;
        Phase s220 = Phase.create("Sv220S");
        for (double p = pstart; p < pend; p += 1) {
            Raypath raypath = new Raypath(p);
            raypath.compute();
            double sDelta = Math.toDegrees(raypath.computeDelta(6371, s));
            double s220Delta = Math.toDegrees(raypath.computeDelta(6371, s220));
            System.out.println(p + " " + sDelta + " " + s220Delta + " " + raypath.getTurningR(PhasePart.SH));
        }


    }

    private static void noScheck() {
//        1379.029253094265 1386.527397038334
        double pstart = 1379.029253094265;
        double pend = 1386.527397038334;
        for (double p = pstart; p < pend; p += 0.1) {
            Raypath raypath = new Raypath(p);
            raypath.compute();
            double sDelta = Math.toDegrees(raypath.computeDelta(6371, Phase.S));
            System.out.println(p + " " + sDelta + " " + raypath.getTurningR(PhasePart.SH));

        }
    }
private static void kennetFig2(){
//        Raypath raypath = new Raypath(100)
}


    public static void main(String[] args) throws IOException, ParseException {
//        recordSection();
//kennetFig2();
//        single10();
//        catalog1();
//        single1();
//       test1986();
//       check220();
//        anCheck3();
//        anCheck();
//        anCheck2();
//        mergeCheck();
//        checkV3630();
//        singleS1382();
//        catalogDev();
//                catalogOut();
//        diffBack();
        catalogCheck();
//        sh220();
//manyPKIKP();
//        singlePKIKP();
//        singleP();
//        noScheck();
        System.exit(0);
//        pTriple();
//showBoundaries();
//        pkikip();
//        download();
//        extract();
//        RaypathCatalog.extractInShare();
//        diffBack();
//        doubleDiff();
//        skiks();
//        compareAnalytical();
//        debug1();
//        catalog();
//        diff();
//        eternally();
//        creation();
//        System.exit(0);
//        pplot();
//        testNan();
//        reflect();
//        velo();
//        meshCheck();
//        ghost();
//        single2();
//        single();
//ray0();
    }

    private static void ray0() {
        Raypath raypath = new Raypath(0, VelocityStructure.iprem());
        raypath.compute();
        System.out.println(raypath.computeDelta(6371, Phase.PcP) + " " + raypath.computeT(6371, Phase.PcP));
    }

    private static void single2() {
        ComputationalMesh simpleMesh = ComputationalMesh.simple(VelocityStructure.ak135());
        System.out.println(VelocityStructure.ak135().innerCoreBoundary());
//        System.exit(0);
        Raypath raypath = new Raypath(254.71807497801723, VelocityStructure.ak135(), simpleMesh);


    }

    private static void meshCheck() {
        ComputationalMesh mesh = ComputationalMesh.simple(VelocityStructure.iprem());
        RealVector mesh1 = mesh.getMesh(Partition.MANTLE);
        for (int i = 0; i < mesh1.getDimension(); i++) {
            System.out.println(mesh1.getEntry(i));
        }
        for (double v : VelocityStructure.iprem().velocityBoundaries()) {
            System.out.println(v);
        }
    }

    private static void catalogOut() throws IOException {
        Raypath[] raypaths = RaypathCatalog.prem().getRaypaths();

        Phase phase = Phase.P;
        PhasePart pp = PhasePart.P;
        Phase pv = Phase.create("Pv220P");
        Path out = Paths.get("/home/kensuke/workspace/kibrary/anisotime/kennet/test.txt");
        List<String> lines = new ArrayList<>();
        for (Raypath raypath : raypaths) {
            double delta = Math.toDegrees(raypath.computeDelta(6371, phase));
            double time = raypath.computeT(6371, phase);
            double deltaV = Math.toDegrees(raypath.computeDelta(6371, pv));
            double timeV = raypath.computeT(6371, pv);
            String line = raypath.getRayParameter() + " " + delta + " "+time+" " + deltaV+" "+timeV;
//            if (Double.isNaN(delta)) line = "# " + line;
            lines.add(line);
        }
        Files.write(out, lines);
    }

    private static void reflect() {
        double pstart = 499.5891;
        double pend = 499.5923;
        double deltaP = (pend - pstart) / 100;
        for (double p = pstart; p < pend; p += deltaP) {
            Raypath raypath1 = new Raypath(p, VelocityStructure.iprem());
            raypath1.compute();
            System.out.println(p + " " + Math.toDegrees(raypath1.computeDelta(6371, Phase.create("Sv2741S"))) + " " +
                    raypath1.getTurningR(PhasePart.SH));

        }
    }


    private static void ghost() {
        Raypath raypath1 = new Raypath(1990.930175750244, VelocityStructure.iprem());
        raypath1.compute();
        System.out
                .println(Math.toDegrees(raypath1.computeDelta(6371, Phase.S)) + " " + raypath1.computeT(6371, Phase.S));
    }

    private static void eternally() {
//        Raypath raypath = new Raypath(778.9507789768932,VelocityStructure.iprem());
        Raypath raypath1 = new Raypath(499.5891057171594, VelocityStructure.iprem());
        raypath1.compute();
        System.out
                .println(Math.toDegrees(raypath1.computeDelta(6371, Phase.S)) + " " + raypath1.computeT(6371, Phase.S));

    }


    private static void velo() {
        double rstart = 3630 - 1;
        double rend = 3630 + 1;
        io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure prem =
                io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure.PREM;
        for (double r = rstart; r < rend; r += 0.01) {
            System.out.println(r + " " + prem.getVshAt(r));
        }


    }


    private static void catalog() throws ParseException {
        //  java io.github.kensuke1984.anisotime.ANISOtimeCLI -mod iprem -h 0 -ph S -deg 94.2 -dec 9
        String[] s = "-mod iprem -h 0 -ph S -deg 93.9 -dec 9".split(" ");
        ANISOtimeCLI.main(s);
    }

    private static void debug1() {
//        Raypath raypath = new Raypath(701);
        long start = System.nanoTime();
        Raypath raypath = new Raypath(301);
        raypath.compute();
        for (int i = 0; i < 100000; i++) {


            Phase p = Phase.SKS;
            double delta = raypath.computeDelta(6371, p);
            System.out.println(Math.toDegrees(delta));
        }
        long end = System.nanoTime();
        System.out.println(Utilities.toTimeString(end - start));
    }

    private static void debug() {
        //568.133430480957 568.1334352493286 24.073127644768874 23.859371079448923
        VelocityStructure velocityStructure = VelocityStructure.prem();
        double pstart = 568.13;
        double pend = 568.14;
        int n = 10000;
        double deltaP = (pend - pstart) / n;
        for (int i = 0; i < n; i++) {
            double p = pstart + deltaP * i;
            Raypath raypath = new Raypath(p, velocityStructure, ComputationalMesh.simple(velocityStructure));

            raypath.compute();
            double delta = Math.toDegrees(raypath.computeDelta(6371, Phase.P));
            System.out.println(p + " " + delta);
        }
    }

    public static void compareAnalytical() throws IOException {
        double rayparameter = 10.;

        PrintWriter pwD =
                new PrintWriter(Paths.get("/home/kensuke/secondDisk/workspace/anisotime/analyt/distance.dat").toFile());
        PrintWriter pwT = new PrintWriter(
                Paths.get("/home/kensuke/secondDisk/workspace/anisotime/analyt/error_analytical.dat").toFile());

        pwT.println("# dr (km), err_scs_sh (%), err_scs_sv (%), err_pcp (%)");
        pwD.println("# dr (km), d_scs_sh (deg), d_scs_sv (deg), d_pcp (deg)");

        PolynomialStructure homogen = PolynomialStructure.HOMOGEN;
        double r0 = 5000.;
        double rho0 = homogen.getRho(r0);
        double N0 = homogen.getN(1);
        double L0 = homogen.getL(1);
        double A0 = homogen.getA(1);
        double C0 = homogen.getC(1);
        double F0 = homogen.getF(1);
        System.out.println(A0 + " " + C0 + " " + F0 + " " + L0 + " " + N0 + " " + rho0);

        //mesh
        for (int i = 0; i < 20; i++) {
            double dr = 1000 * Math.exp(-i / 4.);
            System.out.println(i + " " + dr);
            ComputationalMesh mesh = new ComputationalMesh(homogen, dr, dr, dr);

            Raypath raypath = new Raypath(rayparameter, homogen, mesh);
            raypath.compute();
            double eventR = 6371.;

            double delta_scs_sh = Math.toDegrees(raypath.computeDelta(eventR, Phase.ScS));
            double t_scs_sh = raypath.computeT(eventR, Phase.ScS);

            double delta_scs_sv = Math.toDegrees(raypath.computeDelta(eventR, Phase.create("ScS", true)));
            double t_scs_sv = raypath.computeT(eventR, Phase.create("ScS", true));

            double delta_pcp = Math.toDegrees(raypath.computeDelta(eventR, Phase.PcP));
            double t_pcp = raypath.computeT(eventR, Phase.PcP);

            double tAnal_scs_sh = computeTanalyticalHomogenSH(rayparameter, 3480., 6371.) * 2;
            double tAnal_scs_sv = computeTanalyticalHomogenSV(rayparameter, 3480., 6371.) * 2;
            double tAnal_pcp = computeTanalyticalHomogenP(rayparameter, 3480., 6371.) * 2;

            System.out.println("ScS (SH): " + delta_scs_sh + " " + t_scs_sh + " " + tAnal_scs_sh);
            System.out.println("ScS (SV): " + delta_scs_sv + " " + t_scs_sv + " " + tAnal_scs_sv);
            System.out.println("PcP: " + delta_pcp + " " + t_pcp + " " + tAnal_pcp);

            double e_t_scs_sh = Math.abs(t_scs_sh - tAnal_scs_sh) / tAnal_scs_sh;
            double e_t_scs_sv = Math.abs(t_scs_sv - tAnal_scs_sv) / tAnal_scs_sv;
            double e_t_pcp = Math.abs(t_pcp - tAnal_pcp) / tAnal_pcp;


            pwD.println(dr + " " + delta_scs_sh + " " + delta_scs_sv + " " + delta_pcp);
            pwT.println(dr + " " + e_t_scs_sh + " " + e_t_scs_sv + " " + e_t_pcp);
            pwD.flush();
            pwT.flush();
        }
        pwT.close();
        pwD.close();
    }

    public static double computeTanalyticalHomogenSH(double rayparameter, double rmin, double rmax) {
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
