/**
 * 
 */
package mathtool.geometry;

/**
 * @since 2014/02/04
 * @version 0.0.1
 *
 * point (x, y)
 *
 * @author kensuke
 *
 */
public class Point2D {

	double x;
	double y; 
	
	
	
	
	public Point2D(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	

	
	public double getX() {
		return x;
	}




	public double getY() {
		return y;
	}




	public  String toString(){
		return "("+x+", "+y+")";
	}



}
