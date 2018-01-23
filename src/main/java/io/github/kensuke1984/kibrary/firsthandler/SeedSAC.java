package io.github.kensuke1984.kibrary.firsthandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;

import io.github.kensuke1984.kibrary.external.SAC;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;
import io.github.kensuke1984.kibrary.util.sac.SACUtil;

/**
 * Class for extracting a seed file. It creates SAC files from the seed file.
 * 
 * This class assumes that rdseed, evalresp and sac exists in your PATH. The
 * software can be found in IRIS.
 * 
 * @version 0.1.8.3
 * 
 * @author Kensuke Konishi
 * 
 */
class SeedSAC implements Runnable {

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
	private boolean eventDirAlreadyExists;

	/**
	 * PDE時刻, 位置を基準に解凍するかどうか デフォルトはfalse (CMT)
	 */
	private boolean byPDE;

	/**
	 * 
	 * @param seedPath
	 *            to be extracted from
	 * @param outputDirectoryPath
	 *            Path where extracted files are placed
	 * @throws IOException
	 *             if the outputDirectoryPath already has events which also
	 *             exists in the seed file or an error occurs
	 */
	SeedSAC(Path seedPath, Path outputDirectoryPath) throws IOException {
		this(seedPath, outputDirectoryPath, null);
	}

	/**
	 * 解凍するseed file
	 * 
	 * @param seedPath
	 *            解凍するseedファイル
	 * @param outputDirectoryPath
	 *            inside this folder, the seed file is extracted. If the folder
	 *            does not exist, it will be created.
	 * 
	 * @param id
	 *            global cmt id
	 * @throws IOException
	 *             If the folder already has event folders which also exists in
	 *             the seed file.
	 */
	SeedSAC(Path seedPath, Path outputDirectoryPath, GlobalCMTID id) throws IOException {
		seedFile = new SEEDFile(seedPath);
		if (id != null)
			this.id = id;
		else
			setID();
		if (!idValidity())
			throw new RuntimeException("The ID " + this.id + " is invalid for " + seedPath);

		if (!Files.exists(outputDirectoryPath))
			Files.createDirectories(outputDirectoryPath);
		eventDir = new EventFolder(outputDirectoryPath.resolve(this.id.toString()));

		if (eventDir.exists())
			eventDirAlreadyExists = false;
		else if (!eventDir.mkdirs())
			throw new RuntimeException("Can not create " + eventDir);
		else
			eventDirAlreadyExists = true;

		seedFile.toDirectory(eventDir.toPath());
	}

	/**
	 * set GlobalCMTID
	 */
	private void setID() {
		// try to find id in the name of the file
		id = findIDinFilename();
		if (id != null)
			return;
		if (GlobalCMTID.isGlobalCMTID(seedFile.getVolumeLabel())) {
			id = new GlobalCMTID(seedFile.getVolumeLabel());
			return;
		}
		System.err.println("Dataset in this seed file starts " + seedFile.getStartingDate());
		GlobalCMTSearch sc = new GlobalCMTSearch(seedFile.getStartingDate(), seedFile.getEndingDate());
		id = sc.select();
		Objects.requireNonNull(id, "There is no event in the global CMT catalogue");
	}

	/**
	 * TODO use volumeID in seed files
	 * 
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
		return m0.find() ? new GlobalCMTID(m0.group()) : null;
	}

	/**
	 * @return global cmt id が日付的に合っているかどうか （startが発震時刻より前かつendがCMT時刻より後かどうか）
	 */
	private boolean idValidity() {
		event = id.getEvent();
		return event != null && id != null && seedFile.getStartingDate().isBefore(event.getPDETime())
				&& seedFile.getEndingDate().isAfter(event.getCMTTime());
	}

