
package net.markus.projects.dh4;

import net.markus.projects.dh4.util.Utils;

/**
 * 
 */
public class OpCode {

    private byte[] data;
    
    private int pos;
    
    private String asm;
    private String comment = "";
    
    public OpCode() {
        
    }
    
    public OpCode(String asm) {
        fromAsm(asm);
    }

    public OpCode(String dataHex, String asm, String comment) {
        data = Utils.hexStringToByteArray(dataHex);
        this.asm = asm;
        this.comment = comment;
    }
    
    @Override
    public String toString() {
        return getPosAsHex() + " " + Utils.bytesToHex(data).toUpperCase() + " " + toAsm();
    }
    
    public void fromAsm(String asm) {
        String bits = "";
        
        String[] segments = asm.split(" ");
        
        String op = segments[0].trim();
        String[] params = new String[0];
        if(segments.length > 1) {
            params = segments[1].trim().split(",");
        }
        
        if(asm.startsWith("setb r21,r22,")) {
            
            //2ED5 => 0010 1110 1101 0101
            bits += "0010111011010101";
            
            bits += Utils.toBits(Utils.hexStringToByteArray(params[2])).substring(0, 16);

            
        } else if(asm.startsWith("setb r21,r23,")) {
            
            //2EF5 => 0010 1110 1101 0101
            bits += "0010111011110101";
            
            bits += Utils.toBits(Utils.hexStringToByteArray(params[2])).substring(0, 16);
            
        } else {
            switch(op) {
                case "nop":
                    bits += "000";
                    bits += "000";
                    break;
                    
                    //necessary to load bit index
                case "lui": //Load   Upper   Immediate
                    
                    //0011 1100 0001 0110 => 3C16 (it is for r22 = s6)
                    bits += "0011110000010110";
                    
                    String valueBits = Utils.toBits(Utils.hexStringToByteArray(params[1]));
                    
                    valueBits = valueBits.substring(0, 16);
                    
                    bits += valueBits;
                    break;
                    
                case "ori": //Or   Immediate
                    
                    //0011 0100 0001 0111 => 3417 (it is for r23 = s7)
                    bits += "001101000001";
                    
                    switch(params[0].trim()) {
                        case "r23": //s7
                            bits += "0111"; 
                            break;
                        case "r22": //s6
                            bits += "0110"; 
                            break;
                        case "r21": //s5
                            bits += "0101"; 
                            break;
                    }
                    
                    String valBits = Utils.toBits(Utils.hexStringToByteArray(params[1]));
                    
                    valBits = valBits.substring(0, 16);
                    
                    bits += valBits;
                    break;
                    
                case "jmp":
                    bits += "000"; //0
                    bits += "010"; //2
                    
                    String addrBits = Utils.toBits(Utils.hexStringToByteArray(params[0]));
                    addrBits = addrBits.substring(addrBits.length() - 28, addrBits.length());
                    
                    addrBits = addrBits.substring(0, addrBits.length() - 2);
                    
                    bits += addrBits;
                    break;
                    
                case "jnz": //jump not zero
                    //jump not equal [register] <0>
                    
                    //101101010 => 16A (it is for r21)
                    bits += "000101101010";
                    
                    String moveBits = Utils.toBits(Utils.intToByteArray(Integer.parseInt(params[1])));
                    
                    moveBits = moveBits.substring(moveBits.length() - 20, moveBits.length());
                    
                    bits += moveBits;
                    break;
                    
                case "jne": //jump not zero
                    //jump not equal [register] [register]
                    
                    //r21 s5 101
                    //r22 s6 110
                    //r23 s7 111
                    
                    //101 101 010 => 16A (it is for r21)
                    bits += "000101";
                    
                    switch(params[0].trim()) {
                        case "r23": //s7
                            bits += "10111"; 
                            break;
                        case "r22": //s6
                            bits += "10110"; 
                            break;
                        case "r21": //s5
                            bits += "10101"; 
                            break;
                    }
                    
                    switch(params[1].trim()) {
                        case "r23": //s7
                            bits += "10111"; 
                            break;
                        case "r22": //s6
                            bits += "10110"; 
                            break;
                        case "r21": //s5
                            bits += "10101"; 
                            break;
                    }
                    
                    String mvBits = Utils.toBits(Utils.intToByteArray(Integer.parseInt(params[2])));
                    
                    mvBits = mvBits.substring(mvBits.length() - 16, mvBits.length());
                    
                    bits += mvBits;
                    break;
            }
        }
        
        while(bits.length() < 32) {
            bits += "0";
        }
        
        byte[] array = new byte[4];
        for(int i = 0; i < 4; i++) {
            array[i] = (byte) Integer.parseInt(bits.substring(i * 8, (i * 8) + 8), 2);
        }
        
        this.data = array;
        this.asm = asm;
        //this.data = Utils.hexStringToByteArray("24060001");
    }
    
    public String toAsm() {
        if(asm != null) {
            return asm;
        }
        
        if(isNop()) {
            return "nop";
        }
        
        String bits = Utils.toBits(data);
        
        int a = Utils.bitsToIntLE(bits.substring(0, 3));
        int b = Utils.bitsToIntLE(bits.substring(3, 6));
        
        //jmp
        if(a == 0 && b == 2) {
            return "jmp";
        }
        
        return "";
    }
    
    public String getPosAsHex() {
        byte[] bs = Utils.intToByteArray(pos);
        return Utils.bytesToHex(bs).toUpperCase();
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public boolean isNop() {
        return Utils.allZero(data);
    }

    public String getAsm() {
        return asm;
    }

    public String getComment() {
        return comment;
    }
    
    public OpCode setComment(String comment) {
        this.comment = comment;
        return this;
    }
    
}
