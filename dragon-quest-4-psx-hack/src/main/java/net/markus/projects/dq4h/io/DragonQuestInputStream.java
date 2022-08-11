
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
    private int position;
    
    
    public DragonQuestInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.position = 0;
    }

    public int read(byte[] b) throws IOException {
        int read = inputStream.read(b);
        if(read != -1) {
            position += read;
        }
        return read;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int read = inputStream.read(b, off, len);
        if(read != -1) {
            position += read;
        }
        return read;
    }

    public long skip(long n) throws IOException {
        long skipped = inputStream.skip(n);
        if(skipped != -1) {
            position += skipped;
        }
        return skipped;
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
     * Reads <i>n</i> bytes from the stream and returns it as big endian (the normal way the bytes are read).
     * @param n number of bytes, greater equal 0
     * @return
     * @throws IOException 
     */
    public byte[] readBytesBE(int n) throws IOException {
        if(n <= 0) {
            return new byte[0];
        }
        
        byte[] b = new byte[n];
        read(b);
        return b;
    }
    
    /**
     * Reads <i>n</i> bytes from the stream and returns it as little endian.
     * It is the same as {@link #readBytesBE(int) } but reversed.
     * This is the usual way to read bytes in Dragon Quest 4.
     * @param n number of bytes, greater equal 0
     * @return
     * @throws IOException 
     */
    public byte[] readBytesLE(int n) throws IOException {
        return Converter.reverse(readBytesBE(n));
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

    public int getPosition() {
        return position;
    }
    
}
