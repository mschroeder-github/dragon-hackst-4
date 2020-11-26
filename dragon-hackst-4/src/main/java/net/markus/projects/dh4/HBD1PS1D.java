package net.markus.projects.dh4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import net.markus.projects.dh4.data.H60010108;
import net.markus.projects.dh4.data.HBDBlock;
import net.markus.projects.dh4.data.ParseNode;
import net.markus.projects.dh4.data.StarZeros;
import net.markus.projects.dh4.data.StarZerosSubBlock;
import net.markus.projects.dh4.data.TextBlock;
import net.markus.projects.dh4.data.VeryFirstBlock;
import net.markus.projects.dh4.util.MinAvgMaxSdDouble;
import net.markus.projects.dh4.util.Utils;
import org.apache.commons.io.FileUtils;

/**
 * The main data archive of dragon quest 4.
 */
public class HBD1PS1D {

    public File file;
    public byte[] data;

    //for japanese to byte conversion
    public PsxJisReader reader;

    //all blocks
    public List<HBDBlock> blocks;

    //the first 2048 bytes of the file
    public VeryFirstBlock veryFirstBlock;

    //only the * 00 00 00 blocks
    public List<StarZeros> starZerosList;

    //only the 60 01 01 08 blocks
    public List<H60010108> h60010108List;

    public List<TextBlock> textBlocks;

    public static final byte[] qQES = new byte[]{(byte) 'q', (byte) 'Q', (byte) 'E', (byte) 'S'};
    public static final byte[] TIM = Utils.hexStringToByteArray("10000000");

    private int numberOfTrailingZeros;

    public static final List<Integer> imageTypes = Arrays.asList(
            1, //font image
            5, //image (from VII)
            6, //multi image
            8, //TIM
            9, //gradient
            10, //multi TIM
            13, //sprites
            14, //sprites
            15, //sprites
            16, //sprites (VII)
            17, //sprites
            18, //sprites
            19, //battle effects
            //21 is in VII also background texture
            25, //background texture
            43 //sprites
    );

    public static final List<Integer> qqesTypes = Arrays.asList(
            20,
            21,
            22,
            24
    );

    public static final List<Integer> dq7TextTypes = Arrays.asList(23, 24, 25, 27);
    public static final List<Integer> dq4TextTypes = Arrays.asList(40, 42);

    public HBD1PS1D(File file) throws IOException {
        this.file = file;

        System.out.println("read " + file.length() + " bytes in RAM...");
        data = FileUtils.readFileToByteArray(file);
        System.out.println("done");

        blocks = new ArrayList<>();
        starZerosList = new ArrayList<>();
        h60010108List = new ArrayList<>();

        reader = new PsxJisReader();
        reader.readTable();
    }

    //extracts all blocks and puts them in lists for further analysis
    public void correctBlockExtraction() {

        //always this length
        int len = 2048;
        int offset = 2048;
        int blockIndex = 1;

        VeryFirstBlock vfb = new VeryFirstBlock();
        vfb.pos = 0;
        vfb.data = Arrays.copyOfRange(data, 0, len);
        vfb.blockIndex = 0;
        vfb.header = vfb.data;
        vfb.full2048 = vfb.data;
        vfb.writeThisBlock = vfb.data;

        blocks.add(vfb);
        veryFirstBlock = vfb;

        while (true) {
            byte[] block = Arrays.copyOfRange(data, offset, offset + len);

            //60010180 block
            if (Arrays.equals(Arrays.copyOfRange(block, 0, 4), Utils.hexStringToByteArray("60010180"))) {

                byte[] onlyHeader = Arrays.copyOfRange(block, 0, 32);

                H60010108 h60block = new H60010108();
                h60block.pos = offset;
                h60block.blockIndex = blockIndex;
                h60block.header = onlyHeader;
                h60block.full2048 = block;
                h60block.writeThisBlock = block;
                h60block.data = block;

                //in h60010180Extraction we extract the information
                h60010108List.add(h60block);
                blocks.add(h60block);

                offset += len;

            } else if (block[0] != 0 && Arrays.equals(Arrays.copyOfRange(block, 1, 4), Utils.hexStringToByteArray("000000"))) {

                //the * 00 00 00 header
                int numberOf2048blocks = Utils.bytesToIntLE(Arrays.copyOfRange(block, 4, 4 + 4));

                StarZeros starZerosEntry = new StarZeros();
                starZerosEntry.blockIndex = blockIndex;
                starZerosEntry.pos = offset;
                starZerosEntry.full2048 = Arrays.copyOfRange(data, offset, offset + (len * numberOf2048blocks));

                starZerosList.add(starZerosEntry);
                blocks.add(starZerosEntry);

                offset += len * (numberOf2048blocks);

            } else {
                break;
            }

            blockIndex++;

        }//for loop

        //check where trailing zeros start
        int i = data.length - 1;
        for (; i > 0; i--) {
            if (data[i] != 0) {
                break;
            }
        }
        i++;

        numberOfTrailingZeros = (int) (file.length() - (long) i);

        System.out.println(blocks.size() + " blocks in total");
        System.out.println("1 very first block");
        System.out.println(starZerosList.size() + " star zeros blocks");
        System.out.println(h60010108List.size() + " h60010108 blocks");

        System.out.println("block count matches: " + ((1 + starZerosList.size() + h60010108List.size()) == blocks.size()));

        System.out.println("trailing zero starting at " + "0x" + Integer.toHexString(i));
        System.out.println("end found at " + "0x" + Integer.toHexString(offset));
    }

