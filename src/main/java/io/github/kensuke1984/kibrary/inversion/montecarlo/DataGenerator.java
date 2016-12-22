package io.github.kensuke1984.kibrary.inversion.montecarlo;

/**
 * @author Kensuke Konishi
 * @version 0.0.2
 */
public interface DataGenerator<M, D> {
    D generate(M model);
}
