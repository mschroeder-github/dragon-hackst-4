
package net.markus.projects.dq4h.tests;

import java.io.File;
import java.io.IOException;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileReader;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileWriter;

/**
 * 
 */
public class InputOutputRoundtrip {
    
    public static void main(String[] args) throws IOException {
        
        DragonQuestBinaryFileReader binReader = new DragonQuestBinaryFileReader();
        DragonQuestBinary bin = binReader.read(new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4.bin"));
        System.out.println(bin);
        
        
        
        DragonQuestBinaryFileWriter binWriter = new DragonQuestBinaryFileWriter();
        binWriter.patch(
                bin,
                new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4.bin"),
                new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-patched.bin")
        );
        
        
    }
    
}
