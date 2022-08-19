
package net.markus.projects.dq4h.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import static net.markus.projects.dq4h.io.Inspector.toASCII;
import static net.markus.projects.dq4h.io.Inspector.toHex;
import org.apache.commons.io.FileUtils;
import org.crosswire.common.compress.LZSS;

/**
 * To track what was actually changed in the data.
 * Here we can also visualize all the changes to see what happend.
 */
public class ChangeLogEntry {

    private HeartBeatDataFile file;
    
    private byte[] originalBytes;
    private byte[] changedBytes;

    public ChangeLogEntry() {
    }

    public ChangeLogEntry(HeartBeatDataFile file, byte[] originalBytes, byte[] changedBytes) {
        this.file = file;
        this.originalBytes = originalBytes;
        this.changedBytes = changedBytes;
    }
    
    /**
     * The file which was changed.
     * @return 
     */
    public HeartBeatDataFile getFile() {
        return file;
    }

    public void setFile(HeartBeatDataFile file) {
        this.file = file;
    }

    public byte[] getOriginalBytes() {
        return originalBytes;
    }

    public void setOriginalBytes(byte[] originalBytes) {
        this.originalBytes = originalBytes;
    }

    public byte[] getChangedBytes() {
        return changedBytes;
    }

    public void setChangedBytes(byte[] changedBytes) {
        this.changedBytes = changedBytes;
    }
    
    public String toStringHexCompare() {
        return Inspector.splitScreen(Inspector.toHexDump(originalBytes, 25), 100, Inspector.toHexDump(changedBytes, 25));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChangeLogEntry{");
        sb.append("file=").append(file);
        sb.append(", originalBytes=(").append(originalBytes.length).append(" bytes)");
        sb.append(", changedBytes=(").append(changedBytes.length).append(" bytes)");
        sb.append('}');
        return sb.toString();
    }
    
    public void saveHtmlReport(File htmlFile) throws IOException {
        StringBuilder html = new StringBuilder();
        
        int w = 24;
        
        byte[] a = originalBytes;
        byte[] b = changedBytes;
        
        int len = 1;
        
        //this works because uncompressed should be the same 
        //because we will not change the original uncompressed byte array length
        if(file.isCompressed()) {
            len = 2;
        }
        
        html.append("<html>");
        html.append("    <head>");
        html.append("        <title>");
        html.append("            ").append(file.getPath()).append(" - DQ4 Comparison");
        html.append("        </title>");
        html.append("    </head>");
        html.append("    <body style=\"font-family: arial;\">");
        html.append("        <style>mark { background-color: lightblue }</style>");
        html.append("        <h2>").append(file.getPath()).append("</h2>");
        html.append("        <pre>").append(file.toString()).append("</pre>");
        
        for(int i = 0; i < len; i++) {
            if(i == 1) {
                html.append("<br/>");
                html.append("<h4>Uncompressed</h4>");
                a = LZSS.uncompress(new ByteArrayInputStream(a), file.getOriginalSizeUncompressed());
                b = LZSS.uncompress(new ByteArrayInputStream(b), file.getOriginalSizeUncompressed());
                //a = LZSS.uncompress(new ByteArrayInputStream(a));
                //b = LZSS.uncompress(new ByteArrayInputStream(b));
            }
            html.append("    <table style=\"width: 100%; margin: 5px; padding: 5px;\">");
            html.append("        <tr>");
            html.append("        <td>");
            html.append("            <h3>Original</h3>");
            html.append("            <pre>");
            html.append(toHexDump(a, b, w, true, false));
            html.append("            </pre>");
            html.append("        </td>");
            html.append("        <td>");
            html.append("            <h3>Changed</h3>");
            html.append("            <pre>");
            html.append(toHexDump(b, a, w, true, false));
            html.append("            </pre>");
            html.append("        </td>");
            html.append("        </tr>");
            html.append("    </table>");
        }
        html.append("    </body>");
        html.append("</html>");
        
        String htmlText = html.toString();
        FileUtils.writeStringToFile(htmlFile, htmlText, StandardCharsets.UTF_8);
    }
    
    private String toHexDump(byte[] original, byte[] changed, int w, boolean address, boolean ascii) {
        
        int h = (original.length / w) + 1;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < h; i++) {
            byte[] line = Arrays.copyOfRange(original, i * w, Math.min(i * w + w, original.length));
            
            byte[] otherLine;
            if(i * w < changed.length) {
                otherLine = Arrays.copyOfRange(changed, i * w, Math.min(i * w + w, changed.length));
            } else {
                otherLine = new byte[0];
            }
            
            if (address) {
                byte[] addr = Converter.shortToBytesBE((short) (i * w));
                String addrStr = "0x" + toHex(new byte[]{addr[0]}) + toHex(new byte[]{addr[1]});
                sb.append(addrStr).append(" | ");
            }

            sb.append(toHexString(line, otherLine));

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
    
    private String toHexString(byte[] bytes, byte[] other, int... cut) {
        if (bytes == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        int j = 0;
        for (int k = 0; k < bytes.length; k++) {
            
            byte b = bytes[k];
            
            if (j < cut.length && i == cut[j]) {
                j++;
                i = 0;
                sb.append(" | ");
            } else {
                sb.append(" ");
            }

            boolean different = false;
            if(k < other.length) {
                byte otherB = other[k];
                different = b != otherB;
                if(different) {
                    sb.append("<mark>");
                }
            }
            
            sb.append(toHex(new byte[]{b}));
            
            if(different) {
                sb.append("</mark>");
            }
            
            i++;
        }
        return sb.toString().trim();
    }
    
    public String getFilename() {
        return file.getPath().replace("/", "-") + "-type" + file.getOriginalType();
    }
}
