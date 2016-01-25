package parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * 
 * 各種設定ファイルからパラメタの値を読み込む Stringとして読み込む
 * 
 * #で始まる行は無視、 一つの値しか書いていない場合も無視
 * 
 * 区切り文字は スペースかタブ If there are no values, the key is also ignored.
 * 
 * @version 0.0.4
 * 
 * @author kensuke
 * 
 */
class ParameterReader {

	private Map<String, String[]> parameterMap = new HashMap<>();
	private Path parameterPath;

	/**
	 * if parameterFile is null or does not exist, choosing dialog will be
	 * shown.
	 * 
	 * @param parameterFile
	 *            parameter file
	 */
	ParameterReader(Path parameterPath) throws IOException {
		if (parameterPath == null || !Files.exists(parameterPath))
			showDialog();
		else
			this.parameterPath = parameterPath;
		readParameterFile();
	}

	/**
	 * show dialog for selecting an input file
	 */
	private void showDialog() throws IOException {

		String filename = null;
		Path path = null;
		boolean exists = false;
		do {
			try {
				filename = JOptionPane.showInputDialog("What is the parameter file name", filename);
			} catch (Exception e) {
				System.out.println("What is the parameter file name?");
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(new CloseShieldInputStream(System.in)))) {
					filename = br.readLine().trim();
					if (!filename.startsWith("/"))
						filename = System.getProperty("user.dir") + "/" + filename;
				} catch (Exception e2) {
					e2.printStackTrace();
					System.exit(0);
				}
			}
			if (filename == null || filename.equals(""))
				System.exit(0);
			path = Paths.get(filename);
			exists = Files.exists(path);
			if (!exists) {
				System.out.println(filename + " ...... no such file.");
				Path parent = path.getParent();
				if (parent != null && Files.exists(parent))
					filename = find(path);
			}
			if (Files.isDirectory(path))
				System.out.println(filename + " is a directory...");
		} while (!exists || Files.isDirectory(path));
		parameterPath = path;
		// readParameterFile();

	}

	private static String find(Path path) throws IOException {
		Path parent = path.getParent();
		String s = path.toString();
		try (Stream<Path> listStream = Files.list(parent)) {
			Path[] candidates = listStream
					.filter(candidate -> candidate.getFileName().toString().startsWith(path.getFileName().toString()))
					.sorted().toArray(n -> new Path[n]);
			Arrays.stream(candidates).forEach(System.out::println);
			if (candidates.length == 1)
				s = candidates[0].toString();
		}
		return s;
	}

	Path getParameterPath() {
		return parameterPath;
	}

	/**
	 * パラメタファイルを読み込む １つ目は鍵 ２つ目は値。 空行 ＃で始まる行は無視
	 */
	private void readParameterFile() {
		try (BufferedReader br = Files.newBufferedReader(parameterPath)) {
			String line = null;
			while (null != (line = br.readLine())) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;

				// System.out.println(line);
				String[] parts = line.trim().split("\\s+");

				//
				if (parts.length < 2)
					continue;
				if (parameterMap.containsKey(parts[0])) {
					System.out.println("Duplicate is found :" + parts[0]);
					continue;
				}
				// System.out.println(parts.length);
				String[] values = new String[parts.length - 1];
				for (int i = 1; i < parts.length; i++)
					values[i - 1] = parts[i];

				parameterMap.put(parts[0], values);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param key
	 *            a name of parameter
	 * @return (1st) {@link String} for the key or null if no key
	 */
	String getString(String key) {
		if (!parameterMap.containsKey(key))
			return null;
		return parameterMap.get(key)[0];
	}

	/**
	 * @param key
	 *            a name of parameter
	 * @return (1st) double for the key or {@link Double#NaN} if the value is
	 *         not a double
	 */
	double getDouble(String key) {
		try {
			return Double.parseDouble(parameterMap.get(key)[0]);
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	int getInt(String key) {
		return Integer.parseInt(parameterMap.get(key)[0]);
	}

	/**
	 * @param key
	 *            a name of parameters
	 * @return Array of {@link String} for the key
	 */
	String[] getStringArray(String key) {
		return parameterMap.get(key);
	}

	boolean containsKey(String key) {
		return parameterMap.containsKey(key);
	}

	/**
	 * @param keys
	 * @return keysの値をすべて含んでいるか
	 */
	boolean containsAll(Set<String> keys) {
		boolean isOK = true;
		// System.out.println("Checking if "+parameterFile+" has enough
		// information");
		for (String key : keys)
			if (!parameterMap.containsKey(key)) {
				isOK = false;
				System.out.println("There is no information about " + key + " "
						+ Thread.currentThread().getStackTrace()[1].getClassName() + "#"
						+ Thread.currentThread().getStackTrace()[1].getMethodName());
			}

		return isOK;
	}

}
