
package net.markus.projects.dh4.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * 
 */
public class DQFiles {

    public String name;
    
    //read
    public File readHbdFile;
    
    //for write
    public File patchedFolder;
    public File exeFile;
    public File cnfFile;
    public File writeHbdFile;
    
    //for translation
    public File translationFolderWrite;
    public File translationFolderRead;
    
    //psxbuild
    public File catFile;
    public File sysFile;
    
    
    public static DQFiles dq7en() {
        DQFiles files = new DQFiles();
        
        //read
        files.readHbdFile  = new File("../../Dragon Warrior VII (Disc 1)/dq7-psxrip/HBD1PS1D.W71");
        
        files.patchedFolder = new File("../../Dragon Warrior VII (Disc 1) (Patched)");
        files.exeFile = new File("../../Dragon Warrior VII (Disc 1)/dq7-psxrip/SLUSP012.06");
        files.cnfFile = new File("../../Dragon Warrior VII (Disc 1)/dq7-psxrip/SYSTEM.CNF");
        files.writeHbdFile = new File("../../Dragon Warrior VII (Disc 1)/dq7-psxrip/HBD1PS1D.W71.patched");
        
        //psxbuild
        files.catFile = new File("../../Dragon Warrior VII (Disc 1)/Dragon Warrior VII (Disc 1).cat");
        files.sysFile = new File("../../Dragon Warrior VII (Disc 1)/Dragon Warrior VII (Disc 1).sys");
        
        files.name = "dq7";
        
        return files;
    }
    
    public static DQFiles dq4() {
        DQFiles files = new DQFiles();
        
        //read
        files.readHbdFile  = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip/HBD1PS1D.Q41");
        
        files.patchedFolder = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan) (Patched)");
        files.exeFile = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip/SLPM_869.16");
        files.cnfFile = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip/SYSTEM.CNF");
        files.writeHbdFile = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip/HBD1PS1D.Q41.patched");
        
        files.translationFolderWrite = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/translation");
        files.translationFolderRead = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/translation"); //new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/translation_test");
        
        //psxbuild
        files.catFile = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip.cat");
        files.sysFile = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/dq4-psxrip.sys");
        
        files.name = "dq4";
        
        return files;
    }
    
    public void psxbuild(File psxbuildBin) throws IOException {
        
        //folders
        patchedFolder.mkdirs();
        File newCatFile = new File(patchedFolder, name + ".cat");
        File tmpFolder = new File(patchedFolder, name);
        tmpFolder.mkdirs();
        
        String hbdFileName = writeHbdFile.getName().replace(".patched", "");
        
        //===============================
        
        String catContent = FileUtils.readFileToString(catFile, StandardCharsets.UTF_8);
        
        StringBuilder newCatSB = new StringBuilder();
        /*
        system_area {
          file "../../../../Dragon Warrior VII (Disc 1)/Dragon Warrior VII (Disc 1).sys"
        }
        */
        newCatSB.append("system_area {\n");
        newCatSB.append("file \"" + sysFile.getAbsolutePath() + "\"\n");
        newCatSB.append("}\n\n");
        /*
        volume {
          ...
        }
        */
        int volumeBegin = catContent.indexOf("volume {");
        int volumeEnd = catContent.indexOf("}", volumeBegin) + 1;
        String volume = catContent.substring(volumeBegin, volumeEnd);
        newCatSB.append(volume);
        newCatSB.append("\n\n");
        /*
        dir {
          file SYSTEM.CNF
          file SLUSP012.06
          file HBD1PS1D.W71
        }
        */
        newCatSB.append("dir {\n");
        newCatSB.append("file " + cnfFile.getName() + "\n");
        newCatSB.append("file " + exeFile.getName() + "\n");
        newCatSB.append("file " + hbdFileName + "\n");
        newCatSB.append("}\n");
        
        
        //====================================
        
        
        //the three files in the patched folder (working dir)
        FileUtils.writeStringToFile(newCatFile, newCatSB.toString(), StandardCharsets.UTF_8);
        FileUtils.copyFile(cnfFile, new File(tmpFolder, cnfFile.getName()));
        //FileUtils.copyFile(exeFile, new File(tmpFolder, exeFile.getName()));
        FileUtils.deleteQuietly(new File(tmpFolder,hbdFileName));
        FileUtils.moveFile(writeHbdFile, new File(tmpFolder, hbdFileName));
        
        //--cuefile --verbose
        String[] cmd = new String[] { psxbuildBin.getAbsolutePath(), "--cuefile", "--verbose", newCatFile.getAbsolutePath() };
        
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(patchedFolder.getCanonicalFile());
        System.out.println("start psxbuild ...");
        Process process = pb.start();
        try {
            process.waitFor();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        int ret = process.exitValue();
        
        String out = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        String err = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
        
        System.out.println(out);
        System.out.println(err);
    }
}
