
package net.markus.projects.dq4h.data;

/**
 * An abstract class representing a script entry used in {@link HeartBeatDataScriptContent}.
 */
public abstract class ScriptEntry {

    private HeartBeatDataScriptContent parent;

    public HeartBeatDataScriptContent getParent() {
        return parent;
    }

    public void setParent(HeartBeatDataScriptContent parent) {
        this.parent = parent;
    }

}
