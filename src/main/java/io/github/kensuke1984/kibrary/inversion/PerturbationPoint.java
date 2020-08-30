package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.Location;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 深さを含んだ実際にインバージョンで求める摂動点情報
 * <p>
 * dr dlatitude dlongitude で dVを作る
 * <p>
 * TODO 名前のチェック validity
 *
 * @author Kensuke Konishi
 * @version 0.1.3
 */
public class PerturbationPoint extends HorizontalPoint {

    private double dR;

    private double dLatitude;

    private double dLongitude;

    private Map<Location, Double> volumeMap;
    /**
     * locations of points perturbationLocation[i] = the location of the i th point
     */
    private Location[] perturbationLocation;
    /**
     * number of points
     */
    private int pointN;
    /**
     * names of points pointName[i] = name of the i th point
     */
    private String[] pointName;
    /**
     * perturbation point file <br>
     * <p>
     * e.g. XY??? r
     */
    private final File PERTURBATION_POINT_FILE;

    /**
     * @param horizontalPointFile   {@link File} for {@link HorizontalPoint}
     * @param perturbationPointFile {@link File} for link PerturbationPoint}
     * @throws NoSuchFileException if any
     */
    public PerturbationPoint(File horizontalPointFile, File perturbationPointFile) throws NoSuchFileException {
        super(horizontalPointFile);
        PERTURBATION_POINT_FILE = perturbationPointFile;
        readPerturbationPointFile();
    }

    /**
     * @param args dir dR dLatitude dLongitude
     * @throws FileAlreadyExistsException if any
     * @throws NoSuchFileException        if any
     */
    public static void main(String[] args) throws FileAlreadyExistsException, NoSuchFileException {
        if (args.length != 4)
            throw new IllegalArgumentException("dir dR dLatitude dLongitude");
        File dir = new File(args[0]);
        PerturbationPoint pp =
                new PerturbationPoint(new File(dir, "horizontalPoint.inf"), new File(dir, "perturbationPoint.inf"));
        pp.dR = Double.parseDouble(args[1]);
        pp.dLatitude = Double.parseDouble(args[2]);
        pp.dLongitude = Double.parseDouble(args[3]);
        pp.createUnknownParameterSetFile(new File(dir, "unknown.inf"));
    }

    /**
     * 全てのr、水平分布に対しての摂動点情報のperturbationInfoファイルを書く
     *
     * @param r              array of radius
     * @param horizontalInfo file for {@link HorizontalPoint}
     * @param out            for write
     * @throws NoSuchFileException if any
     */
    public static void createPerturbationPoint(double[] r, File horizontalInfo, File out) throws NoSuchFileException {
        HorizontalPoint hp = new HorizontalPoint(horizontalInfo);
        Set<String> pointNameSet = hp.getHorizontalPointNameSet();
        // Map<String, HorizontalPosition> pointMap = hp.getPerPointMap();
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(out)))) {
            for (String name : pointNameSet)
                for (double aR : r) pw.println(name + " " + aR);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void printVolumes() {
        for (Location perLoc : perturbationLocation)
            System.out.println(perLoc + " " + volumeMap.get(perLoc));
    }

    /**
     * compute volumes for points
     */
    public void computeVolumes() {
        volumeMap = new HashMap<>();
        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (Location perLoc : perturbationLocation)
            es.execute(new VolumeCalculator(perLoc));

        es.shutdown();
        while (!es.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // TODO
    public void createUnknownParameterSetFile(File outFile) throws FileAlreadyExistsException {
        if (outFile.exists()) throw new FileAlreadyExistsException(outFile.getPath());

        computeVolumes();

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))) {
            for (Location loc : perturbationLocation) {
                System.err.println(loc);
                double volume = volumeMap.get(loc);
                pw.println("MU " + loc + " " + volume);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDr(double dr) {
        dR = dr;
    }

    public void setDlatitude(double dlatitude) {
        dLatitude = dlatitude;
    }

    public void setDlongitude(double dlongitude) {
        dLongitude = dlongitude;
    }

    public Map<Location, Double> getVolumeMap() {
        return volumeMap;
    }

    public String[] getPointName() {
        return pointName;
    }

    public File getFile() {
        return PERTURBATION_POINT_FILE;
    }

    /**
     * @return array of radius
     */
    public double[] getR() {
        return Arrays.stream(perturbationLocation).mapToDouble(Location::getR).toArray();
    }

    /**
     * @param i index
     * @return radius [km] of the i th point
     */
    public double getR(int i) {
        return perturbationLocation[i].getR();
    }

    /**
     * read a perturbation point file<br>
     * XY??? r1 XY??? r2 .....
     */
    private void readPerturbationPointFile() {
        try {
            List<String> lines = FileUtils.readLines(PERTURBATION_POINT_FILE, Charset.defaultCharset());
            lines.removeIf(line -> line.trim().isEmpty() || line.trim().startsWith("#"));

            pointN = lines.size();
            perturbationLocation = new Location[pointN];
            pointName = new String[pointN];

            for (int i = 0; i < pointN; i++) {
                String[] parts = lines.get(i).split("\\s+");
                pointName[i] = parts[0];
                double r = Double.parseDouble(parts[1]);
                perturbationLocation[i] = new Location(getHorizontalPosition(pointName[i]).getLatitude(),
                        getHorizontalPosition(pointName[i]).getLongitude(), r);
                // perturbationLocation[i].setR(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Location[] getPerturbationLocation() {
        return perturbationLocation;
    }

    public int getPointN() {
        return pointN;
    }

    /**
     * @param location {@link Location} for target
     * @return Location[] in order of the distance from an input location from the closest
     */
    public Location[] getNearestLocation(Location location) {
        Location[] locations = Arrays.copyOf(perturbationLocation, perturbationLocation.length);
        Arrays.sort(locations, Comparator.comparingDouble(o -> o.getDistance(location)));
        return locations;
    }

    private class VolumeCalculator implements Runnable {

        private final Location LOC;

        private VolumeCalculator(Location loc) {
            LOC = loc;
        }

        @Override
        public void run() {
            volumeMap.put(LOC, Earth.getVolume(LOC, dR, dLatitude, dLongitude));
            System.err.println(LOC);
        }

    }

}
