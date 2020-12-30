package net.markus.projects.dh4;

import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.markus.projects.dh4.util.Utils;
import org.apache.commons.io.IOUtils;

/**
 * Japanese Reader.
 */
public class PsxJisReader {

    public static final Map<String, String> ascii2jp;
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
        ascii2jp.put("'", "’");
        ascii2jp.put("(", "（");
        ascii2jp.put(")", "）");
        ascii2jp.put("*", "＊");
        ascii2jp.put("+", "＋");
        ascii2jp.put(",", "、");
        ascii2jp.put("-", "ー");
        ascii2jp.put(".", "。");
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
    
    
    //https://en.wikipedia.org/wiki/JIS_X_0208
    
    public Map<Short, Character> sjishort2char;
    public Map<Character, Byte[]> char2sjis;
    
    //deprecated: does not work with byte[] key, use sjishort2char
    @Deprecated
    public Map<Byte[], Character> sjis2char;
    

    public PsxJisReader() {
        
    }

    /*
    //was for searching japanese
    @Deprecated
    public void read(Consumer<Short> consumer, Consumer<Short> consumer2) throws IOException {
        long l = file.length();

        System.out.println(l + " bytes");

        FileInputStream fis = new FileInputStream(file);

        long i = 0;
        long begin = System.currentTimeMillis();

        byte[] buffer = new byte[(int) Math.pow(2, 14)];
        System.out.println(buffer.length + " buffer size");

        int b = 0;
        while (b != -1) {
            b = fis.read(buffer);

            
            i += buffer.length;
            
            for (int j = 0; j + 2 < buffer.length; j += 2) {

                byte[] win = new byte[]{buffer[j], buffer[j + 1]};
                short winShort = Utils.bytesToShort(win);

                byte[] win2 = new byte[]{buffer[j + 1], buffer[j + 2]};
                short win2Short = Utils.bytesToShort(win2);

                consumer.accept(winShort);
                consumer2.accept(win2Short);

                //Character c = sjishort2char.get(winShort);
                //if (c != null) {
                //    System.out.println(c);
                //}

                //if(j+2 < buffer.length) {
                //    Byte[] win1 = new Byte[] { buffer[j+1], buffer[j+2] };
                //  
                //    Character c = sjis2char.get(win1);
                //    if(c != null) {
                //        System.out.println(c);
                //    }
                //}
            }

            if(i % 10000 == 0) {
                System.out.println(i + "/" + file.length() + ", " + String.format("%.00f%%", (i / (float) l)*100));
            }
        }

        fis.close();

        long end = System.currentTimeMillis();

        System.out.println((end - begin) + " ms");
    }   
*/

    public void readTable() throws IOException {
        String content = IOUtils.toString(PsxJisReader.class.getResourceAsStream("/kanji_codes.sjis.txt"), UTF_8);
        //System.out.println(content);

        sjis2char = new HashMap<>();
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

            //System.out.println(start);
            start = start.trim().replace(" ", "");

            byte[] b = Utils.hexStringToByteArray(start);
            short s = Utils.bytesToShort(b);

            //System.out.println(rest);
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
                char2sjis.put(c, Utils.toObjects(Utils.shortToBytes(next)));
                sjishort2char.put(next, c);
            }

            //System.out.println(start + " " + Arrays.toString(b) + " " + bytesToHex(b) + ", after add one " + bytesToHex(next));
        }

        System.out.println("sjis2char " + sjis2char.size() + ", char2sjis " + char2sjis.size());

        //"
        sjishort2char.put(Utils.bytesToShort(Utils.hexStringToByteArray("ff02")), '"');
        sjishort2char.put(Utils.bytesToShort(Utils.hexStringToByteArray("ff0a")), '*');
        sjishort2char.put(Utils.bytesToShort(Utils.hexStringToByteArray("ff11")), '1');//FULLWIDTH DIGIT ONE
        
        for (Entry e : char2sjis.entrySet()) {
            Character c = (Character) e.getKey();

            //System.out.println(c.charValue() + " => " + e.getValue());
        }

        //System.out.println("あ => " + char2sjis.get('あ'));
    }

    
    /*
    public static void main(String[] args) throws IOException {
        PsxJisReader r = new PsxJisReader();
        r.readTable();

        //short[] array = r.toShortArray("ぼうけんをする");
        short[] array = Utils.toShortArray("ある", r.char2sjis);
        System.out.println(Arrays.toString(array));

        LimitedQueue<Short> l = new LimitedQueue<>(array.length);
        
        int hashCode = Arrays.hashCode(array);
        
        r.read(s -> {
            l.add(s);
            
            if(l.size() == array.length) {
                if(hashCode == Arrays.hashCode(l.toArray())) {
                    int a = 0;
                }
            }
        }, s -> {});

        //r.read();
        //http://www.rikai.com/library/kanjitables/kanji_codes.sjis.shtml
    }
    */

}
