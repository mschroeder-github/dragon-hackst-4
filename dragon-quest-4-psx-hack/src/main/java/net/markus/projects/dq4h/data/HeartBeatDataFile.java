
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.compare.DragonQuestComparator;
import net.markus.projects.dq4h.io.Verifier;

/**
 * A file in a {@link HeartBeatDataFolderEntry}.
 * It holds the header information and original content bytes.
 * When the content is parsed a {@link HeartBeatDataFileContent} object is stored.
 */
public class HeartBeatDataFile implements DragonQuestComparator<HeartBeatDataFile> {

    private HeartBeatDataFolderEntry parent;
    
    private int index;
    
    private int originalSize;
    
    private int originalSizeUncompressed;
    
    private byte[] originalUnknown;
    
    private short originalFlags;
    
    private short originalType;

    private byte[] originalContentBytes;
    
    private HeartBeatDataFileContent content;
    
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

    public byte[] getOriginalContentBytes() {
        return originalContentBytes;
    }

    public void setOriginalContentBytes(byte[] originalContentBytes) {
        this.originalContentBytes = originalContentBytes;
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
    
    /**
     * The path of a file consists of the parent index and the index of the file.
     * @return 
     */
    public String getPath() {
        return parent.getIndex() + "/" + getIndex();
    }

    /**
     * The content of the file as a Java object when its {@link #getOriginalContentBytes() } was parsed.
     * @return 
     */
    public HeartBeatDataFileContent getContent() {
        return content;
    }

    /**
     * This method sets a new content of the file.
     * This content is then serialized to bytes when the file is written.
     * @param content 
     */
    public void setContent(HeartBeatDataFileContent content) {
        this.content = content;
    }

    /**
     * Checks if the file's content was parsed and is available as a Java object.
     * @return true if it has content which can be accessed with {@link #getContent() }.
     */
    public boolean hasContent() {
        return content != null;
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
        sb.append(", hasContent=").append(hasContent());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void compare(HeartBeatDataFile other, ComparatorReport report) {
        
        Verifier.compareNumbers(this, this.index, other, other.index, "index", report);
        Verifier.compareNumbers(this, this.originalFlags, other, other.originalFlags, "originalFlags", report);
        Verifier.compareNumbers(this, this.originalType, other, other.originalType, "originalType", report);
        Verifier.compareBytes(this, this.originalUnknown, other, other.originalUnknown, "originalUnknown", report);
        Verifier.compareObjects(this, this.getPath(), other, other.getPath(), "getPath", report);
        Verifier.compareObjects(this, this.hasContent(), other, other.hasContent(), "hasContent", report);
        Verifier.compareObjects(this, this.isCompressed(), other, other.isCompressed(), "isCompressed", report);
        
        //compare text
        if(this.hasContent() && other.hasContent() && 
           this.getContent() instanceof HeartBeatDataTextContent &&
           other.getContent() instanceof HeartBeatDataTextContent) {
            
            HeartBeatDataTextContent thisText = (HeartBeatDataTextContent) this.getContent();
            HeartBeatDataTextContent otherText = (HeartBeatDataTextContent) other.getContent();
            
            thisText.compare(otherText, report);
        }
        
        //could have changed:
        //* size
        //* size uncompressed
        
    }
    
}
