#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodOtherClassTest.java $TESTSRC/MethodOtherClassTestOtherClass.java

set -e

javap -c $TESTCLASSES/MethodOtherClassTest.class | grep -q "[0-9]*: invokevirtual #[0-9]*[[:space:]]*// Method MethodOtherClassTestOtherClass.fother_\$maybe:(Ljava/lang/Integer;)Ljava/lang/Integer;"

javap -c $TESTCLASSES/MethodOtherClassTestOtherClass.class | grep -q "[0-9]*: invokevirtual #[0-9]*[[:space:]]*// Method MethodOtherClassTest.f_\$maybe:(Ljava/lang/Integer;)Ljava/lang/Integer;"
