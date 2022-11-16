package net.markus.projects.dq4h.gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Graphical user interface for translation.
 */
public class TranslatorFrame extends javax.swing.JFrame implements ProjectListener {

    //manages a project
    private TranslationProject project;

    private boolean logTime;

    public TranslatorFrame() {
        initComponents();

        Properties p = new Properties();
        try {
            p.load(TranslatorFrame.class.getResourceAsStream("/net/markus/projects/dq4h/dq4h.properties"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        String version = p.getProperty("dq4h.version");

        //center and title window
        this.setLocationRelativeTo(null);
        this.setTitle("Dragon Quest 4 PSX Patcher - Version " + version);
        this.log(getTitle());
        this.log("For more information visit http://markus-projects.net/dragon-hackst-iv/");
        this.log("\nShort tutorial:\n\t* Click File > Open... and select dq4.bin file\n\t* Click on left-handed text id list to open text content\n\t* Enter translated text in right-handed table\n\t* Enable for patching with checkbox\n\t* Click File > Patch");

        configureFileChooser();

        this.project = new TranslationProject();

        this.jListTextIds.setModel(project.getTextIdListModel());

        jToolBarTop.setFloatable(false);

        //custom renderer
        jListTextIds.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody

                String textId = (String) value;

                StringBuilder sb = new StringBuilder();
                //text id
                sb.append(textId);
                //progress
                int[] stat = project.getTranslatedNumber(textId);
                sb.append(String.format(" (%d/%d)", stat[0], stat[1]));
                //show name
                String name = project.getName(textId);
                if (!name.isBlank()) {
                    sb.append(" ").append(name);
                }
                //set
                lbl.setText(sb.toString());

                //if not patch enabled
                if (!project.getPatch(textId)) {
                    //no bold
                    lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getStyle() & ~Font.BOLD));
                }

                return lbl;
            }
        });

    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooserProject = new javax.swing.JFileChooser();
        jSplitPaneMain = new javax.swing.JSplitPane();
        jPanelTop = new javax.swing.JPanel();
        jSplitPaneTop = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableTextContent = new javax.swing.JTable();
        jToolBarTop = new javax.swing.JToolBar();
        jCheckBoxPatch = new javax.swing.JCheckBox();
        jTextFieldTextName = new javax.swing.JTextField();
        jScrollPaneTextIds = new javax.swing.JScrollPane();
        jListTextIds = new javax.swing.JList<>();
        jPanelBottom = new javax.swing.JPanel();
        jScrollPaneLog = new javax.swing.JScrollPane();
        jTextAreaLog = new javax.swing.JTextArea();
        jProgressBarMain = new javax.swing.JProgressBar();
        jMenuBarMain = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemOpen = new javax.swing.JMenuItem();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemPatch = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuItemMail = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemQuit = new javax.swing.JMenuItem();
        jMenuSettings = new javax.swing.JMenu();
        jCheckBoxMenuItemPatchReport = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPatchAll = new javax.swing.JCheckBoxMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPaneMain.setDividerLocation(200);
        jSplitPaneMain.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPaneMain.setResizeWeight(1.0);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jTableTextContent.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jTableTextContent.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTableTextContent.setRowSelectionAllowed(false);
        jScrollPane1.setViewportView(jTableTextContent);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jToolBarTop.setPreferredSize(new java.awt.Dimension(108, 30));

        jCheckBoxPatch.setText("Patch");
        jCheckBoxPatch.setFocusable(false);
        jCheckBoxPatch.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jCheckBoxPatch.setPreferredSize(new java.awt.Dimension(70, 40));
        jCheckBoxPatch.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jCheckBoxPatch.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxPatchItemStateChanged(evt);
            }
        });
        jToolBarTop.add(jCheckBoxPatch);

        jTextFieldTextName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextFieldTextNameKeyReleased(evt);
            }
        });
        jToolBarTop.add(jTextFieldTextName);

        jPanel1.add(jToolBarTop, java.awt.BorderLayout.PAGE_START);

        jSplitPaneTop.setRightComponent(jPanel1);

        jListTextIds.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jListTextIds.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListTextIdsValueChanged(evt);
            }
        });
        jScrollPaneTextIds.setViewportView(jListTextIds);

        jSplitPaneTop.setLeftComponent(jScrollPaneTextIds);

        javax.swing.GroupLayout jPanelTopLayout = new javax.swing.GroupLayout(jPanelTop);
        jPanelTop.setLayout(jPanelTopLayout);
        jPanelTopLayout.setHorizontalGroup(
            jPanelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPaneTop)
        );
        jPanelTopLayout.setVerticalGroup(
            jPanelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPaneTop, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
        );

        jSplitPaneMain.setLeftComponent(jPanelTop);

        jPanelBottom.setLayout(new java.awt.BorderLayout());

        jTextAreaLog.setEditable(false);
        jTextAreaLog.setColumns(20);
        jTextAreaLog.setFont(new java.awt.Font("Bitstream Vera Sans Mono", 0, 12)); // NOI18N
        jTextAreaLog.setRows(5);
        jTextAreaLog.setTabSize(4);
        jScrollPaneLog.setViewportView(jTextAreaLog);

        jPanelBottom.add(jScrollPaneLog, java.awt.BorderLayout.CENTER);

        jProgressBarMain.setString("");
        jProgressBarMain.setStringPainted(true);
        jPanelBottom.add(jProgressBarMain, java.awt.BorderLayout.SOUTH);

        jSplitPaneMain.setBottomComponent(jPanelBottom);

        jMenuFile.setText("File");

        jMenuItemOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItemOpen.setText("Open...");
        jMenuItemOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemOpen);

        jMenuItemSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItemSave.setText("Save");
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSave);

        jMenuItemPatch.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItemPatch.setText("Patch");
        jMenuItemPatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPatchActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemPatch);
        jMenuFile.add(jSeparator2);

        jMenuItemMail.setText("Report Log via Mail");
        jMenuItemMail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMailActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemMail);
        jMenuFile.add(jSeparator1);

        jMenuItemQuit.setText("Quit");
        jMenuItemQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemQuitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemQuit);

        jMenuBarMain.add(jMenuFile);

        jMenuSettings.setText("Settings");

        jCheckBoxMenuItemPatchReport.setSelected(true);
        jCheckBoxMenuItemPatchReport.setText("Generate Patch Report");
        jCheckBoxMenuItemPatchReport.setToolTipText("Generates report files in the patch-report folder to check what bytes were changed.");
        jCheckBoxMenuItemPatchReport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPatchReportActionPerformed(evt);
            }
        });
        jMenuSettings.add(jCheckBoxMenuItemPatchReport);

        jCheckBoxMenuItemPatchAll.setText("Patch All");
        jCheckBoxMenuItemPatchAll.setToolTipText("Regardless of the patch flag, patches all text contents. This is helpful to find the text IDs in dialog boxes.");
        jCheckBoxMenuItemPatchAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPatchAllActionPerformed(evt);
            }
        });
        jMenuSettings.add(jCheckBoxMenuItemPatchAll);

        jMenuBarMain.add(jMenuSettings);

        setJMenuBar(jMenuBarMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 843, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenActionPerformed
        if (jFileChooserProject.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            tryTo(() -> {
                project.open(jFileChooserProject.getSelectedFile(), this);
            });
        }
    }//GEN-LAST:event_jMenuItemOpenActionPerformed

    private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveActionPerformed
        tryTo(() -> {
            project.save(this);
        });
    }//GEN-LAST:event_jMenuItemSaveActionPerformed

    private void jMenuItemQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemQuitActionPerformed
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jMenuItemQuitActionPerformed

    private void jListTextIdsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListTextIdsValueChanged
        if (!evt.getValueIsAdjusting()) {
            String textId = jListTextIds.getSelectedValue();
            jTableTextContent.setModel(project.getTableModel(textId));
            jTextFieldTextName.setText(project.getName(textId));
            jCheckBoxPatch.setSelected(project.getPatch(textId));
            //jTableTextContent.getColumnModel().getColumn(0).setPreferredWidth(30);
            //jTableTextContent.getColumnModel().getColumn(0).setWidth(30);
            jTableTextContent.getColumnModel().getColumn(0).setMaxWidth(35);
            jTableTextContent.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        }
    }//GEN-LAST:event_jListTextIdsValueChanged

    private void jCheckBoxPatchItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxPatchItemStateChanged
        if (jListTextIds.getSelectedValue() != null) {
            project.setPatch(jListTextIds.getSelectedValue(), evt.getStateChange() == ItemEvent.SELECTED);
        }
    }//GEN-LAST:event_jCheckBoxPatchItemStateChanged

    private void jTextFieldTextNameKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldTextNameKeyReleased
        if (jListTextIds.getSelectedValue() != null) {
            project.setName(jListTextIds.getSelectedValue(), jTextFieldTextName.getText());
        }
    }//GEN-LAST:event_jTextFieldTextNameKeyReleased

    private void jMenuItemMailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemMailActionPerformed
        StringBuilder sb = new StringBuilder();
        sb.append("Hi Markus,\n\n\n\n");
        sb.append("My log is\n");
        sb.append(jTextAreaLog.getText());

        project.sendMail("dq4psx-patcher@markus-projects.net", getTitle(), sb.toString());
    }//GEN-LAST:event_jMenuItemMailActionPerformed

    private void jMenuItemPatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPatchActionPerformed
        tryTo(() -> {
            project.setWritePatchReport(jCheckBoxMenuItemPatchAll.isSelected());
            project.setPatchAll(jCheckBoxMenuItemPatchAll.isSelected());
            project.patch(this);
        });
    }//GEN-LAST:event_jMenuItemPatchActionPerformed

    private void jCheckBoxMenuItemPatchReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPatchReportActionPerformed

    }//GEN-LAST:event_jCheckBoxMenuItemPatchReportActionPerformed

    private void jCheckBoxMenuItemPatchAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPatchAllActionPerformed

    }//GEN-LAST:event_jCheckBoxMenuItemPatchAllActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPatchAll;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPatchReport;
    private javax.swing.JCheckBox jCheckBoxPatch;
    private javax.swing.JFileChooser jFileChooserProject;
    private javax.swing.JList<String> jListTextIds;
    private javax.swing.JMenuBar jMenuBarMain;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemMail;
    private javax.swing.JMenuItem jMenuItemOpen;
    private javax.swing.JMenuItem jMenuItemPatch;
    private javax.swing.JMenuItem jMenuItemQuit;
    private javax.swing.JMenuItem jMenuItemSave;
    private javax.swing.JMenu jMenuSettings;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelBottom;
    private javax.swing.JPanel jPanelTop;
    private javax.swing.JProgressBar jProgressBarMain;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneLog;
    private javax.swing.JScrollPane jScrollPaneTextIds;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JSplitPane jSplitPaneMain;
    private javax.swing.JSplitPane jSplitPaneTop;
    private javax.swing.JTable jTableTextContent;
    private javax.swing.JTextArea jTextAreaLog;
    private javax.swing.JTextField jTextFieldTextName;
    private javax.swing.JToolBar jToolBarTop;
    // End of variables declaration//GEN-END:variables

    /**
     * Opens the GUI.
     */
    public static void showGUI() {
        java.awt.EventQueue.invokeLater(() -> {
            new TranslatorFrame().setVisible(true);
        });
    }

    /**
     * Logs a line in the {@link #jTextAreaLog}.
     *
     * @param line
     */
    private void log(String line) {
        try {
            String ln;
            if (logTime) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                ln = LocalDateTime.now().format(formatter) + " " + line + "\n";
            } else {
                ln = line + "\n";
            }

            jTextAreaLog.getDocument().insertString(jTextAreaLog.getDocument().getLength(), ln, null);
            jTextAreaLog.setCaretPosition(jTextAreaLog.getDocument().getLength());

        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Configures the file chooser.
     */
    private void configureFileChooser() {
        //current working dir
        jFileChooserProject.setCurrentDirectory(new File("."));
        jFileChooserProject.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(".bin");
            }

            @Override
            public String getDescription() {
                return "Binary Image File for Compact Disc (*.bin)";
            }
        });

        //shortcut for me
        File folder = new File("../../Dragon Quest IV - Michibikareshi Mono Tachi (Japan)/");
        File inputFile = new File(folder, "dq4.bin");
        if (inputFile.exists()) {
            jFileChooserProject.setCurrentDirectory(folder);
            this.jFileChooserProject.setSelectedFile(inputFile);
        }
    }

    /**
     * Runs a {@link ThrowableRunnable}. If it throws an exception its stacktrace is shown in the log. This also resets the {@link #jProgressBarMain}. The exception is printed. It uses
     * {@link SwingWorker} to do it in background to prevent GUI blocking.
     *
     * @param throwableRunnable
     */
    private void tryTo(ThrowableRunnable throwableRunnable) {
        SwingWorker worker = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                try {
                    throwableRunnable.run();
                } catch (Exception e) {
                    String stacktrace = ExceptionUtils.getStackTrace(e);

                    log(stacktrace);
                    log("Something went wrong. You can report this with File > Report Log via Mail");

                    jProgressBarMain.setValue(0);
                    jProgressBarMain.setMaximum(1);
                    jProgressBarMain.setString("Error");

                    e.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }

    //progress
    @Override
    public void setProgressText(String text) {
        //SwingUtilities.invokeLater(() -> {
        jProgressBarMain.setString(text);
        log(text);
        //});
    }

    @Override
    public void setProgressMax(int max) {
        //SwingUtilities.invokeLater(() -> {
        jProgressBarMain.setMaximum(max);
        //});
    }

    @Override
    public void setProgressValue(int value) {
        //SwingUtilities.invokeLater(() -> {
        jProgressBarMain.setValue(value);
        //});
    }

}
