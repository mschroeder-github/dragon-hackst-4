
package net.markus.projects.dq4h.translation;

import java.util.Arrays;
import net.markus.projects.dq4h.io.ShiftJIS;

/**
 * Tracks where japenese text occurs.
 */
public class JapaneseTextOccurrence {

    //can be any source
    private Object source;
    
    //the bytes of the source
    private byte[] bytes;
    
    //position in bytes
    private int start;
    private int end;
    
    private boolean swapped;

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
    
    public int getLength() {
        return (end - start) / 2;
    }
    
    public byte[] getCoveredBytes() {
        return Arrays.copyOfRange(bytes, start, end);
    }

    public boolean isSwapped() {
        return swapped;
    }

    public void setSwapped(boolean swapped) {
        this.swapped = swapped;
    }

    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JapaneseTextOccurrence{");
        sb.append("text=").append(ShiftJIS.getString(getCoveredBytes()));
        sb.append(", start=").append(start);
        sb.append(", end=").append(end);
        sb.append(", swapped=").append(swapped);
        sb.append(", source=").append(source);
        sb.append('}');
        return sb.toString();
    }

    
    
}
