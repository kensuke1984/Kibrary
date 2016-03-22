package io.github.kensuke1984.kibrary.util.spc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;

import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.util.Raypath;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACExtension;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * 
 * This class create SAC files from one or two spector files(
 * {@link SpectrumFile})
 * 
 * 
 * @version 0.1.2.3
 * 
 * @author Kensuke Konishi
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
public class SACMaker implements Runnable {

	private final static Map<SACHeaderEnum, String> initialMap = new EnumMap<>(SACHeaderEnum.class);

	static {
		initialMap.put(SACHeaderEnum.DEPMIN, "-12345.0");
		initialMap.put(SACHeaderEnum.DEPMAX, "-12345.0");
		initialMap.put(SACHeaderEnum.SCALE, "-12345.0");
		initialMap.put(SACHeaderEnum.ODELTA, "-12345.0");
		initialMap.put(SACHeaderEnum.O, "-12345.0");
		initialMap.put(SACHeaderEnum.A, "-12345.0");
		initialMap.put(SACHeaderEnum.T0, "-12345.0");
		initialMap.put(SACHeaderEnum.T1, "-12345.0");
		initialMap.put(SACHeaderEnum.T2, "-12345.0");
		initialMap.put(SACHeaderEnum.T3, "-12345.0");
		initialMap.put(SACHeaderEnum.T4, "-12345.0");
		initialMap.put(SACHeaderEnum.T5, "-12345.0");
		initialMap.put(SACHeaderEnum.T6, "-12345.0");
		initialMap.put(SACHeaderEnum.T7, "-12345.0");
		initialMap.put(SACHeaderEnum.T8, "-12345.0");
		initialMap.put(SACHeaderEnum.T9, "-12345.0");
		initialMap.put(SACHeaderEnum.F, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP0, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP1, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP2, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP3, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP4, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP5, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP6, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP7, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP8, "-12345.0");
		initialMap.put(SACHeaderEnum.RESP9, "-12345.0");
		initialMap.put(SACHeaderEnum.STEL, "-12345.0");
		initialMap.put(SACHeaderEnum.STDP, "-12345.0");
		initialMap.put(SACHeaderEnum.EVEL, "-12345.0");
		initialMap.put(SACHeaderEnum.MAG, "-12345.0");
		initialMap.put(SACHeaderEnum.USER0, "-12345.0");
		initialMap.put(SACHeaderEnum.USER1, "-12345.0");
		initialMap.put(SACHeaderEnum.USER2, "-12345.0");
		initialMap.put(SACHeaderEnum.USER3, "-12345.0");
		initialMap.put(SACHeaderEnum.USER4, "-12345.0");
		initialMap.put(SACHeaderEnum.USER5, "-12345.0");
		initialMap.put(SACHeaderEnum.USER6, "-12345.0");
		initialMap.put(SACHeaderEnum.USER7, "-12345.0");
		initialMap.put(SACHeaderEnum.USER8, "-12345.0");
		initialMap.put(SACHeaderEnum.USER9, "-12345.0");
		initialMap.put(SACHeaderEnum.DIST, "-12345.0");
		initialMap.put(SACHeaderEnum.DEPMEN, "-12345.0");
		initialMap.put(SACHeaderEnum.CMPAZ, "-12345.0");
		initialMap.put(SACHeaderEnum.CMPINC, "-12345.0");
		initialMap.put(SACHeaderEnum.XMINIMUM, "-12345.0");
		initialMap.put(SACHeaderEnum.XMAXIMUM, "-12345.0");
		initialMap.put(SACHeaderEnum.YMINIMUM, "-12345.0");
		initialMap.put(SACHeaderEnum.YMAXIMUM, "-12345.0");
		initialMap.put(SACHeaderEnum.NVHDR, "6");
		initialMap.put(SACHeaderEnum.NORID, "-12345");
		initialMap.put(SACHeaderEnum.NEVID, "-12345");
		initialMap.put(SACHeaderEnum.NWFID, "-12345");
		initialMap.put(SACHeaderEnum.NXSIZE, "-12345");
		initialMap.put(SACHeaderEnum.NYSIZE, "-12345");
		initialMap.put(SACHeaderEnum.IFTYPE, "1");
		initialMap.put(SACHeaderEnum.IDEP, "5");
		initialMap.put(SACHeaderEnum.IZTYPE, "-12345");
		initialMap.put(SACHeaderEnum.IINST, "-12345");
		initialMap.put(SACHeaderEnum.ISTREG, "-12345");
		initialMap.put(SACHeaderEnum.IEVREG, "-12345");
		initialMap.put(SACHeaderEnum.IEVTYP, "-12345");
		initialMap.put(SACHeaderEnum.IQUAL, "-12345");
		initialMap.put(SACHeaderEnum.ISYNTH, "-12345");
		initialMap.put(SACHeaderEnum.IMAGTYP, "-12345");
		initialMap.put(SACHeaderEnum.IMAGSRC, "-12345");
		initialMap.put(SACHeaderEnum.LEVEN, "true");
		initialMap.put(SACHeaderEnum.LPSPOL, "false");
		initialMap.put(SACHeaderEnum.LOVROK, "true");
		initialMap.put(SACHeaderEnum.LCALDA, "true");
		initialMap.put(SACHeaderEnum.KHOLE, "-12345");
		initialMap.put(SACHeaderEnum.KO, "-12345");
		initialMap.put(SACHeaderEnum.KA, "-12345");
		initialMap.put(SACHeaderEnum.KT0, "-12345");
		initialMap.put(SACHeaderEnum.KT1, "-12345");
		initialMap.put(SACHeaderEnum.KT2, "-12345");
		initialMap.put(SACHeaderEnum.KT3, "-12345");
		initialMap.put(SACHeaderEnum.KT4, "-12345");
		initialMap.put(SACHeaderEnum.KT5, "-12345");
		initialMap.put(SACHeaderEnum.KT6, "-12345");
		initialMap.put(SACHeaderEnum.KT7, "-12345");
		initialMap.put(SACHeaderEnum.KT8, "-12345");
		initialMap.put(SACHeaderEnum.KT9, "-12345");
		initialMap.put(SACHeaderEnum.KF, "-12345");
		initialMap.put(SACHeaderEnum.KUSER0, "-12345");
		initialMap.put(SACHeaderEnum.KUSER1, "-12345");
		initialMap.put(SACHeaderEnum.KUSER2, "-12345");
		initialMap.put(SACHeaderEnum.KDATRD, "-12345");
		initialMap.put(SACHeaderEnum.KINST, "-12345");
		initialMap.put(SACHeaderEnum.num100, "-12345");
		initialMap.put(SACHeaderEnum.num101, "-12345");
		initialMap.put(SACHeaderEnum.num102, "-12345");
		initialMap.put(SACHeaderEnum.num103, "-12345");
		initialMap.put(SACHeaderEnum.num104, "-12345");
		initialMap.put(SACHeaderEnum.num109, "-12345");
		initialMap.put(SACHeaderEnum.num54, "-12345");
		initialMap.put(SACHeaderEnum.num55, "-12345");
		initialMap.put(SACHeaderEnum.num63, "-12345");
		initialMap.put(SACHeaderEnum.num64, "-12345");
		initialMap.put(SACHeaderEnum.num65, "-12345");
		initialMap.put(SACHeaderEnum.num66, "-12345");
		initialMap.put(SACHeaderEnum.num67, "-12345");
		initialMap.put(SACHeaderEnum.num68, "-12345");
		initialMap.put(SACHeaderEnum.num69, "-12345");
		initialMap.put(SACHeaderEnum.num80, "-12345");
		initialMap.put(SACHeaderEnum.num84, "-12345");
		initialMap.put(SACHeaderEnum.num88, "-12345");
		initialMap.put(SACHeaderEnum.num9, "-12345");
		initialMap.put(SACHeaderEnum.num97, "-12345");
		initialMap.put(SACHeaderEnum.num98, "-12345");
		initialMap.put(SACHeaderEnum.num99, "-12345");
	}

