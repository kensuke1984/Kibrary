package io.github.kensuke1984.kibrary.util.spc;

import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;

/**
 * Create a partial derivative from one forward propagation and one
 * backward propagation
 * <p>
 * U<sub>j,q</sub> C<sub>jqrs</sub> &eta;<sub>ri,s</sub>
 *
 * @author Kensuke Konishi
 * @version 0.0.2.1
 */
public class ThreeDPartialMaker {

	/**
	 * forward propagation
	 */
	private DSMOutput fp;
	private DSMOutput fp2;
	private DSMOutput fp3;
	/**
	 * back propagation
	 */
	private DSMOutput bp;
	private DSMOutput bp2;
	private DSMOutput bp3;
	/**
	 * distances for interpolation
	 */
	double[] dh;
	double[] dhFP;
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
    private FujiConversion fujiConversion;
    private SourceTimeFunction sourceTimeFunction;
	Set<Double> ignoreBodyR;

	 /**
     * 用いたいspcファイルたちと ヘッダーに加えたい情報
     *
     * @param fp a spc file for forward propagation
     * @param bp a spc file for back propagation
     */
	public ThreeDPartialMaker(DSMOutput fp, DSMOutput bp) {
		ignoreBodyR = new HashSet<>();
		if (!isGoodPair(fp, bp))
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		ignoreBodyR.forEach(System.out::println);
		this.fp = fp;
		this.bp = bp;
		this.bp2 = null;
		this.bp3 = null;
		this.fp2 = null;
		this.fp3 = null;
		this.dh = null;
		findLsmooth();
		setAngles();
	}
	
