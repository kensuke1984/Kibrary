package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.NoSuchFileException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 摂動点情報 ポイント名に対するLocation XY?? lat lon
 * <p>
 * TODO DSM informationとして書き出す
 *
 * @author Kensuke Konishi
 * @version 0.1.3
 */
public class HorizontalPoint {

    /**
     * map of point name and position
     */
    private Map<String, HorizontalPosition> perPointMap;

    private final File INFOFILE;

    public HorizontalPoint(File infoFile) throws NoSuchFileException {
        if (!infoFile.exists()) throw new NoSuchFileException(infoFile.getPath());
        INFOFILE = infoFile;
        readFile();
    }

    public Set<String> getHorizontalPointNameSet() {
        return perPointMap.keySet();
    }

    public Map<String, HorizontalPosition> getPerPointMap() {
        return perPointMap;
    }

    /**
     * @return name of file
     */
    public File getINFOFILE() {
        return INFOFILE;
    }

    private void readFile() {
        Map<String, HorizontalPosition> perPointMap = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(INFOFILE))) {
            String line;
            while (null != (line = br.readLine())) {
                line = line.trim();
                // skip when a line starts with #
                // skip when a line does not contain anything
                if (line.startsWith("#") || line.isEmpty()) continue;
                HorizontalPosition loc = new HorizontalPosition(Double.parseDouble(line.trim().split("\\s+")[1]),
                        Double.parseDouble(line.trim().split("\\s+")[2]));
                perPointMap.put(line.trim().split("\\s+")[0], loc);
            }
        } catch (Exception e) {
            throw new RuntimeException(INFOFILE + " is not valid");
        }
        this.perPointMap = perPointMap;
    }

    /**
     * @param horizontalPointName (ex. XY???)
     * @return HorizontalPosition of an input name
     */
    public HorizontalPosition getHorizontalPosition(String horizontalPointName) {
        return perPointMap.get(horizontalPointName);
    }

    /**
     * @param pos for searching the name
     * @return the point name for an input HorizontalPosition
     */
    public String toString(HorizontalPosition pos) {
        for (String str : perPointMap.keySet())
            if (pos.equals(perPointMap.get(str))) return str;
        System.err.println("could not find the point");
        return null;
    }

    /**
     * @param position target {@link HorizontalPosition}
     * @return positionに近い順でポイントを返す
     */
    public String[] getNearestPoints(HorizontalPosition position) {
        return perPointMap.keySet().stream()
                .sorted(Comparator.comparingDouble(o -> perPointMap.get(o).getEpicentralDistance(position)))
                .toArray(String[]::new);
    }
}
