package io.github.kensuke1984.kibrary.inversion;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * @author Kensuke Konishi
 * @version 0.0.4
 */
public abstract class InverseProblem {

    RealMatrix ans;
    RealMatrix ata;
    RealVector atd;

    private static void writeDat(Path out, double[] dat) throws IOException {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
            Arrays.stream(dat).forEach(pw::println);
        }
    }
	
	public void setANS(int i, RealVector v) {
		ans.setColumnVector(i-1, v);
	}
	
    public RealMatrix getANS() {
        return ans;
    }

    abstract InverseMethodEnum getEnum();

    /**
     * @param i index (1, 2, ...)
     * @return i th answer
     */
    public RealVector getAns(int i) {
        if (i <= 0) throw new IllegalArgumentException("i must be a natural number.");
        return ans.getColumnVector(i - 1);
    }

	/**
	 * @return the number of unknown parameters
	 */
	public int getParN() {
		if (ata != null)
			return ata.getColumnDimension();
		else
			return atd.getDimension();
	}

    /**
     * @param sigmaD &sigma;<sub>d</sub>
     * @param j      index (1, 2, ...)
     * @return j番目の解の共分散行列 &sigma;<sub>d</sub> <sup>2</sup> V (&Lambda;
     * <sup>T</sup>&Lambda;) <sup>-1</sup> V<sup>T</sup>
     */
    public abstract RealMatrix computeCovariance(double sigmaD, int j);

	/**
	 * output the answer
	 * @param outPath {@link File} for write of solutions
	 * @throws IOException if an I/O error occurs
	 */
	public void outputAns(Path outPath) throws IOException {
		Files.createDirectories(outPath);
		System.err.println("outputting the answer files in " + outPath);
		for (int i = 0; i < getParN(); i++) {
			Path out = outPath.resolve(getEnum().simple() + (i+1) + ".txt");
			double[] m = ans.getColumn(i);
			writeDat(out, m);
		}
	}

	/**
	 * output the answer
	 * @param outPath {@link File} for write of solutions
	 * @param parameterWeights
	 * @throws IOException if an I/O error occurs
	 */
	public void outputAns(Path outPath, double[] parameterWeights) throws IOException {
		Files.createDirectories(outPath);
		System.err.println("outputting the answer files in " + outPath);
		for (int i = 0; i < getParN(); i++) {
			Path out = outPath.resolve(getEnum().simple() + (i+1) + ".txt");
			double[] m = ans.getColumn(i);
			for (int j = 0; j < m.length; j++)
				m[j] *= parameterWeights[j];
			writeDat(out, m);
		}
	}
	
	/**
	 * @param outPath
	 * @throws IOException
	 * @author anselme
	 */
	public void outputAnsX(Path outPath) throws IOException {
		Files.createDirectories(outPath);
		System.err.println("outputting the answer files in " + outPath);
		for (int i = 0; i < getParN(); i++) {
			Path out = outPath.resolve(getEnum().simple() + "_x" + (i+1) + ".txt");
			double[] m = ans.getColumn(i);
			writeDat(out, m);
		}
	}

	public abstract void compute();

    /**
     * @return RealMatrix in which the i-th column is i-th basis vector.
     */
    public abstract RealMatrix getBaseVectors();

}
