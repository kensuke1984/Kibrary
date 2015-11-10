/**
 * 
 */
package anisotime;

/**
 * @author kensuke
 * @since 2014/07/29
 * @version 0.0.1
 * 
 *          Modes of computation
 */
enum ComputationMode {
	RAYPARAMETER(0), TURNING_DEPTH(1), DIFFRACTION(2), EPICENTRAL_DISTANCE(3);
	
	private int value;
	private ComputationMode(int i) {
		value = i;
	}
	
	
	/**
	 * @param i
	 * @return 0:normal, 1:turningdepth, 2:diffraction, 3:epicentral distance or null
	 */
	static ComputationMode valueOf(int i){
		for(ComputationMode mode : values())
			if(mode.value==i)
				return mode;
		return null;
	}
	
}
