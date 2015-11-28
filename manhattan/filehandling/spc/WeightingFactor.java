package filehandling.spc;

import manhattan.elasticparameter.ElasticModuli;

/**
 * Weighting factor in Geller &amp; Hara (1993) to compute 3-D partial
 * derivatives
 * 
 * @author kensuke
 * 
 * @version 0.1.0  
 * 
 */
interface WeightingFactor {

	/**
	 * デカルト座標
	 * 
	 * @param i
	 *            (0, 1, 2) &rarr; (1, 2, 3)
	 * @param j
	 *            (0, 1, 2) &rarr; (1, 2, 3)
	 * @param k
	 *            (0, 1, 2) &rarr; (1, 2, 3)
	 * @param l
	 *            (0, 1, 2) &rarr; (1, 2, 3)
	 * @return Cijkl に入っている重み λ+２μ のところで λなら１ μなら２
	 */
	double getFactor(int i, int j, int k, int l);

	static final WeightingFactor A = new WeightingFactor() {
		/**
		 * Coefficient for A. Geller and Hara (1993) Aの係数
		 */
		@Override
		public double getFactor(int i, int j, int k, int l) {
			switch (ElasticModuli.getElasticModulus(i, j, k, l).getTI()) {
			case A:
			case A_2N:
				return 1.0;
			default:
				return 0.0;
			}
		}
	};

	static final WeightingFactor C = new WeightingFactor() {
		/**
		 * Coefficient for C Geller and Hara (1993)
		 */
		@Override
		public double getFactor(int i, int j, int k, int l) {
			switch (ElasticModuli.getElasticModulus(i, j, k, l).getTI()) {
			case C:
				return 1.0;
			default:
				return 0.0;
			}
		}
	};

	static final WeightingFactor F = new WeightingFactor() {
		/**
		 * Coefficient for F. Geller and Hara (1993)
		 */
		@Override
		public double getFactor(int i, int j, int k, int l) {
			switch (ElasticModuli.getElasticModulus(i, j, k, l).getTI()) {
			case F:
				return 1.0;
			default:
				return 0.0;
			}
		};
	};
	static final WeightingFactor L = new WeightingFactor() {
		/**
		 * Coefficient for L. Geller and Hara (1993)
		 */
		@Override
		public double getFactor(int i, int j, int k, int l) {
			switch (ElasticModuli.getElasticModulus(i, j, k, l).getTI()) {
			case L:
				return 1.0;
			default:
				return 0.0;
			}
		}

	};
	static final WeightingFactor N = new WeightingFactor() {
		/**
		 * Coefficient for N. Geller and Hara (1993)
		 */
		@Override
		public double getFactor(int i, int j, int k, int l) {
			switch (ElasticModuli.getElasticModulus(i, j, k, l).getTI()) {
			case A_2N:
				return -2.0;
			case N:
				return 1.0;
			default:
				return 0.0;
			}
		}
	};
	static final WeightingFactor MU = new WeightingFactor() {
		/**
		 * Coefficint for &mu;
		 */
		@Override
		public double getFactor(int i, int j, int k, int l) {
			switch (ElasticModuli.getElasticModulus(i, j, k, l).getISO()) {
			case MU:
				return 1.0;
			case LAMBDAplus2MU:
				return 2.0;
			default:
				return 0.0;
			}
		}
	};
	static final WeightingFactor LAMBDA = new WeightingFactor() {
		/**
		 * λの係数 i, j, k, l ={0, 1, 2}
		 */
		@Override
		public double getFactor(int i, int j, int k, int l) {
			switch (ElasticModuli.getElasticModulus(i, j, k, l).getISO()) {
			case LAMBDA:
			case LAMBDAplus2MU:
				return 1.0;
			default:
				return 0.0;
			}
		}

	};

}
