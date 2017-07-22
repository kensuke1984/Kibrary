package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Earth;

public class taupModelMaker {

	public static void main(String[] args) {
		PolynomialStructure model = PolynomialStructure.PREM;
		
		for (int i = 0; i < 41; i++) {
			double r = 6346.600 - 5.*i;
			System.out.printf("            %.0f.  %.2f  %.2f  %.2f    %.1f      %.1f  %.2f  %.2f  %.5f%n"
					,r * 1e3
					,model.getRhoAt(r) * 1e3
					,model.getVpvAt(r) * 1e3
					,model.getVsvAt(r) * 1e3
					,model.getQkappaAt(r)
					,model.getQmuAt(r)
					,model.getVphAt(r) * 1e3
					,model.getVshAt(r) * 1e3
					,model.getEtaAt(r));
		}
		System.exit(0);
		
		int n = 10000;
		for (int i = 0; i <= n; i++) {
			double dr = Earth.EARTH_RADIUS / n;
			double r = Earth.EARTH_RADIUS - i * dr;
			double depth = Earth.EARTH_RADIUS - r;
			int izone = model.getiZoneOf(r);
			if (r > 0) {
				if (izone != model.getiZoneOf(r - dr)) {
					if (Math.abs(model.getVshAt(r) - model.getVshAt(r - dr)) > 0.05) {
						double rZone = model.getRMinOf(izone);
						double depthZone = Earth.EARTH_RADIUS - rZone;
						System.out.printf("%.4f %.4f %.4f %.4f\n", depth, model.getVphAt(r), model.getVshAt(r), model.getRhoAt(r));
						System.out.printf("%.4f %.4f %.4f %.4f\n", depthZone, model.getVphAt(rZone + .1), model.getVshAt(rZone + .1), model.getRhoAt(rZone + .1));
						System.out.printf("%.4f %.4f %.4f %.4f\n", depthZone, model.getVphAt(rZone - .1), model.getVshAt(rZone - .1), model.getRhoAt(rZone - .1));
					}
					else
						System.out.printf("%.4f %.4f %.4f %.4f\n", depth, model.getVphAt(r), model.getVshAt(r), model.getRhoAt(r));
				}
				else 
					System.out.printf("%.4f %.4f %.4f %.4f\n", depth, model.getVphAt(r), model.getVshAt(r), model.getRhoAt(r));
			}
			else
				System.out.printf("%.4f %.4f %.4f %.4f\n", depth, model.getVphAt(r), model.getVshAt(r), model.getRhoAt(r));
		}
	}

}
