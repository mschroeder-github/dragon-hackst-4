
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.InputStream;
import net.markus.projects.dq4h.data.HeartBeatDataScriptContent;
import net.markus.projects.dq4h.data.ScriptBinaryEntry;
import net.markus.projects.dq4h.data.ScriptEntry;
import net.markus.projects.dq4h.data.ScriptNopEntry;
import net.markus.projects.dq4h.data.ScriptSectionEntry;
import net.markus.projects.dq4h.data.ScriptSeparatorEntry;
import net.markus.projects.dq4h.data.ScriptSpecialStoreEntry;
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
                case "c021a3": paramLen = 4; break;
                case "c061a0": paramLen = 2; break; //because: ScriptBinaryEntry{command=c061a0, parameters=7802 | f421a0}
                case "c221a1": paramLen = 7; break;
                case "c31678": paramLen = 2; break;
                case "43b100": paramLen = 4; break;
                case "a09a01": paramLen = 4; break;
                case "a0e103": paramLen = 4; break;
                case "a06c01": paramLen = 4; break;
                case "a0ae01": paramLen = 4; break;
                case "a078fd": paramLen = 2; break;
                case "a07901": paramLen = 2; break;
                case "a078fe": paramLen = 2; break;
                case "f1063a": paramLen = 2; break;
                case "f60500": paramLen = 2; break;
                case "e0063d": paramLen = 2; break; //only two because ScriptBinaryEntry{command=e0063d, parameters=1700c021a0fa0210}
                case "5f3030": paramLen = 2; break;
                case "f421a0": paramLen = 6; break; //see e.g. text id 00ec
                case "c01678": paramLen = 2; break; //see text id 0067
                case "202564": paramLen = 2; break; //see text id 0124
                
                //special store
                case "c0267b": paramLen = 7; break;
                case "c02678": paramLen = 5; break;
                case "c0263b": paramLen = 6; break;
            }

            byte[] params = dqis.readBytesBE(paramLen);
            //System.out.println("\tparam " + Inspector.toHex(params));
            String paramsHex = Inspector.toHex(params);
            
            //special case for this command
            if(cmdHex.equals("c061a0")) {
                //in text id 0135
                //ScriptBinaryEntry{command=c061a0, parameters=3b04}
                //ScriptBinaryEntry{command=00b401}
                //ScriptBinaryEntry{command=a0c021}
                //when param starts with '3' not '7' then there is one more byte in params it seems
                if(!paramsHex.startsWith("7")) {//paramsHex.startsWith("3")) {
                    params = new byte[] { params[0], params[1], dqis.readBytesBE(1)[0] };
                }
            }
            
            
            if(cmdHex.equals("c021a0")) {
                script.getEntries().add(new ScriptStoreEntry(params));
                
            } else if(cmdHex.equals("c0267b") || cmdHex.equals("c02678") || cmdHex.equals("c0263b")) {
                ScriptSpecialStoreEntry special = new ScriptSpecialStoreEntry(cmd, params);
                script.getEntries().add(special);
                
            } else {
                //default
                script.getEntries().add(new ScriptBinaryEntry(cmd, params));
            }
            
        }
        
        //set parent
        for(ScriptEntry entry : script.getEntries()) {
            entry.setParent(script);
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
