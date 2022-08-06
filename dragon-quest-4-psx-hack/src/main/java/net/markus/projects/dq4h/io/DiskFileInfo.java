
package net.markus.projects.dq4h.io;

/**
 * Information about files on CD for later patching in {@link DragonQuestBinaryFileWriter}.
 */
public class DiskFileInfo {
    
    private String name;
    private int startSector;
    private int size;

    public DiskFileInfo(String name, int startSector, int size) {
        this.name = name;
        this.startSector = startSector;
        this.size = size;
    }
    
    public String getName() {
        return name;
    }

    public int getStartSector() {
        return startSector;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "DiskFileInfo{" + "name=" + name + ", startSector=" + startSector + ", size=" + size + '}';
    }

}
