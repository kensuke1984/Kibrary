package anisotime;

import java.util.regex.Pattern;

/**
 * Phase name.
 * 
 * @author kensuke
 * @since 2014/06/13
 * @version 0.0.1
 * 
 * @version 0.0.2
 * @since 2014/7/10 install Pdiff, Sdiff
 * 
 * 
 * @version 0.1.0
 * @since 2014/7/14 Waveform is now digitalized. Waveform is divided into parts.
 *        each part has up or downgoing, P or S and the partition in which
 *        waveform exists.
 * 
 * @version 0.1.1
 * @since 2014/7/21 {@link #equals(Object)} {@link #hashCode()}installed
 * 
 * @version 0.1.2
 * @since 2014/7/23 PdiffXX and SdiffXX can be used. XX is positive double XX is
 *        diffractionAngle
 * 
 * @version 0.1.3
 * @since 2014/8/6 Modifies member signatures
 * 
 * 
 * @version 0.1.4
 * @since 2015/1/15 {@link #isDiffracted()} installed. [ps][PS]diffXX is now
 *        acceptable.
 * 
 * @version 0.1.5
 * @since 2015/5/6 preaches modified.
 * 
 * 
 * 
 */
public class Phase {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((phaseName == null) ? 0 : phaseName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Phase other = (Phase) obj;
		if (phaseName == null) {
			if (other.phaseName != null)
				return false;
		} else if (!phaseName.equals(other.phaseName))
			return false;
		return true;
	}

	/**
	 * phase name
	 */
	private String phaseName;

	private Phase(String phase) {
		this.phaseName = phase;
		// System.out.println(phase);
		computeParts();
		digitalize();
	}

	/**
	 * @param phase
	 *            phase name
	 * @return null if phase is an invalid name
	 */
	public static Phase create(String phase) {
		if (isValid(phase))
			return new Phase(phase);
		throw new IllegalArgumentException("Invalid phase name " + phase);
	}

	/**
	 * the number of Parts part is for each layer and each direction. if there
	 * is a turning point in one layer the number of parts will increase for
	 * example S wave has 2 parts.
	 * 
	 */
	private int nPart;

	/**
	 * if each part waveforms go deeper or shallower. if waveforms go deeper
	 * true, if else false.
	 */
	private boolean[] isDownGoing;

	/**
	 * if each part is P wave or S wave P:true, S:false
	 */
	private boolean[] isP;

	/**
	 * where waveform is at each part
	 */
	private Partition[] partition;

	/**
	 * @return angle of diffraction [rad]
	 */
	double getDiffractionAngle() {
		return diffractionAngle;
	}

	private void computeParts() {
		int reflection = 0;
		// waveform goes to a deeper part.
		int zoneMove = 0;
		for (int i = 0; i < phaseName.length(); i++)
			if (phaseName.charAt(i) == 'c' || phaseName.charAt(i) == 'i')
				reflection++;
			else if (phaseName.charAt(i) == 'K' && (phaseName.charAt(i - 1) == 'P' || phaseName.charAt(i - 1) == 'S'))
				zoneMove++;
			else if ((phaseName.charAt(i) == 'I' || phaseName.charAt(i) == 'J') && phaseName.charAt(i - 1) == 'K')
				zoneMove++;
		// reflection++;
		nPart = (phaseName.length() - reflection * 2 - zoneMove) * 2;
		if (phaseName.charAt(0) == 'p' || phaseName.charAt(0) == 's')
			nPart -= 1;

		// diffraction
		if (phaseName.contains("diff")) {
			int header = 0;
			if (phaseName.startsWith("P") || phaseName.startsWith("S")) {
				header = 5;
				nPart = 2;
			} else {
				header = 6;
				nPart = 3;
			}
			if (phaseName.length() != header)
				diffractionAngle = Math.toRadians(Double.parseDouble(phaseName.substring(header)));
		}

		isDownGoing = new boolean[nPart];
		isP = new boolean[nPart];
		partition = new Partition[nPart];
		// System.out.println(nPart);
	}

