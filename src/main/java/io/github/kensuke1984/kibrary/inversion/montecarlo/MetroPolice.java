package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.util.sac.SACData;

/**
 * Waveform inversion with Metropolis–Hastings algorithm <a href=
 * https://en.wikipedia.org/wiki/Metropolis%E2%80%93Hastings_algorithm>wiki</a>
 *
 * @author Kensuke Konishi
 *         <p>
 *         logFile is in run0.
 * @version 0.1.0
 */
class MetroPolice<M, D> {
    private final Path WORK_DIR;

    /**
     * create a model
     */
    private final ModelGenerator<M> MODEL_GENERATOR;

    /**
     * compare data
     */
    private final DataComparator<D> DATA_COMPARATOR;

    private final DataGenerator<M, D> DATA_GENERATOR;

    private MetroPolice(Path workDir, ModelGenerator<M> modelGenerator, DataGenerator<M, D> dataGenerator,
                        DataComparator<D> dataComparator) throws IOException {
        MODEL_GENERATOR = modelGenerator;
        DATA_COMPARATOR = dataComparator;
        DATA_GENERATOR = dataGenerator;
        new ArrayList<>();
        WORK_DIR = workDir;
        MODEL_PATH = workDir.resolve("models");
        if (Files.exists(MODEL_PATH)) throw new FileAlreadyExistsException(MODEL_PATH.toString());
        Files.createDirectories(MODEL_PATH);
    }

    private final Path MODEL_PATH;

    private void run() throws IOException, InterruptedException {

        int nRun = limit;
        boolean betterModel = false;

        System.out.println("MetroPolice is going.");
        M lastAdoptedModel = MODEL_GENERATOR.firstModel();
        D[] lastAdoptedDataset = DATA_GENERATOR.generate(lastAdoptedModel);
        Path lastAdoptedPath = MODEL_PATH.resolve("model0.inf");
        MODEL_GENERATOR.write(lastAdoptedPath, lastAdoptedModel);
        Files.createSymbolicLink(MODEL_PATH.resolve("adopted0.inf"), Paths.get("model0.inf"));
        System.out.println("Starting from run0");
        double lastAdoptedLikelihood = DATA_COMPARATOR.likelihood(lastAdoptedDataset);
        for (int iRun = 1; iRun < nRun + 1; iRun++) {
            M currentModel = MODEL_GENERATOR.createNextModel(lastAdoptedModel);
            Path currentPath = MODEL_PATH.resolve("model" + iRun + ".inf");
            MODEL_GENERATOR.write(currentPath, currentModel);
            D[] currentDataset = DATA_GENERATOR.generate(currentModel);
            double currentLikelihood = DATA_COMPARATOR.likelihood(currentDataset);
            if (judge(lastAdoptedLikelihood, currentLikelihood)) {
                lastAdoptedModel = currentModel;
                lastAdoptedDataset = currentDataset;
                lastAdoptedPath = currentPath;
                lastAdoptedLikelihood = currentLikelihood;
            }
            Files.createSymbolicLink(MODEL_PATH.resolve("adopted" + iRun + ".inf"), lastAdoptedPath.getFileName());
        }
    }

    private static boolean judge(double lastAdoptedLikelihood, double currentLikelihood) {
        try {
            double percentage = currentLikelihood / lastAdoptedLikelihood;
            if (1 < percentage) return true;
            double rand = Math.random();
            return rand < percentage;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static int limit = 100000;

    /**
     * @param args runpath, machinefile (option fo mpirun)
     * @throws IOException          if any
     * @throws InterruptedException iff any
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        Path root = Paths.get("/home/kensuke/secondDisk/montecarlo/test");
        Path tmp = Files.createTempDirectory(root, "metro");
        Path obsdir = root.resolve("obs");
        MetroPolice<PolynomialStructure, SACData> mp = new MetroPolice<>(tmp, new RandomPolynomialModelGenerator(),
                new DSMComputation(obsdir, tmp.resolve("data"), root.resolve("primePSV"),
                        StationInformationFile.read(root.resolve("station.inf"))), new SACVarianceComparator(obsdir));
        mp.run();
    }


}
