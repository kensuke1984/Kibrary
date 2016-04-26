/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Raypath catalogue for one model
 * 
 * 
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class RaypathCatalogue implements Serializable{
	/**
	 * Serialization identifier 2016/4/25
	 */
	private static final long serialVersionUID = 930482955965404351L;

	/**
	 * velocity structure
	 */
	private final VelocityStructure structure;

	/**
	 * Stock raypaths whose bouncing points are at this interval
	 */
	private final double bottomRadiusInterval;

	/**
	 * The grid interval for integral
	 */
	private final double integralRadiusInterval;
	
	private final List<Raypath> rapathList=new ArrayList<>();

	public RaypathCatalogue() {
		structure = VelocityStructure.prem();
		bottomRadiusInterval = 1;
		integralRadiusInterval = 1;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
}
