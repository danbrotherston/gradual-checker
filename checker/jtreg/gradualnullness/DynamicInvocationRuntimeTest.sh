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

$TESTSRC/../../bin/javac -AprintErrorStack -doe -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/DynamicInvocationRuntimeTest.java

java -classpath $TESTSRC/../../dist/checker.jar:.:$TESTCLASSES DynamicInvocationRuntimeTest > $TESTCLASSES/DynamicInvocationRuntimeTest.testout

diff $TESTCLASSES/DynamicInvocationRuntimeTest.testout $TESTSRC/DynamicInvocationRuntimeTest.out
