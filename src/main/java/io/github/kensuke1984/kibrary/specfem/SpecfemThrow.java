/**
 * 
 */
package io.github.kensuke1984.kibrary.specfem;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * @version 0.0.1
 * @since 2018/03/08
 * @author Yuki
 *
 */
public class SpecfemThrow implements Closeable {
	
	public static void main(String[] args) {
		//サーバ
        String host = "eic.eri.u-tokyo.ac.jp";
        //ポート
        int port = 22;
        //ユーザ
        String user = "yukis317";
        String privateKeyPath = "/home/suzuki/.ssh/id_rsa";
		SpecfemThrow jobthrow = new SpecfemThrow(host, user, port, privateKeyPath);
		String runmesh = "qsub calMESH.csh";
	    String runsolver = "qsub calSPECFEM.csh";
	    String checkstatus = "qstatus | grep yukis317";
	    String quenumber = "qstatus | grep yukis317 | grep PEND | wc | awk '{print $1}'";
	    try {
			jobthrow.execute(quenumber);
			
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
	
	private Session session;	 
	private ChannelExec channel;
	 
	  /**
	   * コンストラクタ
	   * @param host
	   * @param userName
	   * @param port
	   * @throws Exception
	   */
	  public SpecfemThrow (String host, String userName, int port, String privateKeyPath) {
	    try {
	      JSch jsch = new JSch();
	      jsch.setKnownHosts("/home/suzuki/.ssh/known_hosts");
	      session = jsch.getSession(userName, host, port);
//	      session.setPassword(password);
	      jsch.addIdentity(privateKeyPath);
	      session.connect();
	      channel = (ChannelExec)session.openChannel("exec");
	    } catch (JSchException e) {
	      // 例外時の処理
	    	e.getStackTrace();
	    }
	  }
	 
	    /**
	     * コマンドを実行する. jobを投げる.
	     * @param command
	     * @return 処理結果
	     * @throws IOException
	     * @throws JSchException
	     */
	    public int execute(String command) throws Exception {
	      // コマンド実行する。
	      this.channel.setCommand(command);
	      channel.connect();
	      // エラーメッセージ用Stream
	        BufferedInputStream errStream = new BufferedInputStream(channel.getInputStream());
	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        byte[] buf = new byte[1024];
	        while (true) {
	            int len = errStream.read(buf);
	            if (len <= 0) {
	                break;
	            }
	            outputStream.write(buf, 0, len);
	        }
	        // エラーメッセージ取得する
	        String message = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
//	        System.out.println(message);
	        channel.disconnect();
	        // コマンドの戻り値を取得する
	        int returnCode = channel.getExitStatus();
	        return  returnCode;
	    }
	 
	  @Override
	  public void close() {
	    session.disconnect();
	  }
	  
	    /**
	     * Timerの設定
	     * 3600s=1hr毎に1回jobStatusをチェック
	     * @throws InterruptedException
	     */
	    public void timer_period() throws InterruptedException {
			TimerTask task = new SampleTask();
			Timer timer = new Timer("時間間隔タイマー");

			System.out.println("main start：" + new Date());
			timer.schedule(task, 0, 5 * 3600);

			TimeUnit.SECONDS.sleep(300);
			timer.cancel();
			System.out.println("main end  ：" + new Date());
		}
	    
	    class SampleTask extends TimerTask {
	    	/** このメソッドがTimerから呼ばれる */
	    	@Override
	    	public void run() {
	    		System.out.println("タスク実行：" + new Date());
	    	}
	    }
}