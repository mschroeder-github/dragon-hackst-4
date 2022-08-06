
package net.markus.projects.dq4h.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the main data file of dragon quest 4 (the HBD1PS1D file).
 */
public class HeartBeatData {

    private List<HeartBeatDataEntry> entries;
    
    private int numberOfSectors;

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
    public int getNumberOfSectors() {
        return numberOfSectors;
    }

    public void setNumberOfSectors(int numberOfSectors) {
        this.numberOfSectors = numberOfSectors;
    }

    
    
}
