/*
 * @test
 * @summary Testing basic runtime tests to validate that invalid values are
 * caught and valid values are allowed.
 *
 * @run shell PublicFieldRuntimeTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class PublicFieldRuntimeTest {
  public Integer publicField = 2;

  public static void main(String [] args) {
    PublicFieldRuntimeTest foo = new PublicFieldRuntimeTest();
    foo.a();

    try {
      PublicFieldRuntimeTestUncheckedPart.setField(foo);
      foo.a();
    } catch(RuntimeException e) {
      System.out.println("Success");
    }
  }

  void a() {
    @NonNull Integer bar = this.publicField;
    System.out.println(bar);
  }
}
