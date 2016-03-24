package io.github.kensuke1984.kibrary.dsminformation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Reader for files which contains c # ! etc for comment lines.
 * 
 * @version 0.0.2.1
 * 
 * @author Kensuke Konishi
 * 
 */
class InformationFileReader {
	private Path informationPath;

	InformationFileReader(Path informationPath) throws IOException {
		this.informationPath = informationPath;
		read();
	}

	private void read() throws IOException {
		lines = Files.readAllLines(informationPath);
		linesNum = lines.size();
	}

	/**
	 * the number of lines already read
	 */
	private int readlineNum;

	/**
	 * lines in {@link #informationPath}
	 */
	private List<String> lines;

	/**
	 * indicators for comment lines
	 */
	private String[] commentOutFlag = { "c", "C", "!", "#" };

	/**
	 * the number of total lines
	 */
	private int linesNum;

	/**
	 * if the next line is a comment line, it will be skipped. (c#!...) the line
	 * will be returned after trimmed
	 * 
	 * @return the next line to the line already read , returns null if all
	 *         lines are already read
	 */
	String next() {
		if (readlineNum == linesNum)
			return null;
		String line = null;
		for (;;)
			if (!isComment(line = lines.get(readlineNum++).trim()))
				return line;
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
	 * blank line will be also considered to be a comment line
	 * 
	 * @param line
	 * @return if the input line is comment line or not
	 */
	private boolean isComment(String line) {
		line = line.trim();
		if (line.isEmpty())
			return true;
		for (String flag : commentOutFlag)
			if (line.startsWith(flag))
				return true;
		return false;
	}

	/**
	 * @return String[] made of non comment lines
	 */
	String[] getNonCommentLines() {
		return lines.stream().filter(line -> !isComment(line)).toArray(String[]::new);
	}

	/**
	 * @param args
	 *            [information file name]
	 * @throws IOException if any
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			InformationFileReader ifr = new InformationFileReader(Paths.get(args[0]));
			for (;;) {
				String line = ifr.next();
				if (line == null)
					break;
				System.out.println(line);
			}
		}
	}

}