	boolean isDiffracted() {
		return phaseName.contains("diff");
	}

	/**
	 * Angle of Diffraction
	 */
	private double diffractionAngle;

	private void digitalize() {
		// P
		int iCurrentPart = 0;
		for (int i = 0; i < phaseName.length(); i++) {
			char s = phaseName.charAt(i);
			switch (s) {
			case 'c':
				continue;
			case 'i':
				continue;
			case 'p':
				// System.out.println("p");
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.MANTLE;
				iCurrentPart++;
				break;
			case 's':
				// System.out.println("s");
				isP[iCurrentPart] = false;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.MANTLE;
				iCurrentPart++;
				break;
			case 'P':
				// System.out.println("P");
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = i == 0
						|| (phaseName.charAt(i - 1) != 'K' && phaseName.charAt(i - 1) != 'c');
				partition[iCurrentPart] = Partition.MANTLE;
				if (i == 0 || ((phaseName.charAt(i - 1) != 'c' && phaseName.charAt(i - 1) != 'K')))
					if ((i + 1) == phaseName.length()
							|| (phaseName.charAt(i + 1) != 'c' && phaseName.charAt(i + 1) != 'K')) {
						iCurrentPart++;
						isDownGoing[iCurrentPart] = false;
						isP[iCurrentPart] = true;
						partition[iCurrentPart] = Partition.MANTLE;
					}
				iCurrentPart++;
				break;
			case 'S':
				isP[iCurrentPart] = false;
				isDownGoing[iCurrentPart] = i == 0
						|| (phaseName.charAt(i - 1) != 'K' && phaseName.charAt(i - 1) != 'c');
				partition[iCurrentPart] = Partition.MANTLE;
				if (i == 0 || ((phaseName.charAt(i - 1) != 'c' && phaseName.charAt(i - 1) != 'K')))
					if ((i + 1) == phaseName.length()
							|| (phaseName.charAt(i + 1) != 'c' && phaseName.charAt(i + 1) != 'K')) {
						iCurrentPart++;
						partition[iCurrentPart] = Partition.MANTLE;
						isP[iCurrentPart] = false;
						isDownGoing[iCurrentPart] = false;
					}
				iCurrentPart++;
				break;
			case 'I':
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = true;
				partition[iCurrentPart] = Partition.INNERCORE;
				iCurrentPart++;
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.INNERCORE;
				iCurrentPart++;
				break;
			case 'J':
				isP[iCurrentPart] = false;
				isDownGoing[iCurrentPart] = true;
				partition[iCurrentPart] = Partition.INNERCORE;
				iCurrentPart++;
				isP[iCurrentPart] = false;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.INNERCORE;
				iCurrentPart++;
				break;
			case 'K':
				// System.out.println("K " + iCurrentPart);
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = phaseName.charAt(i - 1) != 'i' && phaseName.charAt(i - 1) != 'I'
						&& phaseName.charAt(i - 1) != 'J';
				partition[iCurrentPart] = Partition.OUTERCORE;
				// isDownGoing[iCurrentPart] = true;
				if (phaseName.charAt(i - 1) != 'i' && phaseName.charAt(i - 1) != 'I' && phaseName.charAt(i - 1) != 'J')
					if (phaseName.charAt(i + 1) != 'i' && phaseName.charAt(i + 1) != 'I'
							&& phaseName.charAt(i + 1) != 'J') {
						iCurrentPart++;
						isDownGoing[iCurrentPart] = false;
						isP[iCurrentPart] = true;
						partition[iCurrentPart] = Partition.OUTERCORE;
					}
				iCurrentPart++;
				break;
			}
			// System.out.println(s);
		}

		// for (int i = 0; i < nPart; i++)
		// System.out.print(isP[i]?"P ":"S ");
		// System.out.println();
		// for (int i = 0; i < nPart; i++)
		// System.out.print((isDownGoing[i]?"down ":"up "));
		// System.out.println();
		// for (int i = 0; i < nPart; i++)
		// System.out.print(partition[i]+" ");
	}

