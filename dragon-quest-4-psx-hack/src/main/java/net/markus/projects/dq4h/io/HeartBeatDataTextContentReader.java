
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import net.markus.projects.dq4h.data.HuffmanCharacter;
import net.markus.projects.dq4h.data.HuffmanNode;
import net.markus.projects.dq4h.data.HuffmanNode.Type;
import net.markus.projects.dq4h.data.VariableToDialogPointer;

/**
 * Reads {@link HeartBeatDataTextContent}.
 */
public class HeartBeatDataTextContentReader extends DragonQuestReader<HeartBeatDataTextContent> {

    @Override
    public HeartBeatDataTextContent read(InputStream input) throws IOException {
        HeartBeatDataTextContent textContent = new HeartBeatDataTextContent();
        
        DragonQuestInputStream dqis = new DragonQuestInputStream(input);
        
        textContent.setOriginalEnd(dqis.readIntLE());
        textContent.setId(dqis.readBytesLE(4));
        textContent.setOriginalHuffmanTextStart(dqis.readIntLE());
        textContent.setOriginalHuffmanTreeEnd(dqis.readIntLE());
        textContent.setOriginalHuffmanTextEnd(dqis.readIntLE());
        textContent.setOriginalUnknown1(dqis.readIntLE());
        
        //sometimes there is some space between the header's end and the text start
        //dataHeaderToHuffmanCode
        int n = textContent.getOriginalHuffmanTextStart() - 24;
        textContent.setOriginalUnknown2(dqis.readBytesBE(n));
        
        int textBytesLen = textContent.getOriginalHuffmanTextEnd() - textContent.getOriginalHuffmanTextStart();
        byte[] textBytes = dqis.readBytesBE(textBytesLen);
        
        textContent.setOriginalTextBytes(textBytes);
        
        //now we should be at the text end position
        if(dqis.getPosition() != textContent.getOriginalHuffmanTextEnd()) {
            throw new IOException("Not at text end position");
        }
        
        //tree header
        textContent.setOriginalHuffmanTreeStart(dqis.readIntLE());
        textContent.setOriginalHuffmanTreeMiddle(dqis.readIntLE());
        textContent.setOriginalHuffmanTreeNumberOfNodes(dqis.readShortLE());
        
        //special case: if zero, there is no extra range to be read
        int correctTreeEnd = textContent.getOriginalHuffmanTreeEnd();
        if(correctTreeEnd == 0) {
            correctTreeEnd = textContent.getOriginalEnd();
        }
        
        int treeBytesLen = correctTreeEnd - textContent.getOriginalHuffmanTreeStart();
        byte[] treeBytes = dqis.readBytesBE(treeBytesLen);
        
        textContent.setOriginalTreeBytes(treeBytes);
        
        //dataDA
        textContent.setOriginalUnknown3(dqis.readBytesBE(textContent.getOriginalEnd() - correctTreeEnd));
        
        if(dqis.getPosition() != textContent.getOriginalEnd()) {
            throw new IOException("Not at end position");
        }
        
        textContent.setOriginalAtEnd(dqis.readIntLE());
        
        HuffmanNode originalTree = parseTree(treeBytes);
        
        //get the characters
        List<HuffmanCharacter> originalText = decodeText(textBytes, originalTree);
        
        textContent.setOriginalTree(originalTree);
        textContent.setOriginalText(originalText);
        
        //key values pairs containing in value dialog pointers
        textContent.setOriginalNumberOfPointers(dqis.readIntLE());
        for(int i = 0; i < textContent.getOriginalNumberOfPointers(); i++) {
            VariableToDialogPointer vdp = new VariableToDialogPointer();
            vdp.setVariable(dqis.readBytesLE(4));
            vdp.setValue(dqis.readBytesLE(4));
            textContent.getDialogPointers().add(vdp);
            
            //a copy to keep the original
            VariableToDialogPointer vdpCopy = new VariableToDialogPointer();
            vdpCopy.setVariable(vdp.getVariable());
            vdpCopy.setValue(vdp.getValue());
            textContent.getOriginalDialogPointers().add(vdp);
        }
        
        //short inspection
        //System.out.println(textContent);
        //System.out.println(textContent.getTreeAsString());
        //System.out.println(textContent.getTextAsString());
        //System.out.println(textContent.getDialogPointersAsString());
        //textContent.getText().forEach(c -> System.out.println(c));
                
        //they always point to the correct text id
        for(VariableToDialogPointer vdp : textContent.getDialogPointers()) {
            if(!Arrays.equals(vdp.getTextId(), textContent.getId2Bytes())) {
                throw new IOException("Dialog pointer points to another text id then its own: " + vdp + " " + textContent);
            }
        }
        
        return textContent;
    }

