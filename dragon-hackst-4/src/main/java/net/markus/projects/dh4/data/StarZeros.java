
package net.markus.projects.dh4.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class StarZeros extends HBDBlock {
    
    //4 byte
    public int blocks;
    
    //4 byte
    public int sectors;
    
    //4 byte (maybe 8 byte)
    public int sizeTotal;
    
    //4 byte
    public int zeros;
    
    //16 byte headers
    public List<StarZerosSubBlock> starZerosBlocks = new ArrayList<>();
    
    //the rest data after reading sizeTotal
    public byte[] rest;
    
    public int trailing00startPos;
    
    @Deprecated
    public int size2;
    
    @Deprecated
    public int size3;
    
    @Deprecated
    public int zeros2;
    
    @Deprecated
    public int flag1;
    
    @Deprecated
    public int flag2;
    
    
    
    
    @Deprecated
    public boolean regular;
    
    @Deprecated
    public byte[] afterDataSize1;
    @Deprecated
    public byte[] afterDataSize2;
    @Deprecated
    public byte[] afterDataSize3;
}
