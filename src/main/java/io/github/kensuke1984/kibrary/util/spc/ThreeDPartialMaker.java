package io.github.kensuke1984.kibrary.util.spc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.FastMath;

import com.amazonaws.util.CollectionUtils;

import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * Create a partial derivative from one {@link ForwardPropagation} and one
 * {@link BackwardPropagation}<br>
 * <p>
 * U<sub>j,q</sub> C<sub>jqrs</sub> &eta;<sub>ri,s</sub>
 * 
 * 
 * @version 0.0.2.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class ThreeDPartialMaker {

	/**
	 * forward propagation
	 */
	private DSMOutput fp;

	/**
	 * back propagation
	 */
	private DSMOutput bp;

	/**
	 * SACファイルにするときのサンプリング値 デフォルト20Hz
	 */
	private double samplingFrequency = 20;

	private int npts;

	/**
	 * 摂動点における bpからのテンソルをfpのテンソルに合わせるために回転させる角度
	 */
	private double angleForTensor;

	/**
	 * 
	 * 偏微分波形成分を震源観測点大円上の座標に合わせる角度
	 */
	private double angleForVector;

	private int lsmooth;
	
	Set<Double> ignoreBodyR;

	/**
	 * 用いたいspcファイルたちと ヘッダーに加えたい情報
	 * 
	 * @param fp
	 *            a spc file for forward propagation
	 * @param bp
	 *            a spc file for back propagation
	 */
	public ThreeDPartialMaker(DSMOutput fp, DSMOutput bp) {
		ignoreBodyR = new HashSet<>();
		if (!isGoodPairPermissive(fp, bp)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		ignoreBodyR.forEach(System.out::println);
		this.fp = fp;
		this.bp = bp;
		findLsmooth();
		setAngles();
	}

	/**
	 * Create a {@link DSMOutput} from a {@link ForwardPropagation} and a
	 * {@link BackwardPropagation}.
	 * 
	 * @param type
	 *            {@link PartialType}
	 * @return {@link PartialSpectrumFile}
	 */
	public DSMOutput toSpectrum(PartialType type) {
		SpcFileName spcFileName = bp.getSpcFileName();
		double tlen = bp.tlen();
		int np = bp.np();
		int nbody = bp.nbody();
		double omegai = bp.omegai();
		HorizontalPosition observerPosition = bp.getSourceLocation();
		String observerID = bp.getSourceID();
		String observerName = bp.getObserverName();
		String observerNetwork = bp.getObserverNetwork();
		Location sourceLocation = fp.getSourceLocation();
		String sourceID = fp.getSourceID();
		double[] bodyR = bp.getBodyR();
		List<SpcBody> spcBodyList = new ArrayList<>(nbody);
		for (int ibody = 0; ibody < nbody; ibody++) {
			TensorCalculationUCE tensorcalc = new TensorCalculationUCE(fp.getSpcBodyList().get(ibody),
					bp.getSpcBodyList().get(ibody), type.getWeightingFactor(), angleForTensor);
			// tensorcalc.setBP(angleBP);
			// tensorcalc.setFP(angleFP);
			 System.out.println("angleForTensor " + angleForTensor);
			Complex[] partialZ = tensorcalc.calc(0); // frequency domain Z
			Complex[] partial1 = tensorcalc.calc(1); // R
			Complex[] partial2 = tensorcalc.calc(2); // T

			Complex[] partialR = rotatePartial(partial1, partial2, SACComponent.R);
			Complex[] partialT = rotatePartial(partial1, partial2, SACComponent.T);
			SpcBody body = new SpcBody(3, np);
			for (int ip = 0; ip < bp.np() + 1; ip++)
				body.add(ip, partialZ[ip], partialR[ip], partialT[ip]);
			spcBodyList.add(body);
		}
		return new DSMOutput() {

			@Override
			public double tlen() {
				return tlen;
			}

			@Override
			public double omegai() {
				return omegai;
			}

			@Override
			public int np() {
				return np;
			}

			@Override
			public int nbody() {
				return nbody;
			}

			@Override
			public SpcFileType getSpcFileType() {
				return type.toSpcFileType();
			}

			@Override
			public List<SpcBody> getSpcBodyList() {
				return spcBodyList;
			}

			@Override
			public Location getSourceLocation() {
				return sourceLocation;
			}

			@Override
			public String getSourceID() {
				return sourceID;
			}

			@Override
			public HorizontalPosition getObserverPosition() {
				return observerPosition;
			}

			@Override
			@Deprecated
			public String getObserverID() {
				return observerID;
			}
			
			@Override
			public String getObserverName() {
				return observerName;
			}
			
			@Override
			public String getObserverNetwork() {
				return observerNetwork;
			}

			@Override
			public double[] getBodyR() {
				return bodyR;
			}
			
			@Override
			public SpcFileName getSpcFileName() {
				return spcFileName;
			}
		};
	}

	/**
	 * ibody番目のボディ（深さ）に対する摂動の Partial derivatives のiに対する成分 ETAri,s の i
	 * 
	 * @param component
	 *            {@link SACComponent}
	 * @param iBody
	 *            index for SacBody
	 * @param type
	 *            {@link PartialType}
	 * @return Ui(t) u[t] 時間領域
	 */
	public double[] createPartial(SACComponent component, int iBody, PartialType type) {
		// return array of zero for partials whose radius is too close to the BP or FP source
		double bpR = bp.getBodyR()[iBody];
		int iFp = -1;
		for (int i = 0; i < fp.getBodyR().length; i++) {
			if (fp.getBodyR()[i] == bpR) {
				iFp = i;
				break;
			}
		}
		if (iFp == -1)
			return new double[npts];
		//
		
		Complex[] partial_frequency = type == PartialType.Q ? computeQpartial(component, iBody)
				: computeTensorCulculus(component, iBody, iFp, type);
		if (null != sourceTimeFunction)
			partial_frequency = sourceTimeFunction.convolve(partial_frequency);
		Complex[] partial_time = toTimedomain(partial_frequency);
		double[] partialdouble = new double[npts];
		for (int j = 0; j < npts; j++)
			partialdouble[j] = partial_time[j].getReal();
		Arrays.stream(partial_time).mapToDouble(Complex::abs).toArray();
		return partialdouble;
	}

	private FujiConversion fujiConversion;

	/**
	 * The structure is used for computation Q
	 * 
	 * @param structure
	 *            {@link PolynomialStructure}
	 */
	public void setStructure(PolynomialStructure structure) {
		fujiConversion = new FujiConversion(structure);
	}

	private Complex[] computeQpartial(SACComponent component, int iBody) {
		if (fujiConversion == null)
			fujiConversion = new FujiConversion(PolynomialStructure.PREM);
		DSMOutput qspec = fujiConversion.convert(toSpectrum(PartialType.MU));
		return qspec.getSpcBodyList().get(iBody).getSpcComponent(component).getValueInFrequencyDomain();

	}
	
	// TODO
//	private Complex[] computeQpartial(SACComponent component, int iBody) {
//		if (fujiConversion == null)
//			fujiConversion = new FujiConversion(PolynomialStructure.PREM);
//		DSMOutput qspec = fujiConversion.convert(toSpectrum(PartialType.MU));
//		return qspec.getSpcBodyList().get(iBody).getSpcComponent(component).getValueInFrequencyDomain();
//	}

	/**
	 * compute tensor culculus of u Cijkl eta
	 * 
	 * @param component
	 *            {@link SACComponent}
	 * @param iBody
	 *            index for sacbody
	 * @param type
	 *            {@link PartialType}
	 * @return uCe
	 */
	private Complex[] computeTensorCulculus(SACComponent component, int iBody, PartialType type) {
		TensorCalculationUCE tensorcalc = new TensorCalculationUCE(fp.getSpcBodyList().get(iBody),
				bp.getSpcBodyList().get(iBody), type.getWeightingFactor(), angleForTensor);
		return component == SACComponent.Z ? tensorcalc.calc(0)
				: rotatePartial(tensorcalc.calc(1), tensorcalc.calc(2), component);
	}
	
	private Complex[] computeTensorCulculus(SACComponent component, int iBodyBp, int iBodyFp, PartialType type) {
		TensorCalculationUCE tensorcalc = new TensorCalculationUCE(fp.getSpcBodyList().get(iBodyFp),
				bp.getSpcBodyList().get(iBodyBp), type.getWeightingFactor(), angleForTensor);
		return component == SACComponent.Z ? tensorcalc.calc(0)
				: rotatePartial(tensorcalc.calc(1), tensorcalc.calc(2), component);
	}

	private SourceTimeFunction sourceTimeFunction;

	public void setSourceTimeFunction(SourceTimeFunction sourceTimeFunction) {
		this.sourceTimeFunction = sourceTimeFunction;
	}

	/**
	 * 
	 * 周波数領域のデータにしか使えない
	 * 
	 * @param partial1
	 *            partial in local cartesian
	 * @param partial2
	 *            partial in local cartesian
	 * @param component
	 *            R, T 震源 観測点の乗る大円上
	 * @return 回転させてできたi成分の偏微分波形
	 */
	private Complex[] rotatePartial(Complex[] partial1, Complex[] partial2, SACComponent component) {
		Complex[] partial = new Complex[fp.np() + 1];
		
		double cosine = FastMath.cos(angleForVector);
		double sine = FastMath.sin(angleForVector);
//		double e = 1e-2;
//		double absAngle = Math.abs(angleForVector);
//		if (absAngle < Math.PI/2. + e && absAngle > Math.PI/2. - e) {
//			System.out.println("Set cosine to 0");
//			cosine = 0.;
//		}
//		else if (absAngle < Math.PI + e && absAngle > Math.PI - e) {
//			System.out.println("Set sine to 0");
//			sine = 0.;
//		}
		
		switch (component) {
		case R:
			for (int j = 0; j < fp.np() + 1; j++)
				partial[j] = new Complex(
						cosine * partial1[j].getReal()
								+ sine * partial2[j].getReal(),
						cosine * partial1[j].getImaginary()
								+ sine * partial2[j].getImaginary());
			return partial;
		case T:
			for (int j = 0; j < fp.np() + 1; j++)
				partial[j] = new Complex(
						-sine * partial1[j].getReal()
								+ cosine * partial2[j].getReal(),
						-sine * partial1[j].getImaginary()
								+ cosine * partial2[j].getImaginary());
			return partial;
		default:
			System.out.println(Thread.currentThread().getStackTrace()[1].getMethodName());
			System.out.println("an input component is invalid");
			return null;
		}

	}

	/**
	 * 座標軸回転に必要な角度の計算 Z軸を中心に angleForTensor：
	 * bpの（ローカル座標）を回してfpのテンソルに合わせる（Zは一致しているため） angleForVector 得られたｆiに対する応答を回転する
	 * （北極に持って行って、東西南北ベースに力を入れているのでそれを大円内に戻す）
	 */
	private void setAngles() {
		HorizontalPosition event = fp.getSourceLocation();
		HorizontalPosition station = bp.getSourceLocation();
		HorizontalPosition point = bp.getObserverPosition();
		angleForTensor = Earth.getAzimuth(point, station) - Earth.getAzimuth(point, event);

		angleForVector = 2 * Math.PI - Earth.getAzimuth(station, event);
		
//		System.out.println(event + " " + station + " " + point);
//		System.out.println(angleForTensor*180/Math.PI + " " + angleForVector*180/Math.PI);
	}

	/**
	 * 時間領域のデータにGrowingExponentialを考慮する
	 * 
	 */
	private void applyGrowingExponential(Complex[] uTime) {
		final double x = bp.tlen() * fp.omegai() / npts;
		for (int i = 0; i < npts; i++)
			uTime[i] = uTime[i].multiply(Math.exp(i * x));

	}

	/**
	 * after toTimeDomain
	 * 
	 * @param tlen
	 *            time length
	 */
	private void correctAmplitude(Complex[] uTime) {
		final double tmp = npts * 1e3 / bp.tlen();
		for (int i = 0; i < npts; i++)
			uTime[i] = uTime[i].multiply(tmp);
	}

	/**
	 * compute waveform in timedomain from spector in frequency domain. aplitude
	 * correction and growing exponential will be considered.
	 * 
	 * @param spector
	 * @return
	 */
	private Complex[] toTimedomain(Complex[] spector) {
		Complex[] partial_time = inverseFourierTransform(spector);
		applyGrowingExponential(partial_time);
		correctAmplitude(partial_time);
		return partial_time;
	}

	/**
	 * input complexを時間領域に
	 * 
	 * @param complex
	 *            waveform in frequency domain
	 * @return 時間領域の複素数列
	 */
	private Complex[] inverseFourierTransform(Complex[] complex) {
		// must be tested
		int nnp = fp.np() * lsmooth;
		FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

		Complex[] data = new Complex[nnp * 2];

		// pack to temporary Complex array
		System.arraycopy(complex, 0, data, 0, fp.np() + 1);

		// set blank due to lsmooth
		Arrays.fill(data, fp.np() + 1, nnp + 1, Complex.ZERO);

		// set values for imaginary frequency
		for (int i = 0; i < nnp - 1; i++)
			data[nnp + i + 1] = data[nnp - i - 1].conjugate();

		// fast fourier transformation
		return fft.transform(data, TransformType.INVERSE); // check

	}

	/**
	 * frequency domain をsamplingFrequencyでtime-domain tlen(s)にもってくるスムージング値を探す
	 * 
	 */
	private void findLsmooth() {
		int np = Integer.highestOneBit(fp.np());
		if (np < fp.np())
			np *= 2;

		lsmooth = (int) (0.5 * bp.tlen() * samplingFrequency / np);
		int i = Integer.highestOneBit(lsmooth);
		if (i < lsmooth)
			i *= 2;
		lsmooth = i;
		npts = np * lsmooth * 2;
	}

	/**
	 * SpcFileから３次元偏微分係数を作れるか
	 * 
	 * @param fp
	 *            {@link ForwardPropagation}
	 * @param bp
	 *            {@link BackwardPropagation}
	 * @return if the pair of fp and bp is valid for making partials.
	 */
	private static boolean isGoodPair(DSMOutput fp, DSMOutput bp) {
		boolean validity = true;
		if (fp.nbody() != bp.nbody()) {
			System.err.println("nbodies are different. fp, bp: " + fp.nbody() + " ," + bp.nbody());
			validity = false;
		}
		if (validity) {
			double[] fpR = fp.getBodyR();
			double[] bpR = bp.getBodyR();
			validity = Arrays.equals(fpR, bpR);
			if (!validity) {
				System.err.println("the depths are invalid(different) as below  fp : bp");
				for (int i = 0; i < fpR.length; i++)
					System.err.println(fpR[i] + " : " + bpR[i]);
			}
		}
		if (fp.omegai() != bp.omegai()) {
			System.err.println("Omegais are different. fp, bp: " + fp.omegai() + ", " + bp.omegai());
			validity = false;
		}

		if (fp.np() != bp.np()) {
			System.err.println("nps are different. fp, bp: " + fp.np() + ", " + bp.np());
			validity = false;
		}
		// tlen
		if (fp.tlen() != bp.tlen()) {
			System.err.println("tlens are different. fp, bp: " + fp.tlen() + " ," + bp.tlen());
			validity = false;
		}
		// 摂動点名は同じかどうか
//		if (!(fp.getObserverID().equals(bp.getObserverID()))) {
//			System.err.println(
//					"Perturbation points are different fp, bp: " + fp.getObserverID() + " ," + bp.getObserverID());
//			validity = false;
//		}
		if (!(fp.getObserverName().equals(bp.getObserverName()))) {
			System.err.println(
					"Perturbation points are different fp, bp: " + fp.getObserverName() + " ," + bp.getObserverName());
			validity = false;
		}
		// FP and BP have no observation network, thus we do no check it

		// 場所
		if (!fp.getObserverPosition().equals(bp.getObserverPosition())) {
			System.err.println("perturbation point Positions are different.");
			System.err.println("perturbation point of fp, bp are" + "(" + fp.getObserverPosition().getLatitude() + ", "
					+ fp.getObserverPosition().getLongitude() + "), (" + bp.getObserverPosition().getLatitude() + ", "
					+ bp.getObserverPosition().getLongitude() + ")");
			validity = false;
		}
		return validity;
	}

	private boolean isGoodPairPermissive(DSMOutput fp, DSMOutput bp) {
		boolean validity = true;
		if (Math.abs(fp.nbody() - bp.nbody()) > 2) {
			System.err.println("nbodies are different by more than 2. fp, bp: " + fp.nbody() + " ," + bp.nbody());
			validity = false;
		}
		if (validity) {
			double[] fpR = fp.getBodyR();
			double[] bpR = bp.getBodyR();
			for (int i = 0; i < fpR.length; i++) {
				double fpRi = fpR[i];
				boolean isInBpR = false;
				for (int j = 0; j < bpR.length; j++) {
					if (fpRi == bpR[j]) {
						isInBpR = true;
						break;
					}
				}
				if (!isInBpR)
					ignoreBodyR.add(fpRi);
			}
			
//			Set<Double> fpRSet = Arrays.stream(fpR).boxed().collect(Collectors.toSet());
//			Set<Double> bpRSet = Arrays.stream(bpR).boxed().collect(Collectors.toSet());
//			Set<Double> bpRSet_copy = new HashSet<>(bpRSet);
//			bpRSet.removeAll(fpRSet);
//			fpRSet.removeAll(bpRSet_copy);
//			ignoreBodyR.addAll(bpRSet);
//			ignoreBodyR.addAll(fpRSet);
//			ignoreBodyR.forEach(r -> System.out.println(r));
			
			validity = ignoreBodyR.size() <= 2;
			if (!validity) {
				System.err.println("the depths are invalid (different) as below  fp : bp");
				for (int i = 0; i < fpR.length; i++)
					System.err.println(fpR[i] + " : " + bpR[i]);
			}
		}
		if (fp.omegai() != bp.omegai()) {
			System.err.println("Omegais are different. fp, bp: " + fp.omegai() + ", " + bp.omegai());
			validity = false;
		}

		if (fp.np() != bp.np()) {
			System.err.println("nps are different. fp, bp: " + fp.np() + ", " + bp.np());
			validity = false;
		}
		// tlen
		if (fp.tlen() != bp.tlen()) {
			System.err.println("tlens are different. fp, bp: " + fp.tlen() + " ," + bp.tlen());
			validity = false;
		}
		// 摂動点名は同じかどうか
//		if (!(fp.getObserverID().equals(bp.getObserverID()))) {
//			System.err.println(
//					"Perturbation points are different fp, bp: " + fp.getObserverID() + " ," + bp.getObserverID());
//			validity = false;
//		}
		if (!(fp.getObserverName().equals(bp.getObserverName()))) {
			System.err.println(
					"Perturbation points are different fp, bp: " + fp.getObserverName() + " ," + bp.getObserverName());
			validity = false;
		}
		// FP and BP have no observation network, thus we do no check it

		// 場所
		if (!fp.getObserverPosition().equals(bp.getObserverPosition())) {
			System.err.println("perturbation point Positions are different.");
			System.err.println("perturbation point of fp, bp are" + "(" + fp.getObserverPosition().getLatitude() + ", "
					+ fp.getObserverPosition().getLongitude() + "), (" + bp.getObserverPosition().getLatitude() + ", "
					+ bp.getObserverPosition().getLongitude() + ")");
			validity = false;
		}
		return validity;
	}
}
