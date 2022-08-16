
package net.markus.projects.dq4h.data;

/**
 * A referrer which refers to an {@link HuffmanCharacter}.
 */
public interface HuffmanCharacterReferrer {

    public HuffmanCharacter getReference();
    
    public boolean hasReference();
    
    public void setReference(HuffmanCharacter c);
    
}
