
package net.markus.projects.dq4h.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataScriptContent;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import net.markus.projects.dq4h.data.HuffmanCharacter;
import net.markus.projects.dq4h.data.ScriptStoreEntry;
import net.markus.projects.dq4h.data.VariableToDialogPointer;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileReader;
import net.markus.projects.dq4h.io.IOConfig;
import net.markus.projects.dq4h.util.MemoryUtility;

/**
 * With this class we inspect certain properties after data was imported.
 */
public class InspectImportedData {
    
    private DragonQuestBinary binary;
    
    public static void main(String[] args) throws IOException {
        
        InspectImportedData checkAfterImport = new InspectImportedData();
        checkAfterImport.importData();
        
        checkAfterImport.scriptOf00C6();
        //checkAfterImport.checkReferredCharacters();
    }
    
    public void importData() throws IOException {
        File folder = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/");
        File inputFile = new File(folder, "dq4.bin");
        
        IOConfig config = new IOConfig();
        
        //import =============================
        System.out.println("Importing " + inputFile);
        
        long begin = System.currentTimeMillis();
        
        DragonQuestBinaryFileReader binReader = new DragonQuestBinaryFileReader(config);
        binary = binReader.read(inputFile);
        
        long end = System.currentTimeMillis();
        
        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics() + ", " + binary);
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
        
        
        for(HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {
            
            for(HeartBeatDataTextContent text : folder.getContents(HeartBeatDataTextContent.class)) {
                
                for(VariableToDialogPointer vdp : text.getDialogPointers()) {
                    if(vdp.hasReference()) {
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
        
        for(HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {
            
            for(HeartBeatDataScriptContent script : folder.getContents(HeartBeatDataScriptContent.class)) {
                
                //use only the texts in the folder to avoid false negatives
                for(HeartBeatDataTextContent text : folder.getContents(HeartBeatDataTextContent.class)) {    
                
                    for(ScriptStoreEntry store : script.getStoreEntries(text.getId2Bytes())) {
                        if(store.hasReference()) {
                            spPositive++;
                        } else {
                            spNegatives.add(store);
                        }
                    }
                    
                }
                
            }
        }
        
        System.out.println("unique number of text ids: " + textIds.size());
        
        System.out.println("VariableToDialogPointer: " + vdpPositive + " pos vs " + vdpNegatives.size() + " neg, acc:" + vdpPositive / (double) (vdpPositive+vdpNegatives.size()));
        //note: only the 043f texts have missing pointers which seems to be ok
        for(VariableToDialogPointer neg : vdpNegatives) {
            System.out.println("\t" + neg);
        }
        
        //note: there seems to be 3307 pointers to texts
        System.out.println("ScriptStoreEntry: " + spPositive + " pos vs " + spNegatives.size() + " neg, acc:" + spPositive / (double) (spPositive+spNegatives.size()));
        //note: only the 043f texts have missing pointers which seems to be ok
        for(ScriptStoreEntry neg : spNegatives) {
            System.out.println("\t" + neg);
        }
        
        
        List<HuffmanCharacter> coveredAll = new ArrayList<>();
        List<HuffmanCharacter> uncoveredAll = new ArrayList<>();
        
        //check text coverage
        for(HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {
            
            for(HeartBeatDataTextContent content : folder.getContents(HeartBeatDataTextContent.class)) {
                
                List<HuffmanCharacter> covered = new ArrayList<>();
                List<HuffmanCharacter> uncovered = new ArrayList<>();
                
                boolean coveredState = false;
                for(HuffmanCharacter c : content.getOriginalText()) {
                    
                    if(c.hasReferrers()) {
                        
                        if(coveredState) {
                            throw new IOException("already covered");
                        }
                        
                        coveredState = true;
                    }
                    
                    if(coveredState) {
                        covered.add(c);
                    } else {
                        uncovered.add(c);
                    }
                    
                    if(c.getNode().isNullCharacter()) {
                        //if(!coveredState) {
                        //    throw new IOException("{0000} but not covered: " + content.getTextAsString());
                        //}
                        
                        coveredState = false;
                    }
                }
                //ダミー{7f0b}
                if(!uncovered.isEmpty()) {
                    System.out.println(content.getIdHex());
                    //uncovered.forEach(c -> System.out.println(c));
                    //System.out.println();
                }
                
                coveredAll.addAll(covered);
                uncoveredAll.addAll(uncovered);
                
            }//per text content
            
        }
        
        System.out.println("coverage: " + coveredAll.size() + " covered vs " + uncoveredAll.size() + " uncovered, acc: " + coveredAll.size() / (double) (coveredAll.size()+uncoveredAll.size()) );
        
        int a = 0;
    }
}
