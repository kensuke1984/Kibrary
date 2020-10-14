package io.github.kensuke1984.kibrary.math;

public class Interpolation {
	
	 public static double threePointInterpolation(double x, double[] xi, double[] yi) {
	    	double[] h = new double[3];
	    	for (int i = 0; i < 3; i++)
	    		h[i] = x - xi[i];
	    	double h01 = xi[1] - xi[0];
	    	double h02 = xi[2] - xi[0];
	    	double h12 = xi[2] - xi[1];
	    	
			double phi0 = h[1] * h[2] / (h01 * h02); 
			double phi1 = -h[0] * h[2] / (h01 * h12);
			double phi2 = h[0] * h[1] / (h02 * h12);
			
			return yi[0] * phi0 + yi[1] * phi1 + yi[2] * phi2;
		}
	 
	 public static double linear(double x, double[] xi, double[] yi) {
		 return yi[0] + (yi[1] - yi[0]) * (x - xi[0]) / (xi[1] - xi[0]);
	 }
}
