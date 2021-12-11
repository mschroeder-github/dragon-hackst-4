
package net.markus.projects.dh4.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.markus.projects.dh4.util.Utils;

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
    
    public int remaining() {
        int total = sectors * 2048;
        //header, subblock header, raw data size
        return total - (16 + (starZerosBlocks.size() * 16) + sizeTotal);
    }
    
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

    @Override
    public void write() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
     
        int blocks_ = starZerosBlocks.size();
        
        //it is the sum of the subblocks
        int sizeTotal_ = 0;
        for(StarZerosSubBlock subblock : starZerosBlocks) {
            sizeTotal_ += subblock.data.length;
        }
        
        int sizeAll_ = 16; //header
        for(StarZerosSubBlock subblock : starZerosBlocks) {
            sizeAll_ += 16; //header
            sizeAll_ += subblock.data.length;
        }
        
        int sectors_ = (sizeAll_ / 2048) + (sizeAll_ % 2048 != 0 ? 1 : 0);

        //debug tests
        if(blocks != blocks_) {
            System.out.println(blockIndex + " blocks change from " + blocks + " to " + blocks_);
        }
        if(sectors != sectors_) {
            //System.out.println(blockIndex + " sector change from " + sectors + " to " + sectors_);
            //because this causes I/O errors we throw an exception
            throw new RuntimeException(blockIndex + " sector change from " + sectors + " to " + sectors_);
        }
        if(sizeTotal != sizeTotal_) {
            //System.out.println(blockIndex + " sizeTotal change from " + sizeTotal + " to " + sizeTotal_);
        }
        
        baos.write(Utils.intToByteArrayLE(blocks_));
        baos.write(Utils.intToByteArrayLE(sectors_));
        baos.write(Utils.intToByteArrayLE(sizeTotal_));
        baos.write(Utils.intToByteArrayLE(zeros));
        
        
        
        for(StarZerosSubBlock subblock : starZerosBlocks) {
            
            baos.write(Utils.intToByteArrayLE(subblock.data.length));
            baos.write(Utils.intToByteArrayLE(subblock.sizeUncompressed)); //TODO sizeUncompressed
            baos.write(Utils.intToByteArrayLE(subblock.unknown));
            baos.write(Utils.shortToByteArrayLE(subblock.flags1));
            baos.write(Utils.shortToByteArrayLE(subblock.type));
        }
        
        for(StarZerosSubBlock subblock : starZerosBlocks) {
            baos.write(subblock.data);
        }
        
        int l = baos.toByteArray().length;
        
        int i = 2048;
        for(; i < l; i += 2048) {
        }
        
        //trailing
        int rest = i - l;
        baos.write(new byte[rest]);
        
        int cmp = Utils.compare(baos.toByteArray(), this.full2048);
        if(cmp != -1) {
            System.out.println("StarZeros: " + blockIndex + " compare: " + cmp);
        }
        
        //if(full2048.length != baos.toByteArray().length) {
        //    throw new RuntimeException("len has to be same: " + full2048.length + " vs " + baos.toByteArray().length + " in " + blockIndex);
        //}
        
        writeThisBlock = baos.toByteArray();
    }
}
