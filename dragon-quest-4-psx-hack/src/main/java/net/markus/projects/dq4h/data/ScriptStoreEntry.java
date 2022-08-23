
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.io.Converter;
import net.markus.projects.dq4h.io.Inspector;

/**
 * The 0xc021a0 command which puts dialog pointers.
 * It implements {@link HuffmanCharacterReferrer} because it can refer to a {@link HuffmanCharacter}.
 */
public class ScriptStoreEntry extends ScriptEntry implements HuffmanCharacterReferrer {

    /**
     * The command: 0xc021a0
     */
    public static final byte[] STORE = new byte[] { (byte) 0xc0, (byte) 0x21, (byte) 0xa0 };
    
    private byte[] cmd;
    private byte[] params;
    
    private HuffmanCharacter referredCharacter;

    public ScriptStoreEntry(byte[] params) {
        this.cmd = STORE;
        this.params = params;
    }

    public byte[] getCmd() {
        return cmd;
    }

    public void setCmd(byte[] cmd) {
        this.cmd = cmd;
    }
    
    /**
     * Can be a dialog pointer, e.g. when reversed 0x06c00f91.
     * The params are not stored in reversed order.
     * @return 
     */
    public byte[] getParams() {
        return params;
    }

    public void setParams(byte[] params) {
        this.params = params;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptStoreEntry{");
        sb.append("params=").append(Inspector.toHex(params));
        sb.append(", reversed=").append(Inspector.toHex(Converter.reverse(params)));
        sb.append(", textId=").append(Inspector.toHex(getTextId()));
        sb.append(", bitOffset=").append(Inspector.toHex(getBitOffset()));
        sb.append(", bitOffsetAsInt=").append(getBitOffsetAsInt());
        sb.append(", referredCharacter=").append(referredCharacter);
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * Text Id from params in two byte.
     * @return 
     */
    public byte[] getTextId() {
        String param = Inspector.toHex(Converter.reverse(params));
        return Inspector.toBytes("0" + param.substring(0, 3));
    }
    
    
    public int getBitOffsetAsInt() {
        byte[] bitOffset = getBitOffset();
        return Converter.bytesToIntBE(new byte[] {0x00, bitOffset[0], bitOffset[1], bitOffset[2]});
    }
    
    /**
     * Sets the bit offset given as an int.
     * This updates the {@link #getParams() } byte array and reuses
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
        
        this.params = Converter.reverse(bytes);
    }
    
    public byte[] getBitOffset() {
        String param = Inspector.toHex(Converter.reverse(params));
        return Inspector.toBytes("0" + param.substring(3, 8));
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
    
    
}
