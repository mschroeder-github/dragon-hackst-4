
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.OutputStream;
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
    
        //write every entry in the given order
        for(HeartBeatDataEntry entry : hbd.getEntries()) {
            
            if(entry instanceof HeartBeatDataBinaryEntry) {
                //if binary entry; just write its data (one sector)
                HeartBeatDataBinaryEntry binEntry = (HeartBeatDataBinaryEntry) entry;
                output.write(binEntry.getData());
                
            } else if(entry instanceof HeartBeatData60010108Entry) {
                //if 6001 entry; just write its data (one sector)
                HeartBeatData60010108Entry h6001Entry = (HeartBeatData60010108Entry) entry;
                output.write(h6001Entry.getData());
                
            } else if(entry instanceof HeartBeatDataFolderEntry) {
                
                //for folders we use a separate writer to convert the folder infos into bytes
                HeartBeatDataFolderEntry folderEntry = (HeartBeatDataFolderEntry) entry;
                folderWriter.write(folderEntry, output);
                
            } else {
                throw new IOException("Unknown entry type " + entry.getClass());
            }
        }
    }

}
