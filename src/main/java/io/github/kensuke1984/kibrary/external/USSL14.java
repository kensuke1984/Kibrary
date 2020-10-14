package io.github.kensuke1984.kibrary.external;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import opendap.dap.DPrimitive;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

public class USSL14 {
	
	private double[][][] dvsMantle;
	private double[] latMantle, lonMantle, depthMantle;
	private int nDepthMantle, nLonMantle, nLatMantle;
	
	private double[][][] dvsCrust;
	private double[] latCrust, lonCrust, depthCrust;
	
	public static void main(String[] args) {
		double dR = 10.;
		USSL14 model = new USSL14(dR);
		
		Path outpath = Paths.get("/Users/Anselme/Dropbox/Kenji/UPPER_MANTLE/global3D/US-SL-2014/ussl14_trunc6041.txt");
		
		try {
			double rTrunc = 6041.;
			model.writeMantleForSPECFEM(outpath, rTrunc);
//			model.writeMantleForSPECFEM(outpath, 0.);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public USSL14(double dR) {
		try {
			readMantle();
			interpolateRadial(dR);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public double[][][] getDvsMantle() {
		return dvsMantle;
	}
	
	public double[] getLatMantle() {
		return latMantle;
	}
	
	public double[] getLonMantle() {
		return lonMantle;
	}
	
	public double[] getDepthMantle() {
		return depthMantle;
	}
	
	public void writeMantleForSPECFEM(Path outpath, double rTrunc) throws IOException {
		PolynomialStructure prem = PolynomialStructure.PREM;
		
		double depthTrunc = 6371. - rTrunc;
		applyRadialSmoothing(depthTrunc);
		
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int idepth = 0; idepth < nDepthMantle; idepth++) {
			for (int ilon = 0; ilon < nLonMantle; ilon++) {
				for (int ilat = 0; ilat < nLatMantle; ilat++) {
					double dvs = dvsMantle[idepth][ilon][ilat];
//					if (depthMantle[idepth] > depthTrunc)
//						dvs = 0.;
					pw.printf("%.2f %.2f %.2f %.3f %.4f\n",lonMantle[ilon], latMantle[ilat]
						, depthMantle[idepth], dvs
						, prem.getVshAt(6371 - depthMantle[idepth]));
				}
			}
		}
		
		pw.close();
	}
	
	private void applyRadialSmoothing(double depthTrunc) {
		double length = 20.;
		
		int iDepthTrunc = nDepthMantle - 1;
		for (int i = 0; i < nDepthMantle; i++)
			if (depthMantle[i] > depthTrunc) {
				iDepthTrunc = i - 1;
				break;
			}
		
		
		if (iDepthTrunc == nDepthMantle - 1)
			return;
		
		double d = depthMantle[iDepthTrunc + 1] - depthMantle[iDepthTrunc];
//		int iRmax = iDepthTrunc + ((int) (5 * length / d)) + 1;
//		if (iRmax >= nDepthMantle)
//			iRmax = nDepthMantle - 1;
		
		for (int ilon = 0; ilon < nLonMantle; ilon++) {
			for (int ilat = 0; ilat < nLatMantle; ilat++) {
				double dv0 = dvsMantle[iDepthTrunc][ilon][ilat];
				for (int idepth = iDepthTrunc + 1; idepth < nDepthMantle; idepth++) {
					double depth = depthMantle[idepth];
					double depth0 = depthMantle[iDepthTrunc];
					double dv = dv0 * Math.exp(-(depth - depth0) * (depth - depth0) / (2 * length * length));
					dvsMantle[idepth][ilon][ilat] = dv;
				}
			}
		}
	}
	
	public void interpolateRadial(double dR) {
		double minDepth = 30;
		double maxDepth = 1220;
		int nDepth = (int) ((maxDepth - minDepth) / dR);
		
		double[][][] dvsMantleInterp = new double[nDepth][nLonMantle][nLatMantle];
		double[] depthsInterp = new double[nDepth];
		
		for (int idepth = 0; idepth < nDepth; idepth++) {
			double depth = minDepth + dR * idepth;
			depthsInterp[idepth] = depth;
			int iOriginalDepth = getDepthIndex(depth);
			for (int ilon = 0; ilon < nLonMantle; ilon++) {
				for (int ilat = 0; ilat < nLatMantle; ilat++) {
					double dv = 0.;
					if (iOriginalDepth >= 0) {
						double dv0 = dvsMantle[iOriginalDepth][ilon][ilat];
						double dv1 = dvsMantle[iOriginalDepth + 1][ilon][ilat];
						dv = dv1 + (depth - depthMantle[iOriginalDepth + 1]) / 
								(depthMantle[iOriginalDepth] - depthMantle[iOriginalDepth + 1])
								* (dv0 - dv1);
					}
					else {
						dv = dvsMantle[0][ilon][ilat] * (1 - (depthMantle[0] - depth) / (depthMantle[0] - minDepth)); 
					}
					
					dvsMantleInterp[idepth][ilon][ilat] = dv;
				}
			}
		}
		
		dvsMantle = dvsMantleInterp;
		depthMantle = depthsInterp;
		nDepthMantle = nDepth;
	}
	
	private int getDepthIndex(double depth) {
		int index = -1;
		for (int i = 0 ; i < nDepthMantle; i++) {
			if (depthMantle[i] >= depth) {
				index = i - 1;
				break;
			}
		}
		return index;
	}
	
	public void readMantle() throws IOException {
//		String fileName = SpcSAC.class.getClassLoader().getResource("US-SL-2014_percent.nc").toString();
		String fileName = "/Users/Anselme/Dropbox/Kenji/UPPER_MANTLE/global3D/US-SL-2014/US-SL-2014_percent.nc";
		NetcdfFile ncfile = NetcdfFile.open(fileName);
		
		Variable depth = ncfile.findVariable("depth");
		Variable latitude = ncfile.findVariable("latitude");
		Variable longitude = ncfile.findVariable("longitude");
		Variable dvs = ncfile.findVariable("dvs");
		Variable dvp = ncfile.findVariable("dvp");
		
		// ncfile # points
		nDepthMantle = depth.getShape(0);
		nLatMantle = latitude.getShape(0);
		nLonMantle = longitude.getShape(0);
		
		Array depths = depth.read();
		Array latitudes = latitude.read();
		Array longitudes = longitude.read();
		Array dvsArray = dvs.read();
		
		dvsMantle = new double[nDepthMantle][nLonMantle][nLatMantle];
		Index index = dvsArray.getIndex();
		for (int idepth = 0; idepth < nDepthMantle; idepth++) {
			for (int ilon = 0; ilon < nLonMantle; ilon++) {
				for (int ilat = 0; ilat < nLatMantle; ilat++) {
					index.set(idepth, ilat, ilon);
					double tmpDvs = dvsArray.getDouble(index);
					if (tmpDvs == 99999.0)
						tmpDvs = 0.;
					dvsMantle[idepth][ilon][ilat] = tmpDvs; 
				}
			}
		}
		
		latMantle = new double[nLatMantle];
		index = latitudes.getIndex();
		for (int ilat = 0; ilat < nLatMantle; ilat++) {
			index.set(ilat);
			latMantle[ilat] = latitudes.getDouble(index);
		}
		
		lonMantle = new double[nLonMantle];
		index = longitudes.getIndex();
		for (int ilon = 0; ilon < nLonMantle; ilon++) {
			index.set(ilon);
			lonMantle[ilon] = longitudes.getDouble(index);
		}
		
		depthMantle = new double[nDepthMantle];
		index = depths.getIndex();
		for (int idepth = 0; idepth < nDepthMantle; idepth++) {
			index.set(idepth);
			depthMantle[idepth] = depths.getDouble(index);
		}
	}
	
	private void readCrust() throws IOException {
		String fileName = USSL14.class.getClassLoader().getResource("US-CrustVs-2015_kmps.nc").toString();
		NetcdfFile ncfile = NetcdfFile.open(fileName);
		
		Variable depth = ncfile.findVariable("depth");
		Variable latitude = ncfile.findVariable("latitude");
		Variable longitude = ncfile.findVariable("longitude");
		Variable vs = ncfile.findVariable("vs");
		
		// ncfile # points
		int nDepth = depth.getShape(0);
		int nLat = latitude.getShape(0);
		int nLon = longitude.getShape(0);
		
		Array depths = depth.read();
		Array latitudes = latitude.read();
		Array longitudes = longitude.read();
		Array vsArray = vs.read();
		
		dvsCrust = new double[nDepth][nLon][nLat];
		Index index = vsArray.getIndex();
		for (int idepth = 0; idepth < nDepth; idepth++) {
			for (int ilon = 0; ilon < nLon; ilon++) {
				for (int ilat = 0; ilat < nLat; ilat++) {
					index.set(idepth, ilat, ilon);
					dvsCrust[idepth][ilon][ilat] = vsArray.getDouble(index);
				}
			}
		}
		
		latCrust = new double[nDepth];
		index = latitudes.getIndex();
		for (int ilat = 0; ilat < nLat; ilat++) {
			index.set(ilat);
			latCrust[ilat] = latitudes.getDouble(index);
		}
		
		lonCrust = new double[nDepth];
		index = latitudes.getIndex();
		for (int ilon = 0; ilon < nLon; ilon++) {
			index.set(ilon);
			lonCrust[ilon] = longitudes.getDouble(index);
		}
		
		depthCrust = new double[nDepth];
		index = latitudes.getIndex();
		for (int idepth = 0; idepth < nDepth; idepth++) {
			index.set(idepth);
			depthCrust[idepth] = depths.getDouble(index);
		}
	}
	
	private void readMantleAndInterpolate(double dL, double dH, double rmin) throws IOException {
		String fileName = USSL14.class.getClassLoader().getResource("US-SL-2014.nc").toString();
		NetcdfFile ncfile = NetcdfFile.open(fileName);
		
		Variable depth = ncfile.findVariable("depth");
		Variable latitude = ncfile.findVariable("latitude");
		Variable longitude = ncfile.findVariable("longitude");
		Variable dvs = ncfile.findVariable("dvs");
		Variable dvp = ncfile.findVariable("dvp");
		
		// ncfile # points
		int nDepth = depth.getShape(0);
		int nLat = latitude.getShape(0);
		int nLon = longitude.getShape(0);
		
		// interpolation # points
		int mLat = (int) (180. / dL);
		int mLon = (int) (360. / dL);
		int mDepth = (int) ((6371. - rmin) / dH);
		
		double minlat = -90.;
		double minlon = 0.;
		
		Array depths = depth.read();
		Array latitudes = latitude.read();
		Array longitudes = longitude.read();
		
		double[][][] dvs_interp = new double[mDepth][mLon][mLat];
		
		// interpolate
		try {
			int[] size = dvs.getShape();
			for (int idepth = 0; idepth < mDepth; idepth++) {
				double h = idepth * dH;
				int jh = findIndex(depths, h);
				for (int ilon = 0; ilon < mLon; ilon++) {
					double lon = minlon + ilon * dL;
					int jlon = findIndex(longitudes, lon);
					for (int ilat = 0; ilat < mLat; ilat++) {
						double lat = minlat + ilat * dL;
						int jlat = findIndex(latitudes, lat);
						
						int[] origin = new int[] {jh, jlat, jlon};
						dvs_interp[idepth][ilon][ilat] = dvs.read(origin, size).getDouble(0);
					}
				}
			}
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}
		
	}
	
	private int findIndex(Array array, double value) {
		int n = (int) array.getSize();
		double yprevious = array.getDouble(0);
		for (int i = 1; i < n; i++) {
			double y = array.getDouble(i);
			if (value > yprevious && value < y)
				return i;
		}
		return -1;
	}

}
