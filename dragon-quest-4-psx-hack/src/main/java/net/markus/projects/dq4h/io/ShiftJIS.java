
package net.markus.projects.dq4h.io;

import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.markus.projects.dq4h.data.HuffmanCharacter;
import net.markus.projects.dq4h.data.HuffmanNode;
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
    
    private static final Map<String, String> ascii2jp;
    static {
        ascii2jp = new HashMap<>();
        /*
        !"#$%&'()*+,-./0123456789:;<=>?
        @ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_
        `abcdefghijklmnopqrstuvwxyz{|}~
        */
        ascii2jp.put("!", "！");
        ascii2jp.put("\"", "”");
        ascii2jp.put("#", "＃");
        ascii2jp.put("$", "＄");
        ascii2jp.put("%", "％");
        ascii2jp.put("&", "＆");
        ascii2jp.put("'", "「"); //e.g. LEFT SINGLE QUOTATION MARK not avail in game font
        ascii2jp.put("(", "（");
        ascii2jp.put(")", "）");
        ascii2jp.put("*", "＊");
        ascii2jp.put("+", "＋");
        ascii2jp.put(",", "、");
        ascii2jp.put("-", "ー");
        ascii2jp.put(".", "．");//"。"); //FULLWIDTH FULL STOP is available
        ascii2jp.put("/", "／");
        
        //０ １ ２ ３ ４ ５ ６ ７ ８ ９ 
        ascii2jp.put("0", "０");
        ascii2jp.put("1", "１");
        ascii2jp.put("2", "２");
        ascii2jp.put("3", "３");
        ascii2jp.put("4", "４");
        ascii2jp.put("5", "５");
        ascii2jp.put("6", "６");
        ascii2jp.put("7", "７");
        ascii2jp.put("8", "８");
        ascii2jp.put("9", "９");
        ascii2jp.put(":", "：");
        ascii2jp.put(";", "；");
        ascii2jp.put("<", "＜");
        ascii2jp.put("=", "＝");
        ascii2jp.put(">", "＞");
        ascii2jp.put("?", "？");
        ascii2jp.put("@", "＠");
        
        /*
        82 5f |    Ａ Ｂ Ｃ Ｄ Ｅ Ｆ Ｇ Ｈ Ｉ Ｊ Ｋ Ｌ Ｍ Ｎ Ｏ 
        82 6f | Ｐ Ｑ Ｒ Ｓ Ｔ Ｕ Ｖ Ｗ Ｘ Ｙ Ｚ                
        82 80 |    ａ ｂ ｃ ｄ ｅ ｆ ｇ ｈ ｉ ｊ ｋ ｌ ｍ ｎ ｏ 
        82 90 | ｐ ｑ ｒ ｓ ｔ ｕ ｖ ｗ ｘ ｙ ｚ 
        */
        ascii2jp.put("A", "Ａ");
        ascii2jp.put("B", "Ｂ");
        ascii2jp.put("C", "Ｃ");
        ascii2jp.put("D", "Ｄ");
        ascii2jp.put("E", "Ｅ");
        ascii2jp.put("F", "Ｆ");
        ascii2jp.put("G", "Ｇ");
        ascii2jp.put("H", "Ｈ");
        ascii2jp.put("I", "Ｉ");
        ascii2jp.put("J", "Ｊ");
        ascii2jp.put("K", "Ｋ");
        ascii2jp.put("L", "Ｌ");
        ascii2jp.put("M", "Ｍ");
        ascii2jp.put("N", "Ｎ");
        ascii2jp.put("O", "Ｏ");
        ascii2jp.put("P", "Ｐ");
        ascii2jp.put("Q", "Ｑ");
        ascii2jp.put("R", "Ｒ");
        ascii2jp.put("S", "Ｓ");
        ascii2jp.put("T", "Ｔ");
        ascii2jp.put("U", "Ｕ");
        ascii2jp.put("V", "Ｖ");
        ascii2jp.put("W", "Ｗ");
        ascii2jp.put("X", "Ｘ");
        ascii2jp.put("Y", "Ｙ");
        ascii2jp.put("Z", "Ｚ");
        /*
        !"#$%&'()*+,-./0123456789:;<=>?
        @ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_
        `abcdefghijklmnopqrstuvwxyz{|}~
        */
        ascii2jp.put("[", "「");
        ascii2jp.put("\\", "＼");
        ascii2jp.put("]", "」");
        //char2jis.put("^", "");
        ascii2jp.put("_", "＿");
        //char2jis.put("`", "`");
        
        /*
        82 5f |    Ａ Ｂ Ｃ Ｄ Ｅ Ｆ Ｇ Ｈ Ｉ Ｊ Ｋ Ｌ Ｍ Ｎ Ｏ 
        82 6f | Ｐ Ｑ Ｒ Ｓ Ｔ Ｕ Ｖ Ｗ Ｘ Ｙ Ｚ                
        82 80 |    ａ ｂ ｃ ｄ ｅ ｆ ｇ ｈ ｉ ｊ ｋ ｌ ｍ ｎ ｏ 
        82 90 | ｐ ｑ ｒ ｓ ｔ ｕ ｖ ｗ ｘ ｙ ｚ 
        */
        ascii2jp.put("a", "ａ");
        ascii2jp.put("b", "ｂ");
        ascii2jp.put("c", "ｃ");
        ascii2jp.put("d", "ｄ");
        ascii2jp.put("e", "ｅ");
        ascii2jp.put("f", "ｆ");
        ascii2jp.put("g", "ｇ");
        ascii2jp.put("h", "ｈ");
        ascii2jp.put("i", "ｉ");
        ascii2jp.put("j", "ｊ");
        ascii2jp.put("k", "ｋ");
        ascii2jp.put("l", "ｌ");
        ascii2jp.put("m", "ｍ");
        ascii2jp.put("n", "ｎ");
        ascii2jp.put("o", "ｏ");
        ascii2jp.put("p", "ｐ");
        ascii2jp.put("q", "ｑ");
        ascii2jp.put("r", "ｒ");
        ascii2jp.put("s", "ｓ");
        ascii2jp.put("t", "ｔ");
        ascii2jp.put("u", "ｕ");
        ascii2jp.put("v", "ｖ");
        ascii2jp.put("w", "ｗ");
        ascii2jp.put("x", "ｘ");
        ascii2jp.put("y", "ｙ");
        ascii2jp.put("z", "ｚ");
        
        ascii2jp.put("|", "｜");
        ascii2jp.put("~", "〜");
        
        ascii2jp.put(" ", "　");
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
    
    public static String getString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < bytes.length; i += 2) {
            Character c = getCharacter(new byte[] { bytes[i], bytes[i+1] });
            if(c == null) {
                sb.append("<null>");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
    
    /**
     * Returns a japanese version of an ascii char, e.g. 'a' becomes 'ａ'.
     * @param asciiChar
     * @return 
     */
    public static char getJapaneseVersion(char asciiChar) {
        String jp = ascii2jp.get("" + asciiChar);
        if(jp == null) {
            throw new RuntimeException("No japanese version found for " + asciiChar);
        }
        return jp.charAt(0);
    }
    
    /**
     * Turns a given ascii text (english) into the corresponding japanese text as {@link HuffmanCharacter}s, e.g. 'a' becomes 'ａ'.
     * @param asciiText normal ascii text, control character are detected with '{'....'}'
     * @return 
     * @throws RuntimeException if character can not be converted
     */
    public static List<HuffmanCharacter> toJapanese(String asciiText) {
        List<HuffmanCharacter> text = new ArrayList<>();
        
        StringBuilder controlBuffer = new StringBuilder();
        boolean inControl = false;
        for(int i = 0; i < asciiText.length(); i++) {
            String charStr = "" + asciiText.charAt(i);
            
            if(charStr.equals("{")) {
                inControl = true;
                
            } else if(charStr.equals("}")) {
                inControl = false;
                //will be parsed correctly
                if(controlBuffer.length() != 4) {
                    throw new RuntimeException("Control character has not 4 letters: " + controlBuffer);
                }
                text.add(new HuffmanCharacter(new HuffmanNode(HuffmanNode.Type.ControlCharacter, Inspector.toBytes(controlBuffer.toString()))));
                controlBuffer.delete(0, controlBuffer.length());
                
            } else if(inControl) {
                controlBuffer.append(charStr);
                
            } else {
                text.add(new HuffmanCharacter(new HuffmanNode(asciiText.charAt(i))));
            }
        }
        
        return text;
    }
    
}
