package io.github.kensuke1984.anisotime;

import java.nio.file.Paths;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class NamedDiscontinuityStructureTest {
    public static void main(String[] args) throws Exception {
        NamedDiscontinuityStructure namedDiscontinuityStructure =
                new NamedDiscontinuityStructure(Paths.get("/mnt/ntfs/kensuke/workspace/anisotime/nd/miasp91_aniso.nd"));
//        ComputationalMesh mesh = ComputationalMesh.simple(namedDiscontinuityStructure);
//        System.out.println("jij");
//         RaypathCatalog.computeCatalog(namedDiscontinuityStructure, mesh, RaypathCatalog.DEFAULT_MAXIMUM_D_DELTA);
        Raypath raypath = new Raypath(10, namedDiscontinuityStructure);
    }
}