	private class SAC implements SACData, Cloneable {
		private double[] waveData;

		@Override
		protected SAC clone() {
			try {
				SAC sac = (SAC) super.clone();
				sac.headerMap = new EnumMap<>(headerMap);
				return sac;
			} catch (Exception e) {
				throw new RuntimeException("UneXPectED");
			}
		}

		private SAC of(SACComponent component) {
			SAC sac = clone();
			switch (component.valueOf()) {
			case 1:
				sac = sac.setSACString(SACHeaderEnum.KCMPNM, "vertical");
				break;
			case 2:
				sac = sac.setSACString(SACHeaderEnum.KCMPNM, "radial");
				break;
			case 3:
				sac = sac.setSACString(SACHeaderEnum.KCMPNM, "trnsvers");
				break;
			default:
			}
			return sac;
		}

		private Map<SACHeaderEnum, String> headerMap = new EnumMap<>(initialMap);

		private SAC() {
		}

		@Override
		public boolean getBoolean(SACHeaderEnum sacHeaderEnum) {
			return Boolean.parseBoolean(headerMap.get(sacHeaderEnum));
		}

		@Override
		public int getInt(SACHeaderEnum sacHeaderEnum) {
			return Integer.parseInt(headerMap.get(sacHeaderEnum));
		}

