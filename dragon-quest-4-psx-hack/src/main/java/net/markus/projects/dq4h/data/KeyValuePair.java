
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.io.Inspector;

/**
 * 
 */
public class KeyValuePair {

    private byte[] key;
    private byte[] value;

    public KeyValuePair() {
    }

    public KeyValuePair(byte[] key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("KeyValuePair{");
        sb.append("key=").append(Inspector.toHex(key));
        sb.append(", value=").append(Inspector.toHex(value));
        sb.append('}');
        return sb.toString();
    }
    
    
    
}
