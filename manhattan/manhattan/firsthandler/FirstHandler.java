package manhattan.firsthandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import manhattan.template.Utilities;

/**
 * firsthandler.plを移植する 設定ファイルは {@link parameter.FirstHandler}で読み込む First
 * handler. This extracts {@link SEEDFile}s under a working golder
 * 
 * 
 * @since 2013/9/19
 * @version 0.0.1 作業フォルダ下にある.seed ファイルに対して処理を行う 同じイベントに対するseedが存在するとエラー
 *          seedのデータは日付を、またいでいないこととする 確認済み １ rdseed -rfd hogeによるRESP.*の出力は全く同じ
 *          *.SACはSCALEの値だけ異なる しかしIRIS HPによると現在は使われていないので無視 確認済み ２
 *          古いsacのバージョンと新しいsacのバージョンで rtrend やinterpolate 後のsacの値が多少違う
 * 
 *          seed解凍後 channelがBH[ENZ]のものだけから読み込む BH[123]は今のところ使わない
 * 
 * 
 * @version 0.0.2 Workerを独立 {@link SeedSac}
 * 
 * @version 0.1.0 日付をまたいでokにした
 * 
 * @version 0.1.0 解凍後の.seed ファイルをまとめることにした
 * 
 *          TODO NPTSで合わないものを捨てる？
 * 
 * @version 0.1.1 evalresp path fixed
 * @version 0.1.2 ignored Seed folders
 * @since 2014/8/14
 * @version 0.1.3 add an option for removing intermediate files.
 * 
 * @version 0.1.4
 * @since 2014/9/6 to Java 8
 * 
 * @version 0.1.5
 * @since 2014/10/2 rdseed, evalresp need be in PATH
 * 
 * @version 0.1.6
 * @since 2015/8/8 {@link IOException} {@link Path} base
 * 
 * @version 0.1.7
 * @since 2015/8/19 {@link Path} base
 * 
 * @author kensuke
 * 
 */
class FirstHandler extends parameter.FirstHandler {

	private FirstHandler(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	/**
	 * output directory
	 */
	private Path outPath;

	private static FirstHandler parse(String[] args) throws IOException {
		FirstHandler fh = null;
		if (1 == args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			fh = new FirstHandler(parameterPath);
		} else if (args.length == 0)
			fh = new FirstHandler(null);

		return fh;
	}

	/**
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		FirstHandler fh = parse(args);
		long startT = System.nanoTime();
		System.err.println("FirstHandler is going");
		System.err.println("Working directory is " + fh.workPath);

		// check if conditions. if for example there are already existing output
		// files, this program starts here,
		if (!Files.exists(fh.workPath))
			throw new NoSuchFileException(fh.workPath.toString());
		fh.outPath = fh.workPath.resolve("fh" + Utilities.getTemporaryString());
		Path goodSeedPath = fh.outPath.resolve("goodSeeds");
		Path badSeedPath = fh.outPath.resolve("badSeeds");
		Path ignoredSeedPath = fh.outPath.resolve("ignoredSeeds");

		Set<Path> seedPaths = fh.findSeedFiles();
		System.err.println(seedPaths.size() + " seeds are found.");
		if (seedPaths.isEmpty())
			return;

		// creates environment (make output folder ...)
		Files.createDirectories(fh.outPath);
		System.out.println("Output directory is " + fh.outPath);

		Set<SeedSac> seedSacs = seedPaths.stream().map(seedPath -> {
			try {
				return new SeedSac(seedPath, fh.outPath);
			} catch (Exception e) {
				try {
					System.out.println(seedPath + " has problem. " + e);
					Utilities.moveToDirectory(seedPath, ignoredSeedPath, true);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toSet());

		seedSacs.forEach(ss -> ss.setRemoveIntermediateFiles(fh.removeIntermediateFile));

		int threadNum = Runtime.getRuntime().availableProcessors();
		ExecutorService es = Executors.newFixedThreadPool(threadNum);

		seedSacs.forEach(ss -> es.submit(() -> ss.run()));

		es.shutdown();
		try {
			while (!es.isTerminated())
				Thread.sleep(1000 * 5);
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		for (SeedSac seedSac : seedSacs)
			try {
				if (seedSac == null)
					continue;
				if (!seedSac.hadRun())
					Utilities.moveToDirectory(seedSac.getSeedPath(), ignoredSeedPath, true);
				else if (seedSac.hasProblem())
					Utilities.moveToDirectory(seedSac.getSeedPath(), badSeedPath, true);
				else
					Utilities.moveToDirectory(seedSac.getSeedPath(), goodSeedPath, true);
			} catch (Exception e) {
				e.printStackTrace();
			}

		System.err.println("FirstHandler finished in " + Utilities.toTimeString(System.nanoTime() - startT));

	}

	private Set<Path> findSeedFiles() throws IOException {
		try (Stream<Path> workPathStream = Files.list(workPath)) {
			return workPathStream.filter(path -> path.toString().endsWith(".seed")).collect(Collectors.toSet());
		}

	}

}
