
package net.markus.projects.dq4h.data;

/**
 * An entry which starts with the H60010108 header.
 * It has one 2048 byte sector.
 */
public class HeartBeatData60010108Entry extends HeartBeatDataEntry {

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
    
}
