#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodRenameStaticTest.java

set -e

javap -c $TESTCLASSES/MethodRenameStaticTest.class | grep -c "[0-9]*: invokestatic  #[0-9]*[[:space:]]*// Method b_\$safe:(Ljava/lang/Integer;)V" | grep -q '3'

javap -c $TESTCLASSES/MethodRenameStaticTest.class | grep -c "[0-9]*: invokestatic  #[0-9]*[[:space:]]*// Method c:()V" | grep -q '2'
