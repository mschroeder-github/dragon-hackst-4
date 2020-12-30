
package net.markus.projects.dh4.data;

import net.markus.projects.dh4.util.Utils;

/**
 * 
 */
public class HuffmanChar {
    
    //position
    private int byteIndex;
    private int startBitInByte;
    private int startBit;
    private int endBit;
    
    //original bytes (not swaped)
    private byte[] data;
    
    //letter
    private String letter;
    
    private String bits;
    //two byte data representation

    public HuffmanChar(String letter) {
        this.letter = letter;
        
        data = new byte[0];
        if(letter.length() == 4) {
            data = Utils.hexStringToByteArray(letter);
        }
    }
    
    public HuffmanChar(byte[] data, String letter) {
        this.data = data;
        this.letter = letter;
    }

    public HuffmanChar(byte[] data) {
        this.data = data;
    }
    
    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public String getBits() {
        return bits;
    }

    public void setBits(String bits) {
        this.bits = bits;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
    
    public boolean isControlCharacter() {
        return letter.length() == 4;
    }

    public int getStartBit() {
        return startBit;
    }

    public void setStartBit(int startBit) {
        this.startBit = startBit;
    }

    public int getEndBit() {
        return endBit;
    }

    public void setEndBit(int endBit) {
        this.endBit = endBit;
    }

    public int getByteIndex() {
        return byteIndex;
    }

    public void setByteIndex(int byteIndex) {
        this.byteIndex = byteIndex;
    }

    public int getStartBitInByte() {
        return startBitInByte;
    }

    public void setStartBitInByte(int startBitInByte) {
        this.startBitInByte = startBitInByte;
    }
    
    public boolean isControlZero() {
        return letter.equals("0000");
    }
    
    @Override
    public String toString() {
        String str;
        if(isControlCharacter()) {
            str = "{" + Utils.bytesToHex(Utils.reverse(data)) + "}";
        } else {
            str = String.valueOf(letter);
        }
        
        str += " " + bits + " 0x" + Utils.bytesToHex(data) + " @" + startBit + "-" + endBit + "|" + byteIndex + " (" + startBitInByte + ")";
        
        return str;
    }
    
    public String toReadable() {
        if(isControlCharacter()) {
            return "{" + letter + "}";
        }
        return letter;
    }
    
    
    
}
