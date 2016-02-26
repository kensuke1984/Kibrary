package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class MPIControl {

	private final File bucket = new File("/dev/null");

	private Map<String, Integer> npMap;

	private Map<String, Boolean> busyMap;

	MPIControl(Path hostFile) throws IOException {
		List<String> hostList = Files.readAllLines(hostFile);
		npMap = hostList.stream().collect(Collectors.toMap(line -> line, line -> 1, (i, j) -> i + j));
		busyMap = npMap.keySet().stream().collect(Collectors.toMap(k -> k, k -> false));
		pool = Executors.newFixedThreadPool(npMap.size());
		System.out.println("MPI processes run in " + npMap.size() + " hosts.");
	}

	MPIControl(int np) {
		npMap = Collections.singletonMap("localhost", np);
		busyMap = new HashMap<>();
		busyMap.put("localhost", false);
		System.out.println("Each set of MPI processes for each DSM executable runs in " + np + " threads.");
		pool = Executors.newSingleThreadExecutor();
	}

	private Callable<Integer> submit(String host, Path information) {
		return () -> new ProcessBuilder("mpirun", "-host", host, "-np", String.valueOf(npMap.get(host)), "mpi-tish")
				.directory(information.getParent().toFile()).redirectInput(information.toFile()).redirectError(bucket)
				.redirectOutput(bucket).start().waitFor();
	}

	/**
	 * @return host name which is free and it becomes busy when this method
	 *         returns its name
	 */
	private String waitFreeHost() {
		String host = null;
		while (host == null)
			host = getFreeHost();
		return host;
	}

	/**
	 * @return find a free host and set it busy(true)
	 */
	private synchronized String getFreeHost() {
		Optional<String> host = busyMap.entrySet().stream().filter(e -> e.getValue() == false).map(e -> e.getKey())
				.findAny();
		if (!host.isPresent())
			return null;
		busyMap.put(host.get(), true);
		return host.get();
	}

	/**
	 * Set host busy(true)
	 * 
	 * @param host
	 *            name of host
	 */
	private synchronized void setFree(String host) {
		busyMap.put(host, false);
	}

	private ExecutorService pool;

	private Callable<Path> queue(Path information) {
		return () -> {
			String host = waitFreeHost();
			int exit = submit(host, information).call();
			setFree(host);
			return exit == 0 ? information.getParent() : null;
		};
	}

	/**
	 * @param informationPath
	 *            Path of tish information file
	 * @return future of mpi run, the future returns the parent Path of
	 *         information Path
	 */
	Future<Path> tish(Path informationPath) {
		return pool.submit(queue(informationPath));
	};

}
