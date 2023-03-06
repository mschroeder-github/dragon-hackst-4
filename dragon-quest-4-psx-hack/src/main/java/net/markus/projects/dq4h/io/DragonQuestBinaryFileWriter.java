
package net.markus.projects.dq4h.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
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
    private IOConfig config;
    
    public DragonQuestBinaryFileWriter(IOConfig config) {
        this.config = config;
        hbdWriter = new HeartBeatDataWriter(config);
    }
    
    /**
     * Copies input and patches it using the {@link DragonQuestBinary} information.
     * @param bin
     * @param input
     * @param output
     * @throws IOException
     * @deprecated use {@link #write(net.markus.projects.dq4h.data.DragonQuestBinary, java.io.File) } because it does not patch input, it writes everything from bin file
     */
    @Deprecated
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
        writeCue(output);
    }
    
    private void writeCue(File output) throws IOException {
        //CUE file
        File cueFile = new File(output.getParentFile(), output.getName().replace(".bin", ".cue"));
        StringBuilder cue = new StringBuilder();
        cue.append("FILE \"").append(output.getName()).append("\" BINARY\n");
        cue.append("  TRACK 01 MODE2/2352\n");
        cue.append("    INDEX 01 00:00:00");
        FileUtils.writeStringToFile(cueFile, cue.toString(), StandardCharsets.UTF_8);
    }
    
    /**
     * Instead of {@link #patch(net.markus.projects.dq4h.data.DragonQuestBinary, java.io.File, java.io.File) } we write
     * the binary to output without copy the input file.
     * @param bin
     * @param output 
     */
    public void write(DragonQuestBinary bin, File output) throws IOException {
        RandomAccessFile out = new RandomAccessFile(output, "rw");
        
        //they are 22 * 2352 bytes, so we do not use writeToSectors here
        out.write(bin.getFirstSectors());
        
        //SYSTEM.CNF ======================= DiskFileInfo{name=SYSTEM.CNF, startSector=23, size=68
        
        DiskFileInfo cnfDiskFile = bin.getDiskFile("SYSTEM.CNF");
        //it has 86 bytes but we read the full 2048 bytes
        //if(cnfDiskFile.getSize() != bin.getSystemConfig().getData().length)
        //    throw new IOException("system conf data size not equal");
        
        writeToSectors(
                bin.getSystemConfig().getData(), 
                cnfDiskFile.getStartSector(),
                out
        );
        
        
        
        //HBD1PS1D.Q41 ====================== DiskFileInfo{name=HBD1PS1D.Q41, startSector=362, size=319436800
        DiskFileInfo hbdDiskFile = bin.getDiskFile("HBD1PS1D.Q41");
        
        //a new hbd byte array is formed from the java object
        ByteArrayOutputStream hbdOut = new ByteArrayOutputStream();
        hbdWriter.write(bin.getHeartBeatData(), bin, hbdOut);
        byte[] hbdContent = hbdOut.toByteArray();
        
        writeToSectors(
                hbdContent, 
                hbdDiskFile.getStartSector(),
                out
        );
        
        
        //SLPM_869.16 ====================== DiskFileInfo{name=SLPM_869.16, startSector=24, size=692224
        //we write the PSX EXE at the end because it could be changed in the writing (hbdWriter)
        DiskFileInfo exeDiskFile = bin.getDiskFile("SLPM_869.16");
        
        if(exeDiskFile.getSize() != bin.getExecutable().getData().length)
            throw new IOException("executable data size not equal");
        
        //a virtual file for psx exe to use change log entry
        HeartBeatDataFolderEntry folder = new HeartBeatDataFolderEntry();
        HeartBeatDataFile psxFile = new HeartBeatDataFile();
        psxFile.setParent(folder);
        config.getChangeLogEntries().add(new ChangeLogEntry(psxFile, bin.getExecutable().getOriginalData(), bin.getExecutable().getData()));
        
        //write in cd-rom sectors
        writeToSectors(
                bin.getExecutable().getData(), 
                exeDiskFile.getStartSector(),
                out
        );
        
        out.close();
        
        writeCue(output);
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
