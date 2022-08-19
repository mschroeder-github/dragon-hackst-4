
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.io.Converter;
import net.markus.projects.dq4h.io.Inspector;

/**
 * This is an entry in {@link HeartBeatDataTextContent} where a variable (4 bytes) is
 * associated with a dialog pointer.
 */
public class VariableToDialogPointer implements HuffmanCharacterReferrer {

    private byte[] variable;
    private byte[] value;
    
    private HuffmanCharacter referredCharacter;
    
    private HeartBeatDataTextContent parent;

    public VariableToDialogPointer() {
    }

    public VariableToDialogPointer(byte[] key, byte[] value) {
        this.variable = key;
        this.value = value;
    }

    public byte[] getVariable() {
        return variable;
    }

    public void setVariable(byte[] key) {
        this.variable = key;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    
    public byte[] getTextId() {
        String textId = Inspector.toHex(value);
        return Inspector.toBytes("0" + textId.substring(0, 3));
    }
    
    /**
     * A 3 byte little endian value;
     * @return 
     */
    public byte[] getBitOffset() {
        String textId = Inspector.toHex(value);
        return Inspector.toBytes("0" + textId.substring(3, 8));
    }
    
    public int getBitOffsetAsInt() {
        byte[] bitOffset = getBitOffset();
        return Converter.bytesToIntBE(new byte[] {0x00, bitOffset[0], bitOffset[1], bitOffset[2]});
    }
    
    /**
     * Sets the bit offset given as an int.
     * This updates the {@link #getValue() () } byte array and reuses
     * the {@link #getTextId() }.
     * @param bitOffset 
     */
    public void updatesBitOffsetAsInt(int bitOffset) {
        //hex
        //123 45678
        //tex offset
        
        String texPart = Inspector.toHex(getTextId()).substring(1, 4);
        String offsetPart = Inspector.toHex(Converter.intToBytesBE(bitOffset)).substring(3, 8);
        
        String hex = texPart + offsetPart;
        byte[] bytes = Inspector.toBytes(hex);
        
        this.value = Converter.reverse(bytes);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VariableToDialogPointer{");
        sb.append("variable=").append(Inspector.toHex(variable));
        sb.append(", value=").append(Inspector.toHex(value));
        sb.append(", textId=").append(Inspector.toHex(getTextId()));
        sb.append(", bitOffset=").append(Inspector.toHex(getBitOffset()));
        sb.append(", bitOffsetAsInt=").append(getBitOffsetAsInt());
        sb.append(", referredCharacter=").append(referredCharacter);
        sb.append('}');
        return sb.toString();
    }
    
    @Override
    public HuffmanCharacter getReference() {
        return referredCharacter;
    }

    @Override
    public void setReference(HuffmanCharacter c) {
        this.referredCharacter = c;
    }
    
    @Override
    public boolean hasReference() {
        return referredCharacter != null;
    }

    public HeartBeatDataTextContent getParent() {
        return parent;
    }

    public void setParent(HeartBeatDataTextContent parent) {
        this.parent = parent;
    }
    
}
