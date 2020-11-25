package net.markus.projects.dh4;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.markus.projects.dh4.data.StarZerosSubBlock;
import net.markus.projects.dh4.util.SetUtility;

/**
 * To compare Dragon Quest 7 (Japanese) with Dragon Warrior 7 (English).
 */
public class HBDComparison {

    private File hbdEnFile;
    private File hbdJpFile;

    private HBD1PS1D hbdEn;
    private HBD1PS1D hbdJp;

    public HBDComparison(File hbdEnFile, File hbdJpFile) throws IOException {
        this.hbdEnFile = hbdEnFile;
        this.hbdJpFile = hbdJpFile;

        hbdEn = load(hbdEnFile);
        hbdJp = load(hbdJpFile);
    }

    private HBD1PS1D load(File file) throws IOException {

        HBD1PS1D hbd = new HBD1PS1D(file);

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

    public void compare() {
        Map<Integer, List<StarZerosSubBlock>> mapEn = hbdEn.getType2StarZerosSubBlocksMap();
        Map<Integer, List<StarZerosSubBlock>> mapJp = hbdJp.getType2StarZerosSubBlocksMap();
        
        System.out.println(mapEn.size() + " types vs " + mapJp.size() + " types");
        
        Set<Integer> keysEn = mapEn.keySet();
        
        for(Integer type : keysEn) {
            List<StarZerosSubBlock> enSbList = mapEn.get(type);
            List<StarZerosSubBlock> jpSbList = mapJp.get(type);
            
            boolean sizeDiffer = enSbList.size() != jpSbList.size();
            
            System.out.println("type " + type + ": " + enSbList.size() + " sub-blocks vs " + jpSbList.size() + " sub-blocks, " + (sizeDiffer ? "DIFF" : "") + " " + HBD1PS1D.getTypeName(type));
            
            int minSize = Math.min(enSbList.size(), jpSbList.size());
            
            enSbList.forEach(sb -> sb.calculateDataHash());
            jpSbList.forEach(sb -> sb.calculateDataHash());
            
            List<String> hashesEn = new ArrayList<>();
            List<String> hashesJp = new ArrayList<>();
            
            enSbList.forEach(sb -> hashesEn.add(sb.dataHash));
            jpSbList.forEach(sb -> hashesJp.add(sb.dataHash));
            
            Set<String> hashesSetEn = new HashSet<>(hashesEn);
            Set<String> hashesSetJp = new HashSet<>(hashesJp);
            
            Set<String> hashesEnRest = SetUtility.subtract(hashesSetEn, hashesSetJp);
            Set<String> hashesJpRest = SetUtility.subtract(hashesSetJp, hashesSetEn);
            
            System.out.println("\t" + hashesSetEn.size() + " distinct hashes vs " + hashesSetJp.size() + " distinct hashes");
            System.out.println("\t" + hashesEnRest.size() + " en no match vs " + hashesJpRest.size() + " jp no match");
            
            int a = 0;
        }
    }

    public void inspectEn() {
        HBDFrame.showGUI(hbdEn);
    }

    public void inspectJp() {
        HBDFrame.showGUI(hbdJp);
    }

    /*
        List<StarZerosSubBlock> enList = hbdEn.getStarZerosSubBlocks();
        List<StarZerosSubBlock> jpList = hbdJp.getStarZerosSubBlocks();
        
        //24.716 vs 24.710
        System.out.println(enList.size() + " vs " + jpList.size());
        
        int similar = 0;
        
        Map<Integer, Integer> type2count = new HashMap<>();
        
        int i = 0;
        int j = 0;
        for(; i < 24710; ) {
            StarZerosSubBlock en = enList.get(i);
            StarZerosSubBlock jp = jpList.get(j);
            
            
            if(!en.getPath().equals(jp.getPath())) {
                
                
                i++;
                
            } else {
                similar++;
                
                if(en.size != jp.size) {
                    System.out.println(en);
                    System.out.println(jp);
                    System.out.println("");
                    
                    int count = type2count.computeIfAbsent(en.type, t -> 0);
                    type2count.put(en.type, count + 1);
                }
                
                i++;
                j++;
            }
        }
        
        System.out.println(similar + " similar");
        
        //{1=2, 5=9, 6=17, 7=9, 13=2, 19=33, 22=1, 23=1908, 24=166, 25=37, 27=7, 29=5, 30=1480, 31=1502}
        //23=1908
        //31=1502
        //30=1480
        System.out.println(type2count);
     */
}
