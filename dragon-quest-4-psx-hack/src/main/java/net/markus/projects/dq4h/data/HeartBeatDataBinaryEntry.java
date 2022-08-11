
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.compare.DragonQuestComparator;
import net.markus.projects.dq4h.io.Verifier;

/**
 * This can be used for the first entry which is a 2048 byte block and all zero blocks at the end of HBD1PS1D.
 */
public class HeartBeatDataBinaryEntry extends HeartBeatDataEntry implements DragonQuestComparator<HeartBeatDataBinaryEntry> {

    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void compare(HeartBeatDataBinaryEntry other, ComparatorReport report) {
        Verifier.compareNumbers(this, this.index, other, other.index, "index", report);
        Verifier.compareBytes(this, this.data, other, other.data, "data", report);
    }
    
}
