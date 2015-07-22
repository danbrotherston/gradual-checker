#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/DynamicRuntimeValueTest.java
java -classpath $TESTSRC/../../dist/checker.jar:.:$TESTCLASSES DynamicRuntimeValueTest > $TESTCLASSES/DynamicRuntimeValueTest.testout
diff $TESTCLASSES/DynamicRuntimeValueTest.testout $TESTSRC/DynamicRuntimeValueTest.out
