package io.github.kensuke1984.kibrary.external.gnuplot;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
public class PlotConfig {

    private double[] xrange;
    private double[] yrange;
    private String xtitle;
    private String ytitle;
    private String gpTitle;
    private String output;
    private String plotConfigLine;
    private String terminal;

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
        plotConfigLine = plotConfigLine + "set terminal " + terminal + "\n";
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
        plotConfigLine = plotConfigLine + "set write \"" + output + "\"/n";
    }

    public double[] getXrange() {
        return xrange;
    }

    public void setXrange(double[] xrange) {
        this.xrange = xrange;
        plotConfigLine = plotConfigLine + "set xrange [" + xrange[0] + ":" + xrange[1] + "]\n";
    }

    public double[] getYrange() {
        return yrange;
    }

    public void setYrange(double[] yrange) {
        this.yrange = yrange;
        plotConfigLine = plotConfigLine + "set yrange [" + yrange[0] + ":" + yrange[1] + "]\n";
    }

    public String getXtitle() {
        return xtitle;
    }

    public void setXtitle(String xtitle) {
        this.xtitle = xtitle;
        plotConfigLine = plotConfigLine + "set xtitle \"" + xtitle + "\"\n";
    }

    public String getYtitle() {
        return ytitle;
    }

    public void setYtitle(String ytitle) {
        this.ytitle = ytitle;
        plotConfigLine = plotConfigLine + "set ytitle \"" + ytitle + "\"\n";
    }

    public String getGpTitle() {
        return gpTitle;
    }

    public void setGpTitle(String gpTitle) {
        this.gpTitle = gpTitle;
        plotConfigLine = plotConfigLine + "set title \"" + gpTitle + "\"\n";
    }


}
