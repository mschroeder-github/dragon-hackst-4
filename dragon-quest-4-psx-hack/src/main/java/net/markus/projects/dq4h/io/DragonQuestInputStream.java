
package net.markus.projects.dq4h.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A special wrapper around an input stream to read data from dragon quest files.
 * Uses {@link Converter} to convert to the right format.
 */
public class DragonQuestInputStream {

    private InputStream inputStream;
    
    public DragonQuestInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    public void close() throws IOException {
        inputStream.close();
    }
    
    /**
     * Reads an int (4 bytes) in little endian.
     * @return
     * @throws IOException 
     */
    public int readIntLE() throws IOException {
        byte[] b = new byte[4];
        read(b);
        return Converter.bytesToIntLE(b);
    }
    
    /**
     * Reads a short (2 bytes) in little endian.
     * @return
     * @throws IOException 
     */
    public short readShortLE() throws IOException {
        byte[] b = new byte[2];
        read(b);
        return Converter.bytesToShortLE(b);
    }
    
    /**
     * Reads <i>n</i> bytes from the stream and returns it.
     * @param n number of bytes, greater equal 0
     * @return
     * @throws IOException 
     */
    public byte[] readBytes(int n) throws IOException {
        byte[] b = new byte[n];
        read(b);
        return b;
    }
    
    /**
     * Reads the remaining bytes from the stream.
     * @return
     * @throws IOException 
     */
    public byte[] readRemaining() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int bufferSize = 1024;
        
        int len = 0;
        do {
            byte[] buffer = new byte[bufferSize];
            len = read(buffer);
            
            if(len == -1)
                break;
            
            baos.write(buffer, 0, len);
            
        } while(len != -1);
        
        baos.close();
        return baos.toByteArray();
    }
    
}
