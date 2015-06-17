#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/ConstructorTest.java

java -classpath $TESTSRC/../../../dist/checker.jar:.:$TESTCLASSES ConstructorTest > $TESTCLASSES/ConstructorTest.testout

diff $TESTSRC/ConstructorTest.out $TESTCLASSES/ConstructorTest.testout