    public void h60010180Extraction() {

        for (H60010108 h60 : h60010108List) {

            byte[] block = h60.header;

            h60.index = Utils.bytesToShortLE(Arrays.copyOfRange(block, 4, 6));

            //always 5
            h60.count = Utils.bytesToShortLE(Arrays.copyOfRange(block, 6, 8));

            //it is a short and also an int because there is > 255
            h60.part = Utils.bytesToIntLE(Arrays.copyOfRange(block, 8, 12));

            //the unknown integer
            h60.v12to16 = Utils.bytesToIntLE(Arrays.copyOfRange(block, 12, 16));

            //int v12to14 = Utils.bytesToShortLE(Arrays.copyOfRange(block, 12, 14));
            //int v14to16 = Utils.bytesToShortLE(Arrays.copyOfRange(block, 14, 16)); //always zero
            //always 128
            h60.v16to18 = Utils.bytesToShortLE(Arrays.copyOfRange(block, 16, 18));

            //always 120
            h60.v18to20 = Utils.bytesToShortLE(Arrays.copyOfRange(block, 18, 20));

            h60.v20to22 = Utils.bytesToShortLE(Arrays.copyOfRange(block, 20, 22));

            //String headStr = Utils.toHexString(32,     4, 2, 2, 4, 4, 2, 2, 4, 2, 4, 2);
            h60.headerHexString = Utils.toHexString(block, 4, 2, 2, 4, 4, 2, 2, 4, 2, 4, 2);
            //System.out.println(headStr);
            //System.out.println(str);

            //where the FF seqence starts
            int i = h60.data.length - 1;
            for (; i >= 0; i--) {
                if ((h60.data[i] & 0xff) != 0xff) {
                    break;
                }
            }
            i++;
            h60.trailingFFstartPos = i;

            //set correct data block
            h60.data = Arrays.copyOfRange(h60.full2048, 32, i);

            //maybe a https://wiki.multimedia.cx/index.php/XA_ADPCM
            //> XA ADPCM is a type of ADPCM specified for XA format CD-ROMs. It is commonly used on Sony PlayStation games. 
            //http://problemkaputt.de/psx-spx.htm#cdromxaaudioadpcmcompression

            /*
            System.out.println(Arrays.asList(
                    "0x" + Integer.toHexString(offset), 
                    "blockIndex=" + blockIndex,
                    "60 01 01 08 header",
                    "index=" + index, 
                    "count=" + count,
                    "part(8-12)=" + part,
                    "12-16=" + v12to16,
                    //"first 0xFF=" + i,
                    "data size=" + (i - 32)
            ));
             */
            //h60010108dataSize.add(i - 32);
            //at 0x94da000 starts something new with the FF longer sequence with another header?
            //header starts with 60 01 01 08 always
            //seem that numbered subelements exists
            //seem to be that FF is the filling 
            //they are 2048 blocks starting with this header
            //0x94dc800 seems next one
            //break;
        }
    }

    //just analysis
    @Deprecated
    public void h60010180Analysis() {

        List<List<H60010108>> listOfparts = new ArrayList<>();
        List<H60010108> partList = new ArrayList<>();

        for (int i = 0; i < h60010108List.size(); i++) {

            H60010108 h60 = h60010108List.get(i);

            System.out.println(h60);

            if (h60.v12to16 == 0 && !partList.isEmpty()) {

                listOfparts.add(partList);

                partList = new ArrayList<>();
            }

            partList.add(h60);
        }

        if (!partList.isEmpty()) {
            listOfparts.add(partList);
        }

        //40 cluster if we put the parts together
        //downloaded soundtrack of DQ4 has 34 entries
        /*
        List<Integer> typeList = types.stream().sorted().collect(toList());
        System.out.println("indices found: " + h60010108indices);
        System.out.println("count found: " + h60010108counts);
        System.out.println(h60010108dataSize.getHistogram().toString());
         */
        int a = 0;
    }

