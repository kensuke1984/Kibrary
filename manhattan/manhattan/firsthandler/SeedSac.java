package manhattan.firsthandler;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;

import filehandling.sac.SACHeaderEnum;
import filehandling.sac.SACUtil;
import manhattan.external.Sac;
import manhattan.globalcmt.GlobalCMTData;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.globalcmt.GlobalCMTSearch;
import manhattan.template.EventFolder;
import manhattan.template.Utilities;

/**
 * ある一つのシードファイルに対して読み込みSacまで解凍する SeedSac will open a seed file and creates sac
 * files included in the seed file.
 * 
 * @since 2014/1/7
 * @version 0.0.1
 * 
 *          seedのデータは日付を、またいでいないこととする 確認済み １ rdseed -rfd hogeによるRESP.*の出力は全く同じ
 *          *.SACはSCALEの値だけ異なる しかしIRIS HPによると現在は使われていないので無視 確認済み ２
 *          古いsacのバージョンと新しいsacのバージョンで rtrend やinterpolate 後のsacの値が多少違う
 * 
 *          seed解凍後 channelがBH[ENZ]のものだけから読み込む BH[123]は今のところ使わない TODO
 *          output先にすでに解凍済みのイベントがあったら走らない TODO NPTSで合わないものを捨てる？
 * 
 * @version 0.0.2 日付をまたげるようにした
 * 
 * 
 * @version 0.0.3
 * @since 2014/1/15 回転できた場合と回転できなかった場合で出力先を変更 delta cmpaz cmpincを調整
 * 
 * 
 *        TODO つなぎあわせ nptsを設定したらそれを超えた時点でつなぎ合わせストップ
 * 
 * 
 * @version 0.0.4
 * @since 2014/1/15 {@link #setEvalresp(File)} installed
 * 
 * @version 0.0.5
 * @since 2014/2/5 {@link #hadRun()} installed
 * 
 * @version 0.0.6
 * @since 2014/4/29 {@link #deconvolute()}においてevalRespの失敗に対応
 * 
 * 
 * @since 2014/9/7
 * @version 0.0.7 to Java 8
 * 
 * @version 0.0.8
 * @since 2014/10/2 Evalresp MUST be in PATH
 * 
 * @version 0.1.0
 * @since 2015/2/3 BLE BLN BLZ
 * 
 * @version 0.1.5
 * @since 2015/2/12 {@link Calendar} &rarr; {@link LocalDateTime}
 * 
 * @version 0.1.5.1
 * @since 2015/8/15 {@link IOException}
 * 
 * @version 0.1.6
 * @since 2015/8/19 {@link Path} base
 * 
 * @version 0.1.6.1
 * @since 2015/9/11 noSpectraOrInvalidMOD bug fixed
 * 
 * @version 0.1.7
 * @since 2015/9/17 Only N E
 * 
 * 
 * @author kensuke
 * 
 */
class SeedSac implements Runnable {

	Path getSeedPath() {
		return seedFile.getSeedPath();
	}

	/**
	 * if remove intermediate files
	 */
	private boolean removeIntermediateFiles = true;

	/**
	 * If true then all intermediate files will be removed at the end.
	 * 
	 * @param b
	 *            set {@link #removeIntermediateFiles}
	 */
	void setRemoveIntermediateFiles(boolean b) {
		removeIntermediateFiles = b;
	}

	/**
	 * If cmpMod is true, this modifies delta, cmpinc &amp; cmpaz
	 */
	private boolean cmpMod = true;

	/**
	 * delta for sac files. Sac files with different delta will be interpolated
	 * or downsampled.
	 */
	private static final double delta = 0.05;

	/**
	 * seedfileが梱包するイベントのデータ
	 */
	private GlobalCMTData event;

	/**
	 * このseedfileのイベントフォルダ
	 */
	private EventFolder eventDir;

	/**
	 * 処理するseedファイル
	 */
	private SEEDFile seedFile;

	/**
	 * 梱包するイベントの id
	 */
	private GlobalCMTID id;

	/**
	 * Sampling Hz in output SacFile
	 */
	private static final double samplingHz = 20;

	/**
	 * Minimum epicentral distance of output sac files 震央距離の最小値
	 */
	private double epicentralDistanceMin = 0;

	/**
	 * Maximum epicentral distance of output sac files 震央距離の最大値
	 */
	private double epicentralDistanceMax = 180;

	/**
	 * 解凍できるかどうか
	 */
	private boolean canGO;

	/**
	 * PDE時刻, 位置を基準に解凍するかどうか デフォルトはfalse (CMT)
	 */
	private boolean byPDE;

