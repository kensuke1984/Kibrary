package manhattan.gnuplot;

/**
 * Colors in gnuplot
 * @author kensuke 
 * @version 0.0.1
 */
public enum GnuplotColorNames {
	  white              ,//#ffffff = 255 255 255
	  black              ,//#000000 =   0   0   0
	  gray0              ,//#000000 =   0   0   0
	  grey0              ,//#000000 =   0   0   0
	  gray10             ,//#1a1a1a =  26  26  26
	  grey10             ,//#1a1a1a =  26  26  26
	  gray20             ,//#333333 =  51  51  51
	  grey20             ,//#333333 =  51  51  51
	  gray30             ,//#4d4d4d =  77  77  77
	  grey30             ,//#4d4d4d =  77  77  77
	  gray40             ,//#666666 = 102 102 102
	  grey40             ,//#666666 = 102 102 102
	  gray50             ,//#7f7f7f = 127 127 127
	  grey50             ,//#7f7f7f = 127 127 127
	  gray60             ,//#999999 = 153 153 153
	  grey60             ,//#999999 = 153 153 153
	  gray70             ,//#b3b3b3 = 179 179 179
	  grey70             ,//#b3b3b3 = 179 179 179
	  gray80             ,//#cccccc = 204 204 204
	  grey80             ,//#cccccc = 204 204 204
	  gray90             ,//#e5e5e5 = 229 229 229
	  grey90             ,//#e5e5e5 = 229 229 229
	  gray100            ,//#ffffff = 255 255 255
	  grey100            ,//#ffffff = 255 255 255
	  gray               ,//#bebebe = 190 190 190
	  grey               ,//#bebebe = 190 190 190
	  light_gray         ,//#d3d3d3 = 211 211 211
	  light_grey         ,//#d3d3d3 = 211 211 211
	  dark_gray          ,//#a9a9a9 = 169 169 169
	  dark_grey          ,//#a9a9a9 = 169 169 169
	  red                ,//#ff0000 = 255   0   0
	  light_red          ,//#f03232 = 240  50  50
	  dark_red           ,//#8b0000 = 139   0   0
	  yellow             ,//#ffff00 = 255 255   0
	  light_yellow       ,//#ffffe0 = 255 255 224
	  dark_yellow        ,//#c8c800 = 200 200   0
	  green              ,//#00ff00 =   0 255   0
	  light_green        ,//#90ee90 = 144 238 144
	  dark_green         ,//#006400 =   0 100   0
	  spring_green       ,//#00ff7f =   0 255 127
	  forest_green       ,//#228b22 =  34 139  34
	  sea_green          ,//#2e8b57 =  46 139  87
	  blue               ,//#0000ff =   0   0 255
	  light_blue         ,//#add8e6 = 173 216 230
	  dark_blue          ,//#00008b =   0   0 139
	  midnight_blue      ,//#191970 =  25  25 112
	  navy               ,//#000080 =   0   0 128
	  medium_blue        ,//#0000cd =   0   0 205
	  royalblue          ,//#4169e1 =  65 105 225
	  skyblue            ,//#87ceeb = 135 206 235
	  cyan               ,//#00ffff =   0 255 255
	  light_cyan         ,//#e0ffff = 224 255 255
	  dark_cyan          ,//#008b8b =   0 139 139
	  magenta            ,//#ff00ff = 255   0 255
	  light_magenta      ,//#f055f0 = 240  85 240
	  dark_magenta       ,//#8b008b = 139   0 139
	  turquoise          ,//#40e0d0 =  64 224 208
	  light_turquoise    ,//#afeeee = 175 238 238
	  dark_turquoise     ,//#00ced1 =   0 206 209
	  pink               ,//#ffc0cb = 255 192 203
	  light_pink         ,//#ffb6c1 = 255 182 193
	  dark_pink          ,//#ff1493 = 255  20 147
	  coral              ,//#ff7f50 = 255 127  80
	  light_coral        ,//#f08080 = 240 128 128
	  orange_red         ,//#ff4500 = 255  69   0
	  salmon             ,//#fa8072 = 250 128 114
	  light_salmon       ,//#ffa07a = 255 160 122
	  dark_salmon        ,//#e9967a = 233 150 122
	  aquamarine         ,//#7fffd4 = 127 255 212
	  khaki              ,//#f0e68c = 240 230 140
	  dark_khaki         ,//#bdb76b = 189 183 107
	  goldenrod          ,//#daa520 = 218 165  32
	  light_goldenrod    ,//#eedd82 = 238 221 130
	  dark_goldenrod     ,//#b8860b = 184 134  11
	  gold               ,//#ffd700 = 255 215   0
	  beige              ,//#f5f5dc = 245 245 220
	  brown              ,//#a52a2a = 165  42  42
	  orange             ,//#ffa500 = 255 165   0
	  dark_orange        ,//#ff8c00 = 255 140   0
	  violet             ,//#ee82ee = 238 130 238
	  dark_violet        ,//#9400d3 = 148   0 211
	  plum               ,//#dda0dd = 221 160 221
	  purple             ,//#a020f0 = 160  32 240
;
	  public String nameColorName(){
		  String colorName = this.name();
		  colorName = colorName.replaceAll("_", "-");
		  return colorName;
	  }

}
