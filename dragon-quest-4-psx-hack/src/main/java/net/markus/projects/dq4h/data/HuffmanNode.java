
package net.markus.projects.dq4h.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.markus.projects.dq4h.io.Converter;
import net.markus.projects.dq4h.io.Inspector;
import net.markus.projects.dq4h.io.ShiftJIS;
import net.markus.projects.dq4h.io.Verifier;

/**
 * Represents a node in a hufman tree used by {@link HeartBeatDataTextContent}.
 */
public class HuffmanNode {

    public enum Type {
        Branch,
        ControlCharacter,
        Character
    }
    
    private Type type;
    private byte[] content;
    
    private List<HuffmanNode> children;
    
    private double frequency;
    
    public HuffmanNode(Type type, byte[] content) {
        this.type = type;
        this.content = content;
        this.children = new ArrayList<>();
    }
    
    /**
     * Creates a {@link Type#Branch} node with the given id.
     * Uses {@link #setID(int) }.
     * @param id unique number for the branch node
     */
    public HuffmanNode(short id) {
        this.type = Type.Branch;
        setID(id);
        this.children = new ArrayList<>();
    }

    /**
     * The node can be a branch (having two children), a control character (like new line) or a character (letter or symbol).
     * @return 
     */
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isBranch() {
        return type == Type.Branch;
    }
    
    public boolean isControlCharacter() {
        return type == Type.ControlCharacter;
    }
    
    public boolean isCharacter() {
        return type == Type.Character;
    }
    
    /**
     * The node is a leaf if it is a (control) character.
     * @return 
     */
    public boolean isLeaf() {
        return isControlCharacter() || isCharacter();
    }
    
    /**
     * Returns true if the content is {0000}.
     * @return 
     */
    public boolean isZero() {
        return Verifier.allZero(content);
    }
    
    /**
     * Checks if it is '\0' which is the case
     * when {@link #isZero() } and {@link #isControlCharacter() } are true.
     * @return 
     */
    public boolean isNullCharacter() {
        return isZero() && isControlCharacter();
    }
    
    /**
     * The content of a node is always two bytes long.
     * It is already swapped, so it is e.g. {7f**} for a control character and {80**} for a japanese character.
     * It has to start with 0x80, later the writer will swap it.
     * If {@link Type#Branch} it is the id, if {@link Type#ControlCharacter} or {@link Type#Character} it is
     * the character.
     * @return 
     */
    public byte[] getContent() {
        return content;
    }
    
    /**
     * The hex representation of {@link #getContent() }.
     * @return 
     */
    public String getContentHex() {
        return Inspector.toHex(content);
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public List<HuffmanNode> getChildren() {
        return children;
    }
    
    /**
     * If this node is a {@link Type#Branch} then this method returns the node's ID number.
     * @return 
     */
    public short getID() {
        return (short) (Converter.bytesToIntBE(new byte[]{0, 0, content[0], content[1]}) - 0x8000);
    }
    
    /**
     * If this node is a {@link Type#Branch} then this method sets the node's ID number.
     * This will update the {@link #getContent() } of the node.
     * @param id 
     */
    public final void setID(short id) {
        content = Converter.shortToBytesBE(id);
        //perform 'or' with this bit field to have the correct value
        content[0] |= 0x80;
    }

    /**
     * Gets the japanese character representation using {@link ShiftJIS}.
     * @return 
     */
    public Character getCharacter() {
        byte[] copy = new byte[]{content[0], content[1]};
        copy[0] = (byte) (copy[0] + (byte) 0x80);
        return ShiftJIS.getCharacter(copy);
    }
    
    /**
     * For a {@link Type#Branch} node, returns the first child.
     * @return 
     */
    public HuffmanNode getLeftChild() {
        return children.get(0);
    }
    
    /**
     * For a {@link Type#Branch} node, returns the second child.
     * @return 
     */
    public HuffmanNode getRightChild() {
        return children.get(1);
    }

    /**
     * The frequency of a (control) character is used when the tree is built.
     * @return 
     */
    public double getFrequency() {
        return frequency;
    }
    
    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HuffmanNode{");
        sb.append("type=").append(type);
        switch(type) {
            case Branch:
                sb.append(", content=").append(getID());
                break;
            case ControlCharacter:
                sb.append(", content=").append(Inspector.toHex(content));
                break;
            case Character:
                sb.append(", content=").append(getCharacter());
                break;
        }
        
        sb.append('}');
        return sb.toString();
    }
    
    public String toStringTree() {
        StringBuilder sb = new StringBuilder();
        toStringTree("", true, sb);
        return sb.toString();
    }

    private void toStringTree(String prefix, boolean isTail, StringBuilder sb) {
        sb.append(prefix).append(isTail ? "└── " : "├── ").append(toString()).append("\n");
        for (int i = 0; i < children.size() - 1; i++) {
            children.get(i).toStringTree(prefix + (isTail ? "    " : "│   "), false, sb);
        }
        if (children.size() > 0) {
            children.get(children.size() - 1)
                    .toStringTree(prefix + (isTail ?"    " : "│   "), true, sb);
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + Objects.hashCode(this.type);
        hash = 71 * hash + Arrays.hashCode(this.content);
        return hash;
    }

    /**
     * Node are equal if they have the same type and byte content.
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HuffmanNode other = (HuffmanNode) obj;
        if (this.type != other.type) {
            return false;
        }
        return Arrays.equals(this.content, other.content);
    }
    
    
    /**
     * Returns a list of all descendants (excluding this node).
     * @return 
     */
    public List<HuffmanNode> descendants() {
        List<HuffmanNode> l = new ArrayList<>();
        for(HuffmanNode child : getChildren()) {
            l.add(child);
        }
        if(isBranch()) {
            for(HuffmanNode child : getChildren()) {
                l.addAll(child.descendants());
            }
        }
        return l;
    }
    
}
