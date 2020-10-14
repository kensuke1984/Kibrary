package io.github.kensuke1984.kibrary.util.globalcmt;

import io.github.kensuke1984.kibrary.Environment;
import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.io.input.CloseShieldInputStream;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Catalog of global CMT solutions.
 * <p>
 * The catalog contains list of events
 * from <b>1976 January - 2017 September</b>.
 * TODO add the latest catalogs
 *
 * @author Kensuke Konishi
 * @version 0.1.8
 */
public final class GlobalCMTCatalog {

    private final static Set<NDK> NDKs;
    private final static Path CATALOG_PATH = Environment.KIBRARY_HOME.resolve("share/globalcmt.catalog"); //globalcmt.catalog linacmt.catalog synthetics.catalog NDK_no_rm200503211243A NDK_CMT_20170807.catalog

    static {
        Set<NDK> readSet = readCatalog();
        if (null == readSet) readSet = read(selectCatalogFile());
        NDKs = Collections.unmodifiableSet(readSet);
    }

    private GlobalCMTCatalog() {
    }

    private static Path selectCatalogFile() {
        Path catalogFile;
        String path = System.getProperty("user.dir");
        do {
            try {
                path = JOptionPane.showInputDialog("A catalog filename?", path);
            } catch (Exception e) {
                System.err.println("A catalog filename?");
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(new CloseShieldInputStream(System.in)))) {
                    path = br.readLine().trim();
                    if (!path.startsWith("/")) path = System.getProperty("user.dir") + "/" + path;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    throw new RuntimeException("No catalog.");
                }
            } finally {
                catalogFile = Paths.get(path);
            }
        } while (!Files.exists(catalogFile));
        return catalogFile;
    }

    private static Set<NDK> readCatalog() {
        try {
            if (!Files.exists(CATALOG_PATH)) downloadCatalog();
            List<String> lines = Files.readAllLines(CATALOG_PATH);
            if (lines.size() % 5 != 0) throw new Exception("Global CMT catalog is broken.");
            return IntStream.range(0, lines.size() / 5).mapToObj(
                    i -> NDK.read(lines.get(i * 5), lines.get(i * 5 + 1), lines.get(i * 5 + 2), lines.get(i * 5 + 3),
                            lines.get(i * 5 + 4))).collect(Collectors.toSet());
        } catch (NullPointerException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void downloadCatalog() throws IOException {
        Utilities.download(new URL("https://bit.ly/3bl0Ly9"), CATALOG_PATH, false);
    }

    /**
     * read catalogFile and returns NDKs inside.
     *
     * @param catalogPath path of a catalog
     * @return NDKs in catalogFile
     */
    static Set<NDK> read(Path catalogPath) {
        try {
            List<String> lines = Files.readAllLines(catalogPath);
            if (lines.size() % 5 != 0) throw new Exception(catalogPath + " is invalid.");
            return IntStream.range(0, lines.size() / 5).mapToObj(
                    i -> NDK.read(lines.get(i * 5), lines.get(i * 5 + 1), lines.get(i * 5 + 2), lines.get(i * 5 + 3),
                            lines.get(i * 5 + 4))).collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param id for the NDK
     * @return NDK of the input id
     */
    static NDK getNDK(GlobalCMTID id) {
        return NDKs.parallelStream().filter(ndk -> ndk.getGlobalCMTID().equals(id)).findAny()
                .orElseThrow(() -> new RuntimeException("No information for " + id));
    }

    /**
     * @return <b>(Unmodifiable)</b>Set of all NDKs
     */
    static Set<NDK> allNDK() {
        return NDKs;
    }
    
    public static Path getCatalogPath() {
    	return CATALOG_PATH;
    }

}
