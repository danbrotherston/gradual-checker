/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell DynamicRuntimeVarDeclTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class DynamicRuntimeVarDeclTest {
  public static void main(String [] args) {
    DynamicRuntimeVarDeclTest foo = new DynamicRuntimeVarDeclTest();
    foo.a();
  }

  void a() {
    @Nullable Integer h = f();
    System.out.println(h);
  }

  @Dynamic Integer f() {
    return 0;
  }
}
