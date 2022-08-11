
package net.markus.projects.dq4h.compare;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to report in {@link DragonQuestComparator} what is different.
 */
public class ComparatorReport {
    
    private List<ComparatorReportEntry> entries;
    
    private int checkCounter;
    
    public ComparatorReport() {
        entries = new ArrayList<>();
    }
    
    public void add(ComparatorReportEntry entry) {
        entries.add(entry);
    }

    /**
     * To measure how many checks were made.
     */
    public void increaseCheckCounter() {
        checkCounter++;
    }

    public int getCheckCounter() {
        return checkCounter;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if(entries.isEmpty()) {
            sb.append("No entries (").append(checkCounter).append(" checks)");
        } else {
            
            sb.append(entries.size()).append(" ");
            sb.append(entries.size() == 1 ? "entry" : "entries").append(" (").append(checkCounter).append(" checks)").append("\n");
            
            for(ComparatorReportEntry entry : entries) {
                sb.append(entry.toString()).append("\n\n");
            }
        }
        
        return sb.toString();
    }
 
    
    
}
