/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mysh.imagesearch;

/**
 *
 * @author allen
 */
public class SiftHelperTestGUI extends javax.swing.JPanel {

    public interface SiftHelperTestGUIInjecter {

        void onPrepare(SiftHelperTestGUI s);

        void onSearch(SiftHelperTestGUI s);
    }

    /**
     * Creates new form SiftTest
     */
    public SiftHelperTestGUI() {
        initComponents();
    }

    SiftHelperTestGUIInjecter injecter;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        sampleDir = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        resultDir = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        samplePx = new javax.swing.JSpinner();
        targetPx = new javax.swing.JSpinner();
        jScrollPane1 = new javax.swing.JScrollPane();
        output = new javax.swing.JTextPane();
        prepareBtn = new javax.swing.JButton();
        target = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        searchBtn = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        eachFeatureTimeout = new javax.swing.JSpinner();
        clrOutput = new javax.swing.JButton();

        setFont(getFont());

        jLabel1.setFont(jLabel1.getFont());
        jLabel1.setText("样本库目录");

        jLabel2.setText("输出目录");

        jLabel3.setText("样本像素尺度");

        jLabel4.setText("目标像素尺度");

        samplePx.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(180), Integer.valueOf(10), null, Integer.valueOf(5)));

        targetPx.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(150), Integer.valueOf(10), null, Integer.valueOf(5)));

        jScrollPane1.setViewportView(output);

        prepareBtn.setText("准备引擎");
        prepareBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prepareBtnActionPerformed(evt);
            }
        });

        target.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                targetActionPerformed(evt);
            }
        });

        jLabel5.setText("目标文件");

        searchBtn.setText("查找");
        searchBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchBtnActionPerformed(evt);
            }
        });

        jLabel6.setText("特征值搜索超时(微秒)");

        eachFeatureTimeout.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(2500), Integer.valueOf(1000), null, Integer.valueOf(500)));

        clrOutput.setText("清空");
        clrOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clrOutputActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(target)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clrOutput))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sampleDir)
                            .addComponent(resultDir)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(samplePx, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(targetPx, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(eachFeatureTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(prepareBtn)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(sampleDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resultDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(samplePx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(targetPx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(eachFeatureTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prepareBtn))
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(target, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchBtn)
                    .addComponent(jLabel5)
                    .addComponent(clrOutput))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void prepareBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prepareBtnActionPerformed
        if (injecter != null) {
            injecter.onPrepare(this);
        }
    }//GEN-LAST:event_prepareBtnActionPerformed

    private void searchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchBtnActionPerformed
        if (injecter != null) {
            clrOutputActionPerformed(evt);
            injecter.onSearch(this);
        }
    }//GEN-LAST:event_searchBtnActionPerformed

    private void clrOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clrOutputActionPerformed
        this.output.setText("");
    }//GEN-LAST:event_clrOutputActionPerformed

    private void targetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_targetActionPerformed
        searchBtnActionPerformed(evt);
    }//GEN-LAST:event_targetActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clrOutput;
    javax.swing.JSpinner eachFeatureTimeout;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    javax.swing.JTextPane output;
    private javax.swing.JButton prepareBtn;
    javax.swing.JTextField resultDir;
    javax.swing.JTextField sampleDir;
    javax.swing.JSpinner samplePx;
    javax.swing.JButton searchBtn;
    javax.swing.JTextField target;
    javax.swing.JSpinner targetPx;
    // End of variables declaration//GEN-END:variables
}