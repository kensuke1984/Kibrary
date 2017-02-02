package io.github.kensuke1984.kibrary.inversion.montecarlo;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Kensuke Konishi
 * @version 0.1.0
 */
class RandomPolynomialModelGenerator implements ModelGenerator<PolynomialStructure> {

    private final static PolynomialStructure INITIAL_STRUCTURE;

    static {
        PolynomialStructure ps = PolynomialStructure.PREM;
        ps = ps.addBoundaries(3530, 3580, 3630, 3680, 3730, 3780, 3830, 3880);
        PolynomialFunction polynomialFunction = new PolynomialFunction(new double[]{7.15, 0, 0, 0});
        INITIAL_STRUCTURE = ps.setVs(2, polynomialFunction).setVs(3, polynomialFunction).setVs(4, polynomialFunction)
                .setVs(5, polynomialFunction).setVs(6, polynomialFunction).setVs(7, polynomialFunction)
                .setVs(8, polynomialFunction).setVs(9, polynomialFunction);
    }

    private final Random RANDOM = new Random();

    private static void outputModelValue(Path runPath, PolynomialStructure structure) throws IOException {
        double[] vs = ModelProbability.readVs(structure);
        double[] q = ModelProbability.readQ(structure);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double r = 3505 + i * 50;
            lines.add(r + " " + vs[i] + " " + q[i]);
        }
        Files.write(runPath.resolve("candidate.txt"), lines);
    }

    private static PolynomialStructure createStructure(double[] percentage) {
        PolynomialStructure structure = INITIAL_STRUCTURE;
        for (int i = 0; i < 8; i++)
            structure = structure
                    .setVs(i + 2, new PolynomialFunction(new double[]{7.15 * (1 + percentage[i] / 100), 0, 0, 0}));
        for (int i = 8; i < 16; i++)
            structure = structure.setQMu(i - 6, 312 * (1 + percentage[i] / 100));
        return structure;
    }

    private static double[] extractPercentage(PolynomialStructure structure) {
        double[] percentage = new double[16];
        for (int i = 0; i < 8; i++)
            percentage[i] = structure.getVshOf(i + 2).value(0) / 7.15 * 100 - 100;

        for (int i = 8; i < 16; i++)
            percentage[i] = structure.getQMuOf(i - 6) / 312 * 100 - 100;
        return percentage;
    }

    public static void main(String[] args) throws IOException {
        Path p = Paths.get("/home/kensuke/data/WesternPacific/anelasticity/NobuakiInversion/raw");
        double[] percentage = new double[16];
        double value = 1;
        for (int i = 0; i < 8; i++) {
            percentage[i++] = value;
            percentage[i] = value;
            value *= -1;
        }
        value = -50;
        for (int i = 8; i < 16; i++) {
            percentage[i++] = value;
            percentage[i] = value;
            value *= -1;
        }
        PolynomialStructure ps1 = createStructure(percentage);
        ps1.writePSV(p.resolve("opposite50.model"));
    }

    /*
     * +-4% <br> 0:3480-3530 V<br> 1:3530-3580 V<br> .<br> 7:3830-3880 V<br>
     *
     * +- 10%<br> 8:3480-3530 Q<br> 9:3530-3580 Q<br> .<br> 15:3830-3880 Q<br>
     *
     */
    private PolynomialStructure nextStructure(PolynomialStructure former) {
        double[] percentage = extractPercentage(former);
        double[] changed = change(percentage);
        return createStructure(changed);
    }

    private double[] change(double[] percentage) {
        double[] changed = new double[16];
        for (int i = 0; i < 8; i++) {
            double v = percentage[i] + RANDOM.nextGaussian() * 4;
            if (v < -4) v = -8 - v;
            else if (4 < v) v = 8 - v;
            changed[i] = v;
        }
        for (int i = 8; i < 16; i++) {
            double q = percentage[i] + RANDOM.nextGaussian() * 5;
            if (q < -10) q = -20 - q;
            else if (10 < q) q = 20 - q;
            changed[i] = q;
        }

        return changed;
    }

    @Override
    public PolynomialStructure createNextModel(PolynomialStructure current) {
        return nextStructure(current);
    }

    @Override
    public PolynomialStructure firstModel() {
        return INITIAL_STRUCTURE;
    }

    @Override
    public void write(Path path, PolynomialStructure model) throws IOException {
        model.writePSV(path, StandardOpenOption.CREATE_NEW);
    }

}
