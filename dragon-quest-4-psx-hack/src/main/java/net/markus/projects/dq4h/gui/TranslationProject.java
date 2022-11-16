package net.markus.projects.dq4h.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import net.markus.projects.dq4h.data.DragonQuestBinary;
import net.markus.projects.dq4h.data.HeartBeatDataFolderEntry;
import net.markus.projects.dq4h.data.HeartBeatDataTextContent;
import net.markus.projects.dq4h.data.HuffmanCharacter;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileReader;
import net.markus.projects.dq4h.io.DragonQuestBinaryFileWriter;
import net.markus.projects.dq4h.io.IOConfig;
import net.markus.projects.dq4h.translation.Translator;
import net.markus.projects.dq4h.util.MemoryUtility;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Manages a translation project. This is used by {@link TranslatorFrame} to let translators read and write dialog lines.
 */
public class TranslationProject {

    private IOConfig ioConfig;

    /**
     * Input file. dq4.bin.
     */
    private File binaryFile;
    /**
     * Loaded input file.
     */
    private DragonQuestBinary binary;

    /**
     * Output file. dq4-patched.bin.
     */
    private File outputFile;
    
    /**
     * Where the patch report is stored.
     */
    private File reportFolder;
    /**
     * Decides if after patch report is written to reportFolder.
     */
    private boolean writePatchReport;
    
    /**
     * Patch all.
     */
    private boolean patchAll;

    /**
     * A translation file. We use JSON.
     */
    private File translationFile;
    /**
     * We use a json file to store the current translations.
     */
    private JSONObject translation;

    /**
     * To run the translation.
     */
    private Translator translator;
    
    /**
     * The pointer complete text ids in sorted order.
     */
    private List<String> textIds;
    /**
     * Listener to tell that textIds has changed.
     * Inited in {@link #getTextIdListModel() }.
     */
    private ListDataListener listDataListener;
    
    /**
     * A map to hold the number of sequences per text id.
     * This is filled by {@link #getTableModel(java.lang.String) }.
     */
    private Map<String, Integer> textId2seqNoCache;
    
    public TranslationProject() {
        ioConfig = new IOConfig();
        translation = new JSONObject();
        translator = new Translator();
        textId2seqNoCache = new HashMap<>();
        writePatchReport = true;
    }

