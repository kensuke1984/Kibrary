package io.github.kensuke1984.anisotime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * <p>
 * Phase name.
 * </p>
 * <p>
 * This class is <b>immutable</b>.
 * <p>
 * Waveform is now digitalized. Waveform is divided into parts. each part has up
 * or downgoing, P or S and the partition in which waveform exists.
 * <p>
 * ???PdiffXX and ???SdiffXX can be used. XX is positive double XX is
 * diffractionAngle diff must be the last part.
 * <p>
 * Diffraction can only happen at the final part.
 * <p>
 * <p>
 * Numbers in a name.
 * <ul>
 * <li>redundancy</li> A number in parentheses indicates repetition of
 * arguments. S(2K)S &rarr; SKKS; P(2S)P &rarr; PSSP;<br>
 * Multiple parentheses are accepted. (2ScS)(2SKS) &rarr; ScSScSSKSSKS<br>
 * Nesting is not allowed. (2S(2K)S) &rarr; IllegalArgumentException
 * <li>underside reflection</li> TODO
 * <li>topside reflection</li> TODO
 * <li>topside diffraction</li> TODO
 * </ul>
 *
 * @author Kensuke Konishi
 * @version 0.1.6.2
 *          <p>
 *          TODO TauP のように 任意の深さの反射 ADDEDBOUNDARY
 */
public class Phase {

    // no use letters
    private static final Pattern others = Pattern.compile("[abeghj-oqrt-zA-HL-OQ-RT-Z]");

