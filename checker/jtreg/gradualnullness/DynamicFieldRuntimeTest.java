/*
 * @test
 * @summary Testing basic runtime tests to validate that invalid values are
 * caught and valid values are allowed.
 *
 * @run shell DynamicFieldRuntimeTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class DynamicFieldRuntimeTest {
  public static void main(String [] args) {
    DynamicFieldRuntimeTest foo = new DynamicFieldRuntimeTest();
    try {
      foo.a();
    } catch(RuntimeException e) {
      System.out.println("Success");
    }
  }

  void a() {
    DynamicFieldRuntimeTestUnchecked tester =
      new DynamicFieldRuntimeTestUnchecked();
    this.fail(tester.nullValue);
  }

  void fail(@NonNull Integer i) {
    System.out.println(i);
  }
}
