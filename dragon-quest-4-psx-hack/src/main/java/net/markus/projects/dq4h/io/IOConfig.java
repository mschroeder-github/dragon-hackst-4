
package net.markus.projects.dq4h.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A configuration file for I/O {@link DragonQuestReader} and {@link DragonQuestWriter} implementations.
 */
public class IOConfig {

    //to know what type has special file contents
    private Set<Short> textContentTypes;
    private Set<Short> scriptContentTypes;
    
    private List<ChangeLogEntry> changeLogEntries;
    
    public IOConfig() {
        textContentTypes = new HashSet<>(Arrays.asList((short)40, (short)42));
        scriptContentTypes = new HashSet<>(Arrays.asList((short)39));
        
        changeLogEntries = new ArrayList<>();
    }

    public Set<Short> getTextContentTypes() {
        return textContentTypes;
    }

    public Set<Short> getScriptContentTypes() {
        return scriptContentTypes;
    }

    public List<ChangeLogEntry> getChangeLogEntries() {
        return changeLogEntries;
    }
 
    
}
