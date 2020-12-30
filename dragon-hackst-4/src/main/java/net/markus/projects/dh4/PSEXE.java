package net.markus.projects.dh4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.markus.projects.dh4.util.Utils;

/**
 *
 */
public class PSEXE extends ArrayList<OpCode> {

    private File file;

    private BigInteger start;

    private byte[] header;

    public PSEXE(File file, String startHex) throws IOException {
        this.file = file;
        setStart(startHex);
        load();
    }

    public void setStart(String hex) {
        start = new BigInteger(hex, 16);
    }

    private void load() throws IOException {
        FileInputStream fis = new FileInputStream(file);

        header = new byte[2048];
        for (int i = 0; i < 2048; i++) {
            header[i] = (byte) fis.read();
        }

        int offset = 0;

        while (fis.available() > 0) {

            for (int i = 0; i < 4; i++) {

                byte b1 = (byte) fis.read();
                byte b2 = (byte) fis.read();
                byte b3 = (byte) fis.read();
                byte b4 = (byte) fis.read();

                byte[] data = new byte[]{
                    b4,
                    b3,
                    b2,
                    b1
                };

                OpCode opcode = new OpCode();
                opcode.setData(data);
                opcode.setPos(start.add(new BigInteger("" + offset, 10)).intValue());

                offset += 4;

                add(opcode);
            }
        }

        fis.close();
    }

    public void save(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);

        fos.write(header);

        for (OpCode code : this) {
            fos.write(code.getData()[3]);
            fos.write(code.getData()[2]);
            fos.write(code.getData()[1]);
            fos.write(code.getData()[0]);
        }

