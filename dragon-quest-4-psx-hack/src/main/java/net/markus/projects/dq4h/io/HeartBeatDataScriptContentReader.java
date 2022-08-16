
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.InputStream;
import net.markus.projects.dq4h.data.HeartBeatDataScriptContent;
import net.markus.projects.dq4h.data.ScriptBinaryEntry;
import net.markus.projects.dq4h.data.ScriptNopEntry;
import net.markus.projects.dq4h.data.ScriptSectionEntry;
import net.markus.projects.dq4h.data.ScriptSeparatorEntry;
import net.markus.projects.dq4h.data.ScriptStoreEntry;

/**
 * Reads {@link HeartBeatDataScriptContent}.
 * @see <a href="https://github.com/mwilkens/dq4psxtrans#dq-scripting-language">Mandy Wilkens' GitHub Repo</a>
 */
public class HeartBeatDataScriptContentReader extends DragonQuestReader<HeartBeatDataScriptContent> {

    @Override
    public HeartBeatDataScriptContent read(InputStream input) throws IOException {
        HeartBeatDataScriptContent script = new HeartBeatDataScriptContent();
        
        DragonQuestInputStream dqis = new DragonQuestInputStream(input);
        
        //header has always 23 * int length
        int numberOfInts = 23;
        for(int i = 0; i < numberOfInts; i++) {
            script.getOriginalHeader().add(dqis.readIntLE());
        }
        //System.out.println("header: " + script.getOriginalHeader());
        
        //14th value (index is 13): this is end of script offset
        int endOfScript = script.getOriginalHeader().get(13);
        
        //System.out.println(Inspector.toHexDump(dqis.readRemaining(), 8, true, true));
        
        //on each b1 (start) comes a b2 (end)
        
        boolean inSection = false;
        boolean endOfScriptReached = false;
        
        while(dqis.getPosition() < endOfScript) {
            byte[] first = dqis.readBytesBE(1);
            
            //maybe nop
            if(first[0] == (byte) 0xb0) {
                //System.out.println("b0 nop");
                script.getEntries().add(new ScriptNopEntry());
                continue;
            }
            
            //section start
            if(first[0] == (byte) 0xb1) {
                int no = dqis.readIntLE();
                //System.out.println("b1 " + no);
                script.getEntries().add(new ScriptSectionEntry(1, no));
                inSection = true;
                continue;
            }
            
            //section end
            if(first[0] == (byte) 0xb2) {
                int no = dqis.readIntLE();
                //System.out.println("b2 " + no);
                script.getEntries().add(new ScriptSectionEntry(2, no));
                inSection = false;
                continue;
            }
            
            //TODO maybe there is 0xb3 <2 bytes> <4 bytes> and 0xb4 <2 bytes> as well
            //for now this is just a binary command
            
            //command
            byte[] cmd12 = dqis.readBytesBE(2);
            byte[] cmd = new byte[] { first[0], cmd12[0], cmd12[1] };
            //System.out.println("cmd " + Inspector.toHex(cmd));
            
            //after a b1 to b2 section comes a 3 byte separator: 0x434343
            if(cmd[0] == (byte) 0x43 && cmd[1] == (byte) 0x43 && cmd[2] == (byte) 0x43) {
                //System.out.println("\tseparator");
                script.getEntries().add(new ScriptSeparatorEntry(cmd));
                
                //do not add it as command
                continue;
            }

            //separator
            if(cmd[0] == (byte) 0x58 && cmd[1] == (byte) 0x58 /*&& cmd[2] == (byte) 0x58*/) {
                //System.out.println("\tseparator");
                script.getEntries().add(new ScriptSeparatorEntry(cmd));

                if(dqis.getPosition() == endOfScript) {
                    //System.out.println("at end of script");
                    endOfScriptReached = true;
                    break;
                }
                
                //do not add it as command
                continue;
            }
            
            
            //variable parameter count
            int paramLen = 0;
            String cmdHex = Inspector.toHex(cmd);
            switch(cmdHex) {
                case "c021a0": paramLen = 4; break;
                case "43b100": paramLen = 4; break;
                case "f1063a": paramLen = 2; break;
                case "5f3030": paramLen = 2; break;
            }

            byte[] params = dqis.readBytesBE(paramLen);
            //System.out.println("\tparam " + Inspector.toHex(params));
            
            if(cmdHex.equals("c021a0")) {
                script.getEntries().add(new ScriptStoreEntry(params));
                
            } else {
                //default
                script.getEntries().add(new ScriptBinaryEntry(cmd, params));
            }
        }
        
        //System.out.println("dqis.getPosition(): " + dqis.getPosition());
        //System.out.println("endOfScript: " + endOfScript);
        
        //script.getEntries().forEach(e -> System.out.println(e));
        
        //the "LLLL" sections 4 * 0x4c
        byte[] unknown = dqis.readRemaining();
        script.setOriginalUnknown(unknown);
        //System.out.println(Inspector.toHexDump(unknown, 8, true, true));
        
        if(!endOfScriptReached) {
            throw new IOException("script was not correctly parsed because end of script was not correctly reached");
        }
        
        return script;
    }

}
