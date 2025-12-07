
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.awt.datatransfer.DataFlavor;
import java.util.List;

import javax.swing.TransferHandler;
import javax.swing.JFileChooser;
import javax.swing.BorderFactory;

public class Client2025 extends JFrame implements ActionListener, KeyListener {
    private static final long serialVersionUID = 2L;

    // Login GUI
    private JFrame loginGUI = new JFrame("로그인");
    private JPanel loginJpanel;
    private JTextField serverIP_tf;
    private JTextField serverPort_tf;
    private JTextField clientID_tf;
    private JLabel img_Label;
    private JButton loginBtn;
    private String serverIP;
    private int serverPort;
    private String clientID;

    // Main GUI
    private JPanel contentPane;
    private JList<String> clientJlist = new JList<>();
    private JList<String> roomJlist = new JList<>();
    private JTextField msg_tf;
    private JTextArea chatArea = new JTextArea();
    private JButton noteBtn = new JButton("쪽지 보내기");
    private JButton joinRoomBtn = new JButton("채팅방 참여");
    private JButton createRoomBtn = new JButton("방 만들기");
    private JButton sendBtn = new JButton("전송");
    private JButton exitRoomBtn = new JButton("채팅방 나가기");
    private JButton clientExitBtn = new JButton("프로그램 종료");
    private JPanel fileDropPanel;

    // 상태
    private Vector<String> clientVC = new Vector<>();
    private Vector<String> roomClientVC = new Vector<>();
    private String myRoomID = "";

    // Network
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean socketEstablished = false;
    private StringTokenizer st;

    public Client2025() {
        initializeLoginGUI();
        initializeMainGUI();
        addActionListeners();
    }

    // ===== 로그인 GUI =====
    void initializeLoginGUI() {
        loginGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginGUI.setBounds(270, 100, 386, 542);
        loginJpanel = new JPanel();
        loginJpanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        loginGUI.setContentPane(loginJpanel);
        loginJpanel.setLayout(null);

        JLabel title_lb = new JLabel("로그인 창");
        title_lb.setFont(new Font("굴림", Font.PLAIN, 16));
        title_lb.setBounds(24, 14, 100, 40);
        loginJpanel.add(title_lb);

        JLabel serverIP_lb = new JLabel("서버 IP");
        serverIP_lb.setBounds(12, 200, 57, 15);
        loginJpanel.add(serverIP_lb);

        serverIP_tf = new JTextField("127.0.0.1");
        serverIP_tf.setBounds(71, 197, 285, 21);
        loginJpanel.add(serverIP_tf);

        JLabel serverPort_lb = new JLabel("서버 Port");
        serverPort_lb.setBounds(12, 239, 57, 15);
        loginJpanel.add(serverPort_lb);

        serverPort_tf = new JTextField("12345");
        serverPort_tf.setBounds(71, 236, 285, 21);
        loginJpanel.add(serverPort_tf);

        JLabel clientID_lb = new JLabel("사용자 ID");
        clientID_lb.setBounds(12, 291, 57, 15);
        loginJpanel.add(clientID_lb);

        clientID_tf = new JTextField("아이디입력");
        clientID_tf.setBounds(71, 288, 285, 21);
        loginJpanel.add(clientID_tf);

        loginBtn = new JButton("접속");
        loginBtn.setBounds(12, 450, 344, 44);
        loginJpanel.add(loginBtn);

        try {
            ImageIcon im = new ImageIcon("images/다람쥐.jpg");
            img_Label = new JLabel(im);
            img_Label.setBounds(12, 23, 344, 154);
            loginJpanel.add(img_Label);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "이미지 로딩 중 오류 발생.", "오류", JOptionPane.ERROR_MESSAGE);
        }

