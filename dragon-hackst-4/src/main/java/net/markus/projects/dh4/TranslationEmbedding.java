
package net.markus.projects.dh4;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.markus.projects.dh4.data.HuffmanChar;
import net.markus.projects.dh4.data.HuffmanCode;
import net.markus.projects.dh4.data.ParseNode;
import net.markus.projects.dh4.data.TextBlock;
import net.markus.projects.dh4.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * 
 */
public class TranslationEmbedding {

    public void embed(File translationFolder, HBD1PS1D hbd, PSEXE psexe) throws IOException {
        
        Map<String, List<TextBlock>> id2tb = hbd.getTextBlockIDMap();
        System.out.println(id2tb.size() + " unique textblocks found");
        
        //these opcodes are used to create the pointer mapping code
        List<OpCode> code = new ArrayList<>();
        
        for(File translationFile : translationFolder.listFiles()) {
            if(!translationFile.getName().endsWith(".csv"))
                continue;
            
            String name = translationFile.getName();
            String hexId = name.substring(0, name.length() - ".csv".length());
            
            if(!id2tb.containsKey(hexId)) {
                throw new RuntimeException("there is no textblock with the id " + hexId + " because of " + translationFile.getName());
            }
            //we have to update these textblocks
            List<TextBlock> textblocks = id2tb.get(hexId);
            
            CSVParser parser = CSVParser.parse(translationFile, StandardCharsets.UTF_8, CSVFormat.DEFAULT);
            List<CSVRecord> records = parser.getRecords();
            parser.close();
            
            System.out.println(translationFile.getName() + " with " + records.size() + " segments");
            
            //parse translated text
            List<HuffmanCode> segments = new ArrayList<>();
            for(CSVRecord record : records) {
                String text = record.get(0);
                String offsetHex = record.get(2).substring(2);
                
                if(text.trim().isEmpty()) {
                    text = "(text is empty){7f0a}";
                }
                
                //parse will convert ascii to japanese equivalent
                HuffmanCode textAsCode = HuffmanCode.parse(text);
                textAsCode.setByteDiffInHex(offsetHex);
                
                segments.add(textAsCode);
            }
            //sort segments
            segments.sort((a,b) -> a.getByteDiffInHex().compareTo(b.getByteDiffInHex()));
            
            //we merge to fullCode just to collect all chars and create char2freq map
            HuffmanCode fullCode = HuffmanCode.merge(segments);
            Map<String, Integer> char2freq = fullCode.getCharFrequencyMap();
            
            //System.out.println(fullCode.calculateText());
            //System.out.println(char2freq);
            
            ParseNode huffmanTreeRoot = hbd.createHuffmanTree(char2freq);
            byte[] huffmanTreeRootBytes = hbd.toHuffmanTreeBytes(huffmanTreeRoot);
            ParseNode parsedVersion = hbd.parseTree(huffmanTreeRootBytes);
            
            //System.out.println(huffmanTreeRoot.toStringTree());
            System.out.println(parsedVersion.toStringTree());
            
            
            //based on the tree we know what char which bits receive
            Map<String, String> char2bits = hbd.getCharacterToBitsMap(huffmanTreeRoot);
            
            //assign bits to every char in the code of every segment
            for(HuffmanCode segment : segments) {
                for(HuffmanChar ch : segment) {
                    String bits = char2bits.get(ch.getLetter());
                    if(bits == null) {
                        throw new RuntimeException("could not find bits for char " + ch);
                    }
                    ch.setBits(bits);
                }
            }
            
            //calculate the positions so that we know what start position we have to use for the segments
            HuffmanCode.calculateCharacterIndices(segments);
            
            //store hexId in r21
            code.add(new OpCode("ori r21," + hexId));
            //skip the whole section if it is not the hexId
            int skip = segments.size() * 5;
            //String skipHex = Utils.bytesToHex(Utils.intToByteArray(skip)).substring(4).toUpperCase();
            code.add(new OpCode("jne r21,r22," + skip)); //I implemented it with an decimal number (not hex)
            
            //we store the updated diff hex to create our pointer mapping in asm
            for(HuffmanCode segment : segments) {
            
                int firstBitInByteIndex = segment.get(0).getStartBitInByte();
                
                int firstByteIndex = segment.get(0).getByteIndex();
                //add textblock header length, usually 24 (directly after header starts huffman code)
                firstByteIndex += textblocks.get(0).huffmanCodeStart;

                String offsetHex = Utils.bytesToHex(Utils.intToByteArray(firstByteIndex)).toUpperCase();
                if(!offsetHex.startsWith("0000")) {
                    throw new RuntimeException("the offsetHex is greater then a short: " + offsetHex);
                }
                offsetHex = offsetHex.substring(4, 8);

                segment.setByteDiffInHexUpdated(offsetHex);

                segment.setText(segment.calculateText());
                
                //store in r21 the original offset hex
                code.add(new OpCode("ori r21," + segment.getByteDiffInHex()));
                //if this is not the right one skip the rest
                code.add(new OpCode("jne r21,r23,3"));
                
                //if it is the correct one store the bit index in r22
                code.add(new OpCode("lui r22,"+ firstBitInByteIndex +"500")); //load bit index, currently we do not know what the '5' means
                //if it is the correct one store the updated hex diff in r23
                code.add(new OpCode("ori r23," + segment.getByteDiffInHexUpdated()));
                
                //here we have to jump to the conversion at the end (replace it later)
                //we use the comment to find it
                code.add(new OpCode("nop").setComment("replaceLater"));
            }
            
            //merge it to update the byte[] in textblocks
            fullCode = HuffmanCode.merge(segments);
            fullCode.setHuffmanTreeRoot(huffmanTreeRoot);
            
            //just to check that everything is fine
            //seems to work correctly
            String fullCodeBits = fullCode.getBits();
            HuffmanCode decoded = hbd.decode(fullCodeBits, huffmanTreeRoot);
            System.out.println("fullCodeBits decoded: " + decoded.calculateText());
            
            //now we have to actually replace the data
            for(TextBlock tb : textblocks) {
                tb.huffmanTreeBytes = hbd.toHuffmanTreeBytes(huffmanTreeRoot);
                tb.root = huffmanTreeRoot; //is used to count root.descendants()
                tb.huffmanCode = hbd.encode(fullCode);
                
                //System.out.println(Utils.toHexDump(tb.huffmanCode, 12, true, false, null));
                
                //every index is calulated based on the byte arrays
            }
            
            //on patch it will update the subblock bytes
            
            
            //check if correct
            TextBlock tb = textblocks.get(0);
            String allBits = hbd.getBits(tb.huffmanCode);
            for(HuffmanCode segment : segments) {
                
                System.out.println(Utils.toHexDump(tb.huffmanCode, 16, true, false, null).toUpperCase());
                System.out.println(Utils.toHexDump(tb.huffmanTreeBytes, 16, true, false, null).toUpperCase());
                
                System.out.println("expected: " + segment.calculateText());
                System.out.println("original hex diff: " + segment.getByteDiffInHex());
                System.out.println("updated hex diff: " + segment.getByteDiffInHexUpdated());
                int firstBitInByteIndex = segment.get(0).getStartBitInByte();
                System.out.println("firstBitInByteIndex: " + firstBitInByteIndex);
                
                //textblocks.get(0).huffmanCodeStart
                int offset = Utils.bytesToInt(Utils.hexStringToByteArray("0000" + segment.getByteDiffInHexUpdated()));
                //because we calulated from there
                offset -= tb.huffmanCodeStart;
                System.out.println("updated dec diff (-huffmanCodeStart): " + offset);
                
                System.out.println("the start byte is: " + Utils.bytesToHex(new byte[] { tb.huffmanCode[offset] }).toUpperCase());
                System.out.println("the start byte bits are: " + Utils.toBits(new byte[] { tb.huffmanCode[offset] }));
                System.out.println("the start byte +1 bits are: " + Utils.toBits(new byte[] { tb.huffmanCode[offset+1] }));
                System.out.println("the start byte bits start here: " + Utils.toBits(new byte[] { tb.huffmanCode[offset] }).substring(firstBitInByteIndex));
                
                
                
                int bitOffset = (offset * 8) + firstBitInByteIndex;
                System.out.println("bitOffset: " + bitOffset);
                
                String segmentBits = allBits.substring(bitOffset);
                System.out.println("fullCod    : " + fullCodeBits);
                System.out.println("allBits    : " + allBits);
                System.out.println("segmentBits: " + segmentBits);
                
                HuffmanCode actualCode = hbd.decode(segmentBits, huffmanTreeRoot);
                System.out.println("decoded segmentBits: " + actualCode.calculateText());
                        
                System.out.println();
                System.out.println();
            }
            
        }//for each translation file
        
        
        
        
        
        //now we have to patch the psxexe to map the pointers correctly
        psexe.patch("8001D4CC", "8008EBA4", code);
        
    }
    
}
