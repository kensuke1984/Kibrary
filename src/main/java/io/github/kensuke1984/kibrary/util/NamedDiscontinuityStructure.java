package io.github.kensuke1984.kibrary.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;

/**
 * Within each layer Bullen law v(r) = Ar<sup>B</sup> is used.
 * {@link #computeBullenLaw(int, double[])}
 * <p>
 * Xgbm Davis and Henson, 1993
 *
 * @author Kensuke Konishi
 * @version 0.1.0
 */
public class NamedDiscontinuityStructure {

    /**
     * number of boundaries
     */
    private int nBoundary;

    /**
     * values at boundaries
     */
    private double[] rho;
    /**
     * values of Bullen law
     */
    private double[] rhoA;
    /**
     * values of Bullen law
     */
    private double[] rhoB;
    /**
     * values at boundaries
     */
    private double[] vpv;
    /**
     * values at boundaries
     */
    private double[] vph;
    /**
     * values of the Bullen law
     */
    private double[] vpvA;
    /**
     * values of the Bullen law
     */
    private double[] vpvB;
    /**
     * values of the Bullen law
     */
    private double[] vphA;
    /**
     * values of the Bullen law
     */
    private double[] vphB;

    /**
     * values at boundaries
     */
    private double[] vsv;
    /**
     * values at boundaries
     */
    private double[] vsh;
    /**
     * values of the Bullen law
     */
    private double[] vsvA;
    /**
     * values of the Bullen law
     */
    private double[] vsvB;
    /**
     * values of the Bullen law
     */
    private double[] vshA;
    /**
     * values of the Bullen law
     */
    private double[] vshB;

    private double[] eta;
    /**
     * values of the Bullen law
     */
    private double[] etaA;
    /**
     * values of the Bullen law
     */
    private double[] etaB;


    /**
     * values at boundaries
     */
    private double[] qMu;
    /**
     * values of the Bullen law
     */
    private double[] qMuA;
    /**
     * values of the Bullen law
     */
    private double[] qMuB;

    /**
     * values at boundaries
     */
    private double[] qKappa;

    /**
     * values of the Bullen law
     */
    private double[] qKappaA;
    /**
     * values of the Bullen law
     */
    private double[] qKappaB;

    /**
     * [km] radii of boundaries
     */
    private double[] r;

    /**
     * structure file
     */
    private Path infPath;
    private int indexOfCoreMantleBoundary;
    private int indexOfInnerCoreBoundary;
    private int indexOfMohoDiscontinuity;

    protected NamedDiscontinuityStructure() {
    }

    /**
     * @param path model file written by nd format.
     */
    public NamedDiscontinuityStructure(Path path) throws Exception {
        infPath = path;
        readInfFile();
    }

