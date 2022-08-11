
package net.markus.projects.dq4h.data;

/**
 * Abstract class represents parsed content of a file.
 */
public abstract class HeartBeatDataFileContent {

    private HeartBeatDataFile parent;

    public HeartBeatDataFile getParent() {
        return parent;
    }

    public void setParent(HeartBeatDataFile parent) {
        this.parent = parent;
    }
    
}
