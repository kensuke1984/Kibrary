package io.github.kensuke1984.kibrary.inversion;

import java.util.ArrayList;
import java.util.List;

import io.github.kensuke1984.kibrary.math.geometry.ConvexPolygon;
import io.github.kensuke1984.kibrary.math.geometry.Point2D;
import io.github.kensuke1984.kibrary.math.geometry.RThetaPhi;
import io.github.kensuke1984.kibrary.math.geometry.XYZ;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Raypath;

/**
 * RaypathまわりにGridを作る
 * @version 0.0.2
 * @author Kensuke Konishi
 */
public class GridMaker extends Raypath {

	/**
	 * 震源観測点の中心点 波線上の中心（震源深さは考慮していない）
	 */
	private HorizontalPosition midPoint;

	public GridMaker(Location source, HorizontalPosition station) {
		super(source, station);

		// this.station = station;
		System.out.println("station: " + station);
		// this.source = source;
		System.out.println("source: " + source);
		this.midPoint = station.getMidpoint(source);
		System.out.println("mid point: " + midPoint);
	}

	public static HorizontalPosition[] makeGrid(double startLatitude, double endLatitude, double dtheta,
			double startLongitude, double endLongitude, double dphi) {
		int ntheta = (int) ((endLatitude - startLatitude) / dtheta);
		int nphi = (int) ((endLongitude - startLongitude) / dphi);
		HorizontalPosition[] location = new HorizontalPosition[nphi * ntheta];
		for (int i = 0; i < ntheta; i++)
			for (int j = 0; j < nphi; j++)
				location[i * nphi + j] = new HorizontalPosition(startLatitude + i * dtheta, startLongitude + j * dphi);

		return location;
	}

	public static void main(String[] args) {

		HorizontalPosition[] hps = GridMaker.makeGridIn();

		for (HorizontalPosition hp : hps)
			System.out.println(hp);

		System.exit(0);
		HorizontalPosition station = new HorizontalPosition(0, 65);
		Location event = new Location(0, 0, 5871);
		GridMaker gm = new GridMaker(event, station);
		// System.out.println(gm.midPoint.isContainsR());
		// System.out.println(Math.toDegrees(gm.epicentralDistance));
		double thetaRange = Math.toRadians(20);
		double deltaTheta = Math.toRadians(1);
		double phiRange = Math.toRadians(10);
		double deltaPhi = Math.toRadians(1);
		HorizontalPosition[] positions = gm.makeGrids(thetaRange, deltaTheta, phiRange, deltaPhi);

		for (int i = 0; i < positions.length; i++)
			System.out.println(positions[i]);

		System.out.println(positions.length);
		// gm.computePointsLocationsOnRayPath(thetaRange, deltaTheta);
		System.exit(0);
		HorizontalPosition[] loc0 = makeGridWP();
		HorizontalPosition[] loc1 = makeGridWPWide();
		// System.out.println(loc1.length);
		System.out.println("hi");
		int j = 0;
		for (int i = 0; i < loc1.length; i++) {
			boolean contain = false;
			for (int k = 0; k < loc0.length; k++) {
				if (loc0[k].equals(loc1[i])) {
					contain = true;
					break;
				}
			}
			if (!contain)
				System.out.println(loc1[i].getLatitude() + " " + loc1[i].getLongitude());
		}
		System.out.println(j);

		// Location point = new Location(10, 10);
		// point.setR(6000);
		// double v = GridMaker.getVolume(point, 10, 5, 5);
		// System.out.println(v);
	}

