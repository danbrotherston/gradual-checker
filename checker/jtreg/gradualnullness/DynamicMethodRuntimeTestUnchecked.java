public class DynamicMethodRuntimeTestUnchecked {
    DynamicMethodRuntimeTest classUnderTest;
    
    public DynamicMethodRuntimeTestUnchecked(DynamicMethodRuntimeTest classUnderTest) {
	this.classUnderTest = classUnderTest;
    }

    public void testPass() {
	this.classUnderTest.fail(32);
    }

    public void testFail() {
	this.classUnderTest.fail(null);
    }
}
