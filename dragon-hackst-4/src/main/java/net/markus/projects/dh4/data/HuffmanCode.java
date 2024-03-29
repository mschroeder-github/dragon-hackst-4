
package net.markus.projects.dh4.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.markus.projects.dh4.PsxJisReader;

/**
 * 
 */
public class HuffmanCode extends ArrayList<HuffmanChar> {

    private ParseNode huffmanTreeRoot;
    
    private String text;
    
    //if this is a segment it tells us the first byte position difference to the whole textblock
    //this is used to know the pointer position when dialog is loaded
    @Deprecated
    private String byteDiffInHex;
    
    //if this is a segment it tells us the first byte position difference to the whole textblock
    //it is the updated version after we embedded the translated text
    @Deprecated
    private String byteDiffInHexUpdated;
    
    private String bitInHex;
    
    //the original command to reach this segment
    private byte[] command;
    
    //the positions of the original command
    private List<Integer> commandPositions = new ArrayList<>();
    
    //the new command
    private byte[] newCommand;

    public ParseNode getHuffmanTreeRoot() {
        return huffmanTreeRoot;
    }

    public void setHuffmanTreeRoot(ParseNode huffmanTreeRoot) {
        this.huffmanTreeRoot = huffmanTreeRoot;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public String calculateText() {
        StringBuilder sb = new StringBuilder();
        
        for(HuffmanChar c : this) {
            sb.append(c.toReadable());
        }
        
        return sb.toString();
    }

    @Deprecated
    public String getByteDiffInHex() {
        return byteDiffInHex;
    }

    @Deprecated
    public void setByteDiffInHex(String byteDiffInHex) {
        this.byteDiffInHex = byteDiffInHex;
    }

    @Deprecated
    public String getByteDiffInHexUpdated() {
        return byteDiffInHexUpdated;
    }

    @Deprecated
    public void setByteDiffInHexUpdated(String byteDiffInHexUpdated) {
        this.byteDiffInHexUpdated = byteDiffInHexUpdated;
    }

    public String getBitInHex() {
        return bitInHex;
    }

    public void setBitInHex(String bitInHex) {
        this.bitInHex = bitInHex;
    }

    public byte[] getCommand() {
        return command;
    }

    public void setCommand(byte[] command) {
        this.command = command;
    }

    public List<Integer> getCommandPositions() {
        return commandPositions;
    }

    public void setCommandPositions(List<Integer> commandPositions) {
        this.commandPositions = commandPositions;
    }

    

    public byte[] getNewCommand() {
        return newCommand;
    }

    public void setNewCommand(byte[] newCommand) {
        this.newCommand = newCommand;
    }
    
    //text is utf-8 (ascii) chars
    //will be in japanese encoding
    public static HuffmanCode parse(String text) {
        text = text.trim();
        
        HuffmanCode code = new HuffmanCode();
        
        boolean inControl = false;
        String controlBuffer = "";
        for(int i = 0; i < text.length(); i++) {
            String charStr = "" + text.charAt(i);
            
            if(charStr.equals("{")) {
                inControl = true;
                
            } else if(charStr.equals("}")) {
                inControl = false;
                //will be parsed correctly
                code.add(new HuffmanChar(controlBuffer));
                controlBuffer = "";
                
            } else if(inControl) {
                controlBuffer += charStr;
                
            } else {
                
                String jpChar = PsxJisReader.ascii2jp.get(charStr);
                if(jpChar == null) {
                    throw new RuntimeException("no japanese char found for " + charStr);
                }
                
                code.add(new HuffmanChar(jpChar));
            }
        }
        
        //always end with {0000}
        if(!code.isEmpty()) {
            HuffmanChar last = code.get(code.size()-1);
            if(!last.getLetter().equals("0000")) {
                code.add(new HuffmanChar("0000"));
            }
        }
        
        return code;
    }
    
    public static HuffmanCode merge(List<HuffmanCode> list) {
        //list.sort((a,b) -> a.getByteDiffInHex().compareTo(b.getByteDiffInHex()));
        HuffmanCode code = new HuffmanCode();
        list.forEach(hc -> code.addAll(hc));
        return code;
    }

    public Map<String, Integer> getCharFrequencyMap() {
        Map<String, Integer> map = new HashMap<>();
        for(HuffmanChar c : this) {
            int count = map.computeIfAbsent(c.getLetter(), l -> 0);
            map.put(c.getLetter(), count + 1);
        }
        return map;
    }
    
    public static void calculateCharacterIndices(List<HuffmanCode> segments) {
        
        int bitIndex = 0;
        
        for(HuffmanCode segment : segments) {
            for(HuffmanChar ch : segment) {
                
                ch.setStartBit(bitIndex);
                
                //wo do net need this anymore
                ch.setStartBitInByte(bitIndex % 8);
                ch.setByteIndex(bitIndex / 8);
                
                bitIndex += ch.getBits().length();
                
                //also we do not need this
                ch.setEndBit(bitIndex);
            }
        }
        
    }
    
    public String getBits() {
        StringBuilder sb = new StringBuilder();
        for(HuffmanChar ch : this) {
            sb.append(ch.getBits());
        }
        return sb.toString();
    }
}
