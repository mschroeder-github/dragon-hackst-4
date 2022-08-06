
package net.markus.projects.dq4h.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatData;
import net.markus.projects.dq4h.data.PsxExe;
import net.markus.projects.dq4h.data.SystemConfig;

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
    
    public DragonQuestBinaryFileReader() {
        heartBeatDataReader = new HeartBeatDataReader();
    }
    
    public DragonQuestBinary read(File inputFile) throws IOException {
        
        RandomAccessFile file = new RandomAccessFile(inputFile, "r");
        
        DragonQuestBinary binary = new DragonQuestBinary();
        
        //header stuff
        file.seek(SECTORSIZE * 16);
        
        //search for header and primary volume descriptor
        byte[] sector = new byte[SECTORSIZE];
        while (file.read(sector) > 0) {
            if ((isHeader(Arrays.copyOfRange(sector, 24, 30)))
                    && (isPrimaryVolumeDescriptor(sector[24]))) {
                break;
            }
        }
        
        //root data
        byte[] rootData = Arrays.copyOfRange(sector, 24, 2072);
        int startSector = readLittleEndianWord(Arrays.copyOfRange(rootData, 158, 162));
		int size = readLittleEndianWord(Arrays.copyOfRange(rootData, 166,170));
        
        
        //files in root
        byte[] data = new byte[size];
        file.seek(SECTORSIZE * startSector + 24);
        file.read(data);
        int count = 0;
        for (int index = 0; index < data.length; index++) {
            
            int offset = data[index];
            if (offset == 0)
                break;
            
            if (count > 1) {
                parseFile(file, Arrays.copyOfRange(data, index, index + offset), binary);
                index += offset - 1;
            } else {
                count++;
                index += offset - 1;
            }
        }
        
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

		int startSector = readLittleEndianWord(Arrays.copyOfRange(header, 2, 6));
		int size = readLittleEndianWord(Arrays.copyOfRange(header, 10, 14));
        
        binary.getDiskFiles().put(name, new DiskFileInfo(name, startSector, size));
        
        //byte[] data = new byte[size];
        file.seek(startSector * SECTORSIZE);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(baos.size() < size) {
            
            byte[] sector = new byte[SECTORSIZE];
            file.read(sector);
            
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
            
            byte[] userData = Arrays.copyOfRange(sector, 24, 24 + 2048);
            
            baos.write(userData);
        }        
        
        byte[] data = baos.toByteArray();
        
        switch(name) {
            case "HBD1PS1D.Q41": 
                HeartBeatData hbd = heartBeatDataReader.read(new ByteArrayInputStream(data));
                binary.setHeartBeatData(hbd);
                break;
            case "SLPM_869.16": 
                PsxExe exe = new PsxExe();
                exe.setData(data);
                binary.setExecutable(exe);
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
