package io.github.kensuke1984.anisotime;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.SortedSet;
import java.util.TreeSet;

import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * 
 * Raypath catalogue for one model
 * 
 * 
 * @author Kensuke Konishi
 * @version 0.0.2b
 */
class RaypathCatalogue implements Serializable {


	/**
	 * 2016/5/23
	 */
	private static final long serialVersionUID = 8730045312450734842L;

	/**
	 * Woodhouse formula with certain velocity structure
	 */
	private final Woodhouse1981 WOODHOUSE;

	/**
	 * This value is in [rad].
	 * <p>
	 * We compute epicentral distances &Delta;<sup>(P)</sup><sub>i</sub> (P or
	 * PcP') and &Delta;<sup>(S)</sup><sub>i</sub> (S or ScS) for ray parameters
	 * p<sub>i</sub> (p<sub>i</sub> &lt; p<sub>i+1</sub>) for a catalogue. If
	 * &delta;&Delta;<sub>i</sub> (|&Delta;<sub>i</sub> - &Delta;<sub>i</sub>|)
	 * &lt; this value, both p<sub>i</sub> and p<sub>i+1</sub> are stored,
	 * otherwise either only one of them is stored.
	 */
	private final double D_DELTA;

	/**
	 * Standard &delta;P. In case the &delta;P is too big, another value is
	 * used.
	 */
	private final double DELTA_P = 5;

	/**
	 * Minimum value of &delta;P. Even if similar raypath is not found within
	 * this value, a catalogue does not have denser rayparameter.
	 */
	private final double MINIMUM_DELTA_P = 0.1;

	/**
	 * List of stored raypaths. Ordered by rayparameter p.
	 */
	private final SortedSet<Raypath> raypathList = new TreeSet<>();

	/**
	 * Mesh for computation
	 */
	private final ComputationalMesh MESH;

	public static RaypathCatalogue computeCatalogue(VelocityStructure structure, ComputationalMesh mesh,
			double dDelta) {
		RaypathCatalogue cat = new RaypathCatalogue();
		cat.create();
		return cat;
	}

	private RaypathCatalogue(VelocityStructure structure, ComputationalMesh mesh, double dDelta) {
		WOODHOUSE = new Woodhouse1981(structure);
		D_DELTA = dDelta;
		MESH = mesh;
	}

	private RaypathCatalogue() {
		this(VelocityStructure.prem(), ComputationalMesh.simple(), Math.toRadians(1));
	}

	public static void main(String[] args) throws Exception {
		// 605.5 605.5 26.78959947466123
		RaypathCatalogue r = new RaypathCatalogue();
		r.test();
	}

	private void test() throws IOException, ClassNotFoundException {
		 create();
//		 write(Paths.get("/tmp/ray0.tmp"));
//		readtest();
	}

	private static void readtest() throws ClassNotFoundException, IOException {
		RaypathCatalogue r = RaypathCatalogue.read(Paths.get("/tmp/ray0.tmp"));
		Raypath ray = r.raypathList.first();
		System.out.println(ray.computeT(5371, Phase.ScS));
		r.raypathList.forEach(ra -> {
			if(!ra.exists(6371, Phase.ScS))
				return;
			double delta = Math.toDegrees(ra.computeDelta(6371, Phase.ScS));
			double time = ra.computeT(6371, Phase.ScS);
			System.out.println(delta+" "+time);
		});
	}

	/**
	 * Rayparameter for Pdiff
	 */
	private double p_Pdiff;

	/**
	 * Rayparameter for SVdiff
	 */
	private double p_SVdiff;

	/**
	 * Rayparameter for SHdiff
	 */
	private double p_SHdiff;

	/**
	 * Computes rayparameters of diffraction phases.
	 */
	private void computeDiffraction() {
		VelocityStructure structure = WOODHOUSE.getStructure();
		double cmb = structure.coreMantleBoundary() + ComputationalMesh.eps;
		double rho = structure.getRho(cmb);
		p_Pdiff = cmb * Math.sqrt(rho / structure.getA(cmb));
		p_SVdiff = cmb * Math.sqrt(rho / structure.getL(cmb));
		p_SHdiff = cmb * Math.sqrt(rho / structure.getN(cmb));
		System.out.println(p_Pdiff + " " + p_SVdiff + " " + p_SHdiff);
	}

