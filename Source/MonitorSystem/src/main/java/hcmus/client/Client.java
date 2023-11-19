package hcmus.client;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class Client extends javax.swing.JFrame {

    private Client _this;
    public static String GET_ALL_DISKS_AND_FOLDER = "GET_ALL_DISKS_AND_FOLDER";
    public static String DISCONNECT = "DISCONNECT";
    public static String SEND_MONITORED_FOLDER = "SEND_MONITORED_FOLDER";

    public static String CREATE_FILE = "CREATE_FILE";
    public static String DELETE_FILE = "DELETE_FILE";
    public static String MODIFY_FILE = "MODIFY_FILE";

    public static String CREATE_FOLDER = "CREATE_FOLDER";
    public static String DELETE_FOLDER = "DELETE_FOLDER";
    public static String MODIFY_FOLDER = "MODIFY_FOLDER";

    private Socket client;
    private DataOutputStream dout = null;
    private DataInputStream din = null;
    private boolean isConnecting = false;
    private boolean inactiveConnection = false;

    public Client() throws IOException {
        initComponents();
        _this = this;
        init();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (isConnecting) {
                    try {
                        dout.writeUTF(Client.DISCONNECT);
                        dout.flush();
                        client.close();
                    } catch (IOException ex) {
                        System.out.println("Client.java, row: 276, ERROR: " + ex);
                    }
                }
            }

        });
    }

    public final void init() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        hostInput.setText(localHost.getHostAddress());
    }

    public final void startClientSocket(String host, int port) throws IOException {
        try {
            client = new Socket(host, port);
            client.setTcpNoDelay(true);
            isConnecting = true;
            inactiveConnection = false;

            dout = new DataOutputStream(client.getOutputStream());
            din = new DataInputStream(client.getInputStream());

            String sendMsg = "", receiveMsg;
            statusLabel.setText("Kết nối thành công");

            while (true) {
                receiveMsg = din.readUTF();
                if (receiveMsg.equals(Client.GET_ALL_DISKS_AND_FOLDER)) {
                    File[] disks;
                    dout.writeUTF(receiveMsg);

                    // returns pathnames for disks
                    disks = File.listRoots();
                    int numberOfDisk = disks.length - 1;

                    // for each pathname in pathname array
                    dout.writeUTF(numberOfDisk + "");
                    dout.flush();

                    for (File disk : disks) {
                        if (!disk.getPath().equals("C:\\")) {
                            sendDirectory(disk, dout);
                        }
                    }

                } else if (receiveMsg.equals(Client.SEND_MONITORED_FOLDER)) {
                    String folderPath = din.readUTF();

                    WatchService watcher = FileSystems.getDefault().newWatchService();
                    Path monitoredPath = Path.of(folderPath);

                    WatchKey key = monitoredPath.register(watcher,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);

                    while (true) {

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                            Path fileName = pathEvent.context();

                            WatchEvent.Kind<?> kind = event.kind();

                            boolean isDirectory = Files.isDirectory(Path.of(folderPath).resolve(fileName));
                            String type = "";

                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                if (isDirectory) {
                                    type = CREATE_FOLDER;
                                    System.out.println("A new folder is created : " + fileName);
                                } else {
                                    type = CREATE_FILE;
                                    System.out.println("A new file is created : " + fileName);
                                }

                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                // When deleting a file or folder, we cann't check if it is a folder or file because
                                // it is deleted so i use a trick that file'name often has dot (.). Ex: data.txt or data.docx
                                // I know that trick isn't perfect but I don't know whatever way to do

                                if (isDirectory || !fileName.toString().contains(".")) {
                                    type = DELETE_FOLDER;
                                    System.out.println("A new folder is deleted : " + fileName);
                                } else {
                                    type = DELETE_FILE;
                                    System.out.println("A new file is deleted : " + fileName);
                                }
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                if (isDirectory) {
                                    type = MODIFY_FOLDER;
                                    System.out.println("A new folder is modified : " + fileName);
                                } else {
                                    type = MODIFY_FILE;
                                    System.out.println("A new file is modified : " + fileName);
                                }
                            }

                            sendMsg = "MONITOR - " + type + " - " + fileName.toString();
                        }

                        if (!sendMsg.equals("")) {
                            dout.writeUTF(sendMsg);
                            dout.flush();
                            sendMsg = "";
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                    }
                }
            }

        } catch (IOException ex) {
            System.out.println(ex);
            statusLabel.setText("Kết nối thất bại!");
            if (!inactiveConnection) {
                JOptionPane.showMessageDialog(_this,
                        "Vui lòng kiểm tra IP và Port server đúng không?\nHoặc có thể server chưa hoạt động\nHoặc bạn bị server ngắt kết nối",
                        "Lỗi kết nối",
                        JOptionPane.WARNING_MESSAGE);
            }

        } finally {
            if (dout != null && din != null && client != null) {
                dout.close();
                din.close();
                client.close();
                isConnecting = false;
            }
        }
    }

    private void sendDirectory(File directory, DataOutputStream dout) throws IOException {
        if (directory.isDirectory()) {
            // Send folder'name to server
            int length = directory.getPath().length();

            if (length == 3) {
                dout.writeUTF(directory.getPath());
            } else {
                dout.writeUTF(directory.getName());
            }

            // Get all subfolders and files in current folder to server
            File[] files = directory.listFiles();
            int numberOfDirectory = 0;

            // Count the number of subfolder
            for (File file : files) {
                // Check if it file is a folder?
                if (file.isDirectory()) {
                    if (!file.getPath().contains("RECYCLE.BIN")
                            && !file.getPath().contains("System Volume Information")) {
                        numberOfDirectory++;
                    }
                }
            }

            dout.writeInt(numberOfDirectory);
            dout.flush();

            if (numberOfDirectory > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        if (!file.getPath().contains("RECYCLE.BIN")
                                && !file.getPath().contains("System Volume Information")) {
                            sendDirectory(file, dout);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        hostInput = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        portInput = new javax.swing.JTextField();
        disconnectBtn = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        statusLabel = new javax.swing.JLabel();
        connectBtn1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Client");

        jPanel1.setBackground(new java.awt.Color(204, 255, 204));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Client");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel4.setText("Nhập IP Server:");

        hostInput.setText("192.168.137.1");
        hostInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hostInputActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("Kết nối đến Server để giám sát");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel6.setText("Nhập Port:");

        portInput.setText("3000");
        portInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portInputActionPerformed(evt);
            }
        });

        disconnectBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        disconnectBtn.setText("Ngắt kết nối");
        disconnectBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnectBtnActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel7.setText("Trạng thái");

        statusLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        statusLabel.setForeground(new java.awt.Color(255, 0, 0));
        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        statusLabel.setText("Chưa kết nối");

        connectBtn1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        connectBtn1.setText("Kết nối");
        connectBtn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectBtn1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(169, 169, 169)
                        .addComponent(jLabel1)
                        .addContainerGap(203, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
                        .addComponent(jLabel7)
                        .addGap(70, 70, 70))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(portInput, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(hostInput, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24))))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addComponent(disconnectBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(connectBtn1)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(hostInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(statusLabel))
                .addGap(29, 29, 29)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(portInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(disconnectBtn)
                    .addComponent(connectBtn1))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void hostInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hostInputActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_hostInputActionPerformed

    private void portInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portInputActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_portInputActionPerformed

    private void disconnectBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disconnectBtnActionPerformed
        // TODO add your handling code here:
        if (isConnecting) {
            try {
                dout.writeUTF(Client.DISCONNECT);
                dout.flush();
                Thread.sleep(500);
                client.close();
            } catch (IOException e) {
                System.out.println("Client.java, row: 276, ERROR: " + e);
            } catch (InterruptedException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            isConnecting = false;
            inactiveConnection = true;

        } else {
            JOptionPane.showMessageDialog(this,
                    "Bạn chưa kết nối tới server nào!",
                    "Chưa kết nối!",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_disconnectBtnActionPerformed

    private void connectBtn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectBtn1ActionPerformed
        // TODO add your handling code here:
        if (!isConnecting) {
            String host = hostInput.getText();
            int port = Integer.parseInt(portInput.getText());

            if (host.length() > 0 && port > 0) {
                Thread socketThread = new Thread(() -> {
                    try {
                        startClientSocket(host, port);
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                });
                socketThread.start();

            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Bạn đang kết nối tới server",
                    "Đang kết nối!",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_connectBtn1ActionPerformed

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
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Client().setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton connectBtn1;
    private javax.swing.JButton disconnectBtn;
    private javax.swing.JTextField hostInput;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField portInput;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables
}
