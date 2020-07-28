package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class PolynomialStructureTest {
    public static void main(String[] args) throws IOException {
        PolynomialStructure.PREM.writePSV(Paths.get("/tmp/hoge"));
    }
}
