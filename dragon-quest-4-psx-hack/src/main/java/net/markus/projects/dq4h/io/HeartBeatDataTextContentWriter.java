
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import net.markus.projects.dq4h.data.HuffmanCharacter;
import net.markus.projects.dq4h.data.HuffmanNode;
import net.markus.projects.dq4h.data.VariableToDialogPointer;

/**
 * Writes {@link HeartBeatDataTextContent}.
 */
public class HeartBeatDataTextContentWriter extends DragonQuestWriter<HeartBeatDataTextContent>{

    @Override
    public void write(HeartBeatDataTextContent textContent, OutputStream output) throws IOException {
        
        //encode tree and text with tree
        byte[] treeBytes = toHuffmanTreeBytes(textContent.getTree());
        //System.out.println(Inspector.toHexDump(treeBytes, 8));
        String writtenTreeStr = textContent.getTree().toStringTree();
        
        //better safe than sorry
        HeartBeatDataTextContentReader reader = new HeartBeatDataTextContentReader();
        HuffmanNode parsedTree = reader.parseTree(treeBytes);
        String parsedTreeStr = parsedTree.toStringTree();
        if(!writtenTreeStr.equals(parsedTreeStr)) {
            throw new IOException("The written tree is not the parsed tree for " + textContent.getParent());
        }
        
        //encodeText uses the originalBits information in the HuffmanCharactersg
        byte[] textBytes = encodeText(textContent.getText());
        
        //textContent.getText().forEach(c -> System.out.println(c));
        
        //this block was for checking the size, but now it can be bigger than orginal
        //maybe we just extend the text bytes by zeros until file size is reached
        /*
        int treeBytesDiff = textContent.getOriginalTreeBytes().length - treeBytes.length;
        int textBytesDiff = textContent.getOriginalTextBytes().length - textBytes.length;
        
        if(treeBytesDiff < 0) {
            //throw new IOException(textContent.getIdHex() + " number of byte for tree is larger than original: " + 
            //        textContent.getOriginalTreeBytes().length + " < " + treeBytes.length + "\n" +
            //        textContent.getOriginalTree().toStringTree()
            //);
            
            //cannot get smaller than original
            //we created a larger tree, but this is still fine because we have space left in text bytes
            treeBytesDiff = 0;
        }
        
        if(textBytesDiff < 0) {
            //this is not good, our text became larger than the original which will increase the file size
            throw new IOException(textContent.getIdHex() + " number of byte for text is larger than original: " + 
                    textContent.getOriginalTextBytes().length + " < " + textBytes.length + "\n" +
                    "original text is: " + HuffmanCharacter.listToString(textContent.getOriginalText())
            );
        }
        
        
        //this works
        byte[] longerTextBytes = new byte[textBytes.length + treeBytesDiff + textBytesDiff];
        System.arraycopy(textBytes, 0, longerTextBytes, 0, textBytes.length);
        textBytes = longerTextBytes;
        */
        
        //better safe than sorry
        List<HuffmanCharacter> parsedText = reader.decodeText(textBytes, parsedTree);
        String writtenTextStr = HuffmanCharacter.listToString(textContent.getText());
        String parsedTextStr = HuffmanCharacter.listToString(parsedText);
        if(!writtenTextStr.equals(parsedTextStr)) {
            throw new IOException("The written text is not the parsed text for " + textContent.getParent());
        }
        
        //just the nodes
        //root has the highest number
        //0 ... 5 = 6 nodes, root has ID 6 so no +1 necessary
        short numberOfNodes = (short) textContent.getTree().getID(); //(short) (textContent.getTree().getID() + 1);
        
        if(parsedTree.descendantsBranch().size() != numberOfNodes) {
            throw new IOException("numberOfNodes and actual number of nodes in tree differ");
        }
        
        //write it correctly
        int end = 
                4 * 6 + //header
                textContent.getOriginalUnknown2().length + 
                textBytes.length + 
                10 + //header for tree
                treeBytes.length + 
                textContent.getOriginalUnknown3().length;
        
        int huffmanTextStart = 
                4 * 6 + //header
                textContent.getOriginalUnknown2().length;
        
        int huffmanTextEnd = 
                huffmanTextStart + 
                textBytes.length;
        
        int huffmanTreeEnd =
                huffmanTextEnd +
                10 + //tree header
                treeBytes.length;
        
        if(huffmanTreeEnd == end) {
            huffmanTreeEnd = 0;
        }
        
        int huffmanTreeStart = huffmanTextEnd + 10;
        int huffmanTreeMiddle = huffmanTreeStart + (treeBytes.length / 2) - 1;
        
        DragonQuestOutputStream dqos = new DragonQuestOutputStream(output);
        
        dqos.writeIntLE(end);
        dqos.writeBytesLE(textContent.getId());
        dqos.writeIntLE(huffmanTextStart);
        dqos.writeIntLE(huffmanTreeEnd);
        dqos.writeIntLE(huffmanTextEnd);
        dqos.writeIntLE(textContent.getOriginalUnknown1());
        
        dqos.writeBytesBE(textContent.getOriginalUnknown2());
        
        dqos.writeBytesBE(textBytes);
        
        dqos.writeIntLE(huffmanTreeStart);
        dqos.writeIntLE(huffmanTreeMiddle);
        dqos.writeShortLE(numberOfNodes);
        
        dqos.writeBytesBE(treeBytes);
        dqos.writeBytesBE(textContent.getOriginalUnknown3());
        
        dqos.writeIntLE(end);
        
        //the original one can be changed by the above patch because they are referrers
        dqos.writeIntLE(textContent.getOriginalDialogPointers().size());
        for(VariableToDialogPointer kvp : textContent.getOriginalDialogPointers()) {
            dqos.writeBytesLE(kvp.getVariable());
            dqos.writeBytesLE(kvp.getValue());
        }
        
        //fill with zeros until we reach the original file size
        //hopefully this will work and not break the game
        //because file can now grow bigger than original, we do not have to add zeros here anymore
        /*
        int originalFileSize = textContent.getParent().getOriginalContentBytes().length;
        while(dqos.getPosition() < originalFileSize) {
            dqos.write(new byte[] { 0 });
        }
        */
        
    }
    
