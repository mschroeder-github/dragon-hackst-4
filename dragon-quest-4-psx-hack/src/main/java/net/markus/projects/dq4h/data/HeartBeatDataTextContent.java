
package net.markus.projects.dq4h.data;

import java.util.List;
import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.compare.DragonQuestComparator;
import net.markus.projects.dq4h.io.Inspector;
import net.markus.projects.dq4h.io.Verifier;

/**
 * Represents a file content containing dialog text encoded with huffman trees.
 */
public class HeartBeatDataTextContent extends HeartBeatDataFileContent implements DragonQuestComparator<HeartBeatDataTextContent>{

    //id stays the same
    private byte[] id;
    
    //the original offsets in the header (24 bytes)
    private int originalEnd;
    private int originalHuffmanTextStart;
    private int originalHuffmanTreeEnd;
    private int originalHuffmanTextEnd;
    private int originalUnknown1;
    
    //dataHeaderToHuffmanCode; byte array from data header end to huffman text start
    private byte[] originalUnknown2;
    
    //was the "DA" block
    private byte[] originalUnknown3;
    
    private int originalHuffmanTreeStart;
    private int originalHuffmanTreeMiddle;
    private short originalHuffmanTreeNumberOfNodes;
    
    private int originalAtEnd;
    private int originalNumberOfPointers;
    
    //the actual content
    
    private HuffmanNode tree;
    private List<HuffmanCharacter> text;
    private List<KeyValuePair> keyValuePairs;

    /**
     * The ID is a four byte array starting with zeros, e.g. 0x0000006C. 
     * IDs are never longer than 2 bytes.
     * @return 
     */
    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public int getOriginalEnd() {
        return originalEnd;
    }

    public void setOriginalEnd(int originalEnd) {
        this.originalEnd = originalEnd;
    }

    public int getOriginalHuffmanTextStart() {
        return originalHuffmanTextStart;
    }

    public void setOriginalHuffmanTextStart(int originalHuffmanTextStart) {
        this.originalHuffmanTextStart = originalHuffmanTextStart;
    }

    public int getOriginalHuffmanTreeEnd() {
        return originalHuffmanTreeEnd;
    }

    public void setOriginalHuffmanTreeEnd(int originalHuffmanTreeEnd) {
        this.originalHuffmanTreeEnd = originalHuffmanTreeEnd;
    }

    public int getOriginalHuffmanTextEnd() {
        return originalHuffmanTextEnd;
    }

    public void setOriginalHuffmanTextEnd(int originalHuffmanTextEnd) {
        this.originalHuffmanTextEnd = originalHuffmanTextEnd;
    }

    /**
     * An int after the header.
     * @return 
     */
    public int getOriginalUnknown1() {
        return originalUnknown1;
    }

    public void setOriginalUnknown1(int originalUnknown1) {
        this.originalUnknown1 = originalUnknown1;
    }

    /**
     * Sometimes there is some space between the header's end and the text start.
     * @return 
     */
    public byte[] getOriginalUnknown2() {
        return originalUnknown2;
    }

    public void setOriginalUnknown2(byte[] originalUnknown2) {
        this.originalUnknown2 = originalUnknown2;
    }

    /**
     * Was dataDA.
     * @return 
     */
    public byte[] getOriginalUnknown3() {
        return originalUnknown3;
    }

    public void setOriginalUnknown3(byte[] originalUnknown3) {
        this.originalUnknown3 = originalUnknown3;
    }

    public int getOriginalHuffmanTreeStart() {
        return originalHuffmanTreeStart;
    }

    public void setOriginalHuffmanTreeStart(int originalHuffmanTreeStart) {
        this.originalHuffmanTreeStart = originalHuffmanTreeStart;
    }

    public int getOriginalHuffmanTreeMiddle() {
        return originalHuffmanTreeMiddle;
    }

    public void setOriginalHuffmanTreeMiddle(int originalHuffmanTreeMiddle) {
        this.originalHuffmanTreeMiddle = originalHuffmanTreeMiddle;
    }

