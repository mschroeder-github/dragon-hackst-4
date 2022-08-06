
package net.markus.projects.dq4h.data;

/**
 * This can be used for the first entry which is a 2048 byte block.
 */
public class HeartBeatDataBinaryEntry extends HeartBeatDataEntry {

    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
    
}
