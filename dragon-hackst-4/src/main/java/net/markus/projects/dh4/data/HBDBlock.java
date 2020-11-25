
package net.markus.projects.dh4.data;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 */
public abstract class HBDBlock {

    public int pos;
    public int blockIndex;
    
    public byte[] header;
    public byte[] data;
    public byte[] full2048;
    
    public byte[] writeThisBlock;
    
    public String headerHexString;
    
    public String dataHash;
    
    public String hexPos() {
        return "0x" + Integer.toHexString(pos);
    }
    
    public void calculateDataHash() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        
        digest.update(data);
        
        byte[] encodedhash = digest.digest();
        
        dataHash = bytesToHex(encodedhash);
    }
    
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    //writes the full2048 with the current data in java RAM
    public abstract void write() throws IOException;
    
}