    public void starZerosExtraction() {

        for (StarZeros starZeros : starZerosList) {

            byte[] block = starZeros.full2048;

            starZeros.blocks = Utils.bytesToIntLE(Arrays.copyOfRange(block, 0, 0 + 4));
            starZeros.sectors = Utils.bytesToIntLE(Arrays.copyOfRange(block, 4, 4 + 4));

            //has to be int so that the reading works correctly
            //maybe this is a long = 8 byte
            starZeros.sizeTotal = Utils.bytesToIntLE(Arrays.copyOfRange(block, 8, 8 + 4));
            starZeros.zeros = Utils.bytesToIntLE(Arrays.copyOfRange(block, 12, 12 + 4)); //always zero

            int offset = 16;
            for (int i = 0; i < starZeros.blocks; i++) {

                StarZerosSubBlock szb = new StarZerosSubBlock();
                szb.blockIndex = i;
                szb.parent = starZeros;
                szb.size = Utils.bytesToIntLE(Arrays.copyOfRange(block, offset, offset + 4));

                szb.header = Arrays.copyOfRange(block, offset, offset + 16);
                szb.headerHexString = Utils.toHexString(szb.header, 4, 4, 4, 2, 2);

                offset += 4;

                //seems to be a header block of size 16 bytes
                szb.sizeUncompressed = Utils.bytesToIntLE(Arrays.copyOfRange(block, offset, offset + 4));

                offset += 4;

                szb.unknown = Utils.bytesToIntLE(Arrays.copyOfRange(block, offset, offset + 4));

                offset += 4;

                szb.flags1 = Utils.bytesToShortLE(Arrays.copyOfRange(block, offset, offset + 2));

                offset += 2;

                szb.type = Utils.bytesToShortLE(Arrays.copyOfRange(block, offset, offset + 2));

                offset += 2;

                szb.compressed = szb.sizeUncompressed != szb.size && szb.flags1 != 0; //maybe better to check with sizes

                starZeros.starZerosBlocks.add(szb);
            }

            //I tested 28, 30, 32
            //for 32 the afterData part is all zero, but only if regular (size == size2)
            //makes sense because 16 byte block, and 16 byte block
            starZeros.header = Arrays.copyOfRange(block, 0, 16);

            //16 bytes
            starZeros.headerHexString = Utils.toHexString(starZeros.header, 4, 4, 4, 4);

            //data comes after the 16 byte headers
            starZeros.data = Arrays.copyOfRange(block, offset, offset + starZeros.sizeTotal);

            //the rest data after reading sizeTotal
            starZeros.rest = Arrays.copyOfRange(block, offset + starZeros.sizeTotal, block.length);

            //data of blocks
            offset = 0;
            for (int i = 0; i < starZeros.starZerosBlocks.size(); i++) {

                StarZerosSubBlock szb = starZeros.starZerosBlocks.get(i);

                szb.data = Arrays.copyOfRange(starZeros.data, offset, offset + szb.size);

                offset += szb.size;
            }

            //may not work if data ends with 00
            int i = starZeros.full2048.length - 1;
            for (; i >= 0; i--) {
                if (starZeros.full2048[i] != 0) {
                    break;
                }
            }
            i++;
            starZeros.trailing00startPos = i;

            //starZeros.data = Arrays.copyOfRange(block, starZeros.header.length, starZeros.trailing00startPos);
            //starZeros.data = Arrays.copyOfRange(block, offset, offset + starZeros.sizeTotal);
            //if the one size is exactly the second size
            //starZeros.regular = starZeros.sizeTotal == starZeros.size2;
            //if(starZeros.regular) {
            //    regularCount++;
            //}
            //boolean b = starZeros.data.length == starZeros.size - 1;
            //int diff = starZeros.trailing00startPos - starZeros.size;
            //int approx = (starZeros.trailing00startPos - starZeros.header.length);
            //System.out.println(starZeros.headerHexString);
            //int a = 0;
            /*
            starZeros.afterDataSize1 = Arrays.copyOfRange(block, 
                    starZeros.header.length + starZeros.sizeTotal, 
                    block.length
            );
            starZeros.afterDataSize2 = Arrays.copyOfRange(block, 
                    starZeros.header.length + starZeros.size2, 
                    block.length
            );
            if(starZeros.header.length + starZeros.size3 < block.length) {
                starZeros.afterDataSize3 = Arrays.copyOfRange(block, 
                    starZeros.header.length + starZeros.size3, 
                    block.length
                );
            }
             */
            //if regular the size works when reading starts from a 32 byte header
            /*
            if(starZeros.regular && !Utils.allZero(starZeros.afterDataSize1)) {
                System.out.println("after data should be all 00 but was");
                System.out.println(Utils.toHexString(starZeros.afterDataSize1));
                System.out.println("at " + starZeros.hexPos() + " (" + starZeros.pos + ")");
                break;
            }
             */
            //if(starZeros.size3 < starZeros.size) {
            //    System.out.println("decompressed size should be >= size");
            //    System.out.println(starZeros.size3  + " vs " + starZeros.size);
            //    break;
            //}
            /*
            System.out.println(Arrays.asList(
                    "0x" + Integer.toHexString(offset), 
                    "blockIndex=" + blockIndex,
                    "type=" + typeMaybe, 
                    "numberOf2048blocks=" + numberOf2048blocks, 
                    "byteSize=" + size, 
                    unknown, 
                    "secondByteSize=" + size2, 
                    "decompressedSize=" + decompressedSize
            ));
             */
        }

        //1058/3243
        //System.out.println(regularCount + "/" + starZerosList.size());
    }

