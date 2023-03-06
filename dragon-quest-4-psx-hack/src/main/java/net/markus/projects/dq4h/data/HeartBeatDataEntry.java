
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.io.Converter;
import net.markus.projects.dq4h.io.Inspector;

/**
 * An abstract entry in the {@link HeartBeatData}.
 */
public abstract class HeartBeatDataEntry {

    protected int index;
    protected int sector;
    protected int sectorCount;

    /**
     * The index of the entry.
     * @return 0-based index.
     */
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * The absolute sector on CD.
     * @return 
     */
    public int getSector() {
        return sector;
    }

    public void setSector(int sector) {
        this.sector = sector;
    }

    /**
     * The number of sectors of this entry.
     * In {@link HeartBeatDataFolderEntry#getOriginalNumberOfSectors() } we have the number read from the folder.
     * @return 
     */
    public int getSectorCount() {
        return sectorCount;
    }

    public void setSectorCount(int sectorCount) {
        this.sectorCount = sectorCount;
    }
    
    public String getStartSectorHex() {
        return toHex(sector);
    }
    
    public String getEndSectorHex() {
        return toHex(sector + sectorCount);
    }
    
    public String getSectorCountHex() {
        return toHex(sectorCount);
    }
    
    private static String toHex(int i) {
        return Inspector.toHex(Converter.intToBytesBE(i)).toUpperCase();
    }
    
    /**
     * This is the address which is stored in PSX-EXE to refer to this sector.
     * @return 
     */
    public String getSectorAddressCountStoredHex() {
        return getSectorAddressCountStoredHex(this.sector, this.sectorCount);
    }
    
    public static String getSectorAddressCountStoredHex(int startSec, int countSec) {
        String addrHex = toHex(countSec).substring(5, 8) + toHex(startSec).substring(3, 8);
        return addrHex.substring(6, 8) +  addrHex.substring(4, 6) + addrHex.substring(2, 4) + addrHex.substring(0, 2);
    }
    
}