    /**
     * @return Isotropic PREM
     */
    public static NamedDiscontinuityStructure prem() {
        NamedDiscontinuityStructure nd = new NamedDiscontinuityStructure();
        nd.indexOfCoreMantleBoundary = 37;
        nd.indexOfInnerCoreBoundary = 13;
        nd.indexOfMohoDiscontinuity = 83;
        nd.nBoundary = 88;
        nd.r = new double[]{0.0, 100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1000.0, 1100.0, 1200.0,
                1221.5, 1221.5, 1300.0, 1400.0, 1500.0, 1600.0, 1700.0, 1800.0, 1900.0, 2000.0, 2100.0, 2200.0, 2300.0,
                2400.0, 2500.0, 2600.0, 2700.0, 2800.0, 2900.0, 3000.0, 3100.0, 3200.0, 3300.0, 3400.0, 3480.0, 3480.0,
                3500.0, 3600.0, 3630.0, 3700.0, 3800.0, 3900.0, 4000.0, 4100.0, 4200.0, 4300.0, 4400.0, 4500.0, 4600.0,
                4700.0, 4800.0, 4900.0, 5000.0, 5100.0, 5200.0, 5300.0, 5400.0, 5500.0, 5600.0, 5650.0, 5701.0, 5701.0,
                5736.0, 5771.0, 5821.0, 5871.0, 5921.0, 5971.0, 5971.0, 6016.0, 6061.0, 6106.0, 6151.0, 6151.0, 6186.0,
                6221.0, 6256.0, 6291.0, 6311.0, 6331.0, 6346.6, 6346.6, 6356.0, 6356.0, 6371.0,};
        nd.rho = new double[]{13.08848, 13.0863, 13.07977, 13.06888, 13.05364, 13.03404, 13.01009, 12.98178, 12.94912,
                12.91211, 12.87073, 12.82501, 12.77493, 12.7636, 12.16634, 12.125, 12.06924, 12.00989, 11.94682,
                11.8799, 11.809, 11.73401, 11.65478, 11.57119, 11.48311, 11.39042, 11.29298, 11.19067, 11.08335,
                10.97091, 10.85321, 10.73012, 10.60152, 10.46727, 10.32726, 10.18134, 10.0294, 9.90349, 5.56645,
                5.55641, 5.50642, 5.49145, 5.45657, 5.40681, 5.35706, 5.30724, 5.25729, 5.20713, 5.15669, 5.1059,
                5.05469, 5.00299, 4.95073, 4.89783, 4.84422, 4.78983, 4.7346, 4.67844, 4.62129, 4.56307, 4.50372,
                4.44317, 4.41241, 4.38071, 3.99214, 3.98399, 3.97584, 3.91282, 3.8498, 3.78678, 3.72378, 3.54325,
                3.51639, 3.48951, 3.46264, 3.43578, 3.3595, 3.3633, 3.3671, 3.37091, 3.37471, 3.37688, 3.37906, 3.38076,
                2.9, 2.9, 2.6, 2.6,};
        nd.vph = new double[]{11.2622, 11.26064, 11.25593, 11.24809, 11.23712, 11.22301, 11.20576, 11.18538, 11.16186,
                11.13521, 11.10542, 11.07249, 11.03643, 11.02827, 10.35568, 10.30971, 10.24959, 10.18743, 10.12291,
                10.05572, 9.98554, 9.91206, 9.83496, 9.75393, 9.66865, 9.57881, 9.48409, 9.38418, 9.27867, 9.16752,
                9.05015, 8.92632, 8.79573, 8.65805, 8.51298, 8.36019, 8.19939, 8.06482, 13.7166, 13.71168, 13.68753,
                13.68041, 13.59597, 13.47742, 13.36074, 13.24532, 13.13055, 13.01579, 12.90045, 12.78389, 12.6655,
                12.54466, 12.42075, 12.29316, 12.16126, 12.02445, 11.88209, 11.73357, 11.57828, 11.4156, 11.2449,
                11.06557, 10.91005, 10.75131, 10.26622, 10.21203, 10.15782, 9.90185, 9.64588, 9.3899, 9.13397, 8.90522,
                8.81867, 8.73209, 8.64552, 8.55896, 7.9897, 8.0118, 8.0337, 8.0554, 8.07688, 8.08907, 8.10119, 8.11061,
                6.8, 6.8, 5.8, 5.8,};
        nd.vsh = new double[]{3.6678, 3.6667, 3.66342, 3.65794, 3.65027, 3.64041, 3.62835, 3.61411, 3.59767, 3.57905,
                3.55823, 3.53522, 3.51002, 3.50432, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.26466, 7.26486, 7.26575, 7.26597, 7.23403,
                7.18892, 7.14423, 7.09974, 7.05525, 7.01053, 6.96538, 6.91957, 6.87289, 6.82512, 6.77606, 6.72548,
                6.67317, 6.61891, 6.5625, 6.5037, 6.44232, 6.37813, 6.31091, 6.24046, 6.09418, 5.94508, 5.5702, 5.54311,
                5.51602, 5.37014, 5.22428, 5.07842, 4.93259, 4.76989, 4.7384, 4.7069, 4.6754, 4.64391, 4.41885, 4.43108,
                4.44361, 4.45643, 4.46953, 4.47715, 4.48486, 4.49094, 3.9, 3.9, 3.2, 3.2,};
        nd.qMu = new double[]{85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0,
                312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 143.0, 143.0,
                143.0, 143.0, 143.0, 143.0, 143.0, 143.0, 143.0, 143.0, 143.0, 143.0, 80.0, 80.0, 80.0, 80.0, 80.0,
                600.0, 600.0, 600.0, 600.0, 600.0, 600.0, 600.0,};
        nd.qKappa =
                new double[]{431.0, 431.0, 431.0, 432.0, 432.0, 433.0, 434.0, 436.0, 437.0, 439.0, 440.0, 443.0, 445.0,
                        445.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0,
                        57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0,
                        57822.0, 57822.0, 57822.0, 57822.0, 826.0, 826.0, 823.0, 822.0, 819.0, 815.0, 811.0, 807.0,
                        803.0, 799.0, 795.0, 792.0, 788.0, 784.0, 779.0, 775.0, 770.0, 766.0, 761.0, 755.0, 750.0,
                        743.0, 737.0, 730.0, 744.0, 759.0, 362.0, 362.0, 362.0, 363.0, 364.0, 365.0, 366.0, 372.0,
                        370.0, 367.0, 365.0, 362.0, 195.0, 195.0, 195.0, 195.0, 195.0, 1447.0, 1446.0, 1446.0, 1350.0,
                        1350.0, 1456.0, 1456.0,};
        nd.computeBullenLaw();
        return nd;
    }

