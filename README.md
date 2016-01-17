#Kibrary 
![version][version-image]
[![release] [release-image] ][release]
[ ![aLicense] [alicense-image] ] [alicense]
[ ![oLicense] [olicense-image] ] [olicense]
[![Java8][Java8-image]] [Java8]  


Library for waveform inversion.   

It bundles [ANISOtime][ANISOtime] package.  

[Java SE Runtime Environment(JRE) 8][JRE8] is required.

##Installation
Here is a script [build.gradle][gradlescript] for *Gradle 2.10*.  
If no *Gradle 2.10* in your computer, download and extract [gradlew.tar][gradlew] and execute *gradlew*
and then type **gradlew build**,  
you will see a jar file of *Kibrary* in build/libs.

```bash
 host$ tar xf gradlew.tar
 host$ ./gradlew
 host$ ./gradlew build 
```

##Usage
Please see [Javadoc][javadoc]. [Wiki][wiki] is still under construction.

##Contact me
If you have any comments or questions, please feel free to contact me by [E-mail][email].

##Copyright and Licence
Copyright © 2015 Kensuke Konishi  
Licensed under [Apache-2][alicense] and [Oracle BSD License][olicense]


[release-image]:https://img.shields.io/badge/release-Goblin-pink.svg
[release]:https://en.wikipedia.org/wiki/Goblin
[version-image]:https://img.shields.io/badge/version-0.2.4rc-yellow.svg

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
