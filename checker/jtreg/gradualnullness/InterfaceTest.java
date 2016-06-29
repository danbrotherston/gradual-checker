/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell InterfaceTest.sh
 */

public class InterfaceTest { //implements IInterface {
  public static void main(String [] args) {
    InterfaceTest baz = new InterfaceTest();
    baz.foo();
    baz.bar(10);
  }

  void foo() {
    System.out.println(9);
  }

  void bar(Integer i) {
    System.out.println(i);
  }
}
