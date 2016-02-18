package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

/**
 * (input) Structure of the Earth for softwares of <i>Direct Solution Method</i> (DSM)<br>
 * 
 * Every depth is written in <b>radius</b>.<br>
 * 
 * This class is <b>IMMUTABLE</b> <br>
 * 
 * When you try to get values on radius of boundaries, you will get one in the
 * shallower layer, i.e., the layer which has the radius as rmin.
 * 
 * @version 0.2.1.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class PolynomialStructure {

	/**
	 * the number of layers
	 */
	private int nzone;
	private int coreZone = 2; // TODO
	private double[] rmin;
	private double[] rmax;
	private PolynomialFunction[] rho;
	private PolynomialFunction[] vpv;
	private PolynomialFunction[] vph;
	private PolynomialFunction[] vsv;
	private PolynomialFunction[] vsh;
	private PolynomialFunction[] eta;
	private double[] qMu;
	private double[] qKappa;

	private PolynomialStructure() {
	}

	/**
	 * transversely isotropic (TI) PREM by Dziewonski &amp; Anderson 1981
	 */
	public static final PolynomialStructure PREM = prem();

	/**
	 * isotropic (TI) PREM by Dziewonski &amp; Anderson 1981
	 */
	public static final PolynomialStructure ISO_PREM = iprem();

	/**
	 * AK135 by Kennett <i>et al</i>. (1995)
	 */
	public static final PolynomialStructure AK135 = ak135();

	/**
	 * @return standard TI PREM by Dziewonski &amp; Anderson 1981
	 */
	private static PolynomialStructure prem() {
		PolynomialStructure ps = new PolynomialStructure();
		ps.initialAnisoPREM();
		return ps;
	}

	/**
	 * @return standard AK135 by Kennett <i>et al</i>. (1995)
	 */
	private static PolynomialStructure ak135() {
		PolynomialStructure ps = new PolynomialStructure();
		ps.initialAK135();
		return ps;
	}

	/**
	 * @return standard ISOTROPIC PREM by Dziewonski &amp; Anderson 1981
	 */
	private static PolynomialStructure iprem() {
		PolynomialStructure ps = new PolynomialStructure();
		ps.initialIsoPREM();
		return ps;
	}

	/**
	 * @param structurePath
	 *            {@link Path} of a
	 * @throws IOException
	 *             if an I/O error occurs. A structure file (structurePath) must
	 *             exist.
	 */
	public PolynomialStructure(Path structurePath) throws IOException {
		readStructureFile(structurePath);
	}

	/**
	 * nzoneにしたがって、半径情報や速度情報を初期化する
	 */
	private void initialize() {
		// System.out.println("Initializing polinomial structure"
		// + " components by nzone " + nzone);
		if (nzone < 1)
			throw new IllegalStateException("nzone is invalid.");
		rmin = new double[nzone];
		rmax = new double[nzone];
		rho = new PolynomialFunction[nzone];
		vpv = new PolynomialFunction[nzone];
		vph = new PolynomialFunction[nzone];
		vsv = new PolynomialFunction[nzone];
		vsh = new PolynomialFunction[nzone];
		eta = new PolynomialFunction[nzone];
		qMu = new double[nzone];
		qKappa = new double[nzone];
	}

	/**
	 * (2L+N)/(3*rho)
	 * 
	 * @param r
	 *            radius
	 * @return effective isotropic shear wave velocity
	 */
	public double getVs(double r) {
		double l = computeL(r);
		double n = computeN(r);
		double rho = getRho(r);
		return Math.sqrt((2 * l + n) / 3 / rho);
	}

	/**
	 * @param r
	 *            radius
	 * @return &xi; (N/L)
	 */
	public double getXi(double r) {
		return computeN(r) / computeL(r);
	}

	/**
	 * 半径boundariesのところに境界を作る（層を一つ増やす）
	 * 
	 * Add boundaries at the radiuses.
	 * 
	 * 
	 * if there is already a boundary at r then nothing will be done.
	 * 
	 * @param boundaries
	 *            radiuses for boundaries. Values smaller than 0 or bigger than
	 *            earth radius will be ignored
	 * @return a new structure which have additional layers at the input
	 *         boundaries or this if there all the radiuses already exist in
	 *         this
	 */
	public PolynomialStructure addBoundaries(double... boundaries) {
		PolynomialStructure ps = new PolynomialStructure();
		double[] addBoundaries = Arrays.stream(boundaries)
				.filter(d -> 0 < d && d < rmax[nzone - 1] && Arrays.binarySearch(rmin, d) < 0).distinct().sorted()
				.toArray();
		if (addBoundaries.length == 0)
			return this;
		ps.nzone = nzone + addBoundaries.length;
		ps.initialize();
		ps.rmin = DoubleStream.concat(Arrays.stream(rmin), Arrays.stream(addBoundaries)).sorted().toArray();
		ps.rmax = DoubleStream.concat(Arrays.stream(rmax), Arrays.stream(addBoundaries)).sorted().toArray();
		for (double r : addBoundaries) {
			int izone = rtoZone(r);
			if (izone < coreZone)
				ps.coreZone++;
		}

		for (int iZone = 0; iZone < ps.nzone; iZone++) {
			double rmin = ps.rmin[iZone];
			// izone in this for rmin
			int oldIZone = rtoZone(rmin);
			// // 値のコピー
			// // deeper than r
			ps.qMu[iZone] = qMu[oldIZone];
			ps.qKappa[iZone] = qKappa[oldIZone];
			ps.rho[iZone] = rho[oldIZone];
			ps.vpv[iZone] = vpv[oldIZone];
			ps.vph[iZone] = vph[oldIZone];
			ps.vsv[iZone] = vsv[oldIZone];
			ps.vsh[iZone] = vsh[oldIZone];
			ps.eta[iZone] = eta[oldIZone];
		}
		return ps;
	}

	/**
	 * 
	 * A will be obtained for a radius under TI medium.
	 * 
	 * @param r
	 * @return the parameter A under TI approx.
	 */
	private double computeA(double r) {
		double vph = getVph(r);
		return getRho(r) * vph * vph;
	}

	/**
	 * 
	 * @param r
	 * @return the parameter C under TI approx.
	 */
	private double computeC(double r) {
		double vpv = getVpv(r);
		return getRho(r) * vpv * vpv;
	}

	public double getTransverselyIsotropicValue(TransverselyIsotropicParameter ti, double r) {
		switch (ti) {
		case A:
			return computeA(r);
		case C:
			return computeC(r);
		case ETA:
			return computeEta(r);
		case F:
			return computeF(r);
		case L:
			return computeL(r);
		case N:
			return computeN(r);
		default:
			throw new RuntimeException();
		}

	}

	private double computeEta(double r) {
		return eta[rtoZone(r)].value(toX(r));
	}

	private double computeF(double r) {
		return computeEta(r) * (computeA(r) - 2 * computeL(r));
	}

	private double computeL(double r) {
		double vsv = getVsv(r);
		return getRho(r) * vsv * vsv;
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return &mu; computed by Vs * Vs * &rho;
	 */
	public double getMu(double r) {
		double v = getVs(r);
		return v * v * getRho(r);
	}

	public double getLambda(double r) {
		return getRho(r) * getVph(r) * getVph(r) - 2 * getMu(r);
	}

	private double computeN(double r) {
		double v = getVsh(r);
		return getRho(r) * v * v;
	}

	public int getNzone() {
		return nzone;
	}

	public double getQkappa(double r) {
		return qKappa[rtoZone(r)];
	}

	public double[] getQmu() {
		return qMu.clone();
	}

	public double getQmu(double r) {
		return qMu[rtoZone(r)];
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return a value x to the input r for polynomial functions
	 */
	private double toX(double r) {
		return r / rmax[nzone - 1];
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return &rho; at the radius r
	 */
	public double getRho(double r) {
		return rho[rtoZone(r)].value(toX(r));
	}

	public PolynomialStructure setVsv(int izone, PolynomialFunction polynomialFunction) {
		PolynomialStructure str = deepCopy();
		str.vsv[izone] = polynomialFunction;
		return str;
	}

	public PolynomialStructure setVsh(int izone, PolynomialFunction polynomialFunction) {
		PolynomialStructure str = deepCopy();
		str.vsh[izone] = polynomialFunction;
		return str;
	}

	public PolynomialStructure setQMu(int izone, double qMu) {
		PolynomialStructure str = deepCopy();
		str.qMu[izone] = qMu;
		return str;
	}

	private PolynomialStructure deepCopy() {
		PolynomialStructure structure = new PolynomialStructure();
		structure.coreZone = coreZone;
		structure.rmin = rmin.clone();
		structure.rmax = rmax.clone();
		structure.nzone = nzone;
		structure.qMu = qMu.clone();
		structure.qKappa = qKappa.clone();
		structure.eta = eta.clone();
		structure.rho = rho.clone();
		structure.vpv = vpv.clone();
		structure.vph = vph.clone();
		structure.vsv = vsv.clone();
		structure.vsh = vsh.clone();
		return structure;
	}

	public double[] getRmax() {
		return rmax.clone();
	}

	public double[] getRmin() {
		return rmin.clone();
	}

	public double getVph(double r) {
		return vph[rtoZone(r)].value(toX(r));
	}

	public double getVpv(double r) {
		return vpv[rtoZone(r)].value(toX(r));
	}

	public double getVsh(double r) {
		return vsh[rtoZone(r)].value(toX(r));
	}

	public double getVsv(double r) {
		return vsv[rtoZone(r)].value(toX(r));
	}

	/**
	 * @param r
	 *            radius [0, rmax]
	 * @return the number of the zone which includes r. Note that the zone will
	 *         be rmin &le; r &lt; rmax except r = earth radius
	 */
	public int rtoZone(double r) {
		if (r == rmax[nzone - 1])
			return nzone - 1;
		return IntStream.range(0, nzone).filter(i -> rmin[i] <= r && r < rmax[i]).findAny()
				.orElseThrow(() -> new IllegalArgumentException("Input r:" + r + "is invalid."));
	}

	private void initialAnisoPREM() {
		nzone = 12;
		rmin = new double[] { 0, 1221.5, 3480, 3630, 5600, 5701, 5771, 5971, 6151, 6291, 6346.6, 6356 };
		rmax = new double[] { 1221.5, 3480, 3630, 5600, 5701, 5771, 5971, 6151, 6291, 6346.6, 6356, 6371 };
		double[][] rho = new double[][] { { 13.0885, 0, -8.8381, 0 }, { 12.5815, -1.2638, -3.6426, -5.5281 },
				{ 7.9565, -6.4761, 5.5283, -3.0807 }, { 7.9565, -6.4761, 5.5283, -3.0807 },
				{ 7.9565, -6.4761, 5.5283, -3.0807 }, { 5.3197, -1.4836, 0, 0 }, { 11.2494, -8.0298, 0, 0 },
				{ 7.1089, -3.8045, 0, 0 }, { 2.691, 0.6924, 0, 0 }, { 2.691, 0.6924, 0, 0 }, { 2.9, 0, 0, 0 },
				{ 2.6, 0, 0, 0 }, };

		//
		double[][] vpv = new double[][] { { 11.2622, 0, -6.364, 0 }, { 11.0487, -4.0362, 4.8023, -13.5732 },
				{ 15.3891, -5.3181, 5.5242, -2.5514 }, { 24.952, -40.4673, 51.4832, -26.6419 },
				{ 29.2766, -23.6027, 5.5242, -2.5514 }, { 19.0957, -9.8672, 0, 0 }, { 39.7027, -32.6166, 0, 0 },
				{ 20.3926, -12.2569, 0, 0 }, { 0.8317, 7.218, 0, 0 }, { 0.8317, 7.218, 0, 0 }, { 6.8, 0, 0, 0 },
				{ 5.8, 0, 0, 0 }, };

		double[][] vph = new double[][] { { 11.2622, 0, -6.364, 0 }, { 11.0487, -4.0362, 4.8023, -13.5732 },
				{ 15.3891, -5.3181, 5.5242, -2.5514 }, { 24.952, -40.4673, 51.4832, -26.6419 },
				{ 29.2766, -23.6027, 5.5242, -2.5514 }, { 19.0957, -9.8672, 0, 0 }, { 39.7027, -32.6166, 0, 0 },
				{ 20.3926, -12.2569, 0, 0 }, { 3.5908, 4.6172, 0, 0 }, { 3.5908, 4.6172, 0, 0 }, { 6.8, 0, 0, 0 },
				{ 5.8, 0, 0, 0 }, };

		double[][] vsv = new double[][] { { 3.6678, 0, -4.4475, 0 }, { 0, 0, 0, 0 },
				{ 6.9254, 1.4672, -2.0834, 0.9783 }, { 11.1671, -13.7818, 17.4575, -9.2777 },
				{ 22.3459, -17.2473, -2.0834, 0.9783 }, { 9.9839, -4.9324, 0, 0 }, { 22.3512, -18.5856, 0, 0 },
				{ 8.9496, -4.4597, 0, 0 }, { 5.8582, -1.4678, 0, 0 }, { 5.8582, -1.4678, 0, 0 }, { 3.9, 0, 0, 0 },
				{ 3.2, 0, 0, 0 }, };

		double[][] vsh = new double[][] { { 3.6678, 0, -4.4475, 0 }, { 0, 0, 0, 0 },
				{ 6.9254, 1.4672, -2.0834, 0.9783 }, { 11.1671, -13.7818, 17.4575, -9.2777 },
				{ 22.3459, -17.2473, -2.0834, 0.9783 }, { 9.9839, -4.9324, 0, 0 }, { 22.3512, -18.5856, 0, 0 },
				{ 8.9496, -4.4597, 0, 0 }, { -1.0839, 5.7176, 0, 0 }, { -1.0839, 5.7176, 0, 0 }, { 3.9, 0, 0, 0 },
				{ 3.2, 0, 0, 0 }, };

		double[][] eta = new double[][] { { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 },
				{ 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 3.3687, -2.4778, 0, 0 },
				{ 3.3687, -2.4778, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, };

		this.rho = new PolynomialFunction[nzone];
		this.vpv = new PolynomialFunction[nzone];
		this.vph = new PolynomialFunction[nzone];
		this.vsv = new PolynomialFunction[nzone];
		this.vsh = new PolynomialFunction[nzone];
		this.eta = new PolynomialFunction[nzone];

		for (int i = 0; i < nzone; i++) {
			this.rho[i] = new PolynomialFunction(rho[i]);
			this.vpv[i] = new PolynomialFunction(vpv[i]);
			this.vph[i] = new PolynomialFunction(vph[i]);
			this.vsv[i] = new PolynomialFunction(vsv[i]);
			this.vsh[i] = new PolynomialFunction(vsh[i]);
			this.eta[i] = new PolynomialFunction(eta[i]);
		}

		qMu = new double[] { 84.6, Double.POSITIVE_INFINITY, 312, 312, 312, 143, 143, 143, 80, 600, 600, 600, };
		qKappa = new double[] { 1327.7, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823 };
	}

	private void initialIsoPREM() {
		initialAnisoPREM();

		double[] vp = new double[] { 4.1875, 3.9382, 0, 0 };
		double[] vs = new double[] { 2.1519, 2.3481, 0, 0 };
		double[] eta = new double[] { 1, 0, 0, 0 };
		for (int i = 8; i <= 9; i++) {
			vpv[i] = new PolynomialFunction(vp);
			vph[i] = new PolynomialFunction(vp);
			vsv[i] = new PolynomialFunction(vs);
			vsh[i] = new PolynomialFunction(vs);
			this.eta[i] = new PolynomialFunction(eta);
		}
	}

	/**
	 * standard iasp91 (Kennett and Engdahl, 1991) It has no density model, TODO
	 * should use PREM??
	 */
	private void initialIASP91() {
		nzone = 11;
		rmin = new double[] { 0, 1217.1, 3482, 3631, 5611, 5711, 5961, 6161, 6251, 6336, 6351 };
		rmax = new double[] { 1217.1, 3482, 3631, 5611, 5711, 5961, 6161, 6251, 6336, 6351, 6371 };

		//
		double[][] vpv = new double[][] { { 11.24094, 0, -4.09689, 0 }, { 10.03904, 3.75665, -13.67046, 0 },
				{ 14.49470, -1.47089, 0, 0 }, { 25.1486, -41.1538, 51.9932, -26.6083 }, { 25.96984, -16.93412, 0, 0 },
				{ 29.38896, -21.40656, 0, 0 }, { 30.78765, -23.25415, 0, 0 }, { 25.41389, -17.69722, 0, 0 },
				{ 8.78541, -0.74953, 0, 0 }, { 6.5, 0, 0, 0 }, { 5.8, 0, 0, 0 } };

		double[][] vph = new double[][] { { 11.24094, 0, -4.09689, 0 }, { 10.03904, 3.75665, -13.67046, 0 },
				{ 14.49470, -1.47089, 0, 0 }, { 25.1486, -41.1538, 51.9932, -26.6083 }, { 25.96984, -16.93412, 0, 0 },
				{ 29.38896, -21.40656, 0, 0 }, { 30.78765, -23.25415, 0, 0 }, { 25.41389, -17.69722, 0, 0 },
				{ 8.78541, -0.74953, 0, 0 }, { 6.5, 0, 0, 0 }, { 5.8, 0, 0, 0 } };

		double[][] vsv = new double[][] { { 3.56454, 0, -3.45241, 0 }, { 0, 0, 0, 0 }, { 8.16616, -1.58206, 0, 0 },
				{ 12.9303, -21.259, 27.8988, -14.108 }, { 20.7689, -16.53147, 0, 0 }, { 17.70732, -13.50652, 0, 0 },
				{ 15.24213, -11.08552, 0, 0 }, { 5.7502, -1.2742, 0, 0 }, { 6.706231, -2.248585, 0, 0 },
				{ 3.75, 0, 0, 0 }, { 3.36, 0, 0, 0 } };

		double[][] vsh = new double[][] { { 3.56454, 0, -3.45241, 0 }, { 0, 0, 0, 0 }, { 8.16616, -1.58206, 0, 0 },
				{ 12.9303, -21.259, 27.8988, -14.108 }, { 20.7689, -16.53147, 0, 0 }, { 17.70732, -13.50652, 0, 0 },
				{ 15.24213, -11.08552, 0, 0 }, { 5.7502, -1.2742, 0, 0 }, { 6.706231, -2.248585, 0, 0 },
				{ 3.75, 0, 0, 0 }, { 3.36, 0, 0, 0 } };

		double[][] eta = new double[][] { { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 },
				{ 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 },
				{ 1, 0, 0, 0 }, };
	}

	/**
	 * standard AK135 Kennett <i>et al<i>. (1995)
	 */
	private void initialAK135() {
		nzone = 11;
		rmin = new double[] { 0, 1217.5, 3479.5, 3631, 5611, 5711, 5961, 6161, 6251, 6336.6, 6351 };
		rmax = new double[] { 1217.5, 3479.5, 3631, 5611, 5711, 5961, 6161, 6251, 6336.6, 6351, 6371 };
		double[][] rho = new double[][] { { 13.0885, 0, -8.8381, 0 }, { 12.5815, -1.2638, -3.6426, -5.5281 },
				{ 7.9565, -6.4761, 5.5283, -3.0807 }, { 7.9565, -6.4761, 5.5283, -3.0807 },
				{ 7.9565, -6.4761, 5.5283, -3.0807 }, { 5.3197, -1.4836, 0, 0 }, { 11.2494, -8.0298, 0, 0 },
				{ 7.1089, -3.8045, 0, 0 }, { 2.691, 0.6924, 0, 0 }, { 2.691, 0.6924, 0, 0 }, { 2.9, 0, 0, 0 },
				{ 2.6, 0, 0, 0 }, };
		double[][] vpv = new double[][] { { 11.261692, 0.028794, -6.627846, 0 }, { 10.118851, 3.457774, -13.434875, 0 },
				{ 13.908244, -0.45417, 0, 0 }, { 24.138794, -37.097655, 46.631994, -24.272115 },
				{ 25.969838, -16.934118, 0, 0 }, { 29.38896, -21.40656, 0, 0 }, { 30.78765, -23.25415, 0, 0 },
				{ 25.413889, -17.697222, 0, 0 }, { 8.785412, -0.749529, 0, 0, }, { 6.5, 0.0, 0.0, 0.0 },
				{ 5.8, 0.0, 0.0, 0.0 }, };
		double[][] vph = new double[][] { { 11.261692, 0.028794, -6.627846, 0 }, { 10.118851, 3.457774, -13.434875, 0 },
				{ 13.908244, -0.45417, 0, 0 }, { 24.138794, -37.097655, 46.631994, -24.272115 },
				{ 25.969838, -16.934118, 0, 0 }, { 29.38896, -21.40656, 0, 0 }, { 30.78765, -23.25415, 0, 0 },
				{ 25.413889, -17.697222, 0, 0 }, { 8.785412, -0.749529, 0, 0, }, { 6.5, 0, 0, 0 }, { 5.8, 0, 0, 0 } };
		double[][] vsv = new double[][] { { 3.667865, -0.001345, -4.440915, 0 }, { 0, 0, 0, 0 },
				{ 8.018341, -1.349895, 0, 0 }, { 12.213901, -18.573085, 24.557329, -12.728015 },
				{ 20.208945, -15.895645, 0, 0 }, { 17.71732, -13.50652, 0, 0 }, { 15.212335, -11.053685, 0, 0 },
				{ 5.7502, -1.2742, 0, 0 }, { 5.970824, -1.499059, 0, 0 }, { 3.85, 0, 0, 0 }, { 3.46, 0, 0, 0 } };
		double[][] vsh = new double[][] { { 3.667865, -0.001345, -4.440915, 0 }, { 0, 0, 0, 0 },
				{ 8.018341, -1.349895, 0, 0 }, { 12.213901, -18.573085, 24.557329, -12.728015 },
				{ 20.208945, -15.895645, 0, 0 }, { 17.71732, -13.50652, 0, 0 }, { 15.212335, -11.053685, 0, 0 },
				{ 5.7502, -1.2742, 0, 0 }, { 5.970824, -1.499059, 0, 0 }, { 3.85, 0, 0, 0 }, { 3.46, 0, 0, 0 } };
		double[][] eta = new double[][] { { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 },
				{ 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 },
				{ 1, 0, 0, 0 }, }; // ok

		this.rho = new PolynomialFunction[nzone];
		this.vpv = new PolynomialFunction[nzone];
		this.vph = new PolynomialFunction[nzone];
		this.vsv = new PolynomialFunction[nzone];
		this.vsh = new PolynomialFunction[nzone];
		this.eta = new PolynomialFunction[nzone];

		for (int i = 0; i < nzone; i++) {
			this.rho[i] = new PolynomialFunction(rho[i]);
			this.vpv[i] = new PolynomialFunction(vpv[i]);
			this.vph[i] = new PolynomialFunction(vph[i]);
			this.vsv[i] = new PolynomialFunction(vsv[i]);
			this.vsh[i] = new PolynomialFunction(vsh[i]);
			this.eta[i] = new PolynomialFunction(eta[i]);
		}

		qMu = new double[] { 84.6, -1, 312, 312, 312, 143, 143, 80, 600, 600, 600, }; // ok
		qKappa = new double[] { 1327.7, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823, }; // OK
	}

	/**
	 * structureLines must not have comment lines and must have only structure
	 * lines. structureLines must have been trimmed.
	 * 
	 * @param structureLines
	 *            lines for a structure
	 */
	private void readLines(String[] structureLines) {
		String space = "\\s+";
		int nzone = Integer.parseInt(structureLines[0].split(space)[0]);
		if (structureLines.length != (nzone * 6 + 1))
			throw new IllegalArgumentException("Invalid lines");
		this.nzone = nzone;
		this.initialize();
		for (int i = 0; i < nzone; i++) {
			String[] rangeRhoParts = structureLines[i * 6 + 1].split(space);
			String[] vpvParts = structureLines[i * 6 + 2].split(space);
			String[] vphParts = structureLines[i * 6 + 3].split(space);
			String[] vsvParts = structureLines[i * 6 + 4].split(space);
			String[] vshParts = structureLines[i * 6 + 5].split(space);
			String[] etaParts = structureLines[i * 6 + 6].split(space);
			this.rmin[i] = Double.parseDouble(rangeRhoParts[0]);
			this.rmax[i] = Double.parseDouble(rangeRhoParts[1]);
			double[] rho = new double[4];
			double[] vpv = new double[4];
			double[] vph = new double[4];
			double[] vsv = new double[4];
			double[] vsh = new double[4];
			double[] eta = new double[4];
			for (int j = 0; j < 4; j++) {
				rho[j] = Double.parseDouble(rangeRhoParts[j + 2]);
				vpv[j] = Double.parseDouble(vpvParts[j]);
				vph[j] = Double.parseDouble(vphParts[j]);
				vsv[j] = Double.parseDouble(vsvParts[j]);
				vsh[j] = Double.parseDouble(vshParts[j]);
				eta[j] = Double.parseDouble(etaParts[j]);
			}

			this.rho[i] = new PolynomialFunction(rho);
			this.vpv[i] = new PolynomialFunction(vpv);
			this.vph[i] = new PolynomialFunction(vph);
			this.vsv[i] = new PolynomialFunction(vsv);
			this.vsh[i] = new PolynomialFunction(vsh);
			this.eta[i] = new PolynomialFunction(eta);

			this.qMu[i] = Double.parseDouble(etaParts[4]);
			this.qKappa[i] = Double.parseDouble(etaParts[5]);
		}
	}

	/**
	 * Read a structure file
	 * 
	 * @param structurePath
	 *            {@link Path} of a structureFile
	 */
	private void readStructureFile(Path structurePath) throws IOException {
		InformationFileReader reader = new InformationFileReader(structurePath);
		readLines(reader.getNonCommentLines());
	}

	public String[] toSHlines() {
		String[] outString = new String[3 * (nzone - coreZone) + 4];
		int zone = nzone - coreZone;
		outString[0] = String.valueOf(zone) + " nzone";
		outString[1] = "c  --- Radius(km) ---  --- Density (g/cm^3) ---";
		outString[2] = "c                      ---   Vsv     (km/s) ---";
		outString[3] = "c                      ---   Vsh     (km/s) ---      - Qmu -";
		for (int i = coreZone; i < nzone; i++) {
			outString[3 * (i - coreZone) + 4] = String.valueOf(rmin[i]) + " " + String.valueOf(rmax[i]) + " "
					+ toLine(rho[i]);
			outString[3 * (i - coreZone) + 5] = toLine(vsv[i]);
			outString[3 * (i - coreZone) + 6] = toLine(vsh[i]) + " " + String.valueOf(qMu[i]);
		}
		return outString;
	}

	/**
	 * change String line from coefficients a + bx + cx**2 >>>>> a b c 0
	 * 
	 * @param pf polynomial function for a layer
	 * @return string in a form of this
	 */
	private static String toLine(PolynomialFunction pf) {
		double[] coef = new double[4];
		double[] pfCoef = pf.getCoefficients();
		for (int i = 0; i < pfCoef.length; i++)
			coef[i] = pfCoef[i];
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 4; i++)
			sb.append(coef[i] + " ");
		return sb.toString().trim();

	}

	public String[] toPSVlines() {
		String[] outString = new String[6 * (nzone) + 7];
		// int zone = nzone - cmbZone;
		outString[0] = String.valueOf(nzone) + " nzone";
		// System.out.println(nzone + " " + cmbZone);
		outString[1] = "c  - Radius(km) -     --- Density (g/cm^3)---";
		outString[2] = "c                     ---   Vpv     (km/s) ---";
		outString[3] = "c                     ---   Vph     (km/s) ---";
		outString[4] = "c                     ---   Vsv     (km/s) ---";
		outString[5] = "c                     ---   Vsh     (km/s) ---";
		outString[6] = "c                     ---   eta     (ND  ) ---             - Qmu -  - Qkappa -";
		for (int i = 0; i < nzone; i++) {
			outString[6 * i + 7] = String.valueOf(rmin[i]) + " " + String.valueOf(rmax[i]) + " " + toLine(rho[i]);
			outString[6 * i + 8] = toLine(vpv[i]);
			outString[6 * i + 9] = toLine(vph[i]);
			outString[6 * i + 10] = toLine(vsv[i]);
			outString[6 * i + 11] = toLine(vsh[i]);
			outString[6 * i + 12] = toLine(eta[i]) + " " + String.valueOf(qMu[i]) + " " + String.valueOf(qKappa[i]);
		}
		return outString;
	}

	/**
	 * @param outPath
	 *            {@link Path} of an output file.
	 * @param options
	 *            for output
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void writePSV(Path outPath, OpenOption... options) throws IOException {
		Files.write(outPath, Arrays.asList(toPSVlines()), options);
	}

	/**
	 * @param outPath
	 *            {@link Path} of an output file.
	 * @param options
	 *            for output
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void writeSH(Path outPath, OpenOption... options) throws IOException {
		Files.write(outPath, Arrays.asList(toSHlines()), options);
	}

	public PolynomialFunction[] getVph() {
		return vph.clone();
	}

	public PolynomialFunction[] getVsv() {
		return vsv.clone();
	}

	public PolynomialFunction[] getVsh() {
		return vsh.clone();
	}
}
