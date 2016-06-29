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

$TESTSRC/../../bin/javac -d $TESTCLASSES $TESTSRC/DynamicInheritanceRuntimeTestParent.java $TESTSRC/DynamicInheritanceRuntimeTest.java $TESTSRC/DynamicInheritanceRuntimeTestSubclass.java

rm $TESTCLASSES/DynamicInheritanceRuntimeTest.class

$TESTSRC/../../bin/javac -doe -classpath $TESTCLASSES -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/DynamicInheritanceRuntimeTest.java

java -classpath $TESTSRC/../../dist/checker.jar:.:$TESTCLASSES DynamicInheritanceRuntimeTest > $TESTCLASSES/DynamicInheritanceRuntimeTest.testout

diff $TESTCLASSES/DynamicInheritanceRuntimeTest.testout $TESTSRC/DynamicInheritanceRuntimeTest.out
