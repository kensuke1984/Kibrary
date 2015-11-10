package filehandling.sac;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 2013/7/10 added #writeSacBoolean
 * 
 * @version 0.0.2
 * @since 2013/9/25 public &to; default
 * 
 * @version 0.0.3
 * @since 2015/2/20 {@link #changeEndian(int)} installed.
 * 
 * @version 0.0.3.1
 * @since 2015/9/14
 * 
 * @author kensuke
 * 
 */
final class SACOutputStream extends DataOutputStream {

	SACOutputStream(OutputStream os) {
		super(os);
	}


	void writeSACDouble(double value) throws IOException {
		writeSACInt(Float.floatToIntBits((float) value));
	}

	void writeSACInt(int i) throws IOException {
		int j1 = i << 24;
		int j2 = ((i >> 8) << 24) >>> 8;
		int j3 = ((i >> 16) << 24) >>> 16;
		int j4 = i >>> 24;
		writeInt(j1 | j2 | j3 | j4);
	}

	void writeSACBoolean(boolean bool) throws IOException {
		writeSACInt(bool ? 1 : 0);
	}

	void writeSACString(String s, int i) throws IOException {
		writeBytes(StringUtils.rightPad(s, i));
		return;
	}
}
