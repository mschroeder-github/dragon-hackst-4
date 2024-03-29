
package net.markus.projects.dq4h.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatData;
import net.markus.projects.dq4h.data.HeartBeatDataEntry;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.PsxExe;
import net.markus.projects.dq4h.data.SystemConfig;
import net.markus.projects.dq4h.util.MemoryUtility;

/**
 * Reads {@link DragonQuestBinary}.
 * @see <a href="https://github.com/zevektor/DiskImageReader">DiskImageReader</a>
 */
public class DragonQuestBinaryFileReader {

    //https://kompendium.infotip.de/compactdisc-cd.html
    //Mode 2 XA Form 1
    
    /*
    Mode2/Form1 (CD-XA)
    000h 0Ch  Sync
    00Ch 4    Header (Minute,Second,Sector,Mode=02h)
    010h 4    Sub-Header (File, Channel, Submode AND DFh, Codinginfo)
    014h 4    Copy of Sub-Header
    018h 800h Data (2048 bytes)
    818h 4    EDC (checksum accross [010h..817h])
    81Ch 114h ECC (error correction codes)
    */
    
    public static final int SECTORSIZE = 2352;

    private HeartBeatDataReader heartBeatDataReader;
    
    private IOConfig config;
    
    public DragonQuestBinaryFileReader(IOConfig config) {
        this.config = config;
        heartBeatDataReader = new HeartBeatDataReader(config);
    }
    
    public DragonQuestBinary read(File inputFile) throws IOException {
        
        RandomAccessFile file = new RandomAccessFile(inputFile, "r");
        
        DragonQuestBinary binary = new DragonQuestBinary();
        
        config.trace("seek 16 cd sectors");
        
        //header stuff
        file.seek(SECTORSIZE * 16);
        
        //search for header and primary volume descriptor
        config.trace("search for header and primary volume descriptor");
        byte[] sector = new byte[SECTORSIZE];
        while (file.read(sector) > 0) {
            if ((isHeader(Arrays.copyOfRange(sector, 24, 30)))
                    && (isPrimaryVolumeDescriptor(sector[24]))) {
                break;
            }
        }
        
        //root data
        config.trace("root data");
        byte[] rootData = Arrays.copyOfRange(sector, 24, 2072);
        int startSector = readLittleEndianWord(Arrays.copyOfRange(rootData, 158, 162));
		int size = readLittleEndianWord(Arrays.copyOfRange(rootData, 166,170));
        config.trace("start Sector: " + startSector);
        
        
        //files in root
        config.trace("files in root");
        byte[] data = new byte[size];
        file.seek(SECTORSIZE * startSector + 24);
        file.read(data);
        int count = 0;
        for (int index = 0; index < data.length; index++) {
            
            int offset = data[index];
            if (offset == 0)
                break;
            
            if (count > 1) {
                config.trace("parse file at " + index);
                parseFile(file, Arrays.copyOfRange(data, index, index + offset), binary);
                index += offset - 1;
            } else {
                count++;
                index += offset - 1;
            }
        }
        
        //first 22 sectors of CD
        data = new byte[SECTORSIZE * 22];
        file.seek(0);
        file.read(data);
        binary.setFirstSectors(data);
        
        file.close();
        
        return binary;
    }
    
