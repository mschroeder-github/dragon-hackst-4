
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.io.Inspector;

/**
 * An entry for special separators in a {@link HeartBeatDataScriptContent}.
 */
public class ScriptSeparatorEntry extends ScriptEntry {

    private byte[] separator;

    public ScriptSeparatorEntry(byte[] separator) {
        this.separator = separator;
    }

    public byte[] getSeparator() {
        return separator;
    }

    public void setSeparator(byte[] separator) {
        this.separator = separator;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptSeparatorEntry{");
        sb.append("separator=").append(Inspector.toHex(separator));
        sb.append('}');
        return sb.toString();
    }

    
    
    

}