	/**
	 * @param phase
	 *            phase name
	 * @return if is wellknow phase?
	 */
	static boolean isValid(String phase) {
		if (phase.equals(""))
			return false;

		// diffraction phase
		if (phase.contains("diff")) {
			int header = 0;
			if (phase.startsWith("P") || phase.startsWith("S"))
				header = 5;
			else if (phase.startsWith("p") || phase.startsWith("s"))
				header = 6;
			else
				return false;
			if (phase.length() == header)
				return true;
			String footer = phase.substring(header);
			try {
				double d = Double.parseDouble(footer);
				return 0 <= d;
			} catch (Exception e) {
				return false;
			}
		}

		// check if other letters are used.
		Pattern others = Pattern.compile("[abd-hj-oqrt-zA-HL-OQ-RT-Z]");
		if (others.matcher(phase).find())
			return false;

		// first letter is sSpP
		Pattern firstLetter = Pattern.compile("^[^sSPp]");
		if (firstLetter.matcher(phase).find())
			return false;

		// final letter is psSP
		Pattern finalLetter = Pattern.compile("[^psSP]$");
		if (finalLetter.matcher(phase).find())
			return false;

		// p s must be the first letter
		Pattern ps = Pattern.compile(".[ps]");
		if (ps.matcher(phase).find())
			return false;

		// c must be next to PS
		Pattern nextC = Pattern.compile("[^PS]c|c[^PS]|[^PSps][PS]c|c[PS][^PS]");
		if (nextC.matcher(phase).find())
			return false;

		Pattern nextK = Pattern.compile("[^PSKiIJ]K[^PSKiIJ]|[^psPS][PS]K|K[PS][^PS]");
		if (nextK.matcher(phase).find())
			return false;

		Pattern nextJ = Pattern.compile("[^IJK]J|J[^IJK]");
		if (nextJ.matcher(phase).find())
			return false;

		Pattern smallI = Pattern.compile("[^K]i|i[^K]|[^PSK]Ki|iK[^KPS]");
		if (smallI.matcher(phase).find())
			return false;

		Pattern largeI = Pattern.compile("[^IJK]I|I[^IJK]");
		if (largeI.matcher(phase).find())
			return false;

		// phase turning R is above the CMB
		Pattern mantleP = Pattern.compile("^P$|^P[PS]|[psPS]P$|[psPS]P[PS]");
		Pattern mantleS = Pattern.compile("^S$|^S[PS]|[psPS]S$|[psPS]S[PS]");

		// phase reflected at the cmb
		Pattern cmbP = Pattern.compile("Pc|cP");
		Pattern cmbS = Pattern.compile("Sc|cS");
		// phase turning R r <cmb
		Pattern outercoreP = Pattern.compile("PK|KP");
		Pattern outercoreS = Pattern.compile("SK|KS");
		// phase turning R icb < r < cmb
		Pattern outercore = Pattern.compile("[PSK]K[PSK]");

		// phase reflected at the icb
		boolean icb = phase.contains("i");
		boolean innercoreP = phase.contains("I");
		boolean innercoreS = phase.contains("J");

		if (mantleP.matcher(phase).find())
			if (cmbP.matcher(phase).find() || outercoreP.matcher(phase).find() || innercoreP)
				return false;

		if (mantleS.matcher(phase).find())
			if (cmbS.matcher(phase).find() || outercoreS.matcher(phase).find() || innercoreS)
				return false;

		if (outercore.matcher(phase).find())
			if (innercoreP || icb || innercoreS)
				return false;

		return true;
	}

