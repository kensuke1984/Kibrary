package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;

import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

public class LookAtSAC {

	public static void main(String[] args) {
		SACFileName sacname = new SACFileName(args[0]);
		try {
			SACData sacdata = sacname.read();
			double[] data = sacdata.getData();
			double t0 = sacdata.getValue(SACHeaderEnum.B);
			double delta = sacdata.getValue(SACHeaderEnum.DELTA);
			int npts = sacdata.getInt(SACHeaderEnum.NPTS);
			for (int i = 0; i < npts; i++) {
				double t = t0 + i * delta;
				System.out.format("%.4f %.4e%n", t, data[i]);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}

	}

}
