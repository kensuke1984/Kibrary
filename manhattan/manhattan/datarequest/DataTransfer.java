/**
 * 
 */
package manhattan.datarequest;

import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;

import manhattan.template.Utilities;

/**
 * FTP access to IRIS server. OHP will be prepared
 * 
 * @author kensuke
 * @version 0.0.1
 */
final class DataTransfer {

	private DataTransfer() {
	}

	/**
	 * user PATH in IRIS
	 */
	private static final String irisUserPath = "/pub/userdata/" + System.getProperty("user.name") + "/";

	private static void get(String date, Path outPath) {
		// create an FTPClient
		FTPClient ftpclient = new FTPClient();
		try {
			// connect
			ftpclient.connect("ftp.iris.washington.edu");
			int reply = ftpclient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply))
				throw new RuntimeException("connect fail");
			// login
			if (ftpclient.login("anonymous", "password") == false)
				throw new RuntimeException("login fail");
			// passive mode
			ftpclient.enterLocalPassiveMode();
			// binary mode
			ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
			// ftpclient.changeWorkingDirectory(userPath);

			FTPFileFilter fff = file -> date.equals("*") ? file.getName().endsWith(".seed")
					: file.getName().endsWith(".seed") && file.getName().contains(date);
			FTPFile[] ffiles = ftpclient.listFiles(irisUserPath, fff);
			System.out.println(ffiles.length + " seed files are found in the server.");
			System.out.println("Downloading in 10 s");
			Thread.sleep(10 * 1000);
			Files.createDirectories(outPath);
			for (FTPFile ffile : ffiles)
				try (BufferedOutputStream ostream = new BufferedOutputStream(
						Files.newOutputStream(outPath.resolve(ffile.getName()), StandardOpenOption.CREATE_NEW))) {
					// ファイル受信
					System.out.println("receiving " + ffile.getName());
					ftpclient.retrieveFile(irisUserPath + "/" + ffile.getName(), ostream);
					// System.out.println(m);
				} catch (Exception e) {
					e.printStackTrace();
				}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ftpclient.isConnected())
				try {
					ftpclient.disconnect();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
		}
	}

	/**
	 * @param args
	 *            [option] [tag]<br>
	 *            If option -c, then check the number of files with "tag", else
	 *            FTP [date string] to get seed files(*.seed) in
	 *            (/pub/userdata/`USERNAME`/) with the `tag`. If "*" (you might
	 *            need "\*"), then get all seed files in the folder. <br>
	 */
	public static void main(String[] args) {
		if (2 < args.length || (args.length == 2 && !args[0].equals("-c")))
			throw new IllegalArgumentException(
					"Usage:[-c](to check the number only) [tag] or [*] (may need be \\*) if all seeds you need");

		try {
			Path outPath = Paths.get("seedsTransferredAt" + Utilities.getTemporaryString());
			get(args[0], outPath);
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
