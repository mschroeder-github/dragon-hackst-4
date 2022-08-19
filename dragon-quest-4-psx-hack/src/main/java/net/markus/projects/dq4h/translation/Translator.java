package net.markus.projects.dq4h.translation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatDataFileContent;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import net.markus.projects.dq4h.data.HuffmanCharacter;
import net.markus.projects.dq4h.data.HuffmanCharacterReferrer;
import net.markus.projects.dq4h.data.HuffmanNode;
import net.markus.projects.dq4h.data.ScriptStoreEntry;
import net.markus.projects.dq4h.data.VariableToDialogPointer;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileReader;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileWriter;
import net.markus.projects.dq4h.io.HeartBeatDataTextContentWriter;
import net.markus.projects.dq4h.io.IOConfig;
import net.markus.projects.dq4h.io.ShiftJIS;
import net.markus.projects.dq4h.util.MemoryUtility;

/**
 * The main class doing the translation or patching.
 */
public class Translator {

    private File inputFile;
    private File outputFile;

    private DragonQuestBinary binary;

    public static void main(String[] args) throws IOException {
        File folder = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/");
        File inputFile = new File(folder, "dq4.bin");
        File outputFile = new File(folder, "dq4-patched.bin");

        Translator translator = new Translator(inputFile, outputFile);

        translator.importData();

        translator.selectiveTranslation("006c");

        translator.exportData();
    }

    public Translator() {
    }
    
    public Translator(File inputFile, File outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public void setBinary(DragonQuestBinary binary) {
        this.binary = binary;
    }

    public DragonQuestBinary getBinary() {
        return binary;
    }
    
    public void importData() throws IOException {
        IOConfig config = new IOConfig();

        //import =============================
        System.out.println("Importing " + inputFile);

        long begin = System.currentTimeMillis();

        DragonQuestBinaryFileReader binReader = new DragonQuestBinaryFileReader(config);
        binary = binReader.read(inputFile);

        long end = System.currentTimeMillis();

        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics() + ", " + binary);
    }

    public void exportData() throws IOException {
        IOConfig config = new IOConfig();

        System.out.println("Exporting to " + outputFile);

        long begin = System.currentTimeMillis();

        DragonQuestBinaryFileWriter binWriter = new DragonQuestBinaryFileWriter(config);
        binWriter.patch(
                binary,
                inputFile,
                outputFile
        );

        long end = System.currentTimeMillis();

        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics());

        if (inputFile.length() != outputFile.length()) {
            throw new RuntimeException("file length differ");
        }