	/**
	 * Used for BP/FP catalog
	 * @param fp
	 * @param bp1
	 * @param bp2
	 * @param bp3
	 * @param dh
	 * @author anselme
	 */
	public ThreeDPartialMaker(DSMOutput fp, DSMOutput bp1, DSMOutput bp2, DSMOutput bp3, double[] dh) {
		ignoreBodyR = new HashSet<>();
		if (!isGoodPairPermissive(fp, bp1)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		if (!isGoodPairPermissive(fp, bp2)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		if (!isGoodPairPermissive(fp, bp3)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		ignoreBodyR.forEach(System.out::println);
		this.fp = fp;
		this.fp2 = null;
		this.fp3 = null;
		this.bp = bp1;
		this.bp2 = bp2;
		this.bp3 = bp3;
		this.dh = dh;
		findLsmooth();
		setAngles();
	}
	
	/**
	 * Used for BP/FP catalog
	 * @param fpSH
	 * @param fpPSV
	 * @param bp1SH
	 * @param bp1PSV
	 * @param bp2SH
	 * @param bp2PSV
	 * @param bp3SH
	 * @param bp3PSV
	 * @param dh
	 * @author anselme
	 */
	public ThreeDPartialMaker(DSMOutput fpSH, DSMOutput fpPSV, DSMOutput bp1SH,
			DSMOutput bp1PSV, DSMOutput bp2SH, DSMOutput bp2PSV, DSMOutput bp3SH, DSMOutput bp3PSV, double[] dh) {
		ignoreBodyR = new HashSet<>();
		if (!isGoodPairPermissive(fpSH, bp1SH)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		if (!isGoodPairPermissive(fpSH, bp2SH)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		if (!isGoodPairPermissive(fpSH, bp3SH)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		ignoreBodyR.forEach(System.out::println);
		
		this.fp = fpSH;
		this.bp = bp1SH;
		this.bp2 = bp2SH;
		this.bp3 = bp3SH;
//		System.out.println(fp.getSpcBodyList().get(0).getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		for (int i = 0; i < fpSH.nbody(); i++) {
			SPCBody body = fpPSV.getSpcBodyList().get(i).copy();
			this.fp.getSpcBodyList().get(i).addBody(body);
		}
		
//		System.out.println(fp.getSpcBodyList().get(0).getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		for (int i = 0; i < bp1SH.nbody(); i++) {
			SPCBody body = bp1PSV.getSpcBodyList().get(i).copy();
			this.bp.getSpcBodyList().get(i).addBody(body);
			
			SPCBody body2 = bp2PSV.getSpcBodyList().get(i).copy();
			this.bp2.getSpcBodyList().get(i).addBody(body2);
			
			SPCBody body3 = bp3PSV.getSpcBodyList().get(i).copy();
			this.bp3.getSpcBodyList().get(i).addBody(body3);
		}
//		System.out.println("PSV and SH added");
		
		this.fp2 = null;
		this.fp3 = null;
		this.dh = dh;
		findLsmooth();
		setAngles();
	}
	

	/**
	 * Used for BP/FP catalog
	 * @param fp1
	 * @param fp2
	 * @param fp3
	 * @param bp1
	 * @param bp2
	 * @param bp3
	 * @param dhBP
	 * @param dhFP
	 * @author anselme
	 */
	public ThreeDPartialMaker(DSMOutput fp1, DSMOutput fp2, DSMOutput fp3, DSMOutput bp1, DSMOutput bp2, DSMOutput bp3, double[] dhBP, double[] dhFP) {
		ignoreBodyR = new HashSet<>();
		if (!isGoodPairPermissive(fp1, bp1)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		if (!isGoodPairPermissive(fp2, bp2)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		if (!isGoodPairPermissive(fp3, bp3)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		ignoreBodyR.forEach(System.out::println);
		this.fp = fp1;
		this.fp2 = fp2;
		this.fp3 = fp3;
		this.bp = bp1;
		this.bp2 = bp2;
		this.bp3 = bp3;
		this.dh = dhBP;
		this.dhFP = dhFP;
		findLsmooth();
		setAngles();
	}
	

	/**
	 * Used for BP/FP catalog
	 * @param fp1PSV
	 * @param fp1SH
	 * @param fp2PSV
	 * @param fp2SH
	 * @param fp3PSV
	 * @param fp3SH
	 * @param bp1PSV
	 * @param bp1SH
	 * @param bp2PSV
	 * @param bp2SH
	 * @param bp3PSV
	 * @param bp3SH
	 * @param dhBP
	 * @param dhFP
	 * @author anselme
	 */
	public ThreeDPartialMaker(DSMOutput fp1PSV, DSMOutput fp1SH, DSMOutput fp2PSV,  DSMOutput fp2SH, DSMOutput fp3PSV, DSMOutput fp3SH,
			DSMOutput bp1PSV, DSMOutput bp1SH, DSMOutput bp2PSV, DSMOutput bp2SH, DSMOutput bp3PSV, DSMOutput bp3SH, double[] dhBP, double[] dhFP) {
		ignoreBodyR = new HashSet<>();
		if (!isGoodPair(fp1SH, bp1SH)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		if (!isGoodPair(fp2SH, bp2SH)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		if (!isGoodPair(fp3SH, bp3SH)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		ignoreBodyR.forEach(System.out::println);
		
		this.fp = fp1PSV;
		this.fp2 = fp2PSV;
		this.fp3 = fp3PSV;
		this.bp = bp1PSV;
		this.bp2 = bp2PSV;
		this.bp3 = bp3PSV;
//		System.out.println(fp.getSpcBodyList().get(0).getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		for (int i = 0; i < fp1PSV.nbody(); i++) {
			SPCBody body = fp1SH.getSpcBodyList().get(i).copy();
			this.fp.getSpcBodyList().get(i).addBody(body);
			
			SPCBody body2 = fp2SH.getSpcBodyList().get(i).copy();
			this.fp2.getSpcBodyList().get(i).addBody(body2);
			
			SPCBody body3 = fp3SH.getSpcBodyList().get(i).copy();
			this.fp3.getSpcBodyList().get(i).addBody(body3);
		}
		
//		System.out.println(fp.getSpcBodyList().get(0).getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		for (int i = 0; i < bp1PSV.nbody(); i++) {
			SPCBody body = bp1SH.getSpcBodyList().get(i).copy();
			this.bp.getSpcBodyList().get(i).addBody(body);
			
			SPCBody body2 = bp2SH.getSpcBodyList().get(i).copy();
			this.bp2.getSpcBodyList().get(i).addBody(body2);
			
			SPCBody body3 = bp3SH.getSpcBodyList().get(i).copy();
			this.bp3.getSpcBodyList().get(i).addBody(body3);
		}
		
		this.dh = dhBP;
		this.dhFP = dhFP;
		findLsmooth();
		setAngles();
	}
	
	/**
	 * Used for BP/FP catalog
	 * @param fpSH
	 * @param fpPSV
	 * @param bpSH
	 * @param bpPSV
	 * @author anselme
	 */
	public ThreeDPartialMaker(DSMOutput fpSH, DSMOutput fpPSV, DSMOutput bpSH, DSMOutput bpPSV) {
		ignoreBodyR = new HashSet<>();
		
		this.fp = fpPSV;
		this.bp = bpPSV;
		
//		System.out.println(fpSH.getSpcFileName() + " " + fpPSV.getSpcFileName() + " " + bpSH.getSpcFileName() + " " + bpPSV.getSpcFileName());
		
//		System.out.println(fp.getSpcBodyList().get(0).getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		
		for (int i = 0; i < fpPSV.nbody(); i++) {
			SPCBody body = fpSH.getSpcBodyList().get(i).copy();
			this.fp.getSpcBodyList().get(i).addBody(body);
		}
		
//		System.out.println(fp.getSpcBodyList().get(0).getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		
		for (int i = 0; i < bpPSV.nbody(); i++) {
			SPCBody body = bpSH.getSpcBodyList().get(i).copy();
			this.bp.getSpcBodyList().get(i).addBody(body);
		}
//		System.out.println("PSV and SH added");
		
		if (!isGoodPairPermissive(fp, bp)) //isGoodPair
			throw new RuntimeException("An input pair of forward and backward propagation is invalid.");
		ignoreBodyR.forEach(System.out::println);
		
		this.bp2 = null;
		this.bp3 = null;
		this.fp2 = null;
		this.fp3 = null;
		this.dh = null;
		findLsmooth();
		setAngles();
	}
	
	/**
     * SpcFileから３次元偏微分係数を作れるか
     *
     * @param fp forward propagation
     * @param bp backward propagation
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
		if (!(fp.getObserverID().equals(bp.getObserverID()))) {
			System.err.println(
					"Perturbation points are different fp, bp: " + fp.getObserverID() + " ," + bp.getObserverID());
			validity = false;
		}
		// FP and BP have no observation network, thus we do not check it

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

	/**
	 * @param fp
	 * @param bp
	 * @return
	 * @author anselme
	 */
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
		if (!(fp.getObserverID().equals(bp.getObserverID()))) {
			System.err.println(
					"Perturbation points are different fp, bp: " + fp.getObserverID() + " ," + bp.getObserverID());
			validity = false;
		}
		// FP and BP have no observation network, thus we do not check it

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

    /**
     * Create a {@link DSMOutput} from a forward propagation and a
     * backward propagation.
     *
     * @param type {@link PartialType}
     * @return partial spectrum file
     */
	public DSMOutput toSpectrum(PartialType type) {
		SPCFile spcFileName = bp.getSpcFileName();
		double tlen = bp.tlen();
		int np = bp.np();
		int nbody = bp.nbody();
		double omegai = bp.omegai();
		HorizontalPosition observerPosition = bp.getSourceLocation();
		String observerID = bp.getSourceID(); //TODO check it
//		String observerName = bp.getObserverID();
		String observerNetwork = bp.getObserverNetwork();
		Location sourceLocation = fp.getSourceLocation();
		String sourceID = fp.getSourceID();
		double[] bodyR = bp.getBodyR();
		List<SPCBody> spcBodyList = new ArrayList<>(nbody);
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
			SPCBody body = new SPCBody(3, np);
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
			public SPCType getSpcFileType() {
				return type.toSpcFileType();
			}

			@Override
			public List<SPCBody> getSpcBodyList() {
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
			public String getObserverID() {
				return observerID;
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
			public SPCFile getSpcFileName() {
				return spcFileName;
			}
			
			@Override
			public void setSpcBody(int i, SPCBody body) {
//				spcBody.set(i, body); TODO
			}
		};
	}

    /**
     * ibody番目のボディ（深さ）に対する摂動の Partial derivatives のiに対する成分 ETAri,s の i
     *
     * @param component {@link SACComponent}
     * @param iBody     index for SacBody
     * @param type      {@link PartialType}
     * @return Ui(t) u[t] 時間領域
     * @author Kensuke Konishi
     * @author anselme return array of zero for partials whose radius is too close to the BP or FP source
     */
	public double[] createPartial(SACComponent component, int iBody, PartialType type) {
		// return array of zero for partials whose radius is too close to the BP or FP source
		double bpR = bp.getBodyR()[iBody];
		double fpR = fp.getBodyR()[iBody];
		if (fpR != bpR)
			throw new RuntimeException("Unexpected: fp and bp rBody differ " + fpR + " " + bpR);
		
		long t1i = System.currentTimeMillis();
		Complex[] partial_frequency = type == PartialType.Q ? computeQpartial(component, iBody)
				: computeTensorCulculus(component, iBody, iBody, type);
		long t1f = System.currentTimeMillis();
		System.out.println("Tensor multiplication finished in " + (t1f - t1i)*1e-3 + " s");
		
		if (null != sourceTimeFunction)
			partial_frequency = sourceTimeFunction.convolve(partial_frequency);
		//test tapper
//		partial_frequency = rightTapper(partial_frequency); //TODO
		long t2i = System.currentTimeMillis();
		Complex[] partial_time = toTimedomain(partial_frequency);
		double[] partialdouble = new double[npts];
		for (int j = 0; j < npts; j++)
			partialdouble[j] = partial_time[j].getReal();
		long t2f = System.currentTimeMillis();
		System.out.println("iFFT finished in " + (t2f - t2i)*1e-3 + " s");
		return partialdouble;
	}
	
	/**
	 * return array of zero for partials whose radius is too close to the BP or FP source
	 * @param component
	 * @param iBody
	 * @param type
	 * @return
	 * @author anselme
	 */
	public double[] createPartialSerial(SACComponent component, int iBody, PartialType type) {
		// return array of zero for partials whose radius is too close to the BP or FP source
		double bpR = bp.getBodyR()[iBody];
		double fpR = fp.getBodyR()[iBody];
		if (fpR != bpR)
			throw new RuntimeException("Unexpected: fp and bp rBody differ " + fpR + " " + bpR);
		
		long t1i = System.currentTimeMillis();
		Complex[] partial_frequency = type == PartialType.Q ? computeQpartial(component, iBody)
				: computeTensorCulculusSerial(component, iBody, iBody, type);
		long t1f = System.currentTimeMillis();
//		System.out.println("Tensor multiplication finished in " + (t1f - t1i)*1e-3 + " s");
		
		if (null != sourceTimeFunction)
			partial_frequency = sourceTimeFunction.convolveSerial(partial_frequency);
		
		//test tapper
		partial_frequency = rightTapper(partial_frequency); //TODO
		
		long t2i = System.currentTimeMillis();
		Complex[] partial_time = toTimedomain(partial_frequency);
		double[] partialdouble = new double[npts];
		for (int j = 0; j < npts; j++)
			partialdouble[j] = partial_time[j].getReal();
		long t2f = System.currentTimeMillis();
//		System.out.println("iFFt finished in " + (t2f - t2i)*1e-3 + " s");
//		Arrays.stream(partial_time).mapToDouble(Complex::abs).toArray();
		return partialdouble;
	}
	
	/**
	 * return array of zero for partials whose radius is too close to the BP or FP source
	 * @param component
	 * @param iBody
	 * @param type
	 * @return
	 * @author anselme
	 */
	public Complex[] createPartialFrequencySerial(SACComponent component, int iBody, PartialType type) {
		double bpR = bp.getBodyR()[iBody];
		double fpR = fp.getBodyR()[iBody];
		if (fpR != bpR)
			throw new RuntimeException("Unexpected: fp and bp rBody differ " + fpR + " " + bpR);
		
		long t1i = System.currentTimeMillis();
		Complex[] partial_frequency = type == PartialType.Q ? computeQpartial(component, iBody)
				: computeTensorCulculusSerial(component, iBody, iBody, type);
		long t1f = System.currentTimeMillis();
//		System.out.println("Tensor multiplication finished in " + (t1f - t1i)*1e-3 + " s");
		
		if (null != sourceTimeFunction)
			partial_frequency = sourceTimeFunction.convolveSerial(partial_frequency);
		
		//test tapper
		partial_frequency = rightTapper(partial_frequency); //TODO
		
		return partial_frequency;
	}

    /**
     * The structure is used for computation Q
     *
     * @param structure {@link PolynomialStructure}
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

    /**
     * compute tensor culculus of u Cijkl eta
     *
     * @param component {@link SACComponent}
     * @param iBody     index for sacbody
     * @param type      {@link PartialType}
     * @return uCe
     */
	private Complex[] computeTensorCulculus(SACComponent component, int iBody, PartialType type) {
		TensorCalculationUCE tensorcalc = new TensorCalculationUCE(fp.getSpcBodyList().get(iBody),
				bp.getSpcBodyList().get(iBody), type.getWeightingFactor(), angleForTensor);
		return component == SACComponent.Z ? tensorcalc.calc(0)
				: rotatePartial(tensorcalc.calc(1), tensorcalc.calc(2), component);
	}
	
	/**
	 * Used for BP/FP catalog
	 * @param component
	 * @param iBodyBp
	 * @param iBodyFp
	 * @param type
	 * @return
	 * @author anselme
	 */
	private Complex[] computeTensorCulculus(SACComponent component, int iBodyBp, int iBodyFp, PartialType type) {
		SPCBody bpBody = null;
		SPCBody fpBody = null;
		if (bp2 == null) {
			bpBody = bp.getSpcBodyList().get(iBodyBp);
			fpBody = fp.getSpcBodyList().get(iBodyFp);
//			System.err.println("No interpolation performed");
//			System.out.println("DEBUG BP noInterp: " +  bpBody.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
		}
		else if (fp2 == null) {
			bpBody = SPCBody.interpolate(bp.getSpcBodyList().get(iBodyBp)
					, bp2.getSpcBodyList().get(iBodyBp), bp3.getSpcBodyList().get(iBodyBp), dh);
			fpBody = fp.getSpcBodyList().get(iBodyFp);
//			System.out.println("DEBUG BP: " +  bpBody.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
		}
		else {
			bpBody = SPCBody.interpolate(bp.getSpcBodyList().get(iBodyBp)
					, bp2.getSpcBodyList().get(iBodyBp), bp3.getSpcBodyList().get(iBodyBp), dh);
			fpBody = SPCBody.interpolate(fp.getSpcBodyList().get(iBodyFp)
					, fp2.getSpcBodyList().get(iBodyFp), fp3.getSpcBodyList().get(iBodyFp), dhFP);
//			System.out.println("DEBUG BP: " +  bpBody.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
//			System.out.println("DEBUG FP: " +  fpBody.getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		}
//		System.out.println(fp.getSpcBodyList().get(0).getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		TensorCalculationUCE tensorcalc = new TensorCalculationUCE(fpBody,
				bpBody, type.getWeightingFactor(), angleForTensor);
		return component == SACComponent.Z ? tensorcalc.calc(0)
				: rotatePartial(tensorcalc.calc(1), tensorcalc.calc(2), component);
	}
	
	/**
	 * @param component
	 * @param iBodyBp
	 * @param iBodyFp
	 * @param type
	 * @return
	 * @author anselme
	 */
	private Complex[] computeTensorCulculusSerial(SACComponent component, int iBodyBp, int iBodyFp, PartialType type) {
		SPCBody bpBody = null;
		SPCBody fpBody = null;
		if (bp2 == null) {
			bpBody = bp.getSpcBodyList().get(iBodyBp);
			fpBody = fp.getSpcBodyList().get(iBodyFp);
//			System.err.println("No interpolation performed");
//			System.out.println("DEBUG BP noInterp: " +  bpBody.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
		}
		else if (fp2 == null) {
			bpBody = SPCBody.interpolate(bp.getSpcBodyList().get(iBodyBp)
					, bp2.getSpcBodyList().get(iBodyBp), bp3.getSpcBodyList().get(iBodyBp), dh);
			fpBody = fp.getSpcBodyList().get(iBodyFp);
//			System.out.println("DEBUG BP: " +  bpBody.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
		}
		else {
			bpBody = SPCBody.interpolate(bp.getSpcBodyList().get(iBodyBp)
					, bp2.getSpcBodyList().get(iBodyBp), bp3.getSpcBodyList().get(iBodyBp), dh);
			fpBody = SPCBody.interpolate(fp.getSpcBodyList().get(iBodyFp)
					, fp2.getSpcBodyList().get(iBodyFp), fp3.getSpcBodyList().get(iBodyFp), dhFP);
//			System.out.println("DEBUG BP: " +  bpBody.getSpcComponents()[20].getValueInFrequencyDomain()[10]);
//			System.out.println("DEBUG FP: " +  fpBody.getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		}
//		System.out.println(fp.getSpcBodyList().get(0).getSpcComponents()[8].getValueInFrequencyDomain()[10]);
		TensorCalculationUCE tensorcalc = new TensorCalculationUCE(fpBody,
				bpBody, type.getWeightingFactor(), angleForTensor);
		return component == SACComponent.Z ? tensorcalc.calcSerial(0)
				: rotatePartial(tensorcalc.calcSerial(1), tensorcalc.calcSerial(2), component);
	}
	
	/**
	 * @param bpBody1
	 * @param bpBody2
	 * @param unitDistance
	 * @return
	 * @author anselme
	 */
	@Deprecated
	private SPCBody interpolate(SPCBody bpBody1, SPCBody bpBody2, double unitDistance) {
		return bpBody1.interpolate(bpBody2, unitDistance);
	}

	public void setSourceTimeFunction(SourceTimeFunction sourceTimeFunction) {
		this.sourceTimeFunction = sourceTimeFunction;
	}

    /**
     * 周波数領域のデータにしか使えない
     *
     * @param partial1  partial in local cartesian
     * @param partial2  partial in local cartesian
     * @param component R, T 震源 観測点の乗る大円上
     * @return 回転させてできたi成分の偏微分波形
     */
	private Complex[] rotatePartial(Complex[] partial1, Complex[] partial2, SACComponent component) {
		Complex[] partial = new Complex[fp.np() + 1];
		
		double cosine = FastMath.cos(angleForVector);
		double sine = FastMath.sin(angleForVector);
		
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
     * @param uTime time series
     */
	private void correctAmplitude(Complex[] uTime) {
		final double tmp = npts * 1e3 / bp.tlen();
		for (int i = 0; i < npts; i++)
			uTime[i] = uTime[i].multiply(tmp);
	}

	/**
	 * compute waveform in time domain from spector in frequency domain. amplitude
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
     * @param complex waveform in frequency domain
     * @return 時間領域の複素数列
     */
	private Complex[] inverseFourierTransform(Complex[] complex) {
		// must be tested
//		System.out.println(lsmooth);
		int nnp = fp.np() * lsmooth;
		FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

		Complex[] data = new Complex[nnp * 2];
		
		//test tapper
//		Complex[] tmp = rightTapper(complex);
//		System.arraycopy(tmp, 0, data, 0, fp.np() + 1);
		
		// pack to temporary Complex array
		System.arraycopy(complex, 0, data, 0, fp.np() + 1);

		// set blank due to lsmooth
		Arrays.fill(data, fp.np() + 1, nnp + 1, Complex.ZERO);

		// set values for imaginary frequency
		for (int i = 0; i < nnp - 1; i++)
			data[nnp + i + 1] = data[nnp - i - 1].conjugate();
//		
//		for (int i = 0; i < nnp; i++)
//			data[nnp + i] = data[nnp - i - 1].conjugate();
//
		
		// fast fourier transformation
		return fft.transform(data, TransformType.INVERSE); // check

	}
	
	/**
	 * @param complex
	 * @return
	 * @author anselme
	 */
	private Complex[] rightTapper(Complex[] complex) {
		Complex[] tappered = complex.clone();
		int l = complex.length;
		int n = l / 5;
		
		for (int i = 0; i < n; i++) {
//			tappered[i + l - n] = tappered[i + l - n].multiply(FastMath.cos(Math.PI / (2 * (n - 1)) * i));
			tappered[i + l - n] = tappered[i + l - n].multiply(1. - (double) i / (n - 1.));
		}
		
		return tappered;
	}

	/**
	 * frequency domain をsamplingFrequencyでtime-domain tlen(s)にもってくるスムージング値を探す
	 * 
	 */
	private void findLsmooth() {
		int np = Integer.highestOneBit(fp.np());
		if (np < fp.np()) np *= 2;

		lsmooth = (int) (0.5 * bp.tlen() * samplingFrequency / np);
		int i = Integer.highestOneBit(lsmooth);
		if (i < lsmooth) i *= 2;
		lsmooth = i;
		npts = np * lsmooth * 2;
	}

}
