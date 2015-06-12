/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell MethodSuperTest2.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class MethodSuperTest2 extends MethodSuperTest2Parent {
  public static void main(String [] args) {
    MethodSuperTest2 foo = new MethodSuperTest2();
    foo.a();
  }

  void a() {
    super.a();
  }

  Integer f(Integer bar) {
    Integer i = super.f(bar);
    if (i != null) {
	return i;
    } else {
	return 1;
    }
  }
}
