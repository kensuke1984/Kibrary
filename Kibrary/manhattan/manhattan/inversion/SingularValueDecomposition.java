package manhattan.inversion;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * SVD inversion
 * 
 * 
 * @version 0.0.2
 * @since 2013/10/18
 * 
 * @version 0.0.3
 * @since 2014/9/8 to Java 8
 * 
 * @version 0.0.5
 * @since 2014/11/20 for very small A
 * 
 * @version 0.0.6
 * @since 2015/8/28
 * 
 * @version 0.0.7
 * @since 2015/9/15 MatrixUtils Path
 * 
 * 
 * @author Kensuke
 *
 */
class SingularValueDecomposition extends InverseProblem {

	/**
	 * Output Vt
	 * 
	 * @param outDir
	 *            must not exist
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
		for (int i = 0; i < j + 1; i++) {
			// double sigmaD2lambda2 = sigmaD2/lambda[i]/lambda[i];
			double sigmaD2lambda2 = sigmaD2 / lambda[i];
			RealMatrix v = MatrixUtils.createColumnRealMatrix(svdi.getV().getColumn(i));
			covarianceMatrix = covarianceMatrix.add(v.multiply(v.transpose()).scalarMultiply(sigmaD2lambda2));
		}
		return covarianceMatrix;
	}

	private org.apache.commons.math3.linear.SingularValueDecomposition svdi;

	@Override
	public void compute() {
		System.out.print("singular value decomposing AtA");
		svdi = new org.apache.commons.math3.linear.SingularValueDecomposition(ata);
		// System.exit(0);
		System.out.println("  done");
		RealMatrix vt = svdi.getVT();

		// ////
		// BtB = VtAtAV VtStSV
		RealMatrix btb = vt.multiply(ata).multiply(vt.transpose());
		// sometime btb is too small to be LUdecomposed
		double factor = 1 / ata.getEntry(0, ata.getColumnDimension() - 1);
		btb = btb.scalarMultiply(factor);

		// Btd = VtAtd
		RealVector btd = vt.operate(atd);

		// m = Vp
		RealVector p = new LUDecomposition(btb).getSolver().getInverse().operate(btd);
		p = p.mapMultiply(factor);

		int parN = getParN();

		// mj = pi vi (i<=j, V=(vi ...)
		for (int j = 0; j < parN; j++) {
			RealVector ans = new ArrayRealVector(parN);
			for (int i = 0; i < j + 1; i++) {
				RealVector vi = vt.getRowVector(i);
				ans = ans.add(vi.mapMultiply(p.getEntry(i)));
			}
			super.ans.setColumnVector(j, ans);
		}

	}

	public org.apache.commons.math3.linear.SingularValueDecomposition getSVDI() {
		return svdi;
	}

	public RealMatrix computeCovariance() {
		return new LUDecomposition(ata).getSolver().getInverse(); // TODO
		// return
		// svdi.getV().multiply((svdi.getS().transpose().multiply(svdi.getS())).inverse()).multiply(svdi.getVT());
	}

	public SingularValueDecomposition(RealMatrix ata, RealVector atd) {
		this.ata = ata;
		this.atd = atd;
		ans = MatrixUtils.createRealMatrix(ata.getColumnDimension(), ata.getColumnDimension());
		// compute();
	}

	@Override
	public RealMatrix getBaseVectors() {

		return svdi.getVT();
	}

	@Override
	InverseMethodEnum getEnum() {
		return InverseMethodEnum.SVD;
	}

}
