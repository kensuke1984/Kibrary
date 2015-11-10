/**
 * 
 */
package manhattan.gmt;

/**
 * 
 * color tables in GMT
 * 
 * @since 2014/02/27
 * @version 0.0.1
 * 
 * 
 * @author kensuke
 * 
 */
public enum ColorTable {
	/**
	 * Linear change from blue to magenta [0/1]
	 */
	cool, //
	/**
	 * Dark to light copper brown [0/1]
	 */
	copper, //
	/**
	 * Cyclic colormap, spans 360 degrees of hue [0/360]
	 */
	cyclic, //
	/**
	 * Goes from dry to wet colors [0/12]
	 */
	drywet, //
	/**
	 * Colors for GEBCO bathymetric charts [-7000/0]
	 */
	gebco, //
	/**
	 * Colors for global bathy-topo relief [-10000/10000]
	 */
	globe, //
	/**
	 * Grayramp from black to white [0/1]
	 */
	gray, //
	/**
	 * Bill Haxby's colortable for geoid &amp; gravity [0/32]
	 */
	haxby, //
	/**
	 * Black through red and yellow to white [0/1]
	 */
	hot, //
	/**
	 * Dark to light blue, white, yellow and red [0/1]
	 */
	jet, //
	/**
	 * Colors for DMSP-OLS Nighttime Lights Time Series [0/1]
	 */
	nighttime, //
	/**
	 * For those who hate green [-32/+32]
	 */
	no_green, //
	/**
	 * white-green-blue bathymetry scale [-8000/0]
	 */
	ocean, //
	/**
	 * Default colormap of Panoply [0/16]
	 */
	panoply, //
	/**
	 * Qualitative color map with 6 pairs of colors [0/12]
	 */
	paired, //
	/**
	 * Blue via white to red [-1/+1]
	 */
	polar, //
	/**
	 * Rainbow,// magenta-blue-cyan-green-yellow-red [0/300]
	 */
	rainbow, //
	/**
	 * Polar scale from red to green via white [-1/+1]
	 */
	red2green, //
	/**
	 * Wessel/Martinez colors for topography [-8000/+8000]
	 */
	relief, //
	/**
	 * Smith bathymetry/topography scale [-6000/+3000]
	 */
	sealand, //
	/**
	 * R-O-Y-G-B seismic tomography colors [-1/+1]
	 */
	seis, //
	/**
	 * Like polar, but via black instead of white [-1/+1]
	 */
	split, //
	/**
	 * Sandwell/Anderson colors for topography [-7000/+7000]
	 */
	topo, //
	/**
	 * 20 well-separated RGB colors [0/20]
	 */
	wysiwyg, //
	;
}
