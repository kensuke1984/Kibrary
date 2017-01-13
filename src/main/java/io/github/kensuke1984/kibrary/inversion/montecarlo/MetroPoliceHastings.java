package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Waveform inversion with Metropolis–Hastings algorithm.
 *
 * @author Kensuke Konishi
 *         <p>
 *         logFile is in run0.
 * @version 0.1.0
 * @see <a href=https://en.wikipedia.org/wiki/Metropolis%E2%80%93Hastings_algorithm>Wikipedia</a>
 */
public class MetroPoliceHastings<M, D> {
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

    /**
     * @param workDir        working directory
     * @param modelGenerator Generation a new model from a model randomly.
     * @param dataGenerator  With a model made by the modelGenerator, data are generated.
     * @param dataComparator Data comparison.
     * @throws IOException if any
     */
    public MetroPoliceHastings(Path workDir, ModelGenerator<M> modelGenerator, DataGenerator<M, D> dataGenerator,
                               DataComparator<D> dataComparator) throws IOException {
        MODEL_GENERATOR = modelGenerator;
        DATA_COMPARATOR = dataComparator;
        DATA_GENERATOR = dataGenerator;
        WORK_DIR = workDir;
        MODEL_PATH = workDir.resolve("models");
        if (Files.exists(MODEL_PATH)) throw new FileAlreadyExistsException(MODEL_PATH.toString());
        Files.createDirectories(MODEL_PATH);
    }

    private final Path MODEL_PATH;

    public void run() throws IOException, InterruptedException {

        int nRun = limit;
        System.err.println("MetroPoliceHastings is going.");
        M lastAdoptedModel = MODEL_GENERATOR.firstModel();
        D lastAdoptedDataset = DATA_GENERATOR.generate(lastAdoptedModel);
        System.out.println(WORK_DIR);
        Path lastAdoptedPath = MODEL_PATH.resolve("model0.inf");
        MODEL_GENERATOR.write(lastAdoptedPath, lastAdoptedModel);
        Files.createSymbolicLink(MODEL_PATH.resolve("adopted0.inf"), Paths.get("model0.inf"));
        double lastAdoptedLikelihood = DATA_COMPARATOR.likelihood(lastAdoptedDataset);
        System.out.println(lastAdoptedLikelihood);
        for (int iRun = 1; iRun < nRun + 1; iRun++) {
            M currentModel = MODEL_GENERATOR.createNextModel(lastAdoptedModel);
            Path currentPath = MODEL_PATH.resolve("model" + iRun + ".inf");
            MODEL_GENERATOR.write(currentPath, currentModel);
            D currentDataset = DATA_GENERATOR.generate(currentModel);
            double currentLikelihood = DATA_COMPARATOR.likelihood(currentDataset);
            System.out.println(currentLikelihood);
            if (acceptsCurrent(lastAdoptedLikelihood, currentLikelihood)) {
                lastAdoptedModel = currentModel;
                lastAdoptedDataset = currentDataset;
                lastAdoptedPath = currentPath;
                lastAdoptedLikelihood = currentLikelihood;
            }
            Files.createSymbolicLink(MODEL_PATH.resolve("adopted" + iRun + ".inf"), lastAdoptedPath.getFileName());
        }
    }

    /**
     * @param lastAdoptedLikelihood likelihood for the last model
     * @param currentLikelihood     likelihood for the current model
     * @return if the current model is accept.
     */
    private static boolean acceptsCurrent(double lastAdoptedLikelihood, double currentLikelihood) {
        double percentage = currentLikelihood / lastAdoptedLikelihood;
        if (1 < percentage) return true;
        return Math.random() < percentage;
    }

    private static int limit = 100;

}
