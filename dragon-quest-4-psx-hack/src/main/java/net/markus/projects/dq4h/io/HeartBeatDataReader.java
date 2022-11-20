
package net.markus.projects.dq4h.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import net.markus.projects.dq4h.data.HeartBeatData;
import net.markus.projects.dq4h.data.HeartBeatData60010108Entry;
import net.markus.projects.dq4h.data.HeartBeatDataBinaryEntry;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;

/**
 * A reader for {@link HeartBeatData}.
 */
public class HeartBeatDataReader extends DragonQuestReader<HeartBeatData> {

    public static final int SECTORSIZE = 2048;
    
    private HeartBeatDataFolderEntryReader folderReader;
    
    private IOConfig config;
    
    public HeartBeatDataReader(IOConfig config) {
        this.config = config;
        folderReader = new HeartBeatDataFolderEntryReader(config);
    }
    
    @Override
    public HeartBeatData read(InputStream input) throws IOException {
        HeartBeatData hbd = new HeartBeatData();
        
        int entryIndex = 0;
        int sectorIndex = 0;
        
        //first entry
        config.trace("first entry");
        byte[] first = new byte[SECTORSIZE];
        input.read(first);
        HeartBeatDataBinaryEntry firstEntry = new HeartBeatDataBinaryEntry();
        firstEntry.setData(first);
        firstEntry.setIndex(entryIndex);
        hbd.getEntries().add(firstEntry);
        entryIndex++;
        
        config.setProgressMax(44657);
        
        while(true) {
            config.trace("entry index " + entryIndex + "/44657");
             config.setProgressValue(entryIndex);
            
            byte[] sector = new byte[SECTORSIZE];
            int read = input.read(sector);
            
            if(read == -1) {
                break;
            }
              
            if(isH600(sector)) {
                config.trace("H600 entry");
                
                HeartBeatData60010108Entry h6001 = new HeartBeatData60010108Entry();
                h6001.setData(sector);
                h6001.setIndex(entryIndex);
                hbd.getEntries().add(h6001);
                
            } else if(isFolder(sector)) {
                config.trace("folder entry");
                
                //we buffer all sectors of a folder in one byte array
                ByteArrayOutputStream folderBuffer = new ByteArrayOutputStream();
                folderBuffer.write(sector);
                
                //the number of sectors a folder has
                int folderSectors = Converter.bytesToIntLE(Arrays.copyOfRange(sector, 4, 4 + 4));
                for(int i = 0; i < folderSectors - 1; i++) {
                    sector = new byte[SECTORSIZE];
                    input.read(sector);
                    folderBuffer.write(sector);
                }
                
                //a reader reads the folder
                ByteArrayInputStream folderInput = new ByteArrayInputStream(folderBuffer.toByteArray());
                HeartBeatDataFolderEntry folderEntry = folderReader.read(folderInput);
                folderEntry.setIndex(entryIndex);
                hbd.getEntries().add(folderEntry);
                
                
            } else if(Verifier.allZero(sector)) {
                config.trace("zero entry");
                
                //a zero-bytes sector entry
                HeartBeatDataBinaryEntry entry = new HeartBeatDataBinaryEntry();
                entry.setData(sector);
                entry.setIndex(entryIndex);
                hbd.getEntries().add(entry);
                
            } else {
                throw new IOException("Unknown sector type at " + entryIndex);
            }
            
            entryIndex++;
            sectorIndex++;
            
            //System.out.println(entryIndex + " read");
        }
        
        hbd.setOriginalNumberOfSectors(sectorIndex);
        
        return hbd;
    }

    /**
     * The typical **000000 pattern which indicates a folder.
     * @param sector
     * @return 
     */
    private boolean isFolder(byte[] sector) {
        return sector[0] != 0 && sector[1] == 0 && sector[2] == 0 && sector[3] == 0;
    }
    
    /**
     * The typical 60010108 pattern which indicates a 60010108 entry.
     * @param sector
     * @return 
     */
    private boolean isH600(byte[] sector) {
        //0x60 = 96
        return sector[0] == 96 && sector[1] == 1 && sector[2] == 1 && sector[3] == -128;
    }
    
}
