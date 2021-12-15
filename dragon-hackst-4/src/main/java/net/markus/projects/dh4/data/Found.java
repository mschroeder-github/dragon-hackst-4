
package net.markus.projects.dh4.data;

import net.markus.projects.dh4.util.Utils;

/**
 * 
 */
public class Found {
    
    public StarZerosSubBlock block;
    
    public int begin;
    public int end;
    
    public byte[] data;

    @Override
    public String toString() {
        return block.getPath() + 
                " from 0x" + Utils.toHexString(Utils.intToByteArray(begin)).replace(" ", "") + 
                " to 0x" + Utils.toHexString(Utils.intToByteArray(end)).replace(" ", "") + 
                " | length: " + data.length;
    }
    
    
    
}
