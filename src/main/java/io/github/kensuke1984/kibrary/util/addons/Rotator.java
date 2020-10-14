package io.github.kensuke1984.kibrary.util.addons;

import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.util.Trace;

public class Rotator {
	private Trace trace1;
	private Trace trace2;
	
	public Rotator(Trace trace1, Trace trace2) {
		this.trace1 = trace1;
		this.trace2 = trace2;
	}
	
	public Trace rotate(double thetaDeg) {
		double[] xs = trace1.getX();
		
		double thetaRad = Math.toRadians(thetaDeg);
		
		RealVector yvec = trace1.getYVector().mapMultiply(Math.cos(thetaRad)).add(trace2.getYVector().mapMultiply(Math.sin(thetaRad)));
		
		return new Trace(xs, yvec.toArray());
	}
}
