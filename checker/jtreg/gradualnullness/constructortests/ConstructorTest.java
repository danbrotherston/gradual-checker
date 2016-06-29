/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell ConstructorTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class ConstructorTest {
  public static void main(String [] args) {}

  Integer field;

  ConstructorTest() {
    this(2);
  }

  ConstructorTest(Integer i) {
    field = i;
  }

  void a() {
    ConstructorTest foo = new ConstructorTest(12);
    foo.a();
  }
}
