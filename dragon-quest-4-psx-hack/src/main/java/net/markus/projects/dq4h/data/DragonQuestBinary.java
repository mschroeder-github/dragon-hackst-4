
package net.markus.projects.dq4h.data;

import java.util.HashMap;
import java.util.Map;
import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.compare.DragonQuestComparator;
import net.markus.projects.dq4h.io.DiskFileInfo;
import net.markus.projects.dq4h.io.Verifier;

/**
 * The CD image (*.bin) file of dragon quest psx version.
 */
public class DragonQuestBinary implements DragonQuestComparator<DragonQuestBinary> {

    //first 22 sectors (2352 * 22)
    private byte[] firstSectors;
    private SystemConfig systemConfig;
    private PsxExe executable;
    private HeartBeatData heartBeatData;

    private Map<String, DiskFileInfo> diskFiles;
    
    public DragonQuestBinary() {
        diskFiles = new HashMap<>();
    }

    public byte[] getFirstSectors() {
        return firstSectors;
    }

    public void setFirstSectors(byte[] firstSectors) {
        this.firstSectors = firstSectors;
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

    @Override
    public void compare(DragonQuestBinary other, ComparatorReport report) {
        Verifier.compareBytes(this, this.firstSectors, other, other.firstSectors, "first sectors", report);
        Verifier.compareBytes(this.executable, this.executable.getData(), other.executable, other.executable.getData(), "executable", report);
        Verifier.compareBytes(this.systemConfig, this.systemConfig.getData(), other.systemConfig, other.systemConfig.getData(), "systemConfig", report);
        this.heartBeatData.compare(other.heartBeatData, report);
    }
    
}
