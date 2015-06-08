#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/MethodConversionTest.java
javap $TESTCLASSES/MethodConversionTest.class > $TESTCLASSES/MethodConversionTest.testout
diff $TESTCLASSES/MethodConversionTest.testout $TESTSRC/MethodConversionTest.out
