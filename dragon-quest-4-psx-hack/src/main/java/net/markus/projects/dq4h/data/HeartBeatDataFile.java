
package net.markus.projects.dq4h.data;

/**
 * A file in a {@link HeartBeatDataFolderEntry}.
 */
public class HeartBeatDataFile {

    private HeartBeatDataFolderEntry parent;
    
    private int index;
    
    private int originalSize;
    
    private int originalSizeUncompressed;
    
    private byte[] originalUnknown;
    
    private short originalFlags;
    
    private short originalType;

    private byte[] originalBytes;
    
    public HeartBeatDataFolderEntry getParent() {
        return parent;
    }

    public void setParent(HeartBeatDataFolderEntry parent) {
        this.parent = parent;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
    
    public int getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(int originalSize) {
        this.originalSize = originalSize;
    }

    public int getOriginalSizeUncompressed() {
        return originalSizeUncompressed;
    }

    public void setOriginalSizeUncompressed(int originalSizeUncompressed) {
        this.originalSizeUncompressed = originalSizeUncompressed;
    }

    public byte[] getOriginalUnknown() {
        return originalUnknown;
    }

    public void setOriginalUnknown(byte[] originalUnknown) {
        this.originalUnknown = originalUnknown;
    }

    public short getOriginalFlags() {
        return originalFlags;
    }

    public void setOriginalFlags(short originalFlags) {
        this.originalFlags = originalFlags;
    }

    public short getOriginalType() {
        return originalType;
    }

    public void setOriginalType(short originalType) {
        this.originalType = originalType;
    }

    public byte[] getOriginalBytes() {
        return originalBytes;
    }

    public void setOriginalBytes(byte[] originalBytes) {
        this.originalBytes = originalBytes;
    }
    
    /**
     * Checks if the file is compressed.
     * We use the information of the normal and uncompressed size.
     * If there is a difference we recognize the file as compressed.
     * @return true, if file is compressed
     */
    public boolean isCompressed() {
        return originalSizeUncompressed != originalSize && originalFlags != 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HeartBeatDataFile{");
        sb.append("path=").append(getPath());
        sb.append(", originalSize=").append(originalSize);
        sb.append(", originalSizeUncompressed=").append(originalSizeUncompressed);
        sb.append(", originalFlags=").append(originalFlags);
        sb.append(", originalType=").append(originalType);
        sb.append(", isCompressed=").append(isCompressed());
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * The path of a file consists of the parent index and the index of the file.
     * @return 
     */
    public String getPath() {
        return parent.getIndex() + "/" + getIndex();
    }

    
    
}
