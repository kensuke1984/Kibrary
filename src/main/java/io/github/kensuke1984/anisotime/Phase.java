package io.github.kensuke1984.anisotime;

import java.util.regex.Pattern;

/**
 * <p>
 * Phase name.
 * </p>
 * 
 * This class is <b>immutable</b>.
 * <p>
 * Waveform is now digitalized. Waveform is divided into parts. each part has up
 * or downgoing, P or S and the partition in which waveform exists.
 * 
 * PdiffXX and SdiffXX can be used. XX is positive double XX is diffractionAngle
 * 
 * 
 * @version 0.1.1
 * 
 * @author Kensuke Konishi
 * 
 */
public class Phase {
	
	//frequently use
	public static final Phase P = Phase.create("P");
	public static final Phase PcP = Phase.create("PcP");
	public static final Phase S = Phase.create("S");
	public static final Phase ScS = Phase.create("ScS");
	
	

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

	private Phase(String phaseName) {
		this.phaseName = phaseName;
		computeParts();
		digitalize();
	}

	/**
	 * @param phase
	 *            phase name
	 * @return phase for input
	 * @throws IllegalArgumentException
	 *             if the phase is invalid
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
			switch (phaseName.charAt(i)) {
			case 'c':
				continue;
			case 'i':
				continue;
			case 'p':
				isP[iCurrentPart] = true;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.MANTLE;
				iCurrentPart++;
				break;
			case 's':
				isP[iCurrentPart] = false;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.MANTLE;
				iCurrentPart++;
				break;
			case 'P':
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
		}

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
				return 0 <= Double.parseDouble(footer);
			} catch (Exception e) {
				return false;
			}
		}

		// check if other letters are used.
		final Pattern others = Pattern.compile("[abd-hj-oqrt-zA-HL-OQ-RT-Z]");
		if (others.matcher(phase).find())
			return false;

		// first letter is sSpP
		final Pattern firstLetter = Pattern.compile("^[^sSPp]");
		if (firstLetter.matcher(phase).find())
			return false;

		// final letter is psSP
		final Pattern finalLetter = Pattern.compile("[^psSP]$");
		if (finalLetter.matcher(phase).find())
			return false;

		// p s must be the first letter
		final Pattern ps = Pattern.compile(".[ps]");
		if (ps.matcher(phase).find())
			return false;

		// c must be next to PS
		final Pattern nextC = Pattern.compile("[^PS]c|c[^PS]|[^PSps][PS]c|c[PS][^PS]");
		if (nextC.matcher(phase).find())
			return false;

		final Pattern nextK = Pattern.compile("[^PSKiIJ]K[^PSKiIJ]|[^psPS][PS]K|K[PS][^PS]");
		if (nextK.matcher(phase).find())
			return false;

		final Pattern nextJ = Pattern.compile("[^IJK]J|J[^IJK]");
		if (nextJ.matcher(phase).find())
			return false;

		final Pattern smallI = Pattern.compile("[^K]i|i[^K]|[^PSK]Ki|iK[^KPS]");
		if (smallI.matcher(phase).find())
			return false;

		final Pattern largeI = Pattern.compile("[^IJK]I|I[^IJK]");
		if (largeI.matcher(phase).find())
			return false;

		// phase turning R is above the CMB
		final Pattern mantleP = Pattern.compile("^P$|^P[PS]|[psPS]P$|[psPS]P[PS]");
		final Pattern mantleS = Pattern.compile("^S$|^S[PS]|[psPS]S$|[psPS]S[PS]");

		// phase reflected at the cmb
		final Pattern cmbP = Pattern.compile("Pc|cP");
		final Pattern cmbS = Pattern.compile("Sc|cS");
		// phase turning R r <cmb
		final Pattern outercoreP = Pattern.compile("PK|KP");
		final Pattern outercoreS = Pattern.compile("SK|KS");
		// phase turning R icb < r < cmb
		final Pattern outercore = Pattern.compile("[PSK]K[PSK]");

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
		Partition s = sReaches();
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
					if (!(phaseName.contains("I") || phaseName.contains("J") || phaseName.contains("i")))
						return false;
			if (!p.shallow(pTurning))
				return false;
		}
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

			if (!s.shallow(sTurning))
				return false;
		}
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
	 * @return how many times P wave travels in the mantle. each down or upgoing
	 *         is 0.5
	 */
	double mantleP() {
		double pNum = 0;
		for (int i = 0; i < nPart; i++)
			if (partition[i].equals(Partition.MANTLE) && isP[i])
				pNum += 0.5;

		return phaseName.charAt(0) == 'p' ? pNum - 0.5 : pNum;
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
	 *            index of the target part
	 * @return if it is p phase in i th part.
	 */
	boolean partIsP(int i) {
		return isP[i];
	}

	/**
	 * @param i
	 *            index of the target
	 * @return if it is down going in i th part.
	 */
	boolean partIsDownGoing(int i) {
		return isDownGoing[i];
	}

	/**
	 * @param i
	 *            index of the target
	 * @return the partition in which i th part is
	 */
	Partition partIs(int i) {
		return partition[i];
	}

	/**
	 * how many times S wave travels in the mantle. each down or upgoing is 0.5
	 * 
	 * @return
	 */
	double mantleS() {
		double sNum = 0;
		for (int i = 0; i < nPart; i++)
			if (partition[i].equals(Partition.MANTLE) && !isP[i])
				sNum += 0.5;

		return phaseName.charAt(0) == 's' ? sNum - 0.5 : sNum;
	}

	/**
	 * how many times wave travels in the outer core. Each down or up going is
	 * considered as 0.5
	 * 
	 * @return the times K phase travels in the outer core.
	 */
	double outerCore() {
		double outerCoreNum = 0;
		for (int i = 0; i < nPart; i++)
			if (partition[i].equals(Partition.OUTERCORE))
				outerCoreNum += 0.5;

		return outerCoreNum;
	}

	/**
	 * how many times P wave travels in the inner core. Each down or upgoing is
	 * considered as 0.5
	 * 
	 * @return the times P wave travels in the inner core
	 */
	double innerCoreP() {
		int numI = 0;
		for (int i = 0; i < phaseName.length(); i++)
			if (phaseName.charAt(i) == 'I')
				numI++;
		return numI;
	}

	/**
	 * how many times S wave travels in the inner core. Each down or up going is
	 * considered as 0.5
	 * 
	 * @return the times S wave travels in the inner core.
	 */
	double innerCoreS() {
		int numJ = 0;
		for (int i = 0; i < phaseName.length(); i++)
			if (phaseName.charAt(i) == 'J')
				numJ++;
		return numJ;
	}

	@Override
	public String toString() {
		return phaseName;
	}
}