	/**
	 * TODO チェック必要 波線にそって震源観測点中心から ±thetaRange ±phiRangeをとる thetaは波線方向
	 * phiは大円に直交方向 その領域の中からdeltaTheta deltaPhiで点を作る [手順] 中心点を北極に持って行って、波線方向の点を作る
	 * その点に直交するように 更にポイントを取る。
	 * 
	 * @param thetaRange
	 *            [rad]
	 * @param deltaTheta
	 *            [rad]
	 * @param phiRange
	 *            [rad]
	 * @param deltaPhi
	 *            [rad]
	 * @return points in the ranges
	 */
	public HorizontalPosition[] makeGrids(double thetaRange, double deltaTheta, double phiRange, double deltaPhi) {
		// System.out.println(thetaRange+" "+deltaTheta);
		int nPhi = (int) Math.round(phiRange / deltaPhi);
		int nTheta = (int) (thetaRange / deltaTheta);
		HorizontalPosition[] positions = new HorizontalPosition[(2 * nPhi + 1) * (2 * nTheta + 1)];
		// double midTheta = 0.5*epicentralDistance;
		// System.out.println("midtheta"+Math.toDegrees(midTheta));
		// double lat = Latitude.toLatitude(midTheta);
		// Location loc1 = new Location(lat, 0);
		// System.out.println("loc1"+loc1);
		// Location loc2 = toRaypath(loc1);
		// System.out.println("loc2"+loc2);
		// System.exit(0);

		// 波線上のポイントを作る
		HorizontalPosition[] raypathLocation = computePointsLocationsOnRayPath(thetaRange, deltaTheta);
		int k = 0;
		for (int iTheta = -nTheta; iTheta < nTheta + 1; iTheta++)
			for (int iPhi = -nPhi; iPhi < nPhi + 1; iPhi++) {
				// 波線に直交するポイントを作るための点（北極中心に置く）
				XYZ xyz = iPhi < 0 ? RThetaPhi.toCartesian(Earth.EARTH_RADIUS, Math.abs(iPhi) * deltaPhi, Math.PI * 1.5)
						: RThetaPhi.toCartesian(Earth.EARTH_RADIUS, iPhi * deltaPhi, Math.PI * 0.5);

				xyz = xyz.rotateaboutZ(Math.PI - azimuth);
				xyz = xyz.rotateaboutY(raypathLocation[iTheta + nTheta].getTheta());
				xyz = xyz.rotateaboutZ(raypathLocation[iTheta + nTheta].getPhi());
				positions[k++] = xyz.getLocation();
				// System.out.println(latitude+" "+longitude);
			}

		return positions;
		// System.out.println(iPhi+" "+iTheta);
		// System.out.println(nPhi+" "+nTheta);

	}

	/**
	 * TODO check必要
	 * 
	 * @param thetaRange
	 * @param deltaTheta
	 * @return
	 */
	private HorizontalPosition[] computePointsLocationsOnRayPath(double thetaRange, double deltaTheta) {
		int nTheta = (int) Math.round(thetaRange / deltaTheta);
		HorizontalPosition[] locations = new HorizontalPosition[2 * nTheta + 1];
		System.out.println(locations.length);
		for (int iTheta = -nTheta; iTheta < nTheta + 1; iTheta++) {
			double theta = iTheta * deltaTheta;
			XYZ xyz = RThetaPhi.toCartesian(Earth.EARTH_RADIUS, theta, 0);
			xyz = xyz.rotateaboutZ(Math.PI - azimuth);
			xyz = xyz.rotateaboutY(midPoint.getTheta());
			xyz = xyz.rotateaboutZ(midPoint.getPhi());
			locations[iTheta + nTheta] = xyz.getLocation();
		}

		return locations;
	}

	public static HorizontalPosition[] makeGrid0(double startLongitude, double endLongitude, double startLatitude,
			double endLatitude) {

		double dLatitude = 5;
		double dLongitude = 5;
		int nLongitude = (int) ((endLongitude - startLongitude) / dLongitude);
		int nLatitude = (int) ((endLatitude - startLatitude) / dLatitude);
		double[] longitudes = new double[nLongitude];
		double[] latitudes = new double[nLatitude];
		int nloc = nLongitude * nLatitude;
		HorizontalPosition[] loc = new HorizontalPosition[nloc];
		// System.out.println("yo");
		for (int i = 0; i < nLatitude; i++)
			latitudes[i] = startLatitude + i * dLatitude;

		for (int i = 0; i < nLongitude; i++)
			longitudes[i] = startLongitude + i * dLongitude;

		for (int ilat = 0; ilat < nLatitude; ilat++)
			for (int ilon = 0; ilon < nLongitude; ilon++) {
				loc[ilat * nLongitude + ilon] = new HorizontalPosition(latitudes[ilat], longitudes[ilon]);
			}
		return loc;

	}

