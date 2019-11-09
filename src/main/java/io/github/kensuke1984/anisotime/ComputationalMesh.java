package io.github.kensuke1984.anisotime;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Radius interval for integration.
 * <p>
 * This class is <b>IMMUTABLE</b>.
 * <p>
 *
 * @author Kensuke Konishi
 * @version 0.0.4
 */
public class ComputationalMesh implements Serializable {

    /**
     * when integrate values on boundaries, use the value at point very close to
     * the boundaries by eps. The value is 1e-7
     */
    static final double EPS = 1e-7;

    /**
     * 2019/11/9
     */
    private static final long serialVersionUID = -2606996576121179707L;


    /**
     * Mesh for the inner-core [0, inner-core boundary] [km]
     * <p>
     * (r<sub>0</sub>, r<sub>1</sub>, ..., r<sub>n</sub>) where r<sub>i</sub>
     * &lt; r<sub>i+1</sub>
     */
    private transient RealVector innerCoreMesh;

    /**
     * Mesh for the outer-core [inner-core boundary, core-mantle boundary] [km]
     * <p>
     * (r<sub>0</sub>, r<sub>1</sub>, ..., r<sub>n</sub>) where r<sub>i</sub>
     * &lt; r <sub>i+1</sub>
     */
    private transient RealVector outerCoreMesh;

    /**
     * Mesh for the mantle [core-mantle boundary, surface] [km]
     * <p>
     * (r<sub>0</sub>, r<sub>1</sub>, ..., r<sub>n</sub>) where r<sub>i</sub>
     * &lt; r <sub>i+1</sub>
     */
    private transient RealVector mantleMesh;

    /**
     * @param structure         to create mesh for
     * @param innerCoreInterval [km] interval in the inner-core
     * @param outerCoreInterval [km] interval in the outer-core
     * @param mantleInterval    [km] interval in the mantle
     */
    public ComputationalMesh(VelocityStructure structure, double innerCoreInterval, double outerCoreInterval,
                             double mantleInterval) {
        if (innerCoreInterval < EPS || outerCoreInterval < EPS || mantleInterval < EPS)
            throw new RuntimeException("Intervals are too small.");
        createSimpleMesh(structure, innerCoreInterval, outerCoreInterval, mantleInterval);
    }

    /**
     * @param i        index of the mesh to be refined
     * @param n        the i th mesh is divided into n parts. (2 &lt; n)
     * @param original arrays to be remeshed
     * @return remeshed array
     */
    private static double[] remesh(int i, int n, double[] original) {
        if (original.length < 2 || i < 0 || n < 2 || original.length - 2 < i)
            throw new RuntimeException("Something wrong.");
        double[] remeshed = new double[original.length + n - 1];
        System.arraycopy(original, 0, remeshed, 0, i + 1);
        System.arraycopy(original, i + 1, remeshed, i + n, original.length - i - 1);
        double delta = (original[i + 1] - original[i]) / n;
        for (int j = 1; j < n; j++)
            remeshed[i + j] = original[i] + delta * j;
        return remeshed;
    }

    /**
     * The first and last parts are divided by 1/10 of the interval.
     *
     * @param points original
     * @return remeshed array
     */
    private static double[] considerEdges(double[] points) {
        if (points.length < 2) throw new RuntimeException();
        double[] remeshed = remesh(0, 10, points);
        return points.length == 2 ? remeshed : remesh(remeshed.length - 2, 10, remeshed);
    }

    /**
     * Interval of integration startR to endR is divided by deltaR
     * start and end values are shifted by eps/3.
     *
     * @param startR [km]
     * @param endR   [km]
     * @param deltaR [km]
     * @return Array of radius [km]
     */
    private static double[] point(double startR, double endR, double deltaR) {
        int n = Math.max(1, (int) Math.round((endR - startR) / deltaR));
        double[] x = new double[n + 1];
        for (int i = 1; i < n; i++)
            x[i] = startR + i * deltaR;
        x[0] = startR + EPS / 3;
        x[n] = endR - EPS / 3;
        return x;
    }

    public static ComputationalMesh simple(VelocityStructure structure) {
        return new ComputationalMesh(structure, 1, 1, 1);
    }

    /**
     * @param partition for the return mesh
     * @return mesh for the partition
     */
    RealVector getMesh(Partition partition) {
        switch (partition) {
            case MANTLE:
                return mantleMesh;
            case OUTERCORE:
                return outerCoreMesh;
            case INNERCORE:
                return innerCoreMesh;
            default:
                throw new IllegalArgumentException("Input must not be a boundary.");
        }
    }


