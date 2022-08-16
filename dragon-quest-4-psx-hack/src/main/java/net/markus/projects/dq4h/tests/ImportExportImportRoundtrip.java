
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
        //config.getScriptContentTypes().clear();
        //config.getTextContentTypes().clear();
        
        //import =============================
        System.out.println("Importing " + inputFile);
        
        long begin = System.currentTimeMillis();
        
        DragonQuestBinaryFileReader binReader = new DragonQuestBinaryFileReader(config);
        DragonQuestBinary inputBin = binReader.read(inputFile);
        System.out.println(inputBin);
        
        long end = System.currentTimeMillis();
        
        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics());
        
        
        //export =============================
        System.out.println("Exporting to " + outputFile);
        
        begin = System.currentTimeMillis();
        
        DragonQuestBinaryFileWriter binWriter = new DragonQuestBinaryFileWriter(config);
        binWriter.patch(
                inputBin,
                inputFile,
                outputFile
        );
        
        end = System.currentTimeMillis();
        
        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics());
        
        if(inputFile.length() != outputFile.length()) {
            throw new RuntimeException("file length differ");
        }
        
        begin = System.currentTimeMillis();
        System.out.println("Comparing files");
        List<Long> fileComparison = Verifier.filesCompareByByte(inputFile, outputFile);
        end = System.currentTimeMillis();
        System.out.println("Result (" + fileComparison.size() + " byte changes) took "+ (end-begin) +" ms at " + fileComparison);
        
        //import =============================
        
        System.gc();
        
        System.out.println("Importing " + outputFile);
        
        begin = System.currentTimeMillis();
        
        //reading the written file again
        binReader = new DragonQuestBinaryFileReader(config);
        DragonQuestBinary outputBin = binReader.read(inputFile);
        System.out.println(outputBin);
        
        end = System.currentTimeMillis();
        
        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics());
        
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
