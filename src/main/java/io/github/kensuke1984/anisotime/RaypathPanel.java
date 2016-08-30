/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.QuadCurve2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.math3.util.Precision;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;

/**
 * @author Kensuke Konishi
 * @version 0.0.3.4
 */
final class RaypathPanel extends JPanel {


	/**
	 * 2016/8/30
	 */
	private static final long serialVersionUID = 8852619628102508529L;

	RaypathPanel(VelocityStructure structure) {
		this.earthRadius = structure.earthRadius();
		this.coreMantleBoundary = structure.coreMantleBoundary();
		this.innerCoreBoundary = structure.innerCoreBoundary();
		setSize(700, 700);
	}

	private List<Double> additionalCircle = new ArrayList<>();

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		// for (QuadCurve2D.Double curve : quadCurve)
		// g2.draw(curve);
		drawCurves(g2);
		// g.drawLine(0 , 90, 100, 200);
		drawEarth(g);
		// g2.drawString("jo", 300, 300);
		drawAdditionalCircles(g);
		// System.out.println(g.getClass());
	}

	/**
	 * @param outputStream
	 *            output stream (will not be closed by this method.)
	 * @param phase
	 *            to be shown
	 * @param rayparameter
	 *            p
	 * @param delta
	 *            [rad] to be shown.
	 * @param time
	 *            [s] to be printed
	 * @param eventR
	 *            radius of the event [km]
	 * @throws IOException if any
	 */
	void toEPS(OutputStream outputStream, Phase phase, double rayparameter, double delta, double time, double eventR)
			throws IOException {
		EpsGraphics epsGraphics = new EpsGraphics(phase.toString(), outputStream, 0, 0, getWidth(), getHeight(),
				ColorMode.COLOR_RGB);
		paintComponent(epsGraphics);
		rayparameter = Precision.round(rayparameter, 3);
		delta = Precision.round(Math.toDegrees(delta), 3);
		time = Precision.round(time, 3);
		double depth = Precision.round(earthRadius - eventR, 3);
		String line = phase.toString() + ", Ray parameter: " + rayparameter + ", Depth[km]:" + depth
				+ ", Epicentral distance[deg]: " + delta + ", Travel time[s]: " + time;
		int startInt = (int) changeX(-line.length() / 2 * 6371 / 45);
		epsGraphics.drawString(line, startInt, (int) changeY(earthRadius) - 25);
	}

	private int featured;

	/**
	 * Draw Curves
	 */
	synchronized private void drawCurves(Graphics2D g2) {
		for (int i = 0; i < quadCurves.size(); i++) {
			if (i == featured)
				continue;
			for (QuadCurve2D.Double curve : quadCurves.get(i)) {
				double x1 = changeX(curve.x1);
				double x2 = changeX(curve.x2);
				double y1 = changeY(curve.y1);
				double y2 = changeY(curve.y2);
				double ctrlx = changeX(curve.ctrlx);
				double ctrly = changeY(curve.ctrly);
				QuadCurve2D.Double newCurve = new QuadCurve2D.Double(x1, y1, ctrlx, ctrly, x2, y2);
				g2.draw(newCurve);
			}
		}
		g2.setColor(Color.RED);
		for (QuadCurve2D.Double curve : quadCurves.get(featured)) {
			double x1 = changeX(curve.x1);
			double x2 = changeX(curve.x2);
			double y1 = changeY(curve.y1);
			double y2 = changeY(curve.y2);
			double ctrlx = changeX(curve.ctrlx);
			double ctrly = changeY(curve.ctrly);
			QuadCurve2D.Double newCurve = new QuadCurve2D.Double(x1, y1, ctrlx, ctrly, x2, y2);
			g2.draw(newCurve);
		}
		// g2.drawLine(0, 70, 500, 0);
		g2.setColor(Color.BLACK);
	}

	private void drawEarth(Graphics g) {
		int size = getSize().width < getSize().height ? getSize().width : getSize().height;
		Graphics2D g2 = (Graphics2D) g;
		Stroke defaultStroke = g2.getStroke();
		BasicStroke wideStroke = new BasicStroke(2);
		g2.setStroke(wideStroke);

		// Surface
		g2.drawOval(size / 10, size / 10, iEarthRadius() * 2, iEarthRadius() * 2);
		// CMB
		g2.drawOval((int) changeX(-coreMantleBoundary), (int) changeY(coreMantleBoundary),
				(int) (iEarthRadius() * coreMantleBoundary / earthRadius * 2),
				(int) (iEarthRadius() * coreMantleBoundary / earthRadius * 2));
		// icb
		g2.drawOval((int) changeX(-innerCoreBoundary), (int) changeY(innerCoreBoundary),
				(int) (iEarthRadius() * innerCoreBoundary / earthRadius * 2),
				(int) (iEarthRadius() * innerCoreBoundary / earthRadius * 2));

		g2.setStroke(defaultStroke);
		// 410
		double r410 = earthRadius - 410;
		g.drawOval((int) changeX(-r410), (int) changeY(r410), (int) (iEarthRadius() * r410 / earthRadius * 2),
				(int) (iEarthRadius() * r410 / earthRadius * 2));
		// 660
		double r660 = earthRadius - 660;
		// g.setColor(Color.RED);
		g.drawOval((int) changeX(-r660), (int) changeY(r660), (int) (iEarthRadius() * r660 / earthRadius * 2),
				(int) (iEarthRadius() * r660 / earthRadius * 2));
	}

	/**
	 * @return int value for the Earth radius in the display
	 */
	private int iEarthRadius() {
		int size = getSize().width < getSize().height ? getSize().width : getSize().height;
		return size * 4 / 10;
	}

	void setFeatured(int i) {
		featured = i;
		revalidate();
		repaint();
	}

	/**
	 * Earth radius [km] it will be in the display
	 */
	private double earthRadius;

	/**
	 * Radius of the core mantle boundary [km]
	 */
	private double coreMantleBoundary;

	/**
	 * Radius of the inner core boundary [km]
	 */
	private double innerCoreBoundary;

	private void drawAdditionalCircles(Graphics g) {
		for (double d : additionalCircle)
			g.drawOval((int) changeX(-d), (int) changeY(d), (int) (iEarthRadius() * d / earthRadius * 2),
					(int) (iEarthRadius() * d / earthRadius * 2));
	}

	synchronized void addPath(double[] x, double[] y) {
		if (x.length != y.length)
			throw new IllegalArgumentException("Unexpected");
		// System.out.println("adding path");
		List<QuadCurve2D.Double> curveList = new ArrayList<>();
		for (int i = 0; i < (x.length - 1) / 2; i++) {
			double x1 = x[i * 2];
			double ctrlx = x[i * 2 + 1];
			double x2 = x[i * 2 + 2];
			double y1 = y[i * 2];
			double ctrly = y[i * 2 + 1];
			double y2 = y[i * 2 + 2];
			QuadCurve2D.Double curve = new QuadCurve2D.Double(x1, y1, ctrlx, ctrly, x2, y2);
			curveList.add(curve);
		}
		if (x.length % 2 == 0) {
			double x1 = x[x.length - 2];
			double x2 = x[x.length - 1];
			double y1 = y[x.length - 2];
			double y2 = y[x.length - 1];
			double ctrlx = 0.5 * (x1 + x2);
			double ctrly = 0.5 * (y1 + y2);
			QuadCurve2D.Double curve = new QuadCurve2D.Double(x1, y1, ctrlx, ctrly, x2, y2);
			curveList.add(curve);
		}
		quadCurves.add(curveList);
		SwingUtilities.invokeLater(this::repaint);
	}

	private final List<List<QuadCurve2D.Double>> quadCurves = new ArrayList<>();

	/**
	 * input x is one in cooridate system where the earth center is (0,0) &rarr;
	 * (350, 350)
	 * 
	 * @param x
	 *            to be shifted
	 * @return x coordinate to use
	 */
	double changeX(double x) {
		int size = getSize().width < getSize().height ? getSize().width : getSize().height;
		// double xInDisplay = x / earthRadius * 250 + 350;
		double xInDisplay = x / earthRadius * iEarthRadius() + size / 2;
		// System.out.println(x + " " + xInDisplay+" "+getSize().width);
		return Math.round(xInDisplay);
	}

	/**
	 * input x is one in cooridate system where the earth center is (0,0) &rarr;
	 * (350, 350)
	 * 
	 * @param y
	 *            to be shifted
	 * @return y coordinate to use
	 */
	double changeY(double y) {
		// double yInDisplay = -y / earthRadius * 250 + 350;
		int size = getSize().width < getSize().height ? getSize().width : getSize().height;
		double yInDisplay = -y / earthRadius * iEarthRadius() + size * 0.5;
		return Math.round(yInDisplay);
	}

}
