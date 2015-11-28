package manhattan.inversion;

import filehandling.spc.PartialType;

/**
 * Am = d の中の mのある成分
 * 
 * location (radius), type of parameter, weighting (width or volume or...)
 * 
 * <p>
 * This class is <b>IMMUTABLE</b>.
 * 
 * @version 0.0.2
 * @since 2015/8/27
 * isSamePoint installed.
 * 
 * @author kensuke
 *
 */
public interface UnknownParameter {

	/**
	 * Weighting may be a width of a layer of volume of voxel and so on...
	 * @return weighting for this parameter
	 */
	public double getWeighting();

	/**
	 * @return {@link PartialType} of a parameter
	 */
	public PartialType getPartialType() ;


}
