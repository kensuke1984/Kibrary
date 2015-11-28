package filehandling.sac;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;

/**
 * Output stream for a SAC file
 * @version 0.0.3.1
 * 
 * @author kensuke
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 * @see <a href=https://ds.iris.edu/files/sac-manual/manual/file_format.html>SAC data format</a>
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
