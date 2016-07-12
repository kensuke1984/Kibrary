package io.github.kensuke1984.anisotime;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

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
 * ???PdiffXX and ???SdiffXX can be used. XX is positive double XX is
 * diffractionAngle diff must be the last part.
 * 
 * Diffraction can only happen at the final part.
 * 
 * 
 * If there is a number (n) in the name. the next letter to the number will be
 * repeated n times. (e.g. S3KS &rarr; SKKKS)
 * 
 * @version 0.1.5
 * 
 *          TODO TauP のように 任意の深さの反射 ADDEDBOUNDARY
 * 
 * @author Kensuke Konishi
 * 
 */
public class Phase {

	// no use letters
	private static final Pattern others = Pattern.compile("[abeghj-oqrt-zA-HL-OQ-RT-Z]");
	private static final Pattern number = Pattern.compile("\\d+");

	// first letter is sSpP
	private static final Pattern firstLetter = Pattern.compile("^[^psSP]");
	// static final letter is psSP
	private static final Pattern finalLetter = Pattern.compile("[^psSP]$");
	// p s must be the first letter
	private static final Pattern ps = Pattern.compile(".[ps]");
	// c must be next to PS
	private static final Pattern nextC = Pattern.compile("[^PS]c|c[^PS]|[^PSps][PS]c|c[PS][^PS]");
	private static final Pattern nextK = Pattern.compile("[^PSKiIJ]K[^PSKiIJ]|[^psPS][PS]K|K[PS][^PS]");
	private static final Pattern nextJ = Pattern.compile("[^IJK]J|J[^IJK]");
	private static final Pattern smallI = Pattern.compile("[^Kd]i|i[^fK]|[^PSK]Ki|iK[^KPS]");
	private static final Pattern largeI = Pattern.compile("[^IJK]I|I[^IJK]");

	// phase turning R is above the CMB
	private static final Pattern mantleP = Pattern.compile("^P$|^P[PS]|[psPS]P$|[psPS]P[PS]");
	private static final Pattern mantleS = Pattern.compile("^S$|^S[PS]|[psPS]S$|[psPS]S[PS]");

	// diffraction phase
	private static final Pattern pDiff = Pattern.compile("Pdiff\\d*(\\.\\d+)?$");
	private static final Pattern sDiff = Pattern.compile("Sdiff\\d*(\\.?\\d+)?$");
	private static final Pattern diffRule = Pattern.compile("diff.+diff|P.*Pdiff|S.*Sdiff|diff.*[^\\d]$");

	// phase reflected at the cmb
	private static final Pattern cmbP = Pattern.compile("Pc|cP");
	private static final Pattern cmbS = Pattern.compile("Sc|cS");

	// phase turning R r <cmb
	private static final Pattern outercoreP = Pattern.compile("PK|KP");
	private static final Pattern outercoreS = Pattern.compile("SK|KS");

	// phase turning R icb < r < cmb, i.e., the phase does not go to inner core.
	private static final Pattern outercore = Pattern.compile("[PSK]K[PSK]");

