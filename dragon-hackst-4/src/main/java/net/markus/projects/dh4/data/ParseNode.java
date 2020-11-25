package net.markus.projects.dh4.data;

import java.util.LinkedList;
import java.util.List;

public class ParseNode {
    
    private List<ParseNode> children;
    
    private String coveredText;
    private String label;
    
    public ParseNode left;
    public ParseNode right;
    
    public byte[] data;

    public ParseNode() {
        this.children = new LinkedList<>();
    }
    
    public ParseNode(String coveredText, String label) {
        this();
        this.coveredText = coveredText;
        this.label = label;
    }
    
    public void setLeft(ParseNode left) {
        if(left != null) {
            this.left = left;
            getChildren().add(left);
        }
    }
    
    public void setRight(ParseNode right) {
        if(right != null) {
            this.right = right;
            getChildren().add(right);
        }
    }
    
    /**
     * The children of this node.
     * @return 
     */
    public List<ParseNode> getChildren() {
        return children;
    }

    public ParseNode getChild(int index) {
        if(isEmpty())
            throw new RuntimeException("no children available");
        return children.get(index);
    }
    
    public ParseNode getFirstChild() {
        if(isEmpty())
            throw new RuntimeException("no children available");
        return children.get(0);
    }
    
    public ParseNode getLastChild() {
        if(isEmpty())
            throw new RuntimeException("no children available");
        return children.get(children.size()-1);
    }
    
    public ParseNode getChildByLabel(String label) {
        for(ParseNode child : children) {
            if(child.getLabel().equals(label))
                return child;
        }
        return null;
    }
    
    public ParseNode pruneChildByLabel(String label) {
        ParseNode pn = getChildByLabel(label);
        if(pn == null)
            return null;
        children.remove(pn);
        return pn;
    }
    
    public boolean hasChildren() {
        return !children.isEmpty();
    }
    
    public int size() {
        return children.size();
    }
    
    public boolean isEmpty() {
        return children.isEmpty();
    }
    
    /**
     * The text the node covers in the parsed input.
     * @return covered text.
     */
    public String getCoveredText() {
        return coveredText;
    }

    /**
     * The label of the node.
     * This is the type of the node.
     * @return label name.
     */
    public String getLabel() {
        return label;
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
    public String toString() {
        return getLabel() + " '" + getCoveredText() + "'";
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
    }
}
