package io.github.kensuke1984.kibrary.inversion.montecarlo;


/**
 * @author Kensuke Konishi
 * @version 0.0.2
 */
public interface DataComparator<D> {

    /**
     * @param sigma &sigma;
     * @return exp (-2*variance/&sigma;)
     */
    static DataComparator<Double> varianceComparator(double sigma) {
        return variance -> Math.exp(-2 * variance / sigma);
    }

    /**
     * @param data to compute likelihood for
     * @return likelihood
     */
    double likelihood(D data);
}
