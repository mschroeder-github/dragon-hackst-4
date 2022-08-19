
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.OutputStream;
import net.markus.projects.dq4h.data.HeartBeatDataScriptContent;
import net.markus.projects.dq4h.data.ScriptBinaryEntry;
import net.markus.projects.dq4h.data.ScriptEntry;
import net.markus.projects.dq4h.data.ScriptNopEntry;
import net.markus.projects.dq4h.data.ScriptSectionEntry;
import net.markus.projects.dq4h.data.ScriptSeparatorEntry;
import net.markus.projects.dq4h.data.ScriptStoreEntry;

/**
 * Writes {@link HeartBeatDataScriptContent}.
 */
public class HeartBeatDataScriptContentWriter extends DragonQuestWriter<HeartBeatDataScriptContent> {

    @Override
    public void write(HeartBeatDataScriptContent script, OutputStream output) throws IOException {
        
        //if we have to patch we write everything
        DragonQuestOutputStream dqos = new DragonQuestOutputStream(output);
        
        //write header
        for(Integer headerEntry : script.getOriginalHeader()) {
            dqos.writeIntLE(headerEntry);
        }
        
        for(ScriptEntry entry : script.getEntries()) {
            
            //many possibilities
            
            if(entry instanceof ScriptNopEntry) {
                dqos.write(ScriptNopEntry.NOP);
                
            } else if(entry instanceof ScriptSectionEntry) {
                ScriptSectionEntry section = (ScriptSectionEntry) entry;
                dqos.write(Inspector.toBytes("b" + section.getType()));
                dqos.writeIntLE(section.getNumber());
                
            } else if(entry instanceof ScriptSeparatorEntry) {
                ScriptSeparatorEntry separator = (ScriptSeparatorEntry) entry;
                dqos.writeBytesBE(separator.getSeparator());
                
            } else if(entry instanceof ScriptStoreEntry) {
                ScriptStoreEntry store = (ScriptStoreEntry) entry;
                dqos.writeBytesBE(ScriptStoreEntry.STORE);
                dqos.writeBytesBE(store.getParams());
                
            } else if(entry instanceof ScriptBinaryEntry) {
                ScriptBinaryEntry binary = (ScriptBinaryEntry) entry;
                dqos.writeBytesBE(binary.getCommand());
                dqos.writeBytesBE(binary.getParameters());
                
            }
            
        }
        
        dqos.writeBytesBE(script.getOriginalUnknown());
    }

}
