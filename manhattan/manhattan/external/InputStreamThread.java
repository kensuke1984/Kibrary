package manhattan.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * http://www.ne.jp/asahi/hishidama/home/tech/java/process.html#ProcessBuilder
 * を参考にして
 * 
 * 外部プログラムを実行時の標準出力　標準エラーの読み込みスレッド
 * @since 2013/9/23
 * @version 0.0.1
 * 
 * @since 2014/8/18
 * @version 0.0.2
 * &rarr; public
 * try-resource Java7 grammer
 * {@link #waitAndGetString()} installed
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
	 * @return {@link String}[] from input stream
	 */
	public String[] waitAndGetString(){
		try {
			while(!closed)
				Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list.toArray(new String[list.size()]);
	}
	
	@Override
	public void run() {
//		System.out.println("monitoring");
		try(BufferedReader br = new BufferedReader(inputStreamReader)) {
			for (;;) {
//				System.out.println(0);
				String line = br.readLine();
				if (line == null)
					break;
				list.add(line);
//				System.out.println(line+" "+list.size());
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally{
			closed = true;
		}
	}
	
	
	
	/**
	 * List of String from the stream 
	 * 文字列の取得
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
