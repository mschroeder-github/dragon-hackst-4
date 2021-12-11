package net.markus.projects.dh4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.markus.projects.dh4.data.H60010108;
import net.markus.projects.dh4.data.HBDBlock;
import net.markus.projects.dh4.data.HuffmanChar;
import net.markus.projects.dh4.data.HuffmanCode;
import net.markus.projects.dh4.data.ParseNode;
import net.markus.projects.dh4.data.StarZeros;
import net.markus.projects.dh4.data.StarZerosSubBlock;
import net.markus.projects.dh4.data.TextBlock;
import net.markus.projects.dh4.data.VeryFirstBlock;
import net.markus.projects.dh4.util.MinAvgMaxSdDouble;
import net.markus.projects.dh4.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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

    //========================================================
    //text block
    
    //extracts text blocks and also call tree extraction
    public void textBlockExtraction(List<Integer> types) {
        textBlocks = new ArrayList<>();

        for (Integer type : types) {
            for (StarZerosSubBlock sb : getStarZerosSubBlocks(type)) {
                
                TextBlock tb = new TextBlock();
                tb.subBlock = sb;

                //tb.dataSize1 = Arrays.copyOfRange(sb.data, 8, 8+tb.a);
                tb.endOffset = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 0, 0 + 4));
                tb.id = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 4, 4 + 4));
                tb.huffmanCodeStart = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 8, 8 + 4)); //header size = 4 * 6 bytes
                tb.huffmanTreeBytesEnd = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 12, 12 + 4));
                tb.huffmanCodeEnd = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 16, 16 + 4));
                tb.dataHeaderToHuffmanCodeStart = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, 20, 20 + 4));

                tb.header = Arrays.copyOfRange(sb.data, 0, 24);

                //if c is larger than 24 this can happen
                tb.dataHeaderToHuffmanCode = Arrays.copyOfRange(sb.data, 24, tb.huffmanCodeStart);

                //copy
                tb.huffmanCode = Arrays.copyOfRange(sb.data, tb.huffmanCodeStart, tb.huffmanCodeEnd);

                //if d is zero there is no extra range, so use a for it
                int internalD = tb.huffmanTreeBytesEnd;
                
                if (internalD == 0) {
                    internalD = tb.endOffset;
                }

                if (internalD != 0) {
                    //two ints and a short = +10
                    tb.huffmanTreeBytes = Arrays.copyOfRange(sb.data, tb.huffmanCodeEnd + 10, internalD);
                }
                if (internalD != 0) {
                    tb.dataDA = Arrays.copyOfRange(sb.data, internalD, tb.endOffset);
                }

                //the two (or three) ints at e and e+4
                tb.huffmanTreeBytesStart = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, tb.huffmanCodeEnd, tb.huffmanCodeEnd + 4));
                tb.huffmanTreeBytesMiddle = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, tb.huffmanCodeEnd + 4, tb.huffmanCodeEnd + 8));
                tb.numberOfNodes = Utils.bytesToShortLE(Arrays.copyOfRange(sb.data, tb.huffmanCodeEnd + 8, tb.huffmanCodeEnd + 10));

                //at the end
                tb.atEndOffset = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, tb.endOffset, tb.endOffset + 4));
                tb.numberOfDataEndBlocks = Utils.bytesToIntLE(Arrays.copyOfRange(sb.data, tb.endOffset + 4, tb.endOffset + 8));

                //atAnext is a number and we have to read number * 8 bytes
                tb.dataEnd = new byte[tb.numberOfDataEndBlocks * 8];
                for (int i = 0; i < tb.dataEnd.length; i++) {
                    //+8 : jump over atA (int) and atAnext (int)
                    tb.dataEnd[i] = tb.subBlock.data[tb.endOffset + 8 + i];
                }
                
                tb.root = parseTree(tb.huffmanTreeBytes);

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

    //make file so translators can translate
    public void translationPreparation(File translationFolder) throws IOException {
        translationFolder.mkdirs();
        
        Map<String, List<TextBlock>> id2tb = getTextBlockIDMap();
        System.out.println(id2tb.size() + " unique textblocks found");
        
        
        for (Entry<String, List<TextBlock>> e : id2tb.entrySet()) {
            String idHex = e.getKey();
            
            

            //JSONArray textBlocksJsonArray = new JSONArray();
            //for(TextBlock tb : e.getValue()) {
            //    textBlocksJsonArray.put(tb.subBlock.getPath());
            //}
            
            TextBlock tb = e.getValue().get(0);
            
            HuffmanCode code = decode(getBits(tb.huffmanCode), tb.root);
            
            //cut the code in segments
            List<HuffmanCode> segments = new ArrayList<>();
            HuffmanCode currentSegment = new HuffmanCode();
            for (HuffmanChar hc : code) {

                currentSegment.add(hc);

                if (hc.isControlZero()) {
                    segments.add(currentSegment);
                    
                    int firstByteIndex = currentSegment.get(0).getByteIndex();
                    //add textblock header length, usually 24 (directly after header starts huffman code)
                    firstByteIndex += tb.huffmanCodeStart;
                    
                    //if(tb.huffmanCodeStart != 24) {
                        //happens
                    //}
                    
                    String offsetHex = Utils.bytesToHex(Utils.intToByteArray(firstByteIndex)).toUpperCase();
                    if(!offsetHex.startsWith("0000")) {
                        throw new RuntimeException("the offsetHex is greater then a short: " + offsetHex);
                    }
                    offsetHex = offsetHex.substring(4, 8);
                    
                    currentSegment.setByteDiffInHex(offsetHex);
                    
                    currentSegment.setText(currentSegment.calculateText());
                    
                    currentSegment = new HuffmanCode();
                }
            }
            
            //segments that are just the remaining without a {0000}
            //TODO maybe better check
            segments.removeIf(seg -> seg.size() <= 2);
            
            //ダミー = dummy
            segments.removeIf(seg -> seg.getText().startsWith("ダミー"));
            
            if(!segments.isEmpty()) {
                //so that the dialogs are sorted like they appear
                Collections.reverse(segments);
                
                File translationFile = new File(translationFolder, idHex + ".csv");
                CSVPrinter csvp = CSVFormat.DEFAULT.print(translationFile, StandardCharsets.UTF_8);

                for(HuffmanCode segment : segments) {
                    csvp.printRecord(
                            "", //translation
                            segment.getText(), //original

                            //"ID" + idHex, //id of the textblock
                            "0x" + segment.getByteDiffInHex() //byte diff
                            //textBlocksJsonArray.toString() //so that we know what textblocks we have to update
                    );
                }

                csvp.close();
            }
            
        }//for each unique textblock id 
    }
    
    //just analysis
    @Deprecated
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
                System.out.println("header to c-e:" + Utils.toHexString(tb.dataHeaderToHuffmanCode));
                System.out.println("c-e:" + Utils.toHexString(tb.huffmanCode));
                System.out.println("e1:" + tb.huffmanTreeBytesStart);
                System.out.println("e2:" + tb.huffmanTreeBytesMiddle);
                System.out.println("e3:" + tb.numberOfNodes);
                System.out.println("e(+10)-d:" + Utils.toHexString(tb.huffmanTreeBytes));
                System.out.println("d-a:" + Utils.toHexString(tb.dataDA));
                System.out.println("at a:" + Utils.toHexString(tb.dataEnd));
            }

            //for (int i = 0; i < tb.dataCE.length; i++) {
            //    dataStat.add((int) tb.dataCE[i] & 0xff);
            //}
            //works (they are offsets)
            if (tb.huffmanCodeStart > tb.subBlock.data.length
                    || tb.huffmanTreeBytesEnd > tb.subBlock.data.length
                    || tb.huffmanCodeEnd > tb.subBlock.data.length) {
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
            if (tb.huffmanCodeStart >= tb.huffmanCodeEnd) {
                System.out.println(Utils.toHexDump(tb.subBlock.data, 8, true, false, null));

                throw new RuntimeException("c >= e");
            }

            //works: it is always the same
            if (tb.endOffset != tb.atEndOffset) {
                throw new RuntimeException("tb.a != tb.atA");
            }

            //c < e < d
            if (!(tb.huffmanTreeBytesEnd == 0 || tb.huffmanCodeStart < tb.huffmanCodeEnd && tb.huffmanCodeEnd < tb.huffmanTreeBytesEnd)) {
                throw new RuntimeException("not c < e < d (when d != 0)");
            }

            //works
            if (!(tb.huffmanTreeBytesStart < tb.huffmanTreeBytesMiddle)) {
                throw new RuntimeException("not tb.e1 < tb.e2");
            }

            //works it is in fact the end
            if (!(tb.subBlock.data.length == tb.endOffset + 8 + tb.numberOfDataEndBlocks * 8)) {
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
                System.out.println("header to c-e:" + Utils.toHexString(tb.dataHeaderToHuffmanCode));
                System.out.println("c-e:" + Utils.toHexString(tb.huffmanCode));
                System.out.println("e1:" + tb.huffmanTreeBytesStart);
                System.out.println("e2:" + tb.huffmanTreeBytesMiddle);
                System.out.println("e3:" + tb.numberOfNodes);
                System.out.println("e(+10)-d:" + Utils.toHexString(tb.huffmanTreeBytes));
                System.out.println("d-a:" + Utils.toHexString(tb.dataDA));
                System.out.println("at a:" + Utils.toHexString(tb.dataEnd));
            }
        }

        int a = 0;
    }
    
    //first version with byte
    //deprecated: use decode with ParseNode huffmanTreeRoot
    @Deprecated
    public String decodeToString(String bits, byte[] huffmanTreeBytes) {
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
    
    //second version with tree, use parseTree method
    public String decodeToString(String bits, ParseNode huffmanTreeRoot) {
        StringBuilder sb = new StringBuilder();
        
        ParseNode node = huffmanTreeRoot;
        for(int i = 0; i < bits.length(); i++) {
            
            char c = bits.charAt(i);
            
            if(c == '0') {
                node = node.getChild(0);
            } else {
                node = node.getChild(1);
            }
            
            if(node.getLabel().equals("literal")) {
                sb.append(node.getCoveredText());
                
                node = huffmanTreeRoot;
                
            } else if(node.getLabel().equals("control")) {
                if(node.getCoveredText().equals("7f02")) {
                    sb.append("\n");
                } else {
                    sb.append("{"+node.getCoveredText()+"}");
                }
                
                node = huffmanTreeRoot;
            }
        }
        
        return sb.toString();
    }
    
    public String getBits(byte[] huffmanCode) {
        String allbits = "";
        for(byte b : huffmanCode) {
            //for each byte take the bits
            String bits = Utils.toBits(b);
            //reverse it
            bits = Utils.reverse(bits);
            //add it to long bit sequence
            allbits += bits;
        }
        return allbits;
    }
    
    public HuffmanCode decode(String bits, ParseNode huffmanTreeRoot) {
        HuffmanCode code = new HuffmanCode();
        code.setHuffmanTreeRoot(huffmanTreeRoot);
        
        StringBuilder sb = new StringBuilder();
        
        ParseNode node = huffmanTreeRoot;
        String bitBuffer = "";
        
        int startBit = 0;
        
        for(int i = 0; i < bits.length(); i++) {
            
            char bit = bits.charAt(i);
            
            if(bit == '0') {
                node = node.getChild(0);
            } else {
                node = node.getChild(1);
            }
            
            bitBuffer += bit;
            
            HuffmanChar huffmanChar = null;
            
            if(node.getLabel().equals("literal")) {
                sb.append(node.getCoveredText());
                
                huffmanChar = new HuffmanChar(node.data, node.getCoveredText());
                
            } else if(node.getLabel().equals("control")) {
                
                if(node.getCoveredText().equals("7f02")) {
                    sb.append("\n");
                } else {
                    sb.append("{"+node.getCoveredText()+"}");
                }
                
                //covered text contains the hex value
                huffmanChar = new HuffmanChar(node.data, node.getCoveredText());
            }
            
            //if found
            if(huffmanChar != null) {
                huffmanChar.setBits(bitBuffer);
                huffmanChar.setStartBit(startBit);
                huffmanChar.setEndBit(i);
                huffmanChar.setByteIndex(startBit / 8);
                huffmanChar.setStartBitInByte(startBit % 8);
                
                code.add(huffmanChar);
                
                node = huffmanTreeRoot;
                bitBuffer = "";
                startBit = i + 1;
            }
        }
        
        code.setText(sb.toString());
        
        return code;
    }
    
    public Map<String, String> getCharacterToBitsMap(ParseNode huffmanTreeRoot) {
        Map<String, String> map = new HashMap<>();
        
        getCharacterToBitsMapRecursive("0", huffmanTreeRoot.getChild(0), map);
        getCharacterToBitsMapRecursive("1", huffmanTreeRoot.getChild(1), map);
        
        return map;
    }
    
    private void getCharacterToBitsMapRecursive(String bits, ParseNode parent, Map<String, String> map) {
        if(parent.getLabel().equals("literal") || parent.getLabel().equals("control")) {
            map.put(parent.getCoveredText(), bits);
        } else if(parent.getLabel().equals("node")) {
            getCharacterToBitsMapRecursive(bits + "0", parent.getChild(0), map);
            getCharacterToBitsMapRecursive(bits + "1", parent.getChild(1), map);
        }
    }
    
    public byte[] encode(HuffmanCode code) {
        
        Map<String, String> char2str = getCharacterToBitsMap(code.getHuffmanTreeRoot());
        
        StringBuilder bitsSB = new StringBuilder();
        for(HuffmanChar hc : code) {
            String bits = char2str.get(hc.getLetter());
            
            if(bits == null) {
                new RuntimeException("could not find bit sequence for " + hc + " at index " + code.indexOf(hc));
            }
            
            bitsSB.append(bits);
        }
        
        String allBits = bitsSB.toString();
        
        int len = allBits.length() / 8 + (allBits.length() % 8 == 0 ? 0 : 1);
        
        //multiple of 4
        while(len % 4 != 0) {
            len++;
        }
        
        byte[] data = new byte[len];
        
        for(int i = 0; i < allBits.length(); i += 8) {
            String bits = allBits.substring(i, Math.min(allBits.length(), i+8));
            
            while(bits.length() != 8) {
                bits += "0";
            }
            
            bits = Utils.reverse(bits);
            int v = Utils.bitsToIntLE(bits);
            byte b = (byte) v;
            
            data[i / 8] = b;
        }
        
        return data;
    }
    
    public ParseNode createHuffmanTree(Map<String, Integer> jpchar2freqMap) {
        List<ParseNode> list = new ArrayList<>();
        
        for(String jpLetter : jpchar2freqMap.keySet()) {
            
            ParseNode pn;
            if(jpLetter.length() == 4) {
                //control character
                pn = new ParseNode(jpLetter, "control");
                
            } else {
                /* deprecated: not necessary, is already jp letter
                //we have to convert ascii to japanese character equivalent
                String jpLetter = PsxJisReader.ascii2jp.get(letter);
                if(jpLetter == null) {
                    throw new RuntimeException("letter is not supported: " + letter);
                }
                
                //just a check for later
                Byte[] data = reader.char2sjis.get(jpLetter.charAt(0));
                if(data == null) {
                    throw new RuntimeException("japanese character Shift-JIS bytes not found: " + jpLetter);
                }
                */
                
                pn = new ParseNode(jpLetter, "literal");
            }
            
            pn.freq = jpchar2freqMap.get(jpLetter);
            
            list.add(pn);
        }
        
        while(list.size() > 1) {
            
            list.sort((a,b) -> Integer.compare(a.freq, b.freq));
            
            ParseNode a = list.remove(0);
            ParseNode b = list.remove(0);
            
            ParseNode node = new ParseNode("", "node");
            node.getChildren().add(a);
            node.getChildren().add(b);
            node.freq = a.freq + b.freq;
            
            list.add(node);
        }
        
        //important to have the correct node count
        list.get(0).setCoveredText("root");
        list.get(0).setLabel("root");
        
        return list.get(0);
    }
    
    //parses bytes to tree
    public ParseNode parseTree(byte[] huffmanTreeBytes) {
        
        int lastNodeIndex = huffmanTreeBytes.length - 4;
        byte[] lastNode2Bytes = Arrays.copyOfRange(huffmanTreeBytes, lastNodeIndex, lastNodeIndex + 2);
        int lastNode = Utils.bytesToInt(new byte[]{0, 0, lastNode2Bytes[1], lastNode2Bytes[0]});
        int lastNodeNumber = lastNode - 0x8000;

        int rootNumber = lastNodeNumber + 1;
        
        int offsetA = 0;
        //to get the second tree part
        int offsetB = (int) ((huffmanTreeBytes.length + 2) / 2) - 2;
        
        ParseNode root = new ParseNode("root", "root");
        
        //======================================================================
        
        parseTree(false, rootNumber, root, huffmanTreeBytes);
        parseTree(true, rootNumber, root, huffmanTreeBytes);
        
        return root;
    }

    private void parseTree(boolean bit, int curNumber, ParseNode parent, byte[] huffmanTreeBytes) {
        
        int offsetA = 0;
        //to get the second tree part
        int offsetB = (int) ((huffmanTreeBytes.length + 2) / 2) - 2;
        
        int offset = 0;
        //bit decide what offset is used
        if(!bit) {
           offset = offsetA;
        } else {
           offset = offsetB;
        }
        
        int index = offset + curNumber * 2;
        
        byte[] nodeBytes = Arrays.copyOfRange(huffmanTreeBytes, index, index + 2);
        byte[] swap = new byte[]{nodeBytes[1], nodeBytes[0]};
        String hex = Utils.bytesToHex(swap);

        if (hex.startsWith("8")) {

            int number = Utils.bytesToInt(new byte[]{0, 0, nodeBytes[1], nodeBytes[0]}) - 0x8000;
            
            ParseNode newParent = new ParseNode(hex + " [" + number + "] @" + index, "node");
            newParent.data = nodeBytes;
            
            parent.getChildren().add(newParent);
            
            //recursion
            parseTree(false, number, newParent, huffmanTreeBytes);
            parseTree(true, number, newParent, huffmanTreeBytes);

        } else if (hex.startsWith("7") || hex.equals("0000")) { 

            ParseNode control = new ParseNode(hex, "control");
            control.data = nodeBytes;
            parent.getChildren().add(control);

        } else {
            swap[0] = (byte) (swap[0] + (byte) 0x80);
            short s = Utils.bytesToShort(swap);
            Character c = reader.sjishort2char.get(s);
            String jp = "" + c;

            ParseNode literal = new ParseNode(jp, "literal");
            literal.data = nodeBytes;
            parent.getChildren().add(literal);
        }
    }
    
    public byte[] toHuffmanTreeBytes(ParseNode root) {
        List<ParseNode> nodes = root.descendantsWithoutThis();
        
        int numberOfNodes = 0;
        
        //check if complete
        for(ParseNode pn : nodes) {
            if(pn.getLabel().equals("node")) {
                
                numberOfNodes++;
                
                if(pn.getChildren().size() != 2) {
                    throw new RuntimeException(pn + " has not two children");
                }
            }
        }
        
        int arraySize = nodes.size() * 2 + 2; //+2 because 0000
        byte[] data = new byte[arraySize];
        
        //numberOfNodes = 107
        
        //105
        //toHuffmanTreeBytesRecursiveStep(false, numberOfNodes - 2, root.getChild(0), data);
        
        //106 = 6A
        toHuffmanTreeBytesRecursiveStep(new Counter(numberOfNodes), root, data);
        
        return data;
    }
    
    private void toHuffmanTreeBytesRecursiveStep(Counter counter, ParseNode node, byte[] data) {
        
        ParseNode right = node.getChild(0);
        ParseNode left = node.getChild(1);
        
        int offsetA = 0;
        //to get the second tree part
        int offsetB = (int) ((data.length + 2) / 2) - 2;
        
        int number = counter.value;
        
        byte[] nodeAsBytes;
        
        //===================================================================
        //left
        
        int index = offsetB + number * 2;
        
        if(left.getLabel().equals("node")) {
            
            //107 -> 106
            counter.dec();
            
            nodeAsBytes = toByteArray(left, counter.value);
            
        } else {
            //leaf
            nodeAsBytes = toByteArray(left, -1);
        }
        
        //write
        for(int i = 0; i < nodeAsBytes.length; i++) {
            data[index + i] = nodeAsBytes[i];
        }
        
        //System.out.println("left " + left + " is " + Utils.bytesToHex(nodeAsBytes) + " at " + index);
        
        if(left.getLabel().equals("node")) {
            toHuffmanTreeBytesRecursiveStep(counter, left, data);
        }
        
        //===============================================
        //right
        
        index = offsetA + number * 2;
        
        if(right.getLabel().equals("node")) {
            
            counter.dec();
            
            nodeAsBytes = toByteArray(right, counter.value);
            
        } else {
            //leaf
            nodeAsBytes = toByteArray(right, -1);
        }
        
        //write
        for(int i = 0; i < nodeAsBytes.length; i++) {
            data[index + i] = nodeAsBytes[i];
        }
        
        //System.out.println("right " + right + " is " + Utils.bytesToHex(nodeAsBytes)  + " at " + index);
        
        
        if(right.getLabel().equals("node")) {
            toHuffmanTreeBytesRecursiveStep(counter, right, data);
        }
        
    }
    
    private byte[] toByteArray(ParseNode pn, int number) {
        byte[] data = new byte[2];
        
        if(pn.getLabel().equals("node")) {
            
            byte[] numberData = Utils.intToByteArrayLE(number);
            
            data[0] = numberData[0];
            data[1] = (byte) (numberData[1] | 0x80);
            
        } else if(pn.getLabel().equals("literal")) {
            
            if(pn.getCoveredText().equals("null")) {
                return data;
            }
            
            Byte[] letterData = reader.char2sjis.get(pn.getCoveredText().charAt(0));
            
            if(letterData == null) {
                throw new RuntimeException(pn + " char not found");
            }
            
            data[1] = (byte) (letterData[0] - 0x80);
            data[0] = letterData[1] ;
            
        } else if(pn.getLabel().equals("control")) {
            
            data = Utils.reverse(Utils.hexStringToByteArray(pn.getCoveredText()));
            
        }
        
        return data;
    }

    //to have unique node numbers
    private class Counter {
        
        int value;

        public Counter(int value) {
            this.value = value;
        }
        
        public void dec() {
            value--;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
        
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

    public Map<String, List<TextBlock>> getTextBlockIDMap() {
        Map<String, List<TextBlock>> id2tb = new HashMap<>();
        for(TextBlock tb : textBlocks) {
            String hexId = Utils.bytesToHex(Utils.intToByteArray(tb.id)).toUpperCase();
            
            if(!hexId.startsWith("0000")) {
                throw new RuntimeException("the hexid is greater then a short: " + hexId);
            }
            hexId = hexId.substring(4, 8);
            id2tb.computeIfAbsent(hexId, bb -> new ArrayList<>()).add(tb);
        }
        return id2tb;
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

            //fill with zeros to get the exact same file size
            
            long originalLength = file.length();
            long currentLength = fos.getChannel().size();
            
            if(originalLength < currentLength) {
                throw new RuntimeException("should not happen");
            }
            
            long diffLength = originalLength - currentLength;
            byte[] trailingZeros = new byte[(int) diffLength];
            fos.write(trailingZeros);
            
            System.out.println("original had trailing zeros: " + numberOfTrailingZeros);
            System.out.println("patched has  trailing zeros: " + diffLength);
            
            //30265344 bytes / 2048 bytes = 14778 sectors

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
        System.out.println("original has size " + file.length());
        System.out.println("patched  has size " + target.length());
    }

}
