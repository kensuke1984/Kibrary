package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.math.geometry.ConvexPolygon;
import io.github.kensuke1984.kibrary.math.geometry.Point2D;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SelectLocalVoxel {

	public static void main(String[] args) throws IOException {
		Path unknownsPath = Paths.get(args[0]);
		
		HorizontalPosition lowerLeftPoint = new HorizontalPosition(23, -107);
		HorizontalPosition upperRightPoint = new HorizontalPosition(30, -100);
		double minDepth = 597;
		double maxDepth = 820;
		
		List<UnknownParameter> parameterList = UnknownParameterFile.read(unknownsPath);
		
		Path outpath = Paths.get("maskSelectUnknown" + Utilities.getTemporaryString() + ".inf");
		PrintWriter pw = new PrintWriter(outpath.toFile());
		
		for (UnknownParameter p : parameterList) {
			int bit = 0;
			if (contains(p.getLocation(), lowerLeftPoint, upperRightPoint, minDepth, maxDepth))
				bit = 1;
			pw.println(bit);
		}
		
		pw.close();
	}
	
	public static boolean contains(Location l, HorizontalPosition lowerLeftPoint, HorizontalPosition upperRightPoint, double minDepth, double maxDepth) {
		double depth = 6371. - l.getR();
		if (depth < minDepth || depth > maxDepth)
			return false;
		
		Point2D p1 = lowerLeftPoint.toPoint2D();
		Point2D p3 = upperRightPoint.toPoint2D();
		Point2D p2 = new Point2D(upperRightPoint.getLongitude(), lowerLeftPoint.getLatitude());
		Point2D p4 = new Point2D(lowerLeftPoint.getLongitude(), upperRightPoint.getLatitude());
		Point2D p = l.toPoint2D();
		
		ConvexPolygon polygon = new ConvexPolygon(p1, p2, p3, p4);
		
		return polygon.contains(p);
	}
}
