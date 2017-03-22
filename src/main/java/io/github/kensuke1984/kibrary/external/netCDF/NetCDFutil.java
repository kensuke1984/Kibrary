/**
 * 
 */
package io.github.kensuke1984.kibrary.external.netCDF;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.exception.OutOfRangeException;

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.external.gmt.CrossSection;
import io.github.kensuke1984.kibrary.external.gmt.CrossSectionLine;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
//import data.Earth;
//import manhattan.dsminformation.PolynomialStructure;
//import manhattan.gmt.CrossSection;
//import manhattan.gmt.CrossSectionLine;
//import manhattan.template.HorizontalPosition;
//import manhattan.template.Location;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

/**
 * @version 0.0.1
 * @since 2017/03/15
 * @author Yuki
 *
 */
public class NetCDFutil {
	public NetCDFutil() throws IOException {
		this.netcdffile = NetcdfFile.open("/Users/Anselme/Dropbox/Kenji/FWICarib/global3D/TX2011/TX2011_percent.nc");
		
	}
	
	public static void main(String[] args) {
		try {
			NetCDFutil tx2011 = new NetCDFutil();
			System.out.println(tx2011.getV(new Location(0, 0, 3480.)));
			double[][] section = tx2011.crossSection(new HorizontalPosition(0, 0), 10 * Math.PI / 180, 45 * Math.PI / 180, 2 * Math.PI / 180);
			for (double[] node : section)
				System.out.printf("%.2f %.2f %.2f%n", node[0], node[1], node[2]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public double getV(Location location) {
		double res = -1;
		Variable v = getVariableV();
		int iR = getRIndex(location.getR());
		int iLat = getLatitudeIndex(location.getLatitude());
		int iLon = getLongitudeIndex(location.getLongitude());
		int[] origin = new int[] {iR, iLat, iLon};
		int[] shape = new int[] {1, 1, 1};
		
		try {
			res = v.read(origin, shape).getDouble(0);
		} catch (InvalidRangeException e) {
			System.err.println(location.getR() + " " + origin[0] + " " + origin[1] + " " + origin[2]);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return res;
	}
	
	public double[][] crossSection(HorizontalPosition centerLocation, double theta, double azimuth, double deltaTheta, double[] r) {
		CrossSection grid = new CrossSection(centerLocation, theta, azimuth, deltaTheta, r);
		double[][] section = new double[grid.getLocations().length][3];
		
		Location[] locations = grid.getLocations();
		
		for (int i=0; i < locations.length; i++) {
			double v = this.getV(locations[i]);
			section[i] = new double[3];
			if (locations[i].equals(centerLocation)) {
				section[i][0] = theta;
				section[i][1] = locations[i].getR();
				section[i][2] = v;
			}
			else {
				double az = (centerLocation.getAzimuth(new HorizontalPosition(locations[i].getLatitude(), locations[i].getLongitude())));
				if (az + 0.1 * Math.PI / 180 > azimuth && az - 0.1 * Math.PI / 180 < azimuth) {
					section[i][0] = (centerLocation.getEpicentralDistance(new HorizontalPosition(locations[i].getLatitude(), locations[i].getLongitude())) + theta) * 180 / Math.PI;
					section[i][1] = locations[i].getR();
					section[i][2] = v;
				}
				else {
					section[i][0] = (-centerLocation.getEpicentralDistance(new HorizontalPosition(locations[i].getLatitude(), locations[i].getLongitude())) + theta) * 180 / Math.PI;
					section[i][1] = locations[i].getR();
					section[i][2] = v;
				}
			}
		}
		
		return section;
	}
	
	public double[][] crossSectionDpp(HorizontalPosition centerLocation, double theta, double azimuth, double deltaTheta) {
		double[] r = new double[17];
		for (int i=0; i < r.length; i++)
			r[i] = 3480. + 25 * i;
		return crossSection(centerLocation, theta, azimuth, deltaTheta, r);
	}
	
	public double[][] crossSection(HorizontalPosition centerLocation, double theta, double azimuth, double deltaTheta) throws IOException {
		Variable var = this.getVariableDepth();
		double[] r = new double[(int) var.getSize()];
		for (int i=0; i < r.length; i++)
			r[i] = Earth.EARTH_RADIUS - var.read().getDouble(i);
//		double[] r = new double[57];
//		for (int i=0; i < r.length; i++)
//			r[i] = 3480 + 50 * i;
		 return crossSection(centerLocation, theta, azimuth, deltaTheta, r);
	}
	
	public void makeNcFile(Path location, HorizontalPosition centerLocation, double theta, double azimuth, double deltaTheta) throws IOException {
		NetcdfFileWriter writer = NetcdfFileWriter.createNew(Version.netcdf3, location.toString(), null);
		
		Variable varDepth = this.getVariableDepth();
		
		double[][] section = crossSection(centerLocation, theta, azimuth, deltaTheta);
		CrossSectionLine line = new CrossSectionLine(centerLocation, theta, azimuth, deltaTheta);
		
		Dimension distDim = writer.addDimension(null, "dist", line.getThetaX().length);
		Dimension depthDim = writer.addDimension(null, "depth", (int) varDepth.getSize());
		
		List<Dimension> dimsV = new ArrayList<Dimension>();
		List<Dimension> dimDist = new ArrayList<>();
		List<Dimension> dimDepth = new ArrayList<>();
		dimsV.add(depthDim);
		dimsV.add(distDim);
		dimDist.add(distDim);
		dimDepth.add(depthDim);
		Variable dist = writer.addVariable(null, "dist", DataType.DOUBLE, dimDist);
		Variable depth = writer.addVariable(null, "depth", DataType.DOUBLE, dimDepth);
		Variable vs = writer.addVariable(null, "vs", DataType.DOUBLE, dimsV);
		vs.addAttribute(new Attribute("units", "km/s"));
		
		writer.create();
		writer.close();

		writer = NetcdfFileWriter.openExisting(location.toString());
		vs = writer.findVariable("vs");
		
		int[] shape = vs.getShape();
		ArrayDouble A = new ArrayDouble.D2(shape[0], shape[1]);
		Index index = A.getIndex();
		for (int i=0; i < shape[0]; i++) {
		     for (int j=0; j < shape[1]; j++) {
		    	 double vprem = PolynomialStructure.PREM.getVshAt(Earth.EARTH_RADIUS - varDepth.read().getDouble(i));
		    	 double vdash = (1 - section[i + j * shape[0]][2] / vprem) * 100;
		    	 if (varDepth.read().getDouble(i) == 2900)
		    		 vdash = (1 - section[i + j * shape[0]][2] / PolynomialStructure.PREM.getVshAt(3480.)) * 100;
		    	 vdash = section[i + j * shape[0]][2];
		    	 A.setDouble(index.set(i, j), vdash);
		     }
		}
		int[] origin = new int[2];
		try {
			writer.write(vs, origin, A);
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}
		
		dist = writer.findVariable("dist");
		shape = dist.getShape();
		A = new ArrayDouble.D1(shape[0]);
		index = A.getIndex();
		for (int i=0; i < shape[0]; i++) {
			A.setDouble(index.set(i), i * deltaTheta * 180 / Math.PI);
		}
		origin = new int[1];
		try {
			writer.write(dist, origin, A);
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}
		
		depth = writer.findVariable("depth");
		shape = depth.getShape();
		A = new ArrayDouble.D1(shape[0]);
		index = A.getIndex();
		for (int i=0; i < shape[0]; i++) {
			A.setDouble(index.set(i), Earth.EARTH_RADIUS - varDepth.read().getDouble(i));
		}
		origin = new int[1];
		try {
			writer.write(depth, origin, A);
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}
		
		writer.close();
	}
	
	private Variable getVariableV() {
		return this.getCDFfile().findVariable("dvs");
	}
	
	private Variable getVariableDepth() {
		return this.getCDFfile().findVariable("depth");
	}
	
	private Variable getVariableLongitude() {
		return this.getCDFfile().findVariable("longitude");
	}
	
	private Variable getVariableLatitude() {
		return this.getCDFfile().findVariable("latitude");
	}
	
	private int getRIndex(double radius) {
		double depth = Earth.EARTH_RADIUS - radius;
		int index = -1;
		boolean set = false;
		
		try {
			Variable var = this.getVariableDepth();
			Array value = var.read();
			for (int i=0; i < value.getSize(); i++) {
				double currentdepth = value.getDouble(i);
				if (currentdepth > depth && !set) {
					index = i - 1;
					set = true;
				}
			}
			if (!set)
				index = (int) value.getSize() - 1;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return index;
	}
	
	private int getLatitudeIndex(double latitude) {
		int index = -1;
		
		try {
			Variable var = this.getVariableLatitude();
			Array value = var.read();
			for (int i=0; i < value.getSize() - 1; i++) {
				if (value.getDouble(i + 1) > latitude) {
					index = i;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return index;
	}
	
	private int getLongitudeIndex(double longitude) {
		int index = -1;
		
		try {
			Variable var = this.getVariableLatitude();
			Array value = var.read();
			for (int i=0; i < value.getSize() - 1; i++) {
				if (value.getDouble(i + 1) > longitude) {
					index = i;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return index;
	}
	
	public NetcdfFile getCDFfile() {
		return this.netcdffile;
	}
	
	private NetcdfFile netcdffile;
}

