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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;

/**
 * @author Kensuke Konishi
 * @version 0.0.3
 */
final class RaypathPanel extends JPanel {

	private static final long serialVersionUID = 7294142926931120664L;

	RaypathPanel(double earthRadius, double coreMantleBoundary, double innerCoreBoundary) {
		this.earthRadius = earthRadius;
		this.coreMantleBoundary = coreMantleBoundary;
		this.innerCoreBoundary = innerCoreBoundary;
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

	void toEPS(OutputStream outputStream, Phase phase, double rayparameter, double delta, double time, double radius) {
		EpsGraphics epsGraphics = null;
		try {
			epsGraphics = new EpsGraphics(phase.toString(), outputStream, 0, 0, getWidth(), getHeight(),
					ColorMode.COLOR_RGB);
			paintComponent(epsGraphics);
			rayparameter = Math.round(rayparameter * 1000) / 1000.0;
			delta = Math.round(Math.toDegrees(delta) * 1000) / 1000.0;
			time = Math.round(time * 1000) / 1000.0;
			double depth = Math.round((6371 - radius) * 1000.0) / 1000.0;
			String line = phase.toString() + ", Ray parameter: " + rayparameter + ", Depth[km]:" + depth
					+ ", Epicentral distance[deg]: " + delta + ", Travel time[s]: " + time;
			int startInt = (int) changeX(-line.length() / 2 * 6371 / 45);
			// epsGraphics.drawLine(0, 100, 200, 300);
			// epsGraphics.close();
			epsGraphics.drawString(line, startInt, (int) changeY(6371) - 25);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("orz");
		} finally {
			if (epsGraphics != null)
				try {
					epsGraphics.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
		}

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
				double x1 = curve.x1;
				double x2 = curve.x2;
				double y1 = curve.y1;
				double y2 = curve.y2;
				double ctrlx = curve.ctrlx;
				double ctrly = curve.ctrly;
				x1 = changeX(x1);
				x2 = changeX(x2);
				y1 = changeY(y1);
				y2 = changeY(y2);
				ctrlx = changeX(ctrlx);
				ctrly = changeY(ctrly);
				QuadCurve2D.Double newCurve = new QuadCurve2D.Double(x1, y1, ctrlx, ctrly, x2, y2);
				g2.draw(newCurve);
				// System.out.println("curve"+ x1);
			}
		}
		g2.setColor(Color.RED);
		for (QuadCurve2D.Double curve : quadCurves.get(featured)) {
			double x1 = curve.x1;
			double x2 = curve.x2;
			double y1 = curve.y1;
			double y2 = curve.y2;
			double ctrlx = curve.ctrlx;
			double ctrly = curve.ctrly;
			x1 = changeX(x1);
			x2 = changeX(x2);
			y1 = changeY(y1);
			y2 = changeY(y2);
			ctrlx = changeX(ctrlx);
			ctrly = changeY(ctrly);
			QuadCurve2D.Double newCurve = new QuadCurve2D.Double(x1, y1, ctrlx, ctrly, x2, y2);
			g2.draw(newCurve);
			// System.out.println("curve"+ x1);
		}
		// g2.drawLine(0, 70, 500, 0);
		g2.setColor(Color.BLACK);
	}

	private void drawEarth(Graphics g) {
		// System.out.println(earthRadius);
		// System.out.println(getSize().height);
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
	 * earth radius it will be in the display
	 */
	private double earthRadius;

	/**
	 * radius of the core mantle boundary
	 */
	private double coreMantleBoundary;

	/**
	 * radius of the inner core boundary
	 */
	private double innerCoreBoundary;

	/**
	 * input x is one in cooridate system where the earth center is (0,0) (0,0)
	 * -> (350, 350)
	 * 
	 * @param x
	 * @return x coordinate to use
	 */
	private double changeX(double x) {
		int size = getSize().width < getSize().height ? getSize().width : getSize().height;
		// double xInDisplay = x / earthRadius * 250 + 350;
		double xInDisplay = x / earthRadius * iEarthRadius() + size / 2;
		// System.out.println(x + " " + xInDisplay+" "+getSize().width);
		return Math.round(xInDisplay);
	}

	private void drawAdditionalCircles(Graphics g) {
		for (double d : additionalCircle)
			g.drawOval((int) changeX(-d), (int) changeY(d), (int) (iEarthRadius() * d / earthRadius * 2),
					(int) (iEarthRadius() * d / earthRadius * 2));
	}

	synchronized void addPath(double[] x, double[] y) {
		if (x.length != y.length)
			return;
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
		SwingUtilities.invokeLater(() -> repaint());
	}

	private final List<List<QuadCurve2D.Double>> quadCurves = new ArrayList<>();

	/**
	 * input x is one in cooridate system where the earth center is (0,0) (0,0)
	 * -> (350, 350)
	 * 
	 * @param y
	 * @return y coordinate to use
	 */
	private double changeY(double y) {
		// double yInDisplay = -y / earthRadius * 250 + 350;
		int size = getSize().width < getSize().height ? getSize().width : getSize().height;
		double yInDisplay = -y / earthRadius * iEarthRadius() + size * 0.5;
		return Math.round(yInDisplay);
	}

}