        config.getChangeLogEntries().forEach(e -> System.out.println(e));
        config.getChangeLogEntries().forEach(e -> {
            try {
                e.saveHtmlReport(new File("../../" + e.getFilename() + ".html"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * We should only translate selected text contents.
     *
     * @param textId a two byte hex value, e.g. "006c"
     */
    public void selectiveTranslation(String textId) {

        List<HeartBeatDataTextContent> textContents = new ArrayList<>();
        for (HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {
            textContents.addAll(folder.getTextContentById(textId));
        }

        HeartBeatDataTextContentWriter w = new HeartBeatDataTextContentWriter();

        for (HeartBeatDataTextContent textContent : textContents) {

            //every start character is a sequence from a this character to {0000}
            //for each sequence we would like to define another sequence
            //we expect that each original sequence is referred by a pointer which is stored in the original first character
            //we have to change this pointer according to the new bit offset
            //we have two sequences
            //1. original abcdef{0000}abcdef{0000}abcdef{0000}abcdef{0000}
            //2. translated efg{0000}hijk{0000}lmbn{0000}abc{0000}
            //the starts need to be associated correctly
            List<HuffmanCharacter> originalStarts = textContent.getOriginalStartCharacters();

            //build artifical one
            //TODO this should be the real text
            String asciiStr = "";
            for (int i = 0; i < originalStarts.size(); i++) {
                asciiStr += "test{7f0a}{0000}";
            }

            List<HuffmanCharacter> replacer = ShiftJIS.toJapanese(asciiStr);
            List<HuffmanCharacter> replacerStarts = HeartBeatDataTextContent.getStartCharacters(replacer);

            //need to be same number of starts
            if (originalStarts.size() != replacerStarts.size()) {
                throw new RuntimeException("different number of sequences");
            }

            //copy the referrers
            for (int i = 0; i < originalStarts.size(); i++) {
                replacerStarts.get(i).setReferrers(originalStarts.get(i).getReferrers());
            }

            //from text the tree will be calculated in textcontentwriter
            textContent.setText(replacer);

            //activate that this content will be patched in the file
            textContent.setPerformPatch(true);

            //1. count how often a character occurs in the textContent.getText()
            //we need a set of nodes where frequency is set
            Set<HuffmanNode> nodes = toUniqueNodesWithFrequencies(textContent.getText());

            //2. get the tree from nodes with frequency 
            HuffmanNode tree = createHuffmanTree(nodes);
            textContent.setTree(tree);
            //System.out.println(tree.toStringTree());
            
            //3. the getText characters need bit codes and new bit positions
            updateBitInformation(textContent, tree);
            //after that we know the new bit positions and have to update all the referrers
            
            //4. update the bit postition in the referrers
            updateReferrers(textContent);
        }
    }
    
    /**
     * This method updates the {@link HuffmanCharacter#getOriginalBits() } and 
     * {@link HuffmanCharacter#getOriginalBitPosition() } information based on
     * {@link #getCharacterToBitsMap(net.markus.projects.dq4h.data.HuffmanNode) }.
     * @param textContent
     * @param tree the tree gives us the bit code information for each leaf
     */
    private void updateBitInformation(HeartBeatDataTextContent textContent, HuffmanNode tree) {
        Map<HuffmanNode, String> node2bits = getCharacterToBitsMap(tree);
        int bitPosition = 0;
        for (HuffmanCharacter c : textContent.getText()) {
            String bits = node2bits.get(c.getNode());
            if (bits == null) {
                throw new RuntimeException("No bit code found for " + c);
            }
            c.setOriginalBits(bits);
            c.setOriginalBitPosition(bitPosition);
            bitPosition += bits.length();
        }
    }
    
    /**
     * This method updates the bit positions for the referrers.
     * It updates {@link ScriptStoreEntry}s and {@link VariableToDialogPointer}s.
     * If referrer is changed it also activates {@link HeartBeatDataFileContent#isPerformPatch() }.
     * @param textContent 
     */
    private void updateReferrers(HeartBeatDataTextContent textContent) {
        //4. update referrers: we update the original one
        for (HuffmanCharacter c : textContent.getText()) {
            for (HuffmanCharacterReferrer referrer : c.getReferrers()) {

                //there are two possibilities
                if (referrer instanceof ScriptStoreEntry) {
                    ScriptStoreEntry store = (ScriptStoreEntry) referrer;

                    //String storeBefore = store.toString();
                    store.updatesBitOffsetAsInt(textContent.getTextBitOffset() + c.getOriginalBitPosition());
                    //String storeAfter = store.toString();

                    //System.out.println(storeBefore + " -> " + storeAfter);
                    
                    //because one command changed we have to patch the whole file
                    store.getParent().setPerformPatch(true);

                } else if (referrer instanceof VariableToDialogPointer) {
                    VariableToDialogPointer vdp = (VariableToDialogPointer) referrer;

                    vdp.updatesBitOffsetAsInt(textContent.getTextBitOffset() + c.getOriginalBitPosition());
                    
                    //patch the whole text file, but this happens anyway because text changed
                    vdp.getParent().setPerformPatch(true);
                }

                //update also the reference
                referrer.setReference(c);
            }
        }
    }
    
    //tree creation from text
    
    /**
     * Given a text with characters we count how often a {@link HuffmanNode} occurs and store it in
     * {@link HuffmanNode#getFrequency() }.
     * The output is used to {@link #createHuffmanTree(java.util.Set) }.
     * @param text
     * @return set of unique nodes with frequency counts. 
     */
    private Set<HuffmanNode> toUniqueNodesWithFrequencies(List<HuffmanCharacter> text) {
        Map<HuffmanNode, HuffmanNode> nodeMap = new HashMap<>();
        for (HuffmanCharacter c : text) {
            HuffmanNode n = nodeMap.get(c.getNode());
            if (n == null) {
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
     * Starting with the infrequent leafs it builds the tree until no nodes are left and root is reached.
     * @param nodesWithFrequencies
     * @return
     */
    private HuffmanNode createHuffmanTree(Set<HuffmanNode> nodesWithFrequencies) {

        //to list to sort them by frequency
        List<HuffmanNode> list = new ArrayList<>(nodesWithFrequencies);

        //parsed trees have a 'HuffmanNode{type=Branch, content=0}' so start at 0
        //highest id has the root
        short nodeID = 0;

        while (list.size() > 1) {

            //lowest frequency first
            //if frequency is equal, maybe the original tree used the content has sorting
            list.sort((a, b) -> {
                int cmp = Double.compare(a.getFrequency(), b.getFrequency());
                if (cmp == 0) {
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
            if(a.isBranch() && b.isBranch()) {
                //lowest node number first: 
                //this is also like we read the original trees
                //e.g. root is 183, then we read 181 and then 182
                if(a.getID() < b.getID()) {
                    node.getChildren().add(a);
                    node.getChildren().add(b);
                } else {
                    node.getChildren().add(b);
                    node.getChildren().add(a);
                }
            } else {
                node.getChildren().add(a);
                node.getChildren().add(b);
            }
            
            //accumulate frequency: has to be sum
            node.setFrequency(a.getFrequency() + b.getFrequency());

            //add to list again
            list.add(node);
        }

        return list.get(0);
    }

    //text to bits mapping
    
    /**
     * Based on the huffman tree create for each (control) character node the corresponding bit code.
     * This is used when {@link #updateBitInformation(net.markus.projects.dq4h.data.HeartBeatDataTextContent, net.markus.projects.dq4h.data.HuffmanNode) }.
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
        if (parent.isLeaf()) {
            map.put(parent, bits);

        } else if (parent.isBranch()) {
            getCharacterToBitsMapRecursive(bits + "0", parent.getLeftChild(), map);
            getCharacterToBitsMapRecursive(bits + "1", parent.getRightChild(), map);
        }
    }

}
