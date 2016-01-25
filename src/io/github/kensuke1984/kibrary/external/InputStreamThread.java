package io.github.kensuke1984.kibrary.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is after <a
 * href=http://www.ne.jp/asahi/hishidama/home/tech/java/process.html#
 * ProcessBuilder>here</a>
 * 
 * 
 * 外部プログラムを実行時の標準出力 標準エラーの読み込みスレッド You may have to {@link #join()} after the
 * external program finishes(). {@link ExternalProcess#waitFor()}
 * 
 * @version 0.0.2
 * 
 * 
 * @author kensuke
 * 
 */
public class InputStreamThread extends Thread {

	/**
	 * if the stream is closed
	 */
	private boolean closed;

	/**
	 * Wait until the inputstream is closed and return String[]
	 * 
	 * @return {@link String}[] from input stream
	 */
	public String[] waitAndGetString() {
		try {
			while (!closed)
				Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list.toArray(new String[list.size()]);
	}

	@Override
	public void run() {
		try (BufferedReader br = new BufferedReader(inputStreamReader)) {
			for (;;) {
				String line = br.readLine();
				if (line == null)
					break;
				list.add(line);
				// System.out.println(line+" "+list.size());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			closed = true;
		}
	}

	/**
	 * List of String from the stream 文字列の取得
	 * 
	 * @return {@link List} of {@link String} from the {@link InputStream}
	 */
	public List<String> getStringList() {
		return new ArrayList<>(list);
	}

	private List<String> list = new ArrayList<>();

	private InputStreamReader inputStreamReader;

	public InputStreamThread(InputStream is) {
		inputStreamReader = new InputStreamReader(is);
	}

}
