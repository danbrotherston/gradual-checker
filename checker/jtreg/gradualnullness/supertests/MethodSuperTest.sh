#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodSuperTest.java $TESTSRC/MethodSuperTestParent.java

set -e

javap -c $TESTCLASSES/MethodSuperTest.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodSuperTestParent.f_\$maybe:(Ljava/lang/Integer;)Ljava/lang/Integer;"

javap -c $TESTCLASSES/MethodSuperTest.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodSuperTestParent.a:()V"

