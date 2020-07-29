# Kibrary 

![version][version-image]
[![release][release-image]][release]
[![gLicense][gplicense-image]][gplicense]
[![Java8][Java8-image]][Java8]

Library for waveform inversion.   
It bundles [ANISOtime](https://github.com/kensuke1984/Kibrary/wiki/ANISOtime) ([ANISOtime][ANISOtime]) package.  


## <a name="installation">Installation
 
 <i><b><a name ="java">Java environment</b></i>
 
Kibrary currently runs on *[Java SE Runtime Environment 8][JRE8]* or higher (Java 14 is strongly recommended).
If you are not sure about the version you have, 
click <a href="https://www.java.com/en/download/installed8.jsp" target="_blank">here</a> to check. 
 
You can download from [Oracle](https://www.oracle.com/technetwork/java/javase/downloads/index.html),
while you might want to manage by something like [sdkman](https://sdkman.io/).
If you are a macOS user and have [Homebrew](https://brew.sh) installed, then you can have the latest Java as below.
```bash
 % brew update
 % brew cask install java
```

 <i><b>Executable jar file</b></i>
 
Most general users just need the jar file (and [Java](#java)).
Download the [binary release of Kibrary][kibraryjar].
If you do not agree with [the terms and conditions](#copyright-and-licence), do NOT download the software.

If you just want to use Kibrary, just install as [above](#installation).
If you would like to install useful launchers, execute [this](https://bit.ly/2YUfEB6).
If you have [curl](http://curl.haxx.se/) or [GNU Wget](https://www.gnu.org/software/wget/), paste this at a Terminal prompt.
```bash
#If you have curl installed 
 % kins=$(mktemp) && curl -sL -o $kins https://bit.ly/2YUfEB6 && /bin/sh $kins && rm -f $kins
#else if wget is installed, try
 % kins=$(mktemp) && wget -q -O $kins https://bit.ly/2YUfEB6 && /bin/sh $kins && rm -f $kins
```
If you use an old version of downloader ([curl](https://curl.haxx.se/) or [GNU Wget](https://www.gnu.org/software/wget/)), 
the download may fail. In that case, you must update it, otherwise you can download the [binary release of Kibrary][kibraryjar]. 

The necessary/bundled libraries are  
[*Apache Commons CLI*][cli], [*Apache Commons Email*][email], [*Apache Commons IO*][io],
[*Apache Commons LANG*][lang], [*Apache Commons MATH*][math], [*Apache Commons NET*][net],
[*Epsgraphics*][eps], [*javax.mail*][mail].  
The latest versions are strongly recommended.


 <i>Build by yourself</i>

If you would like to have source files, just get them using ```git``` like below:

```bash
 % cd /path/to/install
 % git clone https://github.com/kensuke1984/Kibrary.git
```

To solve dependencies, ```build.gradle``` is prepared. If you do not have ```gradle```, this might help:

```bash
 % cd /path/to/install
 #If you have curl installed 
 % kins=$(mktemp) && curl -sL -o $kins https://bit.ly/380vUbe && tar xf $kins && rm -f $kins
 #else if wget is installed, try
 % kins=$(mktemp) && wget -q -O $kins https://bit.ly/38OvUbe && tar xf $kins && rm -f $kins
 % ./gradlew
 % ./gradlew build
```

This makes an all-in-one(dependencies) jar file (```kibrary-x.x.jar```) in the ```/path/to/install/build/libs```.

## Usage
Please see [Javadoc][javadoc]. [Wiki][wiki] is still under construction.

## Contact me
If you have any comments or questions, please feel free to contact me by [E-mail][mailto].

## Copyright and Licence
Copyright © 2015 Kensuke Konishi  
Licensed under [GNU General Public License v3][gplicense]  
Last updated Jul 28, 2020


[release-image]:https://img.shields.io/badge/release-Shiva-pink.svg
[release]:https://en.wikipedia.org/wiki/Shiva
[version-image]:https://img.shields.io/badge/version-0.4.9-yellow.svg

[alicense-image]: https://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[alicense]: https://www.apache.org/licenses/LICENSE-2.0

[olicense-image]: http://img.shields.io/badge/license-Oracle-blue.svg?style=flat
[olicense]: https://www.oracle.com/technetwork/licenses/bsd-license-1835287.html

[gplicense]: https://www.gnu.org/licenses/gpl-3.0.html
[gplicense-image]: http://img.shields.io/badge/license-GPL--3.0-blue.svg?style=flat


[ANISOtime]: http://www-solid.eps.s.u-tokyo.ac.jp/~dsm/anisotime.html

[kibraryjar]: https://bit.ly/305JHrE

[Java8-image]:https://img.shields.io/badge/dependencies-JRE%208-brightgreen.svg
[Java8]:https://www.java.com/
[JRE8]:https://www.oracle.com/technetwork/java/javase/downloads/index.html
[gradlescript]:https://kensuke1984.github.io/build.gradle
[gradlew]:https://kensuke1984.github.io/gradlew.tar

[wiki]:https://github.com/kensuke1984/Kibrary/wiki
[mailto]:mailto:kensuke@earth.sinica.edu.tw
[javadoc]:https://kensuke1984.github.io/Kibrary

[cli]:https://commons.apache.org/proper/commons-cli/
[email]:https://commons.apache.org/proper/commons-email/
[io]:https://commons.apache.org/proper/commons-io/
[lang]:https://commons.apache.org/proper/commons-lang/
[math]:https://commons.apache.org/proper/commons-math/
[net]:https://commons.apache.org/proper/commons-net/
[eps]:https://www.abeel.be/wiki/EPSGraphics
[mail]:https://java.net/projects/javamail/pages/Home


