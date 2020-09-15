package net.markus.projects.dh4;

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

    //still TODO
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
                        off = (off + 18) % maxOff;

                        if (DEBUG) {
                            String line = String.format("reference at offsetComp=%d/%d offsetDecomp=%d/%d offsetBuff=%d len=%d off=%d offDiff=%d full=%s offStr=%s | %s",
                                    offsetCompressed,
                                    compressed.length,
                                    offsetDecompressed,
                                    decompressSize,
                                    offsetBuffer,
                                    len,
                                    off,
                                    maxOff - off,
                                    Arrays.asList(p1, p2, p3, p4),
                                    offStr,
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

}
