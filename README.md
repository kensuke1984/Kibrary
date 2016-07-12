#Kibrary 
![version][version-image]
[![release] [release-image] ][release]
[ ![aLicense] [alicense-image] ] [alicense]
[ ![oLicense] [olicense-image] ] [olicense]
[![Java8][Java8-image]] [Java8]  
Library for waveform inversion.   

It bundles [ANISOtime](https://github.com/kensuke1984/Kibrary/wiki/ANISOtime) ([ANISOtime][ANISOtime]) package.  


##<a name="installation">Installation
Kibrary currently runs on *Java SE Runtime Environment 8*.
You need [Java SE Runtime Environment 8][JRE8] or higher (the latest version is strongly recommended).
If you are not sure about the version you have, 
click <link href="https://www.java.com/en/download/installed8.jsp" target="_blank">here</link> to check.  

<Link to="https://www.java.com/en/download/installed8.jsp" target="_blank"> i</Link>



Download and extract [gradlew.tar][gradlew] and execute *gradlew*
and then type **gradlew build**,  
you will see a jar file of *Kibrary* in build/libs, which you might want to move into CLASSPATH.

Here is the flow to download source codes and build *ANISOtime*.
```bash
 % wget http://kensuke1984.github.io/gradlew.tar
 % tar xf gradlew.tar
 % ./gradlew
 % ./gradlew build 
```

You can add *ANISOtime* in CLASSPATH as follows:
```bash
 % mv build/libs/kibrary-*.jar /path/to/kibrary.jar
 % export CLASSPATH=$CLASSPATH:/path/to/kibrary.jar
 (/path/to/ should be the absolute path.)
```
If you use csh, tcsh or so,
```csh
% setenv CLASSPATH ${CLASSPATH}:/path/to/kibrary.jar
```

If you want to have Kibrary-goblin, you try the above flow with *goblin.tar* instead of *gradlew.tar*


##Build by yourself
If you just want to use Kibrary, just install as [above](#installation).
However if you want to modify source codes and build by yourself,
you have to care about dependencies by yourself.  
The necessary libraries are  
[*Apache Commons CLI*][cli], [*Apache Commons Email*][email], [*Apache Commons IO*][io],
[*Apache Commons LANG*][lang], [*Apache Commons MATH*][math], [*Apache Commons NET*][net],
[*Epsgraphics*][eps], [*javax.mail*][mail].  
The latest versions are strongly recommended.



##Usage
Please see [Javadoc][javadoc]. [Wiki][wiki] is still under construction.

##Contact me
If you have any comments or questions, please feel free to contact me by [E-mail][mailto].

##Copyright and Licence
Copyright © 2015 Kensuke Konishi  
Licensed under [Apache-2][alicense] and [Oracle BSD License][olicense]  
Last updated Jul 6, 2016


[release-image]:https://img.shields.io/badge/release-Sahagin-pink.svg
[release]:https://en.wikipedia.org/wiki/Sahuagin
[version-image]:https://img.shields.io/badge/version-0.4-yellow.svg

[alicense-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[alicense]: http://www.apache.org/licenses/LICENSE-2.0

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


