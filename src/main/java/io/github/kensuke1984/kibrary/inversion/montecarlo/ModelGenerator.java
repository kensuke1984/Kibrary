package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * Interface for generating models.
 *
 * @author Kensuke Konishi
 * @version 0.0.2
 */
public interface ModelGenerator<M> {

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
     *
     * @param path    file name of the write
     * @param model   to write in the path
     * @param options options for writing
     */
    default void write(Path path, M model, OpenOption... options) throws IOException {
        Files.write(path, toString(model).getBytes(), options);
    }

    /**
     * This method is used by {@link #write(Path, Object, OpenOption...)} as default.
     *
     * @param model to write
     * @return string describing the model
     */
    String toString(M model);

}