    //just analysis
    @Deprecated
    public void starZerosAnalysis() throws IOException {

        MinAvgMaxSdDouble blockSizes = new MinAvgMaxSdDouble();
        MinAvgMaxSdDouble subBlockTypes = new MinAvgMaxSdDouble();

        MinAvgMaxSdDouble flags1stats = new MinAvgMaxSdDouble();

        Set<Integer> size12DifferTypes = new HashSet<>();

        Map<Integer, Set<Integer>> type2bytesToRead = new HashMap<>();

        //System.out.println(Utils.toHexString(128, 4, 4, 4, 4, 4, 4, 4, 2, 2));
        //System.out.println("qQES: " + Utils.toHexString(qQES));
        for (StarZeros starZeros : starZerosList) {

            //if(starZeros.type == 2) {
            //    byte[] begin = Arrays.copyOfRange(starZeros.full2048, 0, 128);
            //    System.out.println(Utils.toHexString(begin, 4, 4, 4, 4, 4, 4, 4, 2, 2));
            //}
            //byte[] begin = Arrays.copyOfRange(starZeros.full2048, 0, 256);
            //List<Integer> found = Utils.find(qQES, begin);
            //if(!found.isEmpty()) {
            //same data e.g. qQES comes in different types 
            //based on the type there is a different offset
            //type 1 has no offset
            //System.out.println(Utils.toHexString(begin, 4, 4, 4, 4, 4, 4, 4, 2, 2));
            //System.out.println(Utils.toHexString(begin, 4, 4, 4, 4, 16, 16, 16, 16));
            //System.out.println(starZeros.type + " " + found.get(0));
            //}
            int sizeOfBlocksTotal = 0;

            System.out.println(starZeros.blockIndex + ": " + starZeros.headerHexString);

            //System.out.println(starZeros.headerHexString);
            for (StarZerosSubBlock szb : starZeros.starZerosBlocks) {
                //System.out.println("\t" + szb.data.length + " " + szb + " " + szb.headerHexString);

                subBlockTypes.add(szb.type);

                byte[] preview = Arrays.copyOfRange(szb.data, 0, 128);

                List<Integer> qQESposs = Utils.find(qQES, preview);

                List<Integer> TIMposs = Utils.find(TIM, preview);

                List<Integer> hiraganaPoss = Utils.findHiragana(5, szb.data);

                String extra = "";
                if (!hiraganaPoss.isEmpty()) {
                    extra += "contains Hiragana ";
                }
                if (!qQESposs.isEmpty()) {
                    extra += "pQES ";
                }
                //if(!TIMposs.isEmpty()) {
                //    extra += "maybe TIM ";

                //    System.out.println(Utils.toHexString(szb.data));
                //}
                if (szb.compressed) {
                    extra += "compressed ";
                }

                //list all
                System.out.println("\t" + szb.blockIndex + ": " + szb.headerHexString + " " + extra);

                flags1stats.add(szb.flags1);

                sizeOfBlocksTotal += szb.size;

                //works: there is always some data
                if (Utils.allZero(szb.data)) {
                    System.out.println("should not happen");
                    break;
                }

                //works: when compressed the sizeUncompressed is always larger
                if (szb.compressed && szb.size > szb.sizeUncompressed) {
                    System.out.println("something wrong here");
                    break;
                }

                //if(szb.compressed) {
                //    ByteArrayInputStream bais = new ByteArrayInputStream(szb.data);
                //    FF7LZSInputStream ff7lzs = new FF7LZSInputStream(bais);
                //    byte[] uncompressed = new byte[128];//szb.sizeUncompressed];
                //    ff7lzs.read(uncompressed);
                //    int a = 0;
                //}
                if (!hiraganaPoss.isEmpty()) {
                    //System.out.println(szb.parent.blockIndex + ": " + szb.parent.headerHexString);
                    //System.out.println("\t" + szb.blockIndex + ": " + szb.headerHexString);

                    //System.out.println(Utils.toHexString(szb.data));
                    //System.out.println(Utils.toHexStringJp(szb.data, reader.sjishort2char));
                }

                if (!szb.compressed) {
                    //System.out.println(Utils.toHexString(preview));
                    //System.out.println(Utils.toHexStringChar(preview));
                }

                //works as explained in http://loveemu.hatenablog.com/entry/20140103/PlayStation_Dragon_Quest_Format
                //a 60 byte header and then comes "qQES"
                //List<Integer> found = Utils.find(qQES, preview);
                //if(!found.isEmpty()) {
                //    System.out.println(Utils.toHexStringOne(127));
                //    System.out.println(Utils.toHexString(preview));
                //    System.out.println(Utils.toHexStringChar(preview));
                //}
            }

            //works
            //System.out.println(starZeros.sizeTotal + " =?= " + sizeOfBlocksTotal + " =?= " + starZeros.data.length);
            //works
            if (starZeros.sizeTotal != sizeOfBlocksTotal) {
                System.out.println("something wrong here");
                break;
            }

            //works
            if (starZeros.data.length != sizeOfBlocksTotal) {
                System.out.println("something wrong here");
                break;
            }

            blockSizes.add(starZeros.blocks);

            if (!Utils.allZero(starZeros.rest)) {
                System.out.println(starZeros + " rest not all zeros");
                break;
            }

            //works
            //if(starZeros.blocks == 1 && starZeros.sizeTotal != starZeros.size2) {
            //    System.out.println("starZeros.type == 1 && starZeros.size1 != starZeros.size2");
            //   break;
            //}
            //if(starZeros.sizeTotal + 32 > starZeros.full2048.length) {
            //    System.out.println("size1 overflow");
            //    break;
            //}
            //if(starZeros.sizeTotal != starZeros.size2) {
            //System.out.println("");
            //System.out.println(starZeros.headerHexString);
            //is there another header? no seem not to be
            //for(int i = 0; i < starZeros.full2048.length; i += 2048) {
            //    byte[] sector = Arrays.copyOfRange(starZeros.full2048, i, i + 2048);
            //    
            //    System.out.println("sector " + (i/2048) + ": " + Utils.toHexString(sector));
            //}
            // if(!Utils.allZero(starZeros.afterDataSize1)) {
            //System.out.println("still after data size 1 is not zero");
            //System.out.println(starZeros.size1 + " size1");
            //System.out.println(starZeros.full2048.length + " full");
            //System.out.println((starZeros.full2048.length - starZeros.size1 - 32) + " diff");
            //System.out.println(starZeros.size2 + " size2");
            //System.out.println(starZeros.size3 + " size3");
            //System.out.println(starZeros.trailing00startPos + " starZeros.trailing00startPos");
            //System.out.println((starZeros.full2048.length - starZeros.trailing00startPos) + " bytes are 00");
            //System.out.println((starZeros.trailing00startPos - starZeros.size1 - 32 - 1) + " bytes have to be read");
            //System.out.println(starZeros.type + " type");
            //System.out.println(starZeros.sectors + " sectors");
            //System.out.println(Utils.toHexString(starZeros.afterDataSize1.length));
            //System.out.println(Utils.toHexString(starZeros.afterDataSize1));
            //type2bytesToRead.computeIfAbsent(starZeros.blocks, i -> new HashSet<>()).add((starZeros.trailing00startPos - starZeros.sizeTotal - 32 - 1));
            //int a = 0;
            //}
            //size12DifferTypes.add(starZeros.blocks);
            //size2 is not a replacement for size1
            //if(Utils.allZero(starZeros.afterDataSize1) && !Utils.allZero(starZeros.afterDataSize2)) {
            //    int a = 0;
            //}
            //size2 is always smaller or equal size1
            //because this could be the sub block size
            //if(starZeros.sizeTotal < starZeros.size2) {
            //    int a = 0;
            //}
            //}
            //cut was wrong
            //if(!Utils.allZero(starZeros.afterDataSize1)) {
            //System.out.println("sizes: " + Arrays.asList(starZeros.size1, starZeros.size2, starZeros.size3));
            //System.out.println(Utils.toHexString(starZeros.afterDataSize1));
            //    int a = 0;
            //}
        }

        System.out.println("blockSizes histogram:");
        System.out.println(blockSizes.getHistogram());

        /*
        count	prop%	key
        18001	75,546%	0.0    (not compressed?)
        5821	24,429%	1280.0 (compressed?) 00 05
        3	0,013%	32.0
        3	0,013%	126.0
         */
        System.out.println("flags1stats histogram:");
        System.out.println(flags1stats.getHistogram());

        System.out.println("subBlockTypes histogram:");
        System.out.println(subBlockTypes.getHistogram());

        List<Integer> types = new ArrayList<>(subBlockTypes.getHistogram().getValue2Count().keySet());
        Collections.sort(types);
        System.out.println("types: " + types);

        //if not 1
        //System.out.println("size12DifferTypes: " + size12DifferTypes);
        //for(Entry<Integer, Set<Integer>> e : type2bytesToRead.entrySet().stream().sorted(Entry.comparingByKey()).collect(toList())) {
        //    System.out.println(e + " " + e.getValue().stream().mapToInt(i -> i).max().getAsInt() + " " + e.getKey() * 16);
        //}
    }

