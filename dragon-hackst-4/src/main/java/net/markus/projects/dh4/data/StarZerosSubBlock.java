
package net.markus.projects.dh4.data;

/**
 * 
 */
public class StarZerosSubBlock extends HBDBlock {

    public StarZeros parent;
    
    public int size;
    
    public int sizeUncompressed;
    
    public int unknown;
    
    public int flags1;
    
    public int flags2;
    
    public boolean compressed;

    @Override
    public String toString() {
        return "StarZerosBlock{" + "size=" + size + ", sizeUncompressed=" + sizeUncompressed + ", unknown=" + unknown + ", flags1=" + flags1 + ", flags2=" + flags2 + ", compressed=" + compressed + '}';
    }
    
}
