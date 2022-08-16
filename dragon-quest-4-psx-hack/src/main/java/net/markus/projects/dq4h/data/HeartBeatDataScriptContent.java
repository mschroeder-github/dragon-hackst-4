
package net.markus.projects.dq4h.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a file content containing cut scene script in a special script language.
 */
public class HeartBeatDataScriptContent extends HeartBeatDataFileContent {

    private List<Integer> originalHeader;
    private byte[] originalUnknown;
    
    private List<ScriptEntry> entries;
    
    public HeartBeatDataScriptContent() {
        originalHeader = new ArrayList<>();
        entries = new ArrayList<>();
    }

    /**
     * The header has 23 ints.
     * @return 
     */
    public List<Integer> getOriginalHeader() {
        return originalHeader;
    }

    public void setOriginalHeader(List<Integer> originalHeader) {
        this.originalHeader = originalHeader;
    }
    
    /**
     * The byte array after the script.
     * This has the (ASCII) "LLLL" sequences.
     * @return 
     */
    public byte[] getOriginalUnknown() {
        return originalUnknown;
    }

    public void setOriginalUnknown(byte[] originalUnknown) {
        this.originalUnknown = originalUnknown;
    }

    public List<ScriptEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<ScriptEntry> entries) {
        this.entries = entries;
    }

    /**
     * Returns only {@link ScriptStoreEntry}s with the given text id in params.
     * List is sorted by bit offset acending.
     * @param textId2Bytes
     * @return 
     */
    public List<ScriptStoreEntry> getStoreEntries(byte[] textId2Bytes) {
        List<ScriptStoreEntry> list = new ArrayList<>();
        for(ScriptEntry entry : getEntries()) {
            if(entry instanceof ScriptStoreEntry) {
                ScriptStoreEntry store = (ScriptStoreEntry) entry;
                if(Arrays.equals(store.getTextId(), textId2Bytes)) {
                    list.add(store);
                }
            }
        }
        list.sort((a,b) -> Integer.compare(a.getBitOffsetAsInt(), b.getBitOffsetAsInt()));
        return list;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HeartBeatDataScriptContent{");
        sb.append("originalHeader=").append(originalHeader);
        sb.append(", entries=(").append(entries.size()).append(")");
        sb.append(", originalUnknown=(").append(originalUnknown.length).append(" bytes)");
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * Returns the entries as a list in a string.
     * @return 
     */
    public String toStringEntries() {
        StringBuilder sb = new StringBuilder();
        getEntries().forEach(e -> sb.append(e.toString()).append("\n"));
        return sb.toString();
    }
    
    

    
    
}
