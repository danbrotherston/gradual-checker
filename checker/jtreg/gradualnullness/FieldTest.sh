#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/FieldTest.java
javap -private $TESTCLASSES/FieldTest.class > $TESTCLASSES/FieldTest.testout
diff $TESTCLASSES/FieldTest.testout $TESTSRC/FieldTest.out
