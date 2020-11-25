
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
    
    //was flag2
    public int type;
    
    public boolean compressed;

    @Override
    public String toString() {
        return "StarZerosSubBlock{" + "path="+ getPath() + ", size=" + size + ", sizeUncompressed=" + sizeUncompressed + ", unknown=" + unknown + ", flags1=" + flags1 + ", type=" + type + ", compressed=" + compressed + '}';
    }
    
    public String getPath() {
        return parent.blockIndex + "/" + blockIndex;
    }

    @Override
    public void write() {
    
    }
    
    
}
