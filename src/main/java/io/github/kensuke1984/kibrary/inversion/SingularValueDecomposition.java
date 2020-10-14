package io.github.kensuke1984.kibrary.inversion;

import org.apache.commons.math3.linear.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SVD inversion
 *
 * @author Kensuke Konishi
 * @version 0.0.7.4
 * @see <a href=
 * https://ja.wikipedia.org/wiki/%E7%89%B9%E7%95%B0%E5%80%A4%E5%88%86%E8%A7%A3>Japanese
 * wiki</a> <a
 * href=https://en.wikipedia.org/wiki/Singular_value_decomposition>English
 * wiki</a>
 */
public class SingularValueDecomposition extends InverseProblem {

	private org.apache.commons.math3.linear.SingularValueDecomposition svdi;
	
	public SingularValueDecomposition(RealMatrix ata, RealVector atd) {
		this.ata = ata;
		this.atd = atd;
		ans = MatrixUtils.createRealMatrix(ata.getColumnDimension(), ata.getColumnDimension());
	}

	/**
	 * Output Vt
	 *
	 * @param outDir must not exist
	 * @throws IOException if any
	 */
	public void ouputVt(Path outDir) throws IOException {
		if (Files.exists(outDir))
			return;
		Files.createDirectories(outDir);
		RealMatrix vt = svdi.getVT();
		new Thread(() -> {
			for (int i = 0; i < getParN(); i++) {
				Path out = outDir.resolve(i + ".dat");
				RealVector v = vt.getColumnVector(i);
				try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
					for (int j = 0; j < getParN(); j++)
						pw.println(v.getEntry(j));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public RealMatrix computeCovariance(double sigmaD, int j) {
		RealMatrix covarianceMatrix = new Array2DRowRealMatrix(getParN(), getParN());
		double[] lambda = svdi.getSingularValues();
		double sigmaD2 = sigmaD * sigmaD;
		for (int i = 0; i < j; i++) {
			double sigmaD2lambda2 = sigmaD2 / lambda[i];
			RealMatrix v = MatrixUtils.createColumnRealMatrix(svdi.getV().getColumn(i));
			covarianceMatrix = covarianceMatrix.add(v.multiply(v.transpose()).scalarMultiply(sigmaD2lambda2));
		}
		return covarianceMatrix;
	}

	

	@Override
	public void compute() {
		System.err.print("singular value decomposing AtA");
		svdi = new org.apache.commons.math3.linear.SingularValueDecomposition(ata);
		System.err.println("  done");
		RealMatrix vt = svdi.getVT();
		
		// BtB = VtAtAV VtStSV
		RealMatrix btb = vt.multiply(ata).multiply(vt.transpose());
		// sometime btb is too small to be LUdecomposed
		final double factor = 1 / ata.getEntry(0, ata.getColumnDimension() - 1);
		btb = btb.scalarMultiply(factor);

		// Btd = VtAtd
		RealVector btd = vt.operate(atd);

		// m = Vp
		RealVector p = new LUDecomposition(btb).getSolver().getInverse().operate(btd);
		p.mapMultiplyToSelf(factor);

		int parN = getParN();

		// mj = pi vi (i<=j, V=(vi ...)
		for (int j = 0; j < parN; j++) {
			RealVector ans = new ArrayRealVector(parN);
			for (int i = 0; i < j + 1; i++) {
				RealVector vi = vt.getRowVector(i);
				ans = ans.add(vi.mapMultiply(p.getEntry(i)));
			}
			this.ans.setColumnVector(j, ans);
		}
		
		// output singular values
		Path outpath = Paths.get("singularValues.txt");
		try {
			Files.createFile(outpath);
			PrintWriter pw = new PrintWriter(outpath.toFile());
			for (double lambda : svdi.getSingularValues())
				pw.println(lambda);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public org.apache.commons.math3.linear.SingularValueDecomposition getSVDI() {
		return svdi;
	}

	public RealMatrix computeCovariance() {
		return new LUDecomposition(ata).getSolver().getInverse(); // TODO
	}


	@Override
	public RealMatrix getBaseVectors() {
		return svdi.getVT();
	}

	@Override
	InverseMethodEnum getEnum() {
		return InverseMethodEnum.SINGULAR_VALUE_DECOMPOSITION;
	}

}