    //extracts text blocks and also call tree extraction
    public void textBlockExtraction(List<Integer> types) {
        textBlocks = new ArrayList<>();

        for (Integer type : types) {
            for (StarZerosSubBlock sb : getStarZerosSubBlocks(type)) {

                TextBlock tb = new TextBlock();
                tb.subBlock = sb;

                //tb.dataSize1 = Arrays.copyOfRange(sb.data, 8, 8+tb.a);
                tb.a = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 0, 0 + 4));
                tb.b = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 4, 4 + 4));
                tb.c = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 8, 8 + 4)); //header size = 4 * 6 bytes
                tb.d = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 12, 12 + 4));
                tb.e = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 16, 16 + 4));
                tb.f = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 20, 20 + 4));

                tb.header = Arrays.copyOfRange(sb.data, 0, 24);

                //if c is larger than 24 this can happen
                tb.dataHeaderToCE = Arrays.copyOfRange(sb.data, 24, tb.c);

                //copy
                tb.huffmanCode = Arrays.copyOfRange(sb.data, tb.c, tb.e);

                //if d is zero there is no extra range, so use a for it
                if (tb.d == 0) {
                    tb.d = tb.a;
                }

                if (tb.d != 0) {
                    //two ints and a short = +10
                    tb.huffmanTreeBytes = Arrays.copyOfRange(sb.data, tb.e + 10, tb.d);
                }
                if (tb.d != 0) {
                    tb.dataDA = Arrays.copyOfRange(sb.data, tb.d, tb.a);
                }

                //the two (or three) ints at e and e+4
                tb.e1 = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, tb.e, tb.e + 4));
                tb.e2 = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, tb.e + 4, tb.e + 8));
                tb.e3 = Utils.bytesToShortLE(Arrays.copyOfRange(sb.data, tb.e + 8, tb.e + 10));

                //at the end
                tb.atA = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, tb.a, tb.a + 4));
                tb.atAnext = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, tb.a + 4, tb.a + 8));

                //atAnext is a number and we have to read number * 8 bytes
                tb.dataAtA = new byte[tb.atAnext * 8];
                for (int i = 0; i < tb.dataAtA.length; i++) {
                    //+8 : jump over atA (int) and atAnext (int)
                    tb.dataAtA[i] = tb.subBlock.data[tb.a + 8 + i];
                }

                textBlocks.add(tb);
            }
        }

        textBlocks.sort((a, b) -> Integer.compare(a.huffmanTreeBytes.length, b.huffmanTreeBytes.length));

        /*
        int lastLength = 0;
        boolean skipSameLength = true;

        for (TextBlock tb : maybeTextBlocks) {

            if (skipSameLength && tb.dataED.length == lastLength) {
                continue;
            }

            textBlockTreeExtractionV2(tb);

            lastLength = tb.dataED.length;
        }
         */
    }

    //a huffman tree entry to node
    //deprecated: use decode
    @Deprecated
    private ParseNode toParseNode(byte[] entry) {

        byte[] entryCopy = new byte[]{entry[0], entry[1]};

        int b = (entry[0] & 0xff);
        int b2 = (entry[1] & 0xff);

        String hex = Utils.toHexString(entry);

        int full = Utils.bytesToInt(new byte[]{0, 0, entry[0], entry[1]});

        if (b == 0) {

            //ParseNode zeroBlock = new ParseNode("0", "0");
            //if((i/2) != (dataED.length/2) - 1) {
            //    stack.push(zeroBlock);
            //}
            return null;

        } else if (b == 127) {
            //0x7f pointer

            ParseNode special7f = new ParseNode("" + b2, "0x7f");
            special7f.data = entry;

            return special7f;

        } else if (!hex.startsWith("8")) {

            //jp letter
            entryCopy[0] = (byte) (entryCopy[0] + (byte) 0x80);

            short s = Utils.bytesToShort(entryCopy);
            Character c = reader.sjishort2char.get(s);

            String jp = "" + c;

            ParseNode leaf = new ParseNode(jp, "leaf");

            return leaf;

        } else if (hex.startsWith("8")) {
            //0x8* number

            ParseNode node = new ParseNode("" + (full - 0x8000), "node");
            return node;
        }

        return null;

    }

    public void textBlockTreeExtraction(TextBlock tb) {
        byte[] dataED = tb.huffmanTreeBytes;

        Stack<ParseNode> stack = new Stack<>();
        Queue<ParseNode> queue = new LinkedList<>();

        boolean print = true;

        for (int i = 0; i < dataED.length; i += 2) {
            //0x80 is left
            byte[] entry = new byte[]{dataED[i + 1], dataED[i]};
            byte[] entryCopy = new byte[]{dataED[i + 1], dataED[i]};

            String hex = Utils.toHexString(entry);

            String jp = "";
            String status = "";

            int b = (entry[0] & 0xff);
            int b2 = (entry[1] & 0xff);

            int full = Utils.bytesToInt(new byte[]{0, 0, entry[0], entry[1]});

            //127 = 0x80
            if (b == 0) {

                //ParseNode zeroBlock = new ParseNode("0", "0");
                //if((i/2) != (dataED.length/2) - 1) {
                //    stack.push(zeroBlock);
                //}
            } else if (b == 127) {
                //0x7f pointer

                status = "special 0x7f";

                ParseNode special7f = new ParseNode("" + b2, "0x7f");
                special7f.data = entry;
                stack.push(special7f);

                queue.add(special7f);

            } else if (!hex.startsWith("8")) {

                //jp letter
                entryCopy[0] = (byte) (entryCopy[0] + (byte) 0x80);

                short s = Utils.bytesToShort(entryCopy);
                Character c = reader.sjishort2char.get(s);

                jp = "" + c;

                ParseNode leaf = new ParseNode(jp, "leaf");
                leaf.data = entry;
                stack.push(leaf);

                queue.add(leaf);

            } else if (hex.startsWith("8")) {
                //0x8* pointer

                status = "node " + (full - 0x8000);

                ParseNode node = new ParseNode("" + (full - 0x8000), "node");

                //for(int j = 0; j < 2 && !stack.isEmpty();j++) {
                //    ParseNode child = stack.pop();
                //    node.getChildren().add(child);
                //}
                for (int j = 0; j < 2 && !queue.isEmpty(); j++) {
                    ParseNode child = queue.remove();
                    node.getChildren().add(child);
                }

                if (!node.hasChildren()) {
                    throw new RuntimeException("no children?");
                }

                stack.push(node);

                queue.add(node);

                /*
                if(stack.size() >= 2) {
                    ParseNode child1 = stack.pop();
                    ParseNode child2 = stack.pop();
                    
                    node.getChildren().add(child1);
                    node.getChildren().add(child2);
                    stack.push(node);
                } else {
                    
                    //stack.push(node);
                    
                    if(print) {
                        System.out.println("\t\t ERROR: stack.size:" + stack.size());
                    }
                    int a = 0;
                }
                 */
            } else {
                //no other
                throw new RuntimeException("unknown escape sequence");
            }

            if (print) {
                System.out.println("\t" + (i / 2) + "/" + (dataED.length / 2) + ": " + hex + " " + jp + " " + status);
            }
        }

        if (print) {
            System.out.println("QUEUE based:");
            for (ParseNode root : queue) {
                System.out.println(root.toStringTree());
            }
        }

        tb.stack = stack;
        tb.queue = queue;
    }

    public void textBlockTreeExtractionV2(TextBlock tb) {
        byte[] dataED = tb.huffmanTreeBytes;

        ParseNode root = new ParseNode("root", "root");

        Queue<ParseNode> q = new LinkedList<>();
        q.add(root);

        //for (int i = 1; i < tree.length; i++) {
        //for (int i = 0; i < dataED.length; i += 2) {
        for (int i = dataED.length - 2; i >= 0; i -= 2) {

            byte[] entry = new byte[]{dataED[i + 1], dataED[i]};

            ParseNode pn = toParseNode(entry);

            int index = (((dataED.length - 2) - i) / 2);
            //int index = (i / 2) + 1;

            System.out.println(index + " " + pn);

            if (pn != null && pn.getLabel().equals("node")) {
                int number = Integer.parseInt(pn.getCoveredText());
                //System.out.println("\tleft child: " + ((number * 2) + 2));
                //System.out.println("\tright child: " + ((number * 2) + 3));
            }

            /*
            ParseNode node = q.peek();
            
            if(node == null) {
                continue;
            }
            
            if (node.left == null) {
                
                //node.left = new TreeNode(tree[i]);
                node.setLeft(pn);
                
                //if (tree[i] != null) q.add(node.left);
                if(node.left != null && !node.left.getLabel().equals("leaf"))
                    q.add(node.left);

            } else if (node.right == null) {

                //node.right = new TreeNode(tree[i]);
                node.setRight(pn);
                
                //if (tree[i] != null) q.add(node.right);
                if(node.right != null && !node.right.getLabel().equals("leaf"))
                    q.add(node.right);
                
                //q.remove();
                q.remove();
            }
             */
        }

        System.out.println(root.toStringTree());

        tb.root = root;
    }

    public String decode(String bits, byte[] huffmanTreeBytes) {
        boolean print = false;
        
        StringBuilder sb = new StringBuilder();

        int lastNodeIndex = huffmanTreeBytes.length - 4;
        byte[] lastNode2Bytes = Arrays.copyOfRange(huffmanTreeBytes, lastNodeIndex, lastNodeIndex + 2);
        int lastNode = Utils.bytesToInt(new byte[]{0, 0, lastNode2Bytes[1], lastNode2Bytes[0]});
        int lastNodeNumber = lastNode - 0x8000;

        int root = lastNodeNumber + 1;
        
        int offsetA = 0;
        //to get the second tree part
        int offsetB = (int) ((huffmanTreeBytes.length + 2) / 2) - 2;
        
        //======================================================================
        
        int bitIndex = 0;
        int curNumber = root;
        
        if(print) {
            System.out.println("start with: " + curNumber);
        }
        
        while (true) {
            
            if(bitIndex >= bits.length()) {
                break;
            }
            
            int offset = 0;
            //bit decide what offset is used
            if(bits.charAt(bitIndex) == '0') {
               offset = offsetA;
            } else {
               offset = offsetB;
            }
            
            if(print) {
                System.out.println("bit[" + bitIndex + "] = " + bits.charAt(bitIndex) + " => offset: " + offset);
            }
            
            bitIndex++;
            
            int index = offset + curNumber * 2; //offset + shift left (r3 or r6 is used in assembler code)

            byte[] nodeBytes = Arrays.copyOfRange(huffmanTreeBytes, index, index + 2);
            byte[] swap = new byte[]{nodeBytes[1], nodeBytes[0]};
            String hex = Utils.bytesToHex(swap);
            
            if(print) {
                System.out.println("\twill jump to " + hex);
            }
            
            if (hex.startsWith("8")) {

                curNumber = Utils.bytesToInt(new byte[]{0, 0, nodeBytes[1], nodeBytes[0]}) - 0x8000;

            } else if (hex.startsWith("7")) { 
            
                if(print)
                    System.out.println("\t7f Leaf: " + hex);
                
                if(hex.equals("7f02")) {
                    sb.append("\n");
                } else {
                    sb.append("{"+hex+"}");
                }
                
                curNumber = root;
                
            } else {
                swap[0] = (byte) (swap[0] + (byte) 0x80);
                short s = Utils.bytesToShort(swap);
                Character c = reader.sjishort2char.get(s);
                sb.append(c);
                
                if(print)
                    System.out.println("\tLeaf: " + hex + " " + c);
                
                curNumber = root;
            }
        }
        
        
        return sb.toString();
    }

    public void textBlockAnalysis() {
        boolean skipSameSize = true;
        boolean printMore = false;
        boolean inDeep = false;
        boolean printMoreOfMatched = false;
        boolean search = false;

        int lastSize = 0;

        List<TextBlock> matched = new ArrayList<>();

        for (TextBlock tb : textBlocks) {

            if (skipSameSize && lastSize == tb.huffmanTreeBytes.length) {
                continue;
            }

            //int diff = tb.subBlock.size - tb.a;
            //diffs.add(diff);
            System.out.println(tb);

            if (printMore) {
                System.out.println(Utils.toHexDump(tb.subBlock.data, 24, true, false, null));
                System.out.println("header to c-e:" + Utils.toHexString(tb.dataHeaderToCE));
                System.out.println("c-e:" + Utils.toHexString(tb.huffmanCode));
                System.out.println("e1:" + tb.e1);
                System.out.println("e2:" + tb.e2);
                System.out.println("e3:" + tb.e3);
                System.out.println("e(+10)-d:" + Utils.toHexString(tb.huffmanTreeBytes));
                System.out.println("d-a:" + Utils.toHexString(tb.dataDA));
                System.out.println("at a:" + Utils.toHexString(tb.dataAtA));
            }

            //for (int i = 0; i < tb.dataCE.length; i++) {
            //    dataStat.add((int) tb.dataCE[i] & 0xff);
            //}
            //works (they are offsets)
            if (tb.c > tb.subBlock.data.length
                    || tb.d > tb.subBlock.data.length
                    || tb.e > tb.subBlock.data.length) {
                throw new RuntimeException("offset are bigger than data len");
            }

            //it always ends with 0x8* ** (a branch)
            if (!Utils.bytesToHex(new byte[]{tb.huffmanTreeBytes[tb.huffmanTreeBytes.length - 3]}).startsWith("8")) {
                System.out.println("e(+10)-d:" + Utils.toHexString(tb.huffmanTreeBytes));
                throw new RuntimeException("no branch at the end");
            }

            List<Integer> p = null;
            if (search) {
                if (!p.isEmpty()) {
                    matched.add(tb);
                }
            }

            //works (c < e)
            if (tb.c >= tb.e) {
                System.out.println(Utils.toHexDump(tb.subBlock.data, 8, true, false, null));

                throw new RuntimeException("c >= e");
            }

            //works: it is always the same
            if (tb.a != tb.atA) {
                throw new RuntimeException("tb.a != tb.atA");
            }

            //c < e < d
            if (!(tb.d == 0 || tb.c < tb.e && tb.e < tb.d)) {
                throw new RuntimeException("not c < e < d (when d != 0)");
            }

            //works
            if (!(tb.e1 < tb.e2)) {
                throw new RuntimeException("not tb.e1 < tb.e2");
            }

            //works it is in fact the end
            if (!(tb.subBlock.data.length == tb.a + 8 + tb.atAnext * 8)) {
                throw new RuntimeException("is not the end: tb.subBlock.data.length == tb.a + 8 + tb.atAnext * 8)");
            }

            /*
            //never 0
            if (tb.c == 0) {
                cIs0++;
            }

            //191 times its zero
            if (tb.d == 0) {
                dIs0++;
            }

            //never 0
            if (tb.e == 0) {
                eIs0++;
            }
             */
            //always a mulitple of 4
            if (!(tb.huffmanCode.length % 4 == 0)) {
                throw new RuntimeException("tb.dataCE.length % 4 == 0");
            }

            lastSize = tb.huffmanTreeBytes.length;

            int a = 0;
        }

        System.out.println(matched.size() + " matched");
        for (TextBlock tb : matched) {
            if (printMoreOfMatched) {
                System.out.println("MATCH found:");
                System.out.println(Utils.toHexDump(tb.subBlock.data, 24, true, false, null));
                System.out.println("header to c-e:" + Utils.toHexString(tb.dataHeaderToCE));
                System.out.println("c-e:" + Utils.toHexString(tb.huffmanCode));
                System.out.println("e1:" + tb.e1);
                System.out.println("e2:" + tb.e2);
                System.out.println("e3:" + tb.e3);
                System.out.println("e(+10)-d:" + Utils.toHexString(tb.huffmanTreeBytes));
                System.out.println("d-a:" + Utils.toHexString(tb.dataDA));
                System.out.println("at a:" + Utils.toHexString(tb.dataAtA));
            }
        }

        int a = 0;
    }

    //==========================================================================
    //getter and helper
    public static String getTypeName(int type) {
        if (imageTypes.contains(type)) {
            return "Image";
        }
        if (qqesTypes.contains(type)) {
            return "QQES";
        }
        if (dq4TextTypes.contains(type)) {
            return "Text";
        }
        return "Unknown";
    }

    public List<StarZerosSubBlock> getStarZerosSubBlocks() {
        List<StarZerosSubBlock> l = new ArrayList<>();
        for (StarZeros sz : starZerosList) {
            for (StarZerosSubBlock sb : sz.starZerosBlocks) {
                l.add(sb);
            }
        }
        return l;
    }

    public Map<Integer, List<StarZerosSubBlock>> getType2StarZerosSubBlocksMap() {
        Map<Integer, List<StarZerosSubBlock>> m = new HashMap<>();
        for (StarZeros sz : starZerosList) {
            for (StarZerosSubBlock sb : sz.starZerosBlocks) {
                m.computeIfAbsent(sb.type, ss -> new ArrayList<>()).add(sb);
            }
        }
        return m;
    }

    public List<StarZerosSubBlock> getStarZerosSubBlocks(int type) {
        List<StarZerosSubBlock> l = new ArrayList<>();
        for (StarZeros sz : starZerosList) {
            for (StarZerosSubBlock sb : sz.starZerosBlocks) {
                if (sb.type == type) {
                    l.add(sb);
                }
            }
        }
        return l;
    }

    public List<StarZerosSubBlock> getStarZerosSubBlocksCompressed() {
        List<StarZerosSubBlock> l = new ArrayList<>();
        for (StarZeros sz : starZerosList) {
            for (StarZerosSubBlock sb : sz.starZerosBlocks) {
                if (sb.compressed) {
                    l.add(sb);
                }
            }
        }
        l.sort((a, b) -> {
            return Integer.compare(a.size, b.size);
        });
        return l;
    }

    public StarZerosSubBlock getSubBlock(int blockIndex, int subBlockIndex) {
        for (StarZeros sz : starZerosList) {
            if (sz.blockIndex == blockIndex) {
                for (StarZerosSubBlock sb : sz.starZerosBlocks) {
                    if (sb.blockIndex == subBlockIndex) {
                        return sb;
                    }
                }
            }
        }
        return null;
    }

    public TextBlock getTextBlock(int blockIndex, int subBlockIndex) {
        StarZerosSubBlock sb = getSubBlock(blockIndex, subBlockIndex);
        for (TextBlock tb : textBlocks) {
            if (tb.subBlock == sb) {
                return tb;
            }
        }
        return null;
    }

    public void sortBySizeCompressed(List<StarZerosSubBlock> l) {
        l.sort((a, b) -> {

            int cmp = Boolean.compare(b.compressed, a.compressed);

            if (cmp == 0) {
                cmp = Integer.compare(a.size, b.size);
            }

            return cmp;
        });
    }

    public List<StarZerosSubBlock> distinct(List<StarZerosSubBlock> l) {
        List<StarZerosSubBlock> distinct = new ArrayList<>();
        Set<Integer> hashes = new HashSet<>();
        for (StarZerosSubBlock sb : l) {
            int hash = Arrays.hashCode(sb.data);
            if (!hashes.contains(hash)) {
                distinct.add(sb);
                hashes.add(hash);
            }
        }
        return distinct;
    }

    public List<H60010108> getH60010108List() {
        return h60010108List;
    }

    //==========================================================================
    //write
    //call this before write() method
    //updates the writeThisBlock
    public void updateBlocks() {
        try {
            for (HBDBlock hbdBlock : this.blocks) {
                hbdBlock.write();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(File target) {
        System.out.println("write to " + target);
        try (FileOutputStream fos = new FileOutputStream(target)) {

            //all blocks (H60010108 or StarZeros)
            for (HBDBlock hbdBlock : this.blocks) {
                fos.write(hbdBlock.writeThisBlock);
            }

            byte[] trailingZeros = new byte[numberOfTrailingZeros];
            fos.write(trailingZeros);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
