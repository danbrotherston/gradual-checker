#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

rm -rf $TESTCLASSES/*.class
rm -rf $TESTCLASSES/*.testout

set -e

$TESTSRC/../../bin/javac -d $TESTCLASSES $TESTSRC/PublicFieldRuntimeTestUncheckedPart.java $TESTSRC/PublicFieldRuntimeTest.java

rm $TESTCLASSES/PublicFieldRuntimeTest.class

$TESTSRC/../../bin/javac -classpath $TESTCLASSES -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/PublicFieldRuntimeTest.java

java -classpath $TESTSRC/../../dist/checker.jar:.:$TESTCLASSES PublicFieldRuntimeTest > $TESTCLASSES/PublicFieldRuntimeTest.testout

diff $TESTCLASSES/PublicFieldRuntimeTest.testout $TESTSRC/PublicFieldRuntimeTest.out
