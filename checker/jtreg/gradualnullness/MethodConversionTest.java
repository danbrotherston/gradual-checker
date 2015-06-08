/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell MethodConversionTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class MethodConversionTest {
  public static void main(String [] args) {
    MethodConversionTest foo = new MethodConversionTest();
    foo.a();
  }

  void a() {
    @Nullable Integer h = f(3);
    System.out.println(h);
  }

  @Dynamic Integer f(Integer bar) {
    return 0 + bar;
  }
}
