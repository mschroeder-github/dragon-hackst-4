
package net.markus.projects.dq4h.data;

/**
 * Abstract class represents parsed content of a file.
 */
public abstract class HeartBeatDataFileContent {

    private HeartBeatDataFile parent;
    private boolean performPatch;
    
    public HeartBeatDataFile getParent() {
        return parent;
    }

    public void setParent(HeartBeatDataFile parent) {
        this.parent = parent;
    }

    public boolean isPerformPatch() {
        return performPatch;
    }

    public void setPerformPatch(boolean performPatch) {
        this.performPatch = performPatch;
    }

}
