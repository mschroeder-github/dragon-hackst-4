
package net.markus.projects.dh4.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.Stack;
import net.markus.projects.dh4.util.Utils;

/**
 * 
 */
public class TextBlock {

    //here data is only used
    public StarZerosSubBlock subBlock;
    
    public byte[] header;
    
    public int a;
    public int b;
    public int c;
    public int d;
    public int e;
    public int f;
    
    public byte[] dataHeaderToCE;
    public byte[] huffmanCode;
    public byte[] huffmanTreeBytes; //tree data
    public byte[] dataDA;
    
    public ParseNode root;
    public Stack<ParseNode> stack;
    public Queue<ParseNode> queue;
    
    public int e1; //+0 (4)
    public int e2; //+4 (4)
    public int e3; //+8 (2)
    
    public int atA;
    public int atAnext;
    public byte[] dataAtA;
    
    /*
    @Deprecated
    public byte[] dataE1;
    @Deprecated
    public byte[] dataE2;
    
    @Deprecated
    public int atE;
    @Deprecated
    public int atEnext;
    @Deprecated
    public byte[] dataAtE;
    */
    
    /*
    public byte[] data1;
    public byte[] data2;
    public byte[] dataSize1;
    
    public int data2a;
    public int data2b;
    */

    @Override
    public String toString() {
        return "TextBlock{" + "subBlock=" + subBlock + ", a=" + a + ", b=" + b + ", c=" + c + ", d=" + d + ", e=" + e + ", f=" + f + ", dataHeaderToCE=" + dataHeaderToCE.length + ", dataCE=" + huffmanCode.length + ", dataED=" + huffmanTreeBytes.length + ", dataDA=" + dataDA.length + ", e1=" + e1 + ", e2=" + e2 + ", e3=" + e3 + ", atA=" + atA + ", atAnext=" + atAnext + ", dataAtA=" + dataAtA.length + '}';
    }
    
    public void write() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        baos.write(Utils.intToByteArrayLE(a));
        baos.write(Utils.intToByteArrayLE(b));
        baos.write(Utils.intToByteArrayLE(c));
        baos.write(Utils.intToByteArrayLE(d));
        baos.write(Utils.intToByteArrayLE(e));
        baos.write(Utils.intToByteArrayLE(f));
        
        baos.write(dataHeaderToCE);
        baos.write(huffmanCode);
        
        //two ints here
        baos.write(Utils.intToByteArrayLE(e1));
        baos.write(Utils.intToByteArrayLE(e2));
        baos.write(Utils.shortToByteArrayLE(e3));
        
        baos.write(huffmanTreeBytes);
        baos.write(dataDA);
        
        baos.write(Utils.intToByteArrayLE(atA));
        baos.write(Utils.intToByteArrayLE(atAnext));
        baos.write(dataAtA);
        
        byte[] tmp = baos.toByteArray();
        
        if(tmp.length != subBlock.data.length) {
            new RuntimeException("same length problem");
        }
        
        for(int i = 0; i < subBlock.data.length; i++) {
            if(tmp[i] != subBlock.data[i]) {
                new RuntimeException("byte equality problem");
            }
        }
        
        subBlock.data = tmp;
    }
    
}