	// frequently use
	public static final Phase p = Phase.create("p");
	public static final Phase P = Phase.create("P");
	public static final Phase PcP = Phase.create("PcP");
	public static final Phase PKP = Phase.create("PKP");
	public static final Phase PKiKP = Phase.create("PKiKP");
	public static final Phase PKIKP = Phase.create("PKIKP");
	public static final Phase s = Phase.create("s");
	public static final Phase S = Phase.create("S");
	public static final Phase ScS = Phase.create("ScS");
	public static final Phase SKS = Phase.create("SKS");
	public static final Phase SKiKS = Phase.create("SKiKS");
	public static final Phase SKIKS = Phase.create("SKIKS");

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((compiledName == null) ? 0 : compiledName.hashCode());
		return result;
	}

	/**
	 * Compiled name is same or not.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Phase other = (Phase) obj;
		if (compiledName == null) {
			if (other.compiledName != null)
				return false;
		} else if (!compiledName.equals(other.compiledName))
			return false;
		return true;
	}

	/**
	 * @return P-SV (true), SH (false)
	 */
	public boolean isPSV() {
		return psv;
	}

	/**
	 * If this is P-SV(true) or SH(false).
	 */
	private final boolean psv;

	/**
	 * (input) phase name e.g. (SmKS)
	 */
	private final String phaseName;

	/**
	 * name for parsing e.g. (SKKKKKS)
	 */
	private final String compiledName;

	/**
	 * @param phaseName
	 *            of the phase (e.g. SKKS)
	 * @param compiledName
	 *            of the phase (e.g. S2KS)
	 * @param psv
	 *            true:P-SV, false:SH
	 */
	private Phase(String phaseName, String compiledName, boolean psv) {
		this.phaseName = phaseName;
		this.compiledName = compiledName;
		this.psv = psv;
		countParts();
		digitalize();
		setPropagation();
	}

	/**
	 * @param name
	 *            phase name
	 * @param sv
	 *            true:P-SV, false:SH. If the phase contains "P" or "K", it is
	 *            ignored and always is true.
	 * @return phase for input
	 * @throws IllegalArgumentException
	 *             if the phase is invalid
	 */
	public static Phase create(String name, boolean... sv) {
		if (1 < sv.length)
			throw new IllegalArgumentException("SV or not");
		String compiledName = compile(name);
		if (isValid(compiledName))
			return new Phase(name, compiledName, name.contains("P") || name.contains("K") || (sv.length != 0 && sv[0]));
		throw new IllegalArgumentException("Invalid phase name " + name);
	}

	private static String compile(String name) {
		try {
			Matcher m = number.matcher(name);
			int dPos = name.indexOf('d');
			while (m.find() && m.start() < dPos) {
				int n = Integer.parseInt(m.group());
				char c = name.charAt(m.end());
				char[] chars = new char[n - 1];
				Arrays.fill(chars, c);
				String rep = String.copyValueOf(chars);
				name = m.replaceFirst(rep);
				m = number.matcher(name);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid phase name " + name);
		}
		return name;
	}

	/**
	 * the number of Parts part is for each layer and each direction. if there
	 * is a turning point in one layer the number of parts will increase for
	 * example S wave has 2 parts. Sdiff has 3 parts.
	 * 
	 */
	private int nPart;

	/**
	 * If each part waveforms go deeper(true) or shallower(false). When the part
	 * is diffraction, it is false,
	 */
	private boolean[] isDownGoing;

	/**
	 * Which phase is at each part
	 */
	private Partition[] partition;

	private PhasePart[] phaseParts;

	/**
	 * @return angle of diffraction [rad] TODO
	 */
	double getDiffractionAngle() {
		return diffractionAngle;
	}

	private void countParts() {
		int reflection = 0;
		// waveform goes to a deeper part.
		int zoneMove = 0;
		for (int i = 0; i < compiledName.length(); i++)
			if (compiledName.charAt(i) == 'd')
				break;
			else if (compiledName.charAt(i) == 'c' || compiledName.charAt(i) == 'i')
				reflection++;
			else if (compiledName.charAt(i) == 'K'
					&& (compiledName.charAt(i - 1) == 'P' || compiledName.charAt(i - 1) == 'S'))
				zoneMove++;
			else if ((compiledName.charAt(i) == 'I' || compiledName.charAt(i) == 'J')
					&& compiledName.charAt(i - 1) == 'K')
				zoneMove++;
		// reflection++;
		int dPos = compiledName.indexOf('d');
		nPart = ((0 < dPos ? dPos : compiledName.length()) - reflection * 2 - zoneMove) * 2;
		if (compiledName.charAt(0) == 'p' || compiledName.charAt(0) == 's')
			nPart--;

		// diffraction
		if (0 < dPos) {
			nPart++;
			if (dPos + 4 < compiledName.length())
				diffractionAngle = Math.toRadians(Double.parseDouble(compiledName.substring(dPos + 4)));
		}
		isDownGoing = new boolean[nPart];
		partition = new Partition[nPart];
		phaseParts = new PhasePart[nPart];
	}

	boolean isDiffracted() {
		return compiledName.contains("diff");
	}

	/**
	 * Angle of Diffraction
	 */
	private double diffractionAngle;

	private void digitalize() {
		// P
		int iCurrentPart = 0;
		for (int i = 0; i < compiledName.length(); i++) {
			final char nextChar = i + 1 == compiledName.length() ? 0 : compiledName.charAt(i + 1);
			final char beforeChar = i == 0 ? 0 : compiledName.charAt(i - 1);
			switch (compiledName.charAt(i)) {
			case 'd':
				break;
			case 'c':
				continue;
			case 'i':
				continue;
			case 'p':
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.MANTLE;
				phaseParts[iCurrentPart] = PhasePart.P;
				iCurrentPart++;
				break;
			case 's':
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.MANTLE;
				phaseParts[iCurrentPart] = psv ? PhasePart.SV : PhasePart.SH;
				iCurrentPart++;
				break;
			case 'P':
				partition[iCurrentPart] = Partition.MANTLE;
				phaseParts[iCurrentPart] = PhasePart.P;
				isDownGoing[iCurrentPart] = i == 0 || (beforeChar != 'K' && beforeChar != 'c');
				if (isDownGoing[iCurrentPart])
					// ^P$ [psPS]P[PS]...
					if (i + 1 == compiledName.length() || (nextChar != 'c' && nextChar != 'K')) {
					iCurrentPart++;
					isDownGoing[iCurrentPart] = false;
					phaseParts[iCurrentPart] = PhasePart.P;
					if (nextChar != 'd')
					partition[iCurrentPart] = Partition.MANTLE;
					else {
					partition[iCurrentPart] = Partition.CORE_MANTLE_BOUNDARY;
					iCurrentPart++;
					isDownGoing[iCurrentPart] = false;
					partition[iCurrentPart] = Partition.MANTLE;
					phaseParts[iCurrentPart] = PhasePart.P;
					}
					}
				iCurrentPart++;
				break;
			case 'S':
				isDownGoing[iCurrentPart] = i == 0 || (beforeChar != 'K' && beforeChar != 'c');
				partition[iCurrentPart] = Partition.MANTLE;
				phaseParts[iCurrentPart] = psv ? PhasePart.SV : PhasePart.SH;
				if (isDownGoing[iCurrentPart])
					if (i + 1 == compiledName.length() || (nextChar != 'c' && nextChar != 'K')) {
						iCurrentPart++;
						partition[iCurrentPart] = Partition.MANTLE;
						isDownGoing[iCurrentPart] = false;
						phaseParts[iCurrentPart] = psv ? PhasePart.SV : PhasePart.SH;
						if (nextChar != 'd')
							partition[iCurrentPart] = Partition.MANTLE;
						else {
							partition[iCurrentPart] = Partition.CORE_MANTLE_BOUNDARY;
							iCurrentPart++;
							isDownGoing[iCurrentPart] = false;
							partition[iCurrentPart] = Partition.MANTLE;
							phaseParts[iCurrentPart] = psv ? PhasePart.SV : PhasePart.SH;
						}
					}
				iCurrentPart++;
				break;
			case 'I':
				isDownGoing[iCurrentPart] = true;
				partition[iCurrentPart] = Partition.INNERCORE;
				phaseParts[iCurrentPart] = PhasePart.I;
				iCurrentPart++;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.INNERCORE;
				phaseParts[iCurrentPart] = PhasePart.I;
				iCurrentPart++;
				break;
			case 'J':
				isDownGoing[iCurrentPart] = true;
				partition[iCurrentPart] = Partition.INNERCORE;
				phaseParts[iCurrentPart] = psv ? PhasePart.JV : PhasePart.JH;
				iCurrentPart++;
				isDownGoing[iCurrentPart] = false;
				partition[iCurrentPart] = Partition.INNERCORE;
				phaseParts[iCurrentPart] = psv ? PhasePart.JV : PhasePart.JH;
				iCurrentPart++;
				break;
			case 'K':
				isDownGoing[iCurrentPart] = compiledName.charAt(i - 1) != 'i' && compiledName.charAt(i - 1) != 'I'
						&& compiledName.charAt(i - 1) != 'J';
				partition[iCurrentPart] = Partition.OUTERCORE;
				phaseParts[iCurrentPart] = PhasePart.K;
				if (compiledName.charAt(i - 1) != 'i' && compiledName.charAt(i - 1) != 'I'
						&& compiledName.charAt(i - 1) != 'J')
					if (compiledName.charAt(i + 1) != 'i' && compiledName.charAt(i + 1) != 'I'
							&& compiledName.charAt(i + 1) != 'J') {
						iCurrentPart++;
						isDownGoing[iCurrentPart] = false;
						partition[iCurrentPart] = Partition.OUTERCORE;
						phaseParts[iCurrentPart] = PhasePart.K;
					}
				iCurrentPart++;
				break;
			}
		}
	}

	private void setPropagation() {
		if (compiledName.contains("Pdiff"))
			mantlePPropagation = Propagation.DIFFRACTION;
		else if (compiledName.contains("PK") || compiledName.contains("Pc") || compiledName.contains("KP")
				|| compiledName.contains("cP"))
			mantlePPropagation = Propagation.PENETRATING;
		else
			mantlePPropagation = compiledName.contains("P") ? Propagation.BOUNCING : Propagation.NOEXIST;

		if (compiledName.contains("Sdiff"))
			mantleSPropagation = Propagation.DIFFRACTION;
		else if (compiledName.contains("SK") || compiledName.contains("Sc") || compiledName.contains("KS")
				|| compiledName.contains("cS"))
			mantleSPropagation = Propagation.PENETRATING;
		else
			mantleSPropagation = compiledName.contains("S") ? Propagation.BOUNCING : Propagation.NOEXIST;

		if (compiledName.contains("I") || compiledName.contains("J") || compiledName.contains("i"))
			kPropagation = Propagation.PENETRATING;
		else
			kPropagation = compiledName.contains("K") ? Propagation.BOUNCING : Propagation.NOEXIST;

		innerCorePPropagation = compiledName.contains("I") ? Propagation.BOUNCING : Propagation.NOEXIST;

		innerCoreSPropagation = compiledName.contains("J") ? Propagation.BOUNCING : Propagation.NOEXIST;

		if (compiledName.charAt(0) == 'p')
			mantlePTravel = -0.5;
		if (compiledName.charAt(0) == 's')
			mantleSTravel = -0.5;

		for (int i = 0; i < nPart; i++) {
			if (partition[i].isBoundary())
				continue;
			switch (phaseParts[i]) {
			case I:
				innerCorePTravel += 0.5;
				break;
			case JH:
			case JV:
				innerCoreSTravel += 0.5;
				break;
			case K:
				outerCoreTravel += 0.5;
				break;
			case P:
				mantlePTravel += 0.5;
				break;
			case SH:
			case SV:
				mantleSTravel += 0.5;
				break;
			default:
				throw new RuntimeException("UnexpectEd");
			}
		}

	}

	public static void main(String[] args) {
		// System.out.println(Phase.create("2PSdiff900"));
		Phase p = Phase.create("ScS");
		System.out.println(p.innerCoreSTravel);
		p.printInformation();
	}

	void printInformation() {
		IntStream.range(0, nPart)
				.forEach(i -> System.out.println(phaseParts[i] + " " + isDownGoing[i] + " " + partition[i]));
	}

	/**
	 * @param phase
	 *            phase name
	 * @return if is well known phase?
	 */
	static boolean isValid(String phase) {
		if (phase.isEmpty())
			return false;
		// diffraction phase
		if (diffRule.matcher(phase).find())
			return false;
		if (!pDiff.matcher(phase).find() && !sDiff.matcher(phase).find() && finalLetter.matcher(phase).find())
			return false;

		// check if other letters are used.
		if (others.matcher(phase).find())
			return false;

		if (firstLetter.matcher(phase).find())
			return false;

		if (ps.matcher(phase).find())
			return false;

		if (nextC.matcher(phase).find())
			return false;

		if (nextK.matcher(phase).find())
			return false;

		if (nextJ.matcher(phase).find())
			return false;

		if (smallI.matcher(phase).find())
			return false;

		if (largeI.matcher(phase).find())
			return false;

		// phase reflected at the icb
		boolean icb = phase.contains("Ki") || phase.contains("iK");
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

	private Propagation mantlePPropagation;
	private Propagation innerCorePPropagation;
	private Propagation kPropagation;

	private Propagation mantleSPropagation;
	private Propagation innerCoreSPropagation;

	Propagation getMantlePPropagation() {
		return mantlePPropagation;
	}

	Propagation getInnerCorePPropagation() {
		return innerCorePPropagation;
	}

	Propagation getkPropagation() {
		return kPropagation;
	}

	Propagation getMantleSPropagation() {
		return mantleSPropagation;
	}

	Propagation getInnerCoreSPropagation() {
		return innerCoreSPropagation;
	}

	/**
	 * @param pTurning
	 *            turning point of P
	 * @param sTurning
	 *            turning point of S
	 * @return if the input turning points are valid for this
	 */
	boolean exists(Raypath ray) {
		if (mantlePPropagation != Propagation.NOEXIST && ray.getPropagation(PhasePart.P) != mantlePPropagation)
			return false;
		if (mantleSPropagation != Propagation.NOEXIST
				&& ray.getPropagation(psv ? PhasePart.SV : PhasePart.SH) != mantleSPropagation)
			return false;
		if (kPropagation != Propagation.NOEXIST && ray.getPropagation(PhasePart.K) != kPropagation)
			return false;
		if (innerCorePPropagation != Propagation.NOEXIST && ray.getPropagation(PhasePart.I) == Propagation.NOEXIST)
			return false;
		if (innerCoreSPropagation != Propagation.NOEXIST
				&& ray.getPropagation(psv ? PhasePart.JV : PhasePart.JH) == Propagation.NOEXIST)
			return false;
		return true;
	}

	/**
	 * deepest part of P phase
	 * 
	 * @return null if P phase does not exist.
	 */
	Partition pReaches() {
		if (compiledName.contains("Sdiff"))
			return null;
		if (compiledName.contains("I"))
			return Partition.INNERCORE;
		if (compiledName.contains("i") || compiledName.contains("J"))
			return Partition.INNER_CORE_BOUNDARY;
		if (compiledName.contains("K"))
			return Partition.OUTERCORE;
		if (!compiledName.contains("P") && !compiledName.contains("p"))
			return null;
		if (compiledName.contains("Pc") || compiledName.contains("cP") || compiledName.contains("diff"))
			return Partition.CORE_MANTLE_BOUNDARY;
		return Partition.MANTLE;
	}

	/**
	 * deepest part of s Phase
	 * 
	 * @return null if S phase does not exist.
	 */
	Partition sReaches() {
		if (compiledName.contains("J"))
			return Partition.INNERCORE;
		if (!compiledName.contains("S"))
			return null;
		if (compiledName.contains("Sc") || compiledName.contains("cS") || compiledName.contains("KS")
				|| compiledName.contains("SK") || compiledName.contains("diff"))
			return Partition.CORE_MANTLE_BOUNDARY;
		return Partition.MANTLE;
	}

	/**
	 * Number of P wave parts in the path. Each down or upgoing is 0.5
	 */
	private double mantlePTravel;

	/**
	 * @see #mantlePTravel
	 */
	private double mantleSTravel;

	/**
	 * @see #mantlePTravel
	 */
	private double outerCoreTravel;

	/**
	 * @see #mantlePTravel
	 */
	private double innerCorePTravel;

	/**
	 * @see #mantlePTravel
	 */
	private double innerCoreSTravel;

	/**
	 * @return how many times P wave travels in the mantle. each down or upgoing
	 *         is 0.5
	 */
	double getCountOfP() {
		return mantlePTravel;
	}

	/**
	 * @return how many times S wave travels in the mantle. each down or upgoing
	 *         is 0.5
	 */
	double getCountOfS() {
		return mantleSTravel;
	}

	/**
	 * how many times wave travels in the outer core. Each down or up going is
	 * considered as 0.5
	 * 
	 * @return the times K phase travels in the outer core.
	 */
	double getCountOfK() {
		return outerCoreTravel;
	}

	/**
	 * how many times P wave travels in the inner core. Each down or upgoing is
	 * considered as 0.5
	 * 
	 * @return the times P wave travels in the inner core
	 */
	double getCountOfI() {
		return innerCorePTravel;
	}

	/**
	 * how many times S wave travels in the inner core. Each down or up going is
	 * considered as 0.5
	 * 
	 * @return the times S wave travels in the inner core.
	 */
	double getCountOfJ() {
		return innerCoreSTravel;
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
	 *            index of the phase 0 for the first phase part
	 * @return phase part for the index
	 */
	PhasePart phasePartOf(int i) {
		return phaseParts[i];
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
	 * @return the partition where the i th part travels
	 */
	Partition partitionOf(int i) {
		return partition[i];
	}

	@Override
	public String toString() {
		return phaseName;
	}
}
