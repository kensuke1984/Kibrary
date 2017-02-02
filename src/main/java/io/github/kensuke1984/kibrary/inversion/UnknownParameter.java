package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.spc.PartialType;

/**
 * Am = d の中の mのある成分
 * <p>
 * location (radius), type of parameter, weighting (width or volume or...)
 * <p>
 * <p>
 * The class implementing this must be <b>IMMUTABLE</b>.
 *
 * @author Kensuke Konishi
 * @version 0.0.2.1.1
 */
public interface UnknownParameter {

    /**
     * Weighting may be a width of a layer of volume of voxel and so on...
     *
     * @return weighting for this parameter
     */
    double getWeighting();

    /**
     * @return {@link PartialType} of a parameter
     */
    PartialType getPartialType();

}
