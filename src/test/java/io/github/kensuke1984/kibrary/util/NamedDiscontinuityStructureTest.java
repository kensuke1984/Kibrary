package io.github.kensuke1984.kibrary.util;


import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class NamedDiscontinuityStructureTest {
    private static void prem() {
        NamedDiscontinuityStructure premND = NamedDiscontinuityStructure.prem();
        PolynomialStructure premPS = PolynomialStructure.ISO_PREM;
        for (int i = 0; i < 6371; i++)
            System.out.println(i + " " + (premND.getVph(i) - premPS.getVphAt(i)));
    }

    private static void arbitraryFile() throws Exception {
        Path path = Paths.get("/mnt/ntfs/kensuke/workspace/anisotime/nd/miasp91_aniso.nd");
        NamedDiscontinuityStructure namedDiscontinuityStructure = new NamedDiscontinuityStructure(path);
        System.out.println(namedDiscontinuityStructure.innerCoreBoundary() + " innercore");
        System.out.println(namedDiscontinuityStructure.getMohoDiscontinuity() + " moho");

        System.out.println(namedDiscontinuityStructure.coreMantleBoundary() + " outercore");
    }

    public static void main(String[] args) throws Exception {
        arbitraryFile();
    }
}
