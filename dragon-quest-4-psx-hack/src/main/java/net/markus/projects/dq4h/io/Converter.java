
package net.markus.projects.dq4h.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * A converter utility class for all kinds of formats.
 */
public class Converter {

    //----- bits -------------
    
    public static int bitsToIntLE(String bits) {
        int n = 0;
        for (int i = bits.length() - 1; i >= 0; i--) {
            if (bits.charAt(i) == '1') {
                n += (int) Math.pow(2, (bits.length() - 1) - i);
            }
        }
        return n;
    }
    
    //----- bytes ------------
    
    public static int bytesToIntLE(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
    
    public static int bytesToIntBE(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }
    
    public static short bytesToShortLE(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }
 
    public static short bytesToShortBE(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
    }
    
    public static String bytesToBits(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(byteToBits(b));
        }
        return sb.toString();
    }

    public static String byteToBits(byte b) {
        return Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
    }
    
    public static Byte[] bytesToByteObjects(byte[] bytes) {
        Byte[] byteObjects = new Byte[bytes.length];
        Arrays.setAll(byteObjects, n -> bytes[n]);
        return byteObjects;
    }
    
    public static byte[] byteObjectsToBytes(Byte[] byteObjects) {
        byte[] bytes = new byte[byteObjects.length];
        for (int i = 0; i < byteObjects.length; i++) {
            bytes[i] = byteObjects[i];
        }
        return bytes;
    }
     
    // ------- int --------------
    
    public static byte[] intToBytesLE(int value) {
        return new byte[]{
            (byte) value,
            (byte) (value >>> 8),
            (byte) (value >>> 16),
            (byte) (value >>> 24)
        };
    }
    
    public static byte[] intToBytesBE(int value) {
        return new byte[]{
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value
        };
    }
    
    
    // ------ short ------------
    
    public static byte[] shortToBytesLE(short value) {
        return new byte[]{
            (byte) value,
            (byte) (value >>> 8)
        };
    }

    public static byte[] shortToBytesBE(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(value).array();
    }
    
    // ----- helper ------------
    
    public static byte[] reverse(byte[] array) {
        if (array == null) {
            return null;
        }
        byte[] result = new byte[array.length];
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            result[j] = array[i];
            result[i] = tmp;
            j--;
            i++;
        }
        return result;
    }
    
    public static String reverse(String str) {
        return new StringBuilder(str).reverse().toString();
    }
    
}
