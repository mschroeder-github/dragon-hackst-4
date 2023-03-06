package net.markus.projects.dq4h.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatData60010108Entry;
import net.markus.projects.dq4h.data.HeartBeatDataBinaryEntry;
import net.markus.projects.dq4h.data.HeartBeatDataEntry;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataScriptContent;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import net.markus.projects.dq4h.data.HuffmanCharacter;
import net.markus.projects.dq4h.data.ScriptStoreEntry;
import net.markus.projects.dq4h.data.VariableToDialogPointer;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileReader;
import net.markus.projects.dq4h.io.IOConfig;
import net.markus.projects.dq4h.io.Inspector;
import net.markus.projects.dq4h.util.MemoryUtility;

/**
 * With this class we inspect certain properties after data was imported.
 */
public class InspectImportedData {

    private DragonQuestBinary binary;

    public static void main(String[] args) throws IOException {

        InspectImportedData checkAfterImport = new InspectImportedData();
        checkAfterImport.importData();

        //checkAfterImport.inspectSectors();
        checkAfterImport.findSectorAddressInPsxExe();
        //checkAfterImport.scriptOf00C6();
        //checkAfterImport.checkReferredCharacters();
    }

    public void importData() throws IOException {
        File folder = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/");
        File inputFile = new File(folder, "dq4.bin");

        IOConfig config = new IOConfig();
        config.setTrace(false);

        //import =============================
        System.out.println("Importing " + inputFile);

        long begin = System.currentTimeMillis();

        DragonQuestBinaryFileReader binReader = new DragonQuestBinaryFileReader(config);
        binary = binReader.read(inputFile);

        long end = System.currentTimeMillis();

        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics() + ", " + binary);
    }

    //we can determine the sectors, I added it to HeartBeatDataReader
    public void inspectSectors() {

        //134: 23:46:64 rest:48 prop:0,640000 folder containing text id 006c | 23:48:18 rest:14 prop:0,186667 folder containing text id 0069 (cc muss 30 sein)
        //int a = 0;

        //for (int offset = a; offset <= a; offset++) {
        //may start at 00:02:00
        int startSector = binary.getDiskFile("HBD1PS1D.Q41").getStartSector();

        //int currentSector = startSector;
        for (HeartBeatDataEntry entry : binary.getHeartBeatData().getEntries()) {

            String str = "";
            
            if (entry instanceof HeartBeatDataFolderEntry) {
                
                HeartBeatDataFolderEntry folder = (HeartBeatDataFolderEntry) entry;

                List<HeartBeatDataTextContent> l = folder.getContents(HeartBeatDataTextContent.class);
                
                if (!l.isEmpty()) {
                    str = "text id " + l.get(0).getIdHex();
                }
                
                
                
                /*
                int sectors = folder.getOriginalNumberOfSectors();

                //int position = currentSector * 2048;
                
                String startSecHex = Inspector.toHex(Converter.intToBytesBE(currentSector)).toUpperCase();
                String endSecHex = Inspector.toHex(Converter.intToBytesBE(currentSector + sectors)).toUpperCase();
                String sizeHex = Inspector.toHex(Converter.intToBytesBE(sectors)).toUpperCase();
                
                //thus, 04D | 1A23A is 
                //     size   start

                //3 for len and 5 for offset
                String addrHex = sizeHex.substring(5, 8) + startSecHex.substring(3, 8);
                
                //String storeAddrHex = addrHex.substring(0, 2) + addrHex.substring(2, 4) + addrHex.substring(4, 6) + addrHex.substring(6, 8);
                String storeAddrHex =  addrHex.substring(6, 8) +  addrHex.substring(4, 6) + addrHex.substring(2, 4) + addrHex.substring(0, 2);
                
                //String addrHex = startSecHex.substring(6, 8) + startSecHex.substring(4, 6) + startSecHex.substring(3, 4) + sizeHex.substring(5, 8);
                
                System.out.println(String.format("from:%s to:%s size:%s addr:%s storeAddr:%s %s", startSecHex, endSecHex, sizeHex, addrHex, storeAddrHex, str));
                */
                
                
                
                
                //String posHex = Inspector.toHex(Converter.intToBytesBE(position)).toUpperCase();
                /*
                from cellar to town -> 00 01 A2 3A (this is the absolute CD sector for the folder) 0067
                from town to cellar -> 00 01 A1 70 (this is the absolute CD sector for the folder) 006c
                */
                
                /*
                //int totalSize = 2048 * sectors;
                //int folderSize = folder.getOriginalSize();
                int numberOfFiles = folder.getFiles().size();

                int folderHeader = 16;
                int fileHeaders = numberOfFiles * 16;
                int bytePos = folderHeader + fileHeaders;

                int absSec = 0;
                for (HeartBeatDataFile file : folder.getFiles()) {

                    int sectorOffset = bytePos / 2048;
                    
                    absSec = currentSector + sectorOffset;
                    String absSecHex = Inspector.toHex(Converter.intToBytesBE(absSec)).toUpperCase();
                    
                    //System.out.println(String.format("\tbytePos=%d sectorPos=%d absSecHex=%s %s", bytePos, sectorOffset, absSecHex, file));
                    

                    bytePos += file.getOriginalSize();
                }
                
                int lastSector = absSec + 1;
                String lastSectorHex = Inspector.toHex(Converter.intToBytesBE(lastSector)).toUpperCase();

                //System.out.println(String.format("sec=%06d secHex=%s lastSecHex=%s %s", currentSector, startSecHex, lastSectorHex, str));
                
                currentSector += sectors;
                */
                
            } else if (entry instanceof HeartBeatData60010108Entry || entry instanceof HeartBeatDataBinaryEntry) {
                
                //currentSector += 1;

            }
            
            System.out.println(String.format("%s %s", entry.getSectorAddressCountStoredHex(), str));
            
            /*else if (entry instanceof HeartBeatDataFolderEntry) {
                    sector += ((HeartBeatDataFolderEntry) entry).getOriginalNumberOfSectors();
                }*/

        }

        /*
            https://www.cosy.sbg.ac.at/~held/teaching/wiss_arbeiten/slides_03-04/CD_Formate.pdf
            Die Daten f̈ur 1 Sekunde Audioinformation sind in 75 Sektoren unterteilt.
            • Alle Sektoradressangaben im Format mm:ss:cc
         */
        //1 second = 100 centiseconds = 75 sectors
        //75 sectors = 100 centiseconds
        //100 centiseconds = 75 sectors
        //1 centisecond = (75 / 100) sectors
        /*
                int speed = 75;
                //int speed = 150;
                
                
                int seconds = sector / speed;
                int rest = sector % speed;
                double prop = rest / (double) speed;
                int cc = (int) (100 * prop); //((sector / 75) * 100) % 100;
                //double ccd = (sector * 75) / (double) 100;
                //int cc = (int) ccd;
                int ss = seconds % 60; //cc % 6000;
                int mm = seconds / 60; //cc / 6000;
         */
        //cc means centiseconds
        /*
                The sectors on CDROMs and CD Audio disks are numbered in Minutes, Seconds, and 1/75 fragments of a second
                (where a "second" is referring to single-speed drives, ie. the normal CD Audio playback speed).
         */
//                mm	ss	cc	Level	Text ID
//                23	44	35	Transition	
//                23	46	64	Cellar	0x006c   | 23:44:85 (+ 1:79) rest:64 prop:0,853333 folder containing text id 006c
//                23	47	68	Well (maybe 006a)
//                23	48	30	House Top Left	0x0069 | 23:46:40 (+ 1:90) rest:30 prop:0,400000 folder containing text id 0069
//                23	49	41	Town	0x0067
//                23	49	81	House Top Right	0x0068
//                23	51	72	House Bottom Right	0x0065
//                3	48	0		(0x0392)
//                0	7	3		(0x0392)
//                0	7	6		(0x0392)
//                0	9	26		(0x0392)
//                3	50	50	Outside Castle	0x039c
//                3	53	46	Town	0x0390
        /*
                String line = String.format(
                        "%d:%d:%d rest:%d prop:%f %s",
                        //sector, Inspector.toHex(Converter.intToBytesBE(sector)), 
                        mm, ss, cc,
                        rest, prop,
                        //remaining,
                        str);
                
                if(!str.isBlank()) {
                    System.out.println(line);
                }
         */
 /*
                amm:    minute number on entire disk (00h and up)
                ass:    second number on entire disk (00h to 59h)
                asect:  sector number on entire disk (00h to 74h)
         */
        //if(mm >= 23 && ss >= 52) { //str.contains("006c")) {
        //    break;
        //}
        /*
                if (str.contains("006c")) {
                    
                    System.out.println("offset: " + offset + " " + line);
                    
                    if (ss == 46 && cc == 64) {
                        
                    }

                    break;
                }
         */
        //}
        //}
    }

    public void findSectorAddressInPsxExe() {
        
        //System.out.println(binary.getDiskFiles());
        
        //SYSTEM.CNF   = DiskFileInfo{name=SYSTEM.CNF, startSector=23, size=68
        //SLPM_869.16  = DiskFileInfo{name=SLPM_869.16, startSector=24, size=692224
        //HBD1PS1D.Q41 = DiskFileInfo{name=HBD1PS1D.Q41, startSector=362, size=319436800

        
        List<HeartBeatDataEntry> noPosFolderEntries = new ArrayList<>();
        List<HeartBeatDataEntry> ambigPosFolderEntries = new ArrayList<>();
        
        
        List<Integer> allPos = new ArrayList<>();
        
        for (HeartBeatDataEntry entry : binary.getHeartBeatData().getEntries()) {

            String text = "";
            
            if (entry instanceof HeartBeatDataFolderEntry) {
                HeartBeatDataFolderEntry folder = (HeartBeatDataFolderEntry) entry;
                List<HeartBeatDataTextContent> l = folder.getContents(HeartBeatDataTextContent.class);
                if (!l.isEmpty()) {
                    text = "text id " + l.get(0).getIdHex();
                }
            }
            
            String hex = entry.getSectorAddressCountStoredHex();
            
            List<Integer> positions = null;
            if (entry instanceof HeartBeatDataFolderEntry) {
                byte[] pattern = Inspector.toBytes(hex);
                positions = Inspector.find(pattern, binary.getExecutable().getData(), 603632);
                positions.removeIf(i -> i % 4 != 0);
                
                System.out.println(String.format("%s %s pos=%s %s", hex, text, positions, entry.getClass().getSimpleName()));
                
                allPos.addAll(positions);
                
                if(positions.isEmpty()) { 
                    noPosFolderEntries.add(entry);
                }
                if(positions.size() > 1) {
                    ambigPosFolderEntries.add(entry);
                }
            }
            
            
        }
        
        System.out.println("no:");
        noPosFolderEntries.forEach(entry -> System.out.println(entry));
        
        System.out.println("ambig:");
        ambigPosFolderEntries.forEach(entry -> System.out.println(entry));
        
        System.out.println("all positions sorted:");
        allPos.sort((a,b) -> Integer.compare(a, b));
        allPos.forEach(i -> System.out.println(i));
    }
    
    /**
     * We are interested in the script block (type 39) of the folder where text id 00C6 is.
     */
    public void scriptOf00C6() {
        //In the 26046/6 block (type=39) is the compressed cut scene script. 
        HeartBeatDataFile scriptFile = binary.getHeartBeatData().getFileByPath("26046/6");

        HeartBeatDataScriptContent script = (HeartBeatDataScriptContent) scriptFile.getContent();

        HeartBeatDataFile textFile = binary.getHeartBeatData().getFileByPath("26046/13");

        HeartBeatDataTextContent text = (HeartBeatDataTextContent) textFile.getContent();

        text.getOriginalText().forEach(c -> System.out.println(c));

        //HeartBeatDataFile text2File = binary.getHeartBeatData().getFileByPath("0/6");
        //The 06C00F91 value means that it will jump 0xF91 bits in the huffman code to get the right starting point. 
        //The 0x06C is the text id 006C (first scene).
        //it is stored as: 910fc006
        System.out.println(script.toStringEntries());

        int a = 0;
    }

    public void checkReferredCharacters() throws IOException {

        //collect all text ids
        //1106 (on 2022-08-16) but with dummy texts
        Set<String> textIds = new HashSet<>();

        //VariableToDialogPointer check
        int vdpPositive = 0;
        List<VariableToDialogPointer> vdpNegatives = new ArrayList<>();

        for (HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {

            for (HeartBeatDataTextContent text : folder.getContents(HeartBeatDataTextContent.class)) {

                for (VariableToDialogPointer vdp : text.getOriginalDialogPointers()) {
                    if (vdp.hasReference()) {
                        vdpPositive++;
                    } else {
                        vdpNegatives.add(vdp);
                    }
                }

                textIds.add(text.getIdHex());
            }
        }

        //Script pointer check
        int spPositive = 0;
        List<ScriptStoreEntry> spNegatives = new ArrayList<>();

        for (HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {

            for (HeartBeatDataScriptContent script : folder.getContents(HeartBeatDataScriptContent.class)) {

                //use only the texts in the folder to avoid false negatives
                for (HeartBeatDataTextContent text : folder.getContents(HeartBeatDataTextContent.class)) {

                    for (ScriptStoreEntry store : script.getStoreEntries(text.getId2Bytes())) {
                        if (store.hasReference()) {
                            spPositive++;
                        } else {
                            spNegatives.add(store);
                        }
                    }

                }

            }
        }

        System.out.println("unique number of text ids: " + textIds.size());

        System.out.println("VariableToDialogPointer: " + vdpPositive + " pos vs " + vdpNegatives.size() + " neg, acc:" + vdpPositive / (double) (vdpPositive + vdpNegatives.size()));
        //note: only the 043f texts have missing pointers which seems to be ok
        for (VariableToDialogPointer neg : vdpNegatives) {
            System.out.println("\t" + neg);
        }

        //note: there seems to be 3307 pointers to texts
        System.out.println("ScriptStoreEntry: " + spPositive + " pos vs " + spNegatives.size() + " neg, acc:" + spPositive / (double) (spPositive + spNegatives.size()));
        //note: only the 043f texts have missing pointers which seems to be ok
        for (ScriptStoreEntry neg : spNegatives) {
            System.out.println("\t" + neg);
        }

        List<HuffmanCharacter> coveredAll = new ArrayList<>();
        List<HuffmanCharacter> uncoveredAll = new ArrayList<>();

        //check text coverage
        for (HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {

            for (HeartBeatDataTextContent content : folder.getContents(HeartBeatDataTextContent.class)) {

                List<HuffmanCharacter> covered = new ArrayList<>();
                List<HuffmanCharacter> uncovered = new ArrayList<>();

                boolean coveredState = false;
                for (HuffmanCharacter c : content.getOriginalText()) {

                    if (c.hasReferrers()) {

                        if (coveredState) {
                            throw new IOException("already covered");
                        }

                        coveredState = true;
                    }

                    if (coveredState) {
                        covered.add(c);
                    } else {
                        uncovered.add(c);
                    }

                    if (c.getNode().isNullCharacter()) {
                        //if(!coveredState) {
                        //    throw new IOException("{0000} but not covered: " + content.getTextAsString());
                        //}

                        coveredState = false;
                    }
                }
                //ダミー{7f0b}
                if (!uncovered.isEmpty()) {
                    System.out.println(content.getIdHex());
                    //uncovered.forEach(c -> System.out.println(c));
                    //System.out.println();
                }

                coveredAll.addAll(covered);
                uncoveredAll.addAll(uncovered);

            }//per text content

        }

        System.out.println("coverage: " + coveredAll.size() + " covered vs " + uncoveredAll.size() + " uncovered, acc: " + coveredAll.size() / (double) (coveredAll.size() + uncoveredAll.size()));

        int a = 0;
    }
}
