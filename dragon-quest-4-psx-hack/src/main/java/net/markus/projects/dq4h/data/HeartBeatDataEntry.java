
package net.markus.projects.dq4h.data;

/**
 * An abstract entry in the {@link HeartBeatData}.
 */
public abstract class HeartBeatDataEntry {

    protected int index;

    /**
     * The index of the entry.
     * @return 0-based index.
     */
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
    
}
