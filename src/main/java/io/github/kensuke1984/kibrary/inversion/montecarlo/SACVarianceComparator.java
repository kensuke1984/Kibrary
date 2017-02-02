package io.github.kensuke1984.kibrary.inversion.montecarlo;

import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by kensuke on 16/12/21.
 * <b>Assume that there are no stations with the same name but
 * different networks</b>
 *
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class SACVarianceComparator implements DataComparator<SACData[]> {

    private final double OBS2;
    private final SACData[] OBSERVED_DATASET;
    private final double SIGMA = 0.5;


    SACVarianceComparator(Path obsDir) throws IOException {
        OBSERVED_DATASET = readObserved(obsDir);
        OBS2 = Arrays.stream(OBSERVED_DATASET).map(SACData::getData).flatMapToDouble(Arrays::stream)
                .reduce(0, (i, j) -> i + j * j);
    }

    private SACData[] readObserved(Path obsDir) throws IOException {
        SACFileName[] names = Utilities.sacFileNameSet(obsDir).stream().filter(SACFileName::isOBS)
                .sorted(Comparator.comparing(File::getName)).toArray(SACFileName[]::new);
        SACData[] dataset = new SACData[names.length];
        for (int i = 0; i < names.length; i++)
            dataset[i] = names[i].read();
        return dataset;
    }

    private boolean same(SACData data1, SACData data2) {
        return data1.getGlobalCMTID().equals(data2.getGlobalCMTID()) && data1.getStation().equals(data2.getStation()) &&
                data1.getComponent() == data2.getComponent();
    }

    private double computeVariance(SACData[] synSAC) {
        double numerator = 0;
        for (int j = 0; j < synSAC.length; j++) {

            double[] obs = OBSERVED_DATASET[j].getData();
            double[] syn = synSAC[j].getData();
            for (int i = 0; i < obs.length; i++)
                numerator += (obs[i] - syn[i]) * (obs[i] - syn[i]);
        }
        return numerator / OBS2;
    }

    /**
     * @param dataset to compute likelihood with
     * @return if there are problems for computing likelihood of the dataset
     */
    private boolean hasProblems(SACData[] dataset) {
        if (dataset.length != OBSERVED_DATASET.length) return true;
        for (int i = 0; i < dataset.length; i++)
            if (!same(OBSERVED_DATASET[i], dataset[i])) return true;
        return false;
    }

    @Override
    public double likelihood(SACData[] data) {
        if (!hasProblems(data)) throw new RuntimeException("Invalid dataset");
        return Math.exp(-2 * computeVariance(data) / SIGMA);
    }

}
