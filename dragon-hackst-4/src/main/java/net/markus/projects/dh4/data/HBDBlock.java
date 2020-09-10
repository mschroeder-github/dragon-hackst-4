
package net.markus.projects.dh4.data;

/**
 * 
 */
public class HBDBlock {

    public int pos;
    public int blockIndex;
    
    public byte[] header;
    public byte[] data;
    public byte[] full2048;
    
    public String headerHexString;
    
    public String hexPos() {
        return "0x" + Integer.toHexString(pos);
    }
    
}
