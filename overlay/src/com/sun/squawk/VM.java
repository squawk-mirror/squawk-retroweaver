package com.sun.squawk;

/**
 * This class only exists in order to help the tests pass for the Enum.values() transformation that occurs.
 * We translate all array.clone() calls into VM.shallowCopy() calls.
 * @author ea149956
 *
 */
public class VM {
    /** 
     * Perform a shallow copy of the original object, without calling a constructor
     * 
     * @param original the iobject to copy
     * @return a copy of the original object.
     */
    public static Object shallowCopy(Object original) {
        return original;
    }

}
