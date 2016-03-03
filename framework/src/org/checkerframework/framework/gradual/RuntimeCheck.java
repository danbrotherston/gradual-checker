package org.checkerframework.framework.gradual;

/**
 * @author danbrotherston
 *
 * This class contains actual runtime tests which are invoked at compile time
 * by code built in the compiler using AST trees.  Providing the code here makes
 * it easier to understand and modify.
 */
public class RuntimeCheck {

    private final static String MARKER_FIELD_NAME = "$isTypeCheckedMarker";

    /**
     * This method checks to see if a provided object instance is from a class
     * which has actually been checked by the checker framework or not.  This is
     * used when deciding whether to call a version of a method with runtime checks
     * or not.
     *
     * @param value The object to determine if it has been checked or not.
     * @return True iff the actual runtime class of the object passed in has been
     *         checked by the checker framework.
     */
    public static boolean isChecked(Object value) {
        //System.err.println("x");
	Class<?> clazz = value.getClass();
	try {
	    clazz.getDeclaredField(RuntimeCheck.MARKER_FIELD_NAME);
	} catch (NoSuchFieldException e) {
	    //System.err.println("Returning false: " + clazz);
	    //System.err.println("Fields: " + clazz.getDeclaredFields());
	    return false;
	}

	//System.err.println("Returning true");
	return true;
    }
}
