
package net.markus.projects.dq4h.data;

/**
 * Represents a playstation executable file.
 */
public class PsxExe {

    private byte[] data;
    private byte[] originalData;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getOriginalData() {
        return originalData;
    }

    public void setOriginalData(byte[] originalData) {
        this.originalData = originalData;
    }
    
}
