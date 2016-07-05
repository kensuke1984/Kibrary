package io.github.kensuke1984.kibrary.firsthandler;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import io.github.kensuke1984.kibrary.external.SAC;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;
import io.github.kensuke1984.kibrary.util.sac.SACUtil;

/**
 * {@link SeedSAC}内で行うSacの修正
 * 
 * 
 * @version 0.1.8.1
 * 
 * @author Kensuke Konishi
 * 
 */
class SACModifier {

	/**
	 * Sacのイベント
	 */
	private GlobalCMTData event;

	private Path sacPath;

	private Map<SACHeaderEnum, String> headerMap;

	/**
	 * @param event
	 * @param sacPath
	 * @param byPDE
	 *            true: PDE, false: CMT
	 */
	SACModifier(GlobalCMTData event, Path sacPath, boolean byPDE) throws IOException {
		this.sacPath = sacPath;
		headerMap = SACUtil.readHeader(sacPath);
		String modifiedFileName = sacPath.getFileName().toString().replace(".SAC", ".MOD");
		modifiedSacPath = sacPath.resolveSibling(modifiedFileName);
		this.event = event;
		this.byPDE = byPDE;
		setInitialSacStartTime();
	}

	/**
	 * PDEによる解凍かどうか デフォルトはCMT
	 */
	private boolean byPDE;

	private Path modifiedSacPath;

	/**
	 * taperをかける時間（msec）
	 */
	private static final int taperTime = 60 * 1000;

	/**
	 * Headerをチェックする CMPINC、khole
	 * 
	 * @return if the header is valid
	 */
	boolean checkHeader() {
		// System.out.println("Checking header validity in "+sacFile);
		String channel = sacPath.getFileName().toString().split("\\.")[3];

		// check CMPINC
		if (channel.equals("BHN") || channel.equals("BHE") || channel.equals("BH1") || channel.equals("BH2"))
			if (Double.parseDouble(headerMap.get(SACHeaderEnum.CMPINC)) != 90)
				return false;

		// check "khole" value
		String khole = headerMap.get(SACHeaderEnum.KHOLE);
		return khole.isEmpty() || khole.equals("00") || khole.equals("01");
	}

	/**
	 * 
	 * operate rtrend and rmean in SAC and the sac file is write to ??.MOD
	 * 
	 */
	void preprocess() throws IOException, InterruptedException {
		try (SAC sacProcess = SAC.createProcess()) {
			String cwd = sacPath.getParent().toString();
			sacProcess.inputCMD("cd " + cwd);
			sacProcess.inputCMD("r " + sacPath.getFileName());
			sacProcess.inputCMD("ch lovrok true");
			sacProcess.inputCMD("rtrend");
			sacProcess.inputCMD("rmean");
			sacProcess.inputCMD("w " + modifiedSacPath.getFileName());
		}
	}

	/**
	 * if the startTime of sac is after the event time, and the gap is bigger
	 * than taperTime, interpolation cannot be done This method can be valid
	 * before {@link #interpolate()} because the method changes headers.
	 * 
	 * @return if gap between sac starting time and event time is small enough
	 *         (smaller than {@link #taperTime})
	 */
	boolean canInterpolate() {
		LocalDateTime eventTime = byPDE ? event.getPDETime() : event.getCMTTime();
		return eventTime.until(initialSacStartTime, ChronoUnit.MILLIS) < taperTime;
	}

	/**
	 * sac start time when this instance is made
	 */
	private LocalDateTime initialSacStartTime;

	/**
	 * set {@link #initialSacStartTime}
	 */
	private void setInitialSacStartTime() {
		int nzyear = Integer.parseInt(headerMap.get(SACHeaderEnum.NZYEAR));
		int nzjday = Integer.parseInt(headerMap.get(SACHeaderEnum.NZJDAY));
		int nzhour = Integer.parseInt(headerMap.get(SACHeaderEnum.NZHOUR));
		int nzmin = Integer.parseInt(headerMap.get(SACHeaderEnum.NZMIN));
		int nzsec = Integer.parseInt(headerMap.get(SACHeaderEnum.NZSEC));
		int nzmsec = Integer.parseInt(headerMap.get(SACHeaderEnum.NZMSEC));
		double b = Double.parseDouble(headerMap.get(SACHeaderEnum.B));
		long bInNanos = (long) (b * 1000 * 1000 * 1000);
		initialSacStartTime = LocalDateTime.of(nzyear, 1, 1, nzhour, nzmin, nzsec, nzmsec * 1000 * 1000)
				.plusDays(nzjday - 1).plusNanos(bInNanos);
	}

