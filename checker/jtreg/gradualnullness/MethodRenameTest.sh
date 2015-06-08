#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodRenameTest.java

set -e

javap -c $TESTCLASSES/MethodRenameTest.class | grep -q "[0-9]*: invokevirtual #[0-9]*[[:space:]]*// Method f_\$safe:(Ljava/lang/Integer;)Ljava/lang/Integer;"

javap -c $TESTCLASSES/MethodRenameTest.class | grep -q "[0-9]*: invokevirtual #[0-9]*[[:space:]]*// Method a:()V"
