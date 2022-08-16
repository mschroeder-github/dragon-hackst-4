
package net.markus.projects.dq4h.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        
        byte[] treeBytes;
        byte[] textBytes;
        short numberOfNodes;
        
        //when we patch it
        //we have to calculate tree and text
        if(textContent.isPerformPatch()) {

            //1. count how often a character occurs in the textContent.getText()
            //we need a set of nodes where frequency is set
            Set<HuffmanNode> nodes = toUniqueNodesWithFrequencies(textContent.getText());

            //2. get the tree from nodes with frequency 
            HuffmanNode tree = createHuffmanTree(nodes);
            //System.out.println(tree.toStringTree());

            //TODO we maybe have to make the tree artificially bigger to have more bytes
            
            //3. encode tree and text with tree
            treeBytes = toHuffmanTreeBytes(tree);
            textBytes = encodeText(textContent.getText(), tree);
            
            //just the nodes
            //root has the highest number
            //0 ... 5 = 6 nodes, thus + 1
            numberOfNodes = (short) (tree.getID() + 1);
            
        } else {
            
            //the original content
            treeBytes = textContent.getOriginalTreeBytes();
            textBytes = textContent.getOriginalTextBytes();
            numberOfNodes = textContent.getOriginalHuffmanTreeNumberOfNodes();
        }
        
        //4. write it correctly
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
        
        if(textContent.isPerformPatch()) {
            dqos.writeIntLE(textContent.getDialogPointers().size());
            for(VariableToDialogPointer kvp : textContent.getDialogPointers()) {
                dqos.writeBytesLE(kvp.getVariable());
                dqos.writeBytesLE(kvp.getValue());
            }
            
        } else {
            dqos.writeIntLE(textContent.getOriginalDialogPointers().size());
            for(VariableToDialogPointer kvp : textContent.getOriginalDialogPointers()) {
                dqos.writeBytesLE(kvp.getVariable());
                dqos.writeBytesLE(kvp.getValue());
            }
        }
        
    }
    
    //tree =========================
    
    private Set<HuffmanNode> toUniqueNodesWithFrequencies(List<HuffmanCharacter> text) {
        Map<HuffmanNode, HuffmanNode> nodeMap = new HashMap<>();
        for(HuffmanCharacter c : text) {
            HuffmanNode n = nodeMap.get(c.getNode());
            if(n == null) {
                n = c.getNode();
                nodeMap.put(n, n);
                n.setFrequency(1);
            } else {
                n.setFrequency(n.getFrequency() + 1);
            }
        }
        return nodeMap.keySet();
    }
    
    /**
     * From a set of nodes with their frequencies this method builds a huffman tree.
     * @param nodesWithFrequencies
     * @return 
     */
    private HuffmanNode createHuffmanTree(Set<HuffmanNode> nodesWithFrequencies) {
        
        //to list to sort them by frequency
        List<HuffmanNode> list = new ArrayList<>(nodesWithFrequencies);
        
        //parsed trees have a 'HuffmanNode{type=Branch, content=0}' so start at 0
        //highest id has the root
        short nodeID = 0;
        
        while(list.size() > 1) {
            
            //lowest frequency first
            //if frequency is equal, maybe the original tree used the content has sorting
            list.sort((a,b) -> { 
                int cmp = Double.compare(a.getFrequency(), b.getFrequency());
                if(cmp == 0) {
                    //unclear what original algo did
                    return a.getContentHex().compareToIgnoreCase(b.getContentHex());
                }
                return cmp;
            });
            
            //remove from list the two lowerst
            HuffmanNode a = list.remove(0);
            HuffmanNode b = list.remove(0);
            
            //create a parent node for them
            HuffmanNode node = new HuffmanNode(nodeID++);
            //use first a then b, so the branch ids are correct
            node.getChildren().add(a);
            node.getChildren().add(b);
            //accumulate frequency: has to be sum
            node.setFrequency(a.getFrequency() + b.getFrequency());
            
            //add to list again
            list.add(node);
        }
        
        return list.get(0);
    }
    
    private byte[] toHuffmanTreeBytes(HuffmanNode root) {
        List<HuffmanNode> nodes = root.descendants();
        
        int arraySize = nodes.size() * 2 + 2; //+2 because 0000
        byte[] data = new byte[arraySize];
        
        toHuffmanTreeBytesRecursiveStep(root, data);
        
        return data;
    }
    
    private void toHuffmanTreeBytesRecursiveStep(HuffmanNode node, byte[] data) {
        
        int offsetA = 0;
        //to get the second tree part
        int offsetB = (int) ((data.length + 2) / 2) - 2;
        
        int number = node.getID();
        
        //get children returns first left, then right
        for(int i = 0; i < node.getChildren().size(); i++) {
            
            HuffmanNode child = node.getChildren().get(i);
            boolean isLeft = i == 0;
            
            //based on which side use right offset for index calculation
            int index = (isLeft ? offsetB : offsetA) + number * 2;
            
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
            
            //swap and second byte - 0x80 to make it a character
            data[0] = node.getContent()[1];
            data[1] = (byte) (node.getContent()[0] - 0x80);
            
        } else if(node.isControlCharacter()) {
            
            //just reverse it
            data = Converter.reverse(node.getContent());
            
        }
        
        return data;
    }

    
    //text =========================
    
    private byte[] encodeText(List<HuffmanCharacter> text, HuffmanNode root) throws IOException {
        
        Map<HuffmanNode, String> char2str = getCharacterToBitsMap(root);
        
        StringBuilder bitsSB = new StringBuilder();
        for(HuffmanCharacter hc : text) {
            //this works because HuffmanNode has an implemented equals method
            String bits = char2str.get(hc.getNode());
            
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
    
    /**
     * Based on the huffman tree create for each (control) character node the corresponding bit code.
     * @param huffmanTreeRoot
     * @return 
     */
    private Map<HuffmanNode, String> getCharacterToBitsMap(HuffmanNode huffmanTreeRoot) {
        Map<HuffmanNode, String> map = new HashMap<>();
        
        getCharacterToBitsMapRecursive("0", huffmanTreeRoot.getLeftChild(), map);
        getCharacterToBitsMapRecursive("1", huffmanTreeRoot.getRightChild(), map);
        
        return map;
    }
    
    private void getCharacterToBitsMapRecursive(String bits, HuffmanNode parent, Map<HuffmanNode, String> map) {
        if(parent.isLeaf()) {
            map.put(parent, bits);
            
        } else if(parent.isBranch()) {
            getCharacterToBitsMapRecursive(bits + "0", parent.getLeftChild(), map);
            getCharacterToBitsMapRecursive(bits + "1", parent.getRightChild(), map);
        }
    }

}
