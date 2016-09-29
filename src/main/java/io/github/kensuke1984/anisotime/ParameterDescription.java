/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/**
 * 
 * Parameter description used in ANISOtime.
 * 
 * @version 0.0.3.2
 * @author Kensuke Konishi
 * 
 * 
 */
final class ParameterDescription {

	static JFrame createFrameDepth() {
		JFrame f = new JFrame("Depth [km]");
		String html = "<html><br>Depth [km]:<br>&nbsp;source depth</html>";
		JLabel label = new JLabel(html);
		label.setBorder(new EmptyBorder(0, 10, 0, 0));
		f.add(label, BorderLayout.NORTH);
		f.setLocationRelativeTo(null);
		f.setSize(300, 100);
		f.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		return f;
	}

	static JFrame createFrameAllowableError() {
		JFrame f = new JFrame("Allowable error [km]");
		String html = "<html><br>Do NOT need to touch (see Konishi <i>et al</i>., 2014 for detail)";
		JLabel label = new JLabel(html);
		f.setSize(500, 100);
		f.add(label, BorderLayout.NORTH);
		label.setBorder(new EmptyBorder(0, 10, 0, 0));
		// label.setHorizontalAlignment(SwingConstants.CENTER);
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		return f;
	}

	static JFrame createFrameInterval() {
		JFrame f = new JFrame("Integration interval [km]");
		String html = "<html><br>Do NOT need to touch (see Konishi <i>et al</i>., 2014 for detail)";
		JLabel label = new JLabel(html);
		f.setSize(500, 100);
		f.add(label, BorderLayout.NORTH);
		label.setBorder(new EmptyBorder(0, 10, 0, 0));
		// label.setHorizontalAlignment(SwingConstants.CENTER);
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		return f;
	}

	static JFrame createFramePhase() {
		JFrame f = new JFrame("Phase");
		String html = "<html><br>Phase:<br>"
				+ "&nbsp;Typical phase are prepared. You can also identify the name of phase in the convention of XXXX"
				+ "&nbsp;(ex. P, S, ScS, PKP...)";
		JLabel label = new JLabel(html);
		f.setSize(500, 150);
		f.add(label, BorderLayout.NORTH);
		label.setBorder(new EmptyBorder(0, 10, 0, 0));
		// label.setHorizontalAlignment(SwingConstants.CENTER);
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		return f;
	}

	static JFrame createFrameSHSV() {
		JFrame f = new JFrame();
		f.setTitle("SH/SV");
		String html = "<html><br>SH/SV:<br>"
				+ "&nbsp;mode of S polarization (V: vertical, H: horizontal)";
		JLabel label = new JLabel(html);
		f.setSize(500, 150);
		f.add(label, BorderLayout.NORTH);
		label.setBorder(new EmptyBorder(0, 10, 0, 0));
		// label.setHorizontalAlignment(SwingConstants.CENTER);
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		return f;
	}

	static JFrame createFrameModel() {
		JFrame f = new JFrame();
		f.setTitle("Model");
		String html = "<html><br>Model:<br>"
				+ "&nbsp;PREM: Dziewonski &amp; Anderson (1984)'s 1 sec model. "
				+ "The surface is replaced by solid (not ocean)."
				+ "<br>&nbsp;AK135: Kennet <i>et al</i>. (1995) improved model of iasp91"
				+ "<br>&nbsp;Polynomial file: a file written in polynomial form";
		JLabel label = new JLabel(html);
		f.setSize(500, 150);
		f.add(label, BorderLayout.NORTH);
		label.setBorder(new EmptyBorder(0, 10, 0, 0));
		// label.setHorizontalAlignment(SwingConstants.CENTER);
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		return f;
	}

	static JFrame createFrameRayparameter() {
		JFrame f = new JFrame();
		f.setTitle("Rayparameter [s]");
		String html = "<html><br>Rayparameter <i>p</i> [s]:&nbsp;dT/d&Delta;"
				+ "<br>&nbsp;T: Travel time[s], &Delta;: Epicentral distance"
				+ "<br>&nbsp;The relationship between <i>p</i> and the angle <i>i</i> is detailed in Konishi <i>et al</i>. (2014)";
		JLabel label = new JLabel(html);
		f.setSize(500, 150);
		f.add(label, BorderLayout.NORTH);
		label.setBorder(new EmptyBorder(0, 10, 0, 0));
		// label.setHorizontalAlignment(SwingConstants.CENTER);
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		return f;
	}
	private ParameterDescription(){}

}
