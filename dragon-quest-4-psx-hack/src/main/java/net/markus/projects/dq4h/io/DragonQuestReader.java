
package net.markus.projects.dq4h.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An abstract reader for blocks in the dragon quest psx data.
 */
public abstract class DragonQuestReader<T> {

    public abstract T read(InputStream input) throws IOException;
    
    public T read(File inputFile) throws IOException {
        FileInputStream fis = new FileInputStream(inputFile);
        T obj = read(fis);
        fis.close();
        return obj;
    }
    
}
