#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../bin/javac -doe -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/InterfaceTest.java

set -e

javap -private $TESTCLASSES/InterfaceTest.class > $TESTCLASSES/InterfaceTest.testout
diff $TESTCLASSES/InterfaceTest.testout $TESTSRC/InterfaceTest.out

javap -private $TESTCLASSES/IInterface.class > $TESTCLASSES/IInterface.testout
diff $TESTCLASSES/IInterface.testout $TESTSRC/IInterface.out

rm $TESTCLASSES/*.class
rm $TESTCLASSES/*.testout
