#!/bin/sh

if [ -z $TESTSRC ]
    then
      TESTSRC=.
fi

if [ -z $TESTCLASSES ]
    then
      TESTCLASSES=.
fi

$TESTSRC/../../bin/javac -d $TESTCLASSES -processor org.checkerframework.checker.gradualnullness.GradualNullnessChecker $TESTSRC/DynamicRuntimeVarDeclTest.java
java -classpath $TESTSRC/../../dist/checker.jar:.:$TESTCLASSES DynamicRuntimeVarDeclTest > $TESTCLASSES/DynamicRuntimeVarDeclTest.testout
diff $TESTCLASSES/DynamicRuntimeVarDeclTest.testout $TESTSRC/DynamicRuntimeVarDeclTest.out
