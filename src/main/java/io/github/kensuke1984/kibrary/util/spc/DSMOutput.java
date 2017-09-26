/**
 * 
 */
package io.github.kensuke1984.kibrary.util.spc;

import java.util.List;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

/**
 * Data of DSM output
 * 
 * @author Kensuke Konishi
 * @version 0.0.1
 */
public interface DSMOutput {

	/**
	 * @return number of bodies
	 */
	int nbody();

	/**
	 * @return list of spc bodies
	 */
	List<SpcBody> getSpcBodyList();

	/**
	 * @return array of body Rs
	 */
	double[] getBodyR();

	/**
	 * @return Location of a seismic source.
	 */
	Location getSourceLocation();

	/**
	 * @return ID of a source
	 */
	String getSourceID();

	/**
	 * @return ID of an observer
	 */
	@Deprecated
	String getObserverID();
	
	/**
	 * @return Station
	 */
	String getObserverName();
	
	/**
	 * @return Network
	 */
	String getObserverNetwork();

	/**
	 * @return HorizontalPosition of an observer.
	 */
	HorizontalPosition getObserverPosition();

	/**
	 * @return length of time
	 */
	double tlen();

	/**
	 * @return number of steps in frequency domain.
	 */
	int np();

	/**
	 * @return omegai
	 */
	double omegai();

	/**
	 * @return SpcFileType of this
	 */
	SpcFileType getSpcFileType();
	
	SpcFileName getSpcFileName();

}
