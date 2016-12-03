package io.github.kensuke1984.anisotime;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * Radius interval for integration. TODO
 * <p>
 * This class is <b>IMMUTABLE<b>.
 * <p>
 * <p>
 * TODO Automesh by QDelta ?
 *
 * @author Kensuke Konishi
 * @version 0.0.1.1
 */
public class ComputationalMesh implements Serializable {

    /**
     * 2016/12/3
     */
    private static final long serialVersionUID = 9145563734176848831L;

    /**
     * when integrate values on boundaries, use the value at point very close to
     * the boundaries by eps. The value is 1e-7
     */
    static final double eps = 1e-7;

    /**
     * Threshold for the integration. This value (ratio) must be positive and
     * less than 1. If it is a, the difference between two Q<sub>T</sub> at
     * adjacent points must be with in a. a &lt; Q<sub>T</sub> (i)/Q<sub>T</sub>
     * (i+1) &lt; 1/a
     */
    final double integralThreshold = 0.9;

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

    ComputationalMesh(VelocityStructure structure, double innerCoreInterval, double outerCoreInterval,
                      double mantleInterval) {
        if (innerCoreInterval < eps || outerCoreInterval < eps || mantleInterval < eps)
            throw new RuntimeException("Intervals are too small.");
        createSimpleMesh(structure, innerCoreInterval, outerCoreInterval, mantleInterval);
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

    private void createSimpleMesh(VelocityStructure structure, double innerCoreInterval, double outerCoreInterval,
                                  double mantleInterval) {
        double[] mantleBoundaries = DoubleStream
                .concat(DoubleStream.of(structure.coreMantleBoundary(), structure.earthRadius()),
                        Arrays.stream(structure.additionalBoundaries()).filter(b -> structure.coreMantleBoundary() < b))
                .sorted().toArray();
        double[] outerCoreBoundaries = DoubleStream
                .concat(DoubleStream.of(structure.coreMantleBoundary(), structure.innerCoreBoundary()),
                        Arrays.stream(structure.additionalBoundaries())
                                .filter(b -> structure.innerCoreBoundary() < b && b < structure.coreMantleBoundary()))
                .sorted().toArray();
        double[] innerCoreBoundaries = DoubleStream.concat(DoubleStream.of(0, structure.innerCoreBoundary()),
                Arrays.stream(structure.additionalBoundaries()).filter(b -> b < structure.innerCoreBoundary())).sorted()
                .toArray();
        double[][] mantles = new double[mantleBoundaries.length - 1][];
        for (int i = 0; i < mantles.length; i++)
            mantles[i] = point(mantleBoundaries[i], mantleBoundaries[i + 1], mantleInterval);
        double[][] outerCores = new double[outerCoreBoundaries.length - 1][];
        for (int i = 0; i < outerCores.length; i++)
            outerCores[i] = point(outerCoreBoundaries[i], outerCoreBoundaries[i + 1], outerCoreInterval);
        double[][] innerCores = new double[innerCoreBoundaries.length - 1][];
        for (int i = 0; i < innerCores.length; i++)
            innerCores[i] = point(innerCoreBoundaries[i], innerCoreBoundaries[i + 1], innerCoreInterval);

        mantleMesh = RealVector.unmodifiableRealVector(new ArrayRealVector(
                Arrays.stream(mantles).flatMapToDouble(Arrays::stream).distinct().sorted().toArray(), false));
        outerCoreMesh = RealVector.unmodifiableRealVector(new ArrayRealVector(
                Arrays.stream(outerCores).flatMapToDouble(Arrays::stream).distinct().sorted().toArray(), false));
        innerCoreMesh = RealVector.unmodifiableRealVector(new ArrayRealVector(
                Arrays.stream(innerCores).flatMapToDouble(Arrays::stream).distinct().sorted().toArray(), false));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((innerCoreMesh == null) ? 0 : Arrays.hashCode(innerCoreMesh.toArray()));
        result = prime * result + ((mantleMesh == null) ? 0 : Arrays.hashCode(mantleMesh.toArray()));
        result = prime * result + ((outerCoreMesh == null) ? 0 : Arrays.hashCode(outerCoreMesh.toArray()));
        return result;
    }

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
     * Interval of integration startR to endR is divided by deltaR
     *
     * @param startR [km]
     * @param endR   [km]
     * @param deltaR [km]
     * @return Array of radius [km]
     */
    private static double[] point(double startR, double endR, double deltaR) {
        int n = Math.max(1, (int) Math.round((endR - startR) / deltaR));
        double[] x = new double[n + 1];
        for (int i = 0; i < n; i++)
            x[i] = startR + i * deltaR;
        x[0] = startR + eps;
        x[n] = endR - eps;
        return x;
    }

    /**
     * @param r         [km] must be [min, max(+{@link #eps})] of the partition.
     * @param partition in which the r is found
     * @return index of maximum r<sub>i</sub> (r<sub>i</sub> &le; r) in the
     * mesh. If the mesh includes r, it returns the index of r
     * <sub>i</sub> (= r).<br>
     * Note that the return value is the index in certain partition
     * mesh.
     */
    int getNextIndexOf(double r, Partition partition) {
        RealVector mesh = getMesh(partition);
        if (r < mesh.getEntry(0) - eps || mesh.getEntry(mesh.getDimension() - 1) + eps < r)
            throw new IllegalArgumentException("Input " + r + " is out of " + partition);
        if (r <= mesh.getEntry(0)) return 0;
        for (int i = 1; i < mesh.getDimension(); i++)
            if (r < mesh.getEntry(i)) return i - 1;

        return mesh.getDimension() - 1;
    }

    public static ComputationalMesh simple(VelocityStructure structure) {
        return new ComputationalMesh(structure, 1, 1, 1);
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
