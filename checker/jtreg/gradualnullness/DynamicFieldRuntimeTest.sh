#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

set -e

$TESTSRC/../../bin/javac -d $TESTCLASSES $TESTSRC/DynamicFieldRuntimeTestUnchecked.java

$TESTSRC/../../bin/javac -classpath $TESTCLASSES -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/DynamicFieldRuntimeTest.java

java -classpath $TESTSRC/../../dist/checker.jar:.:$TESTCLASSES DynamicFieldRuntimeTest > $TESTCLASSES/DynamicFieldRuntimeTest.testout

diff $TESTCLASSES/DynamicFieldRuntimeTest.testout $TESTSRC/DynamicFieldRuntimeTest.out
