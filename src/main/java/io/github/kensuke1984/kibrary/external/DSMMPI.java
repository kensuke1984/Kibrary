package io.github.kensuke1984.kibrary.external;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Utility for execution of DSM software such as tish and tipsv .<br>
 * They must be in PATH. When you run, mpirun also must be in PATH. <br>
 * <p>
 * All standard write and standard error will be thrown in the same way as the
 * current process.
 * <p>
 * The MPI process will run in the same directory as an input information file,
 * so that you do not care about working folder. (If your information file is
 * tricky, ... ...)
 *
 * @author Kensuke Konishi
 * @version 0.0.2
 */
public final class DSMMPI {
    public final static boolean psvExists = ExternalProcess.isInPath("mpi-tipsv");
    public final static boolean shExists = ExternalProcess.isInPath("mpi-tish");

    static {
        if (System.getProperty("os.name").startsWith("Windows"))
            throw new RuntimeException("So sorry, I love Bill Gates but still this does not work on Windows");
        if (!psvExists) System.err.println("mpi-tipsv is not in PATH");
        if (!shExists) System.err.println("mpi-tish is not in PATH");
        System.err.println("All standard write and error is going to a bit bucket.");
    }

    private DSMMPI() {
    }

    /**
     * Executes: mpirun -np `np` mpi-tipsv &lt; `information` Before the
     * execution, current directory will be the same as the `information` Note
     * that 'mpi-tipsv' must be in $HOME/bin
     *
     * @param np          the number of MPI thread
     * @param information for DSM (tipsv)
     * @return Callable&lt;Integer&gt; for tipsv returning value is exit code.
     */
    public static Callable<Integer> tipsv(int np, Path information) {
        if (!psvExists) throw new RuntimeException("mpi-psv does not exist in PATH");
        if (np <= 0) throw new IllegalArgumentException("np must be positive...");
        if (!Files.exists(information)) throw new RuntimeException(information + " does not exist");

        return () -> {
            System.err.println("mpi-tipsv is going on " + information);
            Path absPath = information.toAbsolutePath();
            int exit = new ProcessBuilder("mpirun", "-np", String.valueOf(np), "mpi-tipsv")
                    .directory(absPath.getParent().toFile()).redirectInput(absPath.toFile())
                    .redirectError(ExternalProcess.bitBucket).redirectOutput(ExternalProcess.bitBucket).start()
                    .waitFor();
            if (exit == 0) System.err.println("looks like mpi-tipsv on " + information + " successfully finished");
            else System.err.println("looks like mpi-tipsv on " + information + " finished with problems");
            return exit;
        };
    }

    /**
     * Executes: mpirun -np `np` mpi-tish &lt; `information` Before the
     * execution, current directory will be the same as the `information` Note
     * that 'mpi-tish' must be in PATH (checked by which).
     *
     * @param np          the number of MPI thread
     * @param information for DSM (tish)
     * @return Callable&lt;Integer&gt; for tish and its returning value is exit code.
     */
    public static Callable<Integer> tish(int np, Path information) {
        if (!shExists) throw new RuntimeException("mpi-sh does not exist in PATH");
        if (np <= 0) throw new IllegalArgumentException("np must be positive...");
        if (!Files.exists(information)) throw new RuntimeException(information + " does not exist");
        return () -> {
            System.err.println("mpi-tish is going on " + information);
            Path absPath = information.toAbsolutePath();
            int exit = new ProcessBuilder("mpirun", "-np", String.valueOf(np), "mpi-tish")
                    .directory(absPath.getParent().toFile()).redirectInput(absPath.toFile())
                    .redirectError(ExternalProcess.bitBucket).redirectOutput(ExternalProcess.bitBucket).start()
                    .waitFor();
            if (exit == 0) System.err.println("looks like mpi-tish on " + information + " successfully finished");
            else System.err.println("looks like mpi-tish on " + information + " finished with problems");
            return exit;
        };
    }

    /**
     * Example:
     * -np 8 -psv hogePSV.inf fooPSV.inf -sh hogeSH.inf fooSH.inf
     * -np is optional. Default is the number of threads in user's environment.
     *
     * @param args arguments for DSMMPI
     * @throws NoSuchFileException if any
     */
    public static void main(String[] args) throws NoSuchFileException {
        if (args.length == 0 || (!args[0].equals("-np") && !args[0].equals("-sh") && !args[0].equals("-psv")))
            throw new IllegalArgumentException(
                    "Usage: (-np threads) -psv (PSV information files...) -sh (SH information files...)");
        List<Callable<Integer>> callList = new ArrayList<>();
        boolean isPSV = false;
        int np = args[0].equals("-np") ? Integer.parseInt(args[1]) : Runtime.getRuntime().availableProcessors();

        if (1 < Arrays.stream(args).filter(s -> s.equals("-np")).count())
            throw new IllegalArgumentException("Option -np (number of threads) appears many times.");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-np":
                    i++;
                    continue;
                case "-sh":
                    isPSV = false;
                    continue;
                case "-psv":
                    isPSV = true;
                    continue;
            }
            Path path = Paths.get(args[i]);
            if (!Files.exists(path)) throw new NoSuchFileException(path.toString());
            callList.add(isPSV ? tipsv(np, path) : tish(np, path));
        }
        callList.forEach(c -> {
            try {
                c.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}
