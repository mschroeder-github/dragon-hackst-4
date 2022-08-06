
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.InputStream;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;

/**
 * 
 */
public class HeartBeatDataFolderEntryReader extends DragonQuestReader<HeartBeatDataFolderEntry> {

    @Override
    public HeartBeatDataFolderEntry read(InputStream input) throws IOException {
        HeartBeatDataFolderEntry folder = new HeartBeatDataFolderEntry();
        
        DragonQuestInputStream dqir = new DragonQuestInputStream(input);
        
        //folder header info
        folder.setOriginalNumberOfFiles(dqir.readIntLE());
        folder.setOriginalNumberOfSectors(dqir.readIntLE());
        folder.setOriginalSize(dqir.readIntLE());
        folder.setOriginalUnknown(dqir.readBytes(4));
        
        //for DQ4 it is all zero
        if(!Verifier.allZero(folder.getOriginalUnknown())) { 
            throw new IOException("Unknown part (after size) in folder is not all zero: " + folder);
        }
        
        //file header infos
        for (int i = 0; i < folder.getOriginalNumberOfFiles(); i++) {
            
            HeartBeatDataFile file = new HeartBeatDataFile();
            
            //16 bytes header
            
            file.setOriginalSize(dqir.readIntLE());
            file.setOriginalSizeUncompressed(dqir.readIntLE());
            file.setOriginalUnknown(dqir.readBytes(4));
            file.setOriginalFlags(dqir.readShortLE());
            file.setOriginalType(dqir.readShortLE());
            
            file.setIndex(i);
            file.setParent(folder);
            folder.getFiles().add(file);
        }
        
        //file contents
        for (HeartBeatDataFile file : folder.getFiles()) {
            file.setOriginalBytes(dqir.readBytes(file.getOriginalSize()));
            
            //TODO use special readers to parse the file content
        }
        
        //remaining bytes
        byte[] remaining = dqir.readRemaining();
        
        //the remaining bytes have to be zero
        if(!Verifier.allZero(remaining)) { 
            throw new IOException("Remaining bytes are not all zero in folder: " + folder);
        }
        
        folder.setOriginalNumberOfRemainingBytes(remaining.length);
        
        return folder;
    }

}
