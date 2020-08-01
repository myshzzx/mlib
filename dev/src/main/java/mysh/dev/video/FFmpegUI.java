/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mysh.dev.video;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

/**
 *
 * @author mysh
 */
public class FFmpegUI extends javax.swing.JFrame {

    private FFmpegUIController controller = new FFmpegUIController(this);

    private void taskStart() {
        splitBtn.setEnabled(false);
        mergeBtn.setEnabled(false);
        h265Btn.setEnabled(false);
        subtitleBtn.setEnabled(false);
        stopBtn.setEnabled(true);
    }

    private void taskComplete() {
        splitBtn.setEnabled(true);
        mergeBtn.setEnabled(true);
        h265Btn.setEnabled(true);
        subtitleBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    /**
     * Creates new form FfmpegUI
     */
    public FFmpegUI() {
        initComponents();
        targetText.setLabel("target file/dir");
        tempText.setLabel("temp dir");
        fromText.setLabel("from time, split as U like");
        toText.setLabel("to time, split as U like");

        DropTargetAdapter fileDropListener = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    Transferable tr = dtde.getTransferable();
                    Component target = dtde.getDropTargetContext().getDropTarget().getComponent();
                    JTextComponent jText;
                    if (target instanceof JTextComponent) {
                        jText = (JTextComponent) target;
                    } else
                        return;

                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        List files = (List) (tr.getTransferData(DataFlavor.javaFileListFlavor));
                        for (Object fo : files) {
                            File f = (File) fo;
                            if(jText instanceof JTextField)
                                jText.setText(f.getAbsolutePath());
                            else
                                jText.setText(jText.getText() + f.getAbsolutePath() + '\n');
                        }
                        dtde.dropComplete(true);
                    } else if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        String text = (String) tr.getTransferData(DataFlavor.stringFlavor);
                        if(jText instanceof JTextField)
                            jText.setText(text);
                        else
                            jText.setText(jText.getText() + text + '\n');
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new DropTarget(srcText, DnDConstants.ACTION_COPY_OR_MOVE, fileDropListener);
        new DropTarget(targetText, DnDConstants.ACTION_COPY_OR_MOVE, fileDropListener);
        new DropTarget(tempText, DnDConstants.ACTION_COPY_OR_MOVE, fileDropListener);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        srcText = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        splitBtn = new javax.swing.JButton();
        mergeBtn = new javax.swing.JButton();
        h265Btn = new javax.swing.JButton();
        stopBtn = new javax.swing.JButton();
        subtitleBtn = new javax.swing.JButton();
        targetText = new mysh.ui.JTextFieldWithTips();
        overwriteChk = new javax.swing.JCheckBox();
        hwAccelChk = new javax.swing.JCheckBox();
        monoChk = new javax.swing.JCheckBox();
        crfSpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        fromText = new mysh.ui.JTextFieldWithTips();
        toText = new mysh.ui.JTextFieldWithTips();
        opusKpsSpinner = new javax.swing.JSpinner();
        copyAudioChk = new javax.swing.JCheckBox();
        opusKpsChk = new javax.swing.JCheckBox();
        frameRateChk = new javax.swing.JCheckBox();
        frameRateSpinner = new javax.swing.JSpinner();
        tempText = new mysh.ui.JTextFieldWithTips();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jScrollPane1.setViewportBorder(javax.swing.BorderFactory.createTitledBorder("src files"));

        srcText.setColumns(20);
        srcText.setRows(5);
        jScrollPane1.setViewportView(srcText);

        splitBtn.setText("split");
        splitBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                splitBtnActionPerformed(evt);
            }
        });

        mergeBtn.setText("merge");
        mergeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mergeBtnActionPerformed(evt);
            }
        });

        h265Btn.setText("h265");
        h265Btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                h265BtnActionPerformed(evt);
            }
        });

        stopBtn.setText("STOP");
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopBtnActionPerformed(evt);
            }
        });

        subtitleBtn.setText("subtitle");
        subtitleBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subtitleBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splitBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(mergeBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(h265Btn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(subtitleBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(stopBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(stopBtn, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(h265Btn, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE)
                        .addComponent(subtitleBtn, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE))
                    .addComponent(mergeBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(splitBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        targetText.setToolTipText("");

        overwriteChk.setText("over write");

        hwAccelChk.setSelected(true);
        hwAccelChk.setText("hw accelerate");

        monoChk.setText("mono");

        crfSpinner.setModel(new javax.swing.SpinnerNumberModel(24, 0, 51, 1));
        crfSpinner.setToolTipText("0:lossless, 51:worst");

        jLabel1.setText("h265-crf =");

        opusKpsSpinner.setValue(128);

        copyAudioChk.setText("copy audio");

        opusKpsChk.setText("opus.kps=");

        frameRateChk.setText("frame rate=");

        frameRateSpinner.setValue(24);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(targetText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tempText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(overwriteChk)
                                .addGap(18, 18, 18)
                                .addComponent(hwAccelChk)
                                .addGap(18, 18, 18)
                                .addComponent(copyAudioChk)
                                .addGap(18, 18, 18)
                                .addComponent(monoChk)
                                .addGap(18, 18, 18)
                                .addComponent(opusKpsChk)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(opusKpsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(crfSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(fromText, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(toText, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(frameRateChk)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(frameRateSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 43, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(toText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(crfSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(fromText, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(frameRateChk)
                        .addComponent(frameRateSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(overwriteChk)
                    .addComponent(hwAccelChk)
                    .addComponent(monoChk)
                    .addComponent(opusKpsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(copyAudioChk)
                    .addComponent(opusKpsChk))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(targetText, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                    .addComponent(tempText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void splitBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_splitBtnActionPerformed
        controller.split(srcText.getText(), targetText.getText(),tempText.getText(),
                fromText.getText(), toText.getText(),
                overwriteChk.isSelected(), hwAccelChk.isSelected(),
                this::taskStart, this::taskComplete
        );
    }//GEN-LAST:event_splitBtnActionPerformed

    private void h265BtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h265BtnActionPerformed
        controller.h265(srcText.getText(), targetText.getText(), tempText.getText(),
                fromText.getText(), toText.getText(), (Integer) crfSpinner.getValue(),
                overwriteChk.isSelected(), hwAccelChk.isSelected(),
                frameRateChk.isSelected(), (Integer) frameRateSpinner.getValue(),
                copyAudioChk.isSelected(), monoChk.isSelected(), opusKpsChk.isSelected(),
                (Integer) opusKpsSpinner.getValue(),
                this::taskStart, this::taskComplete
        );
    }//GEN-LAST:event_h265BtnActionPerformed

    private void mergeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mergeBtnActionPerformed
        controller.merge(srcText.getText(), targetText.getText(),
                overwriteChk.isSelected(), hwAccelChk.isSelected(),
                this::taskStart, this::taskComplete);
    }//GEN-LAST:event_mergeBtnActionPerformed

    private void stopBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopBtnActionPerformed
        controller.stop();
    }//GEN-LAST:event_stopBtnActionPerformed

    private void subtitleBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subtitleBtnActionPerformed
        controller.subtitle(srcText.getText(), targetText.getText(), 
                overwriteChk.isSelected(),
                this::taskStart, this::taskComplete);
    }//GEN-LAST:event_subtitleBtnActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(FFmpegUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(FFmpegUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(FFmpegUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(FFmpegUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FFmpegUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox copyAudioChk;
    private javax.swing.JSpinner crfSpinner;
    private javax.swing.JCheckBox frameRateChk;
    private javax.swing.JSpinner frameRateSpinner;
    private mysh.ui.JTextFieldWithTips fromText;
    private javax.swing.JButton h265Btn;
    private javax.swing.JCheckBox hwAccelChk;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton mergeBtn;
    private javax.swing.JCheckBox monoChk;
    private javax.swing.JCheckBox opusKpsChk;
    private javax.swing.JSpinner opusKpsSpinner;
    private javax.swing.JCheckBox overwriteChk;
    private javax.swing.JButton splitBtn;
    private javax.swing.JTextArea srcText;
    private javax.swing.JButton stopBtn;
    private javax.swing.JButton subtitleBtn;
    private mysh.ui.JTextFieldWithTips targetText;
    private mysh.ui.JTextFieldWithTips tempText;
    private mysh.ui.JTextFieldWithTips toText;
    // End of variables declaration//GEN-END:variables
}