	boolean turningRValidity(Partition pTurning, Partition sTurning) {
		Partition p = pReaches();
		// System.out.println("P reaches "+p);
		Partition s = sReaches();
		// System.out.println("S reaches "+s);
		// System.out.println("P reaches "+p+", S reaches "+s);
		if (p != null) {
			if (pTurning == null)
				return false;
			if (p.equals(Partition.MANTLE)) {
				if (!pTurning.equals(Partition.MANTLE))
					return false;
			} else if (p.equals(Partition.OUTERCORE))
				if (!pTurning.equals(Partition.OUTERCORE))
					return false;
			if (pTurning.equals(Partition.INNERCORE))
				if (phaseName.contains("K"))
					if (!(phaseName.contains("I") || phaseName.contains("J") || phaseName.contains("i"))) {
						// System.out.println("!");
						return false;
					}

			// System.out.println("hi");
			if (!p.shallow(pTurning))
				return false;
		}
		// System.out.println("hi");
		if (s != null) {
			if (sTurning == null)
				return false;
			if (s.equals(Partition.MANTLE))
				if (!sTurning.equals(Partition.MANTLE))
					return false;
			if (sTurning.equals(Partition.INNERCORE))
				if (phaseName.contains("K"))
					if (pTurning.shallow(Partition.CORE_MANTLE_BOUNDARY))
						return false;
					else if (pTurning.shallow(Partition.OUTERCORE))
						return !(phaseName.contains("I") || phaseName.contains("J") || phaseName.contains("i"));
					else if (pTurning.equals(Partition.INNER_CORE_BAUNDARY))
						return phaseName.contains("i");
					else
						return phaseName.contains("I") || phaseName.contains("J") || phaseName.contains("i");

			// System.out.println("?");
			if (!s.shallow(sTurning))
				return false;
		}
		// System.out.println("?");
		return true;
	}

	/**
	 * deepest part of P phase
	 * 
	 * @return null if P phase does not exist.
	 */
	Partition pReaches() {
		if (phaseName.contains("Sdiff"))
			return null;
		if (phaseName.contains("I"))
			return Partition.INNERCORE;
		if (phaseName.contains("i") || phaseName.contains("J"))
			return Partition.INNER_CORE_BAUNDARY;
		if (phaseName.contains("K"))
			return Partition.OUTERCORE;
		if (!phaseName.contains("P") && !phaseName.contains("p"))
			return null;
		if (phaseName.contains("Pc") || phaseName.contains("cP") || phaseName.contains("diff"))
			return Partition.CORE_MANTLE_BOUNDARY;
		return Partition.MANTLE;
	}

	/**
	 * deepest part of s Phase
	 * 
	 * @return null if S phase does not exist.
	 */
	Partition sReaches() {
		if (phaseName.contains("J"))
			return Partition.INNERCORE;
		if (!phaseName.contains("S"))
			return null;
		if (phaseName.contains("Sc") || phaseName.contains("cS") || phaseName.contains("KS") || phaseName.contains("SK")
				|| phaseName.contains("diff"))
			return Partition.CORE_MANTLE_BOUNDARY;

		return Partition.MANTLE;
	}

	/**
	 * how many times P wave travels in the mantle. each down or upgoing is 0.5
	 * 
	 * @return
	 */
	double mantleP() {
		double pNum = 0;
		for (int i = 0; i < nPart; i++) {
			if (partition[i].equals(Partition.MANTLE))
				if (isP[i])
					pNum += 0.5;
		}
		if (phaseName.charAt(0) == 'p')
			pNum -= 0.5;

		return pNum;
	}

	/**
	 * {@link #nPart}
	 * 
	 * @return the number of part
	 */
	int getNPart() {
		return nPart;
	}

	/**
	 * @param i
	 * @return if it is p phase in i th part.
	 */
	boolean partIsP(int i) {
		return isP[i];
	}

