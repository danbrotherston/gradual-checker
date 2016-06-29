#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../../bin/javac -d $TESTCLASSES $TESTSRC/MethodUntypedSuperTest3Parent.java

$TESTSRC/../../../bin/javac -cp $TESTCLASSES -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodUntypedSuperTest3.java $TESTSRC/MethodUntypedSuperTest3TypedParent.java

set -e

javap -c $TESTCLASSES/MethodUntypedSuperTest3.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodUntypedSuperTest3TypedParent.f_\$safe:(Ljava/lang/Integer;)Ljava/lang/Integer;"

javap -c $TESTCLASSES/MethodUntypedSuperTest3.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodUntypedSuperTest3TypedParent.a:()V"

