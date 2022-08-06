
package net.markus.projects.dq4h.data;

/**
 * Represents a playstation system config file.
 */
public class SystemConfig {

    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return new String(data);
    }
    
}
