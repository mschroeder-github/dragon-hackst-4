
package net.markus.projects.dq4h.data;

import java.util.ArrayList;
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
    
    private byte[] originalTreeBytes;
    private byte[] originalTextBytes;
    private List<VariableToDialogPointer> originalDialogPointers;
    
    private HuffmanNode originalTree;
    private List<HuffmanCharacter> originalText;
    
    //the content which can be changed
    
    private HuffmanNode tree;
    private List<HuffmanCharacter> text;

    public HeartBeatDataTextContent() {
        this.originalDialogPointers = new ArrayList<>();
    }

    /**
     * The ID is a four byte array starting with zeros, e.g. 0x0000006C. 
     * IDs are never longer than 2 bytes.
     * @return 
     */
    public byte[] getId() {
        return id;
    }
    
    /**
     * The id but in two bytes, e.g. 0x023d.
     * See {@link #getId() }.
     * @return 
     */
    public byte[] getId2Bytes() {
        return new byte[] { id[2], id[3] };
    }
    
    /**
     * Returns the offset in bits where the text starts.
     * This takes also into account {@link #getOriginalUnknown2() }.
     * Usually this is 24 bytes * 8 bits = 192 = 0xc0.
     * @return 
     */
    public int getTextBitOffset() {
        return (24 * 8) + (originalUnknown2.length * 8);
    }
    
    /**
     * Two bytes hex representation of the text id.
     * @return 
     */
    public String getIdHex() {
        return Inspector.toHex(getId2Bytes());
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

    public byte[] getOriginalTreeBytes() {
        return originalTreeBytes;
    }

    public void setOriginalTreeBytes(byte[] originalTreeBytes) {
        this.originalTreeBytes = originalTreeBytes;
    }

    public byte[] getOriginalTextBytes() {
        return originalTextBytes;
    }

    public void setOriginalTextBytes(byte[] originalTextBytes) {
        this.originalTextBytes = originalTextBytes;
    }

    public List<VariableToDialogPointer> getOriginalDialogPointers() {
        return originalDialogPointers;
    }

    public void setOriginalDialogPointers(List<VariableToDialogPointer> originalDialogPointers) {
        this.originalDialogPointers = originalDialogPointers;
    }

    public HuffmanNode getOriginalTree() {
        return originalTree;
    }

    public void setOriginalTree(HuffmanNode originalTree) {
        this.originalTree = originalTree;
    }

    public List<HuffmanCharacter> getOriginalText() {
        return originalText;
    }

    public void setOriginalText(List<HuffmanCharacter> originalText) {
        this.originalText = originalText;
    }
    
    
    //main content ==============
    
    public HuffmanNode getTree() {
        return tree;
    }

    public List<HuffmanCharacter> getText() {
        return text;
    }
    
    /**
     * Calculates the characters which are the start of a sentence.
     * The result is based on {@link #getOriginalText() }.
     * See {@link #getStartCharacters(java.util.List)}.
     * @return
     */
    public List<HuffmanCharacter> getOriginalStartCharacters() {
        return getStartCharacters(originalText);
    }
    
    /**
     * Calculates the characters which are the start of a sentence.
     * We use the {0000} control character as indicator that after this strings starts another string.
     * @param givenText
     * @return 
     */
    public static List<HuffmanCharacter> getStartCharacters(List<HuffmanCharacter> givenText) {
        List<HuffmanCharacter> startChars = new ArrayList<>();
        
        boolean pick = true;
        for(HuffmanCharacter c : givenText) {
            if(pick) {
                startChars.add(c);
                pick = false;
            }
            
            //if {0000} pick next character
            if(c.getNode().isNullCharacter()) {
                pick = true;
            }
        }
        
        /*
        HuffmanCharacter lastOne = getOriginalText().get(getOriginalText().size() - 1);
        //if the last one is not {0000}
        if(!(lastOne.getNode().isControlCharacter() && lastOne.getNode().isZero())) {
            //then the last startChars is invalid and can be deleted
            startChars.remove(startChars.size() - 1);
        }
        
        //remove all {0000}, they cannot be starting characters. this happens in the dummy texts
        startChars.removeIf(c -> c.getNode().isControlCharacter() && lastOne.getNode().isZero());
        */
        
        return startChars;
    }
    
    public void setTree(HuffmanNode treeRoot) {
        this.tree = treeRoot;
    }

    public void setText(List<HuffmanCharacter> text) {
        this.text = text;
    }
    
    //to string ==================
    
    public String getTextAsString() {
        return HuffmanCharacter.listToString(text);
    }

    public String getTreeAsString() {
        if(tree == null)
            return "";
        return tree.toStringTree();
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
        Verifier.compareLists(this, this.originalText, other, other.originalText, "originalText", report);
     
        //all offsets could have changed because tree and text byte arrays changed
    }
    
}
