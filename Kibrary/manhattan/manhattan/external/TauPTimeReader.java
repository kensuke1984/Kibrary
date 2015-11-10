package manhattan.external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JOptionPane;

import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * @version 0.0.1 taup_time からの情報 taup_timeは /usr/local/taup/bin 下にないと動かない
 * 
 * @version 0.1.0 可変長引数による型指定。
 * @since 2013/7/12
 * 
 * @version 0.1.1 /usr/local 下からTauP*というフォルダを探してその中にbin/taup_timeがあればそれを使う
 *          なければプロンプトで尋ねる。 (それが taup_timeという名前でないとだめ)
 * 
 * @version 0.2.0
 * @since 2014/5/15 {@link InputStreamThread}の実装
 *        {@link #getTauPPhase(double, double, TauPPhaseName...)}の実装 シンクロトンにした
 * 
 * @version 0.2.1
 * @since 2015/1/23 {@link #getTauPPhase(double, double, Set)} installed
 * 
 * @version 0.2.1.1
 * @since 2015/4/2
 * 
 * @version 0.2.2
 * @since 2015/7/2 TauP can be in PATH
 * 
 * @version 0.2.3
 * @since 2015/8/25
 * 
 * 
 * @author kensuke
 * 
 */
public final class TauPTimeReader {

	private TauPTimeReader() {
	}

	private static String path;

	static {
		initialize();
	}

	private static void initialize() {
		if (System.getenv("PATH").contains("TauP")) {
			path = "taup_time";
			return;
		}
		Path usrLocal = Paths.get("/usr/local");
		try {
			Files.list(usrLocal).filter(path -> path.toString().toLowerCase().contains("taup")).findAny()
					.ifPresent(path -> {
						TauPTimeReader.path = path.toString() + "/bin/taup_time";
					});
			if (path != null)
				return;
		} catch (Exception e) {
		}

		do {
			String path = null;
			try {
				path = JOptionPane.showInputDialog("Where is \"taup_time?\" (Full path)", path);
			} catch (Exception e) {
				System.out.println("Where is \"taup_time\" (Full path)?");
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(new CloseShieldInputStream(System.in)))) {
					path = br.readLine().trim();
					// if (!path.startsWith("/"))
					// path = System.getProperty("user.dir") + "/" + path;
				} catch (Exception e2) {
					e2.printStackTrace();
					throw new RuntimeException();
				}
			}
			if (path == null || path.equals(""))
				throw new RuntimeException();

			if (path.endsWith("taup_time") && Files.exists(Paths.get(path))) {
				TauPTimeReader.path = path;
				break;
			}
		} while (true);
	}

	/**
	 * @param eventR
	 *            radius (km) !!not depth from the surface!!
	 * @param epicentralDistance
	 *            [deg] targetDistance
	 * @param phases
	 *            to look for
	 * @return travel times for the phase if theres a multiplication, all values
	 *         will be returned
	 */
	public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, TauPPhaseName... phases) {
		Set<TauPPhaseName> phaseSet = new HashSet<>(Arrays.asList(phases));
		return getTauPPhase(eventR, epicentralDistance, phaseSet);
	}

	/**
	 * @param eventR
	 *            radius of seismic source !!not depth from the surface!!
	 * @param epicentralDistance
	 *            [deg] target epicentral distance
	 * @param phaseSet
	 *            set of seismic phase.
	 * @return {@link Set} of TauPPhases.
	 */
	public static Set<TauPPhase> getTauPPhase(double eventR, double epicentralDistance, Set<TauPPhaseName> phaseSet) {
		return toPhase(operateTauPTime(eventR, epicentralDistance, phaseSet));
	}

	/**
	 * TauPの結果の出力を読み込む
	 * 
	 * @param eventR
	 * @param epicentralDistance
	 * @param phase
	 * @return result lines
	 */
	private static List<String> operateTauPTime(double eventR, double epicentralDistance, Set<TauPPhaseName> phase) {
		String[] cmd = makeCMD(eventR, epicentralDistance, phase);
		// System.out.println();
		ProcessBuilder pb = new ProcessBuilder(cmd);
		// System.exit(0);
		try {
			Process p = pb.start();
			InputStreamThread standard = new InputStreamThread(p.getInputStream());
			InputStreamThread error = new InputStreamThread(p.getErrorStream());
			error.start();
			standard.start();
			p.waitFor();
			standard.join();
			error.join();
			// System.out.println("hi");
			return standard.getStringList();
		} catch (Exception e) {
			System.out.println("Error occured");
			System.out.println("could not find the time");
			e.printStackTrace();
			return null;
		}
	}

	private static Set<TauPPhase> toPhase(List<String> lines) {
		if (lines == null || lines.size() <= 6)
			return Collections.emptySet();
		return IntStream.range(5, lines.size() - 1).mapToObj(lines::get).map(TauPTimeReader::toPhase)
				.collect(Collectors.toSet());
	}

	/**
	 * Distance Depth Phase Travel Ray Param Takeoff Incident Purist distance
	 * Purist name の順ではいっている文
	 * 
	 * @param line
	 * @return
	 */
	private static TauPPhase toPhase(String line) {
		String[] parts = line.trim().split("\\s+");
		// System.out.println(line+"hi");
		double distance = Double.parseDouble(parts[0]);
		double depth = Double.parseDouble(parts[1]);
		TauPPhaseName phaseName = TauPPhaseName.valueOf(parts[2]);
		double travelTime = Double.parseDouble(parts[3]);
		double rayParameter = Double.parseDouble(parts[4]);
		double takeoff = Double.parseDouble(parts[5]);
		double incident = Double.parseDouble(parts[6]);
		double puristDistance = Double.parseDouble(parts[7]);
		// parts[8] = "="
		String puristName = parts[9];
		// System.exit(0);
		return new TauPPhase(distance, depth, phaseName, travelTime, rayParameter, takeoff, incident, puristDistance,
				puristName);
	}

	/**
	 * TauPに投げる命令文を作る
	 * 
	 * @param eventR
	 *            [km] RADIUS (NOT depth from the surface)
	 * @param epicentralDistance
	 *            [deg]
	 * @param phase
	 * @return
	 */
	private static String[] makeCMD(double eventR, double epicentralDistance, Set<TauPPhaseName> phases) {
		Object[] phaseO = phases.toArray();
		StringBuffer phase = new StringBuffer(phaseO[0].toString());
		for (int i = 1; i < phaseO.length; i++)
			phase.append("," + phaseO[i].toString());

		String cmd = path + " -h " + (6371 - eventR) + " -deg " + epicentralDistance + " -model prem -ph " + phase;
		// System.out.println(cmd);
		return cmd.split("\\s+");
	}

}