    private void parseFile(RandomAccessFile file, byte[] header, DragonQuestBinary binary) throws IOException {
		StringBuilder sb = new StringBuilder();
		String flags = String.format("%8s", new Object[] { Integer.toBinaryString(UValue(header[25])) }).replace(' ', '0');
        
        
		boolean dir = false;
		if (flags.charAt(6) == '1')
			dir = true;
		
        int length = UValue(header[32]);
		for (int i = 33; i < 33 + length; i++) {
			sb.append((char) header[i]);
		}
		
        if ((sb.toString().length() == 1) && (sb.charAt(0) == 0))
			return;
		
        String name = dir ? sb.toString() : sb.toString().substring(0, sb.toString().length() - 2);
        
        config.trace("file name is " + name);

		int startSector = readLittleEndianWord(Arrays.copyOfRange(header, 2, 6));
		int size = readLittleEndianWord(Arrays.copyOfRange(header, 10, 14));
        
        binary.getDiskFiles().put(name, new DiskFileInfo(name, startSector, size));
        
        //byte[] data = new byte[size];
        file.seek(startSector * SECTORSIZE);
        
        config.trace("read file sectors of size " + size);
        
        config.trace(MemoryUtility.memoryStatistics());
        if(size < Runtime.getRuntime().totalMemory()) {
            config.trace("need more memory");
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        
        boolean oldCode = false;
        
        byte[] buffer2048 = new byte[2048];
        
        long begin = System.currentTimeMillis();
        
        config.trace("begin");
        while(baos.size() < size) {
            
            /*
            Mode2/Form1 (CD-XA)
            000h 0Ch  Sync
            00Ch 4    Header (Minute,Second,Sector,Mode=02h)
            010h 4    Sub-Header (File, Channel, Submode AND DFh, Codinginfo)
            014h 4    Copy of Sub-Header
            018h 800h Data (2048 bytes)
            818h 4    EDC (checksum accross [010h..817h])
            81Ch 114h ECC (error correction codes)
            */
            
            //12 bytes sync
            // 4 bytes header
            // 8 bytes sub-header
            //--
            //24 bytes header
            
            if(oldCode) {
                byte[] sector = new byte[SECTORSIZE];
                file.read(sector);
                byte[] userData = Arrays.copyOfRange(sector, 24, 24 + 2048);
                baos.write(userData);
            } else {
                //sectorsize = 2352
                
                //config.trace("skipBytes(24)");
                file.skipBytes(24);
                
                //read user data
                //config.trace("read user data");
                int len = file.read(buffer2048);
                baos.write(buffer2048, 0, len);
                
                //remaining = 2352 - 24 - 2048 = 280
                
                //config.trace("skipBytes(280)");
                file.skipBytes(280);
                
                //config.trace(baos.size() + "/" + size + " bytes read");
            }
        }        
        long end = System.currentTimeMillis();
        config.trace("took " + (end - begin) + " ms");
        
        byte[] data = baos.toByteArray();
        
        config.trace(data.length + " bytes read");
        switch(name) {
            case "HBD1PS1D.Q41": 
                HeartBeatData hbd = heartBeatDataReader.read(new ByteArrayInputStream(data), binary);
                binary.setHeartBeatData(hbd);
                break;
            case "SLPM_869.16": 
                PsxExe exe = new PsxExe();
                exe.setData(data);
                exe.setOriginalData(Arrays.copyOf(data, data.length));
                binary.setExecutable(exe);
                findPointerToFolders(binary.getHeartBeatData(), exe);
                break;
            case "SYSTEM.CNF": 
                SystemConfig conf = new SystemConfig();
                conf.setData(data);
                binary.setSystemConfig(conf);
                break;
                
            default:
                throw new IOException("The following sector is not expected: " + name);
        }
	}   
    
    private void findPointerToFolders(HeartBeatData hbd, PsxExe exe) {
        for (HeartBeatDataEntry entry : hbd.getEntries()) {
            if (entry instanceof HeartBeatDataFolderEntry) {
                
                
                byte[] pattern = Inspector.toBytes(entry.getSectorAddressCountStoredHex());
                List<Integer> positions = Inspector.find(pattern, exe.getData(), 603632);
                positions.removeIf(i -> i % 4 != 0);
                
                if(positions.isEmpty()) { 
                    //this happens two times
                    continue;
                }
                if(positions.size() > 1) {
                    throw new RuntimeException("ambiguous reference");
                }
                
                int position = positions.get(0);
                
                HeartBeatDataFolderEntry folder = (HeartBeatDataFolderEntry) entry;
                folder.setExe(exe);
                folder.setReferenceInExe(position);
            }
        }
    }
    
	private boolean isHeader(byte[] header) {
		return (header[1] == 67) && (header[2] == 68) && (header[3] == 48)
				&& (header[4] == 48) && (header[5] == 49);
	}

	private boolean isPrimaryVolumeDescriptor(byte b) {
		return 1 == UValue(b);
	}
    
	private short UValue(byte b) {
		return (short) (b & 0xFF);
	}
    
    private int readLittleEndianWord(byte[] bytes) {
		byte b1 = bytes[0];
		byte b2 = bytes[1];
		byte b3 = bytes[2];
		byte b4 = bytes[3];
		int s = 0;
		s |= b4 & 0xFF;
		s <<= 8;
		s |= b3 & 0xFF;
		s <<= 8;
		s |= b2 & 0xFF;
		s <<= 8;
		s |= b1 & 0xFF;
		return s;
	}
    
}
