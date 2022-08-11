
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.compare.DragonQuestComparator;
import net.markus.projects.dq4h.io.Verifier;

/**
 * An entry which starts with the H60010108 header.
 * It has one 2048 byte sector.
 */
public class HeartBeatData60010108Entry extends HeartBeatDataEntry implements DragonQuestComparator<HeartBeatData60010108Entry> {

    private byte[] data;

    public byte[] getData() {
        return data;
    }

    /**
     * For now we only save the binary data of it.
     * @param data 
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void compare(HeartBeatData60010108Entry other, ComparatorReport report) {
        Verifier.compareNumbers(this, this.index, other, other.index, "index", report);
        Verifier.compareBytes(this, this.data, other, other.data, "data", report);
    }
    
}
