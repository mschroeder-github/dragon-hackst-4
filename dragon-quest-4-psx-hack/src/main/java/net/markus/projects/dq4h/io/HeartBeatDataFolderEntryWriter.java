package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataScriptContent;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import org.crosswire.common.compress.LZSS;

/**
 * Writes {@link HeartBeatDataFolderEntry}.
 */
public class HeartBeatDataFolderEntryWriter extends DragonQuestWriter<HeartBeatDataFolderEntry> {

    //special writer used for file formats
    private HeartBeatDataTextContentWriter textContentWriter;
    private HeartBeatDataScriptContentWriter scriptContentWriter;

    private IOConfig config;

    public HeartBeatDataFolderEntryWriter(IOConfig config) {
        this.config = config;

        textContentWriter = new HeartBeatDataTextContentWriter();
        scriptContentWriter = new HeartBeatDataScriptContentWriter();
    }

    @Override
    public void write(HeartBeatDataFolderEntry entry, OutputStream output) throws IOException {

        //for each file we need the actual bytes and the uncompressed size (if compressed)
        Map<HeartBeatDataFile, FileContent> file2content = new HashMap<>();

        //we just use the original data for now
        //this is identity function
        for (HeartBeatDataFile file : entry.getFiles()) {

            FileContent content = new FileContent();

            //if content is available and flag is activated
            //write new byte array from changed content
            if (file.hasContent() && file.getContent().isPerformPatch()) {

                byte[] contentBytes = null;
                
                if (config.getTextContentTypes().contains(file.getOriginalType())) {
                    //text content

                    //should never happen in DQ4 that a text file is compressed
                    if (file.isCompressed()) {
                        throw new IOException("Text content needs to be compressed");
                    }

                    contentBytes = textContentWriter.write((HeartBeatDataTextContent) file.getContent());
                    content.setBytes(contentBytes);
                    content.setSizeUncompressed(contentBytes.length);

                } else if (config.getScriptContentTypes().contains(file.getOriginalType())) {
                    //script content
                    
                    //writer writes always uncompressed
                    contentBytes = scriptContentWriter.write((HeartBeatDataScriptContent) file.getContent());

                    //folder entry writer is responsible for compressing content
                    if (file.isCompressed()) {
                        //set uncompressed size, then compress it
                        content.setSizeUncompressed(contentBytes.length);
                        
                        //extra compression step
                        contentBytes = LZSS.compress(contentBytes, -1);
                        
                        //TODO it is not dependend on the zeros, it is dependend on some buffersize it seems
                        //because when the pointers are changed (from "test" to "tests") then the size changes
                        //and the compressed script changes.
                        
                        int sizeDiff = file.getOriginalContentBytes().length - contentBytes.length;
                        
                        /*
                        //how many zeros are there in changed
                        int zeroIndexChanged;
                        for(zeroIndexChanged = contentBytes.length-1; zeroIndexChanged >= 0; zeroIndexChanged--) {
                            if(contentBytes[zeroIndexChanged] != (byte) 0) {
                                break;
                            }
                        }
                        
                        //how many zeros are there
                        byte[] orig = file.getOriginalContentBytes();
                        int zeroIndexOriginal;
                        for(zeroIndexOriginal = orig.length-1; zeroIndexOriginal >= 0; zeroIndexOriginal--) {
                            if(orig[zeroIndexOriginal] != (byte) 0) {
                                break;
                            }
                        }
                        
                        //here is a non-zero byte and it should be the same in both arrays
                        if(orig[zeroIndexOriginal] != contentBytes[zeroIndexChanged]) {
                            throw new IOException("End of compressed script do not match with original script");
                        }
                        
                        //how many zeros do we have to add
                        int zerosChanged = contentBytes.length - (zeroIndexChanged + 1);
                        int zerosOriginal = orig.length - (zeroIndexOriginal + 1);
                        
                        if(zerosChanged > zerosOriginal) {
                            throw new IOException("There are more zeros in compressed script than in the original");
                        }
                        int zerosDiff = zerosOriginal - zerosChanged;
                        */
                        
                        //update
                        
                        //tested with 006C text id and its script:
                        //the last two 0x0000 are very important for the decompression algo in the game
                        //if they are not there, the game crashes
                        //maybe 4 times 0x00 are more save that he game will not crash
                        byte[] longerContentBytes = new byte[contentBytes.length + sizeDiff];
                        System.arraycopy(contentBytes, 0, longerContentBytes, 0, contentBytes.length);
                        contentBytes = longerContentBytes;
                        

                    } else {
                        //uncompressed: so uncompressed size is the same
                        content.setSizeUncompressed(contentBytes.length);
                    }
                    
                    //set bytes
                    content.setBytes(contentBytes);
                    
                } else {
                    throw new IOException("content to be patched has not a writer");
                }
                
                //log if a file was changed
                int i = Verifier.compare(file.getOriginalContentBytes(), contentBytes);
                if (i != -1) {
                    config.getChangeLogEntries().add(new ChangeLogEntry(file, file.getOriginalContentBytes(), contentBytes));
                }

            } else {

                //default case (as is), so this is not changed
                content.setBytes(file.getOriginalContentBytes());
                content.setSizeUncompressed(file.getOriginalSizeUncompressed());
            }

            file2content.put(file, content);
        }

        //calculate the meta data for the expected folder
        HeartBeatDataFolderEntry expectedFolder = HeartBeatDataFolderEntry.calculateExpectedFolder(file2content.values());

        //we have to use the same number of sectors so that there is no data shift
        if (expectedFolder.getOriginalNumberOfSectors() != entry.getOriginalNumberOfSectors()) {
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
        for (HeartBeatDataFile file : entry.getFiles()) {
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
        for (HeartBeatDataFile file : entry.getFiles()) {
            FileContent content = file2content.get(file);

            dqos.write(content.getBytes());
        }

        //fill remaining space with zero bytes
        dqos.write(new byte[expectedFolder.getOriginalNumberOfRemainingBytes()]);

    }

}