	/**
	 * イベント時刻よりSac開始が遅い場合 Sacを補完する 補完の際 テーピングをかけ０で補完する。 <br>
	 * 開始時刻の部分を丸みをかけて０にする その幅は {@link #taperTime}
	 * 
	 * @return if success or not
	 */
	boolean interpolate() throws IOException {

		double b = Double.parseDouble(headerMap.get(SACHeaderEnum.B));
		long bInMillis = Math.round(b * 1000);
		double e = Double.parseDouble(headerMap.get(SACHeaderEnum.E));
		long eInMillis = Math.round(e * 1000);
		LocalDateTime eventTime = byPDE ? event.getPDETime() : event.getCMTTime();

		// sacの読み込み
		double[] sacdata = SACUtil.readSACData(modifiedSacPath);

		// イベント時刻と合わせる
		// イベント時刻とSAC時刻の差
		// Sac start time in millisec ー event time in millisec
		long timeGapInMillis = eventTime.until(initialSacStartTime, ChronoUnit.MILLIS);

		// もしイベント時刻よりSac時刻がおそい場合
		// イベント時刻から sacの始まりまでを補完する
		// if sac startTime is after event time, then interpolate gap.
		// if the gap is bigger than tapertime then skip
		// if (sacStartTime.after(eventTime)) {
		if (taperTime < timeGapInMillis) {
			System.err.println(modifiedSacPath + " starts too late");
			return false;
		} else if (0 <= timeGapInMillis) {
			System.err.println("seismograms start at after the event time... interpolating...");
			// delta [msec]
			long deltaInMillis = (long) (Double.parseDouble(headerMap.get(SACHeaderEnum.DELTA)) * 1000);

			// 時刻差のステップ数
			int gapPoint = (int) (timeGapInMillis / deltaInMillis);

			// taping をかけるポイント数
			int taperPoint = (int) (taperTime / deltaInMillis);

			// taperをかける
			for (int i = 0; i < taperPoint; i++)
				sacdata[i] *= Math.sin(i / taperPoint * Math.PI / 2.0);

			// 0で補完する
			double[] neosacdata = new double[sacdata.length + gapPoint];
			// gapの部分は０で
			// for (int i = 0; i < gapPoint; i++)
			// neosacdata[i] = 0;
			// taperをかけたsacdataをくっつける
			for (int i = gapPoint; i < neosacdata.length; i++)
				neosacdata[i] = sacdata[i - gapPoint];
			int npts = neosacdata.length;
			headerMap.put(SACHeaderEnum.NPTS, Integer.toString(npts));
			sacdata = neosacdata;
			timeGapInMillis = 0;
			// headerMap.put(SacHeaderEnum.B, Double.toString(0));
		}

		Location sourceLocation = byPDE ? event.getPDELocation() : event.getCmtLocation();

		headerMap.put(SACHeaderEnum.B, Double.toString((bInMillis + timeGapInMillis) / 1000.0));
		headerMap.put(SACHeaderEnum.E, Double.toString((eInMillis + timeGapInMillis) / 1000.0));

		headerMap.put(SACHeaderEnum.NZYEAR, Integer.toString(eventTime.getYear()));
		headerMap.put(SACHeaderEnum.NZJDAY, Integer.toString(eventTime.getDayOfYear()));
		headerMap.put(SACHeaderEnum.NZHOUR, Integer.toString(eventTime.getHour()));
		headerMap.put(SACHeaderEnum.NZMIN, Integer.toString(eventTime.getMinute()));
		headerMap.put(SACHeaderEnum.NZSEC, Integer.toString(eventTime.getSecond()));
		headerMap.put(SACHeaderEnum.NZMSEC, Integer.toString(eventTime.getNano() / 1000 / 1000));
		headerMap.put(SACHeaderEnum.KEVNM, event.toString());
		headerMap.put(SACHeaderEnum.EVLA, Double.toString(sourceLocation.getLatitude()));
		headerMap.put(SACHeaderEnum.EVLO, Double.toString(sourceLocation.getLongitude()));
		headerMap.put(SACHeaderEnum.EVDP, Double.toString(6371 - sourceLocation.getR()));
		headerMap.put(SACHeaderEnum.LOVROK, Boolean.toString(true));
		SACUtil.writeSAC(modifiedSacPath, headerMap, sacdata);

		return true;
	}

	/**
	 * min <= epicentral distance <= max ならtrue
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	boolean checkEpicentralDistance(double min, double max) {
		double epicentralDistance = Double.parseDouble(headerMap.get(SACHeaderEnum.GCARC));
		return min <= epicentralDistance && max >= epicentralDistance;
	}

	/**
	 * Rebuild by SAC.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	void rebuild() throws IOException, InterruptedException {

		// nptsを元のSacfileのEでのポイントを超えない２の累乗ポイントにする
		Map<SACHeaderEnum, String> headerMap = SACUtil.readHeader(modifiedSacPath);
		int npts = (int) (Double.parseDouble(headerMap.get(SACHeaderEnum.E))
				/ Double.parseDouble(headerMap.get(SACHeaderEnum.DELTA)));
		int newNpts = Integer.highestOneBit(npts);
		// System.out.println("rebuilding "+ sacFile);
		String cwd = sacPath.getParent().toString();
		try (SAC sacP1 = SAC.createProcess()) {
			sacP1.inputCMD("cd " + cwd);
			sacP1.inputCMD("r " + modifiedSacPath.getFileName());
			sacP1.inputCMD("interpolate b 0");
			sacP1.inputCMD("w over");
		}
		try (SAC sacP2 = SAC.createProcess()) {
			// current directoryをうつす
			sacP2.inputCMD("cd " + cwd);
			sacP2.inputCMD("cut b n " + newNpts);
			sacP2.inputCMD("r " + modifiedSacPath.getFileName());
			sacP2.inputCMD("w over");
		}
		// ヘッダーの更新
		this.headerMap = SACUtil.readHeader(modifiedSacPath);
	}

}
