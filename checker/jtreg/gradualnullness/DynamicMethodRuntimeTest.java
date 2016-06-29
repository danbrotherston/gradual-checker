/*
 * @test
 * @summary Testing basic runtime tests to validate that invalid values are
 * caught and valid values are allowed.
 *
 * @run shell DynamicMethodRuntimeTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class DynamicMethodRuntimeTest {
  public static void main(String [] args) {
    DynamicMethodRuntimeTest foo = new DynamicMethodRuntimeTest();
    try {
      foo.a();
    } catch(RuntimeException e) {
      System.out.println("Success");
    }
  }

  void a() {
    DynamicMethodRuntimeTestUnchecked tester =
      new DynamicMethodRuntimeTestUnchecked(this);
    tester.testPass();
    tester.testFail();
  }

  void fail(@NonNull Integer i) {
    System.out.println(i);
  }
}
