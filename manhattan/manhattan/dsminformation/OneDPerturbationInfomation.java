package manhattan.dsminformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * 
 * tipsv1d, tish1dに代入する摂動のinformation file
 * 
 * @since 2013/6/17
 * 
 * @version 0.0.2
 * @since 2013/9/25
 * 
 * @version 0.0.3
 * @since 2014/9/5 to Java 8
 * 
 * @author kensuke
 * 
 * 
 * 
 * 
 */
class OneDPerturbationInfomation {

	private int nzone;
	private int[] ngrid;
	private double[] rmin, rmax;
	private double[][] radius, rho, a, c, f, l, n;

	/**
	 * tipsv1d, tish1dにいれる摂動情報
	 * 
	 * @param nzone
	 *            いくつの部分を切り取るか
	 * @param ngrid
	 *            切り取った中の近似点
	 * @param rmin
	 *            それぞれの近似点の半径
	 * @param rmax
	 *
	 * 
	 */
	public OneDPerturbationInfomation(int nzone, int[] ngrid, double[] rmin, double[] rmax) {
		super();
		boolean valid = true;
		if (nzone < 1) {
			System.out.println("nzone " + nzone + " invalid");
			valid = false;
		} else {
			if (nzone != ngrid.length) {
				System.out.println("nzone " + nzone + " and the number of ngrid " + ngrid.length + " are different");
				valid = false;
			}
			if (nzone != rmin.length) {
				System.out.println("nzone " + nzone + " and the number of rmin " + rmin + " are different");
				valid = false;
			}
			if (nzone != rmax.length) {
				System.out.println("nzone " + nzone + " and the number of rmax " + rmax + " are different");
				valid = false;
			}
		}
		if (!valid)
			return;

		this.nzone = nzone;
		this.ngrid = ngrid;
		this.rmin = rmin;
		this.rmax = rmax;
		radius = new double[nzone][];
		rho = new double[nzone][];
		a = new double[nzone][];
		c = new double[nzone][];
		f = new double[nzone][];
		l = new double[nzone][];
		n = new double[nzone][];

		for (int i = 0; i < nzone; i++) {
			radius[i] = new double[ngrid[i]];
			for (int j = 0; j < ngrid[i]; j++) {
				radius[i][j] = rmin[i] + (rmax[i] - rmin[i]) / (ngrid[i] + 1) * (j + 1);
			}
			rho[i] = new double[ngrid[i]];
			a[i] = new double[ngrid[i]];
			c[i] = new double[ngrid[i]];
			f[i] = new double[ngrid[i]];
			l[i] = new double[ngrid[i]];
			n[i] = new double[ngrid[i]];
		}

	}

	/**
	 * @param outFile
	 *            Path of an output file
	 * @param options
	 *            open options
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outputPSV(Path outFile, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outFile, options))) {
			pw.println("c nzone");
			pw.println(nzone);
			for (int i = 0; i < nzone; i++) {
				pw.println("c ngrid rmin rmax");
				pw.println(ngrid[i] + " " + rmin[i] + " " + rmax[i]);
				pw.println("c radius(km), rho(g/cm^3),A,C,F,L,N(GPa)");
				for (int j = 0; j < ngrid[i]; j++)
					pw.println(radius[i][j] + " " + rho[i][j] + " " + a[i][j] + " " + c[i][j] + " " + f[i][j] + " "
							+ l[i][j] + " " + n[i][j]);
				pw.println("c");
			}
			pw.println("end");

		}
	}

	/**
	 * @param outFile
	 *            Path of an output file
	 * @param options
	 *            open options
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void outputSH(Path outFile, OpenOption... options)throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outFile, options))) {
			pw.println("c nzone");
			pw.println(nzone);
			for (int i = 0; i < nzone; i++) {
				pw.println("c ngrid rmin rmax");
				pw.println(ngrid[i] + " " + rmin[i] + " " + rmax[i]);
				pw.println("c radius(km), rho(g/cm^3), L, N(GPa)");
				for (int j = 0; j < ngrid[i]; j++)
					pw.println(radius[i][j] + " " + rho[i][j] + " " + l[i][j] + " " + n[i][j]);
				pw.println("c");
			}
			pw.println("end");

		}

	}

}
