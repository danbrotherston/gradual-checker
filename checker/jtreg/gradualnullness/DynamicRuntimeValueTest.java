/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell DynamicRuntimeValueTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class DynamicRuntimeValueTest {
  public static void main(String [] args) {
    DynamicRuntimeValueTest foo = new DynamicRuntimeValueTest();
    foo.a();
  }

  void a() {
    int a = 2;
    @Nullable Integer h = f();
    a = 4;
  }

  @Dynamic Integer f() {
    System.out.println("sideeffect value");
    return null;
  }
}
