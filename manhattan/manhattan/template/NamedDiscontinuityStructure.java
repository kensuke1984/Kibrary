package manhattan.template;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * Within each layer Bullen law v(r) = Ar^B is used.
 * {@link #computeBullenLaw(int, double[])}
 * 
 * @version 0.0.2
 * @since 2014/5/20
 * 
 *        Xgbm Davis and Henson, 1993
 * 
 * @version 0.0.3
 * @since 2014/7/25 boundaries installed Bullen law A,B should be saved
 * 
 * @version 0.0.4
 * @since 2015/8/31
 * {@link Path} base
 * 
 * 
 * @author Kensuke
 *
 */
public class NamedDiscontinuityStructure {

	/**
	 * the number of boundaries
	 */
	private int nBoundary;

	/**
	 * values at boundaries
	 */
	private double[] rho;
	/**
	 * values of Bullen law
	 */
	private double[] rhoA;
	/**
	 * values of Bullen law
	 */
	private double[] rhoB;

	/**
	 * values at boundaries
	 */
	private double[] vs;
	/**
	 * values of Bullen law
	 */
	private double[] vsA;
	/**
	 * values of Bullen law
	 */
	private double[] vsB;

	/**
	 * values at boundaries
	 */
	private double[] vp;
	/**
	 * values of Bullen law
	 */
	private double[] vpA;
	/**
	 * values of Bullen law
	 */
	private double[] vpB;

	/**
	 * values at boundaries
	 */
	private double[] qmu;
	/**
	 * values of Bullen law
	 */
	private double[] qmuA;
	/**
	 * values of Bullen law
	 */
	private double[] qmuB;

	/**
	 * values at boundaries
	 */
	private double[] qkappa;

	/**
	 * values of Bullen law
	 */
	private double[] qkappaA;
	/**
	 * values of Bullen law
	 */
	private double[] qkappaB;

	/**
	 * radiuses of boundaries
	 */
	private double[] r;

	/**
	 * structure file
	 */
	private Path infPath;

	public static void main(String[] args) {
		NamedDiscontinuityStructure nd = NamedDiscontinuityStructure.prem();
		System.out.println(nd.getVs(3480 + 10e-9));
	}

	protected NamedDiscontinuityStructure() {
	}

	/**
	 * PREM
	 * 
	 * @return Isotropic PREM
	 */
	public static NamedDiscontinuityStructure prem() {
		NamedDiscontinuityStructure nd = new NamedDiscontinuityStructure();
		nd.coreMantleBoundary = 3480;
		nd.innerCoreBoundary = 1221.5;
		nd.mohoDiscontinuity = 6356;
		nd.nBoundary = 88;
		nd.r = new double[] { 0.0, 100.0, 200.0, 300.0, 400.0, 500.0, 600.0,
				700.0, 800.0, 900.0, 1000.0, 1100.0, 1200.0, 1221.5, 1221.5,
				1300.0, 1400.0, 1500.0, 1600.0, 1700.0, 1800.0, 1900.0, 2000.0,
				2100.0, 2200.0, 2300.0, 2400.0, 2500.0, 2600.0, 2700.0, 2800.0,
				2900.0, 3000.0, 3100.0, 3200.0, 3300.0, 3400.0, 3480.0, 3480.0,
				3500.0, 3600.0, 3630.0, 3700.0, 3800.0, 3900.0, 4000.0, 4100.0,
				4200.0, 4300.0, 4400.0, 4500.0, 4600.0, 4700.0, 4800.0, 4900.0,
				5000.0, 5100.0, 5200.0, 5300.0, 5400.0, 5500.0, 5600.0, 5650.0,
				5701.0, 5701.0, 5736.0, 5771.0, 5821.0, 5871.0, 5921.0, 5971.0,
				5971.0, 6016.0, 6061.0, 6106.0, 6151.0, 6151.0, 6186.0, 6221.0,
				6256.0, 6291.0, 6311.0, 6331.0, 6346.6, 6346.6, 6356.0, 6356.0,
				6371.0, };
		nd.rho = new double[] { 13.08848, 13.0863, 13.07977, 13.06888,
				13.05364, 13.03404, 13.01009, 12.98178, 12.94912, 12.91211,
				12.87073, 12.82501, 12.77493, 12.7636, 12.16634, 12.125,
				12.06924, 12.00989, 11.94682, 11.8799, 11.809, 11.73401,
				11.65478, 11.57119, 11.48311, 11.39042, 11.29298, 11.19067,
				11.08335, 10.97091, 10.85321, 10.73012, 10.60152, 10.46727,
				10.32726, 10.18134, 10.0294, 9.90349, 5.56645, 5.55641,
				5.50642, 5.49145, 5.45657, 5.40681, 5.35706, 5.30724, 5.25729,
				5.20713, 5.15669, 5.1059, 5.05469, 5.00299, 4.95073, 4.89783,
				4.84422, 4.78983, 4.7346, 4.67844, 4.62129, 4.56307, 4.50372,
				4.44317, 4.41241, 4.38071, 3.99214, 3.98399, 3.97584, 3.91282,
				3.8498, 3.78678, 3.72378, 3.54325, 3.51639, 3.48951, 3.46264,
				3.43578, 3.3595, 3.3633, 3.3671, 3.37091, 3.37471, 3.37688,
				3.37906, 3.38076, 2.9, 2.9, 2.6, 2.6, };
		nd.qmu = new double[] { 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 85.0,
				85.0, 85.0, 85.0, 85.0, 85.0, 85.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 312.0, 312.0, 312.0, 312.0,
				312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0,
				312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0, 312.0,
				312.0, 312.0, 312.0, 312.0, 143.0, 143.0, 143.0, 143.0, 143.0,
				143.0, 143.0, 143.0, 143.0, 143.0, 143.0, 143.0, 80.0, 80.0,
				80.0, 80.0, 80.0, 600.0, 600.0, 600.0, 600.0, 600.0, 600.0,
				600.0, };
		nd.qkappa = new double[] { 431.0, 431.0, 431.0, 432.0, 432.0, 433.0,
				434.0, 436.0, 437.0, 439.0, 440.0, 443.0, 445.0, 445.0,
				57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0,
				57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0,
				57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0, 57822.0,
				57822.0, 57822.0, 57822.0, 826.0, 826.0, 823.0, 822.0, 819.0,
				815.0, 811.0, 807.0, 803.0, 799.0, 795.0, 792.0, 788.0, 784.0,
				779.0, 775.0, 770.0, 766.0, 761.0, 755.0, 750.0, 743.0, 737.0,
				730.0, 744.0, 759.0, 362.0, 362.0, 362.0, 363.0, 364.0, 365.0,
				366.0, 372.0, 370.0, 367.0, 365.0, 362.0, 195.0, 195.0, 195.0,
				195.0, 195.0, 1447.0, 1446.0, 1446.0, 1350.0, 1350.0, 1456.0,
				1456.0, };
		nd.vp = new double[] { 11.2622, 11.26064, 11.25593, 11.24809, 11.23712,
				11.22301, 11.20576, 11.18538, 11.16186, 11.13521, 11.10542,
				11.07249, 11.03643, 11.02827, 10.35568, 10.30971, 10.24959,
				10.18743, 10.12291, 10.05572, 9.98554, 9.91206, 9.83496,
				9.75393, 9.66865, 9.57881, 9.48409, 9.38418, 9.27867, 9.16752,
				9.05015, 8.92632, 8.79573, 8.65805, 8.51298, 8.36019, 8.19939,
				8.06482, 13.7166, 13.71168, 13.68753, 13.68041, 13.59597,
				13.47742, 13.36074, 13.24532, 13.13055, 13.01579, 12.90045,
				12.78389, 12.6655, 12.54466, 12.42075, 12.29316, 12.16126,
				12.02445, 11.88209, 11.73357, 11.57828, 11.4156, 11.2449,
				11.06557, 10.91005, 10.75131, 10.26622, 10.21203, 10.15782,
				9.90185, 9.64588, 9.3899, 9.13397, 8.90522, 8.81867, 8.73209,
				8.64552, 8.55896, 7.9897, 8.0118, 8.0337, 8.0554, 8.07688,
				8.08907, 8.10119, 8.11061, 6.8, 6.8, 5.8, 5.8, };
		nd.vs = new double[] { 3.6678, 3.6667, 3.66342, 3.65794, 3.65027,
				3.64041, 3.62835, 3.61411, 3.59767, 3.57905, 3.55823, 3.53522,
				3.51002, 3.50432, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 7.26466, 7.26486, 7.26575, 7.26597, 7.23403,
				7.18892, 7.14423, 7.09974, 7.05525, 7.01053, 6.96538, 6.91957,
				6.87289, 6.82512, 6.77606, 6.72548, 6.67317, 6.61891, 6.5625,
				6.5037, 6.44232, 6.37813, 6.31091, 6.24046, 6.09418, 5.94508,
				5.5702, 5.54311, 5.51602, 5.37014, 5.22428, 5.07842, 4.93259,
				4.76989, 4.7384, 4.7069, 4.6754, 4.64391, 4.41885, 4.43108,
				4.44361, 4.45643, 4.46953, 4.47715, 4.48486, 4.49094, 3.9, 3.9,
				3.2, 3.2, };
		nd.computeBullenLaw();
		return nd;
	}

	/**
	 * @param path
	 *            model file written by nd format.
	 */
	public NamedDiscontinuityStructure(Path path) {
		infPath = path;
		readInfFile();
	}

	/**
	 * @return radius of the core mantle boundary [km]
	 */
	public double getCoreMantleBoundary() {
		return coreMantleBoundary;
	}

	/**
	 * @return radius of the inner core boundary [km]
	 */
	public double getInnerCoreBoundary() {
		return innerCoreBoundary;
	}

	private double coreMantleBoundary;
	private double innerCoreBoundary;

	private void readInfFile() {
		if (!Files.exists(infPath)) {
			// System.out.println("no "+infFile);
			throw new RuntimeException(" no " + infPath);
		}
		List<String[]> useLines = new ArrayList<>();
		// which layers are boundaries
		int iMoho = 0;
		int iCoreMantleBoundary = 0;
		int iInnerCoreBoundary = 0;
		try {
			List<String> lineList = Files.readAllLines(infPath);
			String mantle = "mantle";
			String outercore = "outer-core";
			String innercore = "inner-core";
			for (int i = 0; i < lineList.size(); i++) {
				String line = lineList.get(i);
				if (line.startsWith("#"))
					continue;
				if (line.equals(mantle)) {
					iMoho = i - 1;
					continue;
				} else if (line.equals(outercore)) {
					iCoreMantleBoundary = i - 1;
					continue;
				} else if (line.equals(innercore)) {
					iInnerCoreBoundary = i - 1;
					continue;
				}
				String[] parts = lineList.get(i).trim().split("\\s+");
				if (parts.length != 6)
					throw new RuntimeException("FORMAT ERROR");
				useLines.add(parts);
			}
			// lines = (String[][]) useLines.toArray(new String[0][]);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		nBoundary = useLines.size(); // inner core outer core.....
		rho = new double[nBoundary];
		qkappa = new double[nBoundary];
		qmu = new double[nBoundary];
		vs = new double[nBoundary];
		vp = new double[nBoundary];
		r = new double[nBoundary];
		for (int i = 0; i < nBoundary; i++) {
			String[] parts = useLines.get(i);
			int j = nBoundary - 1 - i;
			if (parts.length != 6)
				continue;
			r[j] = 6371 - Double.parseDouble(parts[0]);
			vp[j] = Double.parseDouble(parts[1]);
			vs[j] = Double.parseDouble(parts[2]);
			rho[j] = Double.parseDouble(parts[3]);
			qkappa[j] = Double.parseDouble(parts[4]);
			qmu[j] = Double.parseDouble(parts[5]);
			// j--;
		}

		coreMantleBoundary = r[nBoundary - iCoreMantleBoundary];
		innerCoreBoundary = r[nBoundary - iInnerCoreBoundary];
		mohoDiscontinuity = r[nBoundary - iMoho];

		computeBullenLaw();
		// System.out.println(coreMantleBoundary);
		// System.out.println(innerCoreBoundary);
	}

	/**
	 * @return radius of the moho discontinuity
	 */
	public double getMohoDiscontinuity() {
		return mohoDiscontinuity;
	}

	private double mohoDiscontinuity;

	/**
	 * @param r
	 *            radius [km]
	 * @return rho at r
	 */
	public double getRho(double r) {
		int izone = rToZone(r);
		// double[] coef = computeBullenLaw(izone, rho);
		// return coef[0] * Math.pow(6371 - r, coef[1]);
		double rho = computeBullenLaw(r, rhoA[izone], rhoB[izone]);
		return rho;
	}

	/**
	 * @param r
	 *            radius [km]
	 * @return Vp at r
	 */
	public double getVp(double r) {
		int izone = rToZone(r);
		// double[] coef = computeBullenLaw(izone, vp);
		double vp = computeBullenLaw(r, vpA[izone], vpB[izone]);
		// return coef[0] * Math.pow(6371 - r, coef[1])-vp;
		return vp;
	}

	/**
	 * @param r
	 *            radius [km]
	 * @return Vs at r
	 */
	public double getVs(double r) {
		int izone = rToZone(r);
		double vs = computeBullenLaw(r, vsA[izone], vsB[izone]);
		// double[] coef = computeBullenLaw(izone, vs);
		// return coef[0] * Math.pow(6371 - r, coef[1]);
		return vs;
	}

	/**
	 * @param r
	 *            radius [km]
	 * @return Qµ at r
	 */
	public double getQMu(double r) {
		int izone = rToZone(r);
		double qMu = computeBullenLaw(r, qmuA[izone], qmuB[izone]);
		// double[] coef = computeBullenLaw(izone, vs);
		// return coef[0] * Math.pow(6371 - r, coef[1]);
		return qMu;
	}

	/**
	 * @param r
	 *            radius [km]
	 * @return Qkappa at r
	 */
	public double getQKappa(double r) {
		int izone = rToZone(r);
		double qKappa = computeBullenLaw(r, qkappaA[izone], qkappaB[izone]);
		// double[] coef = computeBullenLaw(izone, vs);
		// return coef[0] * Math.pow(6371 - r, coef[1]);
		return qKappa;
	}

	/**
	 * @param izone zone number
	 * @return a of a*r**b in i th zone
	 */
	public double getVpA(int izone) {
		return vpA[izone];
	}

	/**
	 * @param izone zone number
	 * @return b of a*r**b in i th zone
	 */
	public double getVpB(int izone) {
		return vpB[izone];
	}

	/**
	 * @param izone zone number
	 * @return a of a*r**b in i th zone
	 */
	public double getVsA(int izone) {
		return vsA[izone];
	}

	/**
	 * @param izone zone number
	 * @return b of a*r**b in i th zone
	 */
	public double getVsB(int izone) {
		return vsB[izone];
	}

	/**
	 * @param r
	 *            radius [km]
	 * @param a
	 * @param b
	 * @return a*(6371-r)**b
	 */
	private static double computeBullenLaw(double r, double a, double b) {
		return a * Math.pow(r, b);
		// return a * Math.pow(6371 - r, b);
	}

	private int rToZone(double r) {
		for (int i = 0; i < this.r.length - 1; i++)
			if (this.r[i] <= r && r <= this.r[i + 1])
				return i;
		System.out.println("rToZone did not work correctly");
		return 0;
	}

	private void computeBullenLaw() {
		int nzone = nBoundary - 1;
		rhoA = new double[nzone];
		rhoB = new double[nzone];
		vpA = new double[nzone];
		vpB = new double[nzone];
		vsA = new double[nzone];
		vsB = new double[nzone];
		qmuA = new double[nzone];
		qmuB = new double[nzone];
		qkappaA = new double[nzone];
		qkappaB = new double[nzone];
		for (int i = 0; i < nzone; i++) {
			double[] rho = computeBullenLaw(i, this.rho);
			rhoA[i] = rho[0];
			rhoB[i] = rho[1];
			double[] vp = computeBullenLaw(i, this.vp);
			vpA[i] = vp[0];
			vpB[i] = vp[1];
			double[] vs = computeBullenLaw(i, this.vs);
			vsA[i] = vs[0];
			vsB[i] = vs[1];
			double[] qmu = computeBullenLaw(i, this.qmu);
			qmuA[i] = qmu[0];
			qmuB[i] = qmu[1];
			double[] qkappa = computeBullenLaw(i, this.qkappa);
			qkappaA[i] = qkappa[0];
			qkappaB[i] = qkappa[1];
		}

	}

	/**
	 * obtain the valued a, b of v(r) = a* r **b r is radius
	 * 
	 * @param izone
	 * @param val
	 * @return the value a, b in Bullen expression.
	 */
	private double[] computeBullenLaw(int izone, double[] val) {
		if (izone >= nBoundary || izone < 0) {
			System.out.println("calcBullenCoef did not work correctly");
			return null;
		}
		if (val[izone] == 0 && val[izone + 1] == 0)
			return new double[] { 0, 1 };
		// double r1 = r[izone];
		// double r2 = r[izone+1];
		// double val1 = val[izone];
		// double val2 = val[izone+1];

		double b = Math.log(val[izone + 1] / val[izone])
				/ Math.log((r[izone + 1]) / (r[izone]));
		// double b = Math.log(val[izone + 1] / val[izone])
		// / Math.log((6371 - r[izone + 1]) / (6371 - r[izone]));
		double a = val[izone] / Math.pow((r[izone]), b);

		return new double[] { a, b };

	}

	/**
	 * i=0 (center) imax(surface)
	 * 
	 * @param i zone number
	 * @return radius of i th boundary [km]
	 */
	public double getBoundary(int i) {
		return r[i];
	}

	/**
	 * @return the number of zones (nBoundary -1)
	 */
	public int getNzone() {
		return nBoundary - 1;
	}

}
