package io.github.kensuke1984.kibrary.util;


import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class NamedDiscontinuityStructureTest {
    private static void prem(){
        NamedDiscontinuityStructure premND = NamedDiscontinuityStructure.prem();
        PolynomialStructure premPS = PolynomialStructure.ISO_PREM;
        for (int i=0;i<6371;i++)
            System.out.println(i+" "+(premND.getVp(i)-premPS.getVphAt(i)));
    }
    public static void main(String[] args) {
prem();
    }
}
