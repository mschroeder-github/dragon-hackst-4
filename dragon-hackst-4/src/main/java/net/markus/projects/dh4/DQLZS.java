package net.markus.projects.dh4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import net.markus.projects.dh4.util.Utils;

/**
 * Dragon Quest LZS algorithm.
 */
public class DQLZS {

    private static boolean DEBUG = false;

    public static DecompressResult decompress(byte[] compressed, int decompressSize, boolean debug) {
        boolean b = DEBUG;
        DEBUG = debug;

        DecompressResult result = decompress(compressed, decompressSize);
        DEBUG = b;

        return result;
    }

    public static DecompressResult decompress(byte[] compressed, int decompressSize) {

        DecompressResult result = new DecompressResult();

        //2^12 = 4096
        byte[] buffer = new byte[4096];
        int maxOff = 4096;

        //length = 2^4 = 16
        //int maxLen = 16;
        if (DEBUG) {
            System.out.println("decompressSize=" + decompressSize);
            System.out.println("compressSize=" + compressed.length);
            System.out.println(Utils.toHexDump(compressed, 16, true, false, null));
        }

        byte[] decompressed = new byte[decompressSize];

        int offsetCompressed = 0;
        int offsetDecompressed = 0;
        int offsetBuffer = 0;

        long begin = System.currentTimeMillis();

        try {

            while (offsetCompressed < compressed.length) {

                byte controlByte = compressed[offsetCompressed];
                String controlByteBits = Utils.toBits(controlByte);

                offsetCompressed++;

                if (DEBUG) {
                    System.out.println();
                    System.out.println("control byte at offset=" + offsetCompressed + " " + controlByteBits + " | 0x" + Utils.bytesToHex(new byte[]{controlByte}));
                }

                boolean fullBreak = false;

                for (int i = controlByteBits.length() - 1; i >= 0; i--) {
                    char bit = controlByteBits.charAt(i);

                    if (bit == '1') {
                        //literal

                        byte literal = compressed[offsetCompressed];

                        decompressed[offsetDecompressed] = compressed[offsetCompressed];
                        buffer[offsetBuffer] = compressed[offsetCompressed];

                        if (DEBUG) {
                            System.out.println("write literal at offset=" + offsetCompressed + " | " + (literal & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{literal}) + " | 0x" + Utils.bytesToHex(new byte[]{literal}));
                        }
                        offsetCompressed++;
                        offsetDecompressed++;
                        offsetBuffer++;

                        offsetBuffer %= maxOff;

                    } else if (bit == '0') {
                        //reference

                        //stop
                        if (offsetDecompressed >= decompressSize) {
                            //the reference is read
                            offsetCompressed += 2;
                            fullBreak = true;

                            result.stopReason = "break because offsetDecompressed >= decompressSize in reference bit [" + (8 - i) + "]";

                            break;
                        }

                        //two bytes for length and offset
                        byte[] refBytes = new byte[]{compressed[offsetCompressed], compressed[offsetCompressed + 1]};
                        String refBytesBits = Utils.toBits(refBytes);
                        String p1 = refBytesBits.substring(0, 4);
                        String p2 = refBytesBits.substring(4, 8);
                        String p3 = refBytesBits.substring(8, 12);
                        String p4 = refBytesBits.substring(12, 16);

                        //length
                        String lenPart = p4;
                        int len = Utils.bitsToIntLE(lenPart);
                        len = (len + 3);

                        //offset
                        String offStr = p3 + p1 + p2;
                        int off = Utils.bitsToIntLE(offStr);
                        int storedOff = off;
                        off = (off + 18) % maxOff;

                        if (DEBUG) {
                            String line = String.format("reference at offsetComp=%d/%d offsetDecomp=%d/%d offsetBuff=%d len=%d off=%d storedOff=%d offDiff=%d full=%s offStr=%s refBytesBits=%s | %s",
                                    offsetCompressed,
                                    compressed.length,
                                    offsetDecompressed,
                                    decompressSize,
                                    offsetBuffer,
                                    len,
                                    off,
                                    storedOff,
                                    maxOff - off,
                                    Arrays.asList(p1, p2, p3, p4),
                                    offStr,
                                    refBytesBits,
                                    Utils.bytesToHex(refBytes)
                            );

                            System.out.println(line);
                        }

                        //copy buffer to decompressed
                        for (int j = 0; j < len; j++) {
                            byte literal = buffer[(off + j) % maxOff];

                            decompressed[offsetDecompressed] = literal;
                            buffer[offsetBuffer] = literal;

                            if (DEBUG) {
                                System.out.println("\twrite referred " + (literal & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{literal}) + " | 0x" + Utils.bytesToHex(new byte[]{literal}));
                            }

                            offsetDecompressed++;
                            offsetBuffer++;

                            offsetBuffer %= maxOff;
                        }

                        offsetCompressed += 2;
                    }

                    //seems to be that a control byte does not have to be evaluated fully
                    if (offsetCompressed >= compressed.length) {
                        result.stopReason = "break after [" + (8 - i) + "] bit is evaluated since compressed is read completely";
                        break;
                    }

                }//for bit

                if (fullBreak) {
                    break;
                }

                int a = 0;

            }//while

            if (DEBUG) {
                System.out.println(Utils.toHexDump(decompressed, 16, true, true, null));

                System.out.println("compressSize=" + offsetCompressed + "/" + compressed.length);
                System.out.println("decompressSize=" + offsetDecompressed + "/" + decompressSize);

                boolean b1 = (offsetCompressed == compressed.length);
                boolean b2 = (offsetDecompressed == decompressSize);
                System.out.println("compress offsets match perfectly: " + b1);
                System.out.println("decompress offsets match perfectly: " + b2);
                System.out.println("all offsets match perfectly: " + (b1 && b2));
            }

        } catch (Exception e) {

            result.exception = e;
        }

        long end = System.currentTimeMillis();

        result.setBeginEnd(begin, end);
        result.data = decompressed;

        result.offsetBuffer = offsetBuffer;
        result.offsetCompressed = offsetCompressed;
        result.offsetDecompressed = offsetDecompressed;

        return result;
    }