	public static HorizontalPosition[] makeGridIn() {
		int startLongitude = 60;
		int endLongitude = 110;
		int startLatitude = 10;
		int endLatitude = 50;
		int dLatitude = 5;
		int dLongitude = 5;
		int nLongitude = (int) ((endLongitude - startLongitude) / dLongitude) + 1;
		int nLatitude = (int) ((endLatitude - startLatitude) / dLatitude) + 1;
		double[] longitudes = new double[nLongitude];
		double[] latitudes = new double[nLatitude];
		// System.out.println("yo");
		HorizontalPosition[] hps = new HorizontalPosition[nLongitude * nLatitude];

		for (int i = 0; i < nLatitude; i++) {
			latitudes[i] = startLatitude + i * dLatitude;
			// System.out.println(latitudes[i]);
			for (int j = 0; j < nLongitude; j++) {
				longitudes[j] = startLongitude + j * dLongitude;
				// System.out.println(longitudes[i]);
				hps[i * nLongitude + j] = new HorizontalPosition(latitudes[i], longitudes[j]);
			}
		}
		return hps;
	}

	public static HorizontalPosition[] makeGridCA() {
		int startLongitude = -105;
		int endLongitude = -74;
		int startLatitude = -10;
		int endLatitude = 31;
		int dLatitude = 1;
		int dLongitude = 1;
		int nLongitude = (int) ((endLongitude - startLongitude) / dLongitude);
		int nLatitude = (int) ((endLatitude - startLatitude) / dLatitude);
		double[] longitudes = new double[nLongitude];
		double[] latitudes = new double[nLatitude];
		// System.out.println("yo");
		for (int i = 0; i < nLatitude; i++) {
			latitudes[i] = startLatitude + i * dLatitude;
			// System.out.println(latitudes[i]);
		}

		for (int i = 0; i < nLongitude; i++) {
			longitudes[i] = startLongitude + i * dLongitude;
			// System.out.println(longitudes[i]);
		}
		List<HorizontalPosition> locs = new ArrayList<>();
		for (int j = 0; j < nLongitude; j++) {
			for (int i = 0; i < nLatitude; i++) {
				if ((-longitudes[j] - 90) < latitudes[i]) {
					// System.out.println(latitudes[i]+" "+longitudes[j]);
					locs.add(new HorizontalPosition(latitudes[i], longitudes[j]));
				}
			}
		}
		HorizontalPosition[] loc = locs.toArray(new HorizontalPosition[0]);
		return loc;

	}

	public static HorizontalPosition[] makeGridWPTA() {
		int startLongitude = 135;
		int endLongitude = 190;
		int startLatitude = -20;
		int endLatitude = 35;

		int nLongitude = (endLongitude - startLongitude) / 5 + 1;
		int nLatitude = (endLatitude - startLatitude) / 5 + 1;
		HorizontalPosition[] hp = new HorizontalPosition[72];
		int k = 0;
		for (int i = 0; i < nLongitude; i++) {
			for (int j = 0; j < nLatitude; j++) {
				int x = startLongitude + i * 5;
				int y = startLatitude + j * 5;

				if (y > x - 130)
					continue;
				if (y < x - 180)
					continue;
				if (y < -x + 140)
					continue;
				if (y > -x + 200)
					continue;
				hp[k++] = new HorizontalPosition(y, x);

				// (135, 5), (165, 35) y=x-130
				// (135, 5), (160, -20) y=-x+140
				// (160,-20), (190, 10) y=x-180
				// (165, 35), (190, 10) y=-x+200

				// (150, 10), (160, 20) y=x-140
				// (150, 10), (165, -5) y= -x*160
				// (165, -5), (175, 5) y=x-170
				// (160, 20), (175, 5) y=-x+180

				if (y > x - 140 || y < -x + 160 || y < x - 170 || y > -x + 180)
					System.out.println(y + " " + x);
			}

		}

		return hp;
	}

	public static HorizontalPosition[] makeGridWPTA2() {
		int startLongitude = 135;
		int endLongitude = 190;
		int startLatitude = -20;
		int endLatitude = 35;

		int nLongitude = (endLongitude - startLongitude) + 1;
		int nLatitude = (endLatitude - startLatitude) + 1;
		HorizontalPosition[] hp = new HorizontalPosition[72];
		int k = 0;
		for (int i = 0; i < nLongitude; i++) {
			for (int j = 0; j < nLatitude; j++) {
				int x = startLongitude + i;
				int y = startLatitude + j;

				if (y > x - 130)
					continue;
				if (y < x - 180)
					continue;
				if (y < -x + 140)
					continue;
				if (y > -x + 200)
					continue;
				// hp[k++] = new HorizontalPosition(y, x);
				k++;
				// (135, 5), (165, 35) y=x-130
				// (135, 5), (160, -20) y=-x+140
				// (160,-20), (190, 10) y=x-180
				// (165, 35), (190, 10) y=-x+200

				// (150, 10), (160, 20) y=x-140
				// (150, 10), (165, -5) y= -x*160
				// (165, -5), (175, 5) y=x-170
				// (160, 20), (175, 5) y=-x+180

				// if (y > x - 140 || y < -x + 160 || y < x - 170 || y > -x +
				// 180)
				System.out.println("XY" + String.format("%02d", k) + " " + y + " " + x);
			}

		}
		System.out.println(k);
		return hp;
	}

