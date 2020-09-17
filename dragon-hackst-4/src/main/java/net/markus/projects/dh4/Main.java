
package net.markus.projects.dh4;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
import net.markus.projects.dh4.data.H60010108;
import net.markus.projects.dh4.data.StarZerosSubBlock;
import net.markus.projects.dh4.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;

/**
 * Main entry point.
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        File hbdFile = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip/HBD1PS1D.Q41");
        
        HBD1PS1D hbd = load(hbdFile);
        
        inspectWithGUI(hbd);
        //h60010108Blocks(hbd);
        //lzsEvaluation(hbd);
        //findJapaneseText(hbd);
        //findJapaneseTextV2(hbd);
        //findJapaneseTextV3SingleByte(hbd);
        //findJapaneseTextV3DoubleByte(hbd);
        //timExtraction(hbd);
        //veryFirstBlock(hbd);
        //fontImages(hbd);
    }
    
    //loads data into RAM
    private static HBD1PS1D load(File file) throws IOException {
        
        HBD1PS1D hbd = new HBD1PS1D(new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip/HBD1PS1D.Q41"));
        
        //the number of 2048 is stated at 0x04
        hbd.correctBlockExtraction();
        
        //look into blocks: 60 01 01 80
        hbd.h60010180Extraction();
        //hbd.h60010180Analysis();
        
        //look into blocks: * 00 00 00
        hbd.starZerosExtraction();
        //hbd.starZerosAnalysis();
        
        return hbd;
    }
    
    //to look into the subblocks
    private static void inspectWithGUI(HBD1PS1D hbd) {
        HBDFrame.showGUI(hbd);
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
    
    //maybe we find the dialog that comes in chapter 1 at the beginning
    //use version 2
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
            //if every letter is a byte
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
                    
                    String id = Utils.toDecString(sub) + " | " + Utils.toHexString(sub, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2) + " type=" + sb.type;
                    
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
}