    // Pattern for repetition
    private static final Pattern repetition = Pattern.compile("\\((\\d*)([^\\d]+?)\\)");
    // nesting of parenthesis is prohibited
    private static Pattern nestParentheses = Pattern.compile("\\([^\\)]*\\(|\\)[^\\(]*\\)");

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
    public static final Phase p = create("p");
    public static final Phase P = create("P");
    public static final Phase PcP = create("PcP");
    public static final Phase PKP = create("PKP");
    public static final Phase PKiKP = create("PKiKP");
    public static final Phase PKIKP = create("PKIKP");
    public static final Phase s = create("s");
    public static final Phase S = create("S");
    static final Phase SV = create("S", true);
    public static final Phase ScS = create("ScS");
    static final Phase SVcS = create("ScS", true);
    public static final Phase SKS = create("SKS");
    public static final Phase SKiKS = create("SKiKS");
    public static final Phase SKIKS = create("SKIKS");

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + ((expandedName == null) ? 0 : expandedName.hashCode());
        result = prime * result + (psv ? 1231 : 1237);
        return result;
    }

    /**
     * It returns true, when compiled names are same. In other words, S(2K)S and
     * SKKS are equal (if the polarity is same).
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Phase other = (Phase) obj;
        if (expandedName == null) {
            if (other.expandedName != null) return false;
        } else if (!expandedName.equals(other.expandedName)) return false;
        return psv == other.psv;
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
     * (input) phase name e.g. S(mK)S
     */
    private final String phaseName;

    /**
     * name for parsing e.g. SKKKKKS
     */
    private final String expandedName;

    /**
     * @param phaseName    of the phase (e.g. SKKS)
     * @param expandedName of the phase (e.g. S2KS)
     * @param psv          true:P-SV, false:SH
     */
    private Phase(String phaseName, String expandedName, boolean psv) {
        this.phaseName = phaseName;
        this.expandedName = expandedName;
        this.psv = psv;
        countParts();
        digitalize();
        setPropagation();
    }

    /**
     * @param name phase name
     * @param sv   true:P-SV, false:SH. If the phase contains "P" or "K", it is
     *             ignored and always is true.
     * @return phase for input
     * @throws IllegalArgumentException if the phase is invalid
     */
    public static Phase create(String name, boolean... sv) {
        if (1 < sv.length) throw new IllegalArgumentException("SV or not");
        String expandedName = expandParentheses(name);
        if (isValid(expandedName))
            return new Phase(name, expandedName, name.contains("P") || name.contains("K") || (sv.length != 0 && sv[0]));
        throw new IllegalArgumentException("Invalid phase name " + name);
    }

    /**
     * @param name phase name before expansion. e.g. S(2K)S
     * @return an expanded phase name e.g. SKKS
     */
    private static String expandParentheses(String name) {
        if (nestParentheses.matcher(name).find())
            throw new IllegalArgumentException("Invalid phase name (Parentheses nesting is detected)" + name);
        try {
            Matcher m = repetition.matcher(name);
            String expanded = name;
            while (m.find()) {
                String numStr = m.group(1);
                String replace = m.group(2);
                int n = !numStr.isEmpty() ? Integer.parseInt(numStr) : 1;
                for (int i = 0; i < n - 1; i++)
                    replace += m.group(2);
                expanded = expanded.replace(m.group(), replace);
            }
            return expanded;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid phase name " + name);
        }
    }

    /**
     * the number of Parts part is for each layer and each direction. if there
     * is a turning point in one layer the number of parts will increase for
     * example S wave has 2 parts. Sdiff has 3 parts.
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
        for (int i = 0; i < expandedName.length(); i++)
            if (expandedName.charAt(i) == 'd') break;
            else if (expandedName.charAt(i) == 'c' || expandedName.charAt(i) == 'i') reflection++;
            else if (expandedName.charAt(i) == 'K' &&
                    (expandedName.charAt(i - 1) == 'P' || expandedName.charAt(i - 1) == 'S')) zoneMove++;
            else if ((expandedName.charAt(i) == 'I' || expandedName.charAt(i) == 'J') &&
                    expandedName.charAt(i - 1) == 'K') zoneMove++;
        // reflection++;
        int dPos = expandedName.indexOf('d');
        nPart = ((0 < dPos ? dPos : expandedName.length()) - reflection * 2 - zoneMove) * 2;
        if (expandedName.charAt(0) == 'p' || expandedName.charAt(0) == 's') nPart--;

        // diffraction
        if (0 < dPos) {
            nPart++;
            if (dPos + 4 < expandedName.length())
                diffractionAngle = Math.toRadians(Double.parseDouble(expandedName.substring(dPos + 4)));
        }
        isDownGoing = new boolean[nPart];
        partition = new Partition[nPart];
        phaseParts = new PhasePart[nPart];
    }

    boolean isDiffracted() {
        return expandedName.contains("diff");
    }

    /**
     * Angle of Diffraction
     */
    private double diffractionAngle;

    private void digitalize() {
        // P
        int iCurrentPart = 0;
        for (int i = 0; i < expandedName.length(); i++) {
            char nextChar = i + 1 == expandedName.length() ? 0 : expandedName.charAt(i + 1);
            char beforeChar = i == 0 ? 0 : expandedName.charAt(i - 1);
            switch (expandedName.charAt(i)) {
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
                        if (i + 1 == expandedName.length() || (nextChar != 'c' && nextChar != 'K')) {
                            iCurrentPart++;
                            isDownGoing[iCurrentPart] = false;
                            phaseParts[iCurrentPart] = PhasePart.P;
                            if (nextChar != 'd') partition[iCurrentPart] = Partition.MANTLE;
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
                        if (i + 1 == expandedName.length() || (nextChar != 'c' && nextChar != 'K')) {
                            iCurrentPart++;
                            partition[iCurrentPart] = Partition.MANTLE;
                            isDownGoing[iCurrentPart] = false;
                            phaseParts[iCurrentPart] = psv ? PhasePart.SV : PhasePart.SH;
                            if (nextChar != 'd') partition[iCurrentPart] = Partition.MANTLE;
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
                    isDownGoing[iCurrentPart] =
                            expandedName.charAt(i - 1) != 'i' && expandedName.charAt(i - 1) != 'I' &&
                                    expandedName.charAt(i - 1) != 'J';
                    partition[iCurrentPart] = Partition.OUTERCORE;
                    phaseParts[iCurrentPart] = PhasePart.K;
                    if (expandedName.charAt(i - 1) != 'i' && expandedName.charAt(i - 1) != 'I' &&
                            expandedName.charAt(i - 1) != 'J')
                        if (expandedName.charAt(i + 1) != 'i' && expandedName.charAt(i + 1) != 'I' &&
                                expandedName.charAt(i + 1) != 'J') {
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
        if (expandedName.contains("Pdiff")) mantlePPropagation = Propagation.DIFFRACTION;
        else if (expandedName.contains("PK") || expandedName.contains("Pc") || expandedName.contains("KP") ||
                expandedName.contains("cP")) mantlePPropagation = Propagation.PENETRATING;
        else mantlePPropagation = expandedName.contains("P") ? Propagation.BOUNCING : Propagation.NOEXIST;

        if (expandedName.contains("Sdiff")) mantleSPropagation = Propagation.DIFFRACTION;
        else if (expandedName.contains("SK") || expandedName.contains("Sc") || expandedName.contains("KS") ||
                expandedName.contains("cS")) mantleSPropagation = Propagation.PENETRATING;
        else mantleSPropagation = expandedName.contains("S") ? Propagation.BOUNCING : Propagation.NOEXIST;

        if (expandedName.contains("I") || expandedName.contains("J") || expandedName.contains("i"))
            kPropagation = Propagation.PENETRATING;
        else kPropagation = expandedName.contains("K") ? Propagation.BOUNCING : Propagation.NOEXIST;

        innerCorePPropagation = expandedName.contains("I") ? Propagation.BOUNCING : Propagation.NOEXIST;

        innerCoreSPropagation = expandedName.contains("J") ? Propagation.BOUNCING : Propagation.NOEXIST;

        if (expandedName.charAt(0) == 'p') mantlePTravel = -0.5;
        if (expandedName.charAt(0) == 's') mantleSTravel = -0.5;

        for (int i = 0; i < nPart; i++) {
            if (partition[i].isBoundary()) continue;
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
        Phase p = create("P(2K)P");
        System.out.println(p.phaseName + " " + p.expandedName);
        System.out.println(p.isDownGoing[1]);
        p.printInformation();
    }

    void printInformation() {
        IntStream.range(0, nPart).forEach(
                i -> System.out.println(phaseParts[i] + " " + (isDownGoing[i] ? "down" : "up") + " " + partition[i]));
    }

    /**
     * @param phase phase name
     * @return if is well known phase?
     */
    static boolean isValid(String phase) {
        if (phase.isEmpty()) return false;
        // diffraction phase
        if (diffRule.matcher(phase).find()) return false;
        if (!pDiff.matcher(phase).find() && !sDiff.matcher(phase).find() && finalLetter.matcher(phase).find())
            return false;

        // check if other letters are used.
        if (others.matcher(phase).find()) return false;

        if (firstLetter.matcher(phase).find()) return false;

        if (ps.matcher(phase).find()) return false;

        if (nextC.matcher(phase).find()) return false;

        if (nextK.matcher(phase).find()) return false;

        if (nextJ.matcher(phase).find()) return false;

        if (smallI.matcher(phase).find()) return false;

        if (largeI.matcher(phase).find()) return false;

        // phase reflected at the icb
        boolean icb = phase.contains("Ki") || phase.contains("iK");
        boolean innercoreP = phase.contains("I");
        boolean innercoreS = phase.contains("J");

        if (mantleP.matcher(phase).find())
            if (cmbP.matcher(phase).find() || outercoreP.matcher(phase).find() || innercoreP) return false;

        if (mantleS.matcher(phase).find())
            if (cmbS.matcher(phase).find() || outercoreS.matcher(phase).find() || innercoreS) return false;

        if (outercore.matcher(phase).find()) if (innercoreP || icb || innercoreS) return false;
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
     * @param ray to be checked
     * @return if the ray exists
     */
    boolean exists(Raypath ray) {
        if (mantlePPropagation != Propagation.NOEXIST && ray.getPropagation(PhasePart.P) != mantlePPropagation)
            return false;
        if (mantleSPropagation != Propagation.NOEXIST &&
                ray.getPropagation(psv ? PhasePart.SV : PhasePart.SH) != mantleSPropagation) return false;
        if (kPropagation != Propagation.NOEXIST && ray.getPropagation(PhasePart.K) != kPropagation) return false;
        if (innerCorePPropagation != Propagation.NOEXIST && ray.getPropagation(PhasePart.I) == Propagation.NOEXIST)
            return false;
        return !(innerCoreSPropagation != Propagation.NOEXIST &&
                ray.getPropagation(psv ? PhasePart.JV : PhasePart.JH) == Propagation.NOEXIST);
    }

    /**
     * deepest part of P phase
     *
     * @return null if P phase does not exist.
     */
    Partition pReaches() {
        if (expandedName.contains("Sdiff")) return null;
        if (expandedName.contains("I")) return Partition.INNERCORE;
        if (expandedName.contains("i") || expandedName.contains("J")) return Partition.INNER_CORE_BOUNDARY;
        if (expandedName.contains("K")) return Partition.OUTERCORE;
        if (!expandedName.contains("P") && !expandedName.contains("p")) return null;
        if (expandedName.contains("Pc") || expandedName.contains("cP") || expandedName.contains("diff"))
            return Partition.CORE_MANTLE_BOUNDARY;
        return Partition.MANTLE;
    }

    /**
     * deepest part of s Phase
     *
     * @return null if S phase does not exist.
     */
    Partition sReaches() {
        if (expandedName.contains("J")) return Partition.INNERCORE;
        if (!expandedName.contains("S")) return null;
        if (expandedName.contains("Sc") || expandedName.contains("cS") || expandedName.contains("KS") ||
                expandedName.contains("SK") || expandedName.contains("diff")) return Partition.CORE_MANTLE_BOUNDARY;
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
     * how many times P wave travels in the inner core. Each down or upgoing is
     * considered as 0.5
     *
     * @param pp to be count
     * @return the times P wave travels in the inner core
     */
    double getCountOf(PhasePart pp) {
        switch (pp) {
            case I:
                return innerCorePTravel;
            case JH:
            case JV:
                return innerCoreSTravel;
            case K:
                return outerCoreTravel;
            case P:
                return mantlePTravel;
            case SH:
            case SV:
                return mantleSTravel;
            default:
                throw new RuntimeException("unikspected");
        }

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
     * @param i index of the phase 0 for the first phase part
     * @return phase part for the index
     */
    PhasePart phasePartOf(int i) {
        return phaseParts[i];
    }

    /**
     * @param i index of the target
     * @return if it is down going in i th part.
     */
    boolean partIsDownGoing(int i) {
        return isDownGoing[i];
    }

    /**
     * @param i index of the target
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
