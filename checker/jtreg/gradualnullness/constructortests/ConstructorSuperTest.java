/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell ConstructorSuperTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class ConstructorSuperTest extends ConstructorSuperTestParent {
  public static void main(String [] args) {}

  Integer field;

  ConstructorSuperTest() {
    super(3);
    field = 2;
  }

  ConstructorSuperTest(Integer i) {
    field = i;
  }

  void a() {
    ConstructorSuperTest foo = new ConstructorSuperTest(12);
    ConstructorSuperTestParent bar = new ConstructorSuperTestParent(12);
    foo.a();
  }
}
