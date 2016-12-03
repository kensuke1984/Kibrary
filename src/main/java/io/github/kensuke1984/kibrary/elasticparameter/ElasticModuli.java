package io.github.kensuke1984.kibrary.elasticparameter;

/**
 * 偏微分係数波形を計算するためのCijklを作る
 * 
 * @author Kensuke Konishi
 * @version 0.0.2
 *
 */
public class ElasticModuli {

	private ElasticModuli() {
	}

	/**
	 * Cijkl ((i, j, k, l) = 0,1,2)
	 */
	protected final static ElasticModulus[][][][] c;

	static {
		c = new ElasticModulus[3][3][3][3];
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				for (int k = 0; k < 3; k++)
					for (int l = 0; l < 3; l++)
						c[i][j][k][l] = new ElasticModulus(i + 1, j + 1, k + 1,
								l + 1);
	}

	/**
	 * @param i
	 *            0, 1, 2 &rarr; 1, 2, 3
	 * @param j
	 *            0, 1, 2 &rarr; 1, 2, 3
	 * @param k
	 *            0, 1, 2 &rarr; 1, 2, 3
	 * @param l
	 *            0, 1, 2 &rarr; 1, 2, 3
	 * @return C<sub>ijkl</sub>
	 */
	public static final ElasticModulus getElasticModulus(int i, int j, int k, int l) {
		return c[i][j][k][l];
	}

}
