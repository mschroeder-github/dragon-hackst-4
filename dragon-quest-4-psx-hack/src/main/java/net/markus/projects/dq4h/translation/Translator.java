package net.markus.projects.dq4h.translation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatDataFile;
import net.markus.projects.dq4h.data.HeartBeatDataFileContent;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataScriptContent;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import net.markus.projects.dq4h.data.HuffmanCharacter;
import net.markus.projects.dq4h.data.HuffmanCharacterReferrer;
import net.markus.projects.dq4h.data.HuffmanNode;
import net.markus.projects.dq4h.data.ScriptStoreEntry;
import net.markus.projects.dq4h.data.VariableToDialogPointer;
import net.markus.projects.dq4h.gui.TranslationProject;
import net.markus.projects.dq4h.io.Converter;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileReader;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileWriter;
import net.markus.projects.dq4h.io.HeartBeatDataTextContentWriter;
import net.markus.projects.dq4h.io.IOConfig;
import net.markus.projects.dq4h.io.Inspector;
import net.markus.projects.dq4h.io.ShiftJIS;
import net.markus.projects.dq4h.util.MemoryUtility;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.crosswire.common.compress.LZSS;
import org.json.JSONObject;

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
        
        //translator.checkJapaneseText();
        
        //translator.checkTextSequences();

        //in cellar
        JSONObject json006c = new JSONObject();
        JSONObject line10 = new JSONObject();
        line10.put("translation", translator.getLongTranslationTest());
        json006c.put("10", line10);
        translator.selectiveTranslation("006c", json006c);
        //town outside
        //translator.selectiveTranslation("0067");
        
        //one with special store
        //translator.selectiveTranslation("022f");
        
        //translator.pointerCompleteTranslationTest();

        translator.exportData();
    }

    public Translator() {
    }
    
    public Translator(File inputFile, File outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    //I/O -------------
    
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
        binWriter.write(binary, outputFile);
        
        long end = System.currentTimeMillis();

        System.out.println("took " + (end - begin) + " ms, " + MemoryUtility.memoryStatistics());

        if (inputFile.length() != outputFile.length()) {
            throw new RuntimeException("file length differ");
        }

        File reportFolder = new File("../../patch-report");
        reportFolder.mkdirs();
        for(File f : reportFolder.listFiles()) {
            f.delete();
        }
        
        config.getChangeLogEntries().forEach(e -> {
            System.out.println(e);
            try {
                e.saveHtmlReport(new File(reportFolder, e.getFilename() + ".html"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * Loads from resources a long text string for translation testing with large text body.
     * @return 
     */
    public String getLongTranslationTest() throws IOException {
        String text = IOUtils.toString(Translator.class.getResourceAsStream("/longTranslationTest.txt"), StandardCharsets.UTF_8);
        //cleanup
        text = text.replace("\n", " ").replace("\t", " ").replaceAll("\\s+", " ");
        
        StringBuilder sb = new StringBuilder();
        
        int maxW = 16;
        int line = 0;
        boolean first = true;
        
        sb.append("{7f04}");
        
        //{7f02} = next line, tab
        //{7f0a} = blinking cursor, wait for user input, moves two lines up, third line is at top, 
        for(int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            
            if(i > 0 && i % maxW == 0) {
                //next line, tab
                if(line <= (first ? 1 : 0)) {
                    sb.append("{7f02}");
                }
                line++;
            }
            
            if(line == (first ? 3 : 2)) {
                line = 0;
                sb.append("{7f0a}{7f02}");
                first = false;
            }
        }
        
        sb.append("{7f0a}{0000}");
        
        return sb.toString();
    }
    
    //-----------------------
    //just checks
    
    public void checkTextSequences() throws IOException {
        
        File reportFolder = new File("../../missing-ref-report");
        reportFolder.mkdirs();
        
        for(File f : reportFolder.listFiles()) {
            f.delete();
        }
        
        int numberOfSequences = 0;
        int numberOfDummySequences = 0;
        int numberOfMissingRefSequences = 0;
        int numberOfAnyRefSequences = 0;
        
        Set<String> textIdsWithMissingRef = new HashSet<>();
        Set<String> textIdsWithAllRef = new HashSet<>();
        
        for (HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {
            
            for(HeartBeatDataTextContent textContent : folder.getContents(HeartBeatDataTextContent.class)) {
                
                List<List<HuffmanCharacter>> sequences = textContent.getOriginalSequences();
                
                //we assume that all refs are there
                boolean allSeqHasRef = true;
                        
                List<HuffmanCharacter> noRefs = new ArrayList<>();
                
                for(List<HuffmanCharacter> seq : sequences) {
                    numberOfSequences++;
                    
                    String str = HuffmanCharacter.listToString(seq);
                    
                    //every sequence ends with {0000}
                    if(!seq.get(seq.size()-1).getNode().isNullCharacter()) {
                        throw new RuntimeException("No \\0 character in " + sequences.indexOf(seq) + " sequence in " + textContent + ": " + HuffmanCharacter.listToString(seq));
                    }
                    
                    boolean isDummy = str.startsWith("ダミー");
                    if(isDummy) {
                        numberOfDummySequences++;
                    }
                    
                    //first has referrers
                    boolean hasRef = seq.get(0).hasReferrers();
                    
                    //note: does not happen
                    if(isDummy && hasRef) {
                        throw new RuntimeException("a dummy is referred in " + textContent);
                    }
                    
                    if(!isDummy && !hasRef) {
                        //those are problematic
                        numberOfMissingRefSequences++;
                        
                        allSeqHasRef = false;
                        
                        //those with missing refs
                        textIdsWithMissingRef.add(textContent.getIdHex());
                        
                        noRefs.add(seq.get(0));
                        
                        //note: if there is no reference then also not in between
                        boolean anyRefInBetween = seq.stream().anyMatch(c -> c.hasReferrers());
                        if(anyRefInBetween) {
                            numberOfAnyRefSequences++;
                        }
                    }
                }//for each seq
                
                if(allSeqHasRef) {
                    textIdsWithAllRef.add(textContent.getIdHex());
                } else {
                    
                    //create a report to look into it
                    
                    List<HeartBeatDataScriptContent> scripts = folder.getContents(HeartBeatDataScriptContent.class);
                    
                    File reportFile;
                    if(scripts.isEmpty()) {
                        //appens once with folder 26030
                        reportFile = new File(reportFolder, textContent.getIdHex() + "-folder" + folder.getIndex() + "-no-script-in-folder.html");
                        reportFile.createNewFile();
                        
                    } else {
                        reportFile = new File(reportFolder, textContent.getIdHex() + "-folder" + folder.getIndex() + ".html");
                    
                        HeartBeatDataScriptContent script = scripts.get(0);
                        
                        StringBuilder html = new StringBuilder();
                        html.append("<html>");
                        html.append("    <head>");
                        html.append("        <title>");
                        html.append("            Folder ").append(folder.getIndex()).append(" ").append(textContent.getIdHex()).append(" - DQ4 Script Check");
                        html.append("        </title>");
                        html.append("    </head>");
                        html.append("    <body style=\"font-family: arial;\">");
                        
                        HeartBeatDataFile file = script.getParent();
                        byte[] bytes = file.getOriginalContentBytes();
                        if(file.isCompressed()) {
                            bytes = LZSS.uncompress(new ByteArrayInputStream(bytes), file.getOriginalSizeUncompressed());
                        }
                        byte[] finalBytes = bytes;
                        
                        List<Integer> possiblePositions = new ArrayList<>();
                        
                        html.append("        <pre>");
                        noRefs.forEach(c -> {
                            int bitPos = c.getOriginalBitPosition();
                            int bitPosShifted = bitPos + textContent.getTextBitOffset();
                            String address = Inspector.toHex(Converter.intToBytesBE(bitPosShifted)).substring(4, 8);
                            String addressReversed = address.substring(2, 4) + address.substring(0, 2);
                            
                            possiblePositions.addAll(Inspector.find(Inspector.toBytes(addressReversed), finalBytes));
                            
                            html
                                    .append(c)
                                    .append(", bitpos: ").append(bitPos)
                                    .append(", bitPosShifted: ").append(bitPosShifted)
                                    .append(", address: ").append(address)
                                    .append(", addressReversed: ").append(addressReversed)
                                    .append("\n");
                        });
                        html.append("        </pre>");
                        
                        html.append("        <pre>");
                        int window = 8;
                        for(int pos : possiblePositions) {
                            byte[] match = Arrays.copyOfRange(bytes, Math.max(0, pos - window), Math.min(pos + 4, bytes.length));
                            String matchHex = Inspector.toHex(match);
                            html.append("0x").append(Inspector.toHex(Converter.intToBytesBE(pos))).append(" | ");
                            html.append(matchHex.substring(0, matchHex.length() - (4*2)));
                            html.append("<mark>");
                            html.append(matchHex.substring(matchHex.length() - (4*2), matchHex.length()));
                            html.append("</mark>");
                            html.append("\n");
                        }
                        html.append("        </pre>");
                        
                        html.append("        <pre>");
                        html.append(Inspector.toHexDump(bytes, 24));
                        html.append("        </pre>");
                        
                        html.append("        <pre>");
                        script.getEntries().forEach(e -> html.append(e).append("\n"));
                        html.append("        </pre>");
                        
                        html.append("    </body>");
                        html.append("</html>");

                        FileUtils.writeStringToFile(reportFile, html.toString(), StandardCharsets.UTF_8);
                    }
                }
                
                
            }//for each text content
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("numberOfSequences: ").append(numberOfSequences).append("\n");
        //sb.append("numberOfMissingRefSequences: ").append(numberOfMissingRefSequences).append("\n");
        //sb.append("numberOfAnyRefSequences: ").append(numberOfAnyRefSequences).append("\n");
        sb.append("numberOfDummySequences: ").append(numberOfDummySequences).append("\n");
        sb.append("textIdsWithMissingRef: ").append("(").append(textIdsWithMissingRef.size()).append(") ").append(textIdsWithMissingRef).append("\n");
        sb.append("textIdsWithAllRef: ").append("(").append(textIdsWithAllRef.size()).append(") ").append(textIdsWithAllRef).append("\n");
        sb.append("Ratio: ").append(textIdsWithAllRef.size() / (double)(textIdsWithAllRef.size()+textIdsWithMissingRef.size())).append("\n");
        System.out.println(sb);
    }
    
    public void checkJapaneseText() throws IOException {
        //List<JapaneseTextOccurrence> l = searchJapaneseText(binary.getExecutable(), binary.getExecutable().getData(), 2);
        
        int minSeqSize = 2;
        
        List<JapaneseTextOccurrence> list = new ArrayList<>();
        
        list.addAll(searchJapaneseText(binary.getExecutable(), binary.getExecutable().getData(), minSeqSize));
        
        for (HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {
            
            for(HeartBeatDataFile file : folder.getFiles()) {
                
                byte[] bytes = file.getOriginalContentBytes();
                if(file.isCompressed()) {
                    bytes = LZSS.uncompress(new ByteArrayInputStream(bytes), file.getOriginalSizeUncompressed());
                }
                
                list.addAll(searchJapaneseText(file, bytes, minSeqSize));
            }
            
        }
        
        //largest sequence first
        list.sort((a,b) -> Integer.compare(b.getLength(), a.getLength()));
        
        FileWriter fw = new FileWriter(new File("../../japanese-text-occurrences.txt"));
        list.forEach(occ -> {
            try {
                fw.write(occ.toString());
                fw.write("\n");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        fw.close();
    }
    
    private List<JapaneseTextOccurrence> searchJapaneseText(Object source, byte[] bytes, int minSeqSize) {
        List<JapaneseTextOccurrence> list = new ArrayList<>();
        
        //shifted | swapped
        //0       | 0
        //1       | 0
        //0       | 1
        //1       | 1
        JapaneseTextOccurrence[] occurArray = new JapaneseTextOccurrence[4];
        
        //for each two bytes
        for(int i = 0; i < bytes.length - 2; i += 2) {
            
            //normal and shifted version
            for(int j = 0; j < 2; j++) {
                
                for(boolean swapped : Arrays.asList(false, true)) {
                
                    byte[] twoBytes = swapped ? 
                            new byte[] { bytes[i + j + 1], bytes[i + j] } : 
                            new byte[] { bytes[i + j], bytes[i + j + 1] };

                    Character c = ShiftJIS.getCharacter(twoBytes);

                    int arrayIndex = j;
                    
                    //for array index
                    if(swapped) {
                        //j = 0 -> 2
                        //j = 1 -> 3
                        arrayIndex += 2;
                    }
                    
                    if(c != null) {
                        //found

                        //System.out.println(j + ": " + c);

                        //there is currently no sequence
                        if(occurArray[arrayIndex] == null) {

                            //init a sequence
                            occurArray[arrayIndex] = new JapaneseTextOccurrence();
                            occurArray[arrayIndex].setStart(i + j);
                            occurArray[arrayIndex].setSource(source);
                            occurArray[arrayIndex].setBytes(bytes);

                        }

                    } else {
                        //not found
                        if(occurArray[arrayIndex] != null) {
                            occurArray[arrayIndex].setEnd(i + j);

                            if(occurArray[arrayIndex].getLength() >= minSeqSize) {
                                list.add(occurArray[arrayIndex]);
                            }

                            //reset
                            occurArray[arrayIndex] = null;
                        }
                    }
                
                }
            }
        }
        
        
        
        return list;
    }
    
    //----------------------
    
    /**
     * Checks all text contents if they have all references and returns
     * their text ids in sorted order.
     * @return 
     */
    public List<String> getPointerCompleteTextIds() {
        Set<String> complete = new HashSet<>();
        
        for (HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {
            
            for(HeartBeatDataTextContent textContent : folder.getContents(HeartBeatDataTextContent.class)) {
                
                List<List<HuffmanCharacter>> sequences = textContent.getOriginalSequences();
                
                boolean allSeqHasRef = true;
                
                if(textContent.getOriginalTreeBytes().length == 18) {
                    //those are all the dummy only text contents where we do not have to patch something
                    /*
                    └── HuffmanNode{type=Branch, content=3}
                    ├── HuffmanNode{type=Branch, content=0}
                    │   ├── HuffmanNode{type=ControlCharacter, content=0000}
                    │   └── HuffmanNode{type=ControlCharacter, content=7f0b}
                    └── HuffmanNode{type=Branch, content=2}
                        ├── HuffmanNode{type=Character, content=ダ}
                        └── HuffmanNode{type=Branch, content=1}
                            ├── HuffmanNode{type=Character, content=ー}
                            └── HuffmanNode{type=Character, content=ミ}
                    */
                    continue;
                }
                
                for(List<HuffmanCharacter> seq : sequences) {
                    String str = HuffmanCharacter.listToString(seq);
                    boolean isDummy = str.startsWith("ダミー");
                    boolean hasRef = seq.get(0).hasReferrers();
                    
                    if(!isDummy && !hasRef) {
                        allSeqHasRef = false;
                        break;
                    }
                }
                
                if(allSeqHasRef) {
                    complete.add(textContent.getIdHex());
                }
            }
        }
        
        List<String> l = new ArrayList<>(complete);
        l.sort((a,b) -> a.compareToIgnoreCase(b));
        return l;
    }
    
    /**
     * Uses {@link #getPointerCompleteTextIds() } to run on them 
     * {@link #selectiveTranslation(java.lang.String, org.json.JSONObject)  } but with empty JSON object (no translation given).
     */
    public void pointerCompleteTranslationTest() {
        for(String textId : getPointerCompleteTextIds()) {
            
            //java.io.IOException: 0180 number of byte for text is larger than original: 8 < 16
            //original text is: ダミー{7f0b}{0000}ホフマンの家{7f0b}{0000}
            if(textId.equals("0180")) {
                continue;
            }
            
            selectiveTranslation(textId, new JSONObject());
        }
    }
    
    /**
     * We should only translate selected text contents.
     * It is a test so it just 'translates' the text to technical information
     * for debugging.
     * @param textId a two byte hex value, e.g. "006c"
     * @deprecated better use {@link #selectiveTranslation(java.lang.String, org.json.JSONObject) } with empty JSONObject.
     */
    @Deprecated
    public void selectiveTranslationTest(String textId) {

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
            //List<HuffmanCharacter> originalStarts = textContent.getOriginalStartCharacters();
            List<List<HuffmanCharacter>> originalSequences = textContent.getOriginalSequences();
            
            if(originalSequences.isEmpty()) {
                System.out.println(textContent.getIdHex() + " skipped because there are no sequences found in text: " + 
                        HuffmanCharacter.listToString(textContent.getOriginalText())
                );
                continue;
            }
            
            //build artifical one for debugging
            String asciiStr = "";
            for (int i = 0; i < originalSequences.size(); i++) {
                int reversedIndex = originalSequences.size() - i;
                asciiStr += textContent.getIdHex() + " " + reversedIndex + ". Line{7f0a}{0000}";
                //asciiStr += "test"+ reversedIndex +"{7f0a}{0000}";
            }

            List<HuffmanCharacter> replacer = ShiftJIS.toJapanese(asciiStr);
            List<HuffmanCharacter> replacerStarts = HeartBeatDataTextContent.getStartCharacters(replacer);

            //need to be same number of starts
            if (originalSequences.size() != replacerStarts.size()) {
                throw new RuntimeException("different number of sequences");
            }

            //copy the referrers
            for (int i = 0; i < originalSequences.size(); i++) {
                String str = HuffmanCharacter.listToString(originalSequences.get(i));
                boolean isDummy = str.startsWith("ダミー");
                
                if(!isDummy && originalSequences.get(i).get(0).getReferrers().isEmpty()) {
                    throw new RuntimeException("No referrer found: " + originalSequences.get(i).get(0));
                }
                
                replacerStarts.get(i).setReferrers(originalSequences.get(i).get(0).getReferrers());
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
     * Translates text contents with given textId using its meta data.
     * This JSON object comes from {@link TranslationProject}.
     * It does not have to be complete. In case the line is not translated, a default is given
     * which is the text id and a line number: <code>"%s %d. Line{7f0a}{0000}"</code>. The line index is 1-indexed.
     * It is a copy of {@link #selectiveTranslationTest(java.lang.String) } but with the possibility to state the
     * translated lines.
     * @param textId a two byte hex value, e.g. "006c"
     * @param textIdMetaData a JSON object with translation text for
     * each line using JSON keys, e.g. <code>{ "5": { "translation": "test" } }</code>
     */
    public void selectiveTranslation(String textId, JSONObject textIdMetaData) {

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
            //List<HuffmanCharacter> originalStarts = textContent.getOriginalStartCharacters();
            List<List<HuffmanCharacter>> originalSequences = textContent.getOriginalSequences();
            
            if(originalSequences.isEmpty()) {
                System.out.println(textContent.getIdHex() + " skipped because there are no sequences found in text: " + 
                        HuffmanCharacter.listToString(textContent.getOriginalText())
                );
                continue;
            }
            
            //build artifical one for debugging
            String asciiStr = "";
            for (int i = 0; i < originalSequences.size(); i++) {
                
                JSONObject textObj = textIdMetaData.optJSONObject("" + i);
                
                if(textObj == null || !textObj.has("translation")) {
                    asciiStr += String.format("%s %d. Line{7f0a}{0000}", textContent.getIdHex(), (i + 1));
                } else {
                    asciiStr += textObj.getString("translation");
                }
            }

            List<HuffmanCharacter> replacer = ShiftJIS.toJapanese(asciiStr);
            List<HuffmanCharacter> replacerStarts = HeartBeatDataTextContent.getStartCharacters(replacer);

            //need to be same number of starts
            if (originalSequences.size() != replacerStarts.size()) {
                throw new RuntimeException("different number of sequences");
            }

            //copy the referrers
            for (int i = 0; i < originalSequences.size(); i++) {
                String str = HuffmanCharacter.listToString(originalSequences.get(i));
                boolean isDummy = str.startsWith("ダミー");
                
                if(!isDummy && originalSequences.get(i).get(0).getReferrers().isEmpty()) {
                    throw new RuntimeException("No referrer found: " + originalSequences.get(i).get(0));
                }
                
                replacerStarts.get(i).setReferrers(originalSequences.get(i).get(0).getReferrers());
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