		@Override
		public int getSACEnumerated(SACHeaderEnum sacHeaderEnum) {
			return Integer.parseInt(headerMap.get(sacHeaderEnum));
		}

		@Override
		public String getSACString(SACHeaderEnum sacHeaderEnum) {
			return headerMap.get(sacHeaderEnum);
		}

		@Override
		public double getValue(SACHeaderEnum sacHeaderEnum) {
			return Double.parseDouble(headerMap.get(sacHeaderEnum));
		}

		@Override
		public Trace createTrace() {
			throw new RuntimeException("UnEXPEcteD");
		}

		@Override
		public SAC setBoolean(SACHeaderEnum sacHeaderEnum, boolean bool) {
			if (headerMap.containsKey(sacHeaderEnum))
				throw new RuntimeException("UNEeXpExted");
			headerMap.put(sacHeaderEnum, String.valueOf(bool));
			return this;
		}

		@Override
		public SAC applyButterworthFilter(ButterworthFilter filter) {
			throw new RuntimeException("UnEXPEcteD");
		}

		@Override
		public SAC setValue(SACHeaderEnum sacHeaderEnum, double value) {
			if (headerMap.containsKey(sacHeaderEnum))
				throw new RuntimeException("UNEeXpExted");
			headerMap.put(sacHeaderEnum, String.valueOf(value));
			return this;
		}

		@Override
		public SAC setInt(SACHeaderEnum sacHeaderEnum, int value) {
			if (headerMap.containsKey(sacHeaderEnum))
				throw new RuntimeException("UNEeXpExted");
			headerMap.put(sacHeaderEnum, String.valueOf(value));
			return this;
		}

		@Override
		public SAC setSACEnumerated(SACHeaderEnum sacHeaderEnum, int value) {
			if (headerMap.containsKey(sacHeaderEnum))
				throw new RuntimeException("UNEeXpExted");
			headerMap.put(sacHeaderEnum, String.valueOf(value));
			return this;
		}

		@Override
		public SAC setSACString(SACHeaderEnum sacHeaderEnum, String string) {
			if (headerMap.containsKey(sacHeaderEnum))
				throw new RuntimeException("UNEeXpExted");
			headerMap.put(sacHeaderEnum, string);
			return this;
		}

		@Override
		public SAC setSACData(double[] waveData) {
			Objects.requireNonNull(waveData);
			if (waveData.length != getInt(SACHeaderEnum.NPTS))
				throw new RuntimeException("UNEeXpExted");
			this.waveData = waveData;
			return this;
		}

		@Override
		public double[] getData() {
			return waveData;
		}

	}

	private DSMOutput secondarySPC;

	private DSMOutput primeSPC;

	/**
	 * 時間領域に持ってくるときのサンプリングヘルツ
	 */
	private double samplingHz = 20;

	/**
	 * 書き出す成分 デフォルトでは R, T, Z （すべて）
	 */
	private Set<SACComponent> components = new HashSet<>(Arrays.asList(SACComponent.values()));

	/**
	 * SACを書き出すディレクトリ
	 */
	private Path outDirectoryPath;

	/**
	 * 
	 * @param oneSPC
	 *            one spc
	 * @param pairSPC
	 *            pair spc
	 */
	public SACMaker(DSMOutput oneSPC, DSMOutput pairSPC) {
		this(oneSPC, pairSPC, null);
	}

	/**
	 * 
	 * @param oneSPC
	 *            one spc
	 * @param pairSPC
	 *            pair spc
	 * @param sourceTimeFunction
	 *            to consider
	 */
	public SACMaker(DSMOutput oneSPC, DSMOutput pairSPC, SourceTimeFunction sourceTimeFunction) {
		if (pairSPC != null && !check(oneSPC, pairSPC))
			throw new RuntimeException("Input spc are not a pair.");
		primeSPC = oneSPC;
		secondarySPC = pairSPC;
		globalCMTID = new GlobalCMTID(oneSPC.getSourceID());
		this.sourceTimeFunction = sourceTimeFunction;
	}

	/**
	 * @param oneSPC
	 *            Spector for SAC
	 */
	public SACMaker(DSMOutput oneSPC) {
		this(oneSPC, null, null);
	}

	private GlobalCMTID globalCMTID;

	/**
	 * 時間微分させるか
	 */
	private boolean temporalDifferentiation;

