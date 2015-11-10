package manhattan.elasticparameter;

public enum TIModulusEnum {
	A, C, F, L, N, A_2N, ZERO;

	public static TIModulusEnum getTI(ElasticMN mn) {
		int m;
		int n;
		if (mn.getN() < mn.getM()) {
			m = mn.getN();
			n = mn.getM();
		} else {
			m = mn.getM();
			n = mn.getN();
		}
		// System.out.println(m+"  "+n);
		switch (m) {
		case 1:
			switch (n) {
			case 1:
				return TIModulusEnum.A;
			case 2:
				return TIModulusEnum.A_2N;
			case 3:
				return TIModulusEnum.F;
			case 4:
				return TIModulusEnum.ZERO;
			case 5:
				return TIModulusEnum.ZERO;
			case 6:
				return TIModulusEnum.ZERO;
			}
		case 2:
			switch (n) {
			case 2:
				return TIModulusEnum.A;
			case 3:
				return TIModulusEnum.F;
			case 4:
				return TIModulusEnum.ZERO;
			case 5:
				return TIModulusEnum.ZERO;
			case 6:
				return TIModulusEnum.ZERO;
			}
		case 3:
			switch (n) {
			case 3:
				return TIModulusEnum.C;
			case 4:
				return TIModulusEnum.ZERO;
			case 5:
				return TIModulusEnum.ZERO;
			case 6:
				return TIModulusEnum.ZERO;
			}
		case 4:
			switch (n) {
			case 4:
				return TIModulusEnum.L;
			case 5:
				return TIModulusEnum.ZERO;
			case 6:
				return TIModulusEnum.ZERO;
			}
		case 5:
			switch (n) {
			case 5:
				return TIModulusEnum.L;
			case 6:
				return TIModulusEnum.ZERO;
			}
		case 6:
			switch (n) {
			case 6:
				return TIModulusEnum.N;
			}
		}
		throw new RuntimeException("Invalid input");
	}

}
