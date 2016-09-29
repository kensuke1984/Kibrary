package io.github.kensuke1984.kibrary.elasticparameter;

/**
 * 
 * Elastic modulus C<sub>ijkl</sub>
 * 
 * ijklのセットは何に属するか ijkl &rarr; 1, 2, 3
 * 
 * @author Kensuke Konishi
 * 
 * 
 * @version 0.0.2.1
 * 
 */
public class ElasticModulus {

	/**
	 * i=(1,2,3)
	 */
	private final int i;
	/**
	 * j=(1,2,3)
	 */
	private final int j;
	/**
	 * k=(1,2,3)
	 */
	private final int k;
	/**
	 * l=(1,2,3)
	 */
	private final int l;

	private ElasticIJKL ijkl;
	private ElasticMN mn;

	private double value;

	private TIModulusEnum ti;
	private IsotropicModulusEnum iso;

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}

	public int getK() {
		return k;
	}

	public int getL() {
		return l;
	}

	public ElasticIJKL getIJKL() {
		return ijkl;
	}

	/**
	 * @return Cmn
	 */
	public ElasticMN getMN() {
		return mn;
	}

	/**
	 * check if n is valid for ijkl.
	 * 
	 * @param n
	 *            must be 1,2 or 3
	 * @return boolean
	 */
	private static boolean checkComponents(int n) {
		return 1 <= n && n <= 3;
	}

	/**
	 * C<sub>ijkl</sub> constructor
	 * 
	 * @param i
	 *            (1, 2, 3)
	 * @param j
	 *            (1, 2, 3)
	 * @param k
	 *            (1, 2, 3)
	 * @param l
	 *            (1, 2, 3)
	 */
	ElasticModulus(int i, int j, int k, int l) {
		if (checkComponents(i) && checkComponents(j) && checkComponents(k) && checkComponents(l)) {
			this.i = i;
			this.j = j;
			this.k = k;
			this.l = l;
			setIJKL();
			mn = ElasticMN.getElasticMN(ijkl);
			ti = TIModulusEnum.getTI(mn);
			iso = IsotropicModulusEnum.getIsotropic(mn);
		} else {
			throw new IllegalArgumentException(
					"Input (i, j, k, l) :" + i + ", " + j + ", " + k + ", " + l + " are invalid.");
		}
	}

	public TIModulusEnum getTI() {
		return ti;
	}

	public IsotropicModulusEnum getISO() {
		return iso;
	}

	private void setIJKL() {
		ijkl = ElasticIJKL.valueOf(i, j, k, l);
	}

}