    private String[] toLines() {
        String[] outString = new String[nBoundary + 3];
        for (int i = nBoundary - 1, j = 0; 0 <= i; i--) {
            if (i == indexOfMohoDiscontinuity) outString[j++] = "mantle";
            else if (i == indexOfCoreMantleBoundary) outString[j++] = "outer-core";
            else if (i == indexOfInnerCoreBoundary) outString[j++] = "inner-core";
            outString[j++] =
                    Math.round((6371.0 - r[i]) * 10000000) / 10000000.0 + " " + rho[i] + " " + vpv[i] + " " + vph[i] +
                            " " + vsv[i] + " " + vsh[i] + " " + qKappa[i] + " " + qMu[i];
        }
        return outString;
    }

    /**
     * @param outPath {@link Path} of an write file.
     * @param options for writing
     * @throws IOException if any
     */
    public void write(Path outPath, OpenOption... options) throws IOException {
        Files.write(outPath, Arrays.asList(toLines()), options);
    }


    /**
     * @param r radius [km]
     * @param a a
     * @param b b
     * @return a*(6371-r)<sup>b</sup>
     */
    private static double computeBullenLaw(double r, double a, double b) {
        return a * Math.pow(r, b);
    }

    /**
     * @return radius of the core mantle boundary [km]
     */
    public double coreMantleBoundary() {
        return r[indexOfCoreMantleBoundary];
    }

    /**
     * @return radius of the inner core boundary [km]
     */
    public double innerCoreBoundary() {
        return r[indexOfInnerCoreBoundary];
    }

    private Map<String, Double> boundaries = new HashMap<>();

