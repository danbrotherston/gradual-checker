#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../../bin/javac -d $TESTCLASSES $TESTSRC/MethodUntypedSuperTestParent.java
$TESTSRC/../../../bin/javac -cp $TESTCLASSES -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodUntypedSuperTest.java

set -e

javap -c $TESTCLASSES/MethodUntypedSuperTest.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodUntypedSuperTestParent.f:(Ljava/lang/Integer;)Ljava/lang/Integer;"

javap -c $TESTCLASSES/MethodUntypedSuperTest.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodUntypedSuperTestParent.a:()V"

