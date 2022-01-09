package net.markus.projects.dh4;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.markus.projects.dh4.data.HuffmanChar;
import net.markus.projects.dh4.data.HuffmanCode;
import net.markus.projects.dh4.data.ParseNode;
import net.markus.projects.dh4.data.StarZeros;
import net.markus.projects.dh4.data.StarZerosSubBlock;
import net.markus.projects.dh4.data.TextBlock;
import net.markus.projects.dh4.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.crosswire.common.compress.LZSS;

/**
 *
 */
public class TranslationEmbedding {

    //first proof-of-concept with dialog pointer switch idea
    public void embed(File translationFolder, HBD1PS1D hbd, PSEXE psexe) throws IOException {

        Map<String, List<TextBlock>> id2tb = hbd.getTextBlockIDMap();
        System.out.println(id2tb.size() + " unique textblocks found");

        //these opcodes are used to create the pointer mapping code
        List<OpCode> code = new ArrayList<>();

        for (File translationFile : translationFolder.listFiles()) {
            if (!translationFile.getName().endsWith(".csv")) {
                continue;
            }

            String name = translationFile.getName();
            String hexId = name.substring(0, name.length() - ".csv".length());

            if (!id2tb.containsKey(hexId)) {
                throw new RuntimeException("there is no textblock with the id " + hexId + " because of " + translationFile.getName());
            }
            //we have to update these textblocks
            List<TextBlock> textblocks = id2tb.get(hexId);

            CSVParser parser = CSVParser.parse(translationFile, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
            List<CSVRecord> records = parser.getRecords();
            parser.close();

            System.out.println(translationFile.getName() + " with " + records.size() + " segments");

            //parse translated text
            List<HuffmanCode> segments = new ArrayList<>();
            for (CSVRecord record : records) {
                String text = record.get(0);
                String offsetHex = record.get(2).substring(2);

                if (text.trim().isEmpty()) {
                    text = "(text is empty){7f0a}";
                }

                //parse will convert ascii to japanese equivalent
                HuffmanCode textAsCode = HuffmanCode.parse(text);
                textAsCode.setByteDiffInHex(offsetHex);

                segments.add(textAsCode);
            }
            //sort segments
            segments.sort((a, b) -> a.getByteDiffInHex().compareTo(b.getByteDiffInHex()));

            //we merge to fullCode just to collect all chars and create char2freq map
            HuffmanCode fullCode = HuffmanCode.merge(segments);
            Map<String, Integer> char2freq = fullCode.getCharFrequencyMap();

            //System.out.println(fullCode.calculateText());
            //System.out.println(char2freq);
            ParseNode huffmanTreeRoot = hbd.createHuffmanTree(char2freq);
            byte[] huffmanTreeRootBytes = hbd.toHuffmanTreeBytes(huffmanTreeRoot);
            ParseNode parsedVersion = hbd.parseTree(huffmanTreeRootBytes);

            //System.out.println(huffmanTreeRoot.toStringTree());
            System.out.println(parsedVersion.toStringTree());

            //based on the tree we know what char which bits receive
            Map<String, String> char2bits = hbd.getCharacterToBitsMap(huffmanTreeRoot);

            //assign bits to every char in the code of every segment
            for (HuffmanCode segment : segments) {
                for (HuffmanChar ch : segment) {
                    String bits = char2bits.get(ch.getLetter());
                    if (bits == null) {
                        throw new RuntimeException("could not find bits for char " + ch);
                    }
                    ch.setBits(bits);
                }
            }

            //calculate the positions so that we know what start position we have to use for the segments
            HuffmanCode.calculateCharacterIndices(segments);

            //store hexId in r21
            code.add(new OpCode("ori r21," + hexId));
            //skip the whole section if it is not the hexId
            int skip = segments.size() * 5;
            //String skipHex = Utils.bytesToHex(Utils.intToByteArray(skip)).substring(4).toUpperCase();
            code.add(new OpCode("jne r21,r22," + skip)); //I implemented it with an decimal number (not hex)

            //we store the updated diff hex to create our pointer mapping in asm
            for (HuffmanCode segment : segments) {

                int firstBitInByteIndex = segment.get(0).getStartBitInByte();

                int firstByteIndex = segment.get(0).getByteIndex();
                //add textblock header length, usually 24 (directly after header starts huffman code)
                firstByteIndex += textblocks.get(0).huffmanCodeStart;

                String offsetHex = Utils.bytesToHex(Utils.intToByteArray(firstByteIndex)).toUpperCase();
                if (!offsetHex.startsWith("0000")) {
                    throw new RuntimeException("the offsetHex is greater then a short: " + offsetHex);
                }
                offsetHex = offsetHex.substring(4, 8);

                segment.setByteDiffInHexUpdated(offsetHex);

                segment.setText(segment.calculateText());

                //store in r21 the original offset hex
                code.add(new OpCode("ori r21," + segment.getByteDiffInHex()));
                //if this is not the right one skip the rest
                code.add(new OpCode("jne r21,r23,3"));

                //if it is the correct one store the bit index in r22
                code.add(new OpCode("lui r22," + firstBitInByteIndex + "500")); //load bit index, currently we do not know what the '5' means
                //if it is the correct one store the updated hex diff in r23
                code.add(new OpCode("ori r23," + segment.getByteDiffInHexUpdated()));

                //here we have to jump to the conversion at the end (replace it later)
                //we use the comment to find it
                code.add(new OpCode("nop").setComment("replaceLater"));
            }

            //merge it to update the byte[] in textblocks
            fullCode = HuffmanCode.merge(segments);
            fullCode.setHuffmanTreeRoot(huffmanTreeRoot);

            //just to check that everything is fine
            //seems to work correctly
            String fullCodeBits = fullCode.getBits();
            HuffmanCode decoded = hbd.decode(fullCodeBits, huffmanTreeRoot);
            System.out.println("fullCodeBits decoded: " + decoded.calculateText());

            //now we have to actually replace the data
            for (TextBlock tb : textblocks) {
                tb.huffmanTreeBytes = hbd.toHuffmanTreeBytes(huffmanTreeRoot);
                tb.root = huffmanTreeRoot; //is used to count root.descendants()
                tb.huffmanCode = hbd.encode(fullCode);

                //System.out.println(Utils.toHexDump(tb.huffmanCode, 12, true, false, null));
                //every index is calulated based on the byte arrays
            }

            //on patch it will update the subblock bytes
            //check if correct
            TextBlock tb = textblocks.get(0);
            String allBits = hbd.getBits(tb.huffmanCode);
            for (HuffmanCode segment : segments) {

                System.out.println(Utils.toHexDump(tb.huffmanCode, 16, true, false, null).toUpperCase());
                System.out.println(Utils.toHexDump(tb.huffmanTreeBytes, 16, true, false, null).toUpperCase());

                System.out.println("expected: " + segment.calculateText());
                System.out.println("original hex diff: " + segment.getByteDiffInHex());
                System.out.println("updated hex diff: " + segment.getByteDiffInHexUpdated());
                int firstBitInByteIndex = segment.get(0).getStartBitInByte();
                System.out.println("firstBitInByteIndex: " + firstBitInByteIndex);

                //textblocks.get(0).huffmanCodeStart
                int offset = Utils.bytesToInt(Utils.hexStringToByteArray("0000" + segment.getByteDiffInHexUpdated()));
                //because we calulated from there
                offset -= tb.huffmanCodeStart;
                System.out.println("updated dec diff (-huffmanCodeStart): " + offset);

                System.out.println("the start byte is: " + Utils.bytesToHex(new byte[]{tb.huffmanCode[offset]}).toUpperCase());
                System.out.println("the start byte bits are: " + Utils.toBits(new byte[]{tb.huffmanCode[offset]}));
                System.out.println("the start byte +1 bits are: " + Utils.toBits(new byte[]{tb.huffmanCode[offset + 1]}));
                System.out.println("the start byte bits start here: " + Utils.toBits(new byte[]{tb.huffmanCode[offset]}).substring(firstBitInByteIndex));

                int bitOffset = (offset * 8) + firstBitInByteIndex;
                System.out.println("bitOffset: " + bitOffset);

                String segmentBits = allBits.substring(bitOffset);
                System.out.println("fullCod    : " + fullCodeBits);
                System.out.println("allBits    : " + allBits);
                System.out.println("segmentBits: " + segmentBits);

                HuffmanCode actualCode = hbd.decode(segmentBits, huffmanTreeRoot);
                System.out.println("decoded segmentBits: " + actualCode.calculateText());

                System.out.println();
                System.out.println();
            }

        }//for each translation file

        //now we have to patch the psxexe to map the pointers correctly
        psexe.patch("8001D4CC", "8008EBA4", code);

    }

    //deprecated: unfinished idea to put additional data in the sector and do dialog pointer switch
    @Deprecated
    public void embedV1_2(File translationFolder, HBD1PS1D hbd, PSEXE psexe) throws IOException {

        List<OpCode> code = new ArrayList<>();

        //for each block its textblocks
        List<Object[]> tbRemList = new ArrayList<>();
        Map<StarZeros, List<TextBlock>> sz2tb = new HashMap<>();
        for (TextBlock tb : hbd.textBlocks) {
            tbRemList.add(new Object[]{tb.subBlock.getPath(), tb.subBlock.parent.remaining()});
            sz2tb.computeIfAbsent(tb.subBlock.parent, s -> new ArrayList<>()).add(tb);
        }

        //check for all textblocks
        Set<String> problematic = new HashSet<>();
        Set<String> works = new HashSet<>();
        Set<File> notFoundFiles = new HashSet<>();
        for (TextBlock tb : hbd.textBlocks) {
            //int numTb = sz2tb.get(tb.subBlock.parent).size();
            int remaining = tb.subBlock.parent.remaining();

            //some are filtered because they were dummys
            File file = new File(translationFolder, tb.getHexID() + ".csv");
            if (!file.exists()) {
                notFoundFiles.add(file);
                continue;
            }

            List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
            int numOfSeg = lines.size();

            //2 bytes is actual diff and 2 bytes expected diff
            int necessaryBytes = numOfSeg * (2 + 2);

            if (necessaryBytes > remaining) {
                //System.out.println("problem: " + necessaryBytes + " > " + remaining + " at " + tb.getHexID() + " in " + tb.subBlock.getPath());
                problematic.add(tb.getHexID());
            } else {
                works.add(tb.getHexID());
            }
        }

        System.out.println(notFoundFiles.size() + " not found");
        System.out.println(problematic.size() + " problematic: " + problematic);
        System.out.println(works.size() + " works");
        /*
        182 not found
        31 problematic
        895 works
         */

        Map<String, List<TextBlock>> id2tb = hbd.getTextBlockIDMap();
        System.out.println(id2tb.size() + " unique textblocks found");

        int fileIndex = 0;
        for (File translationFile : translationFolder.listFiles()) {
            if (!translationFile.getName().endsWith(".csv")) {
                continue;
            }

            String name = translationFile.getName();
            String hexId = name.substring(0, name.length() - ".csv".length());

            //too less space (for now)
            if (problematic.contains(hexId)) {
                continue;
            }

            if (!id2tb.containsKey(hexId)) {
                throw new RuntimeException("there is no textblock with the id " + hexId + " because of " + translationFile.getName());
            }
            //we have to update these textblocks
            List<TextBlock> textblocks = id2tb.get(hexId);

            CSVParser parser = CSVParser.parse(translationFile, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
            List<CSVRecord> records = parser.getRecords();
            parser.close();

            System.out.println(translationFile.getName() + " with " + records.size() + " segments");

            //for each record (= segment)
            List<HuffmanCode> segments = new ArrayList<>();
            for (CSVRecord record : records) {
                String text = record.get(0);
                String offsetHex = record.get(2).substring(2);

                text = hexId + " " + offsetHex + "{7f0a}";

                //parse will convert ascii to japanese equivalent
                HuffmanCode textAsCode = HuffmanCode.parse(text);
                textAsCode.setByteDiffInHex(offsetHex);

                segments.add(textAsCode);
            }
            //sort segments
            segments.sort((a, b) -> a.getByteDiffInHex().compareTo(b.getByteDiffInHex()));

            //we merge to fullCode just to collect all chars and create char2freq map
            HuffmanCode fullCode = HuffmanCode.merge(segments);
            Map<String, Integer> char2freq = fullCode.getCharFrequencyMap();

            //System.out.println(fullCode.calculateText());
            //System.out.println(char2freq);
            ParseNode huffmanTreeRoot = hbd.createHuffmanTree(char2freq);

            //check it:
            //byte[] huffmanTreeRootBytes = hbd.toHuffmanTreeBytes(huffmanTreeRoot);
            //ParseNode parsedVersion = hbd.parseTree(huffmanTreeRootBytes);
            //System.out.println(huffmanTreeRoot.toStringTree());
            //System.out.println(parsedVersion.toStringTree());
            //based on the tree we know what char which bits receive
            Map<String, String> char2bits = hbd.getCharacterToBitsMap(huffmanTreeRoot);

            //assign bits to every char in the code of every segment
            for (HuffmanCode segment : segments) {
                for (HuffmanChar ch : segment) {
                    String bits = char2bits.get(ch.getLetter());
                    if (bits == null) {
                        throw new RuntimeException("could not find bits for char " + ch);
                    }
                    ch.setBits(bits);
                }
            }

            //calculate the positions so that we know what start position we have to use for the segments
            HuffmanCode.calculateCharacterIndices(segments);
            //bits and positions are now set

            ByteArrayOutputStream offsetBAOS = new ByteArrayOutputStream();

            //calculate the updated diff offset and set the new text
            for (HuffmanCode segment : segments) {
                //int firstBitInByteIndex = segment.get(0).getStartBitInByte();

                int firstByteIndex = segment.get(0).getByteIndex();
                //add textblock header length, usually 24 (directly after header starts huffman code)
                //TODO could be different per textblock
                firstByteIndex += textblocks.get(0).huffmanCodeStart;

                String offsetHex = Utils.bytesToHex(Utils.intToByteArray(firstByteIndex)).toUpperCase();
                if (!offsetHex.startsWith("0000")) {
                    throw new RuntimeException("the offsetHex is greater then a short: " + offsetHex);
                }
                offsetHex = offsetHex.substring(4, 8);

                segment.setByteDiffInHexUpdated(offsetHex);

                segment.setText(segment.calculateText());

                System.out.println("\tupdated offset: " + segment.getByteDiffInHex() + " -> " + segment.getByteDiffInHexUpdated());
                offsetBAOS.write(Utils.hexStringToByteArray(segment.getByteDiffInHex()));
                offsetBAOS.write(Utils.hexStringToByteArray(segment.getByteDiffInHexUpdated()));
            }

            byte[] offsetByteArray = offsetBAOS.toByteArray();

            if (offsetByteArray.length != segments.size() * (2 + 2)) {
                throw new RuntimeException("unexpected offsetByteArray length");
            }

            //merge it to update the byte[] in textblocks
            fullCode = HuffmanCode.merge(segments);
            fullCode.setHuffmanTreeRoot(huffmanTreeRoot);

            //just to check that everything is fine
            //seems to work correctly
            //String fullCodeBits = fullCode.getBits();
            //HuffmanCode decoded = hbd.decode(fullCodeBits, huffmanTreeRoot);
            //System.out.println("fullCodeBits decoded: " + decoded.calculateText());
            boolean patchTextblocks = false;
            boolean patchExtraDataEnd = true;

            //now we have to actually replace the data
            for (TextBlock tb : textblocks) {
                if (patchTextblocks) {
                    tb.huffmanTreeBytes = hbd.toHuffmanTreeBytes(huffmanTreeRoot);
                    tb.root = huffmanTreeRoot; //is used to count root.descendants()
                    tb.huffmanCode = hbd.encode(fullCode);
                }

                if (patchExtraDataEnd) {
                    tb.extraDataEnd = offsetByteArray;
                }
            }

            System.out.println(fileIndex + " file " + hexId);
            fileIndex++;

        }//for each translation file = for each unique textblock id

        /*
        if(tb.subBlock.getPath().equals("26046/13")) {
            System.out.println("add bytes for 26046/13");
            tb.extraDataEnd = extraDataEnd;

            //with 701 additional bytes sector changes

            //when sector changes we get an error

            System.out.println(tb.subBlock.parent.remaining() + " remaining");
        }
         */
        List<Entry<StarZeros, List<TextBlock>>> l = new ArrayList<>(sz2tb.entrySet());
        l.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
        //l.forEach(e -> System.out.println(e.getKey().blockIndex + " -> " + e.getValue().size() + ", remaining: " + e.getKey().remaining()));
        tbRemList.sort((a, b) -> Integer.compare((int) a[1], (int) b[1]));
        //tbRemList.forEach(e -> System.out.println(Arrays.toString(e)));
        //some sectors are full

        psexe.patch("8001D4CC", "8008EBA4", code);
    }

    //ca. from 2021-12-21: this idea uses the cut scene script we found and checks if compression will work in-game
    public void embedV2onlyCompress(File translationFolder, HBD1PS1D hbd) {

        Map<String, List<TextBlock>> id2tb = hbd.getTextBlockIDMap();
        System.out.println(id2tb.size() + " unique textblocks found");

        Set<String> hexIdWhitelist = new HashSet<>();
        hexIdWhitelist.add("006C");

        for (File translationFile : translationFolder.listFiles()) {
            if (!translationFile.getName().endsWith(".csv")) {
                continue;
            }

            String name = translationFile.getName();
            String hexId = name.substring(0, name.length() - ".csv".length());

            if (!hexIdWhitelist.isEmpty() && !hexIdWhitelist.contains(hexId)) {
                continue;
            }

            //we have to update these textblocks
            List<TextBlock> textblocks = id2tb.get(hexId);
            StarZerosSubBlock sb = textblocks.get(0).subBlock;

            StarZerosSubBlock cutSceneBlock = null;
            for (StarZerosSubBlock child : sb.parent.starZerosBlocks) {
                if (child.type == 39) {
                    cutSceneBlock = child;
                    break;
                }
            }

            if (cutSceneBlock == null) {
                System.out.println("cut scene block not found");
                continue;
            }

            System.out.println(
                    hexId + " with " + textblocks.size() + " textblocks in subblock " + sb.getPath()
                    + ", cut scene block " + cutSceneBlock.getPath() + " compressed: " + cutSceneBlock.compressed
            );

            byte[] cutSceneDecompData = null;
            if (cutSceneBlock.compressed) {
                DQLZS.DecompressResult decompressResult = DQLZS.decompress(cutSceneBlock.data, cutSceneBlock.sizeUncompressed, false);
                cutSceneDecompData = decompressResult.data;

                System.out.println("uncompressed cut scene block data len=" + cutSceneDecompData.length + " sizeUncompressed=" + cutSceneBlock.sizeUncompressed);
            } else {
                cutSceneDecompData = cutSceneBlock.data;
            }

            //==================================================
            // here we would change text block and cut scene data
            //==================================================
            //if cut scene block was compressed we compress it again
            //leads to an error as soon as the cut scene script is evaluated 
            if (cutSceneBlock.compressed) {
                byte[] compressData = LZSS.compress(cutSceneDecompData, cutSceneBlock.data.length);

                System.out.println("compressed cut scene block data len=" + compressData.length);

                //update data
                cutSceneBlock.data = compressData;
                cutSceneBlock.size = compressData.length;
                //uncompressed size does not change
            }

            //another idea: do not compress it
            //because of larger size leads to: 26046 sector change from 44 to 46
            /*
            if(cutSceneBlock.compressed) {
                
                cutSceneBlock.compressed = false;
                cutSceneBlock.data = cutSceneDecompData;
                cutSceneBlock.size = cutSceneDecompData.length;
                cutSceneBlock.sizeUncompressed = cutSceneDecompData.length;
                cutSceneBlock.flags1 = 0;
            }
             */
        }

    }

    //proof of concept that the dialog pointer change works
    public void embedV2onlyPointerChange(File translationFolder, HBD1PS1D hbd) {

        Map<String, List<TextBlock>> id2tb = hbd.getTextBlockIDMap();
        System.out.println(id2tb.size() + " unique textblocks found");

        Set<String> hexIdWhitelist = new HashSet<>();
        hexIdWhitelist.add("006C");

        for (File translationFile : translationFolder.listFiles()) {
            if (!translationFile.getName().endsWith(".csv")) {
                continue;
            }

            String name = translationFile.getName();
            String hexId = name.substring(0, name.length() - ".csv".length());

            if (!hexIdWhitelist.isEmpty() && !hexIdWhitelist.contains(hexId)) {
                continue;
            }

            //we have to update these textblocks
            List<TextBlock> textblocks = id2tb.get(hexId);
            StarZerosSubBlock sb = textblocks.get(0).subBlock;

            StarZerosSubBlock cutSceneBlock = null;
            for (StarZerosSubBlock child : sb.parent.starZerosBlocks) {
                if (child.type == 39) {
                    cutSceneBlock = child;
                    break;
                }
            }

            if (cutSceneBlock == null) {
                System.out.println("cut scene block not found");
                continue;
            }

            System.out.println(
                    hexId + " with " + textblocks.size() + " textblocks in subblock " + sb.getPath()
                    + ", cut scene block " + cutSceneBlock.getPath() + " compressed: " + cutSceneBlock.compressed
            );

            byte[] cutSceneDecompData = null;
            if (cutSceneBlock.compressed) {
                DQLZS.DecompressResult decompressResult = DQLZS.decompress(cutSceneBlock.data, cutSceneBlock.sizeUncompressed, false);
                cutSceneDecompData = decompressResult.data;

                System.out.println("uncompressed cut scene block data len=" + cutSceneDecompData.length + " sizeUncompressed=" + cutSceneBlock.sizeUncompressed);
            } else {
                cutSceneDecompData = cutSceneBlock.data;
            }

            //==================================================
            // here we change text block and cut scene data
            //==================================================
            //for now cut scene data
            List<CSVRecord> records;
            try {
                CSVParser p = CSVFormat.DEFAULT.parse(new FileReader(translationFile));
                records = p.getRecords();
                p.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            records.forEach(r -> System.out.println(r));

            for (int i = 0; i < 4; i++) {
                CSVRecord record = records.get(i);
                byte[] command = Utils.hexStringToByteArray(record.get(2));

                List<Integer> matches = Utils.find(command, cutSceneDecompData);

                if (matches.isEmpty()) {
                    System.out.println("not found in cut scene: " + record);
                } else if (matches.size() > 1) {
                    System.out.println("multiple found (" + matches.size() + ") in cut scene: " + record);
                }

                int pos = matches.get(0);

                //we just reverse the dialogs
                CSVRecord replaceRecord = records.get(records.size() - 1 - i);
                byte[] replaceCommand = Utils.hexStringToByteArray(replaceRecord.get(2));

                List<Integer> matches2 = Utils.find(replaceCommand, cutSceneDecompData);
                int pos2 = matches2.get(0);

                //swap
                cutSceneDecompData = Utils.replace(replaceCommand, pos, cutSceneDecompData);
                cutSceneDecompData = Utils.replace(command, pos2, cutSceneDecompData);
            }

            //===================================================
            //if cut scene block was compressed we compress it again
            //leads to an error as soon as the cut scene script is evaluated 
            if (cutSceneBlock.compressed) {
                byte[] compressData = LZSS.compress(cutSceneDecompData, cutSceneBlock.data.length);

                System.out.println("compressed cut scene block data len=" + compressData.length);

                //update data
                cutSceneBlock.data = compressData;
                cutSceneBlock.size = compressData.length;
            }
        }

    }

    public void embedV2(File translationFolder, HBD1PS1D hbd) {
        
        //decompress all to search in them later
        /*
        List<StarZerosSubBlock> cutSceneBlockList = hbd.getStarZerosSubBlocks(39);
        Map<StarZerosSubBlock, byte[]> cutSceneBlockDecompressedMap = new HashMap<>();
        for(StarZerosSubBlock cutSceneBlock : cutSceneBlockList) {
            
            byte[] cutSceneDecompData = null;
            if (cutSceneBlock.compressed) {
                DQLZS.DecompressResult decompressResult = DQLZS.decompress(cutSceneBlock.data, cutSceneBlock.sizeUncompressed, false);
                cutSceneDecompData = decompressResult.data;
            } else {
                cutSceneDecompData = cutSceneBlock.data;
            }
            
            cutSceneBlockDecompressedMap.put(cutSceneBlock, cutSceneDecompData);
        }
        System.out.println("decompressed cut scenes: " + cutSceneBlockDecompressedMap.size());
        */
        
        Map<String, List<TextBlock>> id2tb = hbd.getTextBlockIDMap();
        System.out.println(id2tb.size() + " unique textblocks found");

        Set<String> hexIdWhitelist = new HashSet<>();
        //hexIdWhitelist.add("006C"); //first scene
        //hexIdWhitelist.add("0067"); //in town: would work
        //hexIdWhitelist.add("0068"); //the house in top-right corner
        //hexIdWhitelist.add("0065"); //the house in bottom-right corner
        //hexIdWhitelist.add("0069"); //the house in bottom-left corner
        //hexIdWhitelist.add("0392"); //chapter 1 - castle
        //hexIdWhitelist.add("0390"); //chapter 1 - town
        
        List<String> cutSceneBlockNotFound = new ArrayList<>();
        List<String> cutSceneCommandNotAvail = new ArrayList<>();
        List<String> patched = new ArrayList<>();
        
        List<CSVRecord> globalCommandNotFound = new ArrayList<>();
        List<CSVRecord> globalCommandFound = new ArrayList<>();

        List<TextLineRef> textLineRefs = new ArrayList<>();
        
        for (File translationFile : translationFolder.listFiles()) {
            if (!translationFile.getName().endsWith(".csv")) {
                continue;
            }

            String name = translationFile.getName();
            String hexId = name.substring(0, name.length() - ".csv".length());

            if (!hexIdWhitelist.isEmpty() && !hexIdWhitelist.contains(hexId)) {
                continue;
            }

            //we have to update these textblocks
            List<TextBlock> textblocks = id2tb.get(hexId);
            StarZerosSubBlock sb = textblocks.get(0).subBlock;

            List<StarZerosSubBlock> cutSceneBlocks = new ArrayList<>();
            List<StarZerosSubBlock> type40Blocks = new ArrayList<>();
            Map<StarZerosSubBlock, byte[]> child2data = new HashMap<>();
            for (StarZerosSubBlock child : sb.parent.starZerosBlocks) {
                if (child.type == 39) {
                    cutSceneBlocks.add(child);
                }
                if (child.type == 40) {
                    type40Blocks.add(child);
                }
                
                //get all of them to maybe find dialog pointer somewhere else
                byte[] childData;
                if (child.compressed) {
                    DQLZS.DecompressResult decompressResult = DQLZS.decompress(child.data, child.sizeUncompressed, false);
                    childData = decompressResult.data;
                    //System.out.println("uncompressed cut scene block data len=" + cutSceneDecompData.length + " sizeUncompressed=" + cutSceneBlock.sizeUncompressed);
                } else {
                    childData = child.data;
                }
                child2data.put(child, childData);
            }

            System.out.println(
                    hexId + " with " + textblocks.size() + " textblocks in subblock " + sb.getPath()
                    + ", number of cut scene blocks (type 39)=" + cutSceneBlocks.size() + ", number of type40 blocks=" + type40Blocks.size()
            );

            byte[] cutSceneDecompData = null;
            boolean hasScript = false;
            if(!cutSceneBlocks.isEmpty()) {
                StarZerosSubBlock cutSceneBlock = cutSceneBlocks.get(0);
                hasScript = true;
                
                if (cutSceneBlock.compressed) {
                    DQLZS.DecompressResult decompressResult = DQLZS.decompress(cutSceneBlock.data, cutSceneBlock.sizeUncompressed, false);
                    cutSceneDecompData = decompressResult.data;
                    //System.out.println("uncompressed cut scene block data len=" + cutSceneDecompData.length + " sizeUncompressed=" + cutSceneBlock.sizeUncompressed);
                } else {
                    cutSceneDecompData = cutSceneBlock.data;
                }
                
                //should never happen
                if(cutSceneBlocks.size() > 1) {
                    throw new RuntimeException("multiple type 39 blocks");
                }
            }
            
            boolean hasType40 = false;
            Map<StarZerosSubBlock, byte[]> type40blockDataMap = new HashMap<>();
            for(StarZerosSubBlock type40Block : type40Blocks) {
                hasType40 = true;
                byte[] type40blockData;
                
                if (type40Block.compressed) {
                    DQLZS.DecompressResult decompressResult = DQLZS.decompress(type40Block.data, type40Block.sizeUncompressed, false);
                    type40blockData = decompressResult.data;
                    //System.out.println("uncompressed type40 data len=" + type40blockData.length + " sizeUncompressed=" + type40Block.sizeUncompressed);
                } else {
                    type40blockData = type40Block.data;
                }
                
                type40blockDataMap.put(type40Block, type40blockData);
                
                //happens
                //if(type40Blocks.size() > 1) {
                //    throw new RuntimeException("multiple type 40 blocks");
                //}
            }
            
            //==================================================
            // here we change text block and cut scene data
            //==================================================

            //read translation data
            List<CSVRecord> records;
            try {
                CSVParser p = CSVFormat.DEFAULT.parse(new FileReader(translationFile));
                records = p.getRecords();
                p.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            
            //quick check if commands are available
            //List<CSVRecord> notFound = new ArrayList<>();
            for(CSVRecord record : records) {
                
                TextLineRef textLineRef = new TextLineRef();
                textLineRefs.add(textLineRef);
                
                //context info
                textLineRef.hexId = hexId;
                textLineRef.translationFile = translationFile;
                textLineRef.textblocks = textblocks;
                textLineRef.textLine = record;
                
                byte[] command = Utils.hexStringToByteArray(record.get(2));
                
                boolean foundInType39 = false;
                
                if(hasScript) {
                    List<Integer> matches = Utils.find(command, cutSceneDecompData);
                    if(matches.isEmpty()) {
                        
                        
                        //notFound.add(record);
                        //globalCommandNotFound.add(record);
                        
                        
                    } else {
                        textLineRef.refType = "C021A0";
                        textLineRef.refBlock = cutSceneBlocks.get(0);
                        textLineRef.matches = matches;
                        
                        foundInType39 = true;
                    }
                }
                
                boolean foundInType40 = false;
                if(!foundInType39 && hasType40) {
                    //search in type 40 block (FFF0 case)
                    
                    //C2 1E 20 39
                    String dialogInfoHex = record.get(2).substring(9, record.get(2).length());
                    byte[] dialogInfo = Utils.hexStringToByteArray(dialogInfoHex);
                    
                    for(StarZerosSubBlock type40Block : type40Blocks) {
                    
                        List<Integer> matches = Utils.find(dialogInfo, type40blockDataMap.get(type40Block));
                        if(matches.isEmpty()) {

                            //notFound.add(record);
                            //globalCommandNotFound.add(record);

                        } else {
                            textLineRef.refType = "FFF0";
                            textLineRef.refBlock = type40Blocks.get(0);
                            textLineRef.matches = matches;
                            
                            foundInType40 = true;
                        }
                    
                    }
                }
                
                boolean foundInExtra = false;
                if(!foundInType39 && !foundInType40) {
                    
                    String dialogInfoHex = record.get(2).substring(9, record.get(2).length());
                    byte[] dialogInfo = Utils.hexStringToByteArray(dialogInfoHex);
                    
                    byte[] bitOffset = Utils.hexStringToByteArray(record.get(3));
                    
                    List<byte[]> patterns = new ArrayList<>();
                    patterns.add(dialogInfo);//works 2221 times
                    patterns.add(Utils.reverse(dialogInfo));
                    patterns.add(bitOffset);
                    patterns.add(Utils.reverse(bitOffset));
                    
                    List<String> patternName = Arrays.asList("dialogInfo", "dialogInfo-reversed", "bitOffset", "bitOffset-reversed");
                    
                    List<Entry<StarZerosSubBlock,byte[]>> entries = new ArrayList<>(child2data.entrySet());
                    entries.sort((a,b) -> Integer.compare(b.getKey().type, a.getKey().type));
                    
                    for(Entry<StarZerosSubBlock,byte[]> entry : entries) {
                        
                        int p = 0;
                        for(byte[] pattern : patterns) {
                        
                            List<Integer> matches = Utils.find(pattern, entry.getValue());

                            if(!matches.isEmpty()) {
                                //found again happens only once
                                textLineRef.refType = "extra-" + patternName.get(p);
                                textLineRef.refBlock = entry.getKey();
                                textLineRef.matches = matches;
                                textLineRef.pattern = Utils.toHexString(pattern);
                                
                                //System.out.println("\textra type " + entry.getKey().type);
                                //39->cut scene, 42->text block, 46


                                foundInExtra = true;
                            }
                            
                            if(foundInExtra)
                                break;
                            
                            p++;
                        }
                        
                        if(foundInExtra)
                            break;
                    }
                    
                }
            }
            

            //build huffman tree, huffman code, patch textblocks, store in segments the commands
            //List<HuffmanCode> segments = patchTextBlocks(records, hexId, textblocks, hbd);
            
            //replace old command with new ones
            //cutSceneDecompData = patchCutSceneScript(segments, cutSceneDecompData);
            
            //keep track what was patched
            //patched.add(hexId);
            
            //===================================================
            //if cut scene block was compressed we compress it again
            //leads to an error as soon as the cut scene script is evaluated 
            
            /*
            if (cutSceneBlock.compressed) {
                byte[] compressData = LZSS.compress(cutSceneDecompData, cutSceneBlock.data.length);

                System.out.println("compressed cut scene block data len=" + compressData.length);

                //update data
                cutSceneBlock.data = compressData;
                cutSceneBlock.size = compressData.length;
            }
            */
            
            //System.out.println();
            
        }//for translation file

        /*
        cutSceneBlockNotFound.sort((a,b) -> a.compareTo(b));
        cutSceneCommandNotAvail.sort((a,b) -> a.compareTo(b));
        patched.sort((a,b) -> a.compareTo(b));
        
        System.out.println(cutSceneBlockNotFound.size() + " cut scene block not found");
        cutSceneBlockNotFound.forEach(hex -> System.out.println("\t" + hex));
        System.out.println(cutSceneCommandNotAvail.size() + " cut scene command not avail");
        cutSceneCommandNotAvail.forEach(hex -> System.out.println("\t" + hex));
        System.out.println(patched.size() + " patched");
        patched.forEach(hex -> System.out.println("\t" + hex));
        
        System.out.println(globalCommandFound.size() + " text lines c0 21 a0 ** ** command found");
        System.out.println(globalCommandNotFound.size() + " text lines c0 21 a0 ** ** command not found");
        System.out.println(globalCommandNotFound.size() / (double) (globalCommandFound.size() + globalCommandNotFound.size()) + " not found");
        */
        
        Map<String, Integer> refType2count = new HashMap<>();
        
        for(TextLineRef textLineRef : textLineRefs) {
            
            if(textLineRef.refType.equals("none") || textLineRef.refType.startsWith("extra"))
                System.out.println(textLineRef);
            
            //count ref types
            int count = refType2count.computeIfAbsent(textLineRef.refType, str -> 0);
            refType2count.put(textLineRef.refType, count + 1);
        }
        
        int fff0 = refType2count.getOrDefault("FFF0", 0);
        int c021a0 = refType2count.getOrDefault("C021A0", 0);
        int extra = refType2count.getOrDefault("extra", 0);
        int none = refType2count.getOrDefault("none", 0);
        
        double missing = none / (double) (none + c021a0 + fff0 + extra);
        
        System.out.println(textLineRefs.size() + " text line refs");
        System.out.println(refType2count);
        System.out.println("missing: " + missing);
        
        //TODO look at refType=extra-dialogInfo, (type 42),
        //maybe type 46 is relevant
        
        //{FFF0=7953, C021A0=3200, extra-dialogInfo=2109, extra-bitOffset=276, extra-bitOffset-reversed=1349, none=7}
        
        //14894 text line refs
        //{extra=2221, FFF0=7953, none=1520, C021A0=3200}
        //missing: 0.1020545185980932
        
        int a = 0;
    }

    private List<HuffmanCode> patchTextBlocks(List<CSVRecord> records, String hexId, List<TextBlock> textblocks, HBD1PS1D hbd) {
        List<HuffmanCode> segments = new ArrayList<>();
        for (CSVRecord record : records) {
            
            //get text
            String text = record.get(0);
            if (text.trim().isEmpty()) {
                
                //this works but last dialog in 006C has no spaces
                text = hexId + " " + record.get(3) + " " + record.get(4) + "{7f0a}";
                //text = record.get(1);
            }

            //parse will convert ascii to japanese equivalent
            HuffmanCode textAsCode = HuffmanCode.parse(text);
            //the bit position in hex
            textAsCode.setBitInHex(record.get(3));
            //original command
            textAsCode.setCommand(Utils.hexStringToByteArray(record.get(2)));
            segments.add(textAsCode);
        }
        //sort segments
        segments.sort((a, b) -> a.getBitInHex().compareTo(b.getBitInHex()));
        
        //create full code to get char frequency to build tree
        HuffmanCode fullCode = HuffmanCode.merge(segments);
        Map<String, Integer> char2freq = fullCode.getCharFrequencyMap();
        //System.out.println(fullCode.calculateText());
        //System.out.println(char2freq);
        ParseNode huffmanTreeRoot = hbd.createHuffmanTree(char2freq);

        
        //debug:
        //System.out.println(huffmanTreeRoot.toStringTree());
        byte[] huffmanTreeRootBytes = hbd.toHuffmanTreeBytes(huffmanTreeRoot);
        ParseNode parsedVersion = hbd.parseTree(huffmanTreeRootBytes);
        System.out.println(parsedVersion.toStringTree());

        
        //based on the tree we know what char which bits receive
        Map<String, String> char2bits = hbd.getCharacterToBitsMap(huffmanTreeRoot);
        //assign bits to every char in the code of every segment
        for (HuffmanCode segment : segments) {
            for (HuffmanChar ch : segment) {
                String bits = char2bits.get(ch.getLetter());
                if (bits == null) {
                    throw new RuntimeException("could not find bits for char " + ch);
                }
                ch.setBits(bits);
            }
        }

        //calculate the positions so that we know what start position we have to use for the segments
        HuffmanCode.calculateCharacterIndices(segments);
        
        //merge it to update the byte[] in textblocks
        fullCode = HuffmanCode.merge(segments);
        fullCode.setHuffmanTreeRoot(huffmanTreeRoot);

        //debug:
        //just to check that everything is fine
        //seems to work correctly
        String fullCodeBits = fullCode.getBits();
        HuffmanCode decoded = hbd.decode(fullCodeBits, huffmanTreeRoot);
        System.out.println("fullCodeBits decoded: " + decoded.calculateText());

        //now we have to actually replace the data
        for (TextBlock tb : textblocks) {
            tb.huffmanTreeBytes = hbd.toHuffmanTreeBytes(huffmanTreeRoot);
            tb.root = huffmanTreeRoot; //is used to count root.descendants()
            tb.huffmanCode = hbd.encode(fullCode);
        }
        
        return segments;
    }

    private byte[] patchCutSceneScript(List<HuffmanCode> segments, byte[] cutSceneDecompData) {
        //from here on we know the new bit offsets
        //the first char of the segment tells us the new bit index
        for (HuffmanCode segment : segments) {
            
            HuffmanChar ch = segment.get(0);
            
            //add textblock header length, usually 24 (directly after header starts huffman code)
            int newPosition = ch.getStartBit() + (24 * 8);//24 byte * 8 bit
            String newPositionHex = Utils.toHexString(Utils.intToByteArray(newPosition)).replace(" ", "").substring(4, 8);
            
            List<Integer> matches = Utils.find(segment.getCommand(), cutSceneDecompData);
            /*
            if(matches.isEmpty()) {
                //we quick checked this one before
                System.out.println("not found in cut scene: " + segment);
            } else if(matches.size() > 1) {
                System.out.println("multiple found ("+ matches.size() +") in cut scene: " + segment);
            }
            */
            
            segment.setCommandPositions(matches);
            
            String oldCommand = Utils.toHexString(segment.getCommand()).replace(" ", "");
            
            String cmdInfo = "c021a0";
            String posInfo = newPositionHex.substring(2, 4) + newPositionHex.substring(0, 2);
            String idInfo = oldCommand.substring(10, 14);
            String newCommand = cmdInfo + posInfo + idInfo;

            byte[] newCommandBytes = Utils.hexStringToByteArray(newCommand);
            segment.setNewCommand(newCommandBytes);
            
            System.out.println(segment.getBitInHex() + " -> " + newPositionHex);
            System.out.println(Utils.toHexString(segment.getCommand()) + " -> " + Utils.toHexString(newCommandBytes) + " found at " + matches);
        }
        
        //actual patching: replace old command with new command at position
        for (HuffmanCode segment : segments) {
            
            for(Integer pos : segment.getCommandPositions()) {
                cutSceneDecompData = Utils.replace(segment.getNewCommand(), pos, cutSceneDecompData);
            }
        }
        
        return cutSceneDecompData;
    }
    
    private class TextLineRef {
        
        private File translationFile;
        private String hexId;
        private List<TextBlock> textblocks;
        
        private CSVRecord textLine;
        
        private String refType = "none";
        private StarZerosSubBlock refBlock;
        private List<Integer> matches;
        private String pattern;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TextLineRef{");
            sb.append("hexId=").append(hexId);
            sb.append(", textblocks.size=").append(textblocks.size());
            sb.append(", refType=").append(refType);
            if(refBlock != null)
                sb.append(", refBlock=").append(refBlock.getPath() + " (type " + refBlock.type + ")");
            if(matches != null)
                sb.append(", matches=").append(matches);
            sb.append(", textLine=").append(textLine);
            sb.append('}');
            return sb.toString();
        }
        
        
        
        
    }
    
    
    //extra injected
    //if(true) { //subBlock.getPath().equals("26046/13")) {
    //first scene, 0x6C
    //String idHex = Utils.toHexString(Utils.intToByteArrayLE(id));
    //System.out.println(Utils.toHexDump(huffmanTreeBytes, 16, true, false, null));
    //dataDA = new byte[512];
    //at the end leads to I/O error
    //for(int i = 0; i < 1024; i++) {
    //    baos.write(i % 255);
    //}
    //add an extra subblock (also I/O error if all are changed)
    /*
        StarZerosSubBlock sb = new StarZerosSubBlock();
        sb.data = new byte[512];
        sb.sizeUncompressed = sb.data.length;
        sb.size = sb.data.length;
        sb.unknown = 0;
        sb.flags1 = 0;
        sb.type = 128;
        sb.parent = subBlock.parent;
        sb.blockIndex = subBlock.parent.starZerosBlocks.size();
        subBlock.parent.starZerosBlocks.add(sb);
     */
    //}
    //if(subBlock.getPath().equals("26046/13")) {//subBlock.parent.blockIndex < 1500) {
    //    huffmanCode = new byte[1024];
    //}
}
