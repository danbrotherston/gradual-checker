/*
 * @test
 * @summary Testing basic runtime tests to validate that invalid values are
 * caught and valid values are allowed.
 *
 * @run shell DynamicRuntimeTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class DynamicRuntimeTest {
  public static void main(String [] args) {
    DynamicRuntimeTest foo = new DynamicRuntimeTest();
    foo.a();
    try {
      foo.fail();
    } catch(RuntimeException e) {
      System.out.println("Success");
    }

    try {
      foo.f().toString();
    } catch (NullPointerException e) {
      System.out.println("Fail");
    } catch(RuntimeException e) {
      System.out.println("Success");
    }
  }

  void a() {
    @Nullable Integer h = f();
    @NonNull Integer i = g();
  }

  void fail() {
    @NonNull Integer i = f();
  }

  @Dynamic Integer g() {
    return 12;
  }

  @Dynamic Integer f() {
    return null;
  }
}
