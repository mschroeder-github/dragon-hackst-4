
package net.markus.projects.dq4h.io;

import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

/**
 * Class for reading and writing <a href="https://en.wikipedia.org/wiki/Shift_JIS">Shift JIS</a> 
 * which is used in Dragon Quest 4.
 * @see <a href="http://www.rikai.com/library/kanjitables/kanji_codes.sjis.shtml">Shift JIS Kanji Table</a>
 */
public class ShiftJIS {
    
    private static Map<Short, Character> sjishort2char;
    private static Map<Character, Byte[]> char2sjis;

    static {
        try {
            readTable();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static void readTable() throws IOException {
        String content = IOUtils.toString(ShiftJIS.class.getResourceAsStream("/kanji_codes.sjis.txt"), UTF_8);

        char2sjis = new HashMap<>();
        sjishort2char = new HashMap<>();

        int offset = 0;

        for (String line : content.split("\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String start = line.substring(0, 8);

            String rest;
            try {
                rest = line.substring(10);
            } catch (Exception e) {
                continue;
            }

            start = start.trim().replace(" ", "");

            byte[] b = Inspector.toBytes(start);
            short s = Converter.bytesToShortBE(b);

            if (rest.startsWith("   ")) {
                offset = 3;
            } else {
                offset = 0;
            }

            int j = offset > 0 ? 1 : 0;

            for (int i = offset; i < rest.length(); i += 2) {
                char c = rest.charAt(i);

                short next = (short) (s + j);
                j++;

                //System.out.println(Utils.bytesToHex(Utils.shortToBytes(next)) + " " + c);
                
                //sjis2char.put(Utils.toObjects(Utils.shortToBytes(next)), c);
                char2sjis.put(c, Converter.bytesToByteObjects(Converter.shortToBytesBE(next)));
                sjishort2char.put(next, c);
            }
        }

        //fixes
        sjishort2char.put(Converter.bytesToShortBE(Inspector.toBytes("ff02")), '"');
        sjishort2char.put(Converter.bytesToShortBE(Inspector.toBytes("ff0a")), '*');
        sjishort2char.put(Converter.bytesToShortBE(Inspector.toBytes("ff11")), '1');//FULLWIDTH DIGIT ONE
    }
    
    /**
     * Returns a japanese character for a given short.
     * @param s 
     * @return  
     */
    public static Character getCharacter(short s) {
        return sjishort2char.get(s);
    }
    
    /**
     * Returns a japanese character for two bytes.
     * @param bytes
     * @return 
     */
    public static Character getCharacter(byte[] bytes) {
        return sjishort2char.get(Converter.bytesToShortBE(bytes));
    }
    
    /**
     * Returns a japanese character for an hexadecimal value.
     * @param hex
     * @return 
     */
    public static Character getCharacter(String hex) {
        return sjishort2char.get(Converter.bytesToShortBE(Inspector.toBytes(hex)));
    }
    
    /**
     * Returns the shift jis bytes for a given japanese character.
     * @param c
     * @return 
     */
    public static byte[] getBytes(char c) {
        return Converter.byteObjectsToBytes(char2sjis.get(c));
    }
    
    /**
     * Returns the shift jis hexadecimal value for a given japanese character.
     * @param c
     * @return 
     */
    public static String getHex(char c) {
        return Inspector.toHex(Converter.byteObjectsToBytes(char2sjis.get(c)));
    }
    
}