	/**
	 * 
	 * @param bool
	 *            If set true, the time in Sac is PDE time.
	 */
	public void setPDE(boolean bool) {
		pde = bool;
	}

	private SourceTimeFunction sourceTimeFunction;

	public void setSourceTimeFunction(SourceTimeFunction sourceTimeFunction) {
		this.sourceTimeFunction = sourceTimeFunction;
	}

	private boolean pde;

	public void setTemporalDifferentiation(boolean temporalDifferentiation) {
		this.temporalDifferentiation = temporalDifferentiation;
	}

	private LocalDateTime beginDateTime;
	private Station station;
	private Raypath path;

	private int lsmooth;
	private double delta;
	private int npts;

	private void setInformation() {
		station = new Station(primeSPC.getObserverID(), primeSPC.getObserverPosition(), "DSM");

		path = new Raypath(primeSPC.getSourceLocation(), primeSPC.getObserverPosition());
		beginDateTime = pde ? globalCMTID.getEvent().getPDETime() : globalCMTID.getEvent().getCMTTime();
		npts = findNPTS();
		lsmooth = findLsmooth();
		// npts = lsmooth * primeSPC.np * 2;
		delta = primeSPC.tlen() / npts;
	}

	private int findNPTS() {
		int npts = (int) (primeSPC.tlen() * samplingHz);
		int pow2 = Integer.highestOneBit(npts);
		return pow2 < npts ? pow2 * 2 : pow2;
	}

	/**
	 * Create sacFiles for partials in outDirectory.
	 * 
	 * @param outDirectoryPath
	 *            {@link Path} of an output folder
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outputPAR(Path outDirectoryPath) throws IOException {
		Files.createDirectories(outDirectoryPath);
		setInformation();
		SAC sac = new SAC();
		setHeaderOn(sac);
		for (int i = 0; i < primeSPC.nbody(); i++) {
			SpcBody body = primeSPC.getSpcBodyList().get(i).copy();
			if (secondarySPC != null)
				body.addBody(secondarySPC.getSpcBodyList().get(i));
			compute(body);
			String bodyR = Utilities.toStringWithD(2, primeSPC.getBodyR()[i]);
			for (SACComponent component : components) {
				// System.out.println(component);
				SACExtension ext = sourceTimeFunction != null ? SACExtension.valueOfConvolutedSynthetic(component)
						: SACExtension.valueOfSynthetic(component);
				SACFileName sacFileName = new SACFileName(outDirectoryPath.resolve(station.getStationName() + "."
						+ globalCMTID + "." + primeSPC.getSpcFileType() + "..." + bodyR + "." + ext));
				if (sacFileName.exists()) {
					System.err.println(sacFileName + " already exists..");
					return;
				}
				SAC outSAC = sac.of(component);
				outSAC = outSAC.setSACData(body.getTimeseries(component));
				outSAC.writeSAC(sacFileName.toPath());
				// System.exit(0);
			}
		}

	}

	@Override
	public void run() {
		setInformation();
		SAC sac = new SAC();
		setHeaderOn(sac);
		SpcBody body = primeSPC.getSpcBodyList().get(0).copy();
		if (secondarySPC != null)
			body.addBody(secondarySPC.getSpcBodyList().get(0));

		if (temporalDifferentiation) {
			SpcBody bodyT = null;
			bodyT = body.copy();
			bodyT.differentiate(primeSPC.tlen());
			compute(bodyT);
			for (SACComponent component : components) {
				SACExtension extT = sourceTimeFunction != null
						? SACExtension.valueOfConvolutedTemporalPartial(component)
						: SACExtension.valueOfTemporalPartial(component);
				SACFileName sfnT = new SACFileName(
						outDirectoryPath.resolve(station.getStationName() + "." + globalCMTID + "." + extT));
				SAC sfT = sac.of(component);
				sfT.setSACData(bodyT.getTimeseries(component));
				try {
					sfT.writeSAC(sfnT.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		compute(body);

		for (SACComponent component : components) {
			// System.out.println(component);
			SACExtension ext = sourceTimeFunction != null ? SACExtension.valueOfConvolutedSynthetic(component)
					: SACExtension.valueOfSynthetic(component);
			SACFileName sfn = new SACFileName(
					outDirectoryPath.resolve(station.getStationName() + "." + globalCMTID + "." + ext));
			SAC outSAC = sac.of(component);
			outSAC = outSAC.setSACData(body.getTimeseries(component));
			try {
				outSAC.writeSAC(sfn.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * set headers on the input sacFile
	 * 
	 * @param sacFile
	 * @param sacFile
	 * @param component
	 */
	private void setHeaderOn(SAC sac) {
		if (beginDateTime != null)
			sac.setEventTime(beginDateTime);
		sac.setValue(SACHeaderEnum.B, 0);

		sac.setStation(station);
		sac.setEventLocation(primeSPC.getSourceLocation());
		sac.setSACString(SACHeaderEnum.KEVNM, globalCMTID.toString());

		sac.setValue(SACHeaderEnum.GCARC, FastMath.toDegrees(path.getEpicentralDistance()));
		sac.setValue(SACHeaderEnum.AZ, FastMath.toDegrees(path.getAzimuth()));
		sac.setValue(SACHeaderEnum.BAZ, FastMath.toDegrees(path.getBackAzimuth()));

		sac.setInt(SACHeaderEnum.NPTS, npts);
		sac.setValue(SACHeaderEnum.E, delta * npts);
		sac.setValue(SACHeaderEnum.DELTA, delta);

	}