        loginGUI.setVisible(true);
    }

    // ===== 메인 GUI =====
    void initializeMainGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(600, 100, 550, 550); // 조금 크게
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        // 왼쪽 영역: 접속자/방 목록
        JLabel 접속자 = new JLabel("전체 접속자");
        접속자.setBounds(12, 10, 100, 15);
        contentPane.add(접속자);

        clientJlist.setBounds(12, 30, 120, 140);
        contentPane.add(clientJlist);

        noteBtn.setBounds(12, 175, 120, 25);
        contentPane.add(noteBtn);

        JLabel 채팅방 = new JLabel("채팅방");
        채팅방.setBounds(12, 210, 57, 15);
        contentPane.add(채팅방);

        roomJlist.setBounds(12, 230, 120, 140);
        contentPane.add(roomJlist);

        joinRoomBtn.setBounds(12, 380, 120, 25);
        contentPane.add(joinRoomBtn);

        exitRoomBtn.setBounds(12, 410, 120, 25);
        contentPane.add(exitRoomBtn);

        createRoomBtn.setBounds(12, 440, 120, 25);
        contentPane.add(createRoomBtn);

        clientExitBtn.setBounds(12, 470, 120, 25);
        contentPane.add(clientExitBtn);

        // 오른쪽: 채팅 영역
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(150, 10, 370, 320);
        contentPane.add(scrollPane);
        scrollPane.setViewportView(chatArea);
        chatArea.setEditable(false);

        msg_tf = new JTextField();
        msg_tf.setBounds(150, 340, 260, 25);
        contentPane.add(msg_tf);
        msg_tf.setEditable(false);

        sendBtn.setBounds(420, 340, 100, 25);
        contentPane.add(sendBtn);
        sendBtn.setEnabled(false);

        // 파일 드래그&드롭 영역
        fileDropPanel = new JPanel();
        fileDropPanel.setBorder(BorderFactory.createTitledBorder("여기로 파일을 드래그해서 전송"));
        fileDropPanel.setBounds(150, 380, 370, 115);
        fileDropPanel.setTransferHandler(new FileDropHandler());
        contentPane.add(fileDropPanel);

        // 초기 버튼 상태
        joinRoomBtn.setEnabled(false);   // 방이 없으니 참여 불가
        exitRoomBtn.setEnabled(false);   // 아직 방에 안 들어감
        createRoomBtn.setEnabled(true);  // 방은 만들 수 있음

        this.setVisible(false); // 로그인 성공 후에만 보이게
    }

    void addActionListeners() {
        loginBtn.addActionListener(this);
        noteBtn.addActionListener(this);
        joinRoomBtn.addActionListener(this);
        createRoomBtn.addActionListener(this);
        sendBtn.addActionListener(this);
        exitRoomBtn.addActionListener(this);
        clientExitBtn.addActionListener(this);
        msg_tf.addKeyListener(this);
    }

    // ===== 서버 접속 =====
    void connectToServer() {
        if (!validateLoginInputs()) return;

        try {
            socket = new Socket(serverIP, serverPort);
            if (socket.isConnected()) {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
                socketEstablished = true;

                dos.writeUTF(clientID);
                dos.flush();

                InitializeAndRecvMsg();
            }
        } catch (IOException e) {
            handleConnectionError(e);
        }
    }

    private boolean validateLoginInputs() {
        serverIP = serverIP_tf.getText().trim();
        if (serverIP.isEmpty()) {
            showErrorMessage("서버 IP를 입력해주세요.", "입력 오류");
            return false;
        }

        try {
            serverPort = Integer.parseInt(serverPort_tf.getText().trim());
        } catch (NumberFormatException e) {
            showErrorMessage("포트 번호는 숫자여야 합니다.", "입력 오류");
            serverPort_tf.setText("");
            return false;
        }

        clientID = clientID_tf.getText().trim();
        if (clientID.isEmpty()) {
            showErrorMessage("사용자 ID를 입력해주세요.", "입력 오류");
            return false;
        }
        return true;
    }

    private void handleConnectionError(IOException e) {
        String errorMessage = e.getMessage();
        if (errorMessage.contains("Connection refused")) {
            showErrorMessage("서버가 실행 중이 아닙니다. 서버를 먼저 실행해주세요.", "연결 오류");
        } else if (errorMessage.contains("Connection timed out")) {
            showErrorMessage("서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.", "연결 시간 초과");
        } else {
            showErrorMessage("서버 연결 중 알 수 없는 오류가 발생했습니다.", "연결 오류");
        }
    }

    void InitializeAndRecvMsg() {
        this.setVisible(true);
        this.loginGUI.setVisible(false);

        setTitle("사용자: " + clientID);

        // 서버로부터 메시지 계속 받는 스레드
        new Thread(() -> {
            try {
                String msg;
                while (true) {
                    msg = dis.readUTF();
                    parseMsg(msg);
                }
            } catch (IOException e) {
                handleServerShutdown();
            }
        }).start();
    }

    void sendMsg(String msg) {
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            showErrorMessage("메시지 전송 중 오류가 발생했습니다.", "오류");
        }
    }

    private void handleServerShutdown() {
        appendToChatArea("서버가 종료되었거나 연결이 끊겼습니다.");
        closeConnections();
    }

    private void closeConnections() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            showErrorMessage("연결 종료 중 오류가 발생했습니다.", "오류");
        }
    }

    // ===== 프로토콜 처리 =====
    void parseMsg(String msg) {
        st = new StringTokenizer(msg, "/");
        String protocol = st.nextToken();
        String message = st.hasMoreTokens() ? st.nextToken() : "";

        switch (protocol) {
            case "NewClient":
            case "OldClient":
                addClientToList(message);
                break;

            case "Note":
                String note = st.nextToken();
                showMessageBox(note, message + "님으로부터 쪽지");
                break;

            case "CreateRoom":
                handleCreateRoomSuccess(message);
                break;

            case "NewRoom":
            case "OldRoom":
                handleAddRoomJlist(message);
                break;

            case "CreateRoomFail":
                showErrorMessage("방 만들기 실패: " + message, "알림");
                break;

            case "JoinRoomMsg":
                String msg2 = st.nextToken();
                appendToChatArea(message + ": " + msg2);
                break;

            case "JoinRoom":
                handleJoinRoom(message);
                break;

            case "File":
                handleFileReceive(message, st);
                break;

            case "SendMsg":
                String chatMsg = st.nextToken();
                appendToChatArea(message + "님: " + chatMsg);
                break;

            case "ClientJlistUpdate":
                refreshClientJList();
                break;

            case "RoomJlistUpdate":
                refreshRoomJlist();
                break;

            case "ClientExit":
                removeClientFromJlist(message);
                break;

            case "ExitRoom":
                handleExitRoom(message);
                break;

            case "RoomOut":
                handleRoomOut(message);
                break;

            case "ServerExit":
            case "ServerStop":
                JOptionPane.showMessageDialog(null, "서버가 중지되었습니다. 클라이언트 프로그램을 종료합니다.", "알림",
                        JOptionPane.WARNING_MESSAGE);
                System.exit(0);
                break;

            case "ExitRoomMsg":
                String exitMsg = st.nextToken();
                appendToChatArea(message + ": " + exitMsg);
                break;
        }
    }

    private void showMessageBox(String msg, String title) {
        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.CLOSED_OPTION);
    }

    private void showErrorMessage(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void appendToChatArea(String message) {
        chatArea.append(message + "\n");
    }

    // ===== 리스트 / 상태 관련 메소드 =====

    private void addClientToList(String id) {
        if (!clientVC.contains(id)) clientVC.add(id);
        clientJlist.setListData(clientVC);
    }

    private void refreshClientJList() {
        clientJlist.setListData(clientVC);
    }

    private void handleCreateRoomSuccess(String roomName) {
        myRoomID = roomName;
        joinRoomBtn.setEnabled(false);
        createRoomBtn.setEnabled(false);
        exitRoomBtn.setEnabled(true);
        msg_tf.setEditable(true);
        sendBtn.setEnabled(true);
        setTitle("사용자: " + clientID + " | 채팅방: " + myRoomID);
    }

    private void handleAddRoomJlist(String roomName) {
        if (!roomClientVC.contains(roomName)) roomClientVC.add(roomName);
        roomJlist.setListData(roomClientVC);
        if (!roomClientVC.isEmpty() && (myRoomID == null || myRoomID.isEmpty())) {
            joinRoomBtn.setEnabled(true);
        }
    }

    private void refreshRoomJlist() {
        roomJlist.setListData(roomClientVC);
        if (!roomClientVC.isEmpty() && (myRoomID == null || myRoomID.isEmpty())) {
            joinRoomBtn.setEnabled(true);
        } else {
            joinRoomBtn.setEnabled(false);
        }
    }

    private void handleJoinRoom(String roomName) {
        myRoomID = roomName;
        createRoomBtn.setEnabled(false);
        joinRoomBtn.setEnabled(false);
        exitRoomBtn.setEnabled(true);
        msg_tf.setEditable(true);
        sendBtn.setEnabled(true);
        setTitle("사용자: " + clientID + " | 채팅방: " + myRoomID);
    }

    private void handleExitRoom(String roomName) {
        myRoomID = "";
        joinRoomBtn.setEnabled(!roomClientVC.isEmpty());
        createRoomBtn.setEnabled(true);
        exitRoomBtn.setEnabled(false);
        msg_tf.setEditable(false);
        sendBtn.setEnabled(false);
        setTitle("사용자: " + clientID);
    }

    private void handleRoomOut(String roomName) {
        roomClientVC.remove(roomName);
        roomJlist.setListData(roomClientVC);
        if (roomClientVC.isEmpty()) {
            joinRoomBtn.setEnabled(false);
        }
    }

    private void removeClientFromJlist(String id) {
        clientVC.remove(id);
        clientJlist.setListData(clientVC);
    }

    // ===== 파일 전송/수신 =====

    private void sendFileToServer(File file) {
        if (myRoomID == null || myRoomID.isEmpty()) {
            showErrorMessage("채팅방에 참여해야 파일을 전송할 수 있습니다.", "오류");
            return;
        }
        if (file == null || !file.exists()) {
            showErrorMessage("존재하지 않는 파일입니다.", "오류");
            return;
        }

        long fileSize = file.length();
        if (fileSize > 10 * 1024 * 1024) {
            showErrorMessage("파일이 너무 큽니다. (10MB 초과)", "오류");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) fileSize];
            int readBytes = fis.read(data);
            if (readBytes != fileSize) {
                showErrorMessage("파일을 읽는 중 오류가 발생했습니다.", "오류");
                return;
            }

            synchronized (dos) {
                String header = "File/" + clientID + "/" + file.getName() + "/" + fileSize;
                dos.writeUTF(header);
                dos.write(data);
                dos.flush();
            }

            appendToChatArea("[나] " + file.getName() + " 파일을 전송했습니다.");
        } catch (IOException e) {
            showErrorMessage("파일 전송 중 오류: " + e.getMessage(), "파일 전송 오류");
        }
    }

    private void handleFileReceive(String senderID, StringTokenizer st) {
        try {
            String fileName = st.nextToken();
            long fileSize = Long.parseLong(st.nextToken());

            byte[] data = new byte[(int) fileSize];
            dis.readFully(data);

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(fileName));
            int result = chooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File outFile = chooser.getSelectedFile();
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(data);
                }
                appendToChatArea(senderID + "님으로부터 파일 수신: " + outFile.getName());
            } else {
                appendToChatArea(senderID + "님이 보낸 파일을 저장하지 않았습니다.");
            }
        } catch (Exception e) {
            showErrorMessage("파일 수신 중 오류: " + e.getMessage(), "파일 수신 오류");
        }
    }

    private class FileDropHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                List<File> files = (List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                for (File f : files) {
                    sendFileToServer(f);
                }
                return true;
            } catch (Exception e) {
                showErrorMessage("파일 전송 중 오류: " + e.getMessage(), "파일 전송 오류");
                return false;
            }
        }
    }

    // ===== 버튼 동작 =====
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == loginBtn) {
            connectToServer();

        } else if (src == sendBtn) {
            handleSendButtonClick();

        } else if (src == noteBtn) {
            handleNoteButtonClick();

        } else if (src == createRoomBtn) {
            handleCreateRoomButtonClick();

        } else if (src == joinRoomBtn) {
            handleJoinRoomButtonClick();

        } else if (src == exitRoomBtn) {
            handleExitRoomButtonClick();

        } else if (src == clientExitBtn) {
            handleClientExitButtonClick();
        }
    }

    private void handleSendButtonClick() {
        if (myRoomID == null || myRoomID.isEmpty()) {
            showErrorMessage("채팅방에 참여해야 메시지를 전송할 수 있습니다.", "오류");
            return;
        }
        String message = msg_tf.getText().trim();
        if (!message.isEmpty()) {
            sendMsg("SendMsg/" + myRoomID + "/" + message);
            msg_tf.setText("");
            msg_tf.requestFocus();
        }
    }

    private void handleNoteButtonClick() {
        String selectedClient = clientJlist.getSelectedValue();
        if (selectedClient != null && !selectedClient.equals(clientID)) {
            String noteContent = JOptionPane.showInputDialog(this, "쪽지 내용을 입력하세요:",
                    selectedClient + "님에게 보낼 쪽지");
            if (noteContent != null && !noteContent.trim().isEmpty()) {
                sendMsg("Note/" + selectedClient + "/" + noteContent);
            }
        } else {
            showErrorMessage("쪽지를 보낼 클라이언트를 선택하세요.", "알림");
        }
    }

    private void handleCreateRoomButtonClick() {
        String roomName = JOptionPane.showInputDialog(this, "방 이름을 입력하세요:", "새 방 만들기",
                JOptionPane.PLAIN_MESSAGE);
        if (roomName != null && !roomName.trim().isEmpty()) {
            sendMsg("CreateRoom/" + roomName.trim());
        } else if (roomName != null) {
            showErrorMessage("방 이름은 비워둘 수 없습니다.", "입력 오류");
        }
    }

    private void handleJoinRoomButtonClick() {
        String roomName = roomJlist.getSelectedValue();
        if (roomName != null) {
            sendMsg("JoinRoom/" + roomName);
        } else {
            showErrorMessage("참여할 채팅방을 선택해주세요.", "오류");
        }
    }

    private void handleExitRoomButtonClick() {
        if (myRoomID != null && !myRoomID.isEmpty()) {
            sendMsg("ExitRoom/" + myRoomID);
        }
    }

    private void handleClientExitButtonClick() {
        if (myRoomID != null && !myRoomID.isEmpty()) {
            sendMsg("ExitRoom/" + myRoomID);
        }
        sendMsg("ClientExit/" + clientID);
        closeConnections();
        System.exit(0);
    }

    // ===== 키 이벤트 =====
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getSource() == msg_tf && e.getKeyCode() == KeyEvent.VK_ENTER) {
            handleSendButtonClick();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        new Client2025();
    }
}
