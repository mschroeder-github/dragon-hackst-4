
package net.markus.projects.dh4.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import net.markus.projects.dh4.util.Utils;

/**
 * 
 */
public class TextBlock {

    //here data is only used
    public StarZerosSubBlock subBlock;
    
    //just to see the header only
    @Deprecated
    public byte[] header;
    
    public int endOffset;
    public int id;//unknown
    public int huffmanCodeStart; //c
    public int huffmanTreeBytesEnd; //d
    public int huffmanCodeEnd; //e
    public int dataHeaderToHuffmanCodeStart; //f
    
    public byte[] dataHeaderToHuffmanCode; //unkown
    public byte[] huffmanCode; //c - e
    public byte[] huffmanTreeBytes; //e(+10) - d
    public byte[] dataDA; //unknown
    
    public ParseNode root;
    
    public int huffmanTreeBytesStart; //+0 (4)
    public int huffmanTreeBytesMiddle; //+4 (4)
    public int numberOfNodes; //+8 (2)
    
    public int atEndOffset;
    public int numberOfDataEndBlocks;
    public byte[] dataEnd;
    
    
    public byte[] extraDataEnd = new byte[0];
    
    public String getHexID() {
        return Utils.bytesToHex(Utils.intToByteArray(id)).toUpperCase().substring(4, 8);
    }
    
    @Override
    public String toString() {
        return "TextBlock{" + "subBlock=" + subBlock + ", a=" + endOffset + ", b=" + id + ", c=" + huffmanCodeStart + ", d=" + huffmanTreeBytesEnd + ", e=" + huffmanCodeEnd + ", f=" + dataHeaderToHuffmanCodeStart + ", dataHeaderToCE=" + dataHeaderToHuffmanCode.length + ", dataCE=" + huffmanCode.length + ", dataED=" + huffmanTreeBytes.length + ", dataDA=" + dataDA.length + ", e1=" + huffmanTreeBytesStart + ", e2=" + huffmanTreeBytesMiddle + ", e3=" + numberOfNodes + ", atA=" + atEndOffset + ", atAnext=" + numberOfDataEndBlocks + ", dataAtA=" + dataEnd.length + '}';
    }
    
    public void write() throws IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int endOffsetInternal = 
                4 * 6 + //header
                dataHeaderToHuffmanCode.length + 
                huffmanCode.length + 
                10 + //header for tree
                huffmanTreeBytes.length + 
                dataDA.length;
        
        int huffmanCodeStartInternal = 
                4 * 6 + 
                dataHeaderToHuffmanCode.length;
        
        int huffmanCodeEndInternal = 
                huffmanCodeStartInternal + 
                huffmanCode.length;
        
        int huffmanTreeBytesEndInternal =
                huffmanCodeEndInternal +
                10 +
                huffmanTreeBytes.length;
        
        if(huffmanTreeBytesEndInternal == endOffsetInternal) {
            huffmanTreeBytesEndInternal = 0;
        }
                
        //if((dataEnd.length /*+ extraDataEnd.length*/) % 8 != 0) {
        //    throw new RuntimeException("(dataEnd.length + extraDataEnd.length) % 8 != 0: " + (dataEnd.length /*+ extraDataEnd.length*/) % 8);
        //}
        
        int numberOfDataEndBlocksInternal = 
                (dataEnd.length /*+ extraDataEnd.length*/) / 8;
        
        int dataHeaderToHuffmanCodeStartInternal = 0;
        if(dataHeaderToHuffmanCode.length > 0) {
            dataHeaderToHuffmanCodeStartInternal = 6 * 4;
        }
        
        baos.write(Utils.intToByteArrayLE(endOffsetInternal));
        baos.write(Utils.intToByteArrayLE(id));
        baos.write(Utils.intToByteArrayLE(huffmanCodeStartInternal));
        baos.write(Utils.intToByteArrayLE(huffmanTreeBytesEndInternal));
        baos.write(Utils.intToByteArrayLE(huffmanCodeEndInternal));
        baos.write(Utils.intToByteArrayLE(dataHeaderToHuffmanCodeStartInternal));
        
        baos.write(dataHeaderToHuffmanCode);
        baos.write(huffmanCode);
        
        int numberOfNodesInternal = (int) root.descendants().stream().filter(pn -> pn.getLabel().equals("node")).count();
        
        int huffmanTreeBytesStartInternal = huffmanCodeEndInternal + 10;
        int huffmanTreeBytesMiddleInternal = huffmanTreeBytesStartInternal + (huffmanTreeBytes.length / 2) - 1;
        
        //two ints here
        baos.write(Utils.intToByteArrayLE(huffmanTreeBytesStartInternal));
        baos.write(Utils.intToByteArrayLE(huffmanTreeBytesMiddleInternal));
        baos.write(Utils.shortToByteArrayLE(numberOfNodesInternal));
        
        baos.write(huffmanTreeBytes);
        baos.write(dataDA);
        
        baos.write(Utils.intToByteArrayLE(endOffsetInternal));
        baos.write(Utils.intToByteArrayLE(numberOfDataEndBlocksInternal));
        baos.write(dataEnd);
        
        baos.write(extraDataEnd);
        
        byte[] tmp = baos.toByteArray();
        
        int cmp = Utils.compare(tmp, subBlock.data);
        
        if(cmp != -1) {
            System.out.println("TextBlock: " + subBlock.getPath() + " compare: " + cmp);
            
            //System.out.println(Utils.toHexDump(tmp, 16, true, false, null));
            
            //System.out.println(Utils.toHexDump(subBlock.data, 16, true, false, null));
            
            //in 298/0 we parse the tree not correct, we get 12 nodes but there are 26
            
            List<String> l = Utils.compare(new Object[] {
                "endOffset", endOffset, endOffsetInternal,
                "huffmanCodeStart", huffmanCodeStart, huffmanCodeStartInternal,
                "huffmanTreeBytesEnd", huffmanTreeBytesEnd, huffmanTreeBytesEndInternal,
                "huffmanCodeEnd", huffmanCodeEnd, huffmanCodeEndInternal,
                "dataHeaderToHuffmanCodeStart", dataHeaderToHuffmanCodeStart, dataHeaderToHuffmanCodeStartInternal,
                "huffmanTreeBytesStart", huffmanTreeBytesStart, huffmanTreeBytesStartInternal,
                "huffmanTreeBytesMiddle", huffmanTreeBytesMiddle, huffmanTreeBytesMiddleInternal,
                "numberOfNodes", numberOfNodes, numberOfNodesInternal,
                "numberOfDataEndBlocks", numberOfDataEndBlocks, numberOfDataEndBlocksInternal
            });
            
            l.forEach(str -> System.out.println("\t" + str));
            
            /*
            if(numberOfNodes != numberOfNodesInternal) {
                System.out.println(Utils.toHexDump(huffmanTreeBytes, 16, true, false, null));
                
                System.out.println(root.toStringTree());
            }*/
        }
        
        subBlock.data = tmp;
    }
    
}
