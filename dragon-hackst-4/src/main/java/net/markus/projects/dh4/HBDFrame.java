package net.markus.projects.dh4;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import net.markus.projects.dh4.data.StarZerosSubBlock;
import net.markus.projects.dh4.util.Utils;

/**
 *
 */
public class HBDFrame extends javax.swing.JFrame {

    private HBD1PS1D hbd;

    private StarZerosSubBlock lastSubBlock;

    private BufferedImage image;
    
    public HBDFrame(HBD1PS1D hbd) {
        this.hbd = hbd;
        initComponents();
        setLocationRelativeTo(null);

        jSpinnerDumpWidth.setValue(16);
        jSpinnerImageWidth.setValue(32);
        loadSubBlocks();
    }

    private void loadSubBlocks() {

        List<StarZerosSubBlock> list = hbd.getStarZerosSubBlocks();
        
        //hbd.sortBySizeCompressed(list);
        
        StarZerosSubBlock[] data = list.toArray(new StarZerosSubBlock[0]);
        jListSubBlocks.setListData(data);

        List<StarZerosSubBlock> hiraganaSubBlocksList = new ArrayList<>();
        for(StarZerosSubBlock sb : hbd.getStarZerosSubBlocks()) {
            if(!Utils.findHiragana(4, sb.data).isEmpty()) {
                hiraganaSubBlocksList.add(sb);
            }
        }
        StarZerosSubBlock[] hiraganaSubBlocks = hiraganaSubBlocksList.toArray(new StarZerosSubBlock[0]);
        
        List<StarZerosSubBlock> asciiSubBlocksList = new ArrayList<>();
        for(StarZerosSubBlock sb : hbd.getStarZerosSubBlocks()) {
            if(!Utils.findASCII(10, sb.data, true).isEmpty()) {
                asciiSubBlocksList.add(sb);
            }
        }
        hbd.sortBySizeCompressed(asciiSubBlocksList);
        StarZerosSubBlock[] asciiSubBlocks = asciiSubBlocksList.toArray(new StarZerosSubBlock[0]);
        
        
        
        DefaultComboBoxModel<String> cbm = (DefaultComboBoxModel<String>) jComboBoxBlockFilter.getModel();
        cbm.addElement("all (" + data.length + ")");
        
        cbm.addElement("hiragana (" + hiraganaSubBlocks.length + ")");
        cbm.addElement("ascii (" + asciiSubBlocks.length + ")");

        jComboBoxBlockFilter.addItemListener((ItemEvent e) -> {
            String item = (String) e.getItem();
            if(item.startsWith("all")) {
                //set all
                jListSubBlocks.setListData(hbd.getStarZerosSubBlocks().toArray(new StarZerosSubBlock[0]));
            } else if(item.startsWith("type=")) {
                int t = Integer.parseInt(item.substring("type=".length(), item.indexOf("(")).trim());
                jListSubBlocks.setListData(hbd.getStarZerosSubBlocks(t).toArray(new StarZerosSubBlock[0]));
            } else if(item.startsWith("hiragana")) {
                jListSubBlocks.setListData(hiraganaSubBlocks);
            } else if(item.startsWith("ascii")) {
                jListSubBlocks.setListData(asciiSubBlocks);
            }
        });
        
        Map<Integer, Integer> type2count = new HashMap<>();

        for (StarZerosSubBlock sb : data) {
            int count = type2count.computeIfAbsent(sb.flags2, i -> 0);
            type2count.put(sb.flags2, count + 1);
        }
        List<Integer> types = new ArrayList<>(type2count.keySet());
        Collections.sort(types);
        for (int type : types) {
            
            float f = (type2count.get(type) / (float) data.length) * 100;
            cbm.addElement("type=" + type + " (" + type2count.get(type) + ") " + String.format(Locale.US, "%.2f%%", f));
        }

        jListSubBlocks.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if(e.getValueIsAdjusting())
                return;
            
            StarZerosSubBlock sb = jListSubBlocks.getSelectedValue();
            
            if(sb != null) {
                lastSubBlock = sb;
                
                updateHexDump(sb);
                updateImage(sb);
                //jLabelImg.setIcon(new ImageIcon(Utils.toGrayscale(sb.data, 32, -1)));
            }
        });
        
        jListSubBlocks.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof StarZerosSubBlock) {
                    StarZerosSubBlock sb = (StarZerosSubBlock) value;
                    lbl.setText(sb.parent.blockIndex + "/" + sb.blockIndex + " size=" + sb.size + " uncomp.size=" + sb.sizeUncompressed + " type=" + sb.flags2 + " " + (sb.compressed ? "lz" : ""));
                }

                return lbl;
            }

        });
    }

    private void updateHexDump(StarZerosSubBlock sb) {
        if(sb == null)
            return;
        
        String dump = Utils.toHexDump(sb.data, (int) jSpinnerDumpWidth.getValue(), true, true, hbd.reader.sjishort2char);
        jTextAreaDump.setText(dump);
        jTextAreaDump.setCaretPosition(0);
        
        
        if(sb.compressed) {
            //try {
                //FF7LZSInputStream lzs = new FF7LZSInputStream(new ByteArrayInputStream(sb.data));
                byte[] uncompressed = new byte[0]; //DQLZS.decompress(sb.data, sb.sizeUncompressed); //new byte[sb.sizeUncompressed];
                
                //for(int i = 0; i < uncompressed.length; i++) {
                //    uncompressed[i] = (byte) lzs.read();
                //}
                
                String ud = Utils.toHexDump(uncompressed, (int) jSpinnerDumpWidth.getValue(), true, true, hbd.reader.sjishort2char);
                jTextAreaDumpUncompressed.setText(ud);
                jTextAreaDumpUncompressed.setCaretPosition(0);
                
            //} catch (IOException ex) {
            //    throw new RuntimeException(ex);
            //}
        }
    }

    private void updateImage(StarZerosSubBlock sb) {
        image = Utils.toGrayscale(sb.data, (int) jSpinnerImageWidth.getValue(), -1);
        jPanelImage.repaint();
    }
    
    private void draw(Graphics g) {
        if(image == null)
            return;
        
        g.setColor(Color.white);
        g.fillRect(0, 0, jPanelImage.getWidth(), jPanelImage.getHeight());
        g.drawImage(image, 0, 0, image.getWidth() * 4, image.getHeight() * 4, null);
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListSubBlocks = new javax.swing.JList<>();
        jComboBoxBlockFilter = new javax.swing.JComboBox<>();
        jScrollPane3 = new javax.swing.JScrollPane();
        jListH60 = new javax.swing.JList<>();
        jPanel1 = new javax.swing.JPanel();
        jSpinnerDumpWidth = new javax.swing.JSpinner();
        jSpinnerImageWidth = new javax.swing.JSpinner();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaDump = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextAreaDumpUncompressed = new javax.swing.JTextArea();
        jPanelImage = new JPanel() {
            @Override
            public void paint(Graphics g) {
                draw(g);
            }
        };

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("DQ4 Inspector");

        jListSubBlocks.setFont(new java.awt.Font("DialogInput", 0, 12)); // NOI18N
        jListSubBlocks.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jListSubBlocks.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListSubBlocksValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jListSubBlocks);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jComboBoxBlockFilter, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addGap(0, 0, 0))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jComboBoxBlockFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("block/subblock", jPanel2);

        jScrollPane3.setViewportView(jListH60);

        jTabbedPane1.addTab("0x60010108", jScrollPane3);

        jSplitPane1.setLeftComponent(jTabbedPane1);

        jSpinnerDumpWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerDumpWidthStateChanged(evt);
            }
        });

        jSpinnerImageWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerImageWidthStateChanged(evt);
            }
        });

        jTextAreaDump.setColumns(20);
        jTextAreaDump.setFont(new java.awt.Font("DialogInput", 0, 10)); // NOI18N
        jTextAreaDump.setRows(5);
        jScrollPane2.setViewportView(jTextAreaDump);

        jTabbedPane2.addTab("Hex Dump", jScrollPane2);

        jTextAreaDumpUncompressed.setColumns(20);
        jTextAreaDumpUncompressed.setFont(new java.awt.Font("DialogInput", 0, 10)); // NOI18N
        jTextAreaDumpUncompressed.setRows(5);
        jScrollPane4.setViewportView(jTextAreaDumpUncompressed);

        jTabbedPane2.addTab("Uncompressed Hex Dump", jScrollPane4);

        javax.swing.GroupLayout jPanelImageLayout = new javax.swing.GroupLayout(jPanelImage);
        jPanelImage.setLayout(jPanelImageLayout);
        jPanelImageLayout.setHorizontalGroup(
            jPanelImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 560, Short.MAX_VALUE)
        );
        jPanelImageLayout.setVerticalGroup(
            jPanelImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 434, Short.MAX_VALUE)
        );

        jTabbedPane2.addTab("Image", jPanelImage);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSpinnerDumpWidth, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerImageWidth, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jTabbedPane2)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinnerDumpWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSpinnerImageWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane2))
        );

        jSplitPane1.setRightComponent(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jSplitPane1)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jListSubBlocksValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListSubBlocksValueChanged
        //if(!evt.getValueIsAdjusting()) {
        //    return;
        //}
        //StarZerosSubBlock sb = jListSubBlocks.getModel().getElementAt(evt.getFirstIndex());
        //lastSubBlock = sb;
        //updateHexDump(sb);
    }//GEN-LAST:event_jListSubBlocksValueChanged

    private void jSpinnerDumpWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerDumpWidthStateChanged
        if (lastSubBlock != null) {
            updateHexDump(lastSubBlock);
        }
    }//GEN-LAST:event_jSpinnerDumpWidthStateChanged

    private void jSpinnerImageWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerImageWidthStateChanged
        if (lastSubBlock != null) {
            updateImage(lastSubBlock);
        }
    }//GEN-LAST:event_jSpinnerImageWidthStateChanged

    public static void showGUI(HBD1PS1D hbd1ps1d) {
        java.awt.EventQueue.invokeLater(() -> {
            new HBDFrame(hbd1ps1d).setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> jComboBoxBlockFilter;
    private javax.swing.JList<String> jListH60;
    private javax.swing.JList<StarZerosSubBlock> jListSubBlocks;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelImage;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSpinner jSpinnerDumpWidth;
    private javax.swing.JSpinner jSpinnerImageWidth;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTextArea jTextAreaDump;
    private javax.swing.JTextArea jTextAreaDumpUncompressed;
    // End of variables declaration//GEN-END:variables
}
