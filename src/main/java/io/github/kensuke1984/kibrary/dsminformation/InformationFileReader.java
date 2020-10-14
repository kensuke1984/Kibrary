package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Reader for files which contains c # ! etc for comment lines.
 *
 * @author Kensuke Konishi
 * @version 0.0.2.3
 */
class InformationFileReader {
    /**
     * indicators for comment lines
     */
    private static final char[] commentOutFlag = {'c', 'C', '!', '#'};
    private Path informationPath;
    /**
     * the number of lines already read
     */
    private int readlineNum;
    /**
     * lines in {@link #informationPath}
     */
    private List<String> lines;
    /**
     * the number of total lines
     */
    private int linesNum;

	InformationFileReader(Path informationPath) throws IOException {
		this.informationPath = informationPath;
		read();
	}
	
	InformationFileReader(List<String> lines) {
		this.informationPath = null;
		this.lines = lines;
		linesNum = lines.size();
	}
	
    /**
     * blank line will be also considered to be a comment line
     *
     * @param line to check
     * @return if the input line is comment line or not
     */
    private static boolean isComment(String line) {
        if ((line = line.trim()).isEmpty()) return true;
        for (char flag : commentOutFlag)
            if (line.charAt(0) == flag) return true;
        return false;
    }
	
    /**
     * @param args [information file name]
     * @throws IOException if any
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            InformationFileReader ifr = new InformationFileReader(Paths.get(args[0]));
            String line;
            while (null != (line = ifr.next())) System.out.println(line);
        }
    }

    private void read() throws IOException {
        lines = Files.readAllLines(informationPath);
        linesNum = lines.size();
    }

    /**
     * if the next line is a comment line, it will be skipped. (c#!...) the line
     * will be returned after trimmed
     *
     * @return the next line to the line already read, returns null if all lines
     * are already read
     */
    String next() {
        if (readlineNum == linesNum) return null;
        String line;
        for (; ; )
            if (!isComment(line = lines.get(readlineNum++).trim())) return line;
    }

    /**
     * reset the number of lines already read
     */
    void reset() {
        readlineNum = 0;
    }

    /**
     * all of lines to read may be comment lines..
     *
     * @return if there are lines to read
     */
    boolean hasNext() {
        return readlineNum == linesNum;
    }

    /**
     * @return String[] made of non comment lines
     */
    String[] getNonCommentLines() {
        return lines.stream().filter(line -> !isComment(line)).map(String::trim).toArray(String[]::new);
    }

}
