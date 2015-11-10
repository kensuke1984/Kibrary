package filehandling.sac;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import manhattan.butterworth.BandPassFilter;
import manhattan.butterworth.BandStopFilter;
import manhattan.butterworth.ButterworthFilter;
import manhattan.butterworth.HighPassFilter;
import manhattan.butterworth.LowPassFilter;

/**
 * SACのfileに対するクラス<br>
 * (SAC: Seismic analysis code)
 * 
 * 
 * @since 2013/9/16
 * @version 0.0.2
 * 
 * @version 0.0.3
 * @since 2013/10/1 データをすぐ読み込むようにした
 * 
 * @version 0.0.4
 * @since 2014/8/22 {@link #applyButterworthFilter(ButterworthFilter)} modified
 * 
 * @version 0.0.5
 * @since 2014/8/25 {@link #applyButterworthFilter(ButterworthFilter)} modified
 *        Deletes depricated methods
 * 
 * @version 0.0.6
 * @since 2014/9/8 {@link #read()} fix
 * 
 * @version 0.0.6.1
 * @since 2015/3/31 Use Arrays
 * 
 * @version 0.0.7
 * @since 2015/8/5 If file has any problems, it throws {@link IOException}
 * 
 * @version 0.0.7.1 {@link #writeSAC(Path, OpenOption...)} also throws
 *          {@link IOException}.
 * 
 * @version 0.0.8
 * @since 2015/8/15 Stream
 * 
 * @version 0.0.9
 * @since 2015/8/23 cut method is cut
 * 
 * @version 1.0.0
 * @since 2015/9/12 package access
 * IMMUTABLE
 * 
 * @author Kensuke
 * 
 */
class SACFile extends SACHeader implements SACData {

	/**
	 * waveform data in the sacfile
	 */
	private double[] waveData;

	/**
	 * 
	 * @param sacFileName
	 *            the {@link SACFileName} of the sacfile
	 * @throws IOException
	 *             If an I/O error occurs. in case like a sac file named the
	 *             sacFileName is broken.
	 */
	SACFile(SACFileName sacFileName) throws IOException {
		super(sacFileName);
		read(sacFileName);
	}

	/**
	 * filterをかける Apply {@link ButterworthFilter}
	 * 
	 * @param filter
	 *            to apply on this
	 */
	@Override
	public SACData applyButterworthFilter(ButterworthFilter filter) {
		SACData sd = clone();
		if (filter instanceof BandPassFilter) {
			BandPassFilter bp = (BandPassFilter) filter;
			double periodMax = 2.0 * Math.PI * sd.getValue(SACHeaderEnum.DELTA) / bp.getOmegaL();
			double periodMin = 2.0 * Math.PI * sd.getValue(SACHeaderEnum.DELTA) / bp.getOmegaH();
			sd = sd.setValue(SACHeaderEnum.USER0, periodMin).setValue(SACHeaderEnum.USER1, periodMax);
		} else if (filter instanceof LowPassFilter) {
			LowPassFilter lp = (LowPassFilter) filter;
			double periodMin = 2.0 * Math.PI * getValue(SACHeaderEnum.DELTA) / lp.getOmegaP();
			sd = sd.setValue(SACHeaderEnum.USER0, periodMin);
		} else if (filter instanceof HighPassFilter) {
			HighPassFilter hp = (HighPassFilter) filter;
			double periodMax = 2.0 * Math.PI * getValue(SACHeaderEnum.DELTA) / hp.getOmegaP();
			sd = sd.setValue(SACHeaderEnum.USER1, periodMax);
		} else if (filter instanceof BandStopFilter) {
			BandStopFilter bsf = (BandStopFilter) filter;
			double periodMin = 2 * Math.PI * getValue(SACHeaderEnum.DELTA) / bsf.getOmegaL();
			double periodMax = 2 * Math.PI * getValue(SACHeaderEnum.DELTA) / bsf.getOmegaH();
			sd = sd.setValue(SACHeaderEnum.USER0, periodMax).setValue(SACHeaderEnum.USER1, periodMin);
		}

		double[] sacdata = filter.applyFilter(waveData.clone());
		sd = sd.setSACData(sacdata);
		return sd;
	}

 

	/**
	 * Sacの波形部分を読み込む read sacdata from this.sacFile
	 */
	private void read(SACFileName sacFileName) throws IOException {
		try (SACInputStream stream = new SACInputStream(sacFileName.toPath())) {
			stream.skipBytes(632);
			int npts = getInt(SACHeaderEnum.NPTS);
			waveData = new double[npts];
			// float(4) * 70, int(4) * 40, String (8) * 22 + (16)
			// 4* 70 + 4* 40 + 8* 22 +16 = 632
			for (int i = 0; i < npts; i++)
				waveData[i] = stream.readFloat();
		}
	}
	@Override
	public SACFile setSACData(double[] sacData) {
		// setInt(SacHeaderEnum.NPTS, npts);
		int npts = getInt(SACHeaderEnum.NPTS);

		if (npts != sacData.length)
			throw new IllegalStateException("input npts is invalid. SAC npts" + npts + " input npts:" + npts);
		SACFile sf = clone();
		sf.waveData = sacData.clone();
		return sf;
	}

	
	@Override
	public SACFile clone() {
		try {
			SACFile sf = (SACFile) super.clone();
			sf.waveData = waveData.clone();
			return sf;

		} catch (Exception e) {
			throw new RuntimeException("UNExPECTed");
		}
	}

	@Override
	public SACFile setValue(SACHeaderEnum sacHeaderEnum, double value) {
		return (SACFile) super.setValue(sacHeaderEnum, value);
	}

	@Override
	public SACFile setBoolean(SACHeaderEnum sacHeaderEnum, boolean bool) {
		return (SACFile) super.setBoolean(sacHeaderEnum, bool);
	};

	@Override
	public SACFile setInt(SACHeaderEnum sacHeaderEnum, int value) {
		return (SACFile) super.setInt(sacHeaderEnum, value);
	};

	@Override
	public SACFile setSACEnumerated(SACHeaderEnum sacHeaderEnum, int value) {
		return (SACFile) super.setSACEnumerated(sacHeaderEnum, value);
	};

	@Override
	public SACFile setSACString(SACHeaderEnum sacHeaderEnum, String string) {
		return (SACFile) super.setSACString(sacHeaderEnum, string);
	}

	@Override
	public double[] getData() {
		return waveData.clone();
	};

}
