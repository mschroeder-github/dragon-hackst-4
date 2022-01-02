package net.markus.projects.dh4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.markus.projects.dh4.util.Utils;

/**
 * Dragon Quest LZS algorithm.
 */
public class DQLZS {

    private static boolean DEBUG = false;
    private static boolean ASSERT = true;

    public static DecompressResult decompress(byte[] compressed, int decompressSize, boolean debug) {
        boolean b = DEBUG;
        DEBUG = debug;

        DecompressResult result = decompress(compressed, decompressSize);
        DEBUG = b;

        return result;
    }

    public static DecompressResult decompress(byte[] compressed, int decompressSize) {

        DecompressResult result = new DecompressResult();
        result.compressed = compressed;

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

                StringBuilder logSB = new StringBuilder();
                
                if (DEBUG) {
                    System.out.println();
                    System.out.println("control byte at offset=" + offsetCompressed + " " + controlByteBits + " | 0x" + Utils.bytesToHex(new byte[]{controlByte}));
                }
                
                logSB.append("control byte at offset=" + offsetCompressed + " " + controlByteBits + " | 0x" + Utils.bytesToHex(new byte[]{controlByte}) + "\n");

                boolean fullBreak = false;

                for (int i = controlByteBits.length() - 1; i >= 0; i--) {
                    char bit = controlByteBits.charAt(i);

                    if (bit == '1') {
                        //literal

                        byte literal = compressed[offsetCompressed];
                        
                        //System.out.println("(decomp)literal lit=" + literal);

                        HistoryEntry entry = new HistoryEntry();
                        entry.literal = true;
                        entry.controlBitIndex = 7-i;
                        entry.length = 1;
                        entry.data = new byte[] { literal };
                        entry.offsetCompressed = offsetCompressed;
                        entry.offsetDecompressed = offsetDecompressed;
                        result.history.add(entry);
                        
                        
                        decompressed[offsetDecompressed] = compressed[offsetCompressed];
                        buffer[offsetBuffer] = compressed[offsetCompressed];

                        if (DEBUG) {
                            System.out.println("write literal at offset=" + offsetCompressed + " | " + (literal & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{literal}) + " | 0x" + Utils.bytesToHex(new byte[]{literal}));
                        }
                        logSB.append("write literal at offset=" + offsetCompressed + " | " + (literal & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{literal}) + " | 0x" + Utils.bytesToHex(new byte[]{literal}) + "\n");
                        
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
                        
                        
                        byte[] data = new byte[len];
                        for (int j = 0; j < len; j++) {
                            data[j] = buffer[(off + j) % maxOff];
                        }
                        
                        HistoryEntry entry = new HistoryEntry();
                        entry.literal = false;
                        entry.controlBitIndex = 7-i;
                        entry.offset = off;
                        entry.length = len;
                        entry.data = data;
                        entry.offsetCompressed = offsetCompressed;
                        entry.offsetDecompressed = offsetDecompressed;
                        result.history.add(entry);
                        
                        //System.out.println("(decomp)reference off=" + off + ", len=" + len);

                        
                        String line = String.format("reference at len=%d off=%d storedOff=%d offsetComp=%d/%d offsetDecomp=%d/%d offsetBuff=%d offDiff=%d full=%s offStr=%s refBytesBits=%s | %s",
                                    len,
                                    off,
                                    storedOff,
                                    offsetCompressed,
                                    compressed.length,
                                    offsetDecompressed,
                                    decompressSize,
                                    offsetBuffer,
                                    maxOff - off,
                                    Arrays.asList(p1, p2, p3, p4),
                                    offStr,
                                    refBytesBits,
                                    Utils.bytesToHex(refBytes)
                        );
                        
                        logSB.append(line + "\n");
                        if (DEBUG) {
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
                            logSB.append("\twrite referred " + (literal & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{literal}) + " | 0x" + Utils.bytesToHex(new byte[]{literal}) + "\n");

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
                
                result.logging.add(logSB.toString());

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

    public static class HistoryEntry {
        
        public boolean literal;
        
        public int controlBitIndex;
        
        public int offset;
        public int length;
        
        public byte[] data;
        
        public int offsetCompressed;
        public int offsetDecompressed;
        
        public boolean isLiteral() {
            return literal;
        }
        
        public boolean isRef() {
            return !literal;
        }
        
        public String getType() {
            return literal ? "literal" : "reference";
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append((controlBitIndex + 1) + "/8");
            sb.append(" " + getType());
            sb.append(" offset=" + offset);
            sb.append(" length=" + length);
            sb.append(" data=" + Utils.toHexString(data));
            return sb.toString();
        }
        
        public static String compare(HistoryEntry a, HistoryEntry b) {
            
            if(a.literal != b.literal) {
                return "mode: " + a.getType() + " != " + b.getType();
            }
            
            if(a.literal && b.literal) {
                
                if(a.data[0] != b.data[0]) {
                    return "lit.data: 0x" + Utils.bytesToHex(a.data) + " != 0x" + Utils.bytesToHex(b.data);
                }
                
            } else {
                
                if(a.length != b.length) {
                    return "length: " + a.length + " != " + b.length;
                }
                
                if(a.offset != b.offset) {
                    return "offset: " + a.offset + " != " + b.offset;
                }
                
                if(Utils.compare(a.data, b.data) != -1) {
                    return "ref.data: 0x" + Utils.bytesToHex(a.data) + " != 0x" + Utils.bytesToHex(b.data);
                }
                
            }
            
            return "";
        }
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
        
        public byte[] compressed;
        
        public List<String> logging = new ArrayList<>();
        
        public List<HistoryEntry> history = new ArrayList<>();

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

    public static CompressResult compress(byte[] decompressed, boolean debug, DecompressResult decompResult) throws IOException {
        boolean b = DEBUG;
        DEBUG = debug;

        CompressResult result = compress(decompressed, decompResult);
        DEBUG = b;

        return result;
    }

    public static CompressResult compress(byte[] decompressed, DecompressResult decompResult) throws IOException {

        CompressResult result = new CompressResult();

        try {

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            
            Buffer bufferObj = new Buffer();
            bufferObj.debug = true;

            int decompressedIndex = 0;
            
            //int bufferIndex = 0;
            //int bufferSize = 0;
            
            int entryIndex = 0;

            while (decompressedIndex < decompressed.length) {

                String controlByteBits = "";

                //write here the data: literals or refs
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                StringBuilder logSB = new StringBuilder();
                logSB.append("control byte\n");

                //control byte => use their bits
                //1 = literal
                //0 = ref
                for (int controlByteIndex = 0; controlByteIndex < 8; controlByteIndex++) {
                    //decide if literal or ref

                    //we try to find a match in the buffer for the 18 bytes in the decompressed
                    byte[] decompressedMatchable = Arrays.copyOfRange(
                            decompressed, 
                            decompressedIndex, 
                            Math.min(decompressedIndex + 18, decompressed.length)
                    );
                    
                    //System.out.println("\ndecompressed");
                    //System.out.println(Utils.toHexDump(Arrays.copyOfRange(decompressed, 0, bufferObj.size), 50));
                    
                    BufferMatch bufferMatchObj = bufferObj.matchV2(decompressedMatchable, decompressedIndex, decompResult.history.size());

                    //-------------------
                    
                    boolean literal = !bufferMatchObj.match;

                    if (decompressedIndex < decompressed.length) {
                        //if there is still decompressed data that can be compressed

                        if (literal) {
                            controlByteBits += "1";

                            byte litValue = decompressed[decompressedIndex++];

                            //write literal
                            baos.write(litValue);
                            
                            //write also to buffer
                            bufferObj.fill(litValue);

                            if (DEBUG) {
                                //System.out.println("controlByteIndex=" + controlByteIndex + ", literal with litValue=" + litValue);
                                System.out.println("write literal at offset=" + (decompressedIndex-1) + " | " + (litValue & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{litValue}) + " | 0x" + Utils.bytesToHex(new byte[]{litValue}));
                            }
                            logSB.append("write literal at offset=" + (decompressedIndex-1) + " | " + (litValue & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{litValue}) + " | 0x" + Utils.bytesToHex(new byte[]{litValue}) + "\n");
                            
                            
                            HistoryEntry entry = new HistoryEntry();
                            entry.literal = true;
                            entry.length = 1;
                            entry.data = new byte[] { litValue };
                            entry.offsetDecompressed = decompressedIndex;
                            entry.controlBitIndex = controlByteIndex;
                            result.history.add(entry);
                            
                        } else {
                            controlByteBits += "0";
                            
                            int len = bufferMatchObj.length;
                            int off = bufferMatchObj.offset;
                            
                            HistoryEntry entry = new HistoryEntry();
                            entry.literal = false;
                            entry.length = len;
                            entry.offset = off;
                            entry.data = bufferMatchObj.pattern;
                            entry.offsetDecompressed = decompressedIndex;
                            entry.controlBitIndex = controlByteIndex;
                            result.history.add(entry);
                            
                            //the 00 00 0b case
                            //if(len == 3 && off == 4094) {
                            //    int a = 0;
                            //}
                            
                            //skip the bytes we have compressed with a ref
                            decompressedIndex += len;

                            int storeOffset = off - 18;
                            if (storeOffset < 0) {
                                storeOffset = bufferObj.buffer.length + storeOffset;
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

                            String line = String.format("reference: len=%d off=%d storeOffset=%d full=%s offStr=%s refBytesBits=%s refBytesHex=%s",
                                    len,
                                    off,
                                    storeOffset,
                                    Arrays.asList(p1, p2, p3, lenBits),
                                    offsetBits,
                                    refBytesBits,
                                    refBytesHex
                            );
                            if (DEBUG) {
                                System.out.println(line);
                            }
                            logSB.append(line + "\n");
                            //byte[] data = new byte[len];
                            for (int i = 0; i < len; i++) {
                                int index = (off + i);
                                byte lit = bufferObj.buffer[index % bufferObj.buffer.length];
                                //data[i] = lit;
                                if (DEBUG) {
                                    System.out.println("\tbuffer referred [off="+off+", index=" + index + ", buffer.size="+bufferObj.size+"] " + (lit & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{lit}) + " | 0x" + Utils.bytesToHex(new byte[]{lit}));
                                }
                                logSB.append("\tbuffer referred [off="+off+", index=" + index + ", buffer.size="+bufferObj.size+"] " + (lit & 0xff) + " | " + Utils.toHexStringASCII(new byte[]{lit}) + " | 0x" + Utils.bytesToHex(new byte[]{lit}) + "\n");
                            }
                            //entry.data = data;

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
                    
                    if(ASSERT) {
                    
                        HistoryEntry decomp = decompResult.history.get(entryIndex);
                        HistoryEntry   comp = result.history.get(entryIndex);

                        String compareResult = HistoryEntry.compare(comp, decomp);
                        
                        if(!compareResult.isEmpty()) {
                            System.out.println(compareResult);
                            System.out.println(Utils.splitScreen(comp.toString(), 75, decomp.toString()));
                            System.exit(0);
                        }
                    }
                    
                    entryIndex++;
                    
                }//for control byte

                controlByteBits = Utils.reverse(controlByteBits);

                int controlByteInt = Utils.bitsToIntLE(controlByteBits);
                byte controlByte = (byte) controlByteInt;

                String controlByteHex = Utils.bytesToHex(new byte[]{controlByte});

                output.write(controlByte);
                output.write(baos.toByteArray());
                
                logSB.append("control byte stored: " + controlByteBits + " 0x" + controlByteHex + ", output.size=" + output.size() + "\n");
                if (DEBUG) {
                    System.out.println("control byte stored: " + controlByteBits + " 0x" + controlByteHex + ", output.size=" + output.size());
                    System.out.println();
                }
                
                result.logging.add(logSB.toString());
                
                /*
                if(ASSERT) {
                    
                    HistoryEntry decomp = decompResult.history.get(entryIndex);
                    HistoryEntry comp = result.history.get(entryIndex);
                    
                    int a = 0;
                    
                    
                    byte[] currentOutput = output.toByteArray();
                    for(int i = 0; i < currentOutput.length; i++) {
                        if(decompResult.compressed[i] != currentOutput[i]) {

                            int logIndex = result.logging.size() - 1;
                            String decompLog = decompResult.logging.get(logIndex);

                            String screen = Utils.splitScreen(
                                    "my compression\n" + result.logging.get(logIndex), 
                                    110, 
                                    "given decompression\n" + decompLog
                            );

                            System.out.println(screen);
                            System.exit(0);
                        }
                    }
                    
                }
                */
                
                
                
            }//while
            
            result.data = output.toByteArray();

        } catch (Exception e) {

            result.exception = e;
            result.data = new byte[0];
            
            e.printStackTrace();
            
            if (DEBUG) {
                throw e;
            }
            
            if(ASSERT) {
                System.exit(1);
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
        
        public List<String> logging = new ArrayList<>();
        
        public List<HistoryEntry> history = new ArrayList<>();

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

    //extra implementation of the buffer for compression
    private static class Buffer {
        
        public byte[] buffer;
        public int size;
        
        public boolean debug;
        
        @Deprecated
        public int lastOffset;
        
        public static final int MAX_SIZE = 4096;
        
        public List<BufferMatch> matchHistory = new ArrayList<>();

        public Buffer() {
            buffer = new byte[MAX_SIZE];
            size = 0;
        }
        
        @Deprecated
        public BufferMatch match(byte[] data18bytes, int decompressedIndex) {
            
            println("===============================================");
            println("match method begin, decompressedIndex=" + decompressedIndex + ", buffer.size=" + size + ", data18bytes=" + Utils.toHexString(data18bytes));
            println(this.toString());
            
            int a = 0;
            
            //the minimum match is 3 bytes
            while(data18bytes.length >= 3) {
                
                List<BufferMatch> matches = new ArrayList<>();
                
                //checking makes only sense if buffer is filled
                if(size > 0) {

                    //from off to size
                    //      [data18bytes ........]
                    //[ --- |off< ---- size]
                    //thus, from off to (size - 1)
                    //4096 - 18 = 4078
                    //                             4094 4095 0          
                    //starting at 4094 is possible 0x00 0x00 0xb0
                    
                    int startOffset = size - 1; //!matchHistory.isEmpty() ? matchHistory.get(matchHistory.size() - 1).offset : size - 1;
                    
                    for(int off = startOffset; off >= -data18bytes.length; off--) {
                    //for(int off = -data18bytes.length; off < size; off++) {

                        //at the border at index=0 we have to go to the end of the buffer 4095
                        int offset = off;
                        if(offset < 0) {
                            offset = buffer.length + off;
                        }
                        
                        //but cycle from |off to size
                        CyclicBuffer cyclicBuffer = cyclicBuffer(offset, data18bytes.length);
                        //println("checking off=" + off + " with size=" + size + ", cyclicBuffer is " + Utils.toHexString(cyclicBuffer));
                        
                        int cmp = Utils.compare(data18bytes, cyclicBuffer.data);
                        if(cmp == -1) {
                            //println("checking data got a match (len=" + String.format("%02d", data18bytes.length) + ") " + Utils.toHexString(data18bytes));
                            
                            //if it matches, this length/offset combination works
                            BufferMatch match = new BufferMatch(true, offset, Arrays.copyOf(data18bytes, data18bytes.length));
                            match.cyclicBuffer = cyclicBuffer;
                            //println("found " + match.toString() + " for data (len=" + String.format("%02d", data18bytes.length) + ") " + Utils.toHexString(data18bytes));
                            
                            matches.add(match);
                            
                            //first match of zeros is the right one?
                            //answer: no, if len=18 the last match is used
                            //if(onlyZeros(data18bytes)) {
                            //    break;
                            //}
                        }
                    }
                }
                
                
                //we found matches
                if(!matches.isEmpty()) {
                    
                    BufferMatch selected = null;
                    
                    //maybe last offset decides
                    //matches.sort((a,b) -> Integer.compare(a.getDiffToLastOffset(), b.getDiffToLastOffset()));
                    
                    println("decompressedIndex=" + decompressedIndex + ", buffer.size=" + size);
                    
                    println("history: "+ matchHistory.size());
                    for(int i = 0; i < matchHistory.size(); i++) {
                        println("\t[" + i + "] " + matchHistory.get(i));
                    }
                    println("matches: "+ matches.size());
                    matches.forEach(m -> println("\t" + m.toString()));
                    
                    //32 is the max
                    //we found something at 7
                    
                    //decompressedIndex=32, buffer.size=8, offset=7, length=16,
                    //20 = 32 - 12 
                    //reference: len=16 off=7 | reference at len=12 off=20 
                    
                    //default: take the first one
                    selected = matches.get(0);
                    
                    //if(onlyZeros(selected.pattern)) {
                    //    fillArray(selected.pattern);
                    //}
                    
                    //keep history
                    matchHistory.add(selected);
                    
                    println("selected " + selected.toString() + "\n");
                    
                    return selected;
                }
                
                //we try if a smaller byte sequence matches
                data18bytes = Arrays.copyOfRange(data18bytes, 0, data18bytes.length - 1);
            }
            
            //we tried everything but there is no match
            BufferMatch match = new BufferMatch();
            match.pattern = new byte[] { data18bytes[0] };
            match.length = 1;
            match.offset = decompressedIndex;
            //lastOffset = buffer.length - 18;
            println("no match " + match +"\n");
            
            matchHistory.add(match);
            return match;
        }

        public BufferMatch matchV2(byte[] data18bytes, int decompressedIndex, int max) {
            
            println("===============================================");
            println("match method begin, decompressedIndex=" + decompressedIndex + ", buffer.size=" + size + ", data18bytes=" + Utils.toHexString(data18bytes));
            println("history: "+ matchHistory.size());
            for(int i = 0; i < matchHistory.size(); i++) {
                println("\t[" + i + "/" + max + "] " + matchHistory.get(i));
            }
            int bufferSize = matchHistory.stream().mapToInt(bm -> bm.pattern.length).sum();
            println("bufferSize=" + bufferSize + ", decompressedIndex=" + decompressedIndex);
            //println(this.toString());
            
            int a = 0;
            
            //the minimum match is 3 bytes
            while(data18bytes.length >= 3) {
                
                List<BufferMatch> matches = new ArrayList<>();
                
                //checking makes only sense if buffer is filled
                if(size > 0) {

                    findMatchesV2(data18bytes, decompressedIndex, matches);
                    
                    if(onlyZeros(data18bytes)) {
                        
                        BufferMatch prevMatch = matchHistory.get(matchHistory.size() - 1);
                        if(!prevMatch.zerosFromBehind && prevMatch.hasCyclicBuffer() && prevMatch.cyclicBuffer.endsWithZero /*&& prevMatch.cyclicBuffer.cycled*/) {
                            
                            byte[] copiedData = Arrays.copyOf(data18bytes, data18bytes.length);
                            
                            //from the end of buffer but cycled it seems
                            BufferMatch match = new BufferMatch(true, prevMatch.dstOffset + prevMatch.length - 1, copiedData);
                            match.dstOffset = decompressedIndex;
                            match.cyclicBuffer = new CyclicBuffer();
                            match.cyclicBuffer.data = copiedData;
                            match.cyclicBuffer.cycled = true; //maybe
                            match.cyclicBuffer.endsWithZero = true;
                            match.zerosFromBehind = true; //they are handled like zeros from behind
                            matches.add(match);
                            
                        } else {
                        
                            //zeros from behind
                            BufferMatch match = new BufferMatch(true, buffer.length - data18bytes.length, Arrays.copyOf(data18bytes, data18bytes.length));
                            match.zerosFromBehind = true;
                            match.dstOffset = decompressedIndex;
                            matches.add(match);
                        }
                    }
                    
                    
                }// size > 0
                
                
                //we found matches
                if(!matches.isEmpty()) {
                    
                    BufferMatch selected = null;
                    
                    //default: take the first one
                    selected = matches.get(0);
                    
                    /*
                    println("history: "+ matchHistory.size());
                    for(int i = 0; i < matchHistory.size(); i++) {
                        println("\t[" + i + "] " + matchHistory.get(i));
                    }
                    */
                    println("matches: "+ matches.size());
                    matches.forEach(m -> println("\t" + m.toString()));
                    
                    if(matches.size() > 1) {
                        
                        //prefer the one that was not a reference again
                        Optional<BufferMatch> opt = matches.stream().filter(m -> m.hasCyclicBuffer()).filter(m -> !m.cyclicBuffer.wasRef).findFirst();
                        
                        if(opt.isPresent()) {
                            selected = opt.get();
                        }
                    }
                    
                    
                    //32 is the max
                    //we found something at 7
                    
                    //decompressedIndex=32, buffer.size=8, offset=7, length=16,
                    //20 = 32 - 12 
                    //reference: len=16 off=7 | reference at len=12 off=20 
                    
                    
                    
                    //if(onlyZeros(selected.pattern)) {
                    //    fillArray(selected.pattern);
                    //}
                    
                    //keep history
                    matchHistory.add(selected);
                    
                    println("selected " + selected.toString() + "\n");
                    
                    return selected;
                }
                
                //we try if a smaller byte sequence matches
                data18bytes = Arrays.copyOfRange(data18bytes, 0, data18bytes.length - 1);
            }
            
            //we tried everything but there is no match
            BufferMatch match = new BufferMatch();
            match.pattern = new byte[] { data18bytes[0] };
            match.length = 1;
            match.offset = decompressedIndex;
            match.dstOffset = decompressedIndex;
            //lastOffset = buffer.length - 18;
            println("no match " + match +"\n");
            matchHistory.add(match);
            return match;
        }
        
        public void findMatchesV1(byte[] data18bytes, int decompressedIndex, List<BufferMatch> matches) {
            for(int i = matchHistory.size() - 1; i >= 0; i--) {
                        
                //was not these special zeros
                if(!matchHistory.get(i).zerosFromBehind) {

                    CyclicBuffer cyclicBuffer = historyBufferSeq(i, data18bytes.length);

                    int cmp = Utils.compare(data18bytes, cyclicBuffer.data);
                    if(cmp == -1) {
                        //println("checking data got a match (len=" + String.format("%02d", data18bytes.length) + ") " + Utils.toHexString(data18bytes));

                        //if it matches, this length/offset combination works
                        BufferMatch match = new BufferMatch(true, matchHistory.get(i).offset, Arrays.copyOf(data18bytes, data18bytes.length));
                        match.dstOffset = decompressedIndex;
                        match.offset = matchHistory.get(i).dstOffset;
                        match.cyclicBuffer = cyclicBuffer;
                        matches.add(match);
                    }
                }
            }
        }
        
        public void findMatchesV2(byte[] data18bytes, int decompressedIndex, List<BufferMatch> matches) {
            int len = data18bytes.length;
            boolean onlyZeros = onlyZeros(data18bytes);
            
            List<int[]> startPosList = new ArrayList<>();
            for(int i = matchHistory.size() - 1; i >= 0; i--) {
                byte[] pattern = matchHistory.get(i).pattern;
                for(int k = pattern.length - 1; k >= 0; k--) {
                    startPosList.add(new int[] { i, k });
                }
            }
            
            for(int[] startPos : startPosList) {
                
                CyclicBuffer cyclicBuffer = new CyclicBuffer();
                ByteArrayOutputStream seq = new ByteArrayOutputStream();
                
                boolean hasLiteral = false;
                
                for(int i = startPos[0]; i < matchHistory.size(); i++) {
                    
                    boolean start = i == startPos[0];
                    
                    BufferMatch curMatch = matchHistory.get(i);
                    
                    if(curMatch.length == 1) {
                        hasLiteral |= true;
                    }
                    
                    byte[] pattern = curMatch.pattern;
                    
                    //if(start && curMatch.zerosFromBehind && startPos[1] == 0) {
                        //do not start here
                    //    continue;
                    //}
                    
                    for(int j = start ? startPos[1] : 0; j < pattern.length; j++) {
                        
                        byte b = pattern[j];
                        seq.write(b);
                        
                        if(seq.size() >= len) {
                            break;
                        }
                    }
                    
                    if(seq.size() >= len) {
                        break;
                    }
                }
                
                //maybe fill
                while(seq.size() < data18bytes.length) {
                    seq.write(0);
                }

                cyclicBuffer.data = seq.toByteArray();
                cyclicBuffer.endsWithZero = cyclicBuffer.data[cyclicBuffer.data.length - 1] == 0;
                cyclicBuffer.wasRef = matchHistory.get(startPos[0]).length > 1;

                
                int cmp = Utils.compare(data18bytes, cyclicBuffer.data);
                if(cmp == -1) {
                    //println("checking data got a match (len=" + String.format("%02d", data18bytes.length) + ") " + Utils.toHexString(data18bytes));

                    if(onlyZeros) {
                        int a = 0;
                        
                        if(hasLiteral) {
                            //ok
                            int b = 0;
                            
                        } else {
                            //no
                            continue;
                        }
                    }
                    
                    //if it matches, this length/offset combination works
                    BufferMatch match = new BufferMatch(true, matchHistory.get(startPos[0]).offset + startPos[1], Arrays.copyOf(data18bytes, data18bytes.length));
                    match.dstOffset = decompressedIndex;
                    match.offset = matchHistory.get(startPos[0]).dstOffset;
                    match.cyclicBuffer = cyclicBuffer;
                    matches.add(match);
                }
                
                
            }//for start pos
        }
        
        private CyclicBuffer historyBufferSeq(int historyIndex, int len) {
            
            CyclicBuffer cyclicBuffer = new CyclicBuffer();
            
            ByteArrayOutputStream seq = new ByteArrayOutputStream();
            
            for(int i = historyIndex; i < matchHistory.size(); i++) {
                
                byte[] pattern = matchHistory.get(i).pattern;
                for(int j = 0; j < pattern.length; j++) {
                    
                    seq.write(pattern[j]);
                    
                    if(seq.size() >= len) {
                        break;
                    }
                }
                
                if(seq.size() >= len) {
                    break;
                }
                
                //cycles
                if(i == matchHistory.size() - 1) {
                    i = historyIndex - 1;
                    cyclicBuffer.cycled = true;
                    //after that i++ so we start again at historyIndex
                }
            }
            
            cyclicBuffer.data = seq.toByteArray();
            
            cyclicBuffer.endsWithZero = cyclicBuffer.data[cyclicBuffer.data.length - 1] == 0;
            
            cyclicBuffer.wasRef = matchHistory.get(historyIndex).length > 1;
            
            return cyclicBuffer;
        }
        
        private int maxZeroSeq(byte[] data18bytes) {
            int count = 0;
            for(int i = 0; i < data18bytes.length; i++) {
                if(data18bytes[i] == 0) {
                    count++;
                } else {
                    break;
                }
            }
            return count;
        }
        
        private boolean onlyZeros(byte[] data18bytes) {
            return maxZeroSeq(data18bytes) == data18bytes.length;
        }
        
        private CyclicBuffer cyclicBuffer(int off, int len) {
            //off can be 4095 thus we have to mod 4096
            
            CyclicBuffer b = new CyclicBuffer();
            
            b.data = new byte[len];
            int offsetIndex = off;
            for(int i = 0; i < len; i++) {
                b.data[i] = buffer[offsetIndex];
                offsetIndex++;
                
                //cycle in buffer, e.g. 4096 becomes 0
                offsetIndex %= buffer.length;
                
                //special case: after one step we reach the end
                //if(i == 0 && offsetIndex == size) {
                //    //remaining bytes are 0
                //    b.specialOneStep = true;
                //    break;
                //}
                
                //cyclic break point
                /*
                if(offsetIndex == size) {
                    
                    offsetIndex = off; //02 00 00 68 | 02 00 00
                    //also 18 times 00 can be cycled
                    
                    //if(b.data[i] != 0) {
                    //    offsetIndex = off; //02 00 00 68 | 02 00 00
                    //} else {
                    //    offsetIndex = 0;
                    //}
                    
                    b.sizeReached = true;
                    
                    //set as cycled, there are still some bytes
                    b.cycled = i < (len - 1);
                }
                */
            }
            
            return b;
        }
        
        //is called by match to fill the buffer
        public void fillArray(byte[] dataBytes) {
            //println("buffer fill " + Utils.toHexString(dataBytes));
            
            for(int i = 0; i < dataBytes.length; i++) {
                fill(dataBytes[i]);
            }
        }
        
        //is called by compress to add a literal
        public void fill(byte dataByte) {
            buffer[size] = dataByte;
            size++;
            
            //TODO loop
            if(size == buffer.length) {
                int a = 0;
            }
        }
        
        private void println(String line) {
            if(debug) {
                System.out.println(line);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Buffer{size=" + size + "}\n");
            sb.append(Utils.toHexDump(Arrays.copyOfRange(buffer, 0, size), 50));
            
            return sb.toString();
        }
        
    }
    
    private static class CyclicBuffer {
        
        public byte[] data;
        public boolean cycled;
        public boolean sizeReached;
        //public boolean specialOneStep;
        public boolean endsWithZero;
        public boolean wasRef;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CyclicBuffer{data.len=").append(data.length);
            sb.append(", cycled=").append(cycled);
            //sb.append(", sizeReached=").append(sizeReached);
            sb.append(", endsWithZero=").append(endsWithZero);
            sb.append(", wasRef=").append(wasRef);
            sb.append('}');
            return sb.toString();
        }
        
    }
    
    private static class BufferMatch {
        public boolean match;
        public int offset;
        public int dstOffset;
        public int length;
        public byte[] pattern;
        public int lastOffset;
        public CyclicBuffer cyclicBuffer;
        public boolean zerosFromBehind;
        
        public BufferMatch() {
            match = false;
        }
        
        public BufferMatch(boolean match, int offset, byte[] pattern) {
            this.match = match;
            this.offset = offset;
            this.pattern = pattern;
            this.lastOffset = lastOffset;
            this.length = pattern.length;
        }
        
        @Deprecated
        public int getDiffToLastOffset() {
            
            //between 4093 and 0 should be diff=3
            //thus, 4093 - 4096 = -3 to 0, diff=3
            int diff1 = Math.abs((offset - Buffer.MAX_SIZE) - lastOffset);
            
            //but also 60 to 70, diff=10
            int diff2 = Math.abs(offset - lastOffset);
            
            int diff = Math.min(diff1, diff2);
            return diff;
        }
        
        public boolean hasCyclicBuffer() {
            return cyclicBuffer != null;
        }
        
        //@Override
        //public String toString() {
        //    return "BufferMatch{" + "match=" + match + ", offset=" + offset + ", length=" + length + '}';
        //}

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("BufferMatch{match=").append(match);
            sb.append(", offset=").append(offset);
            sb.append(", dstOffset=").append(dstOffset);
            sb.append(", length=").append(length);
            sb.append(", zerosFromBehind=").append(zerosFromBehind);
            //sb.append(", lastOffset=").append(lastOffset);
            //sb.append(", diffToLastOffset=").append(getDiffToLastOffset());
            sb.append(", pattern=").append(Utils.toHexString(pattern));
            if(cyclicBuffer != null) {
                sb.append(", cyclicBuffer=").append(cyclicBuffer);
            }
            sb.append('}');
            return sb.toString();
        }
        
        
        
    }
}
