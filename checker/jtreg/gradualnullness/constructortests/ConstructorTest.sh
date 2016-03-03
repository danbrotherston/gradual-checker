#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/ConstructorTest.java

javap $TESTCLASSES/ConstructorTest.class > $TESTCLASSES/ConstructorTest.testout

set -e

diff $TESTSRC/ConstructorTest.out $TESTCLASSES/ConstructorTest.testout

javap -c $TESTCLASSES/ConstructorTest.class | grep -c '[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method "<init>":(Lorg/checkerframework/framework/gradual/SafeConstructorMarkerDummy;Ljava/lang/Integer;)V' | grep -q '3'
