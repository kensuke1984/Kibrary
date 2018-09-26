#!/bin/sh

jarname=kibrary-yuki-0.1.2.jar
sendto=lettuce

rm -rf $jarname
jar cf $jarname .
scp -rq $jarname suzuki@$sendto.eps.s.u-tokyo.ac.jp:/home/suzuki/bin