	public static HorizontalPosition[] makeGridIzuBonin() {
		return new HorizontalPosition[] { new HorizontalPosition(30, 140) };
	}

	public static HorizontalPosition[] makeGridFarallonSlab() {
		return new HorizontalPosition[] { new HorizontalPosition(10, -90) };
	}

	public static HorizontalPosition[] makeAdditionalPoints() {
		Point2D[] points = new Point2D[] { new Point2D(140, 15), new Point2D(155, 30), new Point2D(185, 0),
				new Point2D(170, -15) };
		ConvexPolygon cp = new ConvexPolygon(points);
		List<HorizontalPosition> hpList = new ArrayList<>();
		for (int i = 140; i <= 185; i += 5) {
			for (int j = -15; j <= 30; j += 5) {
				Point2D p = new Point2D(i, j);
				// System.out.println(p);
				if (!cp.contains(p))
					continue;
				// System.out.println(i + " " + j);
				hpList.add(new HorizontalPosition(j, i));

			}

		}

		return (HorizontalPosition[]) hpList.toArray(new HorizontalPosition[0]);
	}

	public static HorizontalPosition[] makeGridWP() {
		int startLongitude = 145;
		int endLongitude = 181;
		int startLatitude = -10;
		int endLatitude = 26;
		int dLatitude = 1;
		int dLongitude = 1;
		int nLongitude = (int) ((endLongitude - startLongitude) / dLongitude);
		int nLatitude = (int) ((endLatitude - startLatitude) / dLatitude);
		double[] longitudes = new double[nLongitude];
		double[] latitudes = new double[nLatitude];
		// System.out.println("yo");
		for (int i = 0; i < nLatitude; i++) {
			latitudes[i] = startLatitude + i * dLatitude;
			// System.out.println(latitudes[i]);
		}

		for (int i = 0; i < nLongitude; i++) {
			longitudes[i] = startLongitude + i * dLongitude;
			// System.out.println(longitudes[i]);
		}
		List<HorizontalPosition> locs = new ArrayList<>();
		for (int j = 0; j < nLongitude; j++) {
			for (int i = 0; i < nLatitude; i++) {
				if ((longitudes[j] - 135) > latitudes[i] && longitudes[j] < 161
						&& (-longitudes[j] + 155) < latitudes[i]) {
					// System.out.println(latitudes[i] + " " + longitudes[j]);
					locs.add(new HorizontalPosition(latitudes[i], longitudes[j]));
				}
				if ((-longitudes[j] + 155) < latitudes[i] && longitudes[j] > 160 && longitudes[j] < 166
						&& (-longitudes[j] + 185) > latitudes[i]) {
					// System.out.println(latitudes[i] + " " + longitudes[j]);
					locs.add(new HorizontalPosition(latitudes[i], longitudes[j]));
				}
				if ((-longitudes[j] + 185) > latitudes[i] && longitudes[j] > 165
						&& (longitudes[j] - 175) < latitudes[i]) {
					// System.out.println(latitudes[i] + " " + longitudes[j]);
					locs.add(new HorizontalPosition(latitudes[i], longitudes[j]));
				}

			}
		}
		HorizontalPosition[] loc = (HorizontalPosition[]) locs.toArray(new HorizontalPosition[0]);
		return loc;
	}

	public static HorizontalPosition[] makeGridWPWide() {
		int startLongitude = 145;
		int endLongitude = 181;
		int startLatitude = -10;
		int endLatitude = 31;
		int dLatitude = 1;
		int dLongitude = 1;
		int nLongitude = (int) ((endLongitude - startLongitude) / dLongitude);
		int nLatitude = (int) ((endLatitude - startLatitude) / dLatitude);
		double[] longitudes = new double[nLongitude];
		double[] latitudes = new double[nLatitude];
		// System.out.println("yo");
		for (int i = 0; i < nLatitude; i++) {
			latitudes[i] = startLatitude + i * dLatitude;
			// System.out.println(latitudes[i]);
		}

		for (int i = 0; i < nLongitude; i++) {
			longitudes[i] = startLongitude + i * dLongitude;
			// System.out.println(longitudes[i]);
		}
		List<HorizontalPosition> locs = new ArrayList<>();
		for (int j = 0; j < nLongitude; j++) {
			for (int i = 0; i < nLatitude; i++) {
				locs.add(new HorizontalPosition(latitudes[i], longitudes[j]));
			}
		}
		HorizontalPosition[] loc = (HorizontalPosition[]) locs.toArray(new Location[0]);
		return loc;
	}