    /**
     * Basically, creates a mesh with the input intervals, except boundaries in the structure.
     * Around the boundary, the intervals are to be 10 % of their originals.
     *
     * @param structure         velocity structure
     * @param innerCoreInterval [km] interval of the mesh in the inner-core
     * @param outerCoreInterval [km] interval of the mesh in the outer-core
     * @param mantleInterval    [km] interval of the mesh in the mantle
     */
    private void createSimpleMesh(VelocityStructure structure, double innerCoreInterval, double outerCoreInterval,
                                  double mantleInterval) {
        double[] mantleBoundaries = structure.boundariesInMantle();
        double[] outerCoreBoundaries = structure.boundariesInOuterCore();
        double[] innerCoreBoundaries = structure.boundariesInInnerCore();
        double[][] mantles = new double[mantleBoundaries.length - 1][];
        for (int i = 0; i < mantles.length; i++)
            mantles[i] = considerEdges(point(mantleBoundaries[i], mantleBoundaries[i + 1], mantleInterval));
        double[][] outerCores = new double[outerCoreBoundaries.length - 1][];
        for (int i = 0; i < outerCores.length; i++)
            outerCores[i] = considerEdges(point(outerCoreBoundaries[i], outerCoreBoundaries[i + 1], outerCoreInterval));
        double[][] innerCores = new double[innerCoreBoundaries.length - 1][];
        for (int i = 0; i < innerCores.length; i++)
            innerCores[i] = considerEdges(point(innerCoreBoundaries[i], innerCoreBoundaries[i + 1], innerCoreInterval));

        mantleMesh = RealVector.unmodifiableRealVector(new ArrayRealVector(
                Arrays.stream(mantles).flatMapToDouble(Arrays::stream).distinct().sorted().toArray(), false));
        outerCoreMesh = RealVector.unmodifiableRealVector(new ArrayRealVector(
                Arrays.stream(outerCores).flatMapToDouble(Arrays::stream).distinct().sorted().toArray(), false));
        innerCoreMesh = RealVector.unmodifiableRealVector(new ArrayRealVector(
                Arrays.stream(innerCores).flatMapToDouble(Arrays::stream).distinct().sorted().toArray(), false));
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + ((innerCoreMesh == null) ? 0 : Arrays.hashCode(innerCoreMesh.toArray()));
        result = prime * result + ((mantleMesh == null) ? 0 : Arrays.hashCode(mantleMesh.toArray()));
        result = prime * result + ((outerCoreMesh == null) ? 0 : Arrays.hashCode(outerCoreMesh.toArray()));
        return result;
    }

    /**
     * All the radii in the mesh and the integral_threshold are checked.
     *
     * @param obj another mesh
     * @return if the mesh (obj) equals to this.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ComputationalMesh other = (ComputationalMesh) obj;
        if (innerCoreMesh == null) {
            if (other.innerCoreMesh != null) return false;
        } else if (!Arrays.equals(innerCoreMesh.toArray(), other.innerCoreMesh.toArray())) return false;
        if (mantleMesh == null) {
            if (other.mantleMesh != null) return false;
        } else if (!Arrays.equals(mantleMesh.toArray(), other.mantleMesh.toArray())) return false;
        if (outerCoreMesh == null) {
            if (other.outerCoreMesh != null) return false;
        } else if (!Arrays.equals(outerCoreMesh.toArray(), other.outerCoreMesh.toArray())) return false;
        return true;
    }

    /**
     * @param r         [km] must be [min, max(+{@link #EPS})] of the partition.
     * @param partition in which the r is found
     * @return index of maximum r<sub>i</sub> (r<sub>i</sub> &le; r) in the
     * mesh. If the mesh includes r, it returns the index of r<sub>i</sub> (= r).<br>
     * Note that the return value is the index in certain partition
     * mesh.
     */
    int getNextIndexOf(double r, Partition partition) {
        RealVector mesh = getMesh(partition);
        if (r < mesh.getEntry(0) - EPS || mesh.getEntry(mesh.getDimension() - 1) + EPS < r)
            throw new IllegalArgumentException("Input " + r + " is out of " + partition);
        if (r <= mesh.getEntry(0)) return 0;
        for (int i = 1; i < mesh.getDimension(); i++)
            if (r < mesh.getEntry(i)) return i - 1;
        return mesh.getDimension() - 1;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(mantleMesh.copy());
        stream.writeObject(outerCoreMesh.copy());
        stream.writeObject(innerCoreMesh.copy());
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        mantleMesh = RealVector.unmodifiableRealVector((RealVector) stream.readObject());
        outerCoreMesh = RealVector.unmodifiableRealVector((RealVector) stream.readObject());
        innerCoreMesh = RealVector.unmodifiableRealVector((RealVector) stream.readObject());
    }

}
