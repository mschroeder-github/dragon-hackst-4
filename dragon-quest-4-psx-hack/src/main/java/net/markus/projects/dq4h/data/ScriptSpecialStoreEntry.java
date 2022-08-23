
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.io.Converter;
import net.markus.projects.dq4h.io.Inspector;

/**
 * A special store command in {@link HeartBeatDataScriptContent}.
 * Command is c0267b or c02678 or c0263b.
 * They have different number of parameter bytes but they have in common
 * that how bitoffset and text id are encoded at the end.
 */
public class ScriptSpecialStoreEntry extends ScriptStoreEntry {

    public ScriptSpecialStoreEntry(byte[] command, byte[] parameters) {
        super(parameters);
        this.setCmd(command);
    }

    /**
     * Text Id from params in two byte.
     * @return 
     */
    @Override
    public byte[] getTextId() {
        //e.g. 0a0024 | eb06e01b  => 01be
        String param = Inspector.toHex(getParams());
        return Inspector.toBytes(param.substring(param.length() - 3, param.length()) + param.charAt(param.length() - 4));
    }
    
    @Override
    public int getBitOffsetAsInt() {
        byte[] bitOffset = getBitOffset();
        return Converter.bytesToIntBE(new byte[] {0x00, 0x00, bitOffset[0], bitOffset[1]});
    }
    
    /**
     * Sets the bit offset given as an int.
     * This updates the {@link #getParams() } byte array and reuses
     * the {@link #getTextId() }.
     * @param bitOffset 
     */
    @Override
    public void updatesBitOffsetAsInt(int bitOffset) {
        
        
        //c0267b with 0a0024 | eb06 | e01b (7 bytes param)
        //or
        //c02678 with fd | 201e | f022 (5 bytes param)
        
        String offsetPart = Inspector.toHex(Converter.intToBytesBE(bitOffset)).substring(4, 8);
        offsetPart = offsetPart.substring(2, 4) +  offsetPart.substring(0, 2);
        
        String param = Inspector.toHex(getParams());
        String texPart = param.substring(param.length() - 4, param.length());
        
        String remaining = param.substring(0, param.length() - 8);
        
        byte[] bytes = Inspector.toBytes(remaining + offsetPart + texPart);
        
        this.setParams(bytes);
    }
    
    @Override
    public byte[] getBitOffset() {
        //e.g. 0a0024 | eb06e01b  => 06eb
        String param = Inspector.toHex(getParams());
        return Inspector.toBytes(
                param.substring(param.length() - 6, param.length() - 4) +
                param.substring(param.length() - 8, param.length() - 6)
        );
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptSpecialStoreEntry{");
        sb.append("cmd=").append(Inspector.toHex(getCmd()));
        sb.append(", params=").append(Inspector.toHex(getParams()));
        sb.append(", textId=").append(Inspector.toHex(getTextId()));
        sb.append(", bitOffset=").append(Inspector.toHex(getBitOffset()));
        sb.append(", bitOffsetAsInt=").append(getBitOffsetAsInt());
        sb.append(", referredCharacter=").append(getReference());
        sb.append('}');
        return sb.toString();
    }
    
}
