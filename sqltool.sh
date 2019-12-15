#!/bin/bash

here=`dirname $0`
java -cp $here/data/hsqldb/hsqldb.jar org.hsqldb.util.DatabaseManagerSwing
