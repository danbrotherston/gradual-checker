#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/ConstructorSuperTest.java $TESTSRC/ConstructorSuperTestParent.java

set -e

javap -c $TESTCLASSES/ConstructorSuperTest.class | grep -c '[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method "<init>":(Lorg/checkerframework/checker/gradualnullness/SafeConstructorMarkerDummy;Ljava/lang/Integer;)V' | grep -q '2'

javap -c $TESTCLASSES/ConstructorSuperTest.class | grep -c '[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method ConstructorSuperTestParent."<init>":(Lorg/checkerframework/checker/gradualnullness/SafeConstructorMarkerDummy;Ljava/lang/Integer;)V' | grep -q '2'
