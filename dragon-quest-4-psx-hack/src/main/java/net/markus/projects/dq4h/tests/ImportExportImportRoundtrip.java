
package net.markus.projects.dq4h.tests;

import java.io.File;
import java.io.IOException;
import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileReader;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileWriter;
import net.markus.projects.dq4h.io.IOConfig;
import net.markus.projects.dq4h.util.MemoryUtility;

/**
 * A class to perform a roundtrip to make sure that reading and writing works perfectly.
 */
public class ImportExportImportRoundtrip {
    
    public void run() throws IOException {
        File folder = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/");
        File inputFile = new File(folder, "dq4.bin");
        File outputFile = new File(folder, "dq4-patched.bin");
        
        IOConfig config = new IOConfig();
        
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
        
        //import =============================
        
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
