/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell MethodSuperTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class MethodSuperTest extends MethodSuperTestParent {
  public static void main(String [] args) {
    MethodSuperTest foo = new MethodSuperTest();
    foo.a();
  }

  void a() {
    super.a();
  }

  Integer f(Integer bar) {
    return super.f(bar);
  }
}
