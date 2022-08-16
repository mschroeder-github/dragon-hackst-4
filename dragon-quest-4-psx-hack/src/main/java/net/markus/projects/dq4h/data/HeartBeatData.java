
package net.markus.projects.dq4h.data;

import java.util.ArrayList;
import java.util.List;
import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.compare.DragonQuestComparator;
import net.markus.projects.dq4h.io.Verifier;

/**
 * Represents the main data file of dragon quest 4 (the HBD1PS1D file).
 */
public class HeartBeatData implements DragonQuestComparator<HeartBeatData> {

    private List<HeartBeatDataEntry> entries;
    
    private int originalNumberOfSectors;

    public HeartBeatData() {
        entries = new ArrayList<>();
    }
    
    /**
     * The data consists of an ordered list of entries.
     * @return 
     */
    public List<HeartBeatDataEntry> getEntries() {
        return entries;
    }

    /**
     * The original number of 2048 byte sectors.
     * Its more for debug purpose.
     * @return 
     */
    public int getOriginalNumberOfSectors() {
        return originalNumberOfSectors;
    }

    public void setOriginalNumberOfSectors(int numberOfSectors) {
        this.originalNumberOfSectors = numberOfSectors;
    }

    /**
     * Returns a file by its path which is &lt;folderIndex&gt;/&lt;fileIndex&gt;, e.g. "26046/6".
     * @param path
     * @return null, if file not found
     */
    public HeartBeatDataFile getFileByPath(String path) {
        String[] segments = path.split("/");
        int folderIndex = Integer.parseInt(segments[0]);
        int fileIndex = Integer.parseInt(segments[1]);
        
        for(HeartBeatDataEntry entry : entries) {
            if(entry instanceof HeartBeatDataFolderEntry) {
                HeartBeatDataFolderEntry folder = (HeartBeatDataFolderEntry) entry;
                
                if(folder.index == folderIndex) {
                    
                    for(HeartBeatDataFile file : folder.getFiles()) {
                        if(file.getIndex() == fileIndex) {
                            return file;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    public List<HeartBeatDataFolderEntry> getFolders() {
        List<HeartBeatDataFolderEntry> folders = new ArrayList<>();
        for(HeartBeatDataEntry entry : entries) {
            if(entry instanceof HeartBeatDataFolderEntry) {
                HeartBeatDataFolderEntry folder = (HeartBeatDataFolderEntry) entry;
                folders.add(folder);
            }
        }
        return folders;
    }
    
    @Override
    public void compare(HeartBeatData other, ComparatorReport report) {
        Verifier.compareLists(this, this.entries, other, other.entries, "entries", report);
        Verifier.compareNumbers(this, this.originalNumberOfSectors, other, other.originalNumberOfSectors, "originalNumberOfSectors", report);
    }
    
}
