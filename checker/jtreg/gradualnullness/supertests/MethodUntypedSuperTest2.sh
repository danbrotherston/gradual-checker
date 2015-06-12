#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../../bin/javac -d $TESTCLASSES $TESTSRC/MethodUntypedSuperTest2Parent.java

$TESTSRC/../../../bin/javac -cp $TESTCLASSES -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodUntypedSuperTest2.java $TESTSRC/MethodUntypedSuperTest2TypedParent.java

set -e

javap -c $TESTCLASSES/MethodUntypedSuperTest2.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodUntypedSuperTest2TypedParent.f:(Ljava/lang/Integer;)Ljava/lang/Integer;"

javap -c $TESTCLASSES/MethodUntypedSuperTest2.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodUntypedSuperTest2TypedParent.a:()V"

