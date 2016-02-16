#Kibrary 
![version][version-image]
[![release] [release-image] ][release]
[ ![aLicense] [alicense-image] ] [alicense]
[ ![oLicense] [olicense-image] ] [olicense]
[![Java8][Java8-image]] [Java8]  


Library for waveform inversion.   

It bundles [ANISOtime][ANISOtime] package.  

[Java SE Runtime Environment(JRE) 8][JRE8] is required.

##<a name="installation"> Installation
Download and extract [gradlew.tar][gradlew] and execute *gradlew*
and then type **gradlew build**,  
you will see a jar file of *Kibrary* in build/libs.

Here is the flow.
```bash
 % wget http://kensuke1984.github.io/gradlew.tar
 % tar xf gradlew.tar
 % ./gradlew
 % ./gradlew build 
```

##Build by yourself
If you just want to use Kibrary, just install as [above](#installation).
However if you want to modify source codes and build by yourself,
you have to care about dependencies by yourself.  
The necessary libraries are below:
[*Apache Commons CLI*][cli], [*Apache Commons Email*][email], [*Apache Commons IO*][io],
[*Apache Commons LANG*][lang], [*Apache Commons MATH*][math], [*Apache Commons NET*][net],
[*Epsgraphics*][eps], [*javax.mail*][mail]




##Usage
Please see [Javadoc][javadoc]. [Wiki][wiki] is still under construction.

##Contact me
If you have any comments or questions, please feel free to contact me by [E-mail][email].

##Copyright and Licence
Copyright © 2015 Kensuke Konishi  
Licensed under [Apache-2][alicense] and [Oracle BSD License][olicense]  
Last updated Feb 16,2016


[release-image]:https://img.shields.io/badge/release-Goblin-pink.svg
[release]:https://en.wikipedia.org/wiki/Goblin
[version-image]:https://img.shields.io/badge/version-0.2.8-yellow.svg

[alicense-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[alicense]: http://www.apache.org/licenses/LICENSE-2.0

[olicense-image]: http://img.shields.io/badge/license-Oracle-blue.svg?style=flat
[olicense]: http://www.oracle.com/technetwork/licenses/bsd-license-1835287.html

[ANISOtime]:http://www-solid.eps.s.u-tokyo.ac.jp/~dsm/anisotime.htm

[Java8-image]:https://img.shields.io/badge/dependencies-JRE%208-brightgreen.svg
[Java8]:https://www.java.com/
[JRE8]:http://www.oracle.com/technetwork/java/javase/downloads/index.html
[gradlescript]:http://kensuke1984.github.io/build.gradle
[gradlew]:http://kensuke1984.github.io/gradlew.tar

[wiki]:https://github.com/kensuke1984/Kibrary/wiki
[email]:mailto:kensuke@earth.sinica.edu.tw
[javadoc]:https://kensuke1984.github.io/Kibrary

[cli]:http://commons.apache.org/proper/commons-cli/
[email]:http://commons.apache.org/proper/commons-email/
[io]:http://commons.apache.org/proper/commons-io/
[lang]:http://commons.apache.org/proper/commons-lang/
[math]:http://commons.apache.org/proper/commons-math/
[net]:http://commons.apache.org/proper/commons-net/
[eps]:http://www.abeel.be/wiki/EPSGraphics
[mail]:https://java.net/projects/javamail/pages/Home