	/**
	 * 
	 * @param seedPath
	 *            解凍するseedファイル
	 * @param outputDirectoryPath
	 *            解凍先 ここにイベントのフォルダを作りその下に解凍 must exist
	 * @throws IOException
	 */
	SeedSac(Path seedPath, Path outputDirectoryPath) throws IOException {
		this(seedPath, outputDirectoryPath, null);
	}

	/**
	 * 解凍するseed file
	 * 
	 * @param seedPath
	 *            解凍するseedファイル
	 * @param outputDirectoryPath
	 *            解凍先 must exist
	 * @param id
	 *            global cmt id
	 * @throws IOException
	 */
	SeedSac(Path seedPath, Path outputDirectoryPath, GlobalCMTID id) throws IOException {
		this.seedFile = new SEEDFile(seedPath);
		if (id != null)
			this.id = id;
		else
			setID();
		if (!idValidity())
			throw new RuntimeException(this.id + " is invalid for " + seedPath);

		eventDir = new EventFolder(outputDirectoryPath.resolve(this.id.toString()));
		if (eventDir.exists() || !eventDir.mkdir())
			throw new FileAlreadyExistsException(eventDir.toString());

		// System.out.println(eventDir + " " + eventDir.exists());
		canGO = this.seedFile.toDirectory(eventDir.toPath());
	}

	/**
	 * set GlobalCMTID
	 */
	private void setID() {
		// try to find id in the name of the file
		id = findIDinFilename();
		if (id != null)
			return;
		GlobalCMTSearch sc = new GlobalCMTSearch(seedFile.getStartingDate().toLocalDate());
		sc.setStartTime(seedFile.getStartingDate().toLocalTime());
		sc.setEndTime(seedFile.getEndingDate().toLocalTime());
		id = sc.select();
		if (id == null) {
			System.out.println("There is no event in the global CMT catalogue");
			canGO = false;
		}
	}

	/**
	 * @return look for GlobalCMTID in the name of the seed file otherwise
	 *         returns null
	 */
	private GlobalCMTID findIDinFilename() {
		String fileName = seedFile.getSeedPath().getFileName().toString();
		// System.out.println(fileName);
		Matcher m1 = GlobalCMTID.RECENT_GLOBALCMTID_PATTERN.matcher(fileName);
		if (m1.find())
			return new GlobalCMTID(m1.group());

		Matcher m0 = GlobalCMTID.PREVIOUS_GLOBALCMTID_PATTERN.matcher(fileName);
		if (m0.find())
			return new GlobalCMTID(m0.group());

		return null;
	}

	/**
	 * global cmt id が日付的に合っているかどうか （startが発震時刻より前かつendがCMT時刻より後かどうか）
	 * 
	 * @return
	 */
	private boolean idValidity() {
		// System.out.print("Checking the id: " + id + " ... ");
		event = id.getEvent();
		return event != null && id != null && seedFile.getStartingDate().isBefore(event.getPDETime())
				&& seedFile.getEndingDate().isAfter(event.getCMTTime());
		// System.out.println(idValidity);
	}