        fos.close();
    }

    public void replace(String positionHex, OpCode opCode) {
        this.set(getIndex(positionHex), opCode);
    }

    public String replaceSeq(String positionHex, OpCode... opCodes) {
        BigInteger pos = new BigInteger(positionHex, 16);
        int offset = pos.subtract(start).intValue();
        int index = offset / 4;

        int i;
        for (i = 0; i < opCodes.length; i++) {
            this.set(index + i, opCodes[i]);

            opCodes[i].setPos(start.add(new BigInteger("" + offset, 10)).intValue());
            offset += 4;
        }

        return Utils.bytesToHex(Utils.intToByteArray(start.intValue() + offset)).toUpperCase();
    }

    //not a good idea to use the nop seq because they are set to zero after
    //assembler code is loaded, it seems
    @Deprecated
    public List<int[]> longestNopSeqs() {

        List<int[]> l = new ArrayList<>();

        int last = -1;

        for (int i = 0; i < this.size(); i++) {

            //end
            if (last != -1 && !get(i).isNop()) {
                l.add(new int[]{last, i});
                last = -1;
            }

            //start
            if (last == -1 && get(i).isNop()) {
                last = i;
            }
        }

        l.removeIf(array -> (array[1] - array[0]) < 500);

        l.sort((a, b) -> Integer.compare(b[1] - b[0], a[1] - a[0]));

        return l;
    }

    public int getIndex(String positionHex) {
        BigInteger pos = new BigInteger(positionHex, 16);
        int index = pos.subtract(start).intValue();
        index /= 4;
        return index;
    }

    public OpCode get(String postitionHex) {
        return get(getIndex(postitionHex));
    }

    //first tests
    @Deprecated
    public void patch() {
        //for(OpCode opcode : psexe) {
        //    System.out.println(opcode);
        //}

        /*
        List<int[]> posList = psexe.longestNopSeqs();
        for(int[] pos : posList) {
            
            System.out.println(Arrays.toString(pos) + 
                    " (" + psexe.get(pos[0]).getPosAsHex() + " " + psexe.get(pos[1]).getPosAsHex() + ") " + 
                    (pos[1]-pos[0])
            );
            
            for(int i = pos[0]; i < pos[1]; i++) {
                System.out.println("\t" + psexe.get(i));
            }
            
        }
         */
        //800B9240 (here we could start our extra code)
        //800B98A8 (here we could end it)
        String startPos = "8001D4CC";
        //String endPos = "8001D4D4";
        //BigInteger start = new BigInteger(startPos, 16);
        //BigInteger end = new BigInteger(endPos, 16);

        //this is the address when the pointer is written to RAM
        //and we could manipulate it
        String nopPos = "8008EBA4";
        String nopPosPlus1 = "8008EBA8";
        String nopPosPlus2 = "8008EBAC";

        //OpCode jzR2 = get(nopPosPlus1);
        //replace(nopPos, jzR2);
        replace(nopPos, new OpCode("jmp " + startPos));

        //3FF8 diff to pointer place
        //"8E170058", "mov r23,[r16+58h]", "loads the dialog pointer"
        List<OpCode> codes = new ArrayList<>();

        //begin
        codes.addAll(Arrays.asList(
                //calculate the difference in r23
                new OpCode("8E170058", "mov r23,[r16+58h]", "loads the dialog pointer"),
                new OpCode("0017BA00", "shl r23,8h", "remove the bit information from the pointer"),
                new OpCode("0017BA02", "shr r23,8h", "remove the bit information from the pointer"),
                new OpCode("3C158000", "mov r21,80000000h", "to create a 0x80... pointer"),
                new OpCode("02F5B825", "or r23,r21", "to create a 0x80... pointer"),
                new OpCode("02E5B823", "sub r23,r5", "get the difference"),
                //get the signature in r22
                new OpCode("8CB60004", "mov r22,[r5+4h]", "loads the textblock signature")
        ));

        //middle
        codes.addAll(Arrays.asList(
                //first scene signature is 0x6C, if r21 is 0 it was equal
                //new OpCode("2ED5006C", "setb r21,r22,6Ch", "check if first scene is loaded"),
                //new OpCode("setb r21,r22,006C"),
                new OpCode("ori r21,006C"),
                new OpCode("jne r21,r22,4"), //skips 4 op codes and stops on the 5th
                
                //per dialog pointer
                //new OpCode("2EF501DF", "setb r21,r23,1DFh", "check if second dialog is loaded"),
                //new OpCode("setb r21,r23,01DF"),
                new OpCode("ori r21,01DF"),
                new OpCode("jne r21,r23,2"), //skips 1 op code
                new OpCode("ori r23,01DF"), //load byte index (diff)
                new OpCode("lui r22,1500"), //load bit index
                //TODO jump to begin of end-block to convert it
                
                new OpCode("nop"),
                new OpCode("nop"),
                new OpCode("nop")
        ));

        //end
        codes.addAll(Arrays.asList(
                //in r23 is the new diff and in r22 is the bit index (already right aligned)
                //final steps
                new OpCode("00B7B821", "add r23,r5,r23", "add to diff the absolute position"),
                new OpCode("0017BA00", "shl r23,8h", "remove the bit information from the pointer"),
                new OpCode("0017BA02", "shr r23,8h", "remove the bit information from the pointer"),
                new OpCode("02F6B825", "or r23,r22", "add bit information from r22"),
                new OpCode("AE170058", "mov [r16+58h],r23", "overwrite updated pointer address"),
                //jump back
                new OpCode("jmp " + nopPosPlus1),
                new OpCode("nop")
        ));

        //replace
        replaceSeq(startPos, codes.toArray(new OpCode[0]));
    }
    
    
    public void patch(String startPosHex, String nopPosHex, List<OpCode> middle) {
        
        //we jump away
        replace(nopPosHex, new OpCode("jmp " + startPosHex));
        
        String nopPosNext1 = Utils.bytesToHex(Utils.intToByteArray(new BigInteger(nopPosHex, 16).intValue() + 4)).toUpperCase();
        
        List<OpCode> codes = new ArrayList<>();
        
        //begin
        codes.addAll(Arrays.asList(
                //calculate the difference in r23
                new OpCode("8E170058", "mov r23,[r16+58h]", "loads the dialog pointer"),
                new OpCode("0017BA00", "shl r23,8h", "remove the bit information from the pointer"),
                new OpCode("0017BA02", "shr r23,8h", "remove the bit information from the pointer"),
                new OpCode("3C158000", "mov r21,80000000h", "to create a 0x80... pointer"),
                new OpCode("02F5B825", "or r23,r21", "to create a 0x80... pointer"),
                new OpCode("02E5B823", "sub r23,r5", "get the difference"),
                //get the signature in r22
                new OpCode("8CB60004", "mov r22,[r5+4h]", "loads the textblock signature")
        ));
        
        //do not change anything if not matched so that normal dialog works
        middle.add(new OpCode("nop"));
        middle.add(new OpCode("jmp " + nopPosNext1));
        
        //the offset in bytes how far we have to jump from startPosHex to the end-block
        int offset = (codes.size() + middle.size()) * 4;
        String endBlockPos = Utils.bytesToHex(Utils.intToByteArray(new BigInteger(startPosHex, 16).intValue() + offset)).toUpperCase();
        OpCode jumpToEnd = new OpCode("jmp " + endBlockPos);
        
        for(int i = 0; i < middle.size(); i++) {
            if(middle.get(i).getComment().equals("replaceLater")) {
                middle.set(i, jumpToEnd);
            }
        }
        
        //middle
        codes.addAll(middle);
        
        //end
        codes.addAll(Arrays.asList(
                //in r23 is the new diff and in r22 is the bit index (already right aligned)
                //final steps
                new OpCode("nop"),
                new OpCode("00B7B821", "add r23,r5,r23", "add to diff the absolute position"),
                new OpCode("0017BA00", "shl r23,8h", "remove the bit information from the pointer"),
                new OpCode("0017BA02", "shr r23,8h", "remove the bit information from the pointer"),
                new OpCode("02F6B825", "or r23,r22", "add bit information from r22"),
                new OpCode("AE170058", "mov [r16+58h],r23", "overwrite updated pointer address"),
                //jump back
                new OpCode("jmp " + nopPosNext1),
                new OpCode("nop")
        ));
        
        //update addresses (just to be sure and check everything)
        for(int i = 0; i < codes.size(); i++) {
            //String addr = Utils.bytesToHex(Utils.intToByteArray(new BigInteger(startPosHex, 16).intValue() + (i*4))).toUpperCase();
            codes.get(i).setPos(new BigInteger(startPosHex, 16).intValue() + (i*4));
        }
        
        codes.forEach(opc -> System.out.println(opc));
        
        //patch it at start pos
        replaceSeq(startPosHex, codes.toArray(new OpCode[0]));
    }
}