	/**
	 * イベントフォルダ内すべてのMODファイルの装置関数をdeconvolutionする。
	 * 対応するRESPのevalrespに失敗したMODファイルはNOSPECTRAMODへ
	 */
	private void deconvolute() throws IOException {
		// System.out.println("Conducting deconvolution");

		Path noSpectraPath = eventDir.toPath().resolve("noSpectraOrInvalidMOD");
		Path duplicateChannelPath = eventDir.toPath().resolve("duplicateChannel");
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
				Path respPath = eventDir.toPath().resolve(respFileName);
				String component;
				switch (componentName) {
				case "BHE":
				case "BLE":
				case "HHE":
				case "HLE":
//					component = "E";
					component = "Eo";
					break;
				case "BHN":
				case "BLN":
				case "HHN":
				case "HLN":
//					component = "N";
					component = "No";
					break;
				case "BHZ":
				case "BLZ":
				case "HHZ":
				case "HLZ":
					component = "Z";
					break;
				case "BH1":
				case "BL1":
				case "HH1":
				case "HL1":
//					component = "1";
					component = "No";
					break;
				case "BH2":
				case "BL2":
				case "HH2":
				case "HL2":
//					component = "2";
					component = "Eo";
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
					Utilities.moveToDirectory(respPath, noSpectraPath, true);
					continue;
				}

				// seedsacを行う
				try {
					int npts = Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS));
					// duplication of channel
					if (Files.exists(afterPath)) {
						// SPECTRAをつくれなかった*.MODファイルをduplicateChannelPathに移す
						Utilities.moveToDirectory(modPath, duplicateChannelPath, true);
						// SPECTRAをつくれなかったSPECTRA.*ファイルをduplicateChannelPathに移す
						Utilities.moveToDirectory(spectraPath, duplicateChannelPath, true);
						// SPECTRAをつくれなかったRESP.*ファイルをduplicateChannelPathに移す
						Utilities.moveToDirectory(respPath, duplicateChannelPath, true);
						continue;
					}
					SACDeconvolution.compute(modPath, spectraPath, afterPath, samplingHz / npts, samplingHz);
				} catch (Exception e) {
					if(Files.exists(modPath))
					// SPECTRAをつくれなかった*.MODファイルをnoSpectraに移す
					Utilities.moveToDirectory(modPath, noSpectraPath, true);
					if(Files.exists(spectraPath))
					// SPECTRAをつくれなかったSPECTRA.*ファイルをnoSpectraに移す
					Utilities.moveToDirectory(spectraPath, noSpectraPath, true);
					if(Files.exists(respPath))
					// SPECTRAをつくれなかったRESP.*ファイルをnoSpectraに移す
					Utilities.moveToDirectory(respPath, noSpectraPath, true);
					continue;
				}

				// 処理の終わったRESP.*ファイルをrespBoxに移す
				Utilities.moveToDirectory(respPath, respBoxPath, true);

				// 処理の終わったSPCTRA.*ファイルをspectraBoxに移す
				Utilities.moveToDirectory(spectraPath, spectraBoxPath, true);

				// 処理の終わったMOD.*ファイルをmodBoxに移す
				Utilities.moveToDirectory(modPath, modBoxPath, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean hadRun;

	/**
	 * @return if already run or not
	 */
	boolean hadRun() {
		return hadRun;
	}

	/**
	 * rdseedで解凍した波形ファイルのうちおかしなものを省くdeltaがずれているものはすて、波形がとびとびのものをmergeする
	 */
	private void mergeUnevenSac() throws IOException {
		// System.out.println("Merging in " + eventDir);
		// mergeする
		UnevenSACMerger u = new UnevenSACMerger(eventDir.toPath());
		u.merge();
		u.move();
	}

	/**
	 * イベントフォルダ内のmerge後のSACを修正する
	 * 
	 * @return if successful
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
				SACModifier sm = new SACModifier(event, sacPath, byPDE);

				// TODO 00 01 "" duplication detect

				// header check khole e.t.c
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
		try (DirectoryStream<Path> eStream = Files.newDirectoryStream(eventDir.toPath(), "*.Eo")) {
			for (Path ePath : eStream) {
				String[] parts = ePath.getFileName().toString().split("\\.");
				Path nPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".No");
				Path nwePath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".E");
				Path nwnPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".N");
				Path rPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".R");
				Path tPath = eventDir.toPath().resolve(parts[0] + "." + parts[1] + ".T");

				if (!Files.exists(nPath)) {
					Utilities.moveToDirectory(ePath, trashBox, true);
					continue;
				}
				
				try{
				Map<SACHeaderEnum, String> header1Map = SACUtil.readHeader(nPath);
				String cmpaz1 = header1Map.get(SACHeaderEnum.CMPAZ);
//				System.out.println("cmpaz 1" + cmpaz1);
				Map<SACHeaderEnum, String> header2Map = SACUtil.readHeader(ePath);
				String cmpaz2 = header2Map.get(SACHeaderEnum.CMPAZ);
//				System.out.println("cmpaz 2 " + cmpaz2);
				
				double[] data1 = SACUtil.readSACData(nPath);
				double[] data2 = SACUtil.readSACData(ePath);
				int newlength = data1.length;
				if(!(header1Map.get(SACHeaderEnum.NPTS).equals(header2Map.get(SACHeaderEnum.NPTS)))){
					
					newlength = Math.min(data1.length,data2.length);
					System.out.println("!!!!!!NPTS are different!!!!!!" + nPath + " " + ePath);
//					Utilities.moveToDirectory(ePath, trashBox, true);
//					Utilities.moveToDirectory(nPath, trashBox, true);
					continue;
				}
				
				
				double[] dataN = new double[newlength];
				double[] dataE = new double[newlength];
//				System.out.println(nPath + " " + data1[0]
//						 + " " + data1[1] + " " + data1[2]);
//				System.out.println(ePath + " " + data2[0]
//						 + " " + data2[1] + " " + data2[2]);
				for(int i = 0; i < newlength; i++){
					dataN[i] = (Math.sin(Math.PI*Double.parseDouble(cmpaz2)/180.)*data1[i]
								- Math.sin(Math.PI*Double.parseDouble(cmpaz1)/180.)*data2[i])
									/((Math.cos(Math.PI*Double.parseDouble(cmpaz1)/180.)*Math.sin(Math.PI*Double.parseDouble(cmpaz2)/180.))
									-(Math.sin(Math.PI*Double.parseDouble(cmpaz1)/180.)*Math.cos(Math.PI*Double.parseDouble(cmpaz2)/180.)));
					dataE[i] = (-Math.cos(Math.PI*Double.parseDouble(cmpaz2)/180.)*data1[i]
								+ Math.cos(Math.PI*Double.parseDouble(cmpaz1)/180.)*data2[i])
									/((Math.cos(Math.PI*Double.parseDouble(cmpaz1)/180.)*Math.sin(Math.PI*Double.parseDouble(cmpaz2)/180.))
									-(Math.sin(Math.PI*Double.parseDouble(cmpaz1)/180.)*Math.cos(Math.PI*Double.parseDouble(cmpaz2)/180.)));
//					dataNs[i] = String.format("%.6f",dataN[i]);
					
				}
//				System.out.println(nwnPath + " " + dataN[0] 
//						+ " " + dataN[1] + " " + dataN[2]);
//				System.out.println(nwePath + " " + dataE[0] 
//						+ " " + dataE[1] + " " + dataE[2]);
			    SACUtil.writeSAC(nwnPath, header1Map, dataN);
			    SACUtil.writeSAC(nwePath, header2Map, dataE);	
				try (SAC sacD = SAC.createProcess()){
					String cwd = nwnPath.getParent().toString();
					sacD.inputCMD("cd " + cwd);
					sacD.inputCMD("r " + nwnPath.getFileName());
					sacD.inputCMD("ch lovrok true");
					sacD.inputCMD("ch cmpaz 0 cmpinc 90");
					sacD.inputCMD("ch kcmpnm BHN");
					sacD.inputCMD("w over");
					sacD.inputCMD("r " + nwePath.getFileName());
					sacD.inputCMD("ch lovrok true");
					sacD.inputCMD("ch cmpaz 90 cmpinc 90");
					sacD.inputCMD("ch kcmpnm BHE");
					sacD.inputCMD("w over");
				}		
				Map<SACHeaderEnum, String> headerNMap = SACUtil.readHeader(nwnPath);	
//				System.out.println("cmpazN " + headerNMap.get(SACHeaderEnum.CMPINC)
//				+ " cmpazN " + headerNMap.get(SACHeaderEnum.CMPAZ));
				Map<SACHeaderEnum, String> headerEMap = SACUtil.readHeader(nwePath);	
//				System.out.println("cmpincE " + headerEMap.get(SACHeaderEnum.CMPINC)
//				+ " cmpazE " + headerEMap.get(SACHeaderEnum.CMPAZ));
//				System.out.println("created NEW sac");				
				
				}catch(IOException e){
				}
				
				boolean rotated = SACUtil.rotate(nwePath, nwnPath, rPath, tPath);
				if (rotated) {
					Utilities.moveToDirectory(nwnPath, neDir, true);
					Utilities.moveToDirectory(nwePath, neDir, true);
				} else {
					Utilities.moveToDirectory(nwePath, trashBox, true);
					Utilities.moveToDirectory(nwnPath, trashBox, true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// If there are files (.N) which had no pair .E, move them to trash
		try (DirectoryStream<Path> nPaths = Files.newDirectoryStream(eventDir.toPath(), "*.No")) {
			for (Path nPath : nPaths)
				Utilities.moveToDirectory(nPath, neDir, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try (DirectoryStream<Path> ePaths = Files.newDirectoryStream(eventDir.toPath(), "*.Eo")) {
			for (Path ePath : ePaths)
				Utilities.moveToDirectory(ePath, neDir, true);
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
			System.out.println("Finished fix delta");
//			try (DirectoryStream<Path> sacPaths = Files.newDirectoryStream(eventDir.toPath(), "*.SAC")) {
//			for (Path sacPath : sacPaths) {
////				Files.copy(sacPath, backupPath.resolve(sacPath.getFileName()));
//				// System.out.println("..illegal delta.");
//				try {
//					fixdeg(sacPath);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
			
			
//		}

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
		try (SAC sacD = SAC.createProcess()) {
			String cwd = sacPath.getParent().toString();
			sacD.inputCMD("cd " + cwd);// set current directory
			sacD.inputCMD("r " + sacPath.getFileName());// read
			sacD.inputCMD("ch lovrok true");// overwrite permission
			sacD.inputCMD("interpolate delta " + delta);
			sacD.inputCMD("w over");
		}
	}

//	private static void fixdeg(Path sacPath) throws IOException, InterruptedException {
//		try (SAC sacD = SAC.createProcess()) {
//			String cwd = sacPath.getParent().toString();
//			sacD.inputCMD("cd " + cwd);// set current directory
//			sacD.inputCMD("r " + sacPath.getFileName());// read
//			sacD.inputCMD("ch lovrok true");// overwrite permission
//			if (sacPath.toString().contains(".BHN.") || sacPath.toString().contains(".BLN."))
//				sacD.inputCMD("ch cmpaz 0 cmpinc 90");
//			else if (sacPath.toString().contains(".BHE.") || sacPath.toString().contains(".BLE."))
//				sacD.inputCMD("ch cmpaz 90 cmpinc 90");		
//			else if (sacPath.toString().contains(".BHZ.") || sacPath.toString().contains(".BLZ."))
//				sacD.inputCMD("ch cmpinc 0");
//			else if (sacPath.toString().contains(".BH1.") || sacPath.toString().contains(".BL1."))
//				createNE(sacPath);
////			sacD.inputCMD("interpolate delta " + delta);
//			sacD.inputCMD("w over");
//		}
//	}	
//	
//	
//	private static void createNE(Path sac1Path){
//		//if文でhとlをわけたい
//		Path sac2Path = Paths.get(sac1Path.toString().replace(".BH1.", ".BH2."));
//		Path sacNPath = Paths.get(sac1Path.toString().replace(".BH1.", ".BHN."));
//		Path sacEPath = Paths.get(sac1Path.toString().replace(".BH1.", ".BHE."));		
//		try{
//			Map<SACHeaderEnum, String> header1Map = SACUtil.readHeader(sac1Path);
//			String cmpaz1 = header1Map.get(SACHeaderEnum.CMPAZ);
//			System.out.println("createNE 1 " + cmpaz1);
//			Map<SACHeaderEnum, String> header2Map = SACUtil.readHeader(sac2Path);
//			String cmpaz2 = header2Map.get(SACHeaderEnum.CMPAZ);
//			System.out.println("createNE 2 " + cmpaz2);			
//			double[] data1 = SACUtil.readSACData(sac1Path);
//			double[] data2 = SACUtil.readSACData(sac2Path);
//			double[] dataN = new double[data1.length];
//			double[] dataE = new double[data2.length];
//			System.out.println(sac1Path);
//			for(int i = 0; i < data1.length; i++){
//				dataN[i] = Double.parseDouble(String.format("%.1f", (Math.sin(Math.PI*Double.parseDouble(cmpaz2)/180.)*data1[i]
//							- Math.sin(Math.PI*Double.parseDouble(cmpaz1)/180.)*data2[i])
//								/((Math.cos(Math.PI*Double.parseDouble(cmpaz1)/180.)*Math.sin(Math.PI*Double.parseDouble(cmpaz2)/180.))
//								-(Math.sin(Math.PI*Double.parseDouble(cmpaz1)/180.)*Math.cos(Math.PI*Double.parseDouble(cmpaz2)/180.)))));
//				dataE[i] = Double.parseDouble(String.format("%.1f", (-Math.cos(Math.PI*Double.parseDouble(cmpaz2)/180.)*data1[i]
//							+ Math.cos(Math.PI*Double.parseDouble(cmpaz1)/180.)*data2[i])
//								/((Math.cos(Math.PI*Double.parseDouble(cmpaz1)/180.)*Math.sin(Math.PI*Double.parseDouble(cmpaz2)/180.))
//								-(Math.sin(Math.PI*Double.parseDouble(cmpaz1)/180.)*Math.cos(Math.PI*Double.parseDouble(cmpaz2)/180.)))));
//			}
//		    SACUtil.writeSAC(sacNPath, header1Map, dataN);
//		    SACUtil.writeSAC(sacEPath, header2Map, dataE);	
//			try (SAC sacD = SAC.createProcess()){
//				String cwd = sac1Path.getParent().toString();
//				sacD.inputCMD("cd " + cwd);
//				sacD.inputCMD("r " + sacNPath.getFileName());
//				sacD.inputCMD("ch lovrok true");
//				sacD.inputCMD("ch cmpaz 0 cmpinc 90");
//				sacD.inputCMD("ch kcmpnm BHN");
//				sacD.inputCMD("w over");
//				sacD.inputCMD("r " + sacEPath.getFileName());
//				sacD.inputCMD("ch lovrok true");
//				sacD.inputCMD("ch cmpaz 90 cmpinc 90");
//				sacD.inputCMD("ch kcmpnm BHE");
//				sacD.inputCMD("w over");
//			}		
//			Map<SACHeaderEnum, String> headerNMap = SACUtil.readHeader(sacNPath);	
////			System.out.println("cmpincN " + headerNMap.get(SACHeaderEnum.CMPINC)
////			+ " cmpazN " + headerNMap.get(SACHeaderEnum.CMPAZ));
//			Map<SACHeaderEnum, String> headerEMap = SACUtil.readHeader(sacEPath);	
////			System.out.println("cmpincE " + headerEMap.get(SACHeaderEnum.CMPINC)
////			+ " cmpazE " + headerEMap.get(SACHeaderEnum.CMPAZ));
//			System.out.println("created NEW sac");
//		}catch(IOException e){
//			System.out.println("UNCREATED NEW sac");
//		}
//		
//	}

	@Override
	public void run() {
		if (!eventDirAlreadyExists)
			throw new RuntimeException("The condition is no good.");
		System.err.println("Opening " + seedFile + " in " + eventDir);
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

		System.out.println("finish");
	}

	private void removeIntermediateFiles() {
		try {
			Path event = eventDir.toPath();
			FileUtils.deleteDirectory(event.resolve("merged").toFile());
			FileUtils.deleteDirectory(event.resolve("mod").toFile());
			FileUtils.deleteDirectory(event.resolve("rdseedOutputBackup").toFile());
			FileUtils.deleteDirectory(event.resolve("resp").toFile());
			FileUtils.deleteDirectory(event.resolve("rotatedNE").toFile());
			FileUtils.deleteDirectory(event.resolve("nonRotatedNE").toFile());
			FileUtils.deleteDirectory(event.resolve("spectra").toFile());
			FileUtils.deleteDirectory(event.resolve("trash").toFile());
			FileUtils.deleteDirectory(event.resolve("mergedUnevendata").toFile());
			FileUtils.deleteDirectory(event.resolve("invalidChannel").toFile());
			FileUtils.deleteDirectory(event.resolve("nonMergedUnevendata").toFile());
			FileUtils.deleteDirectory(event.resolve("noSpectraOrInvalidMOD").toFile());
			FileUtils.deleteDirectory(event.resolve("duplicateChannel").toFile());
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
	 * @param headerMap header of sac file
	 * @return if succeed
	 */
	private boolean runEvalresp(Map<SACHeaderEnum, String> headerMap) {
		int npts = Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS)); // nptsをそろえるようにするか
		double minFreq = samplingHz / npts;
		String command = "evalresp " + headerMap.get(SACHeaderEnum.KSTNM) + " " + headerMap.get(SACHeaderEnum.KCMPNM)
				+ " " + event.getCMTTime().getYear() + " " + event.getCMTTime().getDayOfYear() + " " + minFreq + " "
				+ samplingHz + " " + headerMap.get(SACHeaderEnum.NPTS) + " -s lin -r cs -u vel";

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
	 * Remove files with suffixes other than [BH][HL][ENZ12]
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
						|| channel.equals("BLN") || channel.equals("BLE") || channel.equals("HHZ")
						|| channel.equals("HLZ") || channel.equals("HHN") || channel.equals("HHE")
						|| channel.equals("HLN") || channel.equals("HLE") || channel.equals("BH1")
						|| channel.equals("BH2") || channel.equals("BL1") || channel.equals("BL2"))
					continue;
				Utilities.moveToDirectory(modPath, trashBox, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
