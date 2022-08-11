
package net.markus.projects.dq4h.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import org.crosswire.common.compress.LZSS;

/**
 * Reads {@link HeartBeatDataFolderEntry}.
 * It is responsible for (de)compressing file contents if needed.
 */
public class HeartBeatDataFolderEntryReader extends DragonQuestReader<HeartBeatDataFolderEntry> {

    //readers for special file contents
    private HeartBeatDataTextContentReader textContentReader;
    
    private IOConfig config;
    
    public HeartBeatDataFolderEntryReader(IOConfig config) {
        this.config = config;
        
        textContentReader = new HeartBeatDataTextContentReader();
    }
    
    @Override
    public HeartBeatDataFolderEntry read(InputStream input) throws IOException {
        HeartBeatDataFolderEntry folder = new HeartBeatDataFolderEntry();
        
        DragonQuestInputStream dqir = new DragonQuestInputStream(input);
        
        //folder header info
        folder.setOriginalNumberOfFiles(dqir.readIntLE());
        folder.setOriginalNumberOfSectors(dqir.readIntLE());
        folder.setOriginalSize(dqir.readIntLE());
        folder.setOriginalUnknown(dqir.readBytesBE(4));
        
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
            file.setOriginalUnknown(dqir.readBytesBE(4));
            file.setOriginalFlags(dqir.readShortLE());
            file.setOriginalType(dqir.readShortLE());
            
            file.setIndex(i);
            file.setParent(folder);
            folder.getFiles().add(file);
        }
        
        //file contents
        for (HeartBeatDataFile file : folder.getFiles()) {
            //the original bytes
            file.setOriginalContentBytes(dqir.readBytesBE(file.getOriginalSize()));
            
            byte[] fileContentBytes = file.getOriginalContentBytes();
            
            //content is parsed if we have a reader for it
            short type = file.getOriginalType();
            if(config.getTextContentTypes().contains(type)) {
                
                //note: in DQ4 text contents are never compressed
                
                //read data
                InputStream contentInput = new ByteArrayInputStream(fileContentBytes);
                HeartBeatDataTextContent textContent = textContentReader.read(contentInput);
                contentInput.close();
                
                textContent.setParent(file);
                file.setContent(textContent);
                
            } else if(config.getScriptContentTypes().contains(type)) {
                
                //uncompress if compressed
                if(file.isCompressed()) {
                    ByteArrayInputStream compressedInput = new ByteArrayInputStream(fileContentBytes);
                    
                    fileContentBytes = new LZSS(compressedInput).uncompress(file.getOriginalSizeUncompressed()).toByteArray();
                
                    int expected = file.getOriginalSizeUncompressed();
                    int actual = fileContentBytes.length;
                    
                    //actual can be larger than expected
                    if(expected > actual) {
                        //note: this does not happen in DQ4 data
                        throw new IOException(
                                String.format(
                                        "Uncompressed size expected > actual: expected: %d is greater than actual: %d",
                                        file.getOriginalSizeUncompressed(),
                                        fileContentBytes.length
                                )
                        );
                    } else if(expected != actual) {
                        //if the decompression wrote too many bytes we shorten the result
                        
                        //we shorten the byte array to the expected size
                        fileContentBytes = Arrays.copyOfRange(fileContentBytes, 0, expected);
                    }
                }
                
                //TODO use script reader to read fileContentBytes
                
            }
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