	/**
	 * イベントフォルダ内すべてのMODファイルの装置関数をdeconvolutionする。
	 * 対応するRESPのevalrespに失敗したMODファイルはNOSPECTRAMODへ
	 */
	private void deconvolute() throws IOException {
		// System.out.println("Conducting deconvolution");

		Path noSpectraPath = eventDir.toPath().resolve("noSpectraOrInvalidMOD");

		// evalresp後のRESP.*ファイルを移動する TODO メソッドを分ける
		Path respBoxPath = eventDir.toPath().resolve("resp");
		Path spectraBoxPath = eventDir.toPath().resolve("spectra");
		Path modBoxPath = eventDir.toPath().resolve("mod");
		try (DirectoryStream<Path> eventDirStream = Files.newDirectoryStream(eventDir.toPath(), "*.MOD")) {
			String resp = "RESP.";
			String spectra = "SPECTRA.";
			for (Path modPath : eventDirStream) {
				Map<SACHeaderEnum, String> headerMap = SACUtil.readHeader(modPath);
				String componentName = headerMap.get(SACHeaderEnum.KCMPNM);
				String respFileName = resp + headerMap.get(SACHeaderEnum.KNETWK) + "."
						+ headerMap.get(SACHeaderEnum.KSTNM) + "." + headerMap.get(SACHeaderEnum.KHOLE) + "."
						+ componentName;
				String spectraFileName = spectra + headerMap.get(SACHeaderEnum.KNETWK) + "."
						+ headerMap.get(SACHeaderEnum.KSTNM) + "." + headerMap.get(SACHeaderEnum.KHOLE) + "."
						+ componentName;
				Path spectraPath = eventDir.toPath().resolve(spectraFileName);
				String component = null;

				switch (componentName) {
				case "BHE":
				case "BLE":
					component = "E";
					break;
				case "BHN":
				case "BLN":
					component = "N";
					break;
				case "BHZ":
				case "BLZ":
					component = "Z";
					break;
				case "BH1":
				case "BL1":
					component = "1";
					break;
				case "BH2":
				case "BL2":
					component = "2";
					break;
				default:
					continue;
				}

				String afterName = headerMap.get(SACHeaderEnum.KSTNM) + "." + event + "." + component;
				Path afterPath = eventDir.toPath().resolve(afterName);

				// evalresp を走らせる。
				// 失敗したらMOD, RESPファイルをtrashに
				if (!runEvalresp(headerMap)) {
					// SPECTRAをつくれなかったMOD.*ファイルをnoSpectraに移す
					Utilities.moveToDirectory(modPath, noSpectraPath, true);
					// SPECTRAをつくれなかったRESP.*ファイルをnoSpectraに移す
					Utilities.moveToDirectory(eventDir.toPath().resolve(respFileName), noSpectraPath, true);
					continue;
				}

				// seedsacを行う
				try {
					int npts = Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS));
					SacDeconvolution.compute(modPath, spectraPath, afterPath, samplingHz / npts, samplingHz);
				} catch (Exception e) {
					// SPECTRAをつくれなかったMOD.*ファイルをnoSpectraに移す
					Utilities.moveToDirectory(modPath, noSpectraPath, true);
					Utilities.moveToDirectory(spectraPath, noSpectraPath, true);
					// SPECTRAをつくれなかったRESP.*ファイルをnoSpectraに移す
					Utilities.moveToDirectory(eventDir.toPath().resolve(respFileName), noSpectraPath, true);
					continue;
				}

				// 処理の終わったRESP.*ファイルをrespBoxに移す
				Utilities.moveToDirectory(eventDir.toPath().resolve(respFileName), respBoxPath, true);

				// 処理の終わったSPCTRA.*ファイルをspectraBoxに移す
				Utilities.moveToDirectory(spectraPath, spectraBoxPath, true);

				// 処理の終わったMOD.*ファイルをmodBoxに移す
				Utilities.moveToDirectory(modPath, modBoxPath, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean canGO() {
		return canGO;
	}

	private boolean hadRun;

	/**
	 * @return if already run or not
	 */
	boolean hadRun() {
		return hadRun;
	}

	/**
	 * rdseed で解凍した波形ファイルのうちおかしなものを省く deltaがずれているものはすて、 波形がとびとびのものをmergeする
	 */
	private void mergeUnevenSac() throws IOException {
		// System.out.println("Merging in " + eventDir);
		// mergeする
		UnevenSacMerger u = new UnevenSacMerger(eventDir.toPath());
		u.merge();
		u.move();
		// System.exit(0);
	}

	/**
	 * イベントフォルダ内のmerge後のSACを修正する
	 * 
	 * @return
	 */
	private boolean modifySACs() {
		// System.out.println("Modifying sac files in "
		// + eventDir.getAbsolutePath());
		// イベントフォルダ内のSACを集める
		Path trashBoxPath = eventDir.toPath().resolve("trash");
		Path mergedBoxPath = eventDir.toPath().resolve("merged");

		// SacFileの検証
		try (DirectoryStream<Path> sacPathStream = Files.newDirectoryStream(eventDir.toPath(), "*.SAC")) {
			for (Path sacPath : sacPathStream) {
				// System.out.println(sacFile);
				SacModifier sm = new SacModifier(event, sacPath, byPDE);

				// header check
				if (!sm.canInterpolate() || !sm.checkHeader()) {
					// System.out.println(sacPath + " has too big gap to be
					// interpolated. or header problem");
					Utilities.moveToDirectory(sacPath, trashBoxPath, true);
					continue;
				}

				// Sacのトレンド除去 interpolationをして、.SAC > .MOD として書き込む first
				sm.preprocess();

				// タイムウインドウ補完
				sm.interpolate();

				// Sacを整える
				sm.rebuild();
				// System.exit(0);

				// 震央距離check
				if (!sm.checkEpicentralDistance(epicentralDistanceMin, epicentralDistanceMax)) {
					// System.out.println(sacFile+" is moved.");
					Utilities.moveToDirectory(sacPath, trashBoxPath, true);
					continue;
				}

				// 処理の終わった.SACファイルはmergedに
				Utilities.moveToDirectory(sacPath, mergedBoxPath, true);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;

	}

	/**
	 * SacファイルをE, N -> R, Tに回転する
	 * 
	 * eventDir内のすべての (.E, .N), (.1, .2)ファイルについて.R, .Tに回転する
	 * 
	 * 回転できたものは rotatedNEフォルダに できなかったものはnonrotatedNEに
	 * 
	 */
	private void rotate() {
		Path trashBox = eventDir.toPath().resolve("nonRotatedNE");
		Path neDir = eventDir.toPath().resolve("rotatedNE");
		try (DirectoryStream<Path> eStream = Files.newDirectoryStream(eventDir.toPath(), "*.E")) {
			for (Path ePath : eStream) {
				String[] parts = ePath.getFileName().toString().split("\\.");
				Path nPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".N");
				Path rPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".R");
				Path tPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".T");

				if (!Files.exists(nPath)) {
					Utilities.moveToDirectory(ePath, trashBox, true);
					continue;
				}
				boolean rotated = SACUtil.rotate(ePath, nPath, rPath, tPath);
				if (rotated) {
					Utilities.moveToDirectory(nPath, neDir, true);
					Utilities.moveToDirectory(ePath, neDir, true);
				} else {
					Utilities.moveToDirectory(ePath, trashBox, true);
					Utilities.moveToDirectory(nPath, trashBox, true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// If there are files (.N) which had no pair .E, move them to trash
		try (DirectoryStream<Path> nPaths = Files.newDirectoryStream(eventDir.toPath(), "*.N")) {
			for (Path nPath : nPaths)
				Utilities.moveToDirectory(nPath, trashBox, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * rdseedで解凍されてきた.SACファイルのdelta, cmpinc, cmpaz を整える TODO
	 * 整える前のファイルはrdseedoutputに補完する
	 */
	private void preprocess() throws IOException {
		Path backupPath = eventDir.toPath().resolve("rdseedOutputBackup");
		Files.createDirectories(backupPath);
		try (DirectoryStream<Path> sacPaths = Files.newDirectoryStream(eventDir.toPath(), "*.SAC")) {
			for (Path sacPath : sacPaths) {
				Files.copy(sacPath, backupPath.resolve(sacPath.getFileName()));
				// System.out.println("..illegal delta.");
				try {
					fixDelta(sacPath);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Set DELTA in a sac file to {@link #delta}. (mostly it is down sampling.)
	 * Change cmpaz cmpinc BHN BHE BHZ のときはcmpaz cmpincを変更する
	 * 
	 * @param sacPath
	 *            Path of a file to fix
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InterruptedException
	 *             if something during sac processing.
	 */
	private static void fixDelta(Path sacPath) throws IOException, InterruptedException {
		try (Sac sacD = Sac.createProcess()) {
			String cwd = sacPath.getParent().toString();
			sacD.inputCMD("cd " + cwd);// set current directory
			sacD.inputCMD("r " + sacPath.getFileName());// read
			sacD.inputCMD("ch lovrok true");// overwrite permission
			if (sacPath.toString().contains(".BHN.") || sacPath.toString().contains(".BLN."))
				sacD.inputCMD("ch cmpaz 0 cmpinc 90");
			else if (sacPath.toString().contains(".BHE.") || sacPath.toString().contains(".BLE."))
				sacD.inputCMD("ch cmpaz 90 cmpinc 90");
			else if (sacPath.toString().contains(".BHZ.") || sacPath.toString().contains(".BLZ."))
				sacD.inputCMD("ch cmpinc 0");
			sacD.inputCMD("interpolate delta " + delta);
			sacD.inputCMD("w over");
		}
	}

	public static void main(String args[]) throws IOException {
		Path source = Paths.get("/home/kensuke/test/200504190146A.sd");
		Path seed = Paths.get("/home/kensuke/test/200504190146A.seed");
		if (!Files.exists(seed))
			Files.copy(source, seed);
		SeedSac ss = new SeedSac(seed, seed.resolveSibling("short"));
		ss.removeIntermediateFiles = false;
		ss.run();
	}

	@Override
	public void run() {

		if (!canGO())
			throw new RuntimeException("The condition is no good.");

		System.out.println("Opening " + seedFile + " in " + eventDir);

		// run rdseed -q output -fRd
		try {
			seedFile.extract(seedFile.getSeedPath().getParent());
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

		try {
			if (cmpMod)
				// fix delta values
				preprocess();

			// merge uneven sacfiles
			mergeUnevenSac();
		} catch (IOException e) {
			throw new RuntimeException("Error on " + id, e);
		}

		// sacを修正する
		modifySACs();

		// Use only BH[12ENZ]
		// remove waveforms of .[~NEZ]
		selectChannels();

		// 装置関数のdeconvolution OK!
		try {
			deconvolute();
		} catch (IOException e) {
			throw new RuntimeException("Deconvolution error on " + id, e);
		}
		// System.exit(0);

		// .N, .E -> .R, .Tへの回転
		rotate();

		// あまりものをすてる
		toTrash();

		problem = check();

		hadRun = true;

		if (removeIntermediateFiles)
			removeIntermediateFiles();

		// System.exit(0);
		System.out.println("finish");
	}

	private void removeIntermediateFiles() {
		try {
			FileUtils.deleteDirectory(eventDir.toPath().resolve("merged").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("mod").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("rdseedOutputBackup").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("resp").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("rotatedNE").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("nonRotatedNE").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("spectra").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("trash").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("mergedUnevendata").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("invalidChannel").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("nonMergedUnevendata").toFile());
			FileUtils.deleteDirectory(eventDir.toPath().resolve("noSpectraOrInvalidMOD").toFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 何か問題はないか？ あるかどうかは例外フォルダがあるかどうか true: problem, false: no problem
	 */
	private boolean problem;

	/**
	 * @return true 問題あり false なし
	 */
	boolean hasProblem() {
		return problem;
	}

	/**
	 * @return 何か問題はないか
	 */
	private boolean check() {
		Path rdseedOutput = eventDir.toPath().resolve("rdseedOutputBackup");
		// if(rdseedOutput.exists())
		// System.out.println("There are sac files which delta is different from
		// "+deltaInMillis/1000.0);
		Path unmerged = eventDir.toPath().resolve("nonMergedUnevendata");
		// if(unmerged.exists())
		// System.out.println("There are sac files which couldnt be merged.");
		Path unrotated = eventDir.toPath().resolve("nonRotatedNE");
		// if(unrotated.exists())
		// System.out.println("There are sac files which couldnt be rotated.");
		return Files.exists(rdseedOutput) || Files.exists(unmerged) || Files.exists(unrotated);
	}

	/**
	 * unused SPECTRA*, RESP* files ->trash
	 */
	private void toTrash() {
		Path trash = eventDir.toPath().resolve("trash");
		try (DirectoryStream<Path> files = Files.newDirectoryStream(eventDir.toPath())) {
			for (Path path : files) {
				String name = path.getFileName().toString();
				if (name.contains("SPECTRA.") || name.contains("RESP."))
					Utilities.moveToDirectory(path, trash, true);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * evalrespをはしらせる evalresp station component year julian day minfreq maxfreq
	 * npts -s lin -r cs -u vel firstHandlerと同じ結果になる！
	 * 
	 * @param headerMap
	 * @return if succeed
	 */
	private boolean runEvalresp(Map<SACHeaderEnum, String> headerMap) {
		int npts = Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS)); // nptsをそろえるようにするか
		double minFreq = samplingHz / npts;
		String command = "evalresp " + headerMap.get(SACHeaderEnum.KSTNM) + " " + headerMap.get(SACHeaderEnum.KCMPNM)
				+ " " + event.getCMTTime().getYear() + " " + event.getCMTTime().getDayOfYear() + " " + minFreq + " "
				+ samplingHz + " " + headerMap.get(SACHeaderEnum.NPTS) + " -s lin -r cs -u vel";
		// System.out.println(command);

		ProcessBuilder pb = new ProcessBuilder(command.split("\\s"));
		pb.directory(eventDir.getAbsoluteFile());
		try {
			Process p = pb.start();
			return p.waitFor() == 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Remove files with suffixes other than B[HL][ENZ12]
	 * 
	 */
	private void selectChannels() {
		// System.out.println("Selecting Channels");
		Path trashBox = eventDir.toPath().resolve("invalidChannel");
		try (DirectoryStream<Path> modStream = Files.newDirectoryStream(eventDir.toPath(), "*.MOD")) {
			for (Path modPath : modStream) {
				// System.out.println(modFile);
				String name = modPath.getFileName().toString();
				String channel = name.split("\\.")[3];
				if (channel.equals("BHZ") || channel.equals("BLZ") || channel.equals("BHN") || channel.equals("BHE")
						|| channel.equals("BLN") || channel.equals("BLE"))
					continue;
				Utilities.moveToDirectory(modPath, trashBox, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
