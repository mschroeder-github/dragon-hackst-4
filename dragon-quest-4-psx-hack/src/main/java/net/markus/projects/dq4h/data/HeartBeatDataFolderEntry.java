
package net.markus.projects.dq4h.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileWriter;
import net.markus.projects.dq4h.io.FileContent;

/**
 * The entry represents a folder with files in it.
 * It consists of one to many 2048 byte blocks.
 * It has a 16 byte header (number of files, number of sectors, size).
 */
public class HeartBeatDataFolderEntry extends HeartBeatDataEntry {

    /**
     * 0-4, 4 bytes, int
     */
    private int originalNumberOfFiles;
    
    /**
     * 4-8, 4 bytes, int
     */
    private int originalNumberOfSectors;
    
    /**
     * 8-12, 4 bytes, int (could maybe be long = 8 bytes)
     */
    private int originalSize;
    
    /**
     * 12-16, 4 bytes, zeros
     */
    private byte[] originalUnknown;
    
    private int originalNumberOfRemainingBytes;
    
    private List<HeartBeatDataFile> files;
    
    public HeartBeatDataFolderEntry() {
        files = new ArrayList<>();
    }
    
    /**
     * A folder contains several files.
     * @return 
     */
    public List<HeartBeatDataFile> getFiles() {
        return files;
    }

    /**
     * The origial number of files in the data.
     * @return 
     */
    public int getOriginalNumberOfFiles() {
        return originalNumberOfFiles;
    }

    /**
     * The original number of sectors in the data.
     * @return 
     */
    public int getOriginalNumberOfSectors() {
        return originalNumberOfSectors;
    }

    /**
     * The original size of the folder block in bytes.
     * @return 
     */
    public int getOriginalSize() {
        return originalSize;
    }

    public void setOriginalNumberOfFiles(int originalNumberOfFiles) {
        this.originalNumberOfFiles = originalNumberOfFiles;
    }

    public void setOriginalNumberOfSectors(int originalNumberOfSectors) {
        this.originalNumberOfSectors = originalNumberOfSectors;
    }

    public void setOriginalSize(int originalSize) {
        this.originalSize = originalSize;
    }

    public void setOriginalUnknown(byte[] originalUnknown) {
        this.originalUnknown = originalUnknown;
    }

    public byte[] getOriginalUnknown() {
        return originalUnknown;
    }

    /**
     * At the end of a folder after all file content is read there are
     * some remaining bytes which are all zero.
     * @return 
     */
    public int getOriginalNumberOfRemainingBytes() {
        return originalNumberOfRemainingBytes;
    }

    public void setOriginalNumberOfRemainingBytes(int numberOfRemainingBytes) {
        this.originalNumberOfRemainingBytes = numberOfRemainingBytes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HeartBeatDataFolderEntry{");
        sb.append("index=").append(getIndex());
        sb.append(", originalNumberOfFiles=").append(originalNumberOfFiles);
        sb.append(", originalNumberOfSectors=").append(originalNumberOfSectors);
        sb.append(", originalSize=").append(originalSize);
        sb.append(", originalNumberOfRemainingBytes=").append(originalNumberOfRemainingBytes);
        sb.append(", files=").append(files.size());
        sb.append('}');
        return sb.toString();
    }

    
    /**
     * Given some file contents this method calculates the expected folder size.
     * This is helpful when we later change files and have to check if it still fits in
     * the original folder.
     * @param fileContents a list of file contents
     * @return an expected folder, no files, just the meta-data of the folder
     */
    public static HeartBeatDataFolderEntry calculateExpectedFolder(Collection<FileContent> fileContents) {
        HeartBeatDataFolderEntry folder = new HeartBeatDataFolderEntry();
        
        //folder header size
        int folderHeaderSize = 16;
        
        int fileHeaderSizeSum = 0;
        int fileContentSizeSum = 0;
        
        for(FileContent content : fileContents) {
            
            //every file needs header info
            fileHeaderSizeSum += 16;
            
            //every file's content
            fileContentSizeSum += content.getSize();
        }
        
        int expectedSize = folderHeaderSize + fileHeaderSizeSum + fileContentSizeSum;
        
        //2048
        int sectorUserSize = DragonQuestBinaryFileWriter.SECTOR_USER_SIZE;
        
        //e.g. ceil(2.01) = 3.0, ceil(2.72) = 3.0, but ceil(2.0) = 2.0
        int expectedNumberOfSectors = (int) Math.ceil(expectedSize / (double) sectorUserSize);
        int expectedNumberOfRemainingBytes = (expectedNumberOfSectors * sectorUserSize) - expectedSize;
        
        folder.setOriginalNumberOfFiles(fileContents.size());
        folder.setOriginalNumberOfSectors(expectedNumberOfSectors);
        //raw data without the folder's and file's header information
        folder.setOriginalSize(fileContentSizeSum);
        folder.setOriginalNumberOfRemainingBytes(expectedNumberOfRemainingBytes);
        
        return folder;
    }
    
    
}
