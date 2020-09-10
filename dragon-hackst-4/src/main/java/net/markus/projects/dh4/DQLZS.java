
package net.markus.projects.dh4;

import net.markus.projects.dh4.util.Utils;

/**
 * Dragon Quest LZS algorithm.
 */
public class DQLZS {

    //still TODO
    public static byte[] decompress(byte[] compressed, int decompressSize) {
        
        //System.out.println(Utils.toHexDump(compressed, 16, true, false, null));
        
        byte[] decompressed = new byte[decompressSize];
        
        int offset = 0;
        int offsetDecompressed = 0;
        
        while(offset < compressed.length) {
            
            byte controlByte = compressed[offset];
            String controlByteBits = Utils.toBits(controlByte);
            
            offset++;
            
            System.out.println();
            System.out.println("control byte at offset=" + offset + " " + controlByteBits + " | 0x" + Utils.bytesToHex(new byte[] { controlByte }));
            
            //for(int i = 0; i < controlByteBits.length(); i++) {
            for(int i = controlByteBits.length()-1; i >= 0; i--) {
                char bit = controlByteBits.charAt(i);
                
                if(bit == '1') {
                    //literal
                    
                    byte literal = compressed[offset];
                    
                    decompressed[offsetDecompressed] = compressed[offset];
                    
                    System.out.println("write literal at offset=" + offset + " | " + (literal & 0xff) + " | " + Utils.toHexStringASCII(new byte[] { literal }) + " | 0x" + Utils.bytesToHex(new byte[] { literal }));
                    offset++;
                    offsetDecompressed++;
                    
                } else if(bit == '0') {
                    //reference
                    
                    byte refByte = compressed[offset];
                    byte[] refBytes = new byte[] { compressed[offset],  compressed[offset+1] };
                    byte[] refBytes3 = new byte[] { compressed[offset],  compressed[offset+1], compressed[offset+2] };
                    
                    String refByteBits = Utils.toBits(refByte);
                    String refBytesBits = Utils.toBits(refBytes);
                    String refBytes3Bits = Utils.toBits(refBytes3);
                    
                    //System.out.println("reference at offset=" + offset + " " + refByteBits + " | 0x" + Utils.bytesToHex(new byte[] { refByte }));
                    //System.out.println("or");
                    System.out.println("reference at offset=" + offset + " " + refBytesBits.substring(0, 8) + " " + refBytesBits.substring(8, 16) + " | 0x" + Utils.bytesToHex(refBytes));
                    
                    //System.out.println("reference at offset=" + offset + " " + refBytes3Bits + " | 0x" + Utils.bytesToHex(refBytes3));
                 
                    offset += 2;
                    
                    int a = 0;
                }
                
                
                int a = 0;
            }
            
            
            
            
        }
        
        
        return decompressed;
    }
    
}
