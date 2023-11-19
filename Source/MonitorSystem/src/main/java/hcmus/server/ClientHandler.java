package hcmus.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import javax.swing.tree.DefaultMutableTreeNode;

public class ClientHandler implements Runnable {

    private ChooseFolder folderView;
    private Server serverView;

    private final Socket client;
    private final DataInputStream din;
    private final DataOutputStream dout;
    private String directoryPath = "";

    private String command = "";
    public static String GET_ALL_DISKS_AND_FOLDER = "GET_ALL_DISKS_AND_FOLDER";
    public static String DISCONNECT = "DISCONNECT";
    public static String SEND_MONITORED_FOLDER = "SEND_MONITORED_FOLDER";

    public static String CREATE_FILE = "CREATE_FILE";
    public static String DELETE_FILE = "DELETE_FILE";
    public static String MODIFY_FILE = "MODIFY_FILE";

    public static String CREATE_FOLDER = "DELETE_FILE";
    public static String DELETE_FOLDER = "DELETE_FILE";
    public static String MODIFY_FOLDER = "MODIFY_FOLDER";

    public ClientHandler(Socket clientSocket) throws IOException {
        this.client = clientSocket;
        this.din = new DataInputStream(clientSocket.getInputStream());
        this.dout = new DataOutputStream(clientSocket.getOutputStream());
    }

    public Socket getClient() {
        return client;
    }

    public void setFolderView(ChooseFolder view) {
        this.folderView = view;
    }

    public void setServerView(Server serverView) {
        this.serverView = serverView;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getMonitoredDirectory() {
        return this.directoryPath;
    }

    private DefaultMutableTreeNode createTreeFromStream() throws IOException {
        String name = din.readUTF();
        int numChildren = din.readInt();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
        for (int i = 0; i < numChildren; i++) {
            node.add(createTreeFromStream());
        }
        return node;
    }

    @Override
    public void run() {
        String receiveMsg = "";
        String sendMsg = "";

        try {
            while (true) {
                // Send to clients
                if (!command.equals("")) {
                    if (command.equals(ClientHandler.GET_ALL_DISKS_AND_FOLDER)) {
                        sendMsg = ClientHandler.GET_ALL_DISKS_AND_FOLDER;
                        dout.writeUTF(sendMsg);
                        dout.flush();
                    } else if (command.equals(ClientHandler.SEND_MONITORED_FOLDER)) {
                        directoryPath = folderView.getDirectoryPath();
                        sendMsg = directoryPath;
                        dout.writeUTF(ClientHandler.SEND_MONITORED_FOLDER);
                        dout.writeUTF(sendMsg);
                        dout.flush();
                    }
                    command = "";
                }

                // Receive from clients
                if (din.available() > 0) {
                    receiveMsg = din.readUTF();

                    if (receiveMsg.equals(ClientHandler.GET_ALL_DISKS_AND_FOLDER)) {
                        int numberOfDisk = Integer.parseInt(din.readUTF());

                        DefaultMutableTreeNode root = new DefaultMutableTreeNode("This PC");

                        for (int i = 1; i <= numberOfDisk; i++) {
                            root.add(createTreeFromStream());
                        }
                        this.folderView.createTree(root);
                    } else if (receiveMsg.equals(ClientHandler.DISCONNECT)) {
                        this.serverView.removeClientBySocket(client);
                    } else if (receiveMsg.contains("MONITOR")) {
                        String[] tokens = receiveMsg.split(" - ");
                        Action newAction = new Action(client.getInetAddress().toString(),
                                client.getPort(),
                                tokens[1],
                                tokens[1] + " " + tokens[2], new Date());

                        serverView.addActionToTable(newAction);
                    }
                }

            }
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            try {
                System.out.println("Client " + this.client.getPort() + " disconnected!");
                this.client.close();
                this.din.close();
                this.din.close();
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }
}
