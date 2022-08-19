
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A special wrapper around an output stream to write dragon quest related data formats.
 * Uses {@link Converter} to convert to the right format.
 */
public class DragonQuestOutputStream {

    private OutputStream outputStream;
    private int position;

    public DragonQuestOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void write(byte[] b) throws IOException {
        outputStream.write(b);
        position += b.length;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
        position += len;
    }

    public void flush() throws IOException {
        outputStream.flush();
    }

    public void close() throws IOException {
        outputStream.close();
    }
    
    /**
     * Writes an int (4 bytes) in little endian.
     * @param i 
     */
    public void writeIntLE(int i) throws IOException {
        write(Converter.intToBytesLE(i));
    }
    
    /** 
     * Writes a short (2 bytes) in little endian.
     * @param s 
     */
    public void writeShortLE(short s) throws IOException {
        write(Converter.shortToBytesLE(s));
    }
    
    /**
     * Write byte array as given.
     * @param bytes
     * @throws IOException 
     */
    public void writeBytesBE(byte[] bytes) throws IOException {
        write(bytes);
    }
    
    /**
     * Write byte array in reversed order.
     * @param bytes
     * @throws IOException 
     */
    public void writeBytesLE(byte[] bytes) throws IOException {
        write(Converter.reverse(bytes));
    }

    public int getPosition() {
        return position;
    }

}