    public short getOriginalHuffmanTreeNumberOfNodes() {
        return originalHuffmanTreeNumberOfNodes;
    }

    public void setOriginalHuffmanTreeNumberOfNodes(short originalHuffmanTreeNumberOfNodes) {
        this.originalHuffmanTreeNumberOfNodes = originalHuffmanTreeNumberOfNodes;
    }

    public int getOriginalAtEnd() {
        return originalAtEnd;
    }

    public void setOriginalAtEnd(int originalAtEnd) {
        this.originalAtEnd = originalAtEnd;
    }

    public int getOriginalNumberOfPointers() {
        return originalNumberOfPointers;
    }

    public void setOriginalNumberOfPointers(int originalNumberOfPointers) {
        this.originalNumberOfPointers = originalNumberOfPointers;
    }

    //main content ==============
    
    public HuffmanNode getTree() {
        return tree;
    }

    public List<HuffmanCharacter> getText() {
        return text;
    }
    
    public void setTree(HuffmanNode treeRoot) {
        this.tree = treeRoot;
    }

    public void setText(List<HuffmanCharacter> text) {
        this.text = text;
    }

    public List<KeyValuePair> getKeyValuePairs() {
        return keyValuePairs;
    }

    public void setKeyValuePairs(List<KeyValuePair> keyValuePairs) {
        this.keyValuePairs = keyValuePairs;
    }
    
    //to string ==================
    
    public String getTextAsString() {
        StringBuilder sb = new StringBuilder();
        for(HuffmanCharacter c : text) {
            sb.append(c.asString());
        }
        return sb.toString();
    }

    public String getTreeAsString() {
        return tree.toStringTree();
    }

    public String getKeyValuePairsAsString() {
        StringBuilder sb = new StringBuilder();
        for(KeyValuePair kvp : keyValuePairs) {
            sb.append(kvp.toString()).append("\n");
        }
        return sb.toString().trim();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HeartBeatDataTextContent{");
        sb.append("id=").append(Inspector.toHex(id));
        sb.append(", originalEnd=").append(originalEnd);
        sb.append(", originalHuffmanTextStart=").append(originalHuffmanTextStart);
        sb.append(", originalHuffmanTreeEnd=").append(originalHuffmanTreeEnd);
        sb.append(", originalHuffmanTextEnd=").append(originalHuffmanTextEnd);
        sb.append(", originalUnknown1=").append(originalUnknown1);
        sb.append(", originalUnknown2=(").append(originalUnknown2.length).append(" bytes)");
        sb.append(", originalUnknown3=(").append(originalUnknown3.length).append(" bytes)");
        sb.append(", originalHuffmanTreeStart=").append(originalHuffmanTreeStart);
        sb.append(", originalHuffmanTreeMiddle=").append(originalHuffmanTreeMiddle);
        sb.append(", originalHuffmanTreeNumberOfNodes=").append(originalHuffmanTreeNumberOfNodes);
        sb.append(", originalAtEnd=").append(originalAtEnd);
        sb.append(", originalNumberOfPointers=").append(originalNumberOfPointers);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void compare(HeartBeatDataTextContent other, ComparatorReport report) {
        
        Verifier.compareBytes(this, this.id, other, other.id, "id", report);
        Verifier.compareBytes(this, this.originalUnknown2, other, other.originalUnknown2, "originalUnknown2", report);
        Verifier.compareBytes(this, this.originalUnknown3, other, other.originalUnknown3, "originalUnknown3", report);
        Verifier.compareNumbers(this, this.originalHuffmanTreeNumberOfNodes, other, other.originalHuffmanTreeNumberOfNodes, "originalHuffmanTreeNumberOfNodes", report);
        Verifier.compareLists(this, this.text, other, other.text, "text", report);
     
        //all offsets could have changed because tree and text byte arrays changed
    }
    
    
    
    
    
    
    
}
