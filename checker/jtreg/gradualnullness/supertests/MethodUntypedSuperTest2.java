/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell MethodUntypedSuperTest2.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class MethodUntypedSuperTest2 extends MethodUntypedSuperTest2TypedParent {
  public static void main(String [] args) {
    MethodUntypedSuperTest2 foo = new MethodUntypedSuperTest2();
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