    public static class DecompressResult {

        public byte[] data;
        public Exception exception;

        public long begin;
        public long end;

        public int offsetCompressed;
        public int offsetDecompressed;
        public int offsetBuffer;

        public String stopReason = "";

        public void setBeginEnd(long begin, long end) {
            this.begin = begin;
            this.end = end;
        }

        public long getDuration() {
            return end - begin;
        }

        @Override
        public String toString() {
            return "DecompressResult{" + "data=" + data.length + ", exception=" + exception + ", begin=" + begin + ", end=" + end + ", offsetCompressed=" + offsetCompressed + ", offsetDecompressed=" + offsetDecompressed + ", offsetBuffer=" + offsetBuffer + ", stopReason=" + stopReason + '}';
        }

    }

    public static CompressResult compress(byte[] decompressed, boolean debug) throws IOException {
        boolean b = DEBUG;
        DEBUG = debug;

        CompressResult result = compress(decompressed);
        DEBUG = b;

        return result;
    }

    public static CompressResult compress(byte[] decompressed) throws IOException {

        CompressResult result = new CompressResult();

        try {

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];

            int decompressedIndex = 0;
            int bufferIndex = 0;
            int bufferSize = 0;

            while (decompressedIndex < decompressed.length) {

                String controlByteBits = "";

                //write here the data: literals or refs
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                //control byte => use their bits
                //1 = literal
                //0 = ref
                for (int controlByteIndex = 0; controlByteIndex < 8; controlByteIndex++) {
                    //decide if literal or ref

                    //ok we have to check if we find a pattern that is larger than 3 bytes
                    //in the buffer
                    int len = 0;
                    int off = 0;

                    //assumption: it will match
                    boolean match = true;

                    //8+4+2+1 = 15 
                    //4 bits=15 + 3 = 18 max
                    //look at block 25336/0, it seems that it can not read longer than buffer was written
                    //int initLen = Math.min(bufferSize, 18);
                    //if(initLen < 3) {
                    //    initLen = 3;
                    //}
                    for (len = 18; len >= 3; len--) {

                        match = true;
                        
                        

                        for (off = buffer.length - len; off >= 0; off--) {
                        //for (off = 0; off < buffer.length - len; off++) {
                        
                            match = true;

                            //buffer index
                            int bi = off;
                            
                            //check the full pattern
                            for (int patternIndex = 0; patternIndex < len; patternIndex++) {

                                //pattern would be larger than decompressed data
                                if (patternIndex + decompressedIndex >= decompressed.length) {
                                    match = false;
                                    break;
                                }

                                byte decompByte = decompressed[patternIndex + decompressedIndex];
                                //no '% buffer.length'
                                //it seems not to cycle in the buffer
                                byte bufferByte = buffer[bi]; //buffer[(patternIndex + off)]; 
                                
                                //it seems to cycle this way: if it is larger then bufferIndex it starts at offset again
                                bi++;
                                if(bi > bufferIndex - 1) {
                                    bi = off;
                                }

                                //% buffer.length => it can cycle I guess
                                if (decompByte != bufferByte) {
                                    //try another offset
                                    match = false;
                                    break;
                                }
                            }
                            
                            //if(DEBUG) {
                            //    System.out.println("test len=" + len + ", off=" + off + ", match=" + match + ", off+len=" + (off+len));
                            //}

                            if (match) {
                                //this offset worked
                                break;
                            }
                        }//off

                        if (match) {
                            //this len worked
                            
                            //it seems to be that len + off can never by larger then bufferIndex (buffer size)
                            //if(len + off != buffer.length && len + off > bufferIndex) {
                                
                                //try another len
                            //    int a = 0;
                            //    if(DEBUG) {
                            //        System.out.println("len + off > bufferIndex: " + (len + off) + " > " + bufferIndex);
                            //    }
                                
                            //} else {
                            
                                //break means the len and off are fine
                                break;
                            //}
                        }
                        
                    }//len

                    //-------------------
                    boolean literal = !match;

                    if (decompressedIndex < decompressed.length) {
                        //if there is still decompressed data that can be compressed

                        if (literal) {
                            controlByteBits += "1";

                            byte litValue = decompressed[decompressedIndex++];

                            //write literal
                            baos.write(litValue);

                            //write also to buffer
                            buffer[bufferIndex] = litValue;
                            bufferIndex++;
                            bufferIndex = bufferIndex % buffer.length;

                            //we remember current buffer size
                            //bufferSize++;
                            //if (bufferSize > buffer.length) {
                            //    bufferSize = buffer.length;
                            //}

                            if (DEBUG) {
                                //System.out.println("controlByteIndex=" + controlByteIndex + ", literal with litValue=" + litValue);
                                System.out.println("write literal at offset=" + (decompressedIndex-1) + " | " + (litValue & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{litValue}) + " | 0x" + Utils.bytesToHex(new byte[]{litValue}));
                            }

                        } else {
                            controlByteBits += "0";

                            if (DEBUG) {
                                System.out.println("controlByteIndex=" + controlByteIndex + ", ref with len=" + len + ", off=" + off);
                            }

                            //skip the bytes we have compressed with a ref
                            decompressedIndex += len;

                            //write also to buffer
                            for (int i = 0; i < len; i++) {
                                
                                int index = (off + i); //% buffer.length;
                                
                                byte lit = buffer[index];
                                
                                buffer[bufferIndex] = lit;
                                bufferIndex++;
                                
                                if (DEBUG) {
                                    System.out.println("\tbuffer referred [off="+off+", index=" + index + ", bufferIndex="+bufferIndex+"] " + (lit & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{lit}) + " | 0x" + Utils.bytesToHex(new byte[]{lit}));
                                }
                                
                            }
                            //bufferIndex += len;
                            bufferIndex = bufferIndex % buffer.length;
                            
                            if (DEBUG) {
                                //System.out.println("controlByteIndex=" + controlByteIndex + ", ref with len=" + len + ", off=" + off);
                                
                                //System.out.println("buffer");
                                //System.out.println(Utils.toHexDump(Arrays.copyOfRange(buffer, 0, 64 * 2), 64));
                            }
                            
                            

                            //bufferSize += len;
                            //if (bufferSize > buffer.length) {
                            //    bufferSize = buffer.length;
                            //}

                            int storeOffset = off - 18;
                            if (storeOffset < 0) {
                                storeOffset = buffer.length + storeOffset;
                            }
                            int storeLen = len - 3;

                            String offsetBits = Utils.toBits(Utils.intToByteArray(storeOffset));
                            offsetBits = offsetBits.substring(offsetBits.length() - 12, offsetBits.length());

                            String p3 = offsetBits.substring(0, 4);
                            String p1 = offsetBits.substring(4, 8);
                            String p2 = offsetBits.substring(8, 12);

                            String lenBits = Utils.toBits(Utils.intToByteArray(storeLen));
                            lenBits = lenBits.substring(lenBits.length() - 4, lenBits.length());

                            String refBytesBitsB1 = p1 + p2;
                            String refBytesBitsB2 = p3 + lenBits;
                            String refBytesBits = refBytesBitsB1 + refBytesBitsB2;

                            byte b1 = (byte) Utils.bitsToIntLE(refBytesBitsB1);
                            byte b2 = (byte) Utils.bitsToIntLE(refBytesBitsB2);

                            byte[] refBytes = new byte[]{b1, b2};

                            String refBytesHex = Utils.bytesToHex(refBytes);

                            baos.write(refBytes);

                            if (DEBUG) {
                                String line = String.format("reference: len=%d off=%d storeOffset=%d full=%s offStr=%s refBytesBits=%s refBytesHex=%s",
                                        len,
                                        off,
                                        storeOffset,
                                        Arrays.asList(p1, p2, p3, lenBits),
                                        offsetBits,
                                        refBytesBits,
                                        refBytesHex
                                );
                                System.out.println(line);
                            }

                        }//ref else

                    } else {
                        //all decompressed data is read

                        //fill with zeros
                        while (controlByteBits.length() < 8) {
                            controlByteBits += "0";
                        }

                        //break from  for(int controlByteIndex
                        break;
                    }
                }

                controlByteBits = Utils.reverse(controlByteBits);

                int controlByteInt = Utils.bitsToIntLE(controlByteBits);
                byte controlByte = (byte) controlByteInt;

                String controlByteHex = Utils.bytesToHex(new byte[]{controlByte});

                output.write(controlByte);
                output.write(baos.toByteArray());

                if (DEBUG) {
                    System.out.println("control byte stored: " + controlByteBits + " 0x" + controlByteHex + ", output.size=" + output.size());
                    System.out.println();
                }
            }

            result.data = output.toByteArray();

        } catch (Exception e) {

            result.exception = e;
            result.data = new byte[0];
            
            if (DEBUG) {
                throw e;
            }
        }

        return result;
    }

    public static class CompressResult {

        public byte[] data;
        public Exception exception;

        public int decompressSize;

        public long begin;
        public long end;

        public String stopReason = "";

        public void setBeginEnd(long begin, long end) {
            this.begin = begin;
            this.end = end;
        }

        public long getDuration() {
            return end - begin;
        }

        @Override
        public String toString() {
            return "CompressResult{" + "data=" + data.length + ", exception=" + exception + ", begin=" + begin + ", end=" + end + ", stopReason=" + stopReason + '}';
        }

    }

}
