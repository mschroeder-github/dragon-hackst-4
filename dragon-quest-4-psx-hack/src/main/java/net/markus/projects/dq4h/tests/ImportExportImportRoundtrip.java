
package net.markus.projects.dq4h.tests;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.data.DragonQuestBinary;
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
        
        if(true) {
            Translator translator = new Translator();
            translator.setBinary(originalBin);
            translator.selectiveTranslation("006c");
        }
        
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
        binWriter.patch(
                inputBin,
                inputFile,
                outputFile
        );
        
        long end = System.currentTimeMillis();
        
        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics());
        
        if(inputFile.length() != outputFile.length()) {
            throw new RuntimeException("file length differ");
        }
        
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
    
    public static void main(String[] args) throws IOException {
        ImportExportImportRoundtrip roundtrip = new ImportExportImportRoundtrip();
        roundtrip.run();
    }
}