    private HuffmanNode parseTree(byte[] huffmanTreeBytes) {
        
        int lastNodeIndex = huffmanTreeBytes.length - 4;
        byte[] lastNode2Bytes = Arrays.copyOfRange(huffmanTreeBytes, lastNodeIndex, lastNodeIndex + 2);
        short lastNode = Converter.bytesToShortBE(new byte[]{lastNode2Bytes[1], lastNode2Bytes[0]});
        short lastNodeNumber = (short) (lastNode - 0x8000);

        short rootNumber = (short) (lastNodeNumber + 1);
        HuffmanNode root = new HuffmanNode(rootNumber);
        
        parseTree(false, rootNumber, root, huffmanTreeBytes);
        parseTree(true, rootNumber, root, huffmanTreeBytes);
        
        return root;
    }
    
    private void parseTree(boolean bit, int curNumber, HuffmanNode parent, byte[] huffmanTreeBytes) {
        
        int offsetA = 0;
        //to get the second tree part
        int offsetB = (int) ((huffmanTreeBytes.length + 2) / 2) - 2;
        
        int offset = 0;
        //bit decide what offset is used
        if(!bit) {
           offset = offsetA;
        } else {
           offset = offsetB;
        }
        
        int index = offset + curNumber * 2;
        
        byte[] nodeBytes = Arrays.copyOfRange(huffmanTreeBytes, index, index + 2);
        byte[] swap = new byte[]{nodeBytes[1], nodeBytes[0]};
        String hex = Inspector.toHex(swap);

        if (hex.startsWith("8")) {

            int number = Converter.bytesToIntBE(new byte[]{0, 0, nodeBytes[1], nodeBytes[0]}) - 0x8000;
            //int number2 = Converter.bytesToIntBE(new byte[]{0, 0, swap[0], swap[1]}) - 0x8000;
            
            //String rep = hex + " [" + number + "] @" + index;
            HuffmanNode branch = new HuffmanNode(Type.Branch, swap);
            parent.getChildren().add(branch);
            
            //recursion
            parseTree(false, number, branch, huffmanTreeBytes);
            parseTree(true, number, branch, huffmanTreeBytes);

        } else if (hex.startsWith("7") || hex.equals("0000")) { 

            HuffmanNode control = new HuffmanNode(Type.ControlCharacter, swap);
            parent.getChildren().add(control);

        } else {
            
            HuffmanNode character = new HuffmanNode(Type.Character, swap);
            parent.getChildren().add(character);
            
        }
    }

    /**
     * We have to get the bits per byte and reverse them.
     * @param textBytes
     * @return 
     */
    private String getBits(byte[] textBytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : textBytes) {
            //for each byte take the bits
            String bits = Converter.byteToBits(b);
            //reverse it
            bits = Converter.reverse(bits);
            //add it to long bit sequence
            sb.append(bits);
        }
        return sb.toString();
    }
    
    private List<HuffmanCharacter> decodeText(byte[] textBytes, HuffmanNode huffmanTreeRoot) {
        List<HuffmanCharacter> text = new ArrayList();
        
        StringBuilder sb = new StringBuilder();
        
        HuffmanNode node = huffmanTreeRoot;
        StringBuilder bitBuffer = new StringBuilder();
        
        int startBit = 0;
        
        String bits = getBits(textBytes);
        
        for(int i = 0; i < bits.length(); i++) {
            
            char bit = bits.charAt(i);
            
            if(bit == '0') {
                node = node.getLeftChild();
            } else {
                node = node.getRightChild();
            }
            
            bitBuffer.append(bit);
            
            HuffmanCharacter huffmanChar = null;
            
            if(node.isLeaf()) {
                huffmanChar = new HuffmanCharacter(node);
            }
            
            //if found
            if(huffmanChar != null) {
                huffmanChar.setOriginalBits(bitBuffer.toString());
                huffmanChar.setOriginalBitPosition(startBit);
                //huffmanChar.setEndBit(i);
                //huffmanChar.setByteIndex(startBit / 8);
                //huffmanChar.setStartBitInByte(startBit % 8);
                
                text.add(huffmanChar);
                
                node = huffmanTreeRoot;
                bitBuffer.delete(0, bitBuffer.length());
                startBit = i + 1;
            }
        }
        
        //fixing the end because there are many 0000000* bits which are wrongly turned into characters
        //decode text goes backward and searches for the last (not {0000}) {0000} pair
        int i;
        for(i = text.size()-1; i > 0; i--) {
            HuffmanCharacter n = text.get(i);
            HuffmanCharacter n_1 = text.get(i-1);
            
            if(!n_1.getNode().isNullCharacter() && n.getNode().isNullCharacter()) {
                break;
            }
        }
        
        text = text.subList(0, i+1);
        
        return text;
    }
    
}