	public static HorizontalPosition[] makeGridWPDense() {
		int startLongitude = 145;
		int endLongitude = 181;
		int startLatitude = -10;
		int endLatitude = 26;
		double dLatitude = 0.5;
		double dLongitude = 0.5;
		int nLongitude = (int) ((endLongitude - startLongitude) / dLongitude);
		int nLatitude = (int) ((endLatitude - startLatitude) / dLatitude);
		double[] longitudes = new double[nLongitude];
		double[] latitudes = new double[nLatitude];
		// System.out.println("yo");
		for (int i = 0; i < nLatitude; i++) {
			latitudes[i] = startLatitude + i * dLatitude;
			// System.out.println(latitudes[i]);
		}

		for (int i = 0; i < nLongitude; i++) {
			longitudes[i] = startLongitude + i * dLongitude;
			// System.out.println(longitudes[i]);
		}
		List<HorizontalPosition> locs = new ArrayList<>();
		for (int j = 0; j < nLongitude; j++) {
			for (int i = 0; i < nLatitude; i++) {
				if ((longitudes[j] - 135) > latitudes[i] && longitudes[j] < 161
						&& (-longitudes[j] + 155) < latitudes[i]) {
					System.out.println(latitudes[i] + " " + longitudes[j]);
					locs.add(new HorizontalPosition(latitudes[i], longitudes[j]));
				}
				if ((-longitudes[j] + 155) < latitudes[i] && longitudes[j] > 160 && longitudes[j] < 166
						&& (-longitudes[j] + 185) > latitudes[i]) {
					System.out.println(latitudes[i] + " " + longitudes[j]);
					locs.add(new HorizontalPosition(latitudes[i], longitudes[j]));
				}
				if ((-longitudes[j] + 185) > latitudes[i] && longitudes[j] > 165
						&& (longitudes[j] - 175) < latitudes[i]) {
					System.out.println(latitudes[i] + " " + longitudes[j]);
					locs.add(new HorizontalPosition(latitudes[i], longitudes[j]));
				}

			}
		}
		HorizontalPosition[] loc = (HorizontalPosition[]) locs.toArray(new Location[0]);
		return loc;
	}

	/**
	 * @param point
	 *            center
	 * @param dr
	 *            radius
	 * @param dLatitude
	 *            [deg] 地理緯度での間隔
	 * @param dLongitude
	 *            [deg]
	 * @return volume
	 */
	public static double getVolume(Location point, double dr, double dLatitude, double dLongitude) {
		double r = point.getR();
		if (r <= 0) {
			System.out.println("location has no R information or invalid R:" + r);
			return 0;
		}
		double latitude = point.getLatitude();// 地理緯度
		double longitude = point.getLongitude();
		Location tmpLoc = point.toLocation(r - 0.5 * dr);
		// tmpLoc.setR(r - 0.5 * dr);
		double startA = Earth.getExtendedShaft(tmpLoc);
		tmpLoc = tmpLoc.toLocation(r + 0.5 * dr);
		double endA = Earth.getExtendedShaft(tmpLoc);
		r = Earth.getExtendedShaft(point);
		// System.out.println(startA + " " + endA);
		// System.exit(0);
		double v = Earth.getVolume(startA, endA, latitude - 0.5 * dLatitude, latitude + 0.5 * dLatitude,
				longitude - 0.5 * dLongitude, longitude + 0.5 * dLongitude);

		return v;
	}

	public static HorizontalPosition[] makeGridAL() {
		HorizontalPosition[] loc = new HorizontalPosition[80];

		for (int i = 1; i < 11; i++) {
			double longitude = 180 + i * 5 - 360;
			for (int j = 0; j < 8; j++) {
				double latitude = 40 + j * 5;
				System.out.println(longitude + " " + latitude);
				loc[j + (i - 1) * 8] = new HorizontalPosition(latitude, longitude);
			}
		}

		return loc;
	}
}
