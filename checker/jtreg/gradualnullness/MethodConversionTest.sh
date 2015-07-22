#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodConversionTest.java
javap $TESTCLASSES/MethodConversionTest.class > $TESTCLASSES/MethodConversionTest.testout

set -e

diff $TESTCLASSES/MethodConversionTest.testout $TESTSRC/MethodConversionTest.out

javap -c $TESTCLASSES/MethodConversionTest.class | grep -c 'CheckerFrameworkFillInTypeHere' | grep -q '0'
