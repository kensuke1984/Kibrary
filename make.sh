#!/bin/sh

jarname=kibrary-yuki-0.1.jar
sendto=wave

rm -rf $jarname
jar cvf $jarname .
scp $jarname suzuki@$sendto.eps.s.u-tokyo.ac.jp:/home/suzuki/bin
