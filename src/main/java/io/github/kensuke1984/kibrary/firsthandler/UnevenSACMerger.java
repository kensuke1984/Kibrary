package io.github.kensuke1984.kibrary.firsthandler;

import io.github.kensuke1984.kibrary.util.Utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Merging of SAC files
 *
 * @author Kensuke Konishi
 * @version 0.0.4.3
 */
class UnevenSACMerger {

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
     * SacFileNameのリスト
     */
    private SACFileName[] sacFileNameList;
    private Set<SACGroup> sacGroupSet = new HashSet<>();

    /**
     * Uneven Sacをmergeする作業フォルダ
     *
     * @param workPath
     */
    UnevenSACMerger(Path workPath) throws IOException {
        this.workPath = workPath;
        unevenBoxPath = workPath.resolve("mergedUnevendata");
        notMergedBoxPath = workPath.resolve("nonMergedUnevendata");
        listUpSacFiles();
    }

    /**
     * 作業フォルダの下から.SACファイルを拾う
     */
    private void listUpSacFiles() throws IOException {
        // System.out.println("Listing up sac files");

        try (Stream<Path> sacFileStream = Files.list(workPath)) {
            sacFileNameList =
                    sacFileStream.map(path -> path.getFileName().toString()).filter(path -> path.endsWith(".SAC"))
                            .map(SACFileName::new).toArray(SACFileName[]::new);
        }

        // SacGroupをつくる
        createGroups(sacFileNameList);

    }

    /**
     * すべての {@link #sacGroupSet}をmergeする その後ファイルはゴミ箱へ
     */
    void merge() {
        sacGroupSet.forEach(group -> {
            try {
                if (!group.merge()) group.move(notMergedBoxPath);
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
        Arrays.stream(sacFileNameList).map(Object::toString).map(workPath::resolve).filter(Files::exists)
                .forEach(path -> {
                    try {
                        Utilities.moveToDirectory(path, unevenBoxPath, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

    }

    /**
     * 名前に従い、関連するファイルのグループに分ける {@link SACFileName#isRelated(SACFileName)}
     * がtrue同士で分ける
     */
    private void createGroups(SACFileName[] names) {
        for (SACFileName name : names)
            // 既存のグループに振り分けられなかったら新しいグループを作る
            if (sacGroupSet.stream().noneMatch(group -> group.add(name))) sacGroupSet.add(new SACGroup(workPath, name));
    }

}
