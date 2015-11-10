package manhattan.globalcmt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * localに保存してあるGlobal CMTのカタログ
 * 
 * Catalog of global CMT solutions.
 * 
 * 
 * @since 2013/12/1
 * @version 0.0.1
 * 
 * 
 * @version 0.0.2
 * @since 2014/1/16 if there is no catalogFile at the default location, you can
 *        put file path.
 * 
 * @version 0.0.3
 * @since 2014/6/3 {@link #read()}{@link #read(File)} installed
 * 
 * @version 0.0.4
 * @since 2015/3/18 {@link #getNDK(GlobalCMTID)} modified.
 * 
 * @version 0.0.4.1
 * @since 2015/4/9
 * 
 * @version 0.0.5
 * @since 2015/8/3 The path of catalog is changed.
 * 
 * @version 0.1.0
 * @since 2015/9/15 static..
 * 
 * @author kensuke
 * 
 */
final class GlobalCMTCatalog {

	/**
	 * 読み込んだNDK
	 */
	private final static Set<NDK> NDKs;

	private static Path selectCatalogFile() {
		Path catalogFile = null;
		String path = System.getProperty("user.dir");
		do {
			try {
				path = JOptionPane.showInputDialog("A catalog filename?", path);
			} catch (Exception e) {
				System.out.println("A catalog filename?");
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(new CloseShieldInputStream(System.in)))) {
					path = br.readLine().trim();
					if (!path.startsWith("/"))
						path = System.getProperty("user.dir") + "/" + path;
				} catch (Exception e2) {
					e2.printStackTrace();
					throw new RuntimeException("No catalog.");
				}
			} finally {
				catalogFile = Paths.get(path);
			}
		} while (catalogFile == null || !Files.exists(catalogFile));
		return catalogFile;
	}

	private static Set<NDK> readJar() {
		try {
			List<String> lines = IOUtils
					.readLines(GlobalCMTCatalog.class.getClassLoader().getResourceAsStream("globalcmt.catalog"));
			if (lines.size() % 5 != 0)
				throw new Exception("Global CMT catalog contained in the jar file is broken");
			return IntStream
					.range(0, lines.size() / 5).mapToObj(i -> NDK.read(lines.get(i * 5), lines.get(i * 5 + 1),
							lines.get(i * 5 + 2), lines.get(i * 5 + 3), lines.get(i * 5 + 4)))
					.collect(Collectors.toSet());
		} catch (NullPointerException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private GlobalCMTCatalog() {
	}

	static {
		Set<NDK> readSet = readJar();
		if (null == readSet)
			readSet = read(selectCatalogFile());
		NDKs = Collections.unmodifiableSet(readSet);
	}

	/**
	 * read catalogFile and returns NDKs inside.
	 * 
	 * @param catalogPath
	 * @return NDKs in catalogFile
	 */
	static Set<NDK> read(Path catalogPath) {
		try {
			List<String> lines = Files.readAllLines(catalogPath);
			if (lines.size() % 5 != 0)
				throw new Exception(catalogPath + " is invalid.");
			return IntStream
					.range(0, lines.size() / 5).mapToObj(i -> NDK.read(lines.get(i * 5), lines.get(i * 5 + 1),
							lines.get(i * 5 + 2), lines.get(i * 5 + 3), lines.get(i * 5 + 4)))
					.collect(Collectors.toSet());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * idと一致するNDKを返す
	 * 
	 * @param id
	 *            for the NDK
	 * @return NDK of the input id
	 */
	static NDK getNDK(GlobalCMTID id) {
		return NDKs.parallelStream().filter(ndk -> ndk.getID().equals(id)).findAny()
				.orElseThrow(() -> new RuntimeException("There is no information for " + id));
	}

	/**
	 * @return <b>(Unmodifiable)</b>Set of all NDKs
	 */
	static Set<NDK> allNDK() {
		return NDKs;
	}

}
