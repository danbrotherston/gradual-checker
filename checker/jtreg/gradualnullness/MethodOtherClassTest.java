/*
 * @test
 * @summary Verifying that value expression side effects are not duplicated when building a
 * runtime test.
 *
 * @run shell MethodOtherClassTest.sh
 */

public class MethodOtherClassTest {
  public static void main(String [] args) {}

  MethodOtherClassTestOtherClass foo = new MethodOtherClassTestOtherClass();

  void a() {
    foo.aother();
  }

  Integer f(Integer bar) {
    return foo.fother(bar);
  }
}
