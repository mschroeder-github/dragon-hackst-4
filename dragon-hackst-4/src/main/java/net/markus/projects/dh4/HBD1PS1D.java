
package net.markus.projects.dh4;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import javax.imageio.ImageIO;
import net.markus.projects.dh4.data.H60010108;
import net.markus.projects.dh4.data.HBDBlock;
import net.markus.projects.dh4.data.StarZeros;
import net.markus.projects.dh4.data.StarZerosSubBlock;
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
    
    public static final byte[] qQES = new byte[] { (byte)'q', (byte)'Q', (byte)'E', (byte)'S' };
    public static final byte[] TIM = Utils.hexStringToByteArray("10000000");

    public HBD1PS1D(File file) throws IOException {
        this.file = file;
        
        System.out.println("read " + file.length() + " bytes in RAM...");
        data = FileUtils.readFileToByteArray(file);
        System.out.println("done");
        
        blocks = new ArrayList<>();
        starZerosList = new ArrayList<>();
        h60010108List = new ArrayList<>();
    }
    
    //in Utils now
    @Deprecated
    public List<Integer> find(byte[] pattern) {
        System.out.println("find " + Arrays.toString(pattern));
        
        //int hashCode = Arrays.hashCode(pattern);
        
        List<Integer> pos = new ArrayList<>();
        
        for(int i = 0; i < data.length - pattern.length; i++) {
            byte[] copy = Arrays.copyOfRange(data, i, i+pattern.length);
            
            //if(hashCode == Arrays.hashCode(copy)) {
            //   pos.add(i);
            //}
            if(Arrays.equals(copy, pattern)) {
                pos.add(i);
            }
        }
        
        System.out.println("find " + Arrays.toString(pattern) + " done");
        return pos;
    }
    
    @Deprecated
    public List<Integer> findHiragana(int minLen) {
        List<Integer> pos = new ArrayList<>();
        
        /*
        82 9e |    ぁ あ ぃ い ぅ う ぇ え ぉ お か が き ぎ く
        82 ae | ぐ け げ こ ご さ ざ し じ す ず せ ぜ そ ぞ た
        82 be | だ ち ぢ っ つ づ て で と ど な に ぬ ね の は
        82 ce | ば ぱ ひ び ぴ ふ ぶ ぷ へ べ ぺ ほ ぼ ぽ ま み
        82 de | む め も ゃ や ゅ ゆ ょ よ ら り る れ ろ ゎ わ
        82 ee | ゐ ゑ を ん
        */
        
        byte[] _82 = Utils.hexStringToByteArray("82");
        byte[] beginArray = Utils.hexStringToByteArray("a0"); //a
        byte[] endArray = Utils.hexStringToByteArray("f1"); //n
        
        int len = 0;
        for(int i = 0; i < data.length; i += 2) {
            if(data[i] == _82[0] && data[i+1] >= beginArray[0] && data[i+1] <= endArray[0]) {
                len++;
            } else {
                if(len >= minLen) {
                    pos.add(i - len*2);
                }
                len = 0;
            }
        }
        
        return pos;
    }

    //was wrong
    @Deprecated
    private void headerTest() throws IOException {
        
        for(int i = 0; i < 256; i++) {
            byte[] copy = Arrays.copyOfRange(data, i*4, i*4 + 4);
            
            System.out.println(i + ": " + Utils.bytesToIntLE(copy));
        }
        
        byte[] title = Arrays.copyOfRange(data, 256*4, 256*4 + 12);
        String str = new String(title);
        System.out.println(str);
        
        byte[] firstTim = Arrays.copyOfRange(data, 2048, 8192);
        FileUtils.writeByteArrayToFile(new File(file.getParentFile(), "out.TIM"), firstTim);
        
        int a = 0;
    }
    
    //maybe wrong
    @Deprecated 
    private BufferedImage readTIM(int offset) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        is.skip(offset);
        
        byte[] timMagicNumber = new byte[4];
        is.read(timMagicNumber);
        
        int version = is.read();
        is.skip(3);
        
        byte[] buffer = new byte[4];
        
        byte[] buffer2 = new byte[2];
        
        is.read(buffer);
        int l = Utils.bytesToIntLE(buffer);
        
        is.read(buffer2);
        int x = Utils.bytesToShortLE(buffer2);
        
        is.read(buffer2);
        int y = Utils.bytesToShortLE(buffer2);
        
        is.read(buffer2);
        int w = Utils.bytesToShortLE(buffer2);
        
        is.read(buffer2);
        int h = Utils.bytesToShortLE(buffer2);
        
        
        System.out.println("l: " + l + ", (" + x + "," + y + ") " + w + "x" + h);
        
        //5552
        //is.read(buffer);
        //int a = Utils.bytesToIntLE(buffer);
        
        
        //is.read(buffer);
        //int b = Utils.bytesToIntLE(buffer);
        
        //C8 2F begins here
        is.skip(8);
        
        //C8 = 200
        //2F = 47
        //200 * 47 = 9400 (makes no sense)
        //2FC8 = 12232
        
        //24 bits per pixel => 3 bytes per pixel
        
        //5553 / 3 => 1851 pixel
        
        //The PlayStation is also capable of handling data in 24-bit color (BPP = 11),
        //in which case the color samples are stored as 3-byte groups.
        //In the event that an image's width is an uneven number of pixels, 
        //the last byte is left as padding; the first pixel of a new row is always stored at the corresponding
        //first pixel of the frame buffer row. 
        //The color samples are stored in the following order: 
        
        for(int width = 2; width <= 1851; width++) {
            if(1851 % width == 0) {
                System.out.println(width + "x" + 1851/width);
            }
        }
        
        BufferedImage bi = new BufferedImage(617, 3, BufferedImage.TYPE_INT_RGB);
        int biW = bi.getWidth();
        int biH = bi.getHeight();
        
        
        int j = 0;
        for(int i = 0; i <= l; i += 3) {
            
            int r = is.read();
            int g = is.read();
            int b = is.read();
            
            int xx = j % biW;
            int yy = (int) j / biW;
            bi.setRGB(xx, yy, new Color(r, g, b).getRGB());
            
            System.out.println((j+1) + " at " + xx + "x" +  yy + " " + Arrays.asList(r,g,b));
            j++;
        }
        
        
        
        int b2 = is.read();
        System.out.println("last: " + b2);
        b2 = is.read();
        System.out.println("last: " + b2);
        
        
        
        
        return bi;
    }
    
    private BufferedImage toGrayscale(int w, int max) {
        if(max == -1) {
            max = data.length;
        }
        
        int h = (int) (max / w) + 1;
        
        System.out.println(w + "x" + h);
        
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        for(int i = 0; i < max; i++) {
            int v = (int) (data[i] & 0xff); //unsigned
            bi.setRGB(i % w, i / w, new Color(v, v, v).getRGB());
            
            if(i % 100000 == 0) {
                System.out.println((i / (float) data.length)*100 + "%");
            }
        }
        
        return bi;
    }
    
    //analysis at the beginning
    @Deprecated
    private void sameBlocks(int blockSize, int gtClusterSize) {
        
        Map<Integer, List<Integer>> m = new HashMap<>();
        
        for(int i = 0; i < data.length - blockSize; i += blockSize) {
            
            byte[] block = Arrays.copyOfRange(data, i, i + blockSize);
            
            boolean allSame = true;
            byte first = block[0];
            for(int j = 1; j < block.length; j++) {
                if(block[j] != first) {
                    allSame = false;
                    break;
                }
            }
            
            if(allSame)
                continue;
            
            List<Integer> l = m.computeIfAbsent(Arrays.hashCode(block), a -> new ArrayList<>());
            
            l.add(i);
            
            if(i % 1000000 == 0) {
                System.out.println((i / (float) data.length)*100 + "%");
            }
        }
        
        List<Entry<Integer, List<Integer>>> ll = m.entrySet().stream().filter(e -> e.getValue().size() > gtClusterSize).sorted((a,b) -> Integer.compare(b.getValue().size(), a.getValue().size())).collect(toList());
        System.out.println(ll.size() + " entries");
        
        //double check
        for(Entry<Integer, List<Integer>> e : ll) {
            int pos0 = e.getValue().get(0);
            byte[] b = Arrays.copyOfRange(data, pos0, pos0+blockSize);
            
            for(int i : e.getValue().toArray(new Integer[0])) {
                byte[] b2 = Arrays.copyOfRange(data, i, i+blockSize);
                if(!Arrays.equals(b, b2)) {
                    e.getValue().remove(i);
                }
            }
        }
        
        //move up until not equal
        for(Entry<Integer, List<Integer>> e : ll) {
            
            int offset = 0;
            int pos0 = e.getValue().get(0);
            
            boolean allSame = true;
            while(allSame) {
                offset--;
                
                byte[] b = Arrays.copyOfRange(data, pos0 + offset, pos0 + offset + blockSize);
                for(int i : e.getValue()) {
                    byte[] b2 = Arrays.copyOfRange(data, i + offset, i + offset + blockSize);
                    if(!Arrays.equals(b, b2)) {
                        allSame = false;
                        break;
                    }
                }
            }
            
            offset++;
            
            
            System.out.println(offset);
            
            for(int i = 0; i < e.getValue().size(); i++) {
                e.getValue().set(i, e.getValue().get(i) + offset);
            }
            
            //-431734076=[15659580, 16939580, 17216060, 28740156, 31855164, 40899132, 41153084, 48843324, 49103420, 52046396, 52478524, 52974140, 53191228, 53412412, 53705276, 53924412, 54485564, 54721084, 54944316, 55163452, 55736892, 55972412, 56207932, 56427068, 56648252, 57231932, 57467452, 57702972, 57858620, 58057276, 58384956, 58624572, 59189820, 59425340, 59644476, 59863612, 60043836, 60248636, 60389948, 60545596, 60777020, 61119036, 61399612, 61985340, 62229052, 62468668, 62687804, 62908988, 63072828, 63416892, 63560252, 64131644, 64344636, 64569916, 64811580, 65040956, 65221180, 65423932, 65641020, 66576956, 66896444, 67115580, 67258940, 68823612, 69026364, 69481020, 69755452, 70001212, 74811964, 74975804, 75156028, 75358780, 75500092, 75719228, 75940412, 76542524, 76780092, 77060668, 77400636, 77630012, 77859388, 80767548, 81037884, 84568636, 84822588, 89856572, 92971580, 93237820, 94007868, 94265916, 108145212, 108415548, 109855292, 110115388, 115206716, 115640892, 115894844, 120568380, 120844860]
            System.out.println(e);
        }
        
        
        ll.forEach(e -> System.out.println(e));
     
        
    }
    
    //not necessary anymore
    @Deprecated
    private void writeBlocks2048(int number, int startIndex) throws IOException {
        for(int i = startIndex; i < startIndex + number; i++) {
            
            byte[] block = Arrays.copyOfRange(data, i * 2048, i * 2048 + 2048);
            
            FileUtils.writeByteArrayToFile(new File(file.getParentFile(), "block-" + i + ".bin"), block);
        }
    }
    
    //use correctBlockExtraction
    @Deprecated
    private void extractFirstBlocks() throws IOException {
        
        File folder = new File(file.getParentFile(), "firstBlocks");
        folder.mkdir();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        FileUtils.writeByteArrayToFile(new File(folder, "blockfirst.bin"), Arrays.copyOfRange(data, 0, 2048));
        
        int len = 2048;
        int offset = 2048;
        int blockIndex = 0;
        
        boolean cont = true;
        while(baos.size() < 2048 * 12) {
            byte[] block = Arrays.copyOfRange(data, offset, offset + len);
            
            if(offset != 2048 && 
               block[0] == 1 && block[1] == 0 && block[2] == 0 && block[3] == 0) { //Block here
               
                //save previouse TIM block
                byte[] fullBlock = baos.toByteArray();
                
                int numberOf2048blocks = fullBlock[4] & 0xff;
                
                int size = Utils.bytesToIntLE(Arrays.copyOfRange(fullBlock, 8, 8+4));
                int unknown = Utils.bytesToIntLE(Arrays.copyOfRange(fullBlock, 12, 12+4)); //always zero
                int size2 = Utils.bytesToIntLE(Arrays.copyOfRange(fullBlock, 16, 16+4)); //always as size
                int size3 = Utils.bytesToIntLE(Arrays.copyOfRange(fullBlock, 20, 20+4));
                
                FileUtils.writeByteArrayToFile(new File(folder, "block-"+ Arrays.asList(blockIndex, numberOf2048blocks, size, unknown, size2, size3) +".bin"), fullBlock);
                
                //reset
                baos = new ByteArrayOutputStream();
                blockIndex++;
            }
            
            baos.write(block);
            
            offset += len;
        }
    }

    //extracts all blocks and puts them in lists for further analysis
    private void correctBlockExtraction() {
        
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
        
        blocks.add(vfb);
        veryFirstBlock = vfb;
        
        while(true) {
            byte[] block = Arrays.copyOfRange(data, offset, offset + len);
            
            //60010180 block
            if(Arrays.equals(Arrays.copyOfRange(block, 0, 4), Utils.hexStringToByteArray("60010180"))) {
                
                byte[] onlyHeader = Arrays.copyOfRange(block, 0, 32);
                
                H60010108 h60block = new H60010108();
                h60block.pos = offset;
                h60block.blockIndex = blockIndex;
                h60block.header = onlyHeader;
                h60block.full2048 = block;
                h60block.data = block;
                
                //in h60010180Extraction we extract the information
                
                h60010108List.add(h60block);
                blocks.add(h60block);
                
                offset += len;
                
            } else if(block[0] != 0 && Arrays.equals(Arrays.copyOfRange(block, 1, 4), Utils.hexStringToByteArray("000000"))) {
                
                //the * 00 00 00 header
                
                int numberOf2048blocks = Utils.bytesToIntLE(Arrays.copyOfRange(block, 4, 4+4));

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
        int i = data.length-1;
        for(; i > 0; i--) {
            if(data[i] != 0) {
                break;
            }
        }
        i++;
        
        System.out.println(blocks.size() + " blocks in total");
        System.out.println("1 very first block");
        System.out.println(starZerosList.size() + " star zeros blocks");
        System.out.println(h60010108List.size() + " h60010108 blocks");
        
        System.out.println("block count matches: " + ((1 + starZerosList.size() + h60010108List.size()) == blocks.size()));
        
        System.out.println("trailing zero starting at " + "0x" + Integer.toHexString(i));
        System.out.println("end found at " + "0x" + Integer.toHexString(offset));
    }
    
    public void h60010180Extraction() {
        
        for(H60010108 h60 : h60010108List) {
            
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
            int i = h60.data.length-1;
            for(; i >= 0; i--) {
                if((h60.data[i] & 0xff) != 0xff) {
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
    
    public void h60010180Analysis() {
        
        List<List<H60010108>> listOfparts = new ArrayList<>();
        List<H60010108> partList = new ArrayList<>();
        
        for(int i = 0; i < h60010108List.size(); i++) {
            
            H60010108 h60 = h60010108List.get(i);
            
            System.out.println(h60);
            
            if(h60.v12to16 == 0 && !partList.isEmpty()) {
                
                listOfparts.add(partList);
                
                partList = new ArrayList<>();
            }
            
            partList.add(h60);
        }
        
        if(!partList.isEmpty()) {
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
        
        for(StarZeros starZeros : starZerosList) {
            
            byte[] block = starZeros.full2048;
            
            starZeros.blocks = Utils.bytesToIntLE(Arrays.copyOfRange(block, 0, 0+4));
            starZeros.sectors = Utils.bytesToIntLE(Arrays.copyOfRange(block, 4, 4+4));
            
            //has to be int so that the reading works correctly
            //maybe this is a long = 8 byte
            starZeros.sizeTotal = Utils.bytesToIntLE(Arrays.copyOfRange(block, 8, 8+4));
            starZeros.zeros = Utils.bytesToIntLE(Arrays.copyOfRange(block, 12, 12+4)); //always zero
            
            int offset = 16;
            for(int i = 0; i < starZeros.blocks; i++) {
                
                StarZerosSubBlock szb = new StarZerosSubBlock();
                szb.blockIndex = i;
                szb.parent = starZeros;
                szb.size = Utils.bytesToIntLE(Arrays.copyOfRange(block, offset, offset+4));
                szb.header = Arrays.copyOfRange(block, offset, offset+16);
                szb.headerHexString = Utils.toHexString(szb.header, 4, 4, 4, 2, 2);
                
                offset += 4;
                
                //seems to be a header block of size 16 bytes
                szb.sizeUncompressed = Utils.bytesToIntLE(Arrays.copyOfRange(block, offset, offset+4));
                
                offset += 4;
                
                szb.unknown = Utils.bytesToIntLE(Arrays.copyOfRange(block, offset, offset+4));

                offset += 4;
                
                szb.flags1 = Utils.bytesToShortLE(Arrays.copyOfRange(block, offset, offset+2));
                
                offset += 2;
                
                szb.flags2 = Utils.bytesToShortLE(Arrays.copyOfRange(block, offset, offset+2));
                
                offset += 2;
                
                szb.compressed = szb.flags1 != 0;
                
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
            for(int i = 0; i < starZeros.starZerosBlocks.size(); i++) {
                
                StarZerosSubBlock szb = starZeros.starZerosBlocks.get(i);
                
                szb.data = Arrays.copyOfRange(starZeros.data, offset, offset + szb.size);
                
                offset += szb.size;
            }
            
            //may not work if data ends with 00
            int i = starZeros.full2048.length-1;
            for(; i >= 0; i--) {
                if(starZeros.full2048[i] != 0) {
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
    
    public void starZerosAnalysis() throws IOException {
        
        MinAvgMaxSdDouble blockSizes = new MinAvgMaxSdDouble();
        MinAvgMaxSdDouble subBlockTypes = new MinAvgMaxSdDouble();
        
        
        MinAvgMaxSdDouble flags1stats = new MinAvgMaxSdDouble();

        Set<Integer> size12DifferTypes = new HashSet<>();

        Map<Integer, Set<Integer>> type2bytesToRead = new HashMap<>();
        
        //System.out.println(Utils.toHexString(128, 4, 4, 4, 4, 4, 4, 4, 2, 2));
        //System.out.println("qQES: " + Utils.toHexString(qQES));
        
        for(StarZeros starZeros : starZerosList) {
            
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
            for(StarZerosSubBlock szb : starZeros.starZerosBlocks) {
                //System.out.println("\t" + szb.data.length + " " + szb + " " + szb.headerHexString);
                
                subBlockTypes.add(szb.flags2);
                
                byte[] preview = Arrays.copyOfRange(szb.data, 0, 128);
                
                List<Integer> qQESposs = Utils.find(qQES, preview);
                
                List<Integer> TIMposs = Utils.find(TIM, preview);
                
                List<Integer> hiraganaPoss = Utils.findHiragana(5, szb.data);
                
                String extra = "";
                if(!hiraganaPoss.isEmpty()) {
                    extra += "contains Hiragana ";
                }
                if(!qQESposs.isEmpty()) {
                    extra += "pQES ";
                }
                //if(!TIMposs.isEmpty()) {
                //    extra += "maybe TIM ";
                    
                //    System.out.println(Utils.toHexString(szb.data));
                //}
                if(szb.compressed) {
                    extra += "compressed ";
                }
                
                //list all
                System.out.println("\t" + szb.blockIndex + ": " + szb.headerHexString + " " + extra);
                
                
                flags1stats.add(szb.flags1);
                
                sizeOfBlocksTotal += szb.size;
                
                //works: there is always some data
                if(Utils.allZero(szb.data)) {
                    System.out.println("should not happen");
                    break;
                }
                
                //works: when compressed the sizeUncompressed is always larger
                if(szb.compressed && szb.size > szb.sizeUncompressed) {
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
                
                
                if(!hiraganaPoss.isEmpty()) {
                    //System.out.println(szb.parent.blockIndex + ": " + szb.parent.headerHexString);
                    //System.out.println("\t" + szb.blockIndex + ": " + szb.headerHexString);
                    
                    //System.out.println(Utils.toHexString(szb.data));
                    //System.out.println(Utils.toHexStringJp(szb.data, reader.sjishort2char));
                }
                
                if(!szb.compressed) {
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
            if(starZeros.sizeTotal != sizeOfBlocksTotal) {
                System.out.println("something wrong here");
                break;
            }
            
            //works
            if(starZeros.data.length != sizeOfBlocksTotal) {
                System.out.println("something wrong here");
                break;
            }
            
            blockSizes.add(starZeros.blocks);
            
            if(!Utils.allZero(starZeros.rest)) {
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
    
    public List<StarZerosSubBlock> getStarZerosSubBlocks() {
        List<StarZerosSubBlock> l = new ArrayList<>();
        for(StarZeros sz : starZerosList) {
            for(StarZerosSubBlock sb : sz.starZerosBlocks) {
                l.add(sb);
            }
        }
        return l;
    }
    
    public List<StarZerosSubBlock> getStarZerosSubBlocks(int type) {
        List<StarZerosSubBlock> l = new ArrayList<>();
        for(StarZeros sz : starZerosList) {
            for(StarZerosSubBlock sb : sz.starZerosBlocks) {
                if(sb.flags2 == type) {
                    l.add(sb);
                }
            }
        }
        return l;
    }
    
    public StarZerosSubBlock getSubBlock(int blockIndex, int subBlockIndex) {
        for(StarZeros sz : starZerosList) {
            if(sz.blockIndex == blockIndex) {
                for(StarZerosSubBlock sb : sz.starZerosBlocks) {
                    if(sb.blockIndex == subBlockIndex) {
                        return sb;
                    }
                }
            }
        }
        return null;
    }
    
    public void sortBySizeCompressed(List<StarZerosSubBlock> l) {
        l.sort((a,b) -> {
            
            int cmp = Boolean.compare(b.compressed, a.compressed);
            
            if(cmp == 0) {
                cmp = Integer.compare(a.size, b.size);
            }
            
            return cmp;
        });
    }
    
    
    
    public static void main(String[] args) throws Exception {
        
        //System.out.println(Utils.toBits((byte)255));
        //System.out.println(Utils.toBits((byte)128));
        //System.out.println(Utils.toBits(new byte[] { 127, 3, 18 }));
        
        
        //319.436.800 bytes
        HBD1PS1D hbd = new HBD1PS1D(new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip/HBD1PS1D.Q41"));
        
        
        
        PsxJisReader reader = new PsxJisReader();
        reader.readTable();
        
        hbd.reader = reader;
        
        //want to look into patterns
        //toGrayScale(hbd);
        
        //is hbd made out of 2048 blocks?
        //works they fill it with zeros, but images have different sizes
        //writeBlocks(hbd);
        
        //seems to be that first data are the blocks
        //hbd.extractFirstBlocks();
        
        //the number of 2048 is stated at 0x04
        hbd.correctBlockExtraction();
        
        //look into blocks: 60 01 01 80
        hbd.h60010180Extraction();
        //hbd.h60010180Analysis();
        
        //look into blocks: * 00 00 00
        hbd.starZerosExtraction();
        //hbd.starZerosAnalysis();
        
        //try decompress
        List<StarZerosSubBlock> l = hbd.getStarZerosSubBlocks();
        l.removeIf(sb -> sb.flags2 != 9);
        hbd.sortBySizeCompressed(l);
        StarZerosSubBlock sb = l.get(0);
        
        //26034/9
        //26046/6
        
        
        //593/2 "sample" in lz
        
        //StarZerosSubBlock sbA = hbd.getSubBlock(26034, 9);
        StarZerosSubBlock sbB = hbd.getSubBlock(593, 2);
        
        //System.out.println(Utils.toHexDump(sbA.data, 16, true, false, null));
        //byte[] decompressed = DQLZS.decompress(sbB.data, sbB.sizeUncompressed);
        
        //System.out.println("type=" + sb.flags2);
        //byte[] decompressed = DQLZS.decompress(sb.data, sb.sizeUncompressed);
        
        HBDFrame.showGUI(hbd);
    }
    
    private static void toGrayScale(HBD1PS1D hbd) throws IOException {
        //seems to be that there are 2048 blocks
        BufferedImage img = hbd.toGrayscale((int) Math.pow(2, 11), -1);
        
        //System.out.println("write png...");
        //ImageIO.write(img, "png", new File(hbd.file.getParentFile(), "all.png"));
        System.out.println("write bmp...");
        ImageIO.write(img, "bmp", new File(hbd.file.getParentFile(), img.getWidth() + "x" + img.getHeight() + "-all.bmp"));
    }

}
