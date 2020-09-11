
package net.markus.projects.dh4;

import net.markus.projects.dh4.util.Utils;

/**
 * Dragon Quest LZS algorithm.
 */
public class DQLZS {

    //still TODO
    public static byte[] decompress(byte[] compressed, int decompressSize) {
        
        //2^12 = 4096
        byte[] buffer = new byte[4096]; 
        int maxOff = 4096;
        
        //length = 2^4 = 16
        int maxLen = 16;
        
        
        System.out.println("compressSize=" + compressed.length);
        System.out.println("decompressSize=" + decompressSize);
        
        System.out.println(Utils.toHexDump(compressed, 16, true, false, null));
        
        byte[] decompressed = new byte[decompressSize];
        
        int offsetCompressed = 0;
        int offsetDecompressed = 0;
        int offsetBuffer = 0;
        
        while(offsetCompressed < compressed.length) {
            
            byte controlByte = compressed[offsetCompressed];
            String controlByteBits = Utils.toBits(controlByte);
            
            offsetCompressed++;
            
            //maybe never happen
            //seems to be that a control byte does not have to be evaluated fully
            //if(offsetCompressed >= compressed.length) {
            //    System.out.println("break after control byte read since compressed is read completely");
            //    break;
            //}
            
            
            System.out.println();
            System.out.println("control byte at offset=" + offsetCompressed + " " + controlByteBits + " | 0x" + Utils.bytesToHex(new byte[] { controlByte }));
            
            //for(int i = 0; i < controlByteBits.length(); i++) {
            for(int i = controlByteBits.length()-1; i >= 0; i--) {
                char bit = controlByteBits.charAt(i);
                
                if(bit == '1') {
                    //literal
                    
                    byte literal = compressed[offsetCompressed];
                    
                    decompressed[offsetDecompressed] = compressed[offsetCompressed];
                    buffer[offsetBuffer] = compressed[offsetCompressed];
                    
                    System.out.println("write literal at offset=" + offsetCompressed + " | " + (literal & 0xff) + " | " + Utils.toHexStringASCII(new byte[] { literal }) + " | 0x" + Utils.bytesToHex(new byte[] { literal }));
                    offsetCompressed++;
                    offsetDecompressed++;
                    offsetBuffer++;
                    
                    offsetBuffer %= maxOff;
                    
                } else if(bit == '0') {
                    //reference
                    
                    //byte refByte = compressed[offset];
                    byte[] refBytes = new byte[] { compressed[offsetCompressed],  compressed[offsetCompressed+1] };
                    //byte[] refBytes3 = new byte[] { compressed[offset],  compressed[offset+1], compressed[offset+2] };
                    
                    //String refByteBits = Utils.toBits(refByte);
                    //String refBytes3Bits = Utils.toBits(refBytes3);
                    //System.out.println("reference at offset=" + offset + " " + refByteBits + " | 0x" + Utils.bytesToHex(new byte[] { refByte }));
                    
                    String refBytesBits = Utils.toBits(refBytes);
                    
                    int len = Utils.bitsToIntLE(refBytesBits.substring(8, 12));
                    len = (len + 4) % maxLen; //three zeros would make sense to form the int, offsetDecomp=768
                    //len = (len + 5) % maxLen; 
                    //len = (len + 6) % maxLen; //offsetDecomp=1048
                    //len = (len + 7) % maxLen; //overflow
                    //len = (len + 8) % maxLen; //overflow
                    
                    //1. possibility
                    //String offStr = refBytesBits.substring(0, 8) + refBytesBits.substring(12, 16);
                    
                    //2. possibility
                    //String offStr = refBytesBits.substring(12, 16) + refBytesBits.substring(0, 8);
                    
                    //3. possibility
                    //String offStr = refBytesBits.substring(12, 16) + refBytesBits.substring(4, 8) + refBytesBits.substring(0, 4);
                    
                    //4. possibility
                    //String offStr =  refBytesBits.substring(4, 8) + refBytesBits.substring(0, 4) + refBytesBits.substring(12, 16);
                    
                    //5. possibility
                    String offStr =  refBytesBits.substring(0, 4) + refBytesBits.substring(12, 16) + refBytesBits.substring(4, 8);
                    
                    int off = Utils.bitsToIntLE(offStr);
                    
                    //+397
                    off = (off) % maxOff;
                    
                    //underflow
                    if(off < 0) {
                        off = maxOff - off;
                    }
                    
                    
                    
                    String line = String.format("reference at offsetComp=%d offsetDecomp=%d offsetBuff=%d len=%d off=%d offDiff=%d full=%s offStr=%s | %s", 
                            offsetCompressed,
                            offsetDecompressed,
                            offsetBuffer,
                            len,
                            off,
                            maxOff - off,
                            refBytesBits, //refBytesBits.substring(0, 8) + " " + refBytesBits.substring(8, 16),
                            offStr,
                            Utils.bytesToHex(refBytes)
                    );
                    
                    System.out.println(line);
                    //System.out.println("buffer: " + Utils.toHexString(buffer));
                    
                    //System.out.println("reference at offset=" + offset + " " + refBytes3Bits + " | 0x" + Utils.bytesToHex(refBytes3));
                 
                    
                    //copy buffer to decompressed
                    for(int j = 0; j < len; j++) {
                        byte literal = buffer[(off+j) % maxOff];
                        
                        decompressed[offsetDecompressed] = literal;
                        buffer[offsetBuffer] = literal;

                        System.out.println("\twrite referred " + (literal & 0xff) + " | " + Utils.toHexStringASCII(new byte[] { literal }) + " | 0x" + Utils.bytesToHex(new byte[] { literal }));
                        
                        offsetDecompressed++;
                        offsetBuffer++;

                        offsetBuffer %= maxOff;
                    }
                    
                    offsetCompressed += 2;
                }
                
                //seems to be that a control byte does not have to be evaluated fully
                if(offsetCompressed >= compressed.length) {
                    System.out.println("break after ["+ (8 - i) +"] bit is evaluated since compressed is read completely");
                    break;
                }
            }//for bit
            
            //0001001000001111
            //0010010000001111
            
            //0001 + 1 0010 + 10 0000 1111
            //0010 + 1 0100 + 10 0000 1111
            //0011 + 1 0110 + 10 0000 1111
            //0100 + 1 1000 + 10 0000 1111
            //0101     1010      0000 1111
            //0110     1100      0000 1111
            //0111 +   1110 +  1 0000 1111
            //1101     1111      1111 1111
            // +1(dec)      +2(dec)
            
            
            int a = 0;
            
        }//white
        
        
        System.out.println(Utils.toHexDump(decompressed, 16, true, true, null));
        
        return decompressed;
    }
    
}
