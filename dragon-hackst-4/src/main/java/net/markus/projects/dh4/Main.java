
package net.markus.projects.dh4;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.markus.projects.dh4.DQLZS.DecompressResult;
import net.markus.projects.dh4.data.StarZerosSubBlock;
import net.markus.projects.dh4.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Main entry point.
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        File hbdFile = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip/HBD1PS1D.Q41");
        
        HBD1PS1D hbd = load(hbdFile);
        
        //inspectWithGUI(hbd);
        //lzsEvaluation(hbd);
        findJapaneseText(hbd);
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
}
