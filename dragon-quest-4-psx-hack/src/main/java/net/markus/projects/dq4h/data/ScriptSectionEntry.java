
package net.markus.projects.dq4h.data;

/**
 * Represents a section entry in {@link HeartBeatDataScriptContent}.
 * Can be 0xb1 or 0xb2 (maybe more).
 * A script entry has also a number (maybe id).
 */
public class ScriptSectionEntry extends ScriptEntry {

    private int type;
    private int number;

    public ScriptSectionEntry(int type, int number) {
        this.type = type;
        this.number = number;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptSectionEntry{");
        sb.append("type=").append(type);
        sb.append(", number=").append(number);
        sb.append('}');
        return sb.toString();
    }
    
}
