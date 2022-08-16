
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;

/**
 * 
 */
public class HeartBeatDataFolderEntryWriter extends DragonQuestWriter<HeartBeatDataFolderEntry> {

    //special writer used for file formats
    private HeartBeatDataTextContentWriter textContentWriter;
    
    private IOConfig config;
    
    public HeartBeatDataFolderEntryWriter(IOConfig config) {
        this.config = config;
        
        textContentWriter = new HeartBeatDataTextContentWriter();
    }
    
    @Override
    public void write(HeartBeatDataFolderEntry entry, OutputStream output) throws IOException {
        
        //for each file we need the actual bytes and the uncompressed size (if compressed)
        Map<HeartBeatDataFile, FileContent> file2content = new HashMap<>();
        
        //we just use the original data for now
        //this is identity function
        for(HeartBeatDataFile file : entry.getFiles()) {
            
            FileContent content = new FileContent();
            
            //folder entry writer is responsible for compressing content
            
            //text content
            if(config.getTextContentTypes().contains(file.getOriginalType()) && file.hasContent()) {
                
                //should never happen that a text file is compressed
                if(file.isCompressed()) {
                    throw new IOException("Text content needs to be compressed");
                }
                
                byte[] contentBytes = textContentWriter.write((HeartBeatDataTextContent) file.getContent());
                content.setBytes(contentBytes);
                content.setSizeUncompressed(contentBytes.length);
                
                //a small check
                //int i = Verifier.compare(file.getOriginalContentBytes(), contentBytes);
                //if(i != -1) {
                //    String out = Inspector.splitScreen(Inspector.toHexDump(file.getOriginalContentBytes(), 25), 100, Inspector.toHexDump(contentBytes, 25));
                //    System.out.println(out);
                //}
                
            } else {
                //default case (as is)
                content.setBytes(file.getOriginalContentBytes());
                content.setSizeUncompressed(file.getOriginalSizeUncompressed());
            }
            
            file2content.put(file, content);
        }
        
        //calculate the meta data for the expected folder
        HeartBeatDataFolderEntry expectedFolder = HeartBeatDataFolderEntry.calculateExpectedFolder(file2content.values());
        
        //we have to use the same number of sectors so that there is no data shift
        if(expectedFolder.getOriginalNumberOfSectors() != entry.getOriginalNumberOfSectors()) {
            throw new IOException(String.format("Sector missmatch: expected sectors (%d) != original sectors (%d).\n%s\n%s", 
                    expectedFolder.getOriginalNumberOfSectors(),
                    entry.getOriginalNumberOfSectors(),
                    expectedFolder,
                    entry
            ));
        }
        
        DragonQuestOutputStream dqos = new DragonQuestOutputStream(output);
        
        //folder header info (16 bytes)
        dqos.writeIntLE(entry.getFiles().size());
        dqos.writeIntLE(expectedFolder.getOriginalNumberOfSectors());
        dqos.writeIntLE(expectedFolder.getOriginalSize());
        dqos.write(new byte[4]);
        
        
        //all file header infos concatenated
        for(HeartBeatDataFile file : entry.getFiles()) {
            FileContent content = file2content.get(file);
            
            //16 byte header
            
            //changed content
            dqos.writeIntLE(content.getSize());
            dqos.writeIntLE(content.getSizeUncompressed());
            
            //original data
            dqos.write(file.getOriginalUnknown());
            dqos.writeShortLE(file.getOriginalFlags());
            dqos.writeShortLE(file.getOriginalType());
        }
        
        //all file content concatenated
        for(HeartBeatDataFile file : entry.getFiles()) {
            FileContent content = file2content.get(file);
            
            dqos.write(content.getBytes());
        }
        
        //fill remaining space with zero bytes
        dqos.write(new byte[expectedFolder.getOriginalNumberOfRemainingBytes()]);
        
    }

}
