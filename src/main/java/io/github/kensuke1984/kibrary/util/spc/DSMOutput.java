/**
 *
 */
package io.github.kensuke1984.kibrary.util.spc;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

import java.util.List;

/**
 * Data of DSM write
 *
 * @author Kensuke Konishi
 * @version 0.0.1
 * @author anselme add network
 */
public interface DSMOutput {

    /**
     * @return number of bodies
     */
    int nbody();

    /**
     * @return list of spc bodies
     */
    List<SPCBody> getSpcBodyList();
	
	void setSpcBody(int i, SPCBody body);

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
     * @return ID of an observer (Station)
     */
    String getObserverID();
	
	/**
	 * @return Network of an observer
	 * @author anselme
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
     * @return OMEGAI
     */
    double omegai();

    /**
     * @return SPCType of this
     */
    SPCType getSpcFileType();

	
	SPCFile getSpcFileName();
}
