# Kibrary 

![version][version-image]
[![release][release-image]][release]
[![aLicense][alicense-image]][alicense]
[![oLicense][olicense-image]][olicense]
[![Java8][Java8-image]][Java8]

Library for waveform inversion.   
It bundles [ANISOtime](https://github.com/kensuke1984/Kibrary/wiki/ANISOtime) ([ANISOtime][ANISOtime]) package.  


## <a name="installation">Installation
 
 <i><b><a name ="java">Java environment</b></i>
 
Kibrary currently runs on *Java SE Runtime Environment 8*.
You need [Java SE Runtime Environment 8][JRE8] or higher (the latest version is strongly recommended).
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
Download the [binary release of Kibrary](https://www.dropbox.com/s/utep6ep1l1bxe3d/kibrary-0.4.5.jar?dl=1).
If you do not agree with [the terms and conditions](#copyright-and-licence), do NOT download the software.



 <i>Build by yourself</i>

If you just want to use Kibrary, just install as [above](#installation).
If you would like to have source files, execute [this](https://kensuke1984.github.io/bin/install.sh).
If you have [curl](http://curl.haxx.se/) or [GNU Wget](https://www.gnu.org/software/wget/), paste this at a Terminal prompt.
```bash
#If you have curl installed 
 % kins=$(mktemp) && curl -s -o $kins https://kensuke1984.github.io/bin/install.sh && /bin/sh $kins && rm -f $kins
#else if wget is installed, try
 % kins=$(mktemp) && wget -q -O $kins https://kensuke1984.github.io/bin/install.sh && /bin/sh $kins && rm -f $kins
```
If you use an old version of downloader ([curl](http://curl.haxx.se/) or [GNU Wget](https://www.gnu.org/software/wget/)), 
the download may fail. In that case, you must update it, 

If you want to clean install, use ```/bin/sh $kins -f``` instead of ```/bin/sh $kins```.
Java Development Kit is required for compiling Kibrary. 

otherwise you can download the [binary release of Kibrary](https://www.dropbox.com/s/utep6ep1l1bxe3d/kibrary-0.4.5.jar?dl=1). 

The necessary/bundled libraries are  
[*Apache Commons CLI*][cli], [*Apache Commons Email*][email], [*Apache Commons IO*][io],
[*Apache Commons LANG*][lang], [*Apache Commons MATH*][math], [*Apache Commons NET*][net],
[*Epsgraphics*][eps], [*javax.mail*][mail].  
The latest versions are strongly recommended.



## Usage
Please see [Javadoc][javadoc]. [Wiki][wiki] is still under construction.

## Contact me
If you have any comments or questions, please feel free to contact me by [E-mail][mailto].

## Copyright and Licence
Copyright © 2015 Kensuke Konishi  
Licensed under [Apache-2][alicense] and [Oracle BSD License][olicense]  
Last updated Nov 5, 2019


[release-image]:https://img.shields.io/badge/release-Titan-pink.svg
[release]:https://en.wikipedia.org/wiki/Titan_(mythology)
[version-image]:https://img.shields.io/badge/version-0.4.5-yellow.svg

[alicense-image]: https://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[alicense]: https://www.apache.org/licenses/LICENSE-2.0

[olicense-image]: http://img.shields.io/badge/license-Oracle-blue.svg?style=flat
[olicense]: http://www.oracle.com/technetwork/licenses/bsd-license-1835287.html

[ANISOtime]:http://www-solid.eps.s.u-tokyo.ac.jp/~dsm/anisotime.html

[Java8-image]:https://img.shields.io/badge/dependencies-JRE%208-brightgreen.svg
[Java8]:https://www.java.com/
[JRE8]:http://www.oracle.com/technetwork/java/javase/downloads/index.html
[gradlescript]:http://kensuke1984.github.io/build.gradle
[gradlew]:http://kensuke1984.github.io/gradlew.tar

[wiki]:https://github.com/kensuke1984/Kibrary/wiki
[mailto]:mailto:kensuke@earth.sinica.edu.tw
[javadoc]:https://kensuke1984.github.io/Kibrary

[cli]:http://commons.apache.org/proper/commons-cli/
[email]:http://commons.apache.org/proper/commons-email/
[io]:http://commons.apache.org/proper/commons-io/
[lang]:http://commons.apache.org/proper/commons-lang/
[math]:http://commons.apache.org/proper/commons-math/
[net]:http://commons.apache.org/proper/commons-net/
[eps]:http://www.abeel.be/wiki/EPSGraphics
[mail]:https://java.net/projects/javamail/pages/Home


