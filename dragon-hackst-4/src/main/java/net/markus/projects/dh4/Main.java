
package net.markus.projects.dh4;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.imageio.ImageIO;
import net.markus.projects.dh4.DQLZS.DecompressResult;
import net.markus.projects.dh4.data.DQFiles;
import net.markus.projects.dh4.data.H60010108;
import net.markus.projects.dh4.data.HBDBlock;
import net.markus.projects.dh4.data.HuffmanChar;
import net.markus.projects.dh4.data.HuffmanCode;
import net.markus.projects.dh4.data.StarZeros;
import net.markus.projects.dh4.data.StarZerosSubBlock;
import net.markus.projects.dh4.data.TextBlock;
import net.markus.projects.dh4.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;

/**
 * Main entry point.
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        //shiftjisToHex();
        
        boolean writePatch = false;
        DQFiles dqFiles = DQFiles.dq4();
        
        HBD1PS1D hbd = load(dqFiles.readHbdFile);
        PSEXE psexe = psxexe(dqFiles);
        
        System.out.println();
        System.out.println("transform");
        
        analyseTextBlocksV2(hbd);
        //inspectWithGUI(hbd);
        //translationPreparation(hbd, dqFiles);
        //translationEmbedding(hbd, psexe, dqFiles);
        //translationEmbeddingV2(hbd, psexe, dqFiles);
        //analyseTextBlocks(hbd);
        //printBlocks(hbd);
        //h60010108Blocks(hbd);
        //lzsEvaluation(hbd);
        //timExtraction(hbd);
        //veryFirstBlock(hbd);
        //fontImages(hbd);
        
        if(writePatch) {
            System.out.println();
            System.out.println("write patch");
            
            updateBlocks(hbd);
            save(hbd, dqFiles.writeHbdFile);
            
            dqFiles.patchedFolder.mkdirs();
            System.out.println("mkdir " + dqFiles.patchedFolder);
            
            File patchedPsexe = new File(new File(dqFiles.patchedFolder, dqFiles.name), dqFiles.exeFile.getName());
            System.out.println("write psexe to " + patchedPsexe);
            psexe.save(patchedPsexe);

            //open patched one again
            System.out.println("reload patched file to check for I/O errors -----------");
            HBD1PS1D patchedHbd = load(dqFiles.writeHbdFile);
            System.out.println("done -----------------");
            
            File psxbuildBin = new File("../../tools/psximager-master/psximager-master/src/psxbuild");
            psxbuild(dqFiles, psxbuildBin);
        }
    }
    
    private static PSEXE psxexe(DQFiles dqFiles) throws IOException {
        PSEXE psexe = new PSEXE(dqFiles.exeFile, "80017F00");
        
        //psexe.patch();
        
        return psexe;
    }
    
    //loads data into RAM
    private static HBD1PS1D load(File file) throws IOException {
        
        HBD1PS1D hbd = new HBD1PS1D(file);
        
        //the number of 2048 is stated at 0x04
        hbd.correctBlockExtraction();
        
        //look into blocks: 60 01 01 80
        hbd.h60010180Extraction();
        //hbd.h60010180Analysis();
        
        //look into blocks: * 00 00 00
        hbd.starZerosExtraction();
        //hbd.starZerosAnalysis();
        
        //certain types are text blocks
        hbd.textBlockExtraction(file.getName().contains("Q41") ? HBD1PS1D.dq4TextTypes : HBD1PS1D.dq7TextTypes);
        
        return hbd;
    }
    
    private static void printBlocks(HBD1PS1D hbd) {
        
        //found only in 26046/13
        //byte[] pattern = Utils.hexStringToByteArray("61806380658067806980E708C609");
        //26046/13
        byte[] pattern = Utils.hexStringToByteArray("6B00C808");//C808EA08B202 780B6C0D510E700E
        
        List<StarZerosSubBlock> matched = new ArrayList<>();
        
        for(HBDBlock block : hbd.blocks) {
            byte[] toSearch = null;
            
            if(block instanceof StarZeros) {
                StarZeros sz = (StarZeros) block;
                
                //System.out.println(sz.blockIndex);
                
                for(StarZerosSubBlock sb : sz.starZerosBlocks) {
                    //System.out.println("\t" + sb + " " + HBD1PS1D.getTypeName(sb.type));
                    
                    List<Integer> l = Utils.find(pattern, sb.data);
                    
                    if(!l.isEmpty()) {
                        System.out.println("found at: " + l);
                        matched.add(sb);
                        //return;
                    }
                }
            }
            else if(block instanceof H60010108) {
                H60010108 h6 = (H60010108) block;
            }
        }
        
        System.out.println(matched.size() + " matched");
        
        for(StarZerosSubBlock sb : matched) {
            System.out.println(sb);
            
            for(TextBlock tb : hbd.textBlocks) {
                if(tb.subBlock == sb) {
                    System.out.println(tb);
                }
            }
        }
        
        
    }
    
    //update the binary data before saving it
    private static void updateBlocks(HBD1PS1D hbd) {
        
        //this updates the linked sub-blocks in data attribute
        //but does not update the main-blocks
        
        hbd.textBlocks.sort((a,b) -> {
            return Integer.compare(
                    a.subBlock.parent.blockIndex,
                    b.subBlock.parent.blockIndex
            );
        });
        
        hbd.textBlocks.forEach(tb -> {
            try {
                //updates sub-block data
                tb.write();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        
        //updates the writeThisBlock for VeryFirst, StarZeros and H60
        hbd.updateBlocks();
    }
    
    //write the hbd file that was changed in RAM
    private static void save(HBD1PS1D hbd, File target) {
        hbd.write(target);
    }
    
    //builds the patched version with psxbuild tool
    //see https://github.com/cebix/psximager
    private static void psxbuild(DQFiles dqFiles, File psxbuildBin) throws IOException {
        dqFiles.psxbuild(psxbuildBin);
    }
    
    //to look into the subblocks
    private static void inspectWithGUI(HBD1PS1D hbd) {
        HBDFrame.showGUI(hbd);
    }
    
    private static void analyseTextBlocksV2(HBD1PS1D hbd) {
        
        System.out.println(hbd.textBlocks.size() + " text blocks");
        
        TextBlock firstScene = hbd.getTextBlock(26046, 13);
        
        List<TextBlock> tbs = Arrays.asList(firstScene);
        
        for(TextBlock textBlock : tbs) {
            
            System.out.println(textBlock.getHexID());
            
            byte[] dBlock = textBlock.dataDA;
            
            /*
            O(00000001) D1(000003D8) D2(00000464) DV[0002,0002,0002,000F,0010,0010]
            O -> indicates whether there will be data in the d-block
            
            D1 -> offset to D1
            D2 -> offset to D2
            
            DV -> "D Values"
            DV[0] is the number of sections
            DV[1] and DV[2] are always 0x02
            DV[3] is the total number of entries across sections
            DV[4] and DV[5] are always 0x10
            */
            int offsetD1 = Utils.bytesToInt(Utils.reverse(Arrays.copyOfRange(dBlock, 4,  8)));
            int offsetD2 = Utils.bytesToInt(Utils.reverse(Arrays.copyOfRange(dBlock, 8, 12)));
            
            int offset = 12;
            int i1 = Utils.bytesToShortLE(Arrays.copyOfRange(dBlock, offset, offset + 2)); offset += 2;
            int i2 = Utils.bytesToShortLE(Arrays.copyOfRange(dBlock, offset, offset + 2)); offset += 2;
            int i3 = Utils.bytesToShortLE(Arrays.copyOfRange(dBlock, offset, offset + 2)); offset += 2;
            int i4 = Utils.bytesToShortLE(Arrays.copyOfRange(dBlock, offset, offset + 2)); offset += 2;
            int i5 = Utils.bytesToShortLE(Arrays.copyOfRange(dBlock, offset, offset + 2)); offset += 2;
            int i6 = Utils.bytesToShortLE(Arrays.copyOfRange(dBlock, offset, offset + 2)); offset += 2;
            
            System.out.println("offsetD1=" + offsetD1);
            System.out.println("offsetD2=" + offsetD2);
            
            System.out.println("i1=" + i1);
            System.out.println("i2=" + i2);
            System.out.println("i3=" + i3);
            System.out.println("i4=" + i4);
            System.out.println("i5=" + i5);
            System.out.println("i6=" + i6);
            
            byte[] D1 = Arrays.copyOfRange(textBlock.subBlock.data, offsetD1, offsetD2);
            byte[] D2 = Arrays.copyOfRange(textBlock.subBlock.data, offsetD2, textBlock.endOffset);
            
            System.out.println("D1.length=" + D1.length);
            System.out.println("D2.length=" + D2.length);
            
            System.out.println("dBlock");
            System.out.println(Utils.toHexDump(dBlock, 16, true, false, null));
            
            System.out.println("D1");
            System.out.println(Utils.toHexDump(D1, 8, true, false, null));
            
            System.out.println("D2");
            System.out.println(Utils.toHexDump(D2, 8, true, false, null));
            
            
        }
        
    }
    
    private static void analyseTextBlocks(HBD1PS1D hbd) {
        
        Map<Integer, List<TextBlock>> id2tb = new HashMap<>();
        
        int lastLen = 0;
        
        Set<Integer> ids = new HashSet<>();
        for(TextBlock tb : hbd.textBlocks) {
            
            if(lastLen == tb.endOffset) {
                continue;
            }
            
            //the ones with the dataDA.length == 0 are lists of things: city names, or 男, no new line, just separated by {0000}
            /*
            if(tb.dataDA.length != 0) {
                System.out.println(tb);
                
                String str = hbd.decodeToString(hbd.getBits(tb.huffmanCode), tb.root);
                System.out.println(str);
                
                
                System.out.println(Utils.toHexDump(tb.dataDA, 8, true, false, null));
                System.out.println("===================");
            }
            */
            
            lastLen = tb.endOffset;
            
            id2tb.computeIfAbsent(tb.id, bb -> new ArrayList<>()).add(tb);
            
            
            //if(ids.contains(tb.b)) {
            //    throw new RuntimeException("id duplicate in " + tb);
            //}
            
            ids.add(tb.id);
        }
        
        //works
        for(Entry<Integer, List<TextBlock>> e : id2tb.entrySet()) {
            
            int sz = e.getValue().get(0).endOffset;
            
            for(TextBlock tb : e.getValue()) {
                if(tb.endOffset != sz) {
                    throw new RuntimeException("other end offset");
                }
            }
        }
        
        //is the first scene text
        TextBlock tb = hbd.getTextBlock(26046, 13);
        /*
        //here starts the first dialog but skip 1 bit
        List<Integer> p = Utils.find(Utils.hexStringToByteArray("F38CD759ECADFA2C44"), tb.dataCE);
        //found it
        byte[] range = Arrays.copyOfRange(tb.dataCE, 474, 474 + 10);
        System.out.println(Utils.bytesToHex(range));
        */
        
        //System.out.println(Utils.toHexDump(tb.dataDA, 8, true, false, null));
        
        //26046/13 has id 108
        
        //r9 = 4518D94D
        //r9 = 0518D94E @800F4E40
        //TextPointer tp = new TextPointer("4518D94D");
        //System.out.println(tp);
        
        //TextBlock tb = hbd.textBlocks.get(0);
        //System.out.println(tb);
        //System.out.println(tb.root.toStringTree());
        
        //8018D780 start of first scene huffman code
        String s = "8018D780";
        BigInteger start = new BigInteger(s, 16);
        
        HuffmanCode code = hbd.decode(hbd.getBits(tb.huffmanCode), tb.root);
        //System.out.println(code.getText());
        
        
        
        for(HuffmanChar hc : code) {
            
            String addr = start.add(new BigInteger(String.valueOf(hc.getByteIndex()))).toString(16).toUpperCase();
            
            String bitIndex = new BigInteger("" + hc.getStartBitInByte()).toString(16);
            
            String pointer = addr.substring(6,8) + addr.substring(4,6) + addr.substring(2,4) + bitIndex + "5";
            
            
            System.out.println(code.indexOf(hc) + " " + hc + " " + pointer
            );
            
            if(hc.isControlZero()) {
                System.out.println();
            }
        }
        
        /*
        @800F4E40=5AD91815
        @800F4E40=47D91815
        @800F4E40=09D91865
        */
        
        //634 ？ 1100110 0x4801 @3815-3821|477 (5);
        HuffmanChar otherChar = new HuffmanChar("日");
        //7f02 => scrolls down
        //code.set(630, otherChar);
        //code.set(631, otherChar);
        //code.add(609, otherChar);
        //code.add(609, otherChar);
        /*
        code.set(633, otherChar);
        code.set(639, otherChar);
        code.set(640, otherChar);
        code.set(641, otherChar);
        code.set(642, otherChar);
        code.set(643, otherChar);
        code.set(644, otherChar);
        code.set(645, otherChar);
        */
        
        for(HuffmanChar hc : code) {
            System.out.println(code.indexOf(hc) + " " + hc);
        }
        
        byte[] encoded = hbd.encode(code);
        
        //int cmp = Utils.compare(encoded, tb.huffmanCode);
        
        tb.huffmanCode = encoded;
        
        /*
        for(TextBlock tb2 : hbd.textBlocks) {
            try {
                tb2.write();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        */
        
        //int a = 0;
        
        //System.out.println("tree 1");
        //ParseNode root = hbd.parseTree(tb.huffmanTreeBytes);
        //System.out.println(root.toStringTree());
        
        //System.out.println(Utils.toHexDump(tb.dataCE, 8, true, false, null));
        //System.out.println(Utils.toHexDump(tb.subBlock.data, 8, true, false, null));
        //dictionary
        //System.out.println(Utils.toHexDump(tb.huffmanTreeBytes, 8, true, false, null));
        
        //1001111 => ど
        
        /*
        String allbits = "";
        for(byte b : tb.huffmanCode) {
            //for each byte take the bits
            String bits = Utils.toBits(b);
            //reverse it
            bits = Utils.reverse(bits);
            //add it to long bit sequence
            allbits += bits;
        }
        */
        
        //allbits = allbits.substring(1);
        
        //System.out.println(allbits);
        //String str = hbd.decode(allbits, tb.huffmanTreeBytes);
        //System.out.println(str);
        
        //System.out.println("== with tree ===========================");
        
        //byte[] huffmanTreeBytes = hbd.toHuffmanTreeBytes(root);
        
        //for(int i = 0; i < huffmanTreeBytes.length; i += 2) {
        //    String left = Utils.bytesToHex(Arrays.copyOfRange(tb.huffmanTreeBytes, i, i + 2));
        //    String right = Utils.bytesToHex(Arrays.copyOfRange(huffmanTreeBytes, i, i + 2));
        //    System.out.println("[" + i + "] " + left + hbd);
        //}
        
        //System.out.println("tree 2");
        //ParseNode root2 = hbd.parseTree(huffmanTreeBytes);
        //System.out.println(root2.toStringTree());
        
        //System.out.println("tree 3");
        //ParseNode root3 = hbd.parseTree(hbd.toHuffmanTreeBytes(root2));
        //System.out.println(root3.toStringTree());
        
        //String str2 = hbd.decode(allbits, root2);
        //System.out.println(str2);
        
        //System.out.println("=================");
        
        //String str = hbd.decode(allbits, huffmanTreeBytes);
        //System.out.println(str);
        
        //tb.huffmanTreeBytes = huffmanTreeBytes;
    }
    
    private static void translationPreparation(HBD1PS1D hbd, DQFiles dqFiles) throws IOException {
        hbd.translationPreparation(dqFiles.translationFolderWrite);
    }
    
    private static void translationEmbedding(HBD1PS1D hbd, PSEXE psexe, DQFiles dqFiles) throws IOException {
        TranslationEmbedding embedding = new TranslationEmbedding();
        embedding.embed(dqFiles.translationFolderRead, hbd, psexe);
    }
    
    private static void translationEmbeddingV2(HBD1PS1D hbd, PSEXE psexe, DQFiles dqFiles) throws IOException {
        TranslationEmbedding embedding = new TranslationEmbedding();
        embedding.embedV2(dqFiles.translationFolderRead, hbd, psexe);
    }
    
    //mass decompression: check if everything works
    private static void lzsEvaluation(HBD1PS1D hbd) throws IOException {
        
        int all = hbd.getStarZerosSubBlocks().size();
        List<StarZerosSubBlock> compressedList = hbd.getStarZerosSubBlocksCompressed();
        
        //compressed: 5967/23828
        System.out.println("compressed: " + compressedList.size() + "/" + all);
        
        File lzsEvaluationFolder = new File("lzsEvaluation");
        lzsEvaluationFolder.mkdir();
        
        CSVPrinter p = CSVFormat.DEFAULT.print(new File(lzsEvaluationFolder, "results.csv"), StandardCharsets.UTF_8);
        
        p.printRecord(
                "path",
                "sizeCompressed",
                "sizeUncompressed",
                "type",
                "exception",
                "exceptionMessage",
                "exceptionClass",
                "duration",
                "stopReason",
                "offsetCompressed",
                "offsetDecompressed",
                "offsetBuffer",
                "compressedRead",
                "decompressedRead",
                "compressedReadDiff",
                "uncompressedReadDiff",
                "totalDiff"
        );
        
        //takes ca. 40 seconds
        for(StarZerosSubBlock sb : compressedList) {
            
            DecompressResult result = DQLZS.decompress(sb.data, sb.sizeUncompressed);
            
            p.printRecord(sb.getPath(),
                    sb.size,
                    sb.sizeUncompressed,
                    sb.type,
                    result.exception != null,
                    result.exception != null ? result.exception.getMessage() : "",
                    result.exception != null ? result.exception.getClass().getSimpleName() : "",
                    result.getDuration(),
                    result.stopReason,
                    result.offsetCompressed,
                    result.offsetDecompressed,
                    result.offsetBuffer,
                    result.offsetCompressed == sb.size,
                    result.offsetDecompressed == sb.sizeUncompressed,
                    sb.size - result.offsetCompressed,
                    sb.sizeUncompressed - result.offsetDecompressed,
                    (sb.size - result.offsetCompressed) + (sb.sizeUncompressed - result.offsetDecompressed)
            );
        }
        
        p.close();
        
    }
    
    private static void comparisonW7Q7() throws IOException {
        File hbdEnFile = new File("../../Dragon Warrior VII (Disc 1)/dq7-psxrip/HBD1PS1D.W71");
        File hbdJpFile = new File("../../Dragon Quest VII - Eden no Senshitachi (Japan) (Disc 1)/dq7-psxrip/HBD1PS1D.Q71");
        
        HBDComparison comparison = new HBDComparison(hbdEnFile, hbdJpFile);
        
        comparison.compare();
        
        //comparison.inspectEn();
        //comparison.inspectJp();
    }
    
    private static void timExtraction(HBD1PS1D hbd) throws IOException {
        
        File folder = new File("TIMs");
        folder.mkdir();
        
        for(StarZerosSubBlock sb : hbd.getStarZerosSubBlocks(8)) {
            
            byte[] data;
            if(sb.compressed) {
                data = DQLZS.decompress(sb.data, sb.sizeUncompressed).data;
            } else {
                data = sb.data;
            }
            
            File f = new File(folder, sb.parent.blockIndex + "-" + sb.blockIndex + ".TIM");
            
            FileUtils.writeByteArrayToFile(f, data);
        }
        
    }
    
    private static void h60010108Blocks(HBD1PS1D hbd) {
        
        for(H60010108 h60 : hbd.h60010108List) {
            
            System.out.println();
            System.out.println();
            System.out.println(h60);
            System.out.println(Utils.toHexString(h60.data));
            
            /*
            DecompressResult result = DQLZS.decompress(h60.data, 10000);
            System.out.println(result);
            System.out.println(Utils.toHexString(result.data));
            */
            
            /*
            List<String> charsets = Arrays.asList(
                //"JIS_X0201",
                //"JIS_X0212-1990",
                "Shift_JIS",
                "x-JIS0208",
                "x-JISAutoDetect",
                //"x-SJIS_0213", //same like Shift_JIS
                "EUC-JP",
                "ISO-2022-JP",
                "ISO-2022-JP-2",
                "UTF-8"
            );
            
            for(String charset : charsets) {
                String text = new String(h60.data, Charset.forName(charset));
                
                System.out.println();
                System.out.println();
                System.out.println(charset);
                System.out.println(text);
            }
            */
        }
        
    }
    
    private static void veryFirstBlock(HBD1PS1D hbd) throws IOException {
        //DecompressResult result = DQLZS.decompress(hbd.veryFirstBlock.data, 8000, true);
        //System.out.println(result);
        
        File f = new File("../veryFirstBlock.bin");
        FileUtils.writeByteArrayToFile(f, hbd.veryFirstBlock.data);
        
        //JIS_X0201
        //JIS_X0212-1990
        //Shift_JIS
        //x-JIS0208
        //x-JISAutoDetect
        //x-SJIS_0213
        //EUC-JP
        //ISO-2022-JP
        //ISO-2022-JP-2
        List<String> charsets = Arrays.asList(
                "JIS_X0201",
                "JIS_X0212-1990",
                "Shift_JIS",
                "x-JIS0208",
                "x-JISAutoDetect",
                "x-SJIS_0213",
                "EUC-JP",
                "ISO-2022-JP",
                "ISO-2022-JP-2",
                "UTF-8"
        );
        for(String charset : Charset.availableCharsets().keySet()) {
            
            byte[] b = Arrays.copyOfRange(hbd.veryFirstBlock.data, 1024, 2048);
            
            String text = new String(b, Charset.forName(charset));
            
            //text.getBytes(charset);
            
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println(charset + ": " + text);
        }
    }
    
    private static void fontImages(HBD1PS1D hbd) throws IOException {
        
        File folder = new File("fontImages");
        folder.mkdir();
        
        StarZerosSubBlock sb = hbd.getSubBlock(26023, 1);
        
        int l = sb.data.length;
        int w = 128;
        int h = l / w;
        
        System.out.println(sb.data.length);
        
        System.out.println(Utils.toHexDump(sb.data, w, true, false, null));
        
        BufferedImage img1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        Set<Integer> values = new HashSet<>();
        
        Map<Integer, Integer> value2count = new HashMap<>();
        
        //byte[] decomp = DQLZS.decompress(sb.data, 10000).data;
        
        for(int i = 0; i < h; i++) {
            for(int j = 0; j < w; j++) {
                
                int index = i * w + j;
                byte b = sb.data[i * w + j];
                int v = b & 0xff;
                String hex = Utils.bytesToHex(new byte[] { b });
                
                System.out.println("[" + index + "]" + " - " + hex + " - " + v);
                
                int count = value2count.computeIfAbsent(v, vv -> 0);
                value2count.put(v, count+1);
                
                if(v != 0) {
                    int a = 0;
                }
                
                values.add(v);
                
                img1.setRGB(j, i, new Color(v,v,v).getRGB());
            }
        }
        
        System.out.println(values);

        //List<Entry<Integer, Integer>> ll = new ArrayList<>(value2count.entrySet());
        //ll.sort(Entry.comparingByValue());
        //ll.forEach(e -> System.out.println(e));
        
        ImageIO.write(img1, "png", new File(folder, "01.png"));
    }
    
    private static void shiftjisToHex(String input) {
        try {
            System.out.println(input);
            for(String enc : Arrays.asList("Shift_JIS", "EUC-JP", "ISO-2022-JP")) {
                System.out.println("\t" + enc + ": " + Utils.toHexDump(input.getBytes(enc), 16, true, false, null));
            }
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static void hexToShiftJis(HBD1PS1D hbd, int w, int... data) {
        
        byte[] b = new byte[data.length];
        for(int i = 0; i < data.length; i++) {
            b[i] = (byte) data[i];
        }
        
        System.out.println(Utils.toHexDump(b, w, true, false, hbd.reader.sjishort2char));
    }
    
    //==========================================================================
    //deprecated
    
    //can not work, is huffman coded
    @Deprecated
    private static void findShiftJIS(HBD1PS1D hbd, String text) {
        List<StarZerosSubBlock> list = hbd.getStarZerosSubBlocks();
        //list.removeIf(sb -> HBD1PS1D.imageTypes.contains(sb.type) || HBD1PS1D.qqesTypes.contains(sb.type));
        
        list = hbd.distinct(list);
        
        Charset charset = Charset.forName("Shift_JIS");

        byte[] byteArray;
        try {
            byteArray = String.valueOf(text).getBytes(charset);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        
        System.out.println("pattern: " + Utils.toHexString(byteArray));
        
        for(StarZerosSubBlock sb : list) {
            
            byte[] data;
            if(sb.compressed) {
                data = DQLZS.decompress(sb.data, sb.sizeUncompressed).data;
            } else {
                data = sb.data;
            }
         
            List<Integer> found = Utils.find(Arrays.asList(byteArray), 0, data);
            
            if(!found.isEmpty()) {
                System.out.println("[" + list.indexOf(sb) + "/"+list.size() + "] " + sb);
                System.out.println("\t" + found.size());
            }
        }
    }
    
    //can not work, is huffman coded
    @Deprecated
    private static void findText(HBD1PS1D hbd, String text) {
        List<StarZerosSubBlock> list = hbd.getStarZerosSubBlocks();
        list.removeIf(sb -> HBD1PS1D.imageTypes.contains(sb.type) || HBD1PS1D.qqesTypes.contains(sb.type));
        
        list = hbd.distinct(list);
        
        for(StarZerosSubBlock sb : list) {
            
            byte[] data;
            if(sb.compressed) {
                data = DQLZS.decompress(sb.data, sb.sizeUncompressed).data;
            } else {
                data = sb.data;
            }
            
            String txt = new String(data, StandardCharsets.US_ASCII);
            int index = txt.indexOf(text);
            
            if(index != -1) {
                System.out.println("[" + list.indexOf(sb) + "/"+list.size() + "] " + data.length + " bytes");
                System.out.println("\t" + index + ": " + txt.substring(index, Math.min(index + 200, txt.length())));
            }
        }
    }
    
    //maybe we find the dialog that comes in chapter 1 at the beginning
    //can not work, is huffman coded
    @Deprecated
    private static void findJapaneseText(HBD1PS1D hbd) {
        /*
        どうした？ <Heroname>。
        もう降参かい？
        そうだな。今日は　このくらいに
        しておこう……。
        私の役目は　はやく　お前を
        一人前に　育てることだが
        あせっても　しかたあるまい。
        ちて　もどるとするか。
        <Heroname>も　家で　ゆっくり
        休むといいだろう。
        勇者さま　勇者さま……。
        勇者さま　どうか　たすけて……。
        */
        //could not find words of the first dialog in 46-blocks
        
        //start menu
        //ぼうけんをする
        //ぼうけんのしょをつくる
        //ぼうけんのしょをけす
        
        //"ぼうけん" not found
        
        //メモリーカードをチェックしています。

        //"メモリーカード" (memory card) comes in type=46 often (e.g. in 373/6, 501/12)
        //but not "メモリーカードを" 
        //there is also the "スロット" from the menu
        
        //but for example
        //屋へ can be found in the 46 blocks
        
        //https://en.wikipedia.org/wiki/Category:Encodings_of_Japanese
        
        //Charset.availableCharsets().forEach((a,b) -> System.out.println(a));
        //JIS_X0201
        //JIS_X0212-1990
        //Shift_JIS
        //x-JIS0208
        //x-JISAutoDetect
        //x-SJIS_0213
        //EUC-JP
        //ISO-2022-JP
        //ISO-2022-JP-2
        
        
        
        
        
        
        
        
        byte[] doushitaUtf8 = "どうした".getBytes(StandardCharsets.UTF_8);
        byte[] doushitaSjis = "どうした".getBytes(Charset.forName("SHIFT-JIS"));
        byte[] doushitaJis = "どうした".getBytes(Charset.forName("JIS"));
        byte[] doushitaEucJp = "どうした".getBytes(Charset.forName("EUC-JP"));
        
        //seems not to work
        byte[] doushitaX0201 = "どうした".getBytes(Charset.forName("JIS_X0201"));
        byte[] doushitaX2012_1990 = "どうした".getBytes(Charset.forName("JIS_X0212-1990"));
        
        //correct: https://www.utf8-chartable.de/unicode-utf8-table.pl?start=12288&names=-&htmlent=1
        System.out.println(Utils.toHexString(doushitaUtf8));
        System.out.println(Utils.toHexString(doushitaSjis));
        System.out.println(Utils.toHexString(doushitaJis));
        System.out.println(Utils.toHexString(doushitaX0201));
        System.out.println(Utils.toHexString(doushitaEucJp));
        
        List patterns = new ArrayList<>();
        patterns.add(doushitaUtf8);
        patterns.add(doushitaSjis);
        patterns.add(doushitaJis);
        patterns.add(doushitaEucJp);
        //patterns.add("メモリーカード".getBytes(Charset.forName("SHIFT-JIS")));
        
        List<StarZerosSubBlock> toSearch = new ArrayList<>();
        toSearch.addAll(hbd.getStarZerosSubBlocks(32));
        toSearch.addAll(hbd.getStarZerosSubBlocks(39));
        toSearch.addAll(hbd.getStarZerosSubBlocks(40));
        toSearch.addAll(hbd.getStarZerosSubBlocks(42));
        toSearch.addAll(hbd.getStarZerosSubBlocks(44));
        toSearch.addAll(hbd.getStarZerosSubBlocks(45));
        toSearch.addAll(hbd.getStarZerosSubBlocks(46));
        
        toSearch = hbd.distinct(toSearch);
        
        for(StarZerosSubBlock sb : toSearch) {
            byte[] data;
            if(sb.compressed) {
                data = DQLZS.decompress(sb.data, sb.sizeUncompressed).data;
            } else {
                data = sb.data;
            }
            
            for(Object patternObj : patterns) {
                List<Integer> found = Utils.find((byte[]) patternObj, data);
                if(!found.isEmpty()) {
                    System.out.println(sb);
                }
            }
        }
    }
    
    //can not work, is huffman coded
    @Deprecated
    private static void findJapaneseTextV2(HBD1PS1D hbd) {
        /*
        どうした？ <Heroname>。
        もう降参かい？
        そうだな。今日は　このくらいに
        しておこう……。
        私の役目は　はやく　お前を
        一人前に　育てることだが
        あせっても　しかたあるまい。
        ちて　もどるとするか。
        <Heroname>も　家で　ゆっくり
        休むといいだろう。
        勇者さま　勇者さま……。
        勇者さま　どうか　たすけて……。
        */
        
        List<String> japaneseWords = Arrays.asList(
                "どうした",
                "どう",
                "降参",
                "一人",
                "勇者",
                "家で",
                "休むとい",
                "育てることだが"
                //"メモリーカード" //works
        );
        
        List<String> charsets = Arrays.asList(
                //"JIS_X0201",
                //"JIS_X0212-1990",
                "Shift_JIS",
                "x-JIS0208",
                "x-JISAutoDetect",
                //"x-SJIS_0213", //same like Shift_JIS
                "EUC-JP",
                "ISO-2022-JP",
                "ISO-2022-JP-2",
                "UTF-8"
        );
        
        Map<String, Map<String, List<byte[]>>> word2charset2byteArrayList = new HashMap<>();
        
        for(String japaneseWord : japaneseWords) {
        
            Map<String, List<byte[]>> charset2byteArrayList = new HashMap<>();
            word2charset2byteArrayList.put(japaneseWord, charset2byteArrayList);
            
            for(String charsetName : charsets) {
                
                for(String reversed : Arrays.asList("", " (reversed)")) {
                
                    List<byte[]> byteArrayList = new ArrayList<>();
                    for(int i = 0; i < japaneseWord.length(); i++) {
                        char japaneseCharacter = japaneseWord.charAt(i);

                        Charset charset = Charset.forName(charsetName);

                        byte[] byteArray;
                        try {
                            byteArray = String.valueOf(japaneseCharacter).getBytes(charset);
                        } catch(Exception e) {
                            //System.out.println(charsetName + " " + e.getMessage());
                            break;
                        }

                        if(reversed.equals(" (reversed)")) {
                            byteArray = Utils.reverse(byteArray);
                        }
                        
                        byteArrayList.add(byteArray);
                    }

                    if(!byteArrayList.isEmpty()) {
                        charset2byteArrayList.put(charsetName + reversed, byteArrayList);
                    }
                }
            }
        }
        
        System.out.println(word2charset2byteArrayList);
        
        int maxSkip = 8;
        
        Map<String, List<Integer>> foundMap = new HashMap<>();
        
        List<StarZerosSubBlock> list = hbd.getStarZerosSubBlocks();
        list.removeIf(sb -> HBD1PS1D.imageTypes.contains(sb.type) || HBD1PS1D.qqesTypes.contains(sb.type));
        
        list = hbd.distinct(list);
        
        for(StarZerosSubBlock sb : list) {
            
            byte[] data;
            if(sb.compressed) {
                data = DQLZS.decompress(sb.data, sb.sizeUncompressed).data;
            } else {
                data = sb.data;
            }
            
            System.out.println("[" + list.indexOf(sb) + "/"+list.size() + "] " + data.length + " bytes (" + foundMap.size() + " found)");
            
            for(int skip = 0; skip <= maxSkip; skip++) {
                
                for(Entry word2charsetMap : word2charset2byteArrayList.entrySet()) {
                    
                    String word = (String) word2charsetMap.getKey();
                    Map<String, List<byte[]>> charset2byteArrayMap = (Map<String, List<byte[]>>) word2charsetMap.getValue();
                    
                    for(Entry charset2list : charset2byteArrayMap.entrySet()) {
                        String charset = (String) charset2list.getKey();
                        List<byte[]> byteArrayList = (List<byte[]>) charset2list.getValue();
                        
                        List<Integer> found = Utils.find(byteArrayList, skip, data);
                        if(!found.isEmpty()) {
                            foundMap.put(Arrays.asList(word, charset, "skip=" + skip, sb.getPath(), sb.type).toString(), found);
                        }
                    }
                }
            }
        }
        
        System.out.println("found: " + foundMap.size());
        foundMap.forEach((a,b) -> System.out.println(a + " found at " + b));
    }
    
    //can not work, is huffman coded
    @Deprecated
    private static void findJapaneseTextV3SingleByte(HBD1PS1D hbd) {
        /*
        どうした？ <Heroname>。
        もう降参かい？
        そうだな。今日は　このくらいに
        しておこう……。
        私の役目は　はやく　お前を
        一人前に　育てることだが
        あせっても　しかたあるまい。
        ちて　もどるとするか。
        <Heroname>も　家で　ゆっくり
        休むといいだろう。
        勇者さま　勇者さま……。
        勇者さま　どうか　たすけて……。
        */
        
        //in search for 勇者さま　勇者さま
        
        List<StarZerosSubBlock> list = hbd.getStarZerosSubBlocks();
        list.removeIf(sb -> HBD1PS1D.imageTypes.contains(sb.type) || HBD1PS1D.qqesTypes.contains(sb.type));
        
        list = hbd.distinct(list);
        
        Set<String> found = new HashSet<>();
        
        Map<Integer, Integer> type2count = new HashMap<>();
        Map<String, Integer> hex2count = new HashMap<>();
        
        for(StarZerosSubBlock sb : list) {
            
            byte[] data;
            if(sb.compressed) {
                data = DQLZS.decompress(sb.data, sb.sizeUncompressed).data;
            } else {
                data = sb.data;
            }
            
            System.out.println("[" + list.indexOf(sb) + "/"+list.size() + "] " + data.length + " bytes (" + found.size() + " found)");
            
            //０１２３４５６７８ ９  １０
            //勇者さま　勇者さま ... ...
            //if every letter is a byte
            for(int i = 0; i < data.length - 15; i++) {
                
                if(data[i] == data[i+5] && 
                   data[i+1] == data[i+6] &&
                   data[i+2] == data[i+7] &&
                   data[i+3] == data[i+8] &&
                   data[i+9] == data[i+10] &&
                   new HashSet<>(Arrays.asList(data[i], data[i+1], data[i+2], data[i+3])).size() == 4 &&
                   data[i] != 0 && data[i+1] != 0 && data[i+2] != 0 && data[i+3] != 0 &&
                        
                   (data[i] & 0xff) > (data[i+2] & 0xff) && (data[i+1] & 0xff) > (data[i+3] & 0xff)) { //kanji > hiragana
                    
                    byte[] sub = Arrays.copyOfRange(data, i, i+15);
                    
                    found.add(Utils.toHexString(sub) + " at " + sb.getPath() + " type=" + sb.type + " at " + i);
                    
                    int count = type2count.computeIfAbsent(sb.type, vv -> 0);
                    type2count.put(sb.type, count+1);
                    
                    String id = Utils.toDecString(sub) + " type=" + sb.type;
                    
                    int count2 = hex2count.computeIfAbsent(id, vv -> 0);
                    hex2count.put(id, count2+1);
                }
            }
            
        }
        
        System.out.println(found.size() + " found");
        found.forEach(f -> System.out.println(f));
    
        //40 maybe multi data
        //39 has these CCC (0x00434343) patterns often
        //46 contains error messages and japanese text
        
        //{40=749, 39=332, 46=239, 42=42, 44=26, 31=12, 23=18}
        System.out.println(type2count);
        
        List<Entry<String, Integer>> ll = new ArrayList<>(hex2count.entrySet());
        ll.sort(Entry.comparingByValue());
        ll.forEach(e -> System.out.println(e));
    }
    
    //can not work, is huffman coded
    @Deprecated
    private static void findJapaneseTextV3DoubleByte(HBD1PS1D hbd) {
        /*
        どうした？ <Heroname>。
        もう降参かい？
        そうだな。今日は　このくらいに
        しておこう……。
        私の役目は　はやく　お前を
        一人前に　育てることだが
        あせっても　しかたあるまい。
        ちて　もどるとするか。
        <Heroname>も　家で　ゆっくり
        休むといいだろう。
        勇者さま　勇者さま……。
        勇者さま　どうか　たすけて……。
        */
        
        //in search for 勇者さま　勇者さま
        
        List<StarZerosSubBlock> list = hbd.getStarZerosSubBlocks();
        list.removeIf(sb -> HBD1PS1D.imageTypes.contains(sb.type) || HBD1PS1D.qqesTypes.contains(sb.type));
        
        list = hbd.distinct(list);
        
        Set<String> found = new HashSet<>();
        
        Map<Integer, Integer> type2count = new HashMap<>();
        Map<String, Integer> hex2count = new HashMap<>();
        
        for(StarZerosSubBlock sb : list) {
            
            byte[] data;
            if(sb.compressed) {
                data = DQLZS.decompress(sb.data, sb.sizeUncompressed).data;
            } else {
                data = sb.data;
            }
            
            System.out.println("[" + list.indexOf(sb) + "/"+list.size() + "] " + data.length + " bytes (" + found.size() + " found)");
            
            //０１ ２３ ４５ ６７ ８９   10-11 12-13 14-15 16-17  18-19 20-21
            //勇   者  さ   ま　 space  勇    者    さ     ま     ...   ...
            //if every letter are two bytes
            for(int i = 0; i < data.length - 24; i++) {
                
                if(data[i+0] == data[i+10] && data[i+1] == data[i+11] &&
                   data[i+2] == data[i+12] && data[i+3] == data[i+13] && 
                   data[i+4] == data[i+14] && data[i+5] == data[i+15] && 
                   data[i+6] == data[i+16] && data[i+7] == data[i+17] &&
                   data[i+6] == data[i+16] && data[i+7] == data[i+17] &&
                        
                   data[i+18] == data[i+20] && data[i+19] == data[i+21] && //...
                   (new HashSet<>(Arrays.asList(
                           Utils.bytesToShortLE(new byte[] { data[i+0], data[i+1]}),
                           Utils.bytesToShortLE(new byte[] { data[i+2], data[i+3]}),
                           Utils.bytesToShortLE(new byte[] { data[i+4], data[i+5]}),
                           Utils.bytesToShortLE(new byte[] { data[i+6], data[i+7]})
                   
                   )).size() == 4) &&
                        
                   ! (Arrays.asList(
                           Utils.bytesToShortLE(new byte[] { data[i+0], data[i+1]}),
                           Utils.bytesToShortLE(new byte[] { data[i+2], data[i+3]}),
                           Utils.bytesToShortLE(new byte[] { data[i+4], data[i+5]}),
                           Utils.bytesToShortLE(new byte[] { data[i+6], data[i+7]})
                   
                   ).contains((short)0))
                        
                        
                        ) { 
                    
                    byte[] sub = Arrays.copyOfRange(data, i, i+24);
                    
                    found.add(Utils.toHexString(sub) + " at " + sb.getPath() + " type=" + sb.type + " at " + i);
                    
                    int count = type2count.computeIfAbsent(sb.type, vv -> 0);
                    type2count.put(sb.type, count+1);
                    
                    String id = Utils.toDecString(sub) + " | " + Utils.toHexString(sub, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2) + " type=" + sb.type + " path=" + sb.getPath() + ", address=" + i;
                    
                    int count2 = hex2count.computeIfAbsent(id, vv -> 0);
                    hex2count.put(id, count2+1);
                }
            }
            
        }
        
        System.out.println(found.size() + " found");
        found.forEach(f -> System.out.println(f));
    
        //40 maybe multi data
        //39 has these CCC (0x00434343) patterns often
        //46 contains error messages and japanese text
        
        //{40=749, 39=332, 46=239, 42=42, 44=26, 31=12, 23=18}
        System.out.println(type2count);
        
        List<Entry<String, Integer>> ll = new ArrayList<>(hex2count.entrySet());
        ll.sort(Entry.comparingByValue());
        ll.forEach(e -> System.out.println(e));
    }
    
    //can not work, is huffman coded
    @Deprecated
    private static void findJapaneseTextV3FourByte(HBD1PS1D hbd) {
        /*
        どうした？ <Heroname>。
        もう降参かい？
        そうだな。今日は　このくらいに
        しておこう……。
        私の役目は　はやく　お前を
        一人前に　育てることだが
        あせっても　しかたあるまい。
        ちて　もどるとするか。
        <Heroname>も　家で　ゆっくり
        休むといいだろう。
        勇者さま　勇者さま……。
        勇者さま　どうか　たすけて……。
        */
        
        //in search for 勇者さま　勇者さま
        
        List<StarZerosSubBlock> list = hbd.getStarZerosSubBlocks();
        list.removeIf(sb -> HBD1PS1D.imageTypes.contains(sb.type) || HBD1PS1D.qqesTypes.contains(sb.type));
        
        list = hbd.distinct(list);
        
        Set<String> found = new HashSet<>();
        
        Map<Integer, Integer> type2count = new HashMap<>();
        Map<String, Integer> hex2count = new HashMap<>();
        
        for(StarZerosSubBlock sb : list) {
            
            byte[] data;
            if(sb.compressed) {
                data = DQLZS.decompress(sb.data, sb.sizeUncompressed).data;
            } else {
                data = sb.data;
            }
            
            System.out.println("[" + list.indexOf(sb) + "/"+list.size() + "] " + data.length + " bytes (" + found.size() + " found)");
            
            int letterCount = 11;
            int size = letterCount * 4;
            
            //勇   者  さ   ま　 space  勇    者    さ     ま     ...   ...
            //if every letter are four bytes
            for(int i = 0; i < data.length - size; i++) {
                
                int j = i;
                
                int kanji1A = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int kanji2A = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int saA = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int maA = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int space = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int kanji1B = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int kanji2B = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int saB = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int maB = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int dots1 = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int dots2 = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                int dot = Utils.bytesToIntLE(Arrays.copyOfRange(data, j, j+4));
                j += 4;
                
                
                if(kanji1A == kanji1B && 
                   kanji2A == kanji2B &&
                   saA == saB && 
                   maA == maB &&
                   dots1 == dots2 && 
                   (new HashSet<>(Arrays.asList(
                           kanji1A,
                           kanji2A,
                           saA,
                           maA
                   )).size() == 4) &&
                        
                   ! (Arrays.asList(
                           kanji1A,
                           kanji2A,
                           saA,
                           maA
                   
                   ).contains(0))
                        
                        ) { 
                    
                    byte[] sub = Arrays.copyOfRange(data, i, i+size);
                    
                    found.add(Utils.toHexString(sub) + " at " + sb.getPath() + " type=" + sb.type + " at " + i);
                    
                    int count = type2count.computeIfAbsent(sb.type, vv -> 0);
                    type2count.put(sb.type, count+1);
                    
                    String id = Utils.toDecString(sub) + " | " + Utils.toHexString(sub, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4) + " type=" + sb.type + " path=" + sb.getPath() + ", address=0x" + Integer.toHexString(i);
                    
                    int count2 = hex2count.computeIfAbsent(id, vv -> 0);
                    hex2count.put(id, count2+1);
                }
                
            }
            
        }
        
        System.out.println(found.size() + " found");
        found.forEach(f -> System.out.println(f));
    
        System.out.println(type2count);
        
        List<Entry<String, Integer>> ll = new ArrayList<>(hex2count.entrySet());
        ll.sort(Entry.comparingByValue());
        ll.forEach(e -> System.out.println(e));
    }
    
    //can not work, is huffman coded
    @Deprecated
    private static void findJapaneseTextV3TwoByte(HBD1PS1D hbd) {
        /*
        どうした？ <Heroname>。
        もう降参かい？
        そうだな。今日は　このくらいに
        しておこう……。
        私の役目は　はやく　お前を
        一人前に　育てることだが
        あせっても　しかたあるまい。
        ちて　もどるとするか。
        <Heroname>も　家で　ゆっくり
        休むといいだろう。
        勇者さま　勇者さま……。
        勇者さま　どうか　たすけて……。
        */
        
        //in search for 勇者さま　勇者さま
        
        List<StarZerosSubBlock> list = hbd.getStarZerosSubBlocks();
        list.removeIf(sb -> HBD1PS1D.imageTypes.contains(sb.type) || HBD1PS1D.qqesTypes.contains(sb.type));
        
        list = hbd.distinct(list);
        
        Set<String> found = new HashSet<>();
        
        Map<Integer, Integer> type2count = new HashMap<>();
        Map<String, Integer> hex2count = new HashMap<>();
        
        for(StarZerosSubBlock sb : list) {
            
            byte[] data;
            if(sb.compressed) {
                data = DQLZS.decompress(sb.data, sb.sizeUncompressed).data;
            } else {
                data = sb.data;
            }
            
            System.out.println("[" + list.indexOf(sb) + "/"+list.size() + "] " + data.length + " bytes (" + found.size() + " found)");
            
            int letterCount = 11;
            int size = letterCount * 2;
            
            //勇   者  さ   ま　 space  勇    者    さ     ま     ...   ...
            //if every letter are two bytes
            for(int i = 0; i < data.length - size; i++) {
                
                int j = i;
                
                int kanji1A = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int kanji2A = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int saA = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int maA = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int space = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int kanji1B = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int kanji2B = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int saB = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int maB = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int dots1 = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int dots2 = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                j += 2;
                
                int dot = Utils.bytesToShortLE(Arrays.copyOfRange(data, j, j+2));
                //j += 2;
                
                
                if(kanji1A == kanji1B && 
                   kanji2A == kanji2B &&
                   saA == saB && 
                   maA == maB &&
                   dots1 == dots2 && 
                   (new HashSet<>(Arrays.asList(
                           kanji1A,
                           kanji2A,
                           saA,
                           maA
                   )).size() == 4) /*&&
                        
                   ! (Arrays.asList(
                           kanji1A,
                           kanji2A,
                           saA,
                           maA
                   
                   ).contains(0))*/
                        
                        ) { 
                    
                    byte[] sub = Arrays.copyOfRange(data, i, i+size);
                    
                    found.add(Utils.toHexString(sub) + " at " + sb.getPath() + " type=" + sb.type + " at " + i);
                    
                    int count = type2count.computeIfAbsent(sb.type, vv -> 0);
                    type2count.put(sb.type, count+1);
                    
                    String id = Utils.toDecString(sub) + " | " + Utils.toHexString(sub, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2) + " type=" + sb.type + " path=" + sb.getPath() + ", address=" + i;
                    
                    int count2 = hex2count.computeIfAbsent(id, vv -> 0);
                    hex2count.put(id, count2+1);
                }
                
            }
            
        }
        
        System.out.println(found.size() + " found");
        found.forEach(f -> System.out.println(f));
    
        System.out.println(type2count);
        
        List<Entry<String, Integer>> ll = new ArrayList<>(hex2count.entrySet());
        ll.sort(Entry.comparingByValue());
        ll.forEach(e -> System.out.println(e));
    }
    
    //can not work, is huffman coded
    @Deprecated
    private static void findJapaneseTextV4(HBD1PS1D hbd) {
        
        Map<String, Integer> m = new HashMap<>();
        
        Map<Integer, String> pos2jp = new HashMap<>();
        pos2jp.put(0, "ぁ");
        pos2jp.put(1, "あ");
        pos2jp.put(2, "ぃ");
        pos2jp.put(3, "い");
        pos2jp.put(4, "ぅ");
        pos2jp.put(5, "う");
        pos2jp.put(6, "ぇ");
        pos2jp.put(7, "え");
        pos2jp.put(8, "ぉ");
        pos2jp.put(9, "お");
        pos2jp.put(10, "か");
        pos2jp.put(11, "が");
        pos2jp.put(12, "き");
        pos2jp.put(13, "ぎ");
        pos2jp.put(14, "く");
        pos2jp.put(15, "ぐ");
        pos2jp.put(16, "け");
        pos2jp.put(17, "げ");
        pos2jp.put(18, "こ");
        pos2jp.put(19, "ご");
        pos2jp.put(20, "さ");
        pos2jp.put(21, "ざ");
        pos2jp.put(22, "し");
        pos2jp.put(23, "じ");
        pos2jp.put(24, "す");
        pos2jp.put(25, "ず");
        pos2jp.put(26, "せ");
        pos2jp.put(27, "ぜ");
        pos2jp.put(28, "そ");
        pos2jp.put(29, "ぞ");
        pos2jp.put(30, "た");
        pos2jp.put(31, "だ");
        pos2jp.put(32, "ち");
        pos2jp.put(33, "ち");
        pos2jp.put(34, "っ");
        pos2jp.put(35, "つ");
        pos2jp.put(36, "づ");
        pos2jp.put(37, "て");
        pos2jp.put(38, "で");
        pos2jp.put(39, "と");
        pos2jp.put(40, "ど");
        pos2jp.put(41, "な");
        pos2jp.put(42, "に");
        pos2jp.put(43, "め");
        pos2jp.put(44, "ね");
        pos2jp.put(45, "の");
        pos2jp.put(46, "は");
        pos2jp.put(47, "ば");
        pos2jp.put(48, "ぱ");
        pos2jp.put(49, "ひ");
        pos2jp.put(50, "び");
        pos2jp.put(51, "ぴ");
        pos2jp.put(52, "ふ");
        pos2jp.put(53, "ぶ");
        pos2jp.put(54, "ぷ");
        pos2jp.put(55, "へ");
        pos2jp.put(56, "べ");
        pos2jp.put(57, "ぺ");
        pos2jp.put(58, "ほ");
        pos2jp.put(59, "ぼ");
        pos2jp.put(60, "ぽ");
        pos2jp.put(61, "ま");
        pos2jp.put(62, "み");
        pos2jp.put(63, "む");
        pos2jp.put(64, "め");
        pos2jp.put(65, "も");
        pos2jp.put(66, "ゃ");
        pos2jp.put(67, "や");
        pos2jp.put(68, "ゅ");
        pos2jp.put(69, "ゆ");
        pos2jp.put(70, "ょ");
        pos2jp.put(71, "よ");
        pos2jp.put(72, "ら");
        pos2jp.put(73, "り");
        pos2jp.put(74, "る");
        pos2jp.put(75, "れ");
        pos2jp.put(76, "ろ");
        pos2jp.put(77, "わ");
        pos2jp.put(78, "わ");
        pos2jp.put(79, "を");
        pos2jp.put(80, "ん");
        
        Map<String, String> hex2jp = new HashMap<>();
        
        
        for(StarZerosSubBlock sb : hbd.getStarZerosSubBlocks(40)) {
            System.out.println();
            System.out.println();
            
            
            for(int i = 0; i < sb.data.length - 1; i += 2) {
                
                byte[] pair = Arrays.copyOfRange(sb.data, i, i+2);
                String pairHex = Utils.toHexString(pair);
                
                
                String jp = "";
                if((pair[1] & 0xff) == 0x80) {
                    //String hex = Utils.toHexString(new byte[] { pair[0] });
                    //System.out.println(hex);
                    
                    int pos = pair[0] & 0xff;
                    jp = pos2jp.get(pos);
                    
                    if(jp == null) {
                        jp = "";
                    }
                    
                    
                    System.out.println(pairHex + " " + jp);
                    //int count = m.computeIfAbsent(hex, h -> 0);
                    //m.put(hex, count+1);
                }
                
                
                
            }
        }
        
        m.entrySet().stream().sorted(Entry.comparingByValue()).forEach(e -> System.out.println(e));
    }
   
}
