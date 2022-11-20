
package net.markus.projects.dq4h.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.markus.projects.dq4h.gui.ProjectListener;

/**
 * A configuration file for I/O {@link DragonQuestReader} and {@link DragonQuestWriter} implementations.
 */
public class IOConfig {

    //to know what type has special file contents
    private Set<Short> textContentTypes;
    private Set<Short> scriptContentTypes;
    
    private List<ChangeLogEntry> changeLogEntries;
    
    private boolean trace;
    private ProjectListener projectListener;
    
    public IOConfig() {
        textContentTypes = new HashSet<>(Arrays.asList((short)40, (short)42));
        scriptContentTypes = new HashSet<>(Arrays.asList((short)39));
        
        changeLogEntries = new ArrayList<>();
        
        trace = false;
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
    
    /**
     * To trace the IO operations.
     * Prints to command line.
     * @param line 
     */
    public void trace(String line) {
        if(!trace) {
            return;
        }
        
        System.out.println(line);
    }

    public boolean isTrace() {
        return trace;
    }

    /**
     * Enables tracing of IO operatios.
     * @param trace 
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    public ProjectListener getProjectListener() {
        return projectListener;
    }

    public void setProjectListener(ProjectListener projectListener) {
        this.projectListener = projectListener;
    }

    public void setProgressText(String text) {
        if(projectListener != null)
            projectListener.setProgressText(text);
    }

    public void setProgressMax(int max) {
        if(projectListener != null)
            projectListener.setProgressMax(max);
    }

    public void setProgressValue(int value) {
        if(projectListener != null)
            projectListener.setProgressValue(value);
    }
    
}
