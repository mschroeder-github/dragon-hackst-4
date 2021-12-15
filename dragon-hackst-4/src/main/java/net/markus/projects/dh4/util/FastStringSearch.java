
package net.markus.projects.dh4.util;

/**
 * 
 */
public final class FastStringSearch {

    public static int quickSearch(final byte[] text, final byte[] pattern) {
        return quickSearch(text, 0, text.length, pattern);
    }

    /**
     * An implementation of Sunday's simplified "Quick Finder" version of the
     * Boyer-Moore algorithm. See "A very fast substring search algorithm" (appeared
     * in <em>Communications of the ACM. 33 (8):132-142</em>).
     * <pre>
     * Preprocessing: O(m + &sum;) time
     * Processing   : O(mn) worst case
     * </pre>
     * 
     * @author <a href="mailto:jb@eaio.com">Johann Burkard</a>
     */
    public static int quickSearch(final byte[] text, final int textStart, final int textEnd, final byte[] pattern) {
        final int[] skip = processBytes(pattern);
        final int ptnlen = pattern.length;
        int from = textStart;
        int p;
        while(from + ptnlen <= textEnd) {
            p = 0;
            while(p < ptnlen && pattern[p] == text[from + p]) {
                ++p;
            }
            if(p == ptnlen) {
                return from;
            }
            if(from + ptnlen >= textEnd) {
                return -1;
            }
            from += skip[index(text[from + ptnlen])];
        }
        return -1;
    }

    /**
     * Returns a <code>int</code> array.
     */
    private static int[] processBytes(final byte[] pattern) {
        final int[] skip = new int[256];
        final int ptnlen = pattern.length;
        for(int i = 0; i < 256; ++i) {
            skip[i] = ptnlen + 1;
        }
        for(int i = 0; i < ptnlen; ++i) {
            skip[index(pattern[i])] = ptnlen - i;
        }
        return skip;
    }

    /**
     * Converts the given <code>byte</code> to an <code>int</code>.
     */
    private static int index(final byte idx) {
        return (idx < 0) ? 256 + idx : idx;
    }
    
}
