package net.markus.projects.dq4h.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import net.markus.projects.dq4h.data.HeartBeatDataFileContent;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataScriptContent;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import net.markus.projects.dq4h.data.HuffmanCharacter;
import net.markus.projects.dq4h.data.ScriptStoreEntry;
import net.markus.projects.dq4h.data.VariableToDialogPointer;
import org.crosswire.common.compress.LZSS;

/**
 * Reads {@link HeartBeatDataFolderEntry}. It is responsible for (de)compressing file contents if needed.
 */
public class HeartBeatDataFolderEntryReader extends DragonQuestReader<HeartBeatDataFolderEntry> {

    //readers for special file contents
    private HeartBeatDataTextContentReader textContentReader;
    private HeartBeatDataScriptContentReader scriptContentReader;

    private IOConfig config;

    public HeartBeatDataFolderEntryReader(IOConfig config) {
        this.config = config;

        textContentReader = new HeartBeatDataTextContentReader();
        scriptContentReader = new HeartBeatDataScriptContentReader();
    }

    @Override
    public HeartBeatDataFolderEntry read(InputStream input) throws IOException {
        HeartBeatDataFolderEntry folder = new HeartBeatDataFolderEntry();

        DragonQuestInputStream dqir = new DragonQuestInputStream(input);

        //folder header info
        folder.setOriginalNumberOfFiles(dqir.readIntLE());
        folder.setOriginalNumberOfSectors(dqir.readIntLE());
        folder.setOriginalSize(dqir.readIntLE());
        folder.setOriginalUnknown(dqir.readBytesBE(4));

        //for DQ4 it is all zero
        if (!Verifier.allZero(folder.getOriginalUnknown())) {
            throw new IOException("Unknown part (after size) in folder is not all zero: " + folder);
        }

        //file header infos
        for (int i = 0; i < folder.getOriginalNumberOfFiles(); i++) {

            HeartBeatDataFile file = new HeartBeatDataFile();

            //16 bytes header
            file.setOriginalSize(dqir.readIntLE());
            file.setOriginalSizeUncompressed(dqir.readIntLE());
            file.setOriginalUnknown(dqir.readBytesBE(4));
            file.setOriginalFlags(dqir.readShortLE());
            file.setOriginalType(dqir.readShortLE());

            file.setIndex(i);
            file.setParent(folder);
            folder.getFiles().add(file);
        }

        //file contents
        for (HeartBeatDataFile file : folder.getFiles()) {
            //the original bytes
            file.setOriginalContentBytes(dqir.readBytesBE(file.getOriginalSize()));

            byte[] fileContentBytes = file.getOriginalContentBytes();
            HeartBeatDataFileContent fileContent = null;

            //content is parsed if we have a reader for it
            short type = file.getOriginalType();
            if (config.getTextContentTypes().contains(type)) {

                //note: in DQ4 text contents are never compressed
                //read data
                InputStream contentInput = new ByteArrayInputStream(fileContentBytes);
                fileContent = textContentReader.read(contentInput);
                contentInput.close();

            } else if (config.getScriptContentTypes().contains(type)) {

                //uncompress if compressed
                if (file.isCompressed()) {
                    ByteArrayInputStream compressedInput = new ByteArrayInputStream(fileContentBytes);
                    fileContentBytes = LZSS.uncompress(compressedInput, file.getOriginalSizeUncompressed());
                }

                //use script reader to read fileContentBytes
                InputStream contentInput = new ByteArrayInputStream(fileContentBytes);
                fileContent = scriptContentReader.read(contentInput);
                contentInput.close();
            }

            //connect content to file
            if (fileContent != null) {
                fileContent.setParent(file);
                file.setContent(fileContent);
            }
        }

        //remaining bytes
        byte[] remaining = dqir.readRemaining();

        //the remaining bytes have to be zero
        if (!Verifier.allZero(remaining)) {
            throw new IOException("Remaining bytes are not all zero in folder: " + folder);
        }

        folder.setOriginalNumberOfRemainingBytes(remaining.length);

        pointerAssociation(folder);

        return folder;
    }

    /**
     * After folder is read we look for pointer associations in the text and script contents.
     */
    private void pointerAssociation(HeartBeatDataFolderEntry folder) throws IOException {

        List<HeartBeatDataTextContent> texts = folder.getContents(HeartBeatDataTextContent.class);
        List<HeartBeatDataScriptContent> scripts = folder.getContents(HeartBeatDataScriptContent.class);

        //note: some folders have 4 text files
        //if (texts.size() > 4) {
        //    throw new IOException(texts.size() + " texts in " + folder);
        //}
        //note: always only zero or one script
        //if (scripts.size() > 1) {
        //    throw new IOException(scripts.size() + " scripts in " + folder);
        //}

        onlyDialogPointers(texts);

        pointersInScripts(texts, scripts);
    }

