/**
 * 
 */
package manhattan.globalcmt;

import java.time.LocalDateTime;

import manhattan.datacorrection.MomentTensor;
import manhattan.template.Location;

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
