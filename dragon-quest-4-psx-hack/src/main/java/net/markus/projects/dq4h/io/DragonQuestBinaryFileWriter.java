
package net.markus.projects.dq4h.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import org.apache.commons.io.FileUtils;

/**
 * Writes {@link DragonQuestBinary}.
 */
public class DragonQuestBinaryFileWriter  {

    /**
     * 2048 bytes.
     */
    public static final int SECTOR_USER_SIZE = 2048;
    
    private HeartBeatDataWriter hbdWriter;
    
    public DragonQuestBinaryFileWriter(IOConfig config) {
        hbdWriter = new HeartBeatDataWriter(config);
    }
    
    public void patch(DragonQuestBinary bin, File input, File output) throws IOException {
        
        //copy input to output
        FileUtils.copyFile(input, output);
        
        //use RandomAccessFile to patch output
        RandomAccessFile out = new RandomAccessFile(output, "rw");
        
        //HBD1PS1D.Q41 ======================
        DiskFileInfo hbdDiskFile = bin.getDiskFile("HBD1PS1D.Q41");
        
        //a new hbd byte array is formed from the java object
        ByteArrayOutputStream hbdOut = new ByteArrayOutputStream();
        hbdWriter.write(bin.getHeartBeatData(), hbdOut);
        byte[] hbdContent = hbdOut.toByteArray();
        
        //because hbd content size could be changed, we should check if new size is not larger than original size
        //maybe its better to have the exact size so that every sector starts at the same position
        if(hbdDiskFile.getSize() != hbdContent.length)
            throw new IOException("HeartBeatData size not equal to original size");
        
        writeToSectors(
                hbdContent, 
                hbdDiskFile.getStartSector(),
                out
        );
        
        
        //SLPM_869.16 ======================
        DiskFileInfo exeDiskFile = bin.getDiskFile("SLPM_869.16");
        
        if(exeDiskFile.getSize() != bin.getExecutable().getData().length)
            throw new IOException("executable data size not equal");
        
        //write in cd-rom sectors
        writeToSectors(
                bin.getExecutable().getData(), 
                exeDiskFile.getStartSector(),
                out
        );
        
        //SYSTEM.CNF
        //will not be changed, so commented here
        /*
        SectorInfo cnfSector = bin.getSector("SYSTEM.CNF");
        if(cnfSector.getSize() != bin.getSystemConfig().getData().length)
            throw new IOException("system conf data size not equal");
        out.seek(sectorSize * cnfSector.getStartSector() + 24);
        out.write(bin.getSystemConfig().getData());
        */
        
        out.close();
        
        //CUE file
        File cueFile = new File(output.getParentFile(), output.getName().replace(".bin", ".cue"));
        StringBuilder cue = new StringBuilder();
        cue.append("FILE \"").append(output.getName()).append("\" BINARY\n");
        cue.append("  TRACK 01 MODE2/2352\n");
        cue.append("    INDEX 01 00:00:00");
        FileUtils.writeStringToFile(cueFile, cue.toString(), StandardCharsets.UTF_8);
    }
    
    private void writeToSectors(byte[] data, int startSector, RandomAccessFile out) throws IOException {
        int dataOffset = 0;
        int outOffset = 0;
        while(dataOffset < data.length) {
        
            //12 bytes sync
            // 4 bytes header
            // 8 bytes sub-header
            //--
            //24 bytes header
            
            //seek to user data
            out.seek(DragonQuestBinaryFileReader.SECTORSIZE * startSector + outOffset + 24);
        
            //we write only 2048 bytes
            out.write(data, dataOffset, SECTOR_USER_SIZE);
            
            //maybe we have to update EDC, ECC?
            //818h 4      4 EDC (checksum accross [010h..817h])
            //81Ch 114h 276 ECC (error correction codes)
            //-------------------
            //          280
            
            //jump to next user sector in data (2048 bytes)
            dataOffset += SECTOR_USER_SIZE;
            
            //jump to next cd-rom sector (2352 bytes)
            outOffset += DragonQuestBinaryFileReader.SECTORSIZE;
        }
    }

}
