
package net.markus.projects.dq4h.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An abstract write for blocks in the dragon quest psx data.
 * @param <T> type of object
 */
public abstract class DragonQuestWriter<T> {

    /**
     * Writes the object to the output stream.
     * @param obj the object to be written
     * @param output where the serialization is written
     * @return if the data is written in compressed format, this method returns
     * the uncompressed size, otherwise -1
     * @throws IOException 
     */
    public abstract int write(T obj, OutputStream output) throws IOException;
    
    public int write(T obj, File outputFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFile);
        int uncomp = write(obj, fos);
        fos.close();
        return uncomp;
    }
    
    public byte[] write(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(obj, baos);
        baos.close();
        return baos.toByteArray();
    }
    
    /**
     * Use this method to get a {@link FileContent} object which contains
     * an uncompressed size if the file content is actually compressed.
     * @param obj
     * @return
     * @throws IOException 
     */
    public FileContent writeFileContent(T obj) throws IOException {
        FileContent content = new FileContent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int uncompressedSize = write(obj, baos);
        
        baos.close();
        content.setBytes(baos.toByteArray());
        
        if(uncompressedSize >= 0) {
            //it is compressed, so we store the uncompressed size
            content.setSizeUncompressed(uncompressedSize);
        } else {
            //it is not compressed, so the size is the same
            content.setSizeUncompressed(content.getBytes().length);
        }
        
        return content;
    }
    
}
