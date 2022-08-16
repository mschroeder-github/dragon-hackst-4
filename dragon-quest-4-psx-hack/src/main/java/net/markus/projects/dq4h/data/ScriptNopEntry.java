
package net.markus.projects.dq4h.data;

/**
 * Represents a no operation (nop) entry which is always 0xb0.
 */
public class ScriptNopEntry extends ScriptEntry {

    /**
     * 0xb0.
     */
    public static final byte[] NOP = new byte[] { (byte) 0xb0 };
    
    @Override
    public String toString() {
        return "ScriptNopEntry{" + '}';
    }
    
}