    private void readInfFile() throws Exception {
        if (!Files.exists(infPath)) throw new NoSuchFileException(infPath + " does not exist.");
        List<String[]> useLines = new ArrayList<>();
        Map<String, Integer> boundaries = new HashMap<>();

        // which layers are boundaries
        int iMoho = 0;
        int iCoreMantleBoundary = 0;
        int iInnerCoreBoundary = 0;
        List<String> lineList = Files.readAllLines(infPath);
        String mantle = "mantle";
        String outercore = "outer-core";
        String innercore = "inner-core";
        int discontinuity = 0;
        for (int i = 0; i < lineList.size(); i++) {
            String line = lineList.get(i).trim();
            if (line.charAt(0) == '#' || line.charAt(0) == '!' || line.charAt(0) == 'c') continue;
            if (line.equals(mantle)) {
                iMoho = i - 1 - discontinuity;
                discontinuity++;
                continue;
            } else if (line.equals(outercore)) {
                iCoreMantleBoundary = i - 1 - discontinuity;
                discontinuity++;
                continue;
            } else if (line.equals(innercore)) {
                iInnerCoreBoundary = i - 1 - discontinuity;
                discontinuity++;
                continue;
            } else if (!Character.isDigit(line.charAt(0))) {
                boundaries.put(line, i - 1 - discontinuity);
                discontinuity++;
                continue;
            }
            String[] parts = lineList.get(i).split("\\s+");
            if (parts.length != 9) throw new RuntimeException("FORMAT ERROR at " + lineList.get(i));
            useLines.add(parts);
        }

        nBoundary = useLines.size(); // inner core outer core.....
        r = new double[nBoundary];
        rho = new double[nBoundary];
        vpv = new double[nBoundary];
        vph = new double[nBoundary];
        vsv = new double[nBoundary];
        vsh = new double[nBoundary];
        eta = new double[nBoundary];
        qKappa = new double[nBoundary];
        qMu = new double[nBoundary];
        for (int i = 0; i < nBoundary; i++) {
            String[] parts = useLines.get(i);
            int j = nBoundary - 1 - i;
            r[j] = Double.parseDouble(parts[0]);
            rho[j] = Double.parseDouble(parts[1]);
            vpv[j] = Double.parseDouble(parts[2]);
            vph[j] = Double.parseDouble(parts[3]);
            vsv[j] = Double.parseDouble(parts[4]);
            vsh[j] = Double.parseDouble(parts[5]);
            eta[j] = Double.parseDouble(parts[6]);
            qKappa[j] = Double.parseDouble(parts[7]);
            qMu[j] = Double.parseDouble(parts[8]);
        }

        for (int i = 1; i < r.length; i++) r[i] = r[0] - r[i];

        indexOfCoreMantleBoundary = nBoundary - iCoreMantleBoundary - 1;
        indexOfInnerCoreBoundary = nBoundary - iInnerCoreBoundary - 1;
        indexOfMohoDiscontinuity = nBoundary - iMoho - 1;
        boundaries.forEach((s, i) -> this.boundaries.put(s, r[nBoundary - i - 1]));

        computeBullenLaw();
    }

    /**
     * @return radius of the moho discontinuity
     */
    public double getMohoDiscontinuity() {
        return r[indexOfMohoDiscontinuity];
    }

    /**
     * @param r radius [km]
     * @return rho at r
     */
    public double getRho(double r) {
        int izone = rToZone(r);
        return computeBullenLaw(r, rhoA[izone], rhoB[izone]);
    }

    /**
     * @param r radius [km]
     * @return Vpv at r
     */
    public double getVpv(double r) {
        int izone = rToZone(r);
        return computeBullenLaw(r, vpvA[izone], vpvB[izone]);
    }

    /**
     * @param r radius [km]
     * @return Vph at r
     */
    public double getVph(double r) {
        int izone = rToZone(r);
        return computeBullenLaw(r, vphA[izone], vphB[izone]);
    }

    /**
     * @param r radius [km]
     * @return Vsv at r
     */
    public double getVsv(double r) {
        int izone = rToZone(r);
        return computeBullenLaw(r, vsvA[izone], vsvB[izone]);
    }

    /**
     * @param r radius [km]
     * @return Vsh at r
     */
    public double getVsh(double r) {
        int izone = rToZone(r);
        return computeBullenLaw(r, vshA[izone], vshB[izone]);
    }

    /**
     * @param r radius [km]
     * @return Q&eta; at r
     */
    public double getEta(double r) {
        int izone = rToZone(r);
        return computeBullenLaw(r, etaA[izone], etaB[izone]);
    }

    /**
     * @param r radius [km]
     * @return Q&mu; at r
     */
    public double getQMu(double r) {
        int izone = rToZone(r);
        return computeBullenLaw(r, qMuA[izone], qMuB[izone]);
    }

    /**
     * @param r radius [km]
     * @return Q&kappa; at r
     */
    public double getQKappa(double r) {
        int izone = rToZone(r);
        return computeBullenLaw(r, qKappaA[izone], qKappaB[izone]);
    }


    /**
     * @param izone zone number
     * @return a of a*r<sup>b</sup> in i th zone
     */
    public double getVpvA(int izone) {
        return vpvA[izone];
    }

    /**
     * @param izone zone number
     * @return b of a*r<sup>b</sup> in i th zone
     */
    public double getVpvB(int izone) {
        return vpvB[izone];
    }

