package manhattan.firsthandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import manhattan.template.Utilities;

/**
 * @version 0.0.1
 * @since 2013/9/20
 * 
 *        unevenなrdseedから解凍されたSacをmergeする 指定したフォルダ内のすべての.SACファイルを対象に検証を行う
 * 
 * @version 0.0.2
 * @since 2014/1/14 つなぎ合わせられるようにした
 * 
 * 
 * @version 0.0.3
 * @since 2014/1/14 moveを外部からに
 * 
 * @version 0.0.3.1
 * @since 2015/8/5 {@link IOException}
 * 
 * 
 * @version 0.0.4
 * @since 2015/8/19 {@link Path} base
 * @author kensuke
 * 
 */
class UnevenSacMerger {

	// private File[] sacFileList;

	/**
	 * 作業フォルダ
	 */
	private Path workPath;

	/**
	 * trash box for uneven files that are merged already マージに成功したファイルの行き先
	 */
	private Path unevenBoxPath;

	/**
	 * box for files that cannot be merged マージできなかったファイルの行き先
	 */
	private Path notMergedBoxPath;

	/**
	 * Uneven Sacをmergeする作業フォルダ
	 * 
	 * @param workPath
	 */
	UnevenSacMerger(Path workPath) throws IOException {
		this.workPath = workPath;
		unevenBoxPath = workPath.resolve("mergedUnevendata");
		notMergedBoxPath = workPath.resolve("nonMergedUnevendata");
		listUpSacFiles();
	}

	/**
	 * SacFileNameのリスト
	 */
	private SacFileName[] sacFileNameList;

	/**
	 * 作業フォルダの下から.SACファイルを拾う
	 */
	private void listUpSacFiles() throws IOException {
		// System.out.println("Listing up sac files");

		try (Stream<Path> sacFileStream = Files.list(workPath)) {
			sacFileNameList = sacFileStream.map(path -> path.getFileName().toString())
					.filter(path -> path.endsWith(".SAC")).map(SacFileName::new).toArray(n -> new SacFileName[n]);
		}

		// SacGroupをつくる
		createGroups(sacFileNameList);

	}

	private Set<SacGroup> sacGroupSet = new HashSet<>();

	/**
	 * すべての {@link #sacGroupSet}をmergeする その後ファイルはゴミ箱へ
	 */
	void merge() {
		sacGroupSet.forEach(group -> {
			try {
				if (!group.merge())
					group.move(notMergedBoxPath);
			} catch (Exception e) {
				group.move(notMergedBoxPath);
			}
		});
	}

	/**
	 * {@link #workPath}内の {@link #sacFileNameList}のすべてを {@link #unevenBoxPath}
	 * にすてる
	 */
	void move() {
		Arrays.stream(sacFileNameList).map(name -> name.toString()).map(workPath::resolve).filter(Files::exists)
				.forEach(path -> {
					try {
						Utilities.moveToDirectory(path, unevenBoxPath, true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

	}

	/**
	 * 名前に従い、関連するファイルのグループに分ける {@link SacFileName#isRelated(SacFileName)}
	 * がtrue同士で分ける
	 */
	private void createGroups(SacFileName[] names) {
		for (int i = 0; i < names.length; i++) {
			SacFileName name = names[i];
			// 既存のグループに振り分けられなかったら新しいグループを作る
			if (!sacGroupSet.stream().anyMatch(group -> group.add(name)))
				sacGroupSet.add(new SacGroup(workPath, names[i]));
			// System.out.println("a new group was made for "+names[i]);

		}

	}

}
