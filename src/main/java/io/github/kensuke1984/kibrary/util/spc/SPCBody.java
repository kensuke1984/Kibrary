package io.github.kensuke1984.kibrary.util.spc;

import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunction;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import org.apache.commons.math3.complex.Complex;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * the object means a body in {@link Spectrum} the body means the spcdata of
 * each perturbation point...
 * <p>
 * スペクトルファイル（spcsac） の中のあるボディ
 * <p>
 * ista に対応する
 *
 * @author Kensuke Konishi
 * @version 0.1.2.2
 */
public class SPCBody {

    private final int N_COMPONENT; // datarealsize
    private final int NP;

    private int nptsInTimeDomain;

    private SPCComponent[] spcComponents;

    /**
     * @param nComponent the number of components
     * @param np         the number of steps in frequency domain
     */
    SPCBody(int nComponent, int np) {
        N_COMPONENT = nComponent;
        NP = np;
        allocateComponents();
    }

    /**
     * @return DEEP copy of this
     */
    SPCBody copy() {
        SPCBody s = new SPCBody(N_COMPONENT, NP);
        s.nptsInTimeDomain = nptsInTimeDomain;
        s.spcComponents = new SPCComponent[N_COMPONENT];
        Arrays.setAll(s.spcComponents, i -> spcComponents[i].copy());
        return s;
    }

    /**
     * ω：ip 番目データを読む
     *
     * @param ip step number in frequency domain
     * @param u  u[i] ith component
     */
    void add(int ip, Complex... u) {
        if (u.length != N_COMPONENT) throw new RuntimeException("The number of components is wrong");
        for (int i = 0; i < N_COMPONENT; i++)
            spcComponents[i].set(ip, u[i]);
    }
    
    /**
     * Interpolation for catalog
     * @param anotherBody
     * @param unitDistance
     * @return
     * @author anselme
     */
    public SPCBody interpolate(SPCBody anotherBody, double unitDistance) {
		if (unitDistance < 0 || unitDistance > 1) 
			throw new RuntimeException("Error: unit distance should be between 0-1 " + unitDistance);
		SPCBody s = this.copy();
		if (N_COMPONENT != anotherBody.getNp())
			throw new RuntimeException("Error: Size of body is not equal!");
		else if (N_COMPONENT != anotherBody.getNumberOfComponent())
			throw new RuntimeException("Error: The numbers of each component are different.");

		for (int j = 0; j < N_COMPONENT; j++) {
			SPCComponent comp1 = s.spcComponents[j];
			SPCComponent comp2 = anotherBody.spcComponents[j];
			comp1.mapMultiply(1. - unitDistance);
			comp2.mapMultiply(unitDistance);
			comp1.addComponent(comp2);
			s.spcComponents[j] = comp1;
		}
		
		return s;
	}
	
	/**
	 * @param body1
	 * @param body2
	 * @param body3
	 * @param dh
	 * @return
	 * @author anselme
	 */
	public static SPCBody interpolate(SPCBody body1, SPCBody body2, SPCBody body3, double[] dh) {
		SPCBody s = body1.copy();
//		double c1 = 1 - dh[0] + dh[0]*dh[1]/2.;
//		double c2 = dh[0] - dh[0]*dh[1];
//		double c3 = dh[0]*dh[1]/2.;
		double c1 = dh[1]*dh[2] / 2.;
		double c2 = -dh[0]*dh[2];
		double c3 = dh[0]*dh[1] / 2.;
		
		for (int j = 0; j < body1.N_COMPONENT; j++) {
			SPCComponent comp1 = body1.spcComponents[j].copy();
			SPCComponent comp2 = body2.spcComponents[j].copy();
			SPCComponent comp3 = body3.spcComponents[j].copy();
			
			comp1.mapMultiply(c1);
			comp2.mapMultiply(c2);
			comp3.mapMultiply(c3);
			comp1.addComponent(comp2);
			comp1.addComponent(comp3);
			
			s.spcComponents[j] = comp1;
		}
		
		return s;
	}
	
