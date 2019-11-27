#!/bin/bash
origdir=$PWD
here=`dirname $0`
cd $here
docker run -v $PWD:/docs -p 3000:3000 -d dannyben/madness
cd $origdir