    /**
     * @param izone zone number
     * @return a of a*r<sup>b</sup> in i th zone
     */
    public double getVphA(int izone) {
        return vphA[izone];
    }

    /**
     * @param izone zone number
     * @return b of a*r<sup>b</sup> in i th zone
     */
    public double getVphB(int izone) {
        return vphB[izone];
    }

    /**
     * @param izone zone number
     * @return a of a*r<sup>b</sup> in i th zone
     */
    public double getVsvA(int izone) {
        return vsvA[izone];
    }

    /**
     * @param izone zone number
     * @return b of a*r<sup>b</sup> in i th zone
     */
    public double getVsvB(int izone) {
        return vsvB[izone];
    }

    /**
     * @param izone zone number
     * @return a of a*r<sup>b</sup> in i th zone
     */
    public double getVshA(int izone) {
        return vshA[izone];
    }

    /**
     * @param izone zone number
     * @return b of a*r<sup>b</sup> in i th zone
     */
    public double getVshB(int izone) {
        return vshB[izone];
    }

    private int rToZone(double r) {
        for (int i = 0; i < this.r.length - 1; i++)
            if (this.r[i] <= r && r <= this.r[i + 1]) return i;
        throw new RuntimeException("rToZone did not work correctly");
    }

    private void computeBullenLaw() {
        int nzone = nBoundary - 1;
        rhoA = new double[nzone];
        rhoB = new double[nzone];
        vpvA = new double[nzone];
        vpvB = new double[nzone];
        vphA = new double[nzone];
        vphB = new double[nzone];
        vsvA = new double[nzone];
        vsvB = new double[nzone];
        vshA = new double[nzone];
        vshB = new double[nzone];
        etaA = new double[nzone];
        etaB = new double[nzone];
        qMuA = new double[nzone];
        qMuB = new double[nzone];
        qKappaA = new double[nzone];
        qKappaB = new double[nzone];
        for (int i = 0; i < nzone; i++) {
            double[] rh = computeBullenLaw(i, rho);
            rhoA[i] = rh[0];
            rhoB[i] = rh[1];
            double[] pv = computeBullenLaw(i, vpv);
            vpvA[i] = pv[0];
            vpvB[i] = pv[1];
            double[] ph = computeBullenLaw(i, vph);
            vphA[i] = ph[0];
            vphB[i] = ph[1];
            double[] sv = computeBullenLaw(i, vsv);
            vsvA[i] = sv[0];
            vsvB[i] = sv[1];
            double[] sh = computeBullenLaw(i, vsh);
            vshA[i] = sh[0];
            vshB[i] = sh[1];
            double[] e = computeBullenLaw(i, eta);
            etaA[i] = e[0];
            etaB[i] = e[1];
            double[] qmu = computeBullenLaw(i, qMu);
            qMuA[i] = qmu[0];
            qMuB[i] = qmu[1];
            double[] qkappa = computeBullenLaw(i, qKappa);
            qKappaA[i] = qkappa[0];
            qKappaB[i] = qkappa[1];
        }
    }

    /**
     * obtain the valued a, b of v(r) = a* r **b r is radius
     *
     * @param izone izone
     * @param val   val
     * @return the value a, b in Bullen expression.
     */
    private double[] computeBullenLaw(int izone, double[] val) {
        if (nBoundary <= izone || izone < 0) throw new RuntimeException("calcBullenCoef did not work correctly");
        if (val[izone] == 0 && val[izone + 1] == 0) return new double[]{0, 1};

        double b = Math.log(val[izone + 1] / val[izone]) / Math.log((r[izone + 1]) / (r[izone]));
        double a = val[izone] / Math.pow((r[izone]), b);
        return new double[]{a, b};
    }

    /**
     * i=0 (center) imax(surface)
     *
     * @param i zone number
     * @return radius of i th boundary [km]
     */
    public double getBoundary(int i) {
        return r[i];
    }

    /**
     * @return the number of zones (nBoundary -1)
     */
    public int getNzone() {
        return nBoundary - 1;
    }

}
