public class ConstructorSuperTestParent {
  Integer fieldParent;

  ConstructorSuperTestParent() {
    this(2);
  }

  ConstructorSuperTestParent(Integer i) {
    fieldParent = i;
  }

  void a() {
    ConstructorSuperTestParent foo = new ConstructorSuperTestParent(12);
    foo.a();
  }
}
