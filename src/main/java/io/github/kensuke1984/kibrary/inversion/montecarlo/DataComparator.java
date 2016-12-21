package io.github.kensuke1984.kibrary.inversion.montecarlo;


/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
interface DataComparator<D> {

    /**
     * @param dataset to compute likelihood for
     * @return likelihood
     */
    double likelihood(D[] dataset);

}
