
package net.markus.projects.dq4h.data;

import net.markus.projects.dq4h.io.Inspector;

/**
 * A binary entry for unknown commands in a {@link HeartBeatDataScriptContent}.
 */
public class ScriptBinaryEntry extends ScriptEntry {

    private byte[] command;
    private byte[] parameters;

    public ScriptBinaryEntry(byte[] command) {
        this.command = command;
        this.parameters = new byte[0];
    }

    public ScriptBinaryEntry(byte[] command, byte[] parameters) {
        this.command = command;
        this.parameters = parameters;
    }

    public byte[] getCommand() {
        return command;
    }

    public void setCommand(byte[] command) {
        this.command = command;
    }

    public byte[] getParameters() {
        return parameters;
    }

    public void setParameters(byte[] parameters) {
        this.parameters = parameters;
    }

    //@Override
    //public String toString() {
    //    return "ScriptBinaryEntry{" + "command=" + Inspector.toHex(command) + ", parameters=" + Inspector.toHex(parameters) + '}';
    //}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptBinaryEntry{");
        sb.append("command=").append(Inspector.toHex(command));
        if(parameters.length > 0) {
            sb.append(", parameters=").append(Inspector.toHex(parameters));
        }
        sb.append('}');
        return sb.toString();
    }
    
    

}
