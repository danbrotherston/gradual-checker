/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell MethodRenameTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class MethodRenameTest {
  public static void main(String [] args) {
    MethodRenameTest foo = new MethodRenameTest();
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
