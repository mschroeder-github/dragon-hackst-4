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
