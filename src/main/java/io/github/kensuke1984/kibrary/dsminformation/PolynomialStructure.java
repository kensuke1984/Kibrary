package io.github.kensuke1984.kibrary.dsminformation;

import io.github.kensuke1984.kibrary.util.Earth;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

/**
 * (input) Structure of the Earth for softwares of <i>Direct Solution Method</i>
 * (DSM)<br>
 * 
 * Every depth is written in <b>radius</b>.<br>
 * 
 * This class is <b>IMMUTABLE</b> <br>
 * 
 * When you try to get values on radius of boundaries, you will get one in the
 * shallower layer, i.e., the layer which has the radius as rmin.
 * 
 * @version 0.2.3.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class PolynomialStructure implements Serializable {

	/**
	 * 2016/8/24
	 */
	private static final long serialVersionUID = -5147029504840598303L;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + coreZone;
		result = prime * result + Arrays.hashCode(eta);
		result = prime * result + nzone;
		result = prime * result + Arrays.hashCode(qKappa);
		result = prime * result + Arrays.hashCode(qMu);
		result = prime * result + Arrays.hashCode(rho);
		result = prime * result + Arrays.hashCode(rmax);
		result = prime * result + Arrays.hashCode(rmin);
		result = prime * result + Arrays.hashCode(vph);
		result = prime * result + Arrays.hashCode(vpv);
		result = prime * result + Arrays.hashCode(vsh);
		result = prime * result + Arrays.hashCode(vsv);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PolynomialStructure other = (PolynomialStructure) obj;
		if (coreZone != other.coreZone)
			return false;
		if (!Arrays.equals(eta, other.eta))
			return false;
		if (nzone != other.nzone)
			return false;
		if (!Arrays.equals(qKappa, other.qKappa))
			return false;
		if (!Arrays.equals(qMu, other.qMu))
			return false;
		if (!Arrays.equals(rho, other.rho))
			return false;
		if (!Arrays.equals(rmax, other.rmax))
			return false;
		if (!Arrays.equals(rmin, other.rmin))
			return false;
		if (!Arrays.equals(vph, other.vph))
			return false;
		if (!Arrays.equals(vpv, other.vpv))
			return false;
		if (!Arrays.equals(vsh, other.vsh))
			return false;
		if (!Arrays.equals(vsv, other.vsv))
			return false;
		return true;
	}

	/**
	 * the number of layers
	 */
	private int nzone;

	/**
	 * Number of zones of cores.
	 */
	private int coreZone = 2; // TODO

	/**
	 * @return Number of core zones.
	 */
	public int getCoreZone() {
		return coreZone;
	}

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
	
	private PolynomialStructure(PolynomialStructure ps) {
		this.nzone = ps.nzone;
		this.rmin = ps.rmin;
		this.rmax = ps.rmax;
		this.rho = ps.rho;
		this.vpv = ps.vpv;
		this.vph = ps.vph;
		this.vsv = ps.vsv;
		this.vsh = ps.vsh;
		this.eta = ps.eta;
		this.qMu = ps.qMu;
		this.qKappa = ps.qKappa;
	}
	
	@Override
	public PolynomialStructure clone() {
		PolynomialStructure ps = new PolynomialStructure();
		ps.nzone = this.nzone;
		ps.rmin = this.rmin;
		ps.rmax = this.rmax;
		ps.rho = this.rho;
		ps.vpv = this.vpv;
		ps.vph = this.vph;
		ps.vsv = this.vsv;
		ps.vsh = this.vsh;
		ps.eta = this.eta;
		ps.qMu = this.qMu;
		ps.qKappa = this.qKappa;
		return ps;
	}

	/**
	 * transversely isotropic (TI) PREM by Dziewonski &amp; Anderson 1981
	 */
	public static final PolynomialStructure PREM = initialAnisoPREM();

	/**
	 * isotropic PREM by Dziewonski &amp; Anderson 1981
	 */
	public static final PolynomialStructure ISO_PREM = initialIsoPREM();

	/**
	 * AK135 by Kennett <i>et al</i>. (1995)
	 */
	public static final PolynomialStructure AK135 = initialAK135();

	public static final PolynomialStructure MIASP91 = initialMIASP91();
	
	public static final PolynomialStructure TBL50 = initialTBL50();
	
	public static final PolynomialStructure TNASNA = initialTNASNA();
	
	public static final PolynomialStructure AK135_elastic = initialAK135_elastic();
	
	public static final PolynomialStructure MAK135 = initialMAK135();
	
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
	 * (2L+N)/(3&rho;)
	 * 
	 * @param r
	 *            [km] radius
	 * @return effective isotropic shear wave velocity
	 */
	public double computeVs(double r) {
		return Math.sqrt((2 * computeL(r) + computeN(r)) / 3 / getRhoAt(r));
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return &xi; (N/L)
	 */
	public double computeXi(double r) {
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
		ps.coreZone += Arrays.stream(addBoundaries).filter(r -> r < rmin[coreZone]).count();

		for (int iZone = 0; iZone < ps.nzone; iZone++) {
			double rmin = ps.rmin[iZone];
			// izone in this for rmin
			int oldIZone = zoneOf(rmin);
			// // 値のコピー
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
	
	public PolynomialStructure addRhoDiscontinuity(double r1, double r2, double percent) {
		PolynomialStructure ps = this.clone();
		boolean foundR1 = false;
		boolean foundR2 = false;
		for (double r : ps.rmin) {
			if (Math.abs(r1-r) < 1e-6)
				foundR1 = true;
			if (Math.abs(r2-r) < 1e-6)
				foundR2 = true;
		}
		if (!foundR1)
			ps = ps.addBoundaries(r1);
		if (!foundR2)
			ps = ps.addBoundaries(r2);
		int izoneR1 = ps.getiZoneOf(r1);
		int izoneR2 = ps.getiZoneOf(r2);
		double value = ps.getRhoAt(r2);
		double increment = value * percent;
		PolynomialFunction p0 = new PolynomialFunction(new double[] {increment});
		for (int izone = izoneR1; izone < izoneR2; izone++)
			ps.rho[izone] = ps.rho[izone].add(p0);
		return ps;
	}
	
	public PolynomialStructure addVsDiscontinuity(double r1, double r2, double percent) {
		PolynomialStructure ps = this.clone();
		boolean foundR1 = false;
		boolean foundR2 = false;
		for (double r : ps.rmin) {
			if (Math.abs(r1-r) < 1e-6)
				foundR1 = true;
			if (Math.abs(r2-r) < 1e-6)
				foundR2 = true;
		}
		if (!foundR1)
			ps = ps.addBoundaries(r1);
		if (!foundR2)
			ps = ps.addBoundaries(r2);
		int izoneR1 = ps.getiZoneOf(r1);
		int izoneR2 = ps.getiZoneOf(r2);
		double value = ps.computeVs(r2);
		double increment = value * percent;
		PolynomialFunction p0 = new PolynomialFunction(new double[] {increment});
		for (int izone = izoneR1; izone < izoneR2; izone++) {
			ps.vsv[izone] = ps.vsv[izone].add(p0);
			ps.vsh[izone] = ps.vsh[izone].add(p0);
		}
		return ps;
	}

	/**
	 * A = &rho;V<sub>PH</sub><sup>2</sup>
	 * 
	 * @param r
	 *            [km] radius
	 * @return the parameter A under TI approx.
	 */
	public double computeA(double r) {
		double vph = getVphAt(r);
		return getRhoAt(r) * vph * vph;
	}

	/**
	 * C = &rho;V<sub>PV</sub><sup>2</sup>
	 * 
	 * @param r
	 *            [km] radius
	 * @return the parameter C under TI approximation.
	 */
	public double computeC(double r) {
		double vpv = getVpvAt(r);
		return getRhoAt(r) * vpv * vpv;
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

	public double computeEta(double r) {
		return eta[zoneOf(r)].value(toX(r));
	}

	/**
	 * F = &eta;(A-2*L)
	 * 
	 * @param r
	 *            [km]
	 * @return the parameter F under TI approx.
	 */
	public double computeF(double r) {
		return computeEta(r) * (computeA(r) - 2 * computeL(r));
	}

	/**
	 * L = &rho;V<sub>SV</sub><sup>2</sup>
	 * 
	 * @param r
	 *            [km]
	 * @return the parameter L under TI approx.
	 */
	public double computeL(double r) {
		double vsv = getVsvAt(r);
		return getRhoAt(r) * vsv * vsv;
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return &mu; computed by Vs * Vs * &rho;
	 */
	public double computeMu(double r) {
		double v = computeVs(r);
		return v * v * getRhoAt(r);
	}
	
	public double computeKappa(double r) {
		return computeLambda(r) + 2./3. * computeMu(r);
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return &lambda; computed by Vs * Vs * &rho;
	 */
	public double computeLambda(double r) {
		double v = getVphAt(r);
		return getRhoAt(r) * v * v - 2 * computeMu(r);
	}

	/**
	 * N = &rho;V<sub>SH</sub><sup>2</sup>
	 * 
	 * @param r
	 *            [km]
	 * @return the parameter N under TI approx.
	 */
	public double computeN(double r) {
		double v = getVshAt(r);
		return getRhoAt(r) * v * v;
	}

	/**
	 * @return number of zones
	 */
	public int getNzone() {
		return nzone;
	}
	
	public int getiZoneOf(double r) {
		int iz = 0;
		if (r == Earth.EARTH_RADIUS)
			return nzone - 1;
		for (int i = 0; i < nzone; i++) {
			if (rmin[i] <= r && rmax[i] > r) {
				iz = i;
				break;
			}
		}
		return iz;
	}

	/**
	 * @param i
	 *            index of a zone
	 * @return Q<sub>&mu;</sub> of the zone
	 */
	public double getQMuOf(int i) {
		return qMu[i];
	}

	/**
	 * x = r / earth radius
	 * 
	 * @param r
	 *            [km] radius
	 * @return a value x to the input r for polynomial functions
	 */
	private double toX(double r) {
		return r / rmax[nzone - 1];
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
	
	public PolynomialStructure setVpv(int izone, PolynomialFunction polynomialFunction) {
		PolynomialStructure str = deepCopy();
		str.vpv[izone] = polynomialFunction;
		return str;
	}

	public PolynomialStructure setVph(int izone, PolynomialFunction polynomialFunction) {
		PolynomialStructure str = deepCopy();
		str.vph[izone] = polynomialFunction;
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

	/**
	 * @param izone
	 *            index of the zone
	 * @return minimum radius of the zone
	 */
	public double getRMinOf(int izone) {
		return rmin[izone];
	}

	/**
	 * @param izone
	 *            index of a zone
	 * @return maximum radius of the zone
	 */
	public double getRMaxOf(int izone) {
		return rmax[izone];
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return &rho; at the radius r
	 */
	public double getRhoAt(double r) {
		return rho[zoneOf(r)].value(toX(r));
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return V<sub>PV</sub> at the radius r
	 */
	public double getVpvAt(double r) {
		return vpv[zoneOf(r)].value(toX(r));
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return V<sub>PH</sub> at the radius r
	 */
	public double getVphAt(double r) {
		return vph[zoneOf(r)].value(toX(r));
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return V<sub>SV</sub> at the radius r
	 */
	public double getVsvAt(double r) {
		return vsv[zoneOf(r)].value(toX(r));
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return V<sub>SH</sub> at the radius r
	 */
	public double getVshAt(double r) {
		return vsh[zoneOf(r)].value(toX(r));
	}
	
	public double getVbAt(double r) {
		double vsh = getVshAt(r);
		double vph = getVphAt(r);
		return Math.sqrt(vph * vph - 4./3. * vsh * vsh);
	}
	
	public double getEtaAt(double r) {
		return eta[zoneOf(r)].value(toX(r));
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return Q<sub>&mu;</sub> at the radius r
	 */
	public double getQmuAt(double r) {
		return qMu[zoneOf(r)];
	}

	/**
	 * @param r
	 *            [km] radius
	 * @return Q<sub>&kappa;</sub> at the radius r
	 */
	public double getQkappaAt(double r) {
		return qKappa[zoneOf(r)];
	}

	/**
	 * @param r
	 *            [km] radius [0, rmax]
	 * @return the number of the zone which includes r. Note that the zone will
	 *         be rmin &le; r &lt; rmax except r = earth radius
	 */
	public int zoneOf(double r) {
		if (r == rmax[nzone - 1])
			return nzone - 1;
		return IntStream.range(0, nzone).filter(i -> rmin[i] <= r && r < rmax[i]).findAny()
				.orElseThrow(() -> new IllegalArgumentException("Input r:" + r + "is invalid."));
	}

	private static PolynomialStructure set(int nzone, double[] rmin, double[] rmax, double[][] rho, double[][] vpv,
			double[][] vph, double[][] vsv, double[][] vsh, double[][] eta, double[] qMu, double[] qKappa) {
		final PolynomialStructure structure = new PolynomialStructure();
		structure.nzone = nzone;
		structure.rmin = rmin;
		structure.rmax = rmax;

		structure.rho = new PolynomialFunction[nzone];
		structure.vpv = new PolynomialFunction[nzone];
		structure.vph = new PolynomialFunction[nzone];
		structure.vsv = new PolynomialFunction[nzone];
		structure.vsh = new PolynomialFunction[nzone];
		structure.eta = new PolynomialFunction[nzone];
		structure.qMu = qMu;
		structure.qKappa = qKappa;

		for (int i = 0; i < nzone; i++) {
			structure.rho[i] = new PolynomialFunction(rho[i]);
			structure.vpv[i] = new PolynomialFunction(vpv[i]);
			structure.vph[i] = new PolynomialFunction(vph[i]);
			structure.vsv[i] = new PolynomialFunction(vsv[i]);
			structure.vsh[i] = new PolynomialFunction(vsh[i]);
			structure.eta[i] = new PolynomialFunction(eta[i]);
		}

		return structure;
	}

	private static PolynomialStructure initialAnisoPREM() {
		final int nzone = 12;
		final double[] rmin = new double[] { 0, 1221.5, 3480, 3630, 5600, 5701, 5771, 5971, 6151, 6291, 6346.6, 6356 };
		final double[] rmax = new double[] { 1221.5, 3480, 3630, 5600, 5701, 5771, 5971, 6151, 6291, 6346.6, 6356, 6371 };
		final double[][] rho = new double[][] { { 13.0885, 0, -8.8381, 0 }, { 12.5815, -1.2638, -3.6426, -5.5281 },
				{ 7.9565, -6.4761, 5.5283, -3.0807 }, { 7.9565, -6.4761, 5.5283, -3.0807 },
				{ 7.9565, -6.4761, 5.5283, -3.0807 }, { 5.3197, -1.4836, 0, 0 }, { 11.2494, -8.0298, 0, 0 },
				{ 7.1089, -3.8045, 0, 0 }, { 2.691, 0.6924, 0, 0 }, { 2.691, 0.6924, 0, 0 }, { 2.9, 0, 0, 0 },
				{ 2.6, 0, 0, 0 }, };
		final double[][] vpv = new double[][] { { 11.2622, 0, -6.364, 0 }, { 11.0487, -4.0362, 4.8023, -13.5732 },
				{ 15.3891, -5.3181, 5.5242, -2.5514 }, { 24.952, -40.4673, 51.4832, -26.6419 },
				{ 29.2766, -23.6027, 5.5242, -2.5514 }, { 19.0957, -9.8672, 0, 0 }, { 39.7027, -32.6166, 0, 0 },
				{ 20.3926, -12.2569, 0, 0 }, { 0.8317, 7.218, 0, 0 }, { 0.8317, 7.218, 0, 0 }, { 6.8, 0, 0, 0 },
				{ 5.8, 0, 0, 0 }, };
		final double[][] vph = new double[][] { { 11.2622, 0, -6.364, 0 }, { 11.0487, -4.0362, 4.8023, -13.5732 },
				{ 15.3891, -5.3181, 5.5242, -2.5514 }, { 24.952, -40.4673, 51.4832, -26.6419 },
				{ 29.2766, -23.6027, 5.5242, -2.5514 }, { 19.0957, -9.8672, 0, 0 }, { 39.7027, -32.6166, 0, 0 },
				{ 20.3926, -12.2569, 0, 0 }, { 3.5908, 4.6172, 0, 0 }, { 3.5908, 4.6172, 0, 0 }, { 6.8, 0, 0, 0 },
				{ 5.8, 0, 0, 0 }, };
		final double[][] vsv = new double[][] { { 3.6678, 0, -4.4475, 0 }, { 0, 0, 0, 0 },
				{ 6.9254, 1.4672, -2.0834, 0.9783 }, { 11.1671, -13.7818, 17.4575, -9.2777 },
				{ 22.3459, -17.2473, -2.0834, 0.9783 }, { 9.9839, -4.9324, 0, 0 }, { 22.3512, -18.5856, 0, 0 },
				{ 8.9496, -4.4597, 0, 0 }, { 5.8582, -1.4678, 0, 0 }, { 5.8582, -1.4678, 0, 0 }, { 3.9, 0, 0, 0 },
				{ 3.2, 0, 0, 0 }, };
		final double[][] vsh = new double[][] { { 3.6678, 0, -4.4475, 0 }, { 0, 0, 0, 0 },
				{ 6.9254, 1.4672, -2.0834, 0.9783 }, { 11.1671, -13.7818, 17.4575, -9.2777 },
				{ 22.3459, -17.2473, -2.0834, 0.9783 }, { 9.9839, -4.9324, 0, 0 }, { 22.3512, -18.5856, 0, 0 },
				{ 8.9496, -4.4597, 0, 0 }, { -1.0839, 5.7176, 0, 0 }, { -1.0839, 5.7176, 0, 0 }, { 3.9, 0, 0, 0 },
				{ 3.2, 0, 0, 0 }, };
		final double[][] eta = new double[][] { { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 },
				{ 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 3.3687, -2.4778, 0, 0 },
				{ 3.3687, -2.4778, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, };
		final double[] qMu = new double[] { 84.6, Double.POSITIVE_INFINITY, 312, 312, 312, 143, 143, 143, 80, 600, 600,
				600, };
		final double[] qKappa = new double[] { 1327.7, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823,
				57823, 57823 };
		return set(nzone, rmin, rmax, rho, vpv, vph, vsv, vsh, eta, qMu, qKappa);
	}

	private static PolynomialStructure initialIsoPREM() {
		PolynomialStructure prem = initialAnisoPREM();
		final double[] vp = new double[] { 4.1875, 3.9382, 0, 0 };
		final double[] vs = new double[] { 2.1519, 2.3481, 0, 0 };
		final double[] eta = new double[] { 1, 0, 0, 0 };
		for (int i = 8; i <= 9; i++) {
			prem.vpv[i] = new PolynomialFunction(vp);
			prem.vph[i] = new PolynomialFunction(vp);
			prem.vsv[i] = new PolynomialFunction(vs);
			prem.vsh[i] = new PolynomialFunction(vs);
			prem.eta[i] = new PolynomialFunction(eta);
		}
		return prem;
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
	private static PolynomialStructure initialAK135() {
		final int nzone = 11;
		final double[] rmin = new double[] { 0, 1217.5, 3479.5, 3631, 5611, 5711, 5961, 6161, 6251, 6336.6, 6351 };
		final double[] rmax = new double[] { 1217.5, 3479.5, 3631, 5611, 5711, 5961, 6161, 6251, 6336.6, 6351, 6371 };
		final double[][] rho = new double[][] { { 13.0885, 0, -8.8381, 0 }, { 12.5815, -1.2638, -3.6426, -5.5281 },
				{ 7.9565, -6.4761, 5.5283, -3.0807 }, { 7.9565, -6.4761, 5.5283, -3.0807 },
				{ 7.9565, -6.4761, 5.5283, -3.0807 }, { 5.3197, -1.4836, 0, 0 }, { 11.2494, -8.0298, 0, 0 },
				{ 7.1089, -3.8045, 0, 0 }, { 2.691, 0.6924, 0, 0 }, { 2.691, 0.6924, 0, 0 }, { 2.9, 0, 0, 0 },
				{ 2.6, 0, 0, 0 }, };
		final double[][] vpv = new double[][] { { 11.261692, 0.028794, -6.627846, 0 },
				{ 10.118851, 3.457774, -13.434875, 0 }, { 13.908244, -0.45417, 0, 0 },
				{ 24.138794, -37.097655, 46.631994, -24.272115 }, { 25.969838, -16.934118, 0, 0 },
				{ 29.38896, -21.40656, 0, 0 }, { 30.78765, -23.25415, 0, 0 }, { 25.413889, -17.697222, 0, 0 },
				{ 8.785412, -0.749529, 0, 0, }, { 6.5, 0.0, 0.0, 0.0 }, { 5.8, 0.0, 0.0, 0.0 }, };
		final double[][] vph = new double[][] { { 11.261692, 0.028794, -6.627846, 0 },
				{ 10.118851, 3.457774, -13.434875, 0 }, { 13.908244, -0.45417, 0, 0 },
				{ 24.138794, -37.097655, 46.631994, -24.272115 }, { 25.969838, -16.934118, 0, 0 },
				{ 29.38896, -21.40656, 0, 0 }, { 30.78765, -23.25415, 0, 0 }, { 25.413889, -17.697222, 0, 0 },
				{ 8.785412, -0.749529, 0, 0, }, { 6.5, 0, 0, 0 }, { 5.8, 0, 0, 0 } };
		final double[][] vsv = new double[][] { { 3.667865, -0.001345, -4.440915, 0 }, { 0, 0, 0, 0 },
				{ 8.018341, -1.349895, 0, 0 }, { 12.213901, -18.573085, 24.557329, -12.728015 },
				{ 20.208945, -15.895645, 0, 0 }, { 17.71732, -13.50652, 0, 0 }, { 15.212335, -11.053685, 0, 0 },
				{ 5.7502, -1.2742, 0, 0 }, { 5.970824, -1.499059, 0, 0 }, { 3.85, 0, 0, 0 }, { 3.46, 0, 0, 0 } };
		final double[][] vsh = new double[][] { { 3.667865, -0.001345, -4.440915, 0 }, { 0, 0, 0, 0 },
				{ 8.018341, -1.349895, 0, 0 }, { 12.213901, -18.573085, 24.557329, -12.728015 },
				{ 20.208945, -15.895645, 0, 0 }, { 17.71732, -13.50652, 0, 0 }, { 15.212335, -11.053685, 0, 0 },
				{ 5.7502, -1.2742, 0, 0 }, { 5.970824, -1.499059, 0, 0 }, { 3.85, 0, 0, 0 }, { 3.46, 0, 0, 0 } };
		final double[][] eta = new double[][] { { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 },
				{ 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 },
				{ 1, 0, 0, 0 }, }; // ok
		final double[] qMu = new double[] { 84.6, -1, 312, 312, 312, 143, 143, 80, 600, 600, 600, }; // ok
		final double[] qKappa = new double[] { 1327.7, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823, 57823,
				57823, }; // OK
		return set(nzone, rmin, rmax, rho, vpv, vph, vsv, vsh, eta, qMu, qKappa);
	}
	
	/**
	 * standard AK135 Kennett <i>et al<i>. (1995)
	 */
	private static PolynomialStructure initialMAK135() {
		PolynomialStructure mak135 = initialAK135();
		mak135 = mak135.addBoundaries(5661.0, 5761.0 , 5911.0, 6011.0);
		
		// smoothed 660 km discontinuity
		int i1 = mak135.getiZoneOf(5661+1);
		int i2 = mak135.getiZoneOf(5761-1);
		PolynomialFunction p = new PolynomialFunction(new double[] {38.96100749999991, -36.99958249999989});
		mak135 = mak135.setVsh(i1, p);
		mak135 = mak135.setVsh(i2, p);
		mak135 = mak135.setVsv(i1, p);
		mak135 = mak135.setVsv(i2, p);
		
		// smoothed 410 km discontinuity
		i1 = mak135.getiZoneOf(5911+1);
		i2 = mak135.getiZoneOf(6011-1);
		p = new PolynomialFunction(new double[] {28.99255250000017, -25.65920250000018});
		mak135 = mak135.setVsh(i1, p);
		mak135 = mak135.setVsh(i2, p);
		mak135 = mak135.setVsv(i1, p);
		mak135 = mak135.setVsv(i2, p);
		
		return mak135;
	}
	
	private static PolynomialStructure initialAK135_elastic() {
		PolynomialStructure structure = initialAK135();
		structure.qMu = new double[] {84.6, -1, 3000, 3000, 3000, 3000, 3000, 3000, 3000, 3000, 3000,};
		return structure;
	}
	
	private static PolynomialStructure initialMIASP91() {
		final int nzone = 12;
		final double[] rmin = new double[] {0, 1221.5, 3480, 3630, 5610, 5641, 5781, 5891, 5971, 6030.9, 6160, 6281};
		final double[] rmax = new double[] {1221.5, 3480, 3630, 5610, 5641, 5781, 5891, 5971, 6030.9, 6160, 6281, 6371};
		final double[][] rho = new double[][] { {13.0885, 0.0000, -8.8381, 0.}, {12.5815, -1.2638, -3.6426, -5.5281}
		, {7.2586, -3.1016, 0.0000, 0.0000}, {7.9469, -6.4376, 5.4773, -3.0584}, {7.8896, -3.9208, 0.0000, 0.0000}
		, {22.3146, -20.2128, 0.0000, 0.0000}, {14.743076955227242, -11.868712364945988, 0.0, 0.0}
		, {14.743076955227242, -11.868712364945988, 0.0, 0.0}, {14.743076955227242, -11.868712364945988, 0.0, 0.0}
		, {8.1973, -4.9538, 0.0000, 0.0000}, {6.1900, -2.8776, 0.0000, 0.0000}, {5.6768, -2.3570, 0.0000, 0.0000} };
		final double[][] vpv = new double[][] { {11.2622, 0.0000, -6.3640, 0.0000}, {11.0487, -4.0362, 4.8023, -13.5732}, {14.4729, -1.4327, 0.0000, 0.0000}, {25.0591, -40.7952, 51.5188, -26.4007}, {25.8698, -16.8211, 0.0000, 0.0000}, {51.6956, -45.9896, 0.0000, 0.0000}, {29.3890, -21.4066, 0.0000, 0.0000}, {44.0573, -37.2711, 0.0000, 0.0000}, {44.1294, -37.3480, 0.0000, 0.0000}, {30.7797, -23.2453, 0.0000, 0.0000}, {21.4868, -13.6332, 0.0000, 0.0000}, {8.5458, -0.5058, 0.0000, 0.0000} };
		final double[][] vph = new double[][] { {11.2622, 0.0000, -6.3640, 0.0000}, {11.0487, -4.0362, 4.8023, -13.5732}, {14.4729, -1.4327, 0.0000, 0.0000}, {25.0591, -40.7952, 51.5188, -26.4007}, {25.8698, -16.8211, 0.0000, 0.0000}, {51.6956, -45.9896, 0.0000, 0.0000}, {29.3890, -21.4066, 0.0000, 0.0000}, {44.0573, -37.2711, 0.0000, 0.0000}, {44.1294, -37.3480, 0.0000, 0.0000}, {30.7797, -23.2453, 0.0000, 0.0000}, {21.4868, -13.6332, 0.0000, 0.0000}, {8.5458, -0.5058, 0.0000, 0.0000} };
		final double[][] vsv = new double[][] { {3.6678, 0.0000, -4.4475, 0.0000}, {0.0000, 0.0000, 0.0000, 0.0000}, {8.14951, -1.5525, 0.0000, 0.0000}, {12.90771, -21.1679, 27.7784, -14.0554}, {20.53961, -16.2723, 0.0000, 0.0000}, {33.51471, -30.9268, 0.0000, 0.0000}, {17.70751, -13.5065, 0.0000, 0.0000}, {24.97041, -21.3617, 0.0000, 0.0000}, {25.00361, -21.3971, 0.0000, 0.0000}, {15.34491, -11.1936, 0.0000, 0.0000}, {6.21621, -1.7514, 0.0000, 0.0000}, {5.85131, -1.3812, 0.0000, 0.0000} };
		final double[][] vsh = new double[][] { {3.6678, 0.0000, -4.4475, 0.0000}, {0.0000, 0.0000, 0.0000, 0.0000}, {8.14951, -1.5525, 0.0000, 0.0000}, {12.90771, -21.1679, 27.7784, -14.0554}, {20.53961, -16.2723, 0.0000, 0.0000}, {33.51471, -30.9268, 0.0000, 0.0000}, {17.70751, -13.5065, 0.0000, 0.0000}, {24.97041, -21.3617, 0.0000, 0.0000}, {25.00361, -21.3971, 0.0000, 0.0000}, {15.34491, -11.1936, 0.0000, 0.0000}, {6.21621, -1.7514, 0.0000, 0.0000}, {5.85131, -1.3812, 0.0000, 0.0000} };
		final double[][] eta = new double[][] { {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000} };
		final double[] qMu = new double[] { 84.6, -1.0, 312.0, 312.0, 312.0, 312.0, 143.0, 143.0, 143.0, 143.0, 80.0, 600.0 };
		final double[] qKappa = new double[] { 1327.7, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0 };
		return set(nzone, rmin, rmax, rho, vpv, vph, vsv, vsh, eta, qMu, qKappa);
	}
	
	private static PolynomialStructure initialTBL50() {
		final int nzone = 17;
		final double[] rmin = new double[] { 0.0, 1221.5, 3480.0, 3500.0, 3524.0, 3590.0, 3630.0, 3698.7, 4371.0, 5610.0, 5641.0, 5781.0, 5891.0, 5971.0, 6030.9, 6160.0, 6281.0 };
		final double[] rmax = new double[] { 1221.5, 3480.0, 3500.0, 3524.0, 3590.0, 3630.0, 3698.7, 4371.0, 5610.0, 5641.0, 5781.0, 5891.0, 5971.0, 6030.9, 6160.0, 6281.0, 6371.0 };
		final double[][] rho = new double[][] { {13.0885, 0.0000, -8.8381, 0.0000}, {12.5815, -1.2638, -3.6426, -5.5281}, {7.2586, -3.1016, 0.0000, 0.0000}, {7.2586, -3.1016, 0.0000, 0.0000}, {7.2586, -3.1016, 0.0000, 0.0000}, {7.2586, -3.1016, 0.0000, 0.0000}, {7.9469, -6.4376, 5.4773, -3.0584}, {7.9469, -6.4376, 5.4773, -3.0584}, {7.9469, -6.4376, 5.4773, -3.0584}, {7.8896, -3.9208, 0.0000, 0.0000}, {22.3146, -20.2128, 0.0000, 0.0000}, {14.743076955227242, -11.868712364945988, 0.0, 0.0}, {14.743076955227242, -11.868712364945988, 0.0, 0.0}, {14.743076955227242, -11.868712364945988, 0.0, 0.0}, {8.1973, -4.9538, 0.0000, 0.0000}, {6.1900, -2.8776, 0.0000, 0.0000}, {5.6768, -2.3570, 0.0000, 0.0000} };
		final double[][] vpv = new double[][] { {11.2622, 0.0000, -6.3640, 0.0000}, {11.0487, -4.0362, 4.8023, -13.5732}, {14.4729, -1.4327, 0.0000, 0.0000}, {14.4729, -1.4327, 0.0000, 0.0000}, {14.4729, -1.4327, 0.0000, 0.0000}, {14.4729, -1.4327, 0.0000, 0.0000}, {25.0591, -40.7952, 51.5188, -26.4007}, {25.0591, -40.7952, 51.5188, -26.4007}, {25.0591, -40.7952, 51.5188, -26.4007}, {25.8698, -16.8211, 0.0000, 0.0000}, {51.6956, -45.9896, 0.0000, 0.0000}, {29.3890, -21.4066, 0.0000, 0.0000}, {44.0573, -37.2711, 0.0000, 0.0000}, {44.1294, -37.3480, 0.0000, 0.0000}, {30.7797, -23.2453, 0.0000, 0.0000}, {21.4868, -13.6332, 0.0000, 0.0000}, {8.5458, -0.5058, 0.0000, 0.0000} };
		final double[][] vph = new double[][] { {11.2622, 0.0000, -6.3640, 0.0000}, {11.0487, -4.0362, 4.8023, -13.5732}, {14.4729, -1.4327, 0.0000, 0.0000}, {14.4729, -1.4327, 0.0000, 0.0000}, {14.4729, -1.4327, 0.0000, 0.0000}, {14.4729, -1.4327, 0.0000, 0.0000}, {25.0591, -40.7952, 51.5188, -26.4007}, {25.0591, -40.7952, 51.5188, -26.4007}, {25.0591, -40.7952, 51.5188, -26.4007}, {25.8698, -16.8211, 0.0000, 0.0000}, {51.6956, -45.9896, 0.0000, 0.0000}, {29.3890, -21.4066, 0.0000, 0.0000}, {44.0573, -37.2711, 0.0000, 0.0000}, {44.1294, -37.3480, 0.0000, 0.0000}, {30.7797, -23.2453, 0.0000, 0.0000}, {21.4868, -13.6332, 0.0000, 0.0000}, {8.5458, -0.5058, 0.0000, 0.0000} };
		final double[][] vsv = new double[][] { {3.6678, 0.0000, -4.4475, 0.0000}, {0.0000, 0.0000, 0.0000, 0.0000}, {-385.453506521, 671.1002, 1377.2267, -2361.5226}, {-527.686706521, 926.1360, 1866.1564, -3238.6932}, {-18039.0875065, 96359.3215, -171492.1158, 101729.1953}, {-368.102806521, 1978.2925, -3461.3576, 2011.3330}, {-368.102806521, 1978.2925, -3461.3576, 2011.3330}, {10.2129934788, -9.8915, 11.4860, -5.9204}, {12.90771, -21.1679, 27.7784, -14.0554}, {20.53961, -16.2723, 0.0000, 0.0000}, {33.51471, -30.9268, 0.0000, 0.0000}, {17.70751, -13.5065, 0.0000, 0.0000}, {24.97041, -21.3617, 0.0000, 0.0000}, {25.00361, -21.3971, 0.0000, 0.0000}, {15.34491, -11.1936, 0.0000, 0.0000}, {6.21621, -1.7514, 0.0000, 0.0000}, {5.85131, -1.3812, 0.0000, 0.0000} };
		final double[][] vsh = new double[][] { {3.6678, 0.0000, -4.4475, 0.0000}, {0.0000, 0.0000, 0.0000, 0.0000}, {-385.453506521, 671.1002, 1377.2267, -2361.5226}, {-527.686706521, 926.1360, 1866.1564, -3238.6932}, {-18039.0875065, 96359.3215, -171492.1158, 101729.1953}, {-368.102806521, 1978.2925, -3461.3576, 2011.3330}, {-368.102806521, 1978.2925, -3461.3576, 2011.3330}, {10.2129934788, -9.8915, 11.4860, -5.9204}, {12.90771, -21.1679, 27.7784, -14.0554}, {20.53961, -16.2723, 0.0000, 0.0000}, {33.51471, -30.9268, 0.0000, 0.0000}, {17.70751, -13.5065, 0.0000, 0.0000}, {24.97041, -21.3617, 0.0000, 0.0000}, {25.00361, -21.3971, 0.0000, 0.0000}, {15.34491, -11.1936, 0.0000, 0.0000}, {6.21621, -1.7514, 0.0000, 0.0000}, {5.85131, -1.3812, 0.0000, 0.0000} };
		final double[][] eta = new double[][] { {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000}, {1.0000, 0.0000, 0.0000, 0.0000} };
		final double[] qMu = new double[] { 84.6,  -1.0,  312.0,  312.0,  312.0,  312.0,  312.0,  312.0,  312.0,  312.0,  312.0,  143.0,  143.0,  143.0,  143.0,  80.0,  600.0 };
		final double[] qKappa = new double[] { 1327.7,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0,  57823.0 };
		return set(nzone, rmin, rmax, rho, vpv, vph, vsv, vsh, eta, qMu, qKappa);
	}
	
	private static PolynomialStructure initialTNASNA() {
		final int nzone = 20;
		final double[] rmin = new double[] { 0.0, 1221.5, 3480.0, 3630.0, 3780.0, 5600.0, 5711.0, 5771.0, 5812.0, 5961.0, 6060.0, 6151.0, 6196.0, 6246.0, 6271.0, 6291.0, 6335.0, 6355.0, 6367.0, 6367.5 };
		final double[] rmax = new double[] { 1221.5, 3480.0, 3630.0, 3780.0, 5600.0, 5711.0, 5771.0, 5812.0, 5961.0, 6060.0, 6151.0, 6196.0, 6246.0, 6271.0, 6291.0, 6335.0, 6355.0, 6367.0, 6367.5, 6371.0 };
		final double[][] rho = new double[][] { {13.0885,0.0000,-8.8381,0.0000}, {12.5815,-1.2638,-3.6426,-5.5281}, {7.9565,-6.4761,5.5283,-3.0807}, {7.9565,-6.4761,5.5283,-3.0807}, {7.9565,-6.4761,5.5283,-3.0807}, {7.9565,-6.4761,5.5283,-3.0807}, {5.3197,-1.4836,0.0000,0.0000}, {11.2494,-8.0298,0.0000,0.0000}, {11.2494,-8.0298,0.0000,0.0000}, {7.1089,-3.8045,0.0000,0.0000}, {7.1089,-3.8045,0.0000,0.0000}, {2.6910,0.6924,0.0000,0.0000}, {2.6910,0.6924,0.0000,0.0000}, {2.6910,0.6924,0.0000,0.0000}, {2.6910,0.6924,0.0000,0.0000}, {2.6910,0.6924,0.0000,0.0000}, {2.9000,0.0000,0.0000,0.0000}, {2.6000,0.0000,0.0000,0.0000}, {2.6000,0.0000,0.0000,0.0000}, {2.6000,0.0000,0.0000,0.0000} };
		final double[][] vpv = new double[][] { {11.2622,0.0000,-6.3640,0.0000}, {11.0487,-4.0362,4.8023,-13.5732}, {15.3891,-5.3181,5.5242,-2.5514}, {24.9520,-40.4673,51.4832,-26.6419}, {24.9520,-40.4673,51.4832,-26.6419}, {29.2766,-23.6027,5.5242,-2.5514}, {19.0957,-9.8672,0.0000,0.0000}, {39.7027,-32.6166,0.0000,0.0000}, {39.7027,-32.6166,0.0000,0.0000}, {20.3926,-12.2569,0.0000,0.0000}, {20.3926,-12.2569,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {6.8000,0.0000,0.0000,0.0000}, {5.8000,0.0000,0.0000,0.0000}, {5.8000,0.0000,0.0000,0.0000}, {5.8000,0.0000,0.0000,0.0000} };
		final double[][] vph = new double[][] { {11.2622,0.0000,-6.3640,0.0000}, {11.0487,-4.0362,4.8023,-13.5732}, {15.3891,-5.3181,5.5242,-2.5514}, {24.9520,-40.4673,51.4832,-26.6419}, {24.9520,-40.4673,51.4832,-26.6419}, {29.2766,-23.6027,5.5242,-2.5514}, {19.0957,-9.8672,0.0000,0.0000}, {39.7027,-32.6166,0.0000,0.0000}, {39.7027,-32.6166,0.0000,0.0000}, {20.3926,-12.2569,0.0000,0.0000}, {20.3926,-12.2569,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {4.1875,3.9382,0.0000,0.0000}, {6.8000,0.0000,0.0000,0.0000}, {5.8000,0.0000,0.0000,0.0000}, {5.8000,0.0000,0.0000,0.0000}, {5.8000,0.0000,0.0000,0.0000} };
		final double[][] vsv = new double[][] { {3.6678,0.0000,-4.4475,0.0000}, {0.0000,0.0000,0.0000,0.0000}, {6.9254,1.4672,-2.0834,0.9783}, {11.1671,-13.7818,17.4575,-9.2777}, {11.1671,-13.7818,17.4575,-9.2777}, {22.3459,-17.2473,-2.0834,0.9783}, {54925.2903,-181312.2745,199540.1131,-73204.7210}, {54925.2903,-181312.2745,199540.1131,-73204.7210}, {-3845.9219,12588.8278,-13704.0781,4968.0316}, {29002.6283,-92143.8966,97611.2197,-34472.0067}, {8540.5921,-26047.1895,26484.2029,-8972.8940}, {8540.5921,-26047.1895,26484.2029,-8972.8940}, {4.5000,0.0000,0.0000,0.0000}, {-20.4840,25.4840,0.0000,0.0000}, {4.6000,0.0000,0.0000,0.0000}, {4.6000,0.0000,0.0000,0.0000}, {22.7550,-19.1130,0.0000,0.0000}, {24.8733,-21.2367,0.0000,0.0000}, {3.6500,0.0000,0.0000,0.0000}, {3.2000,0.0000,0.0000,0.0000} };
		final double[][] vsh = new double[][] { {3.6678,0.0000,-4.4475,0.0000}, {0.0000,0.0000,0.0000,0.0000}, {6.9254,1.4672,-2.0834,0.9783}, {11.1671,-13.7818,17.4575,-9.2777}, {11.1671,-13.7818,17.4575,-9.2777}, {22.3459,-17.2473,-2.0834,0.9783}, {54925.2903,-181312.2745,199540.1131,-73204.7210}, {54925.2903,-181312.2745,199540.1131,-73204.7210}, {-3845.9219,12588.8278,-13704.0781,4968.0316}, {29002.6283,-92143.8966,97611.2197,-34472.0067}, {8540.5921,-26047.1895,26484.2029,-8972.8940}, {8540.5921,-26047.1895,26484.2029,-8972.8940}, {4.5000,0.0000,0.0000,0.0000}, {-20.4840,25.4840,0.0000,0.0000}, {4.6000,0.0000,0.0000,0.0000}, {4.6000,0.0000,0.0000,0.0000}, {22.7550,-19.1130,0.0000,0.0000}, {24.8733,-21.2367,0.0000,0.0000}, {3.6500,0.0000,0.0000,0.0000}, {3.2000,0.0000,0.0000,0.0000} };
		final double[][] eta = new double[][] { {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000}, {1.0000,0.0000,0.0000,0.0000} };
		final double[] qMu = new double[] { 84.6, -1.0, 312.0, 312.0, 312.0, 312.0, 143.0, 143.0, 143.0, 143.0, 143.0, 80.0, 80.0, 80.0, 80.0, 600.0, 600.0, 600.0, 600.0, 600.0 };
		final double[] qKappa = new double[] { 1327.7, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0, 57823.0 };
		return set(nzone, rmin, rmax, rho, vpv, vph, vsv, vsh, eta, qMu, qKappa);
	}

	/**
	 * structureLines must not have comment lines and must have only structure
	 * lines. structureLines must have been trimmed.
	 * 
	 * @param structureLines
	 *            lines for a structure
	 */
	private void readLines(String[] structureLines) {
		nzone = Integer.parseInt(structureLines[0].split("\\s+")[0]);
		if (structureLines.length != (nzone * 6 + 1))
			throw new IllegalArgumentException("Invalid lines");
		initialize();
		for (int i = 0; i < nzone; i++) {
			String[] rangeRhoParts = structureLines[i * 6 + 1].split("\\s+");
			String[] vpvParts = structureLines[i * 6 + 2].split("\\s+");
			String[] vphParts = structureLines[i * 6 + 3].split("\\s+");
			String[] vsvParts = structureLines[i * 6 + 4].split("\\s+");
			String[] vshParts = structureLines[i * 6 + 5].split("\\s+");
			String[] etaParts = structureLines[i * 6 + 6].split("\\s+");
			rmin[i] = Double.parseDouble(rangeRhoParts[0]);
			rmax[i] = Double.parseDouble(rangeRhoParts[1]);
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
			qMu[i] = Double.parseDouble(etaParts[4]);
			qKappa[i] = Double.parseDouble(etaParts[5]);
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
		int zone = nzone - coreZone;
		String[] outString = new String[3 * zone + 4];
		outString[0] = zone + " nzone";
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
	 * @param pf
	 *            polynomial function for a layer
	 * @return string in a form of this
	 */
	private static String toLine(PolynomialFunction pf) {
		return Arrays.stream(Arrays.copyOf(pf.getCoefficients(), 4)).mapToObj(Double::toString)
				.collect(Collectors.joining(" "));
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
	
	public void writeVelocity(Path outPath) throws IOException {
		Files.deleteIfExists(outPath);
		Files.createFile(outPath);
		for (int i = 0; i < 10000; i++) {
			double r = i * 6371./10000.;
			double vsh = getVshAt(r);
			double vsv = getVsvAt(r);
			double vph = getVphAt(r);
			double vpv = getVpvAt(r);
			Files.write(outPath, (r + " " + vpv + " " + vph + " " + vsv + " " + vsh + " " + "\n").getBytes(), StandardOpenOption.APPEND);
		}
	}
	
	/**
	 * @param izone
	 *            index of a zone
	 * @return polynomial function for V<sub>PV</sub> of the zone
	 */
	public PolynomialFunction getVpvOf(int izone) {
		return vpv[izone];
	}

	/**
	 * @param izone
	 *            index of a zone
	 * @return polynomial function for V<sub>PH</sub> of the zone
	 */
	public PolynomialFunction getVphOf(int izone) {
		return vph[izone];
	}

	/**
	 * @param izone
	 *            index of a zone
	 * @return polynomial function for V<sub>SV</sub> of the zone
	 */
	public PolynomialFunction getVsvOf(int izone) {
		return vsv[izone];
	}

	/**
	 * @param izone
	 *            index of a zone
	 * @return polynomial function for V<sub>SH</sub> of the zone
	 */
	public PolynomialFunction getVshOf(int izone) {
		return vsh[izone];
	}
}
