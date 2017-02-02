package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private final Path MODEL_PATH;

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

    /**
     * @param lastAdoptedLikelihood likelihood for the last model
     * @param currentLikelihood     likelihood for the current model
     * @return if the current model is accept.
     */
    private static boolean acceptsCurrent(double lastAdoptedLikelihood, double currentLikelihood) {
        double percentage = currentLikelihood / lastAdoptedLikelihood;
        return 1 < percentage || Math.random() < percentage;
    }

    public void run(int nRun) throws IOException, InterruptedException {
        System.err.println("MetroPoliceHastings is going.");
        M lastAdoptedModel = MODEL_GENERATOR.firstModel();
        D lastAdoptedDataset = DATA_GENERATOR.generate(lastAdoptedModel);
        Path lastAdoptedPath = MODEL_PATH.resolve("model0.inf");
        MODEL_GENERATOR.write(lastAdoptedPath, lastAdoptedModel);
//        Files.createSymbolicLink(MODEL_PATH.resolve("adopted0.inf"), Paths.get("model0.inf"));
        double[] likelihoods = new double[nRun + 1];
        double lastAdoptedLikelihood = DATA_COMPARATOR.likelihood(lastAdoptedDataset);
        likelihoods[0] = lastAdoptedLikelihood;
        try (BufferedWriter writer = Files.newBufferedWriter(MODEL_PATH.resolve("adopted.txt"))) {
            writer.write("0 model0.inf");
            writer.newLine();
            for (int iRun = 1; iRun < nRun + 1; iRun++) {
                M currentModel = MODEL_GENERATOR.createNextModel(lastAdoptedModel);
                Path currentPath = MODEL_PATH.resolve("model" + iRun + ".inf");
                MODEL_GENERATOR.write(currentPath, currentModel);
                D currentDataset = DATA_GENERATOR.generate(currentModel);
                double currentLikelihood = DATA_COMPARATOR.likelihood(currentDataset);
                likelihoods[iRun] = currentLikelihood;
                if (acceptsCurrent(lastAdoptedLikelihood, currentLikelihood)) {
                    lastAdoptedModel = currentModel;
                    lastAdoptedDataset = currentDataset;
                    lastAdoptedPath = currentPath;
                    lastAdoptedLikelihood = currentLikelihood;
                }
//                Files.createSymbolicLink(MODEL_PATH.resolve("adopted" + iRun + ".inf"), lastAdoptedPath.getFileName());
                writer.write(iRun + " " + lastAdoptedPath.getFileName());
                writer.newLine();
            }
        }
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(WORK_DIR.resolve("likelihood.txt")))) {
            for (int i = 0; i < likelihoods.length; i++)
                printWriter.println(i + " " + likelihoods[i]);
        }
    }

}