	/**
	 * @param i
	 * @return if it is down going in i th part.
	 */
	boolean partIsDownGoing(int i) {
		return isDownGoing[i];
	}

	/**
	 * @param i
	 * @return the partition in which i th part is
	 */
	Partition partIs(int i) {
		return partition[i];
	}

	// double mantleP() {
	// Pattern halfPattern = Pattern.compile("P[cK]|[cK]P");
	// int halfP = 0;
	// int numP = 0;
	// Matcher m = halfPattern.matcher(phase);
	// for (int i = 0; i < phase.length(); i++)
	// if (phase.charAt(i) == 'P')
	// numP++;
	// for (int start = 0;;) {
	// if (!m.find(start))
	// break;
	// start = m.start() + 1;
	// halfP++;
	// // Pystem.out.println(start-1);
	// }
	// double num = numP - 0.5 * halfP;
	// // System.out.println("P: " + num);
	// return num;
	// }
	/**
	 * how many times S wave travels in the mantle. each down or upgoing is 0.5
	 * 
	 * @return
	 */
	double mantleS() {
		double sNum = 0;
		for (int i = 0; i < nPart; i++) {
			// System.out.println(partition[i]+" "+isP[i]);
			if (partition[i].equals(Partition.MANTLE))
				if (!isP[i])
					sNum += 0.5;
		}
		if (phaseName.charAt(0) == 's')
			sNum -= 0.5;
		// System.out.println(sNum);
		return sNum;
	}

	// /**
	// * @return
	// */
	// double mantleS() {
	// Pattern halfPattern = Pattern.compile("S[cK]|[cK]S");
	// int halfS = 0;
	// int numS = 0;
	// Matcher m = halfPattern.matcher(phase);
	// for (int i = 0; i < phase.length(); i++)
	// if (phase.charAt(i) == 'S')
	// numS++;
	// for (int start = 0;;) {
	// if (!m.find(start))
	// break;
	// start = m.start() + 1;
	// halfS++;
	// // System.out.println(start-1);
	// }
	// double num = numS - 0.5 * halfS;
	// // System.out.println("S: " + num);
	// return num;
	// }
	/**
	 * how many times wave travels in the outercore. each down or upgoing is 0.5
	 * 
	 * @return
	 */
	double outerCore() {
		double outerCoreNum = 0;
		for (int i = 0; i < nPart; i++) {
			if (partition[i].equals(Partition.OUTERCORE))
				outerCoreNum += 0.5;
		}
		return outerCoreNum;
	}

	// double outerCore() {
	// Pattern halfPattern = Pattern.compile("[ijIJ]K|K[IJij]");
	// int halfK = 0;
	// int numK = 0;
	// Matcher m = halfPattern.matcher(phase);
	// for (int i = 0; i < phase.length(); i++)
	// if (phase.charAt(i) == 'K')
	// numK++;
	// for (int start = 0;;) {
	// if (!m.find(start))
	// break;
	// start = m.start() + 1;
	// halfK++;
	// // System.out.println(start-1);
	// }
	// double num = numK - 0.5 * halfK;
	// // System.out.println("K: " + num);
	// return num;
	// }
	/**
	 * how many times P wave travels in the innercore. each down or upgoing is
	 * 0.5
	 * 
	 * @return
	 */
	double innerCoreP() {
		int numI = 0;
		for (int i = 0; i < phaseName.length(); i++)
			if (phaseName.charAt(i) == 'I')
				numI++;

		// System.out.println("I: " + numI);
		return numI;
	}

	/**
	 * how many times S wave travels in the innercore. each down or upgoing is
	 * 0.5
	 * 
	 * @return
	 */
	double innerCoreS() {
		int numJ = 0;
		for (int i = 0; i < phaseName.length(); i++)
			if (phaseName.charAt(i) == 'J')
				numJ++;

		// System.out.println("J: " + numJ);
		return numJ;
	}

	@Override
	public String toString() {
		return phaseName;
	}
}
