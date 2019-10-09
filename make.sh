#!/bin/sh

jarname=kibrary-yuki-0.1.3.jar
sendto=wave

rm -rf $jarname
jar cf $jarname .
scp -rq $jarname suzuki@$sendto.eps.s.u-tokyo.ac.jp:/home/suzuki/bin
