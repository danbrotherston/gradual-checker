#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../../bin/javac -doe -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodSuperTest2.java $TESTSRC/MethodSuperTest2Parent.java $TESTSRC/MethodSuperTest2ParentParent.java

set -e

javap -c $TESTCLASSES/MethodSuperTest2.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodSuperTest2Parent.f_\$safe:(Ljava/lang/Integer;)Ljava/lang/Integer;"

javap -c $TESTCLASSES/MethodSuperTest2.class | grep -q "[0-9]*: invokespecial #[0-9]*[[:space:]]*// Method MethodSuperTest2Parent.a:()V"

