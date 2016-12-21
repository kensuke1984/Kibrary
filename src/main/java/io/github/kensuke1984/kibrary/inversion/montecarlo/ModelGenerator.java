package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for generating models.
 *
 * @author Kensuke Konishi
 * @version 0.0.1
 */
interface ModelGenerator<M> {
    /**
     * @param current model to be based for the next one.
     * @return the next model
     */
    M createNextModel(M current);

    /**
     * @return Creates a first model.
     */
    M firstModel();

    /**
     * Write a model on a file.
     * @param path file name of the output
     * @param model to write in the path
     */
    void write(Path path, M model) throws IOException;
}
