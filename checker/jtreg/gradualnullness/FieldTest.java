/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell FieldTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class FieldTest {
  public static void main(String [] args) {
    FieldTest foo = new FieldTest();
    foo.a();
  }

  private boolean foo = true;

  void a() {
    @Nullable Integer h = f();
    System.out.println(h);
  }

  @Dynamic Integer f() {
    return 0;
  }
}
