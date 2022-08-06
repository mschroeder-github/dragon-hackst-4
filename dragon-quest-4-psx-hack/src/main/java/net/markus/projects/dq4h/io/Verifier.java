
package net.markus.projects.dq4h.io;

/**
 * Verifies data.
 */
public class Verifier {

    /**
     * Checks if all bytes are zero.
     * @param array
     * @return 
     */
    public static boolean allZero(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * -1 means it is equal, an index >= 0 means the bytes are different there, -2
     * means length is different.
     * @param a
     * @param b
     * @return 
     */
    public static int compare(byte[] a, byte[] b) {
        if(a.length != b.length) {
            return -2;
        }
        
        for(int i = 0; i < a.length; i++) {
            if(a[i] != b[i]) {
                return i;
            }
        }
        return -1;
    }

}
