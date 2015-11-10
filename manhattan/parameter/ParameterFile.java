package parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * パラメタファイルの読み込み
 * 
 * @version 0.0.2
 * @since 2013/9/19
 * 
 * @version 0.0.3
 * @since 2014/8/19 Constructors changed
 * 
 * @since 2014/9/10
 * @version 0.0.4 getFile installed
 * 
 * @version 0.0.5
 * @since 2015/8/8 {@link Path} base
 * 
 * @version 0.0.5.1
 * @since 2015/8/13
 * 
 * @author kensuke
 * 
 * 
 * 
 */
abstract class ParameterFile {

	protected static void setExecutable(Path path) throws IOException {
		Set<PosixFilePermission> permission = Files.getPosixFilePermissions(path);
		permission.add(PosixFilePermission.OWNER_EXECUTE);
		Files.setPosixFilePermissions(path, permission);
	}

	protected ParameterReader reader;

	/**
	 * @return if there are enough necessary elements
	 */
	abstract boolean checkElements();


	protected ParameterFile(Path parameterPath) throws IOException{
		reader = new ParameterReader(parameterPath);
		// this.parameterFile = parameterFile;
	}

	protected ParameterFile() {
	}

	public Path getParameterPath() {
		return reader.getParameterPath();
	}

	/**
	 * ファイル名を読み込む
	 * 
	 * @return
	 */
	static Path readFileName() {
		String s = null;
		Path tmp = null;
		do {
			try {
				s = JOptionPane.showInputDialog("An output template filename?", s);
			} catch (Exception e) {
				System.out.println("An output template filename?");
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(new CloseShieldInputStream(System.in)));) {
					s = br.readLine().trim();
					if (!s.startsWith("/"))
						s = System.getProperty("user.dir") + "/" + s;
				} catch (Exception e2) {
					e2.printStackTrace();
					System.exit(0);
				}
			}
			if (s == null || s.equals(""))
				System.exit(0);
			else
				tmp = Paths.get(s);
			// System.out.println(tmp.getAbsolutePath());
			if (tmp.getParent() == null) {
				tmp = tmp.toAbsolutePath();
			} else if (!Files.exists(tmp.getParent()))
				continue;
		} while (Files.exists(tmp));
		// System.out.println(tmp);
		// System.out.println(tmp.canWrite());
		return tmp;
	}

	protected Path workPath;

	/**
	 * @param key
	 *            a name of parameter
	 * @return {@link Path} of the (1st) value for the key under workDir or null
	 *         if key has no values
	 */
	Path getPath(String key) {
		String name = reader.getString(key);
		if (name == null)
			return null;
		Path path = null;
		if (name.startsWith("/"))
			path = Paths.get(name);
		else if (workPath != null)
			path = workPath.resolve(name);
		else
			path = Paths.get(System.getProperty("user.dir"), name);
		// System.out.println(workDir+" "+file);
		// System.exit(0);
		return path;
	}

	static boolean exists(String command) {
		String[] paths = System.getenv("PATH").split(":");

		for (String path : paths)
			if (Files.exists(Paths.get(path, command)))
				return true;

		return false;

	}

}
