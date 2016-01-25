/**
 * 
 */
package io.github.kensuke1984.kibrary.util.globalcmt;

import java.time.LocalDateTime;

import io.github.kensuke1984.kibrary.datacorrection.MomentTensor;
import io.github.kensuke1984.kibrary.util.Location;

/**
 * Data for global CMT data used frequently.
 * @author kensuke
 * @version 0.0.1
 */
public interface GlobalCMTData {

	double getMb();

	double getMs();

	MomentTensor getCmt();

	Location getCmtLocation();

	LocalDateTime getCMTTime();

	double getHalfDuration();

	Location getPDELocation();

	LocalDateTime getPDETime();

	GlobalCMTID getGlobalCMTID();

}
