/*
 * @test
 * @summary Testing basic runtime tests to validate that invalid values are
 * caught and valid values are allowed.
 *
 * @run shell DynamicInheritanceRuntimeTest.sh
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Dynamic;

public class DynamicInheritanceRuntimeTest extends DynamicInheritanceRuntimeTestParent {
  public static void main(String [] args) {
    DynamicInheritanceRuntimeTestParent bar = new DynamicInheritanceRuntimeTestParent();
    DynamicInheritanceRuntimeTestParent foo = new DynamicInheritanceRuntimeTest();
    DynamicInheritanceRuntimeTestParent baz = new DynamicInheritanceRuntimeTestSubclass();

    bar.fail(null);

    try {
      foo.fail(null);
    } catch(RuntimeException e) {
      System.out.println("Success1");
    }
    
    baz.fail(null);
  }

  void fail(@NonNull Integer i) {
    System.out.println(i);
  }
}
