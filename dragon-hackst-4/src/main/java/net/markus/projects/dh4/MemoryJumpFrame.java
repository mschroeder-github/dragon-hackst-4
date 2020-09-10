package net.markus.projects.dh4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.markus.projects.dh4.util.Utils;

/**
 *
 */
public class MemoryJumpFrame extends javax.swing.JFrame {

    private HBD1PS1D hbd1ps1d;
    private PsxJisReader reader;
    
    public MemoryJumpFrame(HBD1PS1D hbd1ps1d, PsxJisReader reader) {
        this.hbd1ps1d = hbd1ps1d;
        this.reader = reader;
        initComponents();
        setLocationRelativeTo(null);
        jButtonJumpActionPerformed(null);
    }

    private void jump(int start, int len) {
        byte[] data = Arrays.copyOfRange(hbd1ps1d.data, start, start+len);
        
        List<String> l = new ArrayList<>();
        
        String nullStr = "?";
        StringBuilder sb = new StringBuilder();
        
        for(int i = 0; i < data.length-1; i += 2) {
            byte[] elem = new byte[] { data[i], data[i+1] };
            //Byte[] elemObj = Utils.toObjects(elem);
            short s = Utils.bytesToShort(elem);
            
            String str = Utils.bytesToHex(elem);
            Character c = reader.sjishort2char.get(s);
            
            String charStr = "" + c;
            if(s == 0) {
                charStr = "\\0";
            }
            
            
            
            if(c != null)
                sb.append(c);
            else
                sb.append(nullStr);
            
            l.add((start + i) + " " + str + " " + charStr + " " + (c == null ? "" + s : ""));
        }
        
        this.jListData.setListData(l.toArray(new String[0]));
        jTextAreaEditor.setText(sb.toString());
        jTextAreaEditor.setCaretPosition(0);
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextFieldStart = new javax.swing.JTextField();
        jTextFieldLen = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListData = new javax.swing.JList<>();
        jButtonJump = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaEditor = new javax.swing.JTextArea();
        jButtonUp = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTextFieldStart.setText("38345668");

        jTextFieldLen.setText("1000");

        jScrollPane1.setViewportView(jListData);

        jButtonJump.setText("Jump");
        jButtonJump.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonJumpActionPerformed(evt);
            }
        });

        jTextAreaEditor.setColumns(20);
        jTextAreaEditor.setLineWrap(true);
        jTextAreaEditor.setRows(5);
        jScrollPane2.setViewportView(jTextAreaEditor);

        jButtonUp.setText("Up");
        jButtonUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextFieldStart, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldLen, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonJump)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonUp))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 419, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldLen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonJump)
                    .addComponent(jButtonUp))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonJumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonJumpActionPerformed
        jump(Integer.parseInt(this.jTextFieldStart.getText()), Integer.parseInt(this.jTextFieldLen.getText()));
    }//GEN-LAST:event_jButtonJumpActionPerformed

    private void jButtonUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUpActionPerformed
        String v = String.valueOf(Integer.parseInt(this.jTextFieldStart.getText()) - 2);
        this.jTextFieldStart.setText(v);
        jump(Integer.parseInt(this.jTextFieldStart.getText()), Integer.parseInt(this.jTextFieldLen.getText()));
    }//GEN-LAST:event_jButtonUpActionPerformed

    public static void showGUI(HBD1PS1D hbd1ps1d, PsxJisReader reader) {
        java.awt.EventQueue.invokeLater(() -> {
            new MemoryJumpFrame(hbd1ps1d, reader).setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonJump;
    private javax.swing.JButton jButtonUp;
    private javax.swing.JList<String> jListData;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextAreaEditor;
    private javax.swing.JTextField jTextFieldLen;
    private javax.swing.JTextField jTextFieldStart;
    // End of variables declaration//GEN-END:variables
}
