#!/bin/bash
cwd=`pwd`
here=`dirname $0`

rm $here/data/alpha/*.csv

cd $here/hot-restart
ls -1 | xargs rm -rf

cd $cwd