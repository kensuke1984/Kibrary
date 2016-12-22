package io.github.kensuke1984.kibrary.util.spc;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.math3.complex.Complex;

import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * the object means a body in {@link SpectrumFile} the body means the spcdata of
 * each perturbation point...
 * 
 * スペクトルファイル（spcsac） の中のあるボディ
 * 
 * ista に対応する
 * 
 * 
 * @version 0.1.2.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class SpcBody {

	private final int nComponent; // datarealsize
	private final int np;

	private int nptsInTimeDomain;

	private SpcComponent[] spcComponents;

	/**
	 * 
	 * @return DEEP copy of this
	 */
	SpcBody copy() {
		SpcBody s = new SpcBody(nComponent, np);
		s.nptsInTimeDomain = nptsInTimeDomain;
		s.spcComponents = new SpcComponent[nComponent];
		Arrays.setAll(s.spcComponents, i -> spcComponents[i].copy());
		return s;
	}

	/**
	 * @param nComponent
	 *            the number of components
	 * @param np
	 *            the number of steps in frequency domain
	 */
	SpcBody(int nComponent, int np) {
		this.nComponent = nComponent;
		this.np = np;
		allocateComponents();
	}

	/**
	 * ω：ip 番目データを読む
	 * 
	 * @param ip
	 *            step number in frequency domain
	 * @param u
	 *            u[i] ith component
	 */
	void add(int ip, Complex... u) {
		if (u.length != nComponent)
			throw new RuntimeException("The number of components is wrong");
		for (int i = 0; i < nComponent; i++)
			spcComponents[i].set(ip, u[i]);
	}

	/**
	 * 
	 * @return SpcComponent[] all the {@link SpcComponent} in this
	 */
	public SpcComponent[] getSpcComponents() {
		return spcComponents;
	}

	/**
	 * 引数で指定されたテンソル成分に対するコンポーネントを返す
	 * 
	 * @param tensor
	 *            SpcTensorComponent
	 * @return SpcComponent for the tensor
	 */
	public SpcComponent getSpcComponent(SpcTensorComponent tensor) {
		return spcComponents[tensor.valueOf() - 1];
	}

	public SpcComponent getSpcComponent(SACComponent sacComponent) {
		return spcComponents[sacComponent.valueOf() - 1];
	}

	/**
	 * 
	 * TODO 別を出すようにする anotherBodyを足し合わせる
	 * 
	 * @param anotherBody
	 *            {@link SpcBody} for addition
	 */
	public void addBody(SpcBody anotherBody) {
		if (np != anotherBody.getNp())
			throw new RuntimeException("Error: Size of body is not equal!");
		else if (nComponent != anotherBody.getNumberOfComponent())
			throw new RuntimeException("Error: The numbers of each component are different.");

		for (int j = 0; j < nComponent; j++)
			spcComponents[j].addComponent(anotherBody.spcComponents[j]);

	}

	private void allocateComponents() {
		spcComponents = IntStream.range(0, nComponent).mapToObj(i -> new SpcComponent(np))
				.toArray(SpcComponent[]::new);
	}

	/**
	 * after toTimeDomain
	 * 
	 * @param tlen
	 *            time length
	 */
	public void amplitudeCorrection(double tlen) {
		Arrays.stream(spcComponents).forEach(component -> component.amplitudeCorrection(tlen));
	}

	/**
	 * after toTime
	 * 
	 * @param omegai
	 *            &omega;<sub>i</sub>
	 * @param tlen
	 *            time length
	 */
	public void applyGrowingExponential(double omegai, double tlen) {
		Arrays.stream(spcComponents).forEach(component -> component.applyGrowingExponential(omegai, tlen));
	}

	/**
	 * before toTime This method applies ramped source time function.
	 * 
	 * @param sourceTimeFunction
	 *            will be applied on all components.
	 */
	public void applySourceTimeFunction(SourceTimeFunction sourceTimeFunction) {
		Arrays.stream(spcComponents).forEach(component -> component.applySourceTimeFunction(sourceTimeFunction));
	}

	/**
	 * すべてのコンポーネントに対し時間微分する。 before toTime
	 * 
	 * @param tlen
	 *            time length
	 */
	public void differentiate(double tlen) {
		Arrays.stream(spcComponents).forEach(component -> component.differentiate(tlen));
	}

	public int getNumberOfComponent() {
		return nComponent;
	}

	public int getNp() {
		return np;
	}

	public int getNPTSinTimeDomain() {
		return nptsInTimeDomain;
	}

	/**
	 * @param component
	 *            {@link SACComponent}
	 * @return the data of i th component time_domain
	 */
	public double[] getTimeseries(SACComponent component) {
		return spcComponents[component.valueOf() - 1].getTimeseries();
	}

	/**
	 * すべてのコンポーネントを時間領域へ
	 * 
	 * @param lsmooth
	 *            lsmooth
	 */
	public void toTimeDomain(int lsmooth) {
		Arrays.stream(spcComponents).forEach(component -> component.toTimeDomain(lsmooth));
	}

}
