
package net.markus.projects.dq4h.io;

/**
 * This class represents a file's content as a byte array and potentially uncompressed size, if the
 * byte array is compressed.
 * This is used when a file is written.
 */
public class FileContent {

    private byte[] bytes;
    private int sizeUncompressed;
    
    public boolean isCompressed() {
        return getSize() != getSizeUncompressed();
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getSizeUncompressed() {
        return sizeUncompressed;
    }

    public void setSizeUncompressed(int sizeUncompressed) {
        this.sizeUncompressed = sizeUncompressed;
    }
    
    /**
     * It is just length of {@link #getBytes() }.
     * @return 
     */
    public int getSize() {
        return bytes.length;
    }
    
}