	// TODO
	/**
	 * Creates a catalogue.
	 */
	private void create() {
		double pMax = computeRayparameterLimit();
		System.out.println("pMax=" + pMax);
		// Compute raparameters for diffration phases.
		computeDiffraction();
		long time = System.nanoTime();
		// pMax = 200;
		Raypath firstPath = new Raypath(0, WOODHOUSE, MESH );
		firstPath.compute();
		raypathList.add(firstPath);
		System.out.println("Computing a catalogue");

		for (double p = firstPath.getRayParameter() + DELTA_P, nextP = p + DELTA_P; p < pMax; p = nextP) {
			Raypath rp = new Raypath(p, WOODHOUSE, MESH );
			rp.compute();
			System.out.println(p);
			if (closeEnough(raypathList.last(), rp)) {
				raypathList.add(rp);
				nextP = p + DELTA_P;
			} else {
				raypathPool.add(rp);
				nextP = (p + raypathList.last().getRayParameter()) / 2;
				// System.out.println(p+" "+nextP);
			}

			// System.out.println(p+" 1-> "+nextP);
			if (lookIntoPool())
				nextP = raypathList.last().getRayParameter() + DELTA_P;
			// System.out.println(p+" 2-> "+nextP);
			// System.out.println(nextP);
			// 253.70677502466899 479.03198571892705 479.03198571892705
			if ((p < p_Pdiff && p_Pdiff < nextP) || (p < p_SVdiff && p_SVdiff < nextP)
					|| (p < p_SHdiff && p_SHdiff < nextP)) {
				Raypath diffPath = new Raypath(nextP,WOODHOUSE, MESH );
				diffPath.compute();
				raypathList.add(diffPath);
				nextP += DELTA_P;
			}
		}
		Raypath pDiffPath = new Raypath(p_Pdiff, WOODHOUSE, MESH );
		pDiffPath.compute();
		Raypath svDiffPath = new Raypath(p_SVdiff, WOODHOUSE, MESH );
		svDiffPath.compute();
		Raypath shDiffPath = new Raypath(p_SHdiff, WOODHOUSE, MESH );
		shDiffPath.compute();
		raypathList.add(pDiffPath);
		raypathList.add(svDiffPath);
		raypathList.add(shDiffPath);
		System.out.println(Utilities.toTimeString(System.nanoTime() - time));

	}

	private final transient SortedSet<Raypath> raypathPool = new TreeSet<>();

	/**
	 * Look for a raypath to be a next one for the {@link #raypathList} If one
	 * is found and another is also found for the next next one, all are added
	 * recursively.
	 * 
	 * @return If any good raypath in the pool
	 */
	private boolean lookIntoPool() {
		boolean added = false;
		for (Raypath raypath : raypathPool)
			if (closeEnough(raypathList.last(), raypath)) {
				raypathList.add(raypath);
				added = true;
			}
		return added;
	}

	/**
	 * Criterion for the catalogue is {@link #D_DELTA} so far in both P and S
	 * wave. The rayparameter of raypath1 must be smaller than that of raypath2,
	 * otherwise, false is returned. TODO SH SV??
	 * 
	 * @param raypath1
	 *            to be checked
	 * @param raypath2
	 *            to be checked
	 * @return If they are similar path.
	 */
	private boolean closeEnough(Raypath raypath1, Raypath raypath2) {
		if (raypath2.getRayParameter() <= raypath1.getRayParameter())
			return false;
		if (raypath2.getRayParameter() - raypath1.getRayParameter() < MINIMUM_DELTA_P) {
			System.out.println("close");
			return true;
		}
		VelocityStructure structure = WOODHOUSE.getStructure();
		double p1 = raypath1.computeDelta(structure.earthRadius(), Phase.P);
		double p2 = raypath2.computeDelta(structure.earthRadius(), Phase.P);
		if (Double.isNaN(p1) ^ Double.isNaN(p2))
			return false;
		if (Double.isNaN(p1)) {
			p1 = raypath1.computeDelta(structure.earthRadius(), Phase.PcP);
			p2 = raypath2.computeDelta(structure.earthRadius(), Phase.PcP);
		}
		if (D_DELTA < Math.abs(p1 - p2))
			return false;

		double s1 = raypath1.computeDelta(structure.earthRadius(), Phase.S);
		double s2 = raypath2.computeDelta(structure.earthRadius(), Phase.S);
		if (Double.isNaN(s1) ^ Double.isNaN(s2))
			return false;
		if (Double.isNaN(s1)) {
			s1 = raypath1.computeDelta(structure.earthRadius(), Phase.ScS);
			s2 = raypath2.computeDelta(structure.earthRadius(), Phase.ScS);
		}
		if (D_DELTA < Math.abs(s1 - s2))
			return false;
		return true;
	}

	/**
	 * Computes ray parmameter p with which q<sub>&tau;</sub> =0 at the earth
	 * surface for P, SV and SH. Returns the maximum value of them. (basically
	 * the one of S)
	 * <p>
	 * P &rarr; r*(&rho;/A)<sup>1/2</sup> <br>
	 * SV &rarr; r*(&rho;/L)<sup>1/2</sup> <br>
	 * SH &rarr; r*(&rho;/N)<sup>1/2</sup> <br>
	 * 
	 * @return maximum ray parameter
	 * @see {@code Woodhouse (1981)}
	 */
	private double computeRayparameterLimit() {
		VelocityStructure structure = WOODHOUSE.getStructure();
		double r = structure.earthRadius();
		double rho = structure.getRho(r);
		double p = r * Math.sqrt(rho / structure.getA(r));
		double sv = r * Math.sqrt(rho / structure.getL(r));
		double sh = r * Math.sqrt(rho / structure.getN(r));
		return Math.max(Math.max(p, sv), sh);
	}

	public static RaypathCatalogue read(Path path, OpenOption... options) throws IOException, ClassNotFoundException {
		try (ObjectInputStream oi = new ObjectInputStream(Files.newInputStream(path, options))) {
			return (RaypathCatalogue) oi.readObject();
		}
	}

	/**
	 * @param path
	 *            the path to the output file
	 * @throws IOException
	 *             If an I/O error happens. it throws error.
	 */
	public void write(Path path, OpenOption... options) throws IOException {
		try (ObjectOutputStream o = new ObjectOutputStream(Files.newOutputStream(path, options))) {
			o.writeObject(this);
		}
	}

}