    /**
     * Opens the binary file and translation file.
     * If translation file does not exist, an empty one is written.
     * @param binaryFile dq4.bin
     * @param listener a listener to explain the progress
     * @throws IOException 
     */
    public void open(File binaryFile, ProjectListener listener) throws IOException {
        listener.setProgressMax(3);
        listener.setProgressValue(0);
        
        this.binaryFile = binaryFile;
        this.reportFolder = new File(binaryFile.getParent(), "patch-report");
        
        //open binary (takes around 10 seconds)
        listener.setProgressText("Opening " + relativePath(binaryFile) + " (" + MemoryUtility.humanReadableByteCount(binaryFile.length()) + ")");
        long begin = System.currentTimeMillis();
        DragonQuestBinaryFileReader binReader = new DragonQuestBinaryFileReader(ioConfig);
        binary = binReader.read(binaryFile);
        long end = System.currentTimeMillis();
        listener.setProgressText("Took " + (end - begin) + " ms");
        listener.setProgressText(MemoryUtility.memoryStatistics());
        listener.setProgressText(binary.toString());
        listener.setProgressValue(1);
        
        //open translation file based on basename
        String basename = FilenameUtils.getBaseName(binaryFile.getName());
        String translationName = basename + ".json";
        translationFile = new File(binaryFile.getParentFile(), translationName);
        if(!translationFile.exists()) {
            listener.setProgressText("Writing empty " + relativePath(translationFile));
            FileUtils.writeStringToFile(translationFile, translation.toString(2), StandardCharsets.UTF_8);
        } else {
            listener.setProgressText("Opening " + relativePath(translationFile) + " (" + MemoryUtility.humanReadableByteCount(translationFile.length()) + ")");
            translation = new JSONObject(new JSONTokener(new FileInputStream(translationFile)));
        }
        String patchedName = basename + "-patched.bin";
        outputFile = new File(binaryFile.getParentFile(), patchedName);
        listener.setProgressValue(2);
        
        //search for text ids
        listener.setProgressText("Search for pointer complete text ids");
        translator.setBinary(binary);
        textIds = translator.getPointerCompleteTextIds();
        listener.setProgressText(textIds.size() + " text ids found");
        //update list in GUI
        if(listDataListener != null) {
            listDataListener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, textIds.size()));
        }
        
        //last step
        listener.setProgressText("Successful");
        listener.setProgressValue(3);
    }

    /**
     * Save the current translation state.
     * @param listener
     * @throws IOException 
     */
    public void save(ProjectListener listener) throws IOException {
        if (translationFile == null) {
            return;
        }
        
        listener.setProgressMax(1);
        listener.setProgressValue(0);
        
        //listener.setProgressText("Writing " + relativePath(translationFile));
        FileUtils.writeStringToFile(translationFile, translation.toString(2), StandardCharsets.UTF_8);
        listener.setProgressText(relativePath(translationFile) + " (" + MemoryUtility.humanReadableByteCount(translationFile.length()) + ") written");
        
        listener.setProgressValue(1);
    }

    /**
     * Patches the binary file.
     * Load the binary first with {@link #open(java.io.File, net.markus.projects.dq4h.gui.ProjectListener) }.
     * @param listener
     * @throws IOException 
     */
    public void patch(ProjectListener listener) throws IOException {
        if (binary == null) {
            return;
        }
                        
        //auto save before patch
        save(listener);
        
        
        if(patchAll) {
            listener.setProgressText("Embed test translations in all texts because Patch All is enabled");
            translator.pointerCompleteTranslationTest();
            
        } else {
            List<String> textIds = new ArrayList<>();
            for(String textId : translation.keySet()) {
                JSONObject textContent = translation.getJSONObject(textId);
                //only those who have this activated
                if(textContent.optBoolean("patch")) {
                    textIds.add(textId);
                }
            }

            if(textIds.isEmpty()) {
                return;
            }

            listener.setProgressMax(3);
            listener.setProgressValue(0);

            //run selective translation, this sets text and trees and changes referer pointer
            for(String textId : textIds) {
                JSONObject textContent = translation.getJSONObject(textId);
                listener.setProgressText("Embed translation in " + textId + " " + textContent.toString());
                //TODO check if this can be done multiple times without any breaks
                translator.selectiveTranslation(textId, textContent);
            }
        }
        
        listener.setProgressValue(1);
        listener.setProgressText("Write patch to " + relativePath(outputFile));
        
        long begin = System.currentTimeMillis();
        DragonQuestBinaryFileWriter binWriter = new DragonQuestBinaryFileWriter(ioConfig);
        binWriter.patch(
                binary,
                binaryFile,
                outputFile
        );
        long end = System.currentTimeMillis();

        listener.setProgressText("Took " + (end - begin) + " ms");
        listener.setProgressText(MemoryUtility.memoryStatistics());

        if (binaryFile.length() != outputFile.length()) {
            throw new RuntimeException("File size differ: " + binaryFile.length() + " != " + outputFile.length());
        }
        
        if(writePatchReport) {
            listener.setProgressValue(2);
            listener.setProgressText("Write patch reports to " + relativePath(reportFolder) + " because Generate Patch Report is enabled");
            
            this.reportFolder.mkdirs();
            for(File f : reportFolder.listFiles()) {
                f.delete();
            }
            ioConfig.getChangeLogEntries().forEach(e -> {
                try {
                    e.saveHtmlReport(new File(reportFolder, e.getFilename() + ".html"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
        
        listener.setProgressValue(3);
        listener.setProgressText("Successful");
    }

    /**
     * Prepares a mail using mailto and {@link Desktop}.
     * @param mail mail address
     * @param subject optional subject
     * @param body optional email text content
     * @throws RuntimeException if not supported or URI/IO error
     */
    public void sendMail(String mail, String subject, String body) {
        Desktop desktop;
        if (Desktop.isDesktopSupported() && (desktop = Desktop.getDesktop()).isSupported(Desktop.Action.MAIL)) {
            try {
                String subjectEsc = URLEncoder.encode(subject, StandardCharsets.UTF_8).replace("+", "%20");
                String bodyEsc = URLEncoder.encode(body, StandardCharsets.UTF_8).replace("+", "%20");
                
                URI mailto = new URI("mailto:"+ mail +"?subject=" + subjectEsc + "&body=" + bodyEsc);
                desktop.mail(mailto);
                
            } catch (URISyntaxException | IOException ex) {
                throw new RuntimeException(ex);
            }
            
        } else {
            throw new RuntimeException("Sending mail not supported");
        }
    }
    
    /**
     * Returns the relative path to the current working directory.
     * @param f
     * @return 
     */
    private String relativePath(File f) {
        Path absolutePath1 = f.getAbsoluteFile().toPath();
        Path absolutePath2 = new File(".").getAbsoluteFile().toPath();
        Path relativePath = absolutePath2.relativize(absolutePath1);
        return relativePath.toString();
    }
    
    /**
     * Returns a {@link ListModel} for the pointer complete {@link #textIds}.
     * @return 
     */
    public ListModel<String> getTextIdListModel() {
        return new AbstractListModel<String>() {
            @Override
            public int getSize() {
                if(textIds == null) {
                    return 0;
                }
                return textIds.size();
            }

            @Override
            public String getElementAt(int i) {
                if(textIds == null) {
                    return null;
                }
                return textIds.get(i);
            }

            @Override
            public void addListDataListener(ListDataListener l) {
                listDataListener = l;
            }
        };
    }
    
    /**
     * Returns a table model for a given textId to translate the dialog lines.
     * @param textId
     * @return 
     */
    public TableModel getTableModel(String textId) {
        List<HeartBeatDataTextContent> textContents = new ArrayList<>();
        for (HeartBeatDataFolderEntry folder : binary.getHeartBeatData().getFolders()) {
            textContents.addAll(folder.getTextContentById(textId));
        }
        
        if(textContents.isEmpty()) {
            throw new RuntimeException("no text content found for text id " + textId);
        }
        
        //we assume that for multiple ones all are the same
        HeartBeatDataTextContent textContent = textContents.get(0);
        
        List<List<HuffmanCharacter>> originalSequences = textContent.getOriginalSequences();
        
        textId2seqNoCache.put(textId, originalSequences.size());
        
        return new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return originalSequences.size();
            }

            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public String getColumnName(int column) {
                switch(column) {
                    case 0: return "Line";
                    case 1: return "Japanese";
                    case 2: return "Translation";
                }
                return "";
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                
                List<HuffmanCharacter> sequence = originalSequences.get(rowIndex);
                
                switch(columnIndex) {
                    case 0: return rowIndex + 1; //1-indexed line number
                    case 1: return HuffmanCharacter.listToString(sequence);
                    case 2: {
                        JSONObject textObj = translation.optJSONObject(textId);
                        if(textObj == null) {
                            return "";
                        }
                        JSONObject seqObj = textObj.optJSONObject(String.valueOf(rowIndex));
                        if(seqObj == null) {
                            return "";
                        }
                        String transl = seqObj.optString("translation");
                        if(transl == null) {
                            return "";
                        }
                        return transl;
                    }
                }
                
                return "";
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
               switch(columnIndex) {
                   case 0: return false;
                   case 1: return true; //to select japanese text
                   case 2: return true;
               }
               return false;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if(!(aValue instanceof String)) {
                    return;
                }
                //no edit for line and japanese text
                if(columnIndex <= 1) {
                    return;
                }
                
                String value = (String) aValue;
                
                JSONObject textObj = translation.optJSONObject(textId);
                if(textObj == null) {
                    textObj = new JSONObject();
                    translation.put(textId, textObj);
                }
                JSONObject seqObj = textObj.optJSONObject(String.valueOf(rowIndex));
                if(seqObj == null) {
                    seqObj = new JSONObject();
                    textObj.put(String.valueOf(rowIndex), seqObj);
                }
                
                if(value.isBlank()) {
                    seqObj.remove("translation");
                } else {
                    
                    //auto {0000} at the end
                    value = value.trim();
                    if(!value.endsWith("{0000}")) {
                        value += "{0000}";
                    }
                    
                    seqObj.put("translation", value);
                }
            }
            
        };
    }
    
    /**
     * Sets a name for the text id for better identification.
     * @param textId 4 letter hex id
     * @param name an arbitrary name
     */
    public void setName(String textId, String name) {
        JSONObject textObj = translation.optJSONObject(textId);
        if(textObj == null) {
            textObj = new JSONObject();
            translation.put(textId, textObj);
        }
        textObj.put("name", name);
    }
    
    /**
     * Returns the name of a text id.
     * @param textId 4 letter hex id
     * @return its name, never null
     */
    public String getName(String textId) {
        JSONObject textObj = translation.optJSONObject(textId);
        if(textObj == null) {
            return "";
        }
        String name = textObj.optString("name");
        if(name == null) {
            return "";
        }
        return name;
    }
    
    /**
     * Sets the patch state.
     * All enabled text ids will be patched.
     * @param textId 4 letter hex id
     * @param state true, the text id will be patched.
     */
    public void setPatch(String textId, boolean state) {
        JSONObject textObj = translation.optJSONObject(textId);
        if(textObj == null) {
            textObj = new JSONObject();
            translation.put(textId, textObj);
        }
        textObj.put("patch", state);
    }
    
    /**
     * Returns the patch state of a text id.
     * @param textId 4 letter hex id
     * @return true if text id will be patched
     */
    public boolean getPatch(String textId) {
        JSONObject textObj = translation.optJSONObject(textId);
        if(textObj == null) {
            return false;
        }
        return textObj.optBoolean("patch");
    }
    
    /**
     * Returns an int array [number of translated sequnces, total sequences].
     * We use a cache here which is filled by {@link #getTableModel(java.lang.String) }. 
     * @param textId 4 letter hex id
     * @return both values: current and total
     */
    public int[] getTranslatedNumber(String textId) {
        
        int total = textId2seqNoCache.getOrDefault(textId, 0);
        
        JSONObject textObj = translation.optJSONObject(textId);
        if(textObj == null) {
            return new int[] { 0, total };
        }
        
        int count = 0;
        for(String key : textObj.keySet()) {
            JSONObject obj = textObj.optJSONObject(key);
            if(obj == null)
                continue;
            if(obj.has("translation")) {
                count++;
            }
        }
        
        return new int[] { count, total };
    }

    /**
     * If true, will write the patch-report folder.
     * @param writePatchReport 
     */
    public void setWritePatchReport(boolean writePatchReport) {
        this.writePatchReport = writePatchReport;
    }

    /**
     * Patches all instead of only the selected ones.
     * @param patchAll 
     */
    public void setPatchAll(boolean patchAll) {
        this.patchAll = patchAll;
    }
    
    
    
}
