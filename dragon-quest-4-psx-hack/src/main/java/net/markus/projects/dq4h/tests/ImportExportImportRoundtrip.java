
package net.markus.projects.dq4h.tests;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatDataBinaryEntry;
import net.markus.projects.dq4h.data.HeartBeatDataEntry;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileReader;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileWriter;
import net.markus.projects.dq4h.io.IOConfig;
import net.markus.projects.dq4h.io.Verifier;
import net.markus.projects.dq4h.translation.Translator;
import net.markus.projects.dq4h.util.MemoryUtility;

/**
 * A class to perform a roundtrip to make sure that reading and writing works perfectly.
 */
public class ImportExportImportRoundtrip {
    
    //needs around 5 GB because imported two times
    public void run() throws IOException {
        File folder = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/");
        File inputFile = new File(folder, "dq4.bin");
        File outputFile = new File(folder, "dq4-patched.bin");
        
        IOConfig config = new IOConfig();
        
        DragonQuestBinary originalBin = importData(inputFile, config);
        
        changeSomething(originalBin);
        
        exportData(originalBin, inputFile, outputFile, config);
        
        DragonQuestBinary patchedBin = importAgain(outputFile, config);
        
        compare(originalBin, patchedBin);
    }
    
    private DragonQuestBinary importData(File inputFile, IOConfig config) throws IOException {
         //import =============================
        System.out.println("Importing " + inputFile);
        
        long begin = System.currentTimeMillis();
        
        DragonQuestBinaryFileReader binReader = new DragonQuestBinaryFileReader(config);
        DragonQuestBinary inputBin = binReader.read(inputFile);
        System.out.println(inputBin);
        
        long end = System.currentTimeMillis();
        
        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics());
        
        return inputBin;
    }
    
    private void exportData(DragonQuestBinary inputBin, File inputFile, File outputFile, IOConfig config) throws IOException {
        //export =============================
        System.out.println("Exporting " + outputFile);
        
        long begin = System.currentTimeMillis();
        
        DragonQuestBinaryFileWriter binWriter = new DragonQuestBinaryFileWriter(config);
        //binWriter.patch(inputBin, inputFile, outputFile);
        binWriter.write(inputBin, outputFile);
        
        long end = System.currentTimeMillis();
        
        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics());
        
        System.out.println(inputFile.length() + " bytes input vs\n" + outputFile.length() + " bytes output");
        
        //if(inputFile.length() != outputFile.length()) {
        //   throw new RuntimeException("file length differ");
        //}
        
        if(false) {
            begin = System.currentTimeMillis();
            System.out.println("Comparing files");
            List<Long> fileComparison = Verifier.filesCompareByByte(inputFile, outputFile);
            end = System.currentTimeMillis();
            System.out.println("Result (" + fileComparison.size() + " byte changes) took "+ (end-begin) +" ms at " + fileComparison);
        }
        
        config.getChangeLogEntries().forEach(c -> System.out.println(c));
        config.getChangeLogEntries().forEach(e -> {
            try {
                e.saveHtmlReport(new File("../../" + e.getFilename() + ".html"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
    
    private DragonQuestBinary importAgain(File outputFile, IOConfig config) throws IOException {
        //import =============================
        
        System.gc();
        
        System.out.println("Importing " + outputFile);
        
        long begin = System.currentTimeMillis();
        
        //reading the written file again
        DragonQuestBinaryFileReader binReader = new DragonQuestBinaryFileReader(config);
        DragonQuestBinary outputBin = binReader.read(outputFile);
        System.out.println(outputBin);
        
        long end = System.currentTimeMillis();
        
        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics());
        
        return outputBin;
    }
    
    private void compare(DragonQuestBinary inputBin, DragonQuestBinary outputBin) {
        //compare =====================
        
        ComparatorReport report = new ComparatorReport();
        inputBin.compare(outputBin, report);
        
        System.out.println(report);
    }
    
    private void changeSomething(DragonQuestBinary originalBin) {
        insertSector(originalBin);
    }
    
    private void insertSector(DragonQuestBinary binary) {
        HeartBeatDataEntry[] originalEntries = binary.getHeartBeatData().getEntries().toArray(new HeartBeatDataEntry[0]);
        
        for (int i = 0; i < originalEntries.length; i++) {

            HeartBeatDataEntry entry = originalEntries[i];
            
            if (entry instanceof HeartBeatDataFolderEntry) {
                HeartBeatDataFolderEntry folder = (HeartBeatDataFolderEntry) entry;
                List<HeartBeatDataTextContent> l = folder.getContents(HeartBeatDataTextContent.class);
                if (!l.isEmpty() && l.get(0).getIdHex().equals("006c")) {
                    HeartBeatDataBinaryEntry binEntry = new HeartBeatDataBinaryEntry();
                    binEntry.setData(new byte[DragonQuestBinaryFileWriter.SECTOR_USER_SIZE]);
                    binary.getHeartBeatData().getEntries().add(i, binEntry);
                }
            }
        }
    }
    
    private void translationTest(DragonQuestBinary originalBin) {
        Translator translator = new Translator();
        translator.setBinary(originalBin);
        translator.selectiveTranslationTest("006c");
    }
    
    public static void main(String[] args) throws IOException {
        ImportExportImportRoundtrip roundtrip = new ImportExportImportRoundtrip();
        roundtrip.run();
    }
}
