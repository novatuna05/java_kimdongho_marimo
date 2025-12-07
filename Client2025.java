import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import java.awt.datatransfer.DataFlavor;

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
    private JTextPane chatPane = new JTextPane();
    private JButton noteBtn = new JButton("쪽지 보내기");
    private JButton joinRoomBtn = new JButton("채팅방 참여");
    private JButton createRoomBtn = new JButton("방 만들기");
    private JButton sendBtn = new JButton("전송");
    private JButton exitRoomBtn = new JButton("탈퇴");
    private JButton clientExitBtn = new JButton("채팅종료");
    private JButton sendImageBtn = new JButton("이미지 전송");
    private JPanel fileDropPanel;

    // 상태
    private Vector<String> clientVC = new Vector<>();
    private Vector<String> roomClientVC = new Vector<>();
    private String myRoomID = "";
    private static final String DOWNLOAD_DIR = "client_downloads/";

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
        loginGUI.setBounds(100, 100, 385, 541);
        loginJpanel = new JPanel();
        loginJpanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        loginGUI.setContentPane(loginJpanel);
        loginJpanel.setLayout(null);

        JLabel lblNewLabel = new JLabel("Server IP");
        lblNewLabel.setFont(new Font("굴림", Font.BOLD, 20));
        lblNewLabel.setBounds(12, 244, 113, 31);
        loginJpanel.add(lblNewLabel);

        serverIP_tf = new JTextField("127.0.0.1");
        serverIP_tf.setBounds(135, 245, 221, 33);
        loginJpanel.add(serverIP_tf);
        serverIP_tf.setColumns(10);

        JLabel lblServerPort = new JLabel("서버 포트");
        lblServerPort.setFont(new Font("굴림", Font.BOLD, 20));
        lblServerPort.setBounds(12, 314, 113, 31);
        loginJpanel.add(lblServerPort);

        serverPort_tf = new JTextField("12345");
        serverPort_tf.setColumns(10);
        serverPort_tf.setBounds(135, 312, 221, 33);
        loginJpanel.add(serverPort_tf);

        JLabel lblId = new JLabel("ID");
        lblId.setFont(new Font("굴림", Font.BOLD, 20));
        lblId.setBounds(12, 376, 113, 31);
        loginJpanel.add(lblId);

        clientID_tf = new JTextField();
        clientID_tf.setColumns(10);
        clientID_tf.setBounds(135, 377, 221, 33);
        loginJpanel.add(clientID_tf);

        loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("굴림", Font.BOLD, 20));
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
        setBounds(600, 100, 600, 560);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JLabel 접속자 = new JLabel("전체 접속자");
        접속자.setBounds(12, 10, 100, 15);
        contentPane.add(접속자);

        clientJlist.setBounds(12, 30, 120, 140);
        contentPane.add(clientJlist);

        clientExitBtn.setBounds(12, 175, 120, 23);
        contentPane.add(clientExitBtn);

        noteBtn.setBounds(12, 205, 120, 23);
        contentPane.add(noteBtn);

        sendImageBtn.setBounds(12, 235, 120, 23);
        contentPane.add(sendImageBtn);

        JLabel 채팅방 = new JLabel("채팅방목록");
        채팅방.setBounds(12, 270, 100, 15);
        contentPane.add(채팅방);

        roomJlist.setBounds(12, 290, 120, 140);
        contentPane.add(roomJlist);

        joinRoomBtn.setBounds(12, 440, 120, 23);
        contentPane.add(joinRoomBtn);
        joinRoomBtn.setEnabled(false);

        exitRoomBtn.setBounds(12, 470, 120, 23);
        contentPane.add(exitRoomBtn);
        exitRoomBtn.setEnabled(false);

        createRoomBtn.setBounds(12, 500, 120, 23);
        contentPane.add(createRoomBtn);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(150, 10, 420, 380);
        contentPane.add(scrollPane);

        chatPane.setEditable(false);
        chatPane.setContentType("text/html");
        chatPane.setText("<html><body style='font-size:12px; margin-top:5px; margin-left:5px;'></body></html>");
        scrollPane.setViewportView(chatPane);

        msg_tf = new JTextField();
        msg_tf.setBounds(150, 400, 300, 25);
        contentPane.add(msg_tf);
        msg_tf.setColumns(10);
        msg_tf.setEditable(false);

        sendBtn.setBounds(460, 400, 110, 25);
        contentPane.add(sendBtn);
        sendBtn.setEnabled(false);

        // 파일 드롭 영역
        fileDropPanel = new JPanel();
        fileDropPanel.setBorder(BorderFactory.createTitledBorder("여기로 파일을 드래그해서 전송"));
        fileDropPanel.setBounds(150, 435, 420, 90);
        fileDropPanel.setTransferHandler(new FileDropHandler());
        contentPane.add(fileDropPanel);

        this.setVisible(false);
    }

    void addActionListeners() {
        loginBtn.addActionListener(this);
        noteBtn.addActionListener(this);
        sendImageBtn.addActionListener(this);
        joinRoomBtn.addActionListener(this);
        createRoomBtn.addActionListener(this);
        sendBtn.addActionListener(this);
        exitRoomBtn.addActionListener(this);
        clientExitBtn.addActionListener(this);
        msg_tf.addKeyListener(this);
    }

    // ===== 서버 접속 =====
    public void connectToServer() {
        if (!socketEstablished) {
            try {
                serverIP = serverIP_tf.getText().trim();
                serverPort = Integer.parseInt(serverPort_tf.getText().trim());
                socket = new Socket(serverIP, serverPort);

                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
                socketEstablished = true;

                sendMyClientID();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "잘못된 포트 번호입니다.", "오류", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "서버에 연결할 수 없습니다. IP와 포트 번호를 확인하세요.", "연결 오류",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 클라이언트 ID 서버로 전송 및 중복 처리
    void sendMyClientID() {
        clientID = clientID_tf.getText().trim();
        if (clientID.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        sendMsg(clientID);

        try {
            String msg = dis.readUTF();
            if ("DuplicateClientID".equals(msg)) {
                JOptionPane.showMessageDialog(this, "이미 사용중인 ID입니다.", "중복 ID", JOptionPane.ERROR_MESSAGE);
                clientID_tf.setText("");
                clientID_tf.requestFocus();
                socketEstablished = false;
                socket.close();
            } else if ("GoodClientID".equals(msg)) {
                InitializeAndRecvMsg();
            } else {
                // 예기치 않은 응답이 온 경우에도 메시지 수신 스레드 시작
                InitializeAndRecvMsg();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버로부터 응답을 받는 중 오류가 발생했습니다.", "통신 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 메인 GUI 표시 및 수신 스레드 시작
    void InitializeAndRecvMsg() {
        this.setVisible(true);
        this.loginGUI.setVisible(false);

        clientVC.add(clientID);
        setTitle("사용자: " + clientID);

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

    // ===== 서버로 문자열 메시지 전송 =====
    void sendMsg(String msg) {
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "메시지 전송 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== 수신 메시지 파싱 =====
    void parseMsg(String msg) {
        st = new StringTokenizer(msg, "/");
        String protocol = st.nextToken();
        String message = st.hasMoreTokens() ? st.nextToken() : "";

        switch (protocol) {
            case "NewClient":
            case "OldClient":
                addClientToList(message);
                break;

            case "Note": {
                String note = st.hasMoreTokens() ? st.nextToken() : "";
                showMessageBox(note, message + "님으로부터 쪽지");
                break;
            }

            case "CreateRoom":
                handleCreateRoom(message);
                break;

            case "NewRoom":
            case "OldRoom":
                handleAddRoomJlist(message);
                break;

            case "CreateRoomFail":
                showErrorMessage(message, "방 만들기 실패");
                break;

            case "JoinRoomMsg": {
                String msg2 = st.hasMoreTokens() ? st.nextToken() : "";
                appendToChatArea("<span style='color: blue;'>[알림]</span> " + message + ": " + msg2);
                break;
            }

            case "JoinRoom":
                handleJoinRoom(message);
                break;

            case "SendMsg": {
                String chatMsg = st.hasMoreTokens() ? st.nextToken() : "";
                appendToChatArea("<span style='font-weight: bold;'>" + message + "</span>: " + chatMsg);
                if (!message.equals(clientID)) {
                    playSound("recv.wav");
                }
                break;
            }

            case "ImageReceived": {
                String fileName_img = st.hasMoreTokens() ? st.nextToken() : "";
                String fileSavePath_img = st.hasMoreTokens() ? st.nextToken() : "";
                handleImageReceived(message, fileName_img, fileSavePath_img);
                break;
            }

            case "FileReady": {
                long fileSize = Long.parseLong(st.nextToken());
                String targetPath = st.nextToken();
                String senderID = st.nextToken();
                receiveFileFromServer(message, fileSize, targetPath, senderID);
                break;
            }

            case "File": { // File/보낸사람ID/파일명/파일크기 + 바이너리
                handleFileBroadcastReceive(message, st);
                break;
            }

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
                handleExitRoomFromServer(message);
                break;

            case "RoomOut":
                handleRoomOut(message);
                break;

            case "ExitRoomMsg": {
                String exitMsg = st.hasMoreTokens() ? st.nextToken() : "";
                appendToChatArea("<span style='color: blue;'>[알림]</span> " + message + ": " + exitMsg);
                break;
            }

            case "FileError":
                showErrorMessage(message, "파일 오류");
                break;

            case "ServerShutdown":
                handleServerShutdown();
                break;

            default:
                break;
        }
    }

    private void showMessageBox(String msg, String title) {
        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.CLOSED_OPTION);
    }

    // ===== 이미지 수신 처리 (ImageTransfer + FileRequest 프로토콜용) =====
    private void handleImageReceived(String senderID, String fileName, String fileSavePath) {
        // 즉시 서버에 파일 다운로드 요청
        sendMsg("FileRequest/" + fileSavePath + "/" + fileName + "/" + senderID);
    }

    // 서버가 FileReady 를 보낸 후, 실제 파일 데이터를 수신
    private void receiveFileFromServer(String fileName, long fileSize, String targetPath, String senderID) {
        File dir = new File(DOWNLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String savePath = DOWNLOAD_DIR + senderID + "_" + fileName;

        try (FileOutputStream fos = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long remaining = fileSize;

            while (remaining > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                fos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
            fos.flush();

            if (fileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$")) {
                previewImageInChat(savePath, senderID);
            } else {
                appendToChatArea("<span style='color: green;'>[파일 수신]</span> " + senderID + "님이 파일("
                        + fileName + ")을 전송했습니다. (저장 위치: " + savePath + ")");
            }

        } catch (IOException e) {
            showErrorMessage("파일 수신 중 오류 발생: " + e.getMessage(), "수신 오류");
        }
    }

    // 채팅창에 이미지 미리보기
    private void previewImageInChat(String filePath, String senderID) {
        String cleanPath = new File(filePath).getAbsolutePath().replace("\\", "/");

        String imageHtml =
                "<p style='margin: 0; padding: 0; color: #555;'>"
                        + "<span style='color: orange; font-weight: bold;'>[이미지]</span> "
                        + senderID + "님이 이미지를 전송했습니다:</p>"
                        + "<img src='file:///" + cleanPath
                        + "' width='200' style='max-width: 100%; height: auto; border: 1px solid #ccc;'/><br>";

        String currentHtml = chatPane.getText();
        String newHtml = currentHtml.replace("</body>", imageHtml + "</body>");
        chatPane.setText(newHtml);
        chatPane.setCaretPosition(chatPane.getDocument().getLength());
    }

    // ===== 리스트 / 상태 관련 메소드 =====
    private void addClientToList(String id) {
        if (!clientVC.contains(id)) {
            clientVC.add(id);
        }
    }

    private void refreshClientJList() {
        clientJlist.setListData(clientVC);
    }

    private void handleCreateRoom(String roomName) {
        myRoomID = roomName;
        joinRoomBtn.setEnabled(false);
        createRoomBtn.setEnabled(false);
        exitRoomBtn.setEnabled(true);
        msg_tf.setEditable(true);
        sendBtn.setEnabled(true);
        setTitle("사용자: " + clientID + " | 채팅방: " + myRoomID);
        appendToChatArea("<span style='color: blue;'>[시스템]</span> " + clientID + "님이 " + myRoomID
                + " 방을 생성하고 가입했습니다.");
    }

    private void handleAddRoomJlist(String roomName) {
        if (roomName == null || roomName.isEmpty()) return;
        if (!roomClientVC.contains(roomName)) {
            roomClientVC.add(roomName);
        }
        roomJlist.setListData(roomClientVC);
        if (myRoomID.equals("")) {
            joinRoomBtn.setEnabled(true);
        }
    }

    private void refreshRoomJlist() {
        roomJlist.setListData(roomClientVC);
        if (!roomClientVC.isEmpty() && myRoomID.equals("")) {
            joinRoomBtn.setEnabled(true);
        } else if (roomClientVC.isEmpty()) {
            joinRoomBtn.setEnabled(false);
        }
    }

    private void handleJoinRoom(String roomName) {
        myRoomID = roomName;
        joinRoomBtn.setEnabled(false);
        createRoomBtn.setEnabled(false);
        exitRoomBtn.setEnabled(true);
        msg_tf.setEditable(true);
        sendBtn.setEnabled(true);
        setTitle("사용자: " + clientID + " | 채팅방: " + myRoomID);
        appendToChatArea("<span style='color: blue;'>[시스템]</span> " + clientID + "님이 " + myRoomID + " 방에 참여했습니다.");
        showInfoMessage("채팅방 참여 성공", "알림");
    }

    private void removeClientFromJlist(String id) {
        clientVC.remove(id);
        clientJlist.setListData(clientVC);
    }

    private void handleExitRoomFromServer(String roomName) {
        if (roomName.equals(myRoomID)) {
            myRoomID = "";
            exitRoomBtn.setEnabled(false);
            msg_tf.setEditable(false);
            sendBtn.setEnabled(false);
            if (!roomClientVC.isEmpty()) {
                joinRoomBtn.setEnabled(true);
            }
            setTitle("사용자: " + clientID);
        }
    }

    private void handleRoomOut(String roomName) {
        roomClientVC.remove(roomName);
        roomJlist.setListData(roomClientVC);
        if (roomClientVC.isEmpty()) {
            joinRoomBtn.setEnabled(false);
        }
    }

    private void showErrorMessage(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void appendToChatArea(String message) {
        String currentHtml = chatPane.getText();
        String newHtml = currentHtml.replace("</body>",
                "<p style='margin: 0; padding: 0;'>" + message + "</p></body>");
        chatPane.setText(newHtml);
        chatPane.setCaretPosition(chatPane.getDocument().getLength());
    }

    private void showInfoMessage(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // ===== 서버 종료 처리 =====
    private void handleServerShutdown() {
        try {
            closeSocket();
            clientVC.removeAllElements();
            roomClientVC.removeAllElements();
        } catch (Exception e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(this, "서버가 종료되었습니다.", "서버 종료", JOptionPane.WARNING_MESSAGE);
        System.exit(0);
    }

    // ===== 파일 브로드캐스트(File 프로토콜) 수신 =====
    private void handleFileBroadcastReceive(String senderID, StringTokenizer st) {
        try {
            if (!st.hasMoreTokens()) return;
            String fileName = st.nextToken();
            if (!st.hasMoreTokens()) return;
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
                appendToChatArea("<span style='color: green;'>" + senderID + "님으로부터 파일 수신: "
                        + outFile.getName() + "</span>");
            } else {
                appendToChatArea("<span style='color: gray;'>" + senderID
                        + "님이 보낸 파일을 저장하지 않았습니다.</span>");
            }
        } catch (Exception e) {
            showErrorMessage("파일 수신 중 오류: " + e.getMessage(), "파일 수신 오류");
        }
    }

    // ===== 파일 전송(File 프로토콜, 드래그&드롭) =====
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

            appendToChatArea("<span style='color: gray;'>[나] " + file.getName() + " 파일을 전송했습니다.</span>");
        } catch (IOException e) {
            showErrorMessage("파일 전송 중 오류: " + e.getMessage(), "파일 전송 오류");
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

    // ===== 액션 이벤트 =====
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == loginBtn) {
            connectToServer();
        } else if (src == noteBtn) {
            handleNoteSendButtonClick();
        } else if (src == sendImageBtn) {
            handleSendImageButtonClick();
        } else if (src == createRoomBtn) {
            handleCreateRoomButtonClick();
        } else if (src == joinRoomBtn) {
            handleJoinRoomButtonClick();
        } else if (src == sendBtn) {
            handleSendButtonClick();
        } else if (src == clientExitBtn) {
            handleClientExitButtonClick();
        } else if (src == exitRoomBtn) {
            handleExitRoomButtonClick();
        }
    }

    // ===== 버튼 핸들러 =====
    private void handleSendButtonClick() {
        if (myRoomID == null || myRoomID.isEmpty()) {
            showErrorMessage("채팅방에 참여해야 메시지를 전송할 수 있습니다.", "오류");
            return;
        }
        String message = msg_tf.getText().trim();
        if (!message.isEmpty()) {
            sendMsg("SendMsg/" + myRoomID + "/" + message);
            playSound("send.wav");
            msg_tf.setText("");
            msg_tf.requestFocus();
        }
    }

    private void handleNoteSendButtonClick() {
        String dstClient = clientJlist.getSelectedValue();
        if (dstClient == null) {
            showErrorMessage("쪽지를 보낼 대상을 선택", "오류");
            return;
        }
        if (dstClient.equals(clientID)) {
            showErrorMessage("자기 자신에게는 쪽지를 보낼 수 없다.", "오류");
            return;
        }

        String note = JOptionPane.showInputDialog("보낼 메시지:");
        if (note != null && !note.trim().isEmpty()) {
            sendMsg("Note/" + dstClient + "/" + note);
        }
    }

    private void handleCreateRoomButtonClick() {
        String roomName = JOptionPane.showInputDialog("채팅방 이름 입력:");
        if (roomName == null || roomName.trim().isEmpty()) {
            return;
        }
        sendMsg("CreateRoom/" + roomName.trim());
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
        sendMsg("ClientExit/Bye");
        closeSocket();
        System.exit(0);
    }

    // 이미지 전송 버튼 클릭
    public void handleSendImageButtonClick() {
        if (myRoomID.isEmpty()) {
            showErrorMessage("채팅방에 참여해야 이미지를 전송할 수 있습니다.", "오류");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        String fileName = file.getName();
        long fileSize = file.length();

        if (!fileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$")) {
            showErrorMessage("JPG, JPEG, PNG, GIF 파일만 전송 가능합니다.", "파일 형식 오류");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            dos.writeUTF("ImageTransfer/" + myRoomID + "/" + fileName + "/" + fileSize);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();

            appendToChatArea("<span style='color: gray;'>[나]</span> 이미지 파일 전송 완료: " + fileName);

        } catch (IOException e) {
            showErrorMessage("이미지 전송 중 오류 발생: " + e.getMessage(), "전송 오류");
        }
    }

    // ===== 소켓 정리 =====
    private void closeSocket() {
        try {
            if (dos != null) dos.close();
            if (dis != null) dis.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    // ===== 사운드 재생 =====
    public void playSound(String fileName) {
        try {
            File file = new File("sounds/" + fileName);
            if (file.exists()) {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
            }
        } catch (Exception e) {
            // 사운드는 필수 기능이 아니므로 오류는 무시
        }
    }

    public static void main(String[] args) {
        new Client2025();
    }
}
