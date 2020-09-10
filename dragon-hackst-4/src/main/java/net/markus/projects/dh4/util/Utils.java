package net.markus.projects.dh4.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class Utils {

    public static short[] toShortArray(String japText, Map<Character, Byte[]> char2sjis) {
        short[] array = new short[japText.length()];
        for (int i = 0; i < array.length; i++) {
            array[i] = bytesToShort(toPrimitives(char2sjis.get(japText.charAt(i))));
        }
        return array;
    }
    
    public static byte[] toByteArray(String japText, Map<Character, Byte[]> char2sjis) {
        byte[] array = new byte[japText.length()*2];
        for (int i = 0; i < japText.length(); i++) {
            Byte[] bb = char2sjis.get(japText.charAt(i));
            array[i*2] = bb[0];
            array[(i*2)+1] = bb[1];
        }
        return array;
    }

    public static Byte[] toObjects(byte[] bytesPrim) {
        Byte[] bytes = new Byte[bytesPrim.length];
        Arrays.setAll(bytes, n -> bytesPrim[n]);
        return bytes;
    }

    public static byte[] toPrimitives(Byte[] oBytes) {
        byte[] bytes = new byte[oBytes.length];

        for (int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }

        return bytes;
    }

    public static short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
    }
    
    public static short bytesToShortLE(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(value).array();
    }

    public static int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }
    
    public static int bytesToIntLE(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static byte[] shortToBytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }
    
    public static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
    
    /*
    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            sb.append(bytesToHex(new byte[] { b })).append(" ");
        }
        return sb.toString();
    }
    */
    
    public static String toBits(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            sb.append(toBits(b));
        }
        return sb.toString();
    }
    
    public static String toBits(byte b) {
        return Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
    }
    
    public static String toHexString(byte[] bytes, int... cut) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int j = 0;
        for(byte b : bytes) {
            if(j < cut.length && i == cut[j]) {
                j++;
                i = 0;
                sb.append(" | ");
            } else {
                sb.append(" ");
            }
            
            sb.append(bytesToHex(new byte[] { b }));
            
            i++;
        }
        return sb.toString().trim();
    }
    
    public static String toHexStringASCII(byte[] bytes, int... cut) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int j = 0;
        for(byte b : bytes) {
            if(j < cut.length && i == cut[j]) {
                j++;
                i = 0;
                sb.append(" | ");
            } else {
                sb.append(" ");
            }
            
            int c = (b & 0xff);
            if(c >= '!' && c <= '~') {
                sb.append(" " + (char) c);
            } else {
                sb.append(" .");
            }
            
            i++;
        }
        return sb.toString().trim();
    }
    
    public static String toHexStringJp(byte[] bytes, Map<Short, Character> sjishort2char, int... cut) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int j = 0;
        for(int k = 0; k < bytes.length - 1; k += 2) {
            
            if(j < cut.length && i == cut[j]) {
                j++;
                i = 0;
                sb.append("｜");
            } else {
                sb.append("");
            }
            
            byte[] bb = new byte[] { bytes[k], bytes[k+1] };
            short s = Utils.bytesToShort(bb);
            Character c = sjishort2char.get(s);
            
            if(c == null) {
                sb.append("　");//jp space
            } else {
                sb.append(c);
            }
            
            
            i++;
        }
        return sb.toString().trim();
    }
    
    public static String toHexString(int len, int... cut) {
        byte[] array = new byte[len];
        for(int b = 0; b < len; b++) {
            array[b] = (byte) b;
        }
        return toHexString(array, cut);
    }
    
    public static String toHexStringOne(int len, int... cut) {
        byte[] array = new byte[len+1];
        for(int b = 1; b <= len; b++) {
            array[b] = (byte) b;
        }
        return toHexString(array, cut);
    }
    
    public static String toHexDump(byte[] bytes, int w, boolean address, boolean ascii, Map<Short, Character> sjishort2char) {
        int h = (bytes.length / w) + 1;
        
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < h; i++) {
            byte[] line = Arrays.copyOfRange(bytes, i * w, Math.min(i*w + w, bytes.length));
            
            if(address) {
                byte[] addr = shortToBytes((short) (i * w));
                String addrStr = "0x" + bytesToHex(new byte[] { addr[0] }) + bytesToHex(new byte[] { addr[1] });
                sb.append(addrStr).append(" | ");
            }
            
            sb.append(toHexString(line));
            
            if(line.length < w) {
                int remaining = w - line.length;
                for(int j = 0; j < remaining; j++) {
                    sb.append("   ");
                }
            }
            
            if(ascii || sjishort2char != null) {
                sb.append(" | ");
            }
            
            if(ascii) {
                sb.append(toHexStringASCII(line)).append(" | ");
            }
            
            if(sjishort2char != null) {
                sb.append(toHexStringJp(line, sjishort2char));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    public static boolean allZero(byte[] array) {
        for(int i = 0; i < array.length; i++) {
            if(array[i] != 0)
                return false;
        }
        return true;
    }
    
    public static List<Integer> find(byte[] pattern, byte[] data) {
        List<Integer> pos = new ArrayList<>();
        
        for(int i = 0; i < data.length - pattern.length; i++) {
            byte[] copy = Arrays.copyOfRange(data, i, i+pattern.length);
            
            if(Arrays.equals(copy, pattern)) {
                pos.add(i);
            }
        }
        
        return pos;
    }
    
    public static List<Integer> findHiragana(int minLen, byte[] data) {
        List<Integer> pos = new ArrayList<>();
        
        /*
        82 9e |    ぁ あ ぃ い ぅ う ぇ え ぉ お か が き ぎ く
        82 ae | ぐ け げ こ ご さ ざ し じ す ず せ ぜ そ ぞ た
        82 be | だ ち ぢ っ つ づ て で と ど な に ぬ ね の は
        82 ce | ば ぱ ひ び ぴ ふ ぶ ぷ へ べ ぺ ほ ぼ ぽ ま み
        82 de | む め も ゃ や ゅ ゆ ょ よ ら り る れ ろ ゎ わ
        82 ee | ゐ ゑ を ん
        */
        
        byte[] _82 = Utils.hexStringToByteArray("82");
        byte[] beginArray = Utils.hexStringToByteArray("a0"); //a
        byte[] endArray = Utils.hexStringToByteArray("f1"); //n
        
        int len = 0;
        for(int i = 0; i < data.length; i += 2) {
            if(data[i] == _82[0] && data[i+1] >= beginArray[0] && data[i+1] <= endArray[0]) {
                len++;
            } else {
                if(len >= minLen) {
                    pos.add(i - len*2);
                }
                len = 0;
            }
        }
        
        return pos;
    }
    
    public static List<Integer> findASCII(int minLen, byte[] data, boolean noSymbols) {
        List<Integer> pos = new ArrayList<>();
        
        int len = 0;
        for(int i = 0; i < data.length; i++) {
            int c = (data[i] & 0xff);
        
            boolean found = false;
            if(noSymbols) {
                found = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
            } else {
                found = c >= '!' && c <= '~';
            }
            
            if(found) { 
                len++;
            } else {
                if(len >= minLen) {
                    pos.add(i - len);
                }
                len = 0;
            }
        }
        
        return pos;
    }
    
    
    public static BufferedImage toGrayscale(byte[] data, int w, int max) {
        if(max == -1) {
            max = data.length;
        }
        
        int h = (int) (max / w) + 1;
        
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        for(int i = 0; i < max; i++) {
            int v = (int) (data[i] & 0xff); //unsigned
            bi.setRGB(i % w, i / w, new Color(v, v, v).getRGB());
        }
        
        return bi;
    }
}