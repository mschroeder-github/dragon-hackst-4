
package net.markus.projects.dq4h.io;

import java.util.Arrays;

/**
 * To inspect data values we convert data to textual representations.
 */
public class Inspector {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    
    
    public static String splitScreen(String left, int w, String right) {
        StringBuilder sb = new StringBuilder();
        
        String[] leftLines = left.split("\n");
        String[] rightLines = right.split("\n");
        
        for(int i = 0; i < Math.max(leftLines.length, rightLines.length); i++) {
            
            String l = i >= leftLines.length ? "" : leftLines[i];
            String r = i >= rightLines.length ? "" : rightLines[i];
            
            l = l.replace("\t", "    ");
            r = r.replace("\t", "    ");
            
            if(l.length() > w) {
                l = l.substring(0, w - 3) + "...";
            } else if(l.length() < w) {
                while(l.length() < w) {
                    l += " ";
                }
            }
            
            sb.append(l + " | " + r + "\n");
        }
        
        return sb.toString();
    } 
    
    public static String toHexString(byte[] bytes, int... cut) {
        if (bytes == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        int j = 0;
        for (byte b : bytes) {
            if (j < cut.length && i == cut[j]) {
                j++;
                i = 0;
                sb.append(" | ");
            } else {
                sb.append(" ");
            }

            sb.append(toHex(new byte[]{b}));

            i++;
        }
        return sb.toString().trim();
    }
    
    /**
     * Converts bytes to a hexadecimal string without spaces.
     * @param bytes
     * @return 
     */
    public static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    public static String toDecString(byte[] bytes, int... cut) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int j = 0;
        for (byte b : bytes) {
            if (j < cut.length && i == cut[j]) {
                j++;
                i = 0;
                sb.append(" | ");
            } else {
                sb.append(" ");
            }

            sb.append(String.format("%03d", (int) (b & 0xff)));

            i++;
        }
        return sb.toString().trim();
    }

    public static String toHexDump(byte[] bytes, int w) {
        return toHexDump(bytes, w, true, false);
    }
    
    public static String toHexDump(byte[] bytes, int w, boolean address, boolean ascii) {
        int h = (bytes.length / w) + 1;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < h; i++) {
            byte[] line = Arrays.copyOfRange(bytes, i * w, Math.min(i * w + w, bytes.length));

            if (address) {
                byte[] addr = Converter.shortToBytesBE((short) (i * w));
                String addrStr = "0x" + toHex(new byte[]{addr[0]}) + toHex(new byte[]{addr[1]});
                sb.append(addrStr).append(" | ");
            }

            sb.append(toHexString(line));

            if (line.length < w) {
                int remaining = w - line.length;
                for (int j = 0; j < remaining; j++) {
                    sb.append("   ");
                }
            }
            
            if (ascii) {
                sb.append(" | ");
                sb.append(toASCII(line)).append(" | ");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    public static String toASCII(byte[] bytes, int... cut) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int j = 0;
        for (byte b : bytes) {
            if (j < cut.length && i == cut[j]) {
                j++;
                i = 0;
                sb.append(" | ");
            } else {
                sb.append(" ");
            }

            int c = (b & 0xff);
            if (c >= '!' && c <= '~') {
                sb.append(" " + (char) c);
            } else {
                sb.append(" .");
            }

            i++;
        }
        return sb.toString().trim();
    }
    
    public static byte[] toBytes(String hex) {
        hex = hex.replace(" ", "");
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
}
