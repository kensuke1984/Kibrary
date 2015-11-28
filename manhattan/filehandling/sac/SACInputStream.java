package filehandling.sac;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * Input stream for a SAC file
 * 
 * @version 0.1.0
 * 
 * 
 * @author kensuke
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 * @see <a href=https://ds.iris.edu/files/sac-manual/manual/file_format.html>SAC data format</a>
 */
final class SACInputStream extends FilterInputStream {

	SACInputStream(Path sacPath, OpenOption... options) throws IOException {
		super(new BufferedInputStream(Files.newInputStream(sacPath, options)));
	}

	final float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	final int readInt() throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException();
		return (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0);

	}

	/**
	 * See the general contract of the <code>skipBytes</code> method of
	 * <code>DataInput</code>.
	 * <p>
	 * Bytes for this operation are read from the contained input stream.
	 *
	 * @param n
	 *            the number of bytes to be skipped.
	 * @return the actual number of bytes skipped.
	 * @exception IOException
	 *                if the contained input stream does not support seek, or
	 *                the stream has been closed and the contained input stream
	 *                does not support reading after close, or another I/O error
	 *                occurs.
	 */
	final int skipBytes(int n) throws IOException {
		int total = 0;
		int cur = 0;

		while ((total < n) && ((cur = (int) in.skip(n - total)) > 0)) {
			total += cur;
		}

		return total;
	}

	final String readString(int i) throws IOException {
		byte a[] = new byte[i];
		read(a);
		return new String(a, 0, i).trim();
	}

	final boolean readSACBoolean() throws IOException {
		return readInt() == 1;
	}
}
