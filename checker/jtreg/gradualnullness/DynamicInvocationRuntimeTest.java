/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell DynamicInvocationRuntimeTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.Dynamic;

public class DynamicInvocationRuntimeTest {
  public static void main(String [] args) {
    System.out.println("Hello World");
    DynamicInvocationRuntimeTest foo = new DynamicInvocationRuntimeTest();

    foo.a();
    foo.value = null;
    try {
      foo.a();
    } catch (RuntimeException e) {
      System.out.println("Success");
    }
  }

  void a() {
    @Dynamic Integer dynamicVal = this.value;
    System.out.println(dynamicVal.toString());
  }

  @Nullable Integer value = 2;
}
