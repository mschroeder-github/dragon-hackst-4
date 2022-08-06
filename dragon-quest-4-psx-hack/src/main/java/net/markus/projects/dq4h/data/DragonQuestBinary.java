
package net.markus.projects.dq4h.data;

import java.util.HashMap;
import java.util.Map;
import net.markus.projects.dq4h.io.DiskFileInfo;

/**
 * The CD image (*.bin) file of dragon quest psx versionU.
 */
public class DragonQuestBinary {

    private HeartBeatData heartBeatData;
    private PsxExe executable;
    private SystemConfig systemConfig;

    private Map<String, DiskFileInfo> diskFiles;
    
    public DragonQuestBinary() {
        diskFiles = new HashMap<>();
    }
    
    public HeartBeatData getHeartBeatData() {
        return heartBeatData;
    }

    public void setHeartBeatData(HeartBeatData heartBeatData) {
        this.heartBeatData = heartBeatData;
    }

    public PsxExe getExecutable() {
        return executable;
    }

    public void setExecutable(PsxExe executable) {
        this.executable = executable;
    }

    public SystemConfig getSystemConfig() {
        return systemConfig;
    }

    public void setSystemConfig(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    /**
     * The sectors to know where to patch them later on CD.
     * @return 
     */
    public Map<String, DiskFileInfo> getDiskFiles() {
        return diskFiles;
    }
    
    /**
     * Returns information about file on disk by name.
     * @param name name of the file (e.g. HBD1PS1D.Q41, SLPM_869.16 or SYSTEM.CNF)
     * @return 
     */
    public DiskFileInfo getDiskFile(String name) {
        return diskFiles.get(name);
    }

    @Override
    public String toString() {
        return "DragonQuestBinary{" + "sectors=" + diskFiles + '}';
    }
    
}