	/**
	 * @param body1
	 * @param body2
	 * @param body3
	 * @param dh
	 * @return
	 * @author anselme
	 */
	public static SPCBody interpolate_backward(SPCBody body1, SPCBody body2, SPCBody body3, double[] dh) {
		SPCBody s = body1.copy();
//		double c1 = 1 - dh[0] + dh[0]*dh[1]/2.;
//		double c2 = dh[0] - dh[0]*dh[1];
//		double c3 = dh[0]*dh[1]/2.;
		double c1 = -dh[1]*dh[2];
		double c2 = dh[0]*dh[2] / 2.;
		double c3 = dh[0]*dh[1] / 2.;
		
		for (int j = 0; j < body1.N_COMPONENT; j++) {
			SPCComponent comp1 = body1.spcComponents[j];
			SPCComponent comp2 = body2.spcComponents[j];
			SPCComponent comp3 = body3.spcComponents[j];
			
			comp1.mapMultiply(c1);
			comp2.mapMultiply(c2);
			comp3.mapMultiply(c3);
			comp1.addComponent(comp2);
			comp1.addComponent(comp3);
			
			s.spcComponents[j] = comp1;
		}
		
		return s;
	}
	
	/**
	 * frequency domain をsamplingFrequencyでtime-domain tlen(s)にもってくるスムージング値を探す
	 * 
	 */
	public int findLsmooth(double tlen, double samplingFrequency) {
		int tmpNp = Integer.highestOneBit(NP);
		if (tmpNp < NP)
			tmpNp *= 2;

		int lsmooth = (int) (0.5 * tlen * samplingFrequency / NP);
		int i = Integer.highestOneBit(lsmooth);
		if (i < lsmooth)
			i *= 2;
		lsmooth = i;
		
		return lsmooth;
	}

    /**
     * @return SPCComponent[] all the {@link SPCComponent} in this
     */
    public SPCComponent[] getSpcComponents() {
        return spcComponents;
    }

    /**
     * 引数で指定されたテンソル成分に対するコンポーネントを返す
     *
     * @param tensor SPCTensorComponent
     * @return SPCComponent for the tensor
     */
    public SPCComponent getSpcComponent(SPCTensorComponent tensor) {
        return spcComponents[tensor.valueOf() - 1];
    }

    public SPCComponent getSpcComponent(SACComponent sacComponent) {
        return spcComponents[sacComponent.valueOf() - 1];
    }

    /**
     * TODO 別を出すようにする anotherBodyを足し合わせる
     *
     * @param anotherBody {@link SPCBody} for addition
     */
    public void addBody(SPCBody anotherBody) {
        if (NP != anotherBody.getNp()) throw new RuntimeException("Error: Size of body is not equal!");
        else if (N_COMPONENT != anotherBody.getNumberOfComponent())
            throw new RuntimeException("Error: The numbers of each component are different.");

        for (int j = 0; j < N_COMPONENT; j++)
            spcComponents[j].addComponent(anotherBody.spcComponents[j]);

    }

    private void allocateComponents() {
        spcComponents =
                IntStream.range(0, N_COMPONENT).mapToObj(i -> new SPCComponent(NP)).toArray(SPCComponent[]::new);
    }

    /**
     * after toTimeDomain
     *
     * @param tlen time length
     */
    public void amplitudeCorrection(double tlen) {
        Arrays.stream(spcComponents).forEach(component -> component.amplitudeCorrection(tlen));
    }

    /**
     * after toTime
     *
     * @param omegaI &omega;<sub>i</sub>
     * @param tlen   time length
     */
    public void applyGrowingExponential(double omegaI, double tlen) {
        Arrays.stream(spcComponents).forEach(component -> component.applyGrowingExponential(omegaI, tlen));
    }

    /**
     * before toTime This method applies ramped source time function.
     *
     * @param sourceTimeFunction will be applied on all components.
     */
    public void applySourceTimeFunction(SourceTimeFunction sourceTimeFunction) {
        Arrays.stream(spcComponents).forEach(component -> component.applySourceTimeFunction(sourceTimeFunction));
    }

    /**
     * すべてのコンポーネントに対し時間微分する。 before toTime
     *
     * @param tlen time length
     */
    public void differentiate(double tlen) {
        Arrays.stream(spcComponents).forEach(component -> component.differentiate(tlen));
    }

    public int getNumberOfComponent() {
        return N_COMPONENT;
    }

    public int getNp() {
        return NP;
    }

    public int getNPTSinTimeDomain() {
        return nptsInTimeDomain;
    }

    /**
     * @param component {@link SACComponent}
     * @return the data of i th component time_domain
     */
    public double[] getTimeseries(SACComponent component) {
        return spcComponents[component.valueOf() - 1].getTimeseries();
    }

    /**
     * Converts all the components to time domain.
     *
     * @param lsmooth lsmooth
     */
    public void toTimeDomain(int lsmooth) {
        Arrays.stream(spcComponents).forEach(component -> component.toTimeDomain(lsmooth));
    }

}
