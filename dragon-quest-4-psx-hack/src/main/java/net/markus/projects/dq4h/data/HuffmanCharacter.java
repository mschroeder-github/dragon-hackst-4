
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.compare.DragonQuestComparator;
import net.markus.projects.dq4h.io.Verifier;

/**
 * Represents a huffman character to form a text used by {@link HeartBeatDataTextContent}.
 * For its content it uses {@link HuffmanNode}.
 */
public class HuffmanCharacter implements DragonQuestComparator<HuffmanCharacter> {

    private HuffmanNode node;
    
    private String originalBits;
    private int originalBitPosition;

    public HuffmanCharacter() {
    }

    public HuffmanCharacter(HuffmanNode node) {
        this.node = node;
    }

    /**
     * A character's content is a node in the huffman tree.
     * @return 
     */
    public HuffmanNode getNode() {
        return node;
    }

    public void setNode(HuffmanNode node) {
        if(node.isBranch()) {
            throw new RuntimeException("Node in HuffmanCharacter cannot be a branch");
        }
        this.node = node;
    }

    /**
     * The original huffman bit code that represents this character.
     * @return 
     */
    public String getOriginalBits() {
        return originalBits;
    }

    public void setOriginalBits(String originalBits) {
        this.originalBits = originalBits;
    }

    /**
     * The original bit position where the character's bit code was read.
     * @return 
     */
    public int getOriginalBitPosition() {
        return originalBitPosition;
    }

    public void setOriginalBitPosition(int originalBitPosition) {
        this.originalBitPosition = originalBitPosition;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HuffmanCharacter{");
        sb.append("node=").append(node);
        sb.append(", originalBitPosition=").append(originalBitPosition);
        sb.append(", originalBits=").append(originalBits);
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * Returns a string representation: for a control character the hex value in {...} brackets, otherwise
     * the character.
     * @return 
     */
    public String asString() {
        if(node.isControlCharacter()) {
            return "{" + node.getContentHex() + "}";
        }
        return String.valueOf(node.getCharacter());
    }

    @Override
    public void compare(HuffmanCharacter other, ComparatorReport report) {
        //this checks equality for node's type and byte[] content for (control) characters
        Verifier.compareObjects(this, this.node, other, other.node, "node", report);
        
        //bits and bits position could have changed because tree changed
    }
    
}