    private void onlyDialogPointers(List<HeartBeatDataTextContent> texts) throws IOException {
        
        List<VariableToDialogPointer> notMatched = new ArrayList<>();
        
        //some texts we ignore for now
        Set<String> ignoreIds = new HashSet<>();
        //this text file looks stange and seems not to have correct sentences
        ignoreIds.add("043f");
        
        //for each text check if there are store entries that refer to the text
        for (HeartBeatDataTextContent text : texts) {
            
            if(ignoreIds.contains(text.getIdHex())) {
                continue;
            }
            
            //check only if dialog pointers exist
            if(text.getOriginalDialogPointers().isEmpty()) {
                continue;
            }

            //the original offsets in the text header (24 bytes), 24 bytes * 8 bit = 192 bits
            //there is a 192 bit offset diff
            int textBitOffset = text.getTextBitOffset();
            //System.out.println("textBitOffset: " + textBitOffset);
            //System.out.println("text id: " + Inspector.toHex(text.getId()));

            //note: this does not happen for dialog pointers
            //if (textBitOffset != 192) {
            //    int specialCase = 1;
            //    //System.out.println("textBitOffset special: " + textBitOffset);
            //}

            //reference target: all possible characters (starts)
            Map<Integer, HuffmanCharacter> bitPosition2character = new LinkedHashMap<>();
            for (HuffmanCharacter startChar : text.getOriginalText()) { //getStartCharacters
                bitPosition2character.put(textBitOffset + startChar.getOriginalBitPosition(), startChar);
            }

            //------------------
            
            //reference source: dialog pointers
            Map<Integer, VariableToDialogPointer> bitPosition2vpd = new HashMap<>();
            for (VariableToDialogPointer vpd : text.getOriginalDialogPointers()) {
                bitPosition2vpd.put(vpd.getBitOffsetAsInt(), vpd);
            }

            for (Entry<Integer, VariableToDialogPointer> e : bitPosition2vpd.entrySet()) {
                HuffmanCharacter c = bitPosition2character.get(e.getKey());

                if (c != null) {
                    //connect both ways
                    c.addRereferrer(e.getValue());
                    e.getValue().setReference(c);
                } else {
                    
                    notMatched.add(e.getValue());
                }

            }
            
            //if one is not matched we throw an exception
            //note: it seems that we match all of them correctly
            if(!notMatched.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("text id: ").append(Inspector.toHex(text.getId())).append("\n");
                bitPosition2character.entrySet().forEach(e -> sb.append(e).append("\n"));
                notMatched.forEach(vdp -> sb.append(vdp).append("\n"));
                throw new IOException(sb.toString());
            }
        }
        
        
    }
    
    private void pointersInScripts(List<HeartBeatDataTextContent> texts, List<HeartBeatDataScriptContent> scripts) throws IOException {
        //only continue when we have both
        //if (texts.isEmpty() || scripts.isEmpty()) {
        //    return;
        //}
        
        List<ScriptStoreEntry> notMatched = new ArrayList<>();

        //the one script, note: can only be one
        for(HeartBeatDataScriptContent script : scripts) {

            //for each text check if there are store entries that refer to the text
            for (HeartBeatDataTextContent text : texts) {
                
                List<ScriptStoreEntry> storeCommands = script.getStoreEntries(text.getId2Bytes());

                //if no command refers to the text we can skip it
                if(storeCommands.isEmpty()) {
                    continue;
                }

                //the original offsets in the text header (24 bytes), 24 bytes * 8 bit = 192 bits
                //there is a 192 bit offset diff
                int textBitOffset = text.getTextBitOffset();

                //reference target: all possible characters (starts)
                Map<Integer, HuffmanCharacter> bitPosition2character = new LinkedHashMap<>();
                for (HuffmanCharacter c : text.getOriginalText()) { //getStartCharacters
                    bitPosition2character.put(textBitOffset + c.getOriginalBitPosition(), c);
                }

                //------------------
                //reference source: script command, we use a list here because there can be two commands pointing to the same bit offset
                Map<Integer, List<ScriptStoreEntry>> bitPosition2stores = new HashMap<>();
                for (ScriptStoreEntry store : storeCommands) {
                    
                    List<ScriptStoreEntry> l = bitPosition2stores.get(store.getBitOffsetAsInt());
                    if(l == null) {
                        bitPosition2stores.put(store.getBitOffsetAsInt(), new ArrayList<>(Arrays.asList(store)));
                    } else {
                        l.add(store);
                    }
                }

                //based on bit position we match
                for (Entry<Integer, List<ScriptStoreEntry>> e : bitPosition2stores.entrySet()) {
                    
                    for(ScriptStoreEntry store : e.getValue()) {
                        HuffmanCharacter c = bitPosition2character.get(e.getKey());

                        if (c != null) {
                            //connect both ways
                            c.addRereferrer(store);
                            store.setReference(c);
                        } else {
                            notMatched.add(store);
                        }
                    }

                }

                if(!notMatched.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("text id: ").append(Inspector.toHex(text.getId())).append("\n");
                    sb.append("textBitOffset: ").append(textBitOffset).append("\n");
                    bitPosition2character.entrySet().forEach(e -> sb.append(e).append("\n"));
                    notMatched.forEach(sp -> sb.append(sp).append("\n"));
                    throw new IOException(sb.toString());
                }

            }//for each text
            
        }//for each script
    }
}
