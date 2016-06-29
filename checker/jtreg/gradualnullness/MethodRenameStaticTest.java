/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell MethodRenameStaticTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class MethodRenameStaticTest {
  public static void main(String [] args) {
    MethodRenameStaticTest foo = new MethodRenameStaticTest();
    foo.a();
    MethodRenameStaticTest.b(2);
    MethodRenameStaticTest.c();
  }

  void a() {
    @Nullable Integer h = f(3);
    System.out.println(h);
  }

  static void b(Integer h) {
    System.out.println(h);
  }

  static void c() {
    System.out.println("c");
  }

  @Dynamic Integer f(Integer bar) {
    MethodRenameStaticTest.b(3);
    MethodRenameStaticTest.c();
    return 0 + bar;
  }
}
