package io.github.kensuke1984.kibrary.external.gmt;

/**
 * Symbols in GMT
 *
 * @author kensuke
 * @version 0.0.1
 */
public enum Symbol {
    XDASH("-S-"), // x-dash（x軸方向の直線）
    STAR("-Sa"), // star（星）
    CIRCLE("-Sc"), // circle（円）
    DIAMOND("-Sd"), // diamond（ダイヤモンド）
    HEXAGON("-Sh"), // hexagon（六角形）
    INVERTED_TRIANGLE("-Si"), // inverted triangle（逆三角形）
    OCTAGON("-Sg"), // octagon（八角形）
    PENTAGON("-Sn"), // pentagon（五角形）
    SQUARE("-Ss"), // square（四角形）
    TRIANGLE("-St"), // triangle（三角形）
    CROSS("-Sx"), // cross（十字）
    YDASH("-Sy"); // y-dash（y軸方向の直線）

    private String option;

    Symbol(String option) {
        this.option = option;
    }

    /**
     * @return "-S??"
     */
    String getOption() {
        return option;
    }
}
