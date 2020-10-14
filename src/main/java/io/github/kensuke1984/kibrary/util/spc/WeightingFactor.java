package io.github.kensuke1984.kibrary.util.spc;

import io.github.kensuke1984.kibrary.elasticparameter.ElasticModuli;

/**
 * Weighting factor in Geller &amp; Hara (1993) to compute 3-D partial
 * derivatives
 *
 * @author Kensuke Konishi
 * @version 0.1.0.1
 */
interface WeightingFactor {

    WeightingFactor A = new WeightingFactor() {
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
    WeightingFactor C = new WeightingFactor() {
        /**
         * Coefficient for C Geller and Hara (1993)
         */
        @Override
        public double getFactor(int i, int j, int k, int l) {
            switch (ElasticModuli.getElasticModulus(i, j, k, l).getTI()) {
                case C:
                    return 1;
                default:
                    return 0;
            }
        }
    };
    WeightingFactor F = new WeightingFactor() {
        /**
         * Coefficient for F. Geller and Hara (1993)
         */
        @Override
        public double getFactor(int i, int j, int k, int l) {
            switch (ElasticModuli.getElasticModulus(i, j, k, l).getTI()) {
                case F:
                    return 1;
                default:
                    return 0;
            }
        }
    };
    WeightingFactor L = new WeightingFactor() {
        /**
         * Coefficient for L. Geller and Hara (1993)
         */
        @Override
        public double getFactor(int i, int j, int k, int l) {
            switch (ElasticModuli.getElasticModulus(i, j, k, l).getTI()) {
                case L:
                    return 1;
                default:
                    return 0;
            }
        }

    };
    WeightingFactor N = new WeightingFactor() {
        /**
         * Coefficient for N. Geller and Hara (1993)
         */
        @Override
        public double getFactor(int i, int j, int k, int l) {
            switch (ElasticModuli.getElasticModulus(i, j, k, l).getTI()) {
                case A_2N:
                    return -2;
                case N:
                    return 1;
                default:
                    return 0;
            }
        }
    };
    WeightingFactor MU = new WeightingFactor() {
        /**
         * Coefficint for &mu;
         */
        @Override
        public double getFactor(int i, int j, int k, int l) {
            switch (ElasticModuli.getElasticModulus(i, j, k, l).getISO()) {
                case MU:
                    return 1;
                case LAMBDAplus2MU:
                    return 2;
                default:
                    return 0;
            }
        }
    };
    WeightingFactor LAMBDA = new WeightingFactor() {
        /**
         * λの係数 i, j, k, l ={0, 1, 2}
         */
        @Override
        public double getFactor(int i, int j, int k, int l) {
            switch (ElasticModuli.getElasticModulus(i, j, k, l).getISO()) {
                case LAMBDA:
                case LAMBDAplus2MU:
                    return 1;
                default:
                    return 0;
            }
        }

    };
    
    /**
     * @author anselme
     */
    static final WeightingFactor KAPPA = new WeightingFactor() {
		/**
		 * λの係数 i, j, k, l ={0, 1, 2}
		 */
		@Override
		public double getFactor(int i, int j, int k, int l) {
			switch (ElasticModuli.getElasticModulus(i, j, k, l).getISO()) {
			case LAMBDA:
				return 1.;
			case LAMBDAplus2MU:
				return 4.;
			case MU:
				return 1.5;
			default:
				return 0;
			}
		}
	};
	
	/**
	 * @author anselme
	 */
	static final WeightingFactor LAMBDA2MU = new WeightingFactor() {
		/**
		 * λの係数 i, j, k, l ={0, 1, 2}
		 */
		@Override
		public double getFactor(int i, int j, int k, int l) {
			switch (ElasticModuli.getElasticModulus(i, j, k, l).getISO()) {
			case LAMBDA:
				return 1.;
			case LAMBDAplus2MU:
				return 2.;
			case MU:
				return .5;
			default:
				return 0;
			}
		}
	};

    /**
     * デカルト座標
     *
     * @param i (0, 1, 2) &rarr; (1, 2, 3)
     * @param j (0, 1, 2) &rarr; (1, 2, 3)
     * @param k (0, 1, 2) &rarr; (1, 2, 3)
     * @param l (0, 1, 2) &rarr; (1, 2, 3)
     * @return Cijkl に入っている重み λ+２μ のところで λなら１ μなら２
     */
    double getFactor(int i, int j, int k, int l);

}