    //tree =========================
    
    
    private byte[] toHuffmanTreeBytes(HuffmanNode root) {
        List<HuffmanNode> nodes = root.descendants();
        
        //root is not stored
        int arraySize = (nodes.size() * 2) + 2; //because the array ends with 0x0000 (but the root number is not stored)
        byte[] data = new byte[arraySize];
        
        toHuffmanTreeBytesRecursiveStep(root, data);
        
        return data;
    }
    
    private void toHuffmanTreeBytesRecursiveStep(HuffmanNode node, byte[] data) {
        
        int offsetA = 0;
        //to get the second tree part
        int offsetB = (int) ((data.length + 2) / 2) - 2;
        //int offsetB = (int) (data.length / 2);
        
        int number = node.getID();
        
        //get children returns first left, then right
        for(int i = 0; i < node.getChildren().size(); i++) {
            
            HuffmanNode child = node.getChildren().get(i);
            boolean isLeft = i == 0;
            
            //root is 183
            //first child has the lower number, e.g. 181 
            //second child has the higher number, e.g. 182
            
            //second child needs to be at end of array because largest number is expected at end of array
            
            //based on which side use right offset for index calculation
            int index = (isLeft ? offsetA : offsetB) + number * 2;
            
            //System.out.println(child + " at index " + index + ", isLeft: " + isLeft);
            
            byte[] nodeAsBytes = nodeToByteArray(child);
            
            //write
            //for(int j = 0; j < nodeAsBytes.length; j++) {
            //    data[index + j] = nodeAsBytes[j];
            //}
            System.arraycopy(nodeAsBytes, 0, data, index, nodeAsBytes.length);
            
            if(child.isBranch()) {
                toHuffmanTreeBytesRecursiveStep(child, data);
            }
        }
    }
    
    private byte[] nodeToByteArray(HuffmanNode node) {
        byte[] data = new byte[2];
        
        if(node.isBranch()) {
            
            //[0] -74
            //[1] -128 => 0x80 this has to be the second byte
            
            data = Converter.shortToBytesLE(node.getID());
            data[1] = (byte) (data[1] | 0x80);
            
        } else if(node.isCharacter()) {
            
            //0293 => original but swapped value
            //System.out.println(Inspector.toHex(node.getContent()));
            
            //just swap it
            data[0] = node.getContent()[1];
            data[1] = node.getContent()[0];
            
        } else if(node.isControlCharacter()) {
            
            //just reverse it
            data = Converter.reverse(node.getContent());
            
        }
        
        return data;
    }

    
    //text =========================
    
    private byte[] encodeText(List<HuffmanCharacter> text) throws IOException {
        
        //Map<HuffmanNode, String> char2str = getCharacterToBitsMap(root);
        
        StringBuilder bitsSB = new StringBuilder();
        for(HuffmanCharacter hc : text) {
            //this works because HuffmanNode has an implemented equals method
            //String bits = char2str.get(hc.getNode());
            
            String bits = hc.getOriginalBits();
            
            if(bits == null) {
                throw new IOException("could not find bit sequence for " + hc + " at index " + text.indexOf(hc));
            }
            
            bitsSB.append(bits);
        }
        
        String allBits = bitsSB.toString();
        
        int len = (int) Math.ceil(allBits.length() / 8.0);
        
        
        //multiple of 4
        while(len % 4 != 0) {
            len++;
        }
        
        byte[] data = new byte[len];
        
        for(int i = 0; i < allBits.length(); i += 8) {
            String bits = allBits.substring(i, Math.min(allBits.length(), i+8));
            
            while(bits.length() != 8) {
                bits += "0";
            }
            
            bits = Converter.reverse(bits);
            int v = Converter.bitsToIntLE(bits);
            byte b = (byte) v;
            
            data[i / 8] = b;
        }
        
        return data;
    }
    

}
