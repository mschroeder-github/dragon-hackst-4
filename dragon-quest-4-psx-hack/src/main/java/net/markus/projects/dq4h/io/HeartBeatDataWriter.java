
package net.markus.projects.dq4h.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatData;
import net.markus.projects.dq4h.data.HeartBeatData60010108Entry;
import net.markus.projects.dq4h.data.HeartBeatDataBinaryEntry;
import net.markus.projects.dq4h.data.HeartBeatDataEntry;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;

/**
 * Writes {@link HeartBeatData}.
 */
public class HeartBeatDataWriter extends DragonQuestWriter<HeartBeatData> {

    private HeartBeatDataFolderEntryWriter folderWriter;
    
    public HeartBeatDataWriter(IOConfig config) {
        folderWriter = new HeartBeatDataFolderEntryWriter(config);
    }
    
    @Override
    public void write(HeartBeatData hbd, OutputStream output) throws IOException {
        write(hbd, null, output);
    }
    
    public void write(HeartBeatData hbd, DragonQuestBinary binary, OutputStream output) throws IOException {
    
        int sectorIndex = binary == null ? 0 : binary.getDiskFile("HBD1PS1D.Q41").getStartSector();
        
        int pointerChangeCount = 0;
        
        //write every entry in the given order
        for(HeartBeatDataEntry entry : hbd.getEntries()) {
            
            if(entry instanceof HeartBeatDataBinaryEntry) {
                //if binary entry; just write its data (one sector)
                HeartBeatDataBinaryEntry binEntry = (HeartBeatDataBinaryEntry) entry;
                output.write(binEntry.getData());
                sectorIndex++;
                
            } else if(entry instanceof HeartBeatData60010108Entry) {
                //if 6001 entry; just write its data (one sector)
                HeartBeatData60010108Entry h6001Entry = (HeartBeatData60010108Entry) entry;
                output.write(h6001Entry.getData());
                sectorIndex++;
                
            } else if(entry instanceof HeartBeatDataFolderEntry) {
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                //for folders we use a separate writer to convert the folder infos into bytes
                HeartBeatDataFolderEntry folderEntry = (HeartBeatDataFolderEntry) entry;
                folderWriter.write(folderEntry, baos);
                //we expect that data is a multiplication of 2048, thus zeros were added
                byte[] data = baos.toByteArray();
                if(data.length % HeartBeatDataReader.SECTORSIZE != 0) {
                    throw new RuntimeException("data of folder does not add up to multiplication of sector size 2048: " + folderEntry);
                }
                int numberOfSectors = data.length / HeartBeatDataReader.SECTORSIZE;
                output.write(data);
                
                //check if the folder moved
                String ref = HeartBeatDataEntry.getSectorAddressCountStoredHex(sectorIndex, numberOfSectors);
                String origRef = entry.getSectorAddressCountStoredHex();
                if(!ref.equals(origRef)) {
                    //System.out.println(origRef + " -> " + ref);
                    
                    //if the sector moved, we have to update the pointer in psx exe
                    int position = folderEntry.getReferenceInExe();
                    
                    //there are currently (2023-03-06) two folders we could not find a reference in exe 
                    if(position == 0) {
                        
                    } else {
                        //overwrites the reference in the exe file
                        Inspector.overwrite(folderEntry.getExe().getData(), ref, position);
                        
                        pointerChangeCount++;
                    }
                }
                
                sectorIndex += numberOfSectors;
                
            } else {
                throw new IOException("Unknown entry type " + entry.getClass());
            }
        }
        
        System.out.println(pointerChangeCount + " pointer changes");
    }

}