	/**
	 * compute {@link SpcBody} for output.
	 * 
	 * @param body
	 */
	private void compute(SpcBody body) {

		if (sourceTimeFunction != null)
			body.applySourceTimeFunction(sourceTimeFunction);
		body.toTimeDomain(lsmooth);
		body.applyGrowingExponential(primeSPC.omegai(), primeSPC.tlen());
		body.amplitudeCorrection(primeSPC.tlen());
	}

	public void setComponents(Set<SACComponent> components) {
		this.components = components;
	}

	private int findLsmooth() {
		int np = Integer.highestOneBit(primeSPC.np());
		if (np < primeSPC.np())
			np *= 2;
		int lsmooth = npts / np / 2;
		int i = Integer.highestOneBit(lsmooth);
		if (i < lsmooth)
			i *= 2;
		return i;
	}

	/**
	 * @param outPath
	 *            {@link Path} of a foldew which will conatin output sac files.
	 */
	public void setOutPath(Path outPath) {
		outDirectoryPath = outPath;
	}

	/**
	 * TODO
	 * 
	 * @param spc1
	 * @param spc2
	 * @return if spc1 and spc2 have same information
	 */
	private static boolean check(DSMOutput spc1, DSMOutput spc2) {
		boolean isOK = true;
		if (spc1.nbody() != spc2.nbody()) {
			System.err
					.println("Numbers of bodies (nbody) are different. fp, bp: " + spc1.nbody() + " ," + spc2.nbody());
			isOK = false;
		}

		if (!spc1.getSourceID().equals(spc2.getSourceID())) {
			System.err.println("Source names are different " + spc1.getSourceID() + " " + spc2.getSourceID());
			isOK = false;
		}
		if (isOK) {
			if (!Arrays.equals(spc1.getBodyR(), spc2.getBodyR()))
				isOK = false;

			if (!isOK) {
				System.err.println("the depths are invalid(different) as below  fp : bp");
				for (int i = 0; i < spc1.nbody(); i++)
					System.out.println(spc1.getBodyR()[i] + " : " + spc2.getBodyR()[i]);
			}
		}
		if (spc1.np() != spc2.np()) {
			System.err.println("nps are different. fp, bp: " + spc1.np() + ", " + spc2.np());
			isOK = false;
		}

		// double
		// tlen
		if (spc1.tlen() != spc2.tlen()) {
			System.out.println("tlens are different. fp, bp: " + spc1.tlen() + " ," + spc2.tlen());
			isOK = false;
		}

		// 場所
		if (!spc1.getSourceLocation().equals(spc2.getSourceLocation())) {
			System.out.println("locations of sources of input spcfiles are different");
			System.out.println(spc1.getSourceLocation());
			System.out.println(spc2.getSourceLocation());
			isOK = false;
		}
		if (!spc1.getObserverPosition().equals(spc2.getObserverPosition())) {
			System.out.println("locations of stations of input spcfiles are different");
			isOK = false;
		}
		return isOK;
	}

	/**
	 * Creates and outputs synthetic SAC files of Z R T from input spectrums
	 * 
	 * @param args
	 *            [onespc] [pairspc]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		if (args == null || args.length != 2) {
			System.out.println("Usage: psv sh");
			return;
		}

		SpcFileName oneName = new SpcFileName(args[0]);
		SpcFileName pairName = new SpcFileName(args[1]);

		SpectrumFile pairSPC = SpectrumFile.getInstance(pairName);
		SpectrumFile oneSPC = SpectrumFile.getInstance(oneName);

		SACMaker sm = new SACMaker(oneSPC, pairSPC);
		sm.setOutPath(Paths.get(System.getProperty("user.dir")));
		sm.run();
	}

}
