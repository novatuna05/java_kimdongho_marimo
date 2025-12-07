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
import java.util.StringTokenizer;
import java.util.Vector;

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
import javax.swing.JTextPane; // JTextPane 추가
import javax.swing.border.EmptyBorder;

public class Client2025 extends JFrame implements ActionListener, KeyListener {
    private static final long serialVersionUID = 2L;

    // Login GUI 변수
    private JFrame loginGUI = new JFrame("로그인"); // 로그인 창 프레임
    private JPanel loginJpanel; // 로그인 패널
    private JTextField serverIP_tf; // 서버 IP 입력 필드
    private JTextField serverPort_tf; // 서버 포트 입력 필드
    private JTextField clientID_tf; // 클라이언트 ID 입력 필드
    private JLabel img_Label; // 이미지 표시 레이블
    private JButton loginBtn; // 로그인 버튼
    private String serverIP; // 서버 IP 주소
    private int serverPort; // 서버 포트 번호
    private String clientID; // 클라이언트 ID (사용자 이름)

    // Main GUI 변수
    private JPanel contentPane;
    private JList<String> clientJlist = new JList(); // 전체 접속자 명단
    private JList<String> roomJlist = new JList();
    private JTextField msg_tf;
    private JTextPane chatPane = new JTextPane(); // **JTextArea -> JTextPane으로 변경**
    private JButton noteBtn = new JButton("쪽지 보내기");
    private JButton sendImageBtn = new JButton("이미지 전송");
    private JButton joinRoomBtn = new JButton("채팅방 참여");
    private JButton createRoomBtn = new JButton("방 만들기");
    private JButton sendBtn = new JButton("전송");
    private JButton exitRoomBtn = new JButton("탈퇴");
    private JButton clientExitBtn = new JButton("채팅종료");

    // 클라이언트 관리
    private Vector<String> clientVC = new Vector<>(); // 전체 접속자 ID 목록
    private Vector<String> roomClientVC = new Vector<>(); // 채팅방 이름 목록
    private String myRoomID = ""; // 현재 클라이언트가 참여한 채팅방 ID
    private static final String DOWNLOAD_DIR = "client_downloads/"; // 다운로드 폴더

    // network 변수
    private Socket socket; // 서버 연결 소켓
    private DataInputStream dis; // 서버로부터 데이터를 읽어오는 스트림
    private DataOutputStream dos; // 서버로 데이터를 보내는 스트림

    // 기타
    StringTokenizer st; // 메시지 파싱을 위한 토크나이저
    private boolean socketEstablished = false; // 소켓 연결 성공 여부 플래그

    public Client2025() {
        initializeLoginGUI();
        initializeMainGUI();
        addActionListeners();
    }

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

        serverIP_tf = new JTextField();
        serverIP_tf.setBounds(135, 245, 221, 33);
        serverIP_tf.setText("127.0.0.1"); // 기본 IP 주소 설정
        loginJpanel.add(serverIP_tf);
        serverIP_tf.setColumns(10);

        JLabel lblServerPort = new JLabel("서버 포트");
        lblServerPort.setFont(new Font("굴림", Font.BOLD, 20));
        lblServerPort.setBounds(12, 314, 113, 31);
        loginJpanel.add(lblServerPort);

        serverPort_tf = new JTextField();
        serverPort_tf.setColumns(10);
        serverPort_tf.setBounds(135, 312, 221, 33);
        serverPort_tf.setText("12345"); // 기본 포트 번호 설정
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

        loginGUI.setVisible(true); // 로그인 창 표시
    }

    void initializeMainGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(600, 100, 510, 490); // Y축 크기 증가
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JLabel 접속자 = new JLabel("전체 접속자");
        접속자.setBounds(12, 20, 73, 15);
        contentPane.add(접속자);

        clientJlist.setBounds(12, 45, 108, 107);
        contentPane.add(clientJlist);

        clientExitBtn.setBounds(12, 162, 108, 23);
        contentPane.add(clientExitBtn);

        noteBtn.setBounds(12, 192, 108, 23);
        contentPane.add(noteBtn);

        sendImageBtn.setBounds(12, 222, 108, 23);
        contentPane.add(sendImageBtn);


        JLabel 채팅방 = new JLabel("채팅방목록");
        채팅방.setBounds(12, 255, 97, 15);
        contentPane.add(채팅방);

        roomJlist.setBounds(12, 270, 108, 107);
        contentPane.add(roomJlist);

        joinRoomBtn.setBounds(6, 387, 60, 23);
        contentPane.add(joinRoomBtn);
        joinRoomBtn.setEnabled(false); // 초기 비활성화

        exitRoomBtn.setBounds(68, 387, 60, 23);
        contentPane.add(exitRoomBtn);
        exitRoomBtn.setEnabled(false); // 초기 비활성화

        createRoomBtn.setBounds(12, 416, 108, 23);
        contentPane.add(createRoomBtn);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(142, 16, 340, 393);
        contentPane.add(scrollPane);

        chatPane.setEditable(false); // 편집 불가
        chatPane.setContentType("text/html"); // **HTML 콘텐츠 타입 설정**
        // 초기 HTML 설정 (body 태그 안에 내용이 추가됨)
        chatPane.setText("<html><body style='font-size:12px; margin-top: 5px; margin-left: 5px;'></body></html>");
        scrollPane.setViewportView(chatPane); // **JTextPane 추가**

        msg_tf = new JTextField();
        msg_tf.setBounds(144, 417, 268, 21);
        contentPane.add(msg_tf);
        msg_tf.setColumns(10);
        msg_tf.setEditable(false); // 초기 편집 불가

        sendBtn.setBounds(412, 416, 70, 23);
        contentPane.add(sendBtn);
        sendBtn.setEnabled(false); // 초기 비활성화

        this.setVisible(false); // 메인 창은 로그인 성공 후 표시
    }

    // 이벤트 리스너 등록
    void addActionListeners() {
        loginBtn.addActionListener(this); // 로그인 버튼 리스너
        noteBtn.addActionListener(this); // 쪽지 버튼 리스너
        sendImageBtn.addActionListener(this); // **이미지 전송 버튼 리스너**
        joinRoomBtn.addActionListener(this); // 채팅방 참여 버튼 리스너
        createRoomBtn.addActionListener(this); // 방 만들기 버튼 리스너
        sendBtn.addActionListener(this); // 전송 버튼 리스너
        exitRoomBtn.addActionListener(this); // 채팅방 탈퇴 버튼 리스너
        msg_tf.addKeyListener(this); // 메시지 입력 필드 키보드 리스너 (Enter 처리용)
        clientExitBtn.addActionListener(this); // 채팅 종료 버튼 리스너
    }

    // 서버 연결 시도
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

    // 클라이언트 ID 서버로 전송 및 중복 ID 처리
    void sendMyClientID() {
        clientID = clientID_tf.getText().trim();
        sendMsg(clientID);

        try {
            String msg = dis.readUTF();
            if ("DuplicateClientID".equals(msg)) {
                JOptionPane.showMessageDialog(this, "이미 사용중인 ID입니다.", "중복 ID", JOptionPane.ERROR_MESSAGE);
                clientID_tf.setText("");
                clientID_tf.requestFocus();
                socketEstablished = false;
                socket.close();
                System.exit(0);

            } else if ("GoodClientID".equals(msg)) {
                InitializeAndRecvMsg();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버로부터 응답을 받는 중 오류가 발생했습니다.", "통신 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 메인 GUI 표시 및 메시지 수신 스레드 시작
    void InitializeAndRecvMsg() {
        this.setVisible(true);
        this.loginGUI.setVisible(false);

        clientVC.add(clientID);
        setTitle("사용자: " + clientID);

        // 서버로부터 메시지를 계속 수신하는 별도 스레드 시작
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg;
                    while (true) {
                        msg = dis.readUTF();
                        System.out.println("서버로부터 받은 메시지: " + msg);
                        parseMsg(msg);
                    }
                } catch (IOException e) {
                    handleServerShutdown();
                }
            }
        }).start();
    }

    // 서버로 메시지 전송 (문자열 프로토콜 전송용)
    void sendMsg(String msg) {
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "메시지 전송 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 수신한 메시지 파싱 및 프로토콜 처리
    void parseMsg(String msg) {
        st = new StringTokenizer(msg, "/");
        String protocol = st.nextToken();
        String message = "";
        if (st.hasMoreTokens()) {
            message = st.nextToken();
        }

        // 프로토콜 종류에 따라 분기 처리
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
                handleCreateRoom(message);
                break;

            case "NewRoom":
            case "OldRoom":
                handleAddRoomJlist(message);
                break;

            case "CreateRoomFail":
                showErrorMessage("방 만들기 실패", "알림");
                break;

            case "JoinRoomMsg":
                String msg2 = st.nextToken();
                appendToChatArea("<span style='color: blue;'>[알림]</span> " + message + ": " + msg2);
                break;

            case "JoinRoom":
                handleJoinRoom(message);
                break;

            case "SendMsg":
                String chatMsg = st.nextToken();
                appendToChatArea("<span style='font-weight: bold;'>" + message + "</span>: " + chatMsg);
                break;

            case "ImageReceived": // **이미지 수신 처리**
                String senderID_img = message; // 보낸 사람 ID
                String fileName_img = st.nextToken();
                String fileSavePath_img = st.nextToken(); // 서버에 저장된 절대 경로
                handleImageReceived(senderID_img, fileName_img, fileSavePath_img);
                break;

            case "FileReady": // **서버가 파일 전송 준비 완료 알림**
                long fileSize = Long.parseLong(st.nextToken());
                String targetPath = st.nextToken();
                String senderID = st.nextToken(); // **보낸 사람 ID 수신 추가**
                receiveFileFromServer(message, fileSize, targetPath, senderID); // **senderID 인자 추가**
                break;

            case "ClientJlistUpdate":
                refreshClientJList();
                break;

            case "RoomJlistUpdate":
                System.out.println("채팅방 목록 갱신");
                refreshRoomJlist();
                break;

            case "ClientExit":
                removeClientFromJlist(message);
                break;

            case "ServerShutdown":
                handleServerShutdown();
                break;

            case "RoomOut":
                handleRoomOut(message);
                break;

            case "ExitRoomMsg":
                String exitMsg = st.nextToken();
                appendToChatArea("<span style='color: blue;'>[알림]</span> " + message + ": " + exitMsg);
                break;

            case "FileError":
                showErrorMessage(message, "파일 수신 오류");
                break;

            default:
                break;
        }
    }

    // **이미지 수신 처리**
    private void handleImageReceived(String senderID, String fileName, String fileSavePath) {
        // 사용자에게 다운로드 여부를 묻지 않고, 즉시 다운로드 요청 및 미리보기 시도

        // 서버에 파일 요청: FileRequest/서버에저장된경로/다운로드될파일명/보낸사람ID
        sendMsg("FileRequest/" + fileSavePath + "/" + fileName + "/" + senderID);
    }

    // **파일 수신 로직 (서버로부터)**
    private void receiveFileFromServer(String fileName, long fileSize, String targetPath, String senderID) {
        File dir = new File(DOWNLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 다운로드 파일명은 senderID와 실제 파일명을 조합하여 충돌 방지
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

            // 미리보기 시도 (이미지 파일인 경우)
            if (fileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$")) {
                previewImageInChat(savePath, senderID);
            } else {
                // 이미지 파일이 아닐 경우 텍스트로 알림
                appendToChatArea("<span style='color: green;'>[파일 수신]</span> " + senderID + "님이 파일(" + fileName + ")을 전송했습니다. (저장 위치: " + savePath + ")");
            }

        } catch (IOException e) {
            showErrorMessage("파일 수신 중 오류 발생: " + e.getMessage(), "수신 오류");
        }
    }

    // **채팅창 내부에 이미지 미리보기 표시**
    private void previewImageInChat(String filePath, String senderID) {
        // 1. HTML 이미지 태그 생성
        // Windows 경로 구분자(\)를 웹 경로 구분자(/)로 변환해야 JTextPane의 HTML 이미지가 로드됨
        String cleanPath = new File(filePath).getAbsolutePath().replace("\\", "/");

        String imageHtml = "<p style='margin: 0; padding: 0; color: #555;'>" +
                "<span style='color: orange; font-weight: bold;'>[이미지]</span> " + senderID + "님이 이미지를 전송했습니다:</p>" +
                "<img src='file:///" + cleanPath + "' width='200' style='max-width: 100%; height: auto; border: 1px solid #ccc;'/><br>";

        // 2. 현재 HTML 내용에 이미지 태그 삽입
        String currentHtml = chatPane.getText();
        // </body> 태그 직전에 새 HTML 내용 삽입
        String newHtml = currentHtml.replace("</body>", imageHtml + "</body>");
        chatPane.setText(newHtml);

        // 3. 스크롤을 맨 아래로 이동
        chatPane.setCaretPosition(chatPane.getDocument().getLength());
    }


    private void showMessageBox(String msg, String title) {
        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.CLOSED_OPTION);// 메시지 박스 표시.
    }

    // 클라이언트 목록(Vector)에 ID 추가
    private void addClientToList(String clientID) {
        clientVC.add(clientID);
    }

    // 클라이언트 JList 화면 갱신
    private void refreshClientJList() {
        clientJlist.setListData(clientVC);
    }

    // 방 생성 성공 시 처리
    private void handleCreateRoom(String roomName) {
        myRoomID = roomName;
        joinRoomBtn.setEnabled(false);
        createRoomBtn.setEnabled(false);
        exitRoomBtn.setEnabled(true);
        msg_tf.setEditable(true);
        sendBtn.setEnabled(true);
        setTitle("사용자: " + clientID + " | 채팅방: " + myRoomID);
        appendToChatArea("<span style='color: blue;'>[시스템]</span> " + clientID + "님이 " + myRoomID + " 방을 생성하고 가입했습니다.");
    }

    // 방 목록(Vector)에 방 이름 추가 및 참여 버튼 활성화
    private void handleAddRoomJlist(String roomName) {
        if (myRoomID.equals("")) {
            joinRoomBtn.setEnabled(true);
        }
        roomClientVC.add(roomName);
        roomJlist.setListData(roomClientVC);
    }

    // 방 JList 화면 갱신
    private void refreshRoomJlist() {
        roomJlist.setListData(roomClientVC);
    }

    // 방 참여 성공 시 처리
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

    // 클라이언트 목록(Vector)에서 ID 제거
    private void removeClientFromJlist(String clientID) {
        clientVC.remove(clientID);
    }

    // 서버 종료 시 처리
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

    // 서버에서 방이 삭제되었을 때 처리
    private void handleRoomOut(String roomName) {
        roomClientVC.remove(roomName);
        if (roomClientVC.isEmpty()) {
            joinRoomBtn.setEnabled(false);
        }
    }

    private void showErrorMessage(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    // **채팅창에 메시지 추가 (HTML)**
    private void appendToChatArea(String message) {
        // HTML body 태그 내에 텍스트를 추가하고 줄바꿈 (<br>) 처리
        String currentHtml = chatPane.getText();
        // </body> 직전에 새 메시지를 삽입. <p> 태그를 사용하여 줄 간격 확보
        String newHtml = currentHtml.replace("</body>", "<p style='margin: 0; padding: 0;'>" + message + "</p></body>");
        chatPane.setText(newHtml);

        // 스크롤을 항상 맨 아래로 이동
        chatPane.setCaretPosition(chatPane.getDocument().getLength());
    }

    // 정보 메시지 박스 표시
    private void showInfoMessage(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // 액션 이벤트 처리 (버튼 클릭)
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginBtn) {
            connectToServer();
        } else if (e.getSource() == noteBtn) {
            handleNoteSendButtonClick();
        } else if (e.getSource() == sendImageBtn) {
            handleSendImageButtonClick();
        } else if (e.getSource() == createRoomBtn) {
            handleCreateRoomButtonClick();
        } else if (e.getSource() == joinRoomBtn) {
            handleJoinRoomButtonClick();
        } else if (e.getSource() == sendBtn) {
            handleSendButtonClick();
        } else if (e.getSource() == clientExitBtn) {
            handleClientExitButtonClick();
        } else if (e.getSource() == exitRoomBtn) {
            handleExitRoomButtonClick();
        }
    }

    // **이미지 파일 전송 버튼 클릭 처리**
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
            // 1. 프로토콜 및 메타데이터 전송: ImageTransfer/방ID/파일명/파일크기
            dos.writeUTF("ImageTransfer/" + myRoomID + "/" + fileName + "/" + fileSize);

            // 2. 파일 데이터를 DataOutputStream으로 직접 전송
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();

            // 전송 성공 시 채팅창에 알림
            appendToChatArea("<span style='color: gray;'>[나]</span> 이미지 파일 전송 완료: " + fileName);

        } catch (IOException e) {
            showErrorMessage("이미지 전송 중 오류 발생: " + e.getMessage(), "전송 오류");
        }
    }


    // 쪽지 보내기 버튼 클릭 처리
    public void handleNoteSendButtonClick() {
        String dstClient = (String) clientJlist.getSelectedValue();
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

    // 방 만들기 버튼 클릭 처리
    private void handleCreateRoomButtonClick() {
        String roomName = JOptionPane.showInputDialog("채팅방 이름 입력:");
        if (roomName == null || roomName.trim().isEmpty()) {
            return;
        }
        sendMsg("CreateRoom/" + roomName.trim());
    }

    // 채팅방 참여 버튼 클릭 처리
    private void handleJoinRoomButtonClick() {
        String roomName = (String) roomJlist.getSelectedValue();
        if (roomName != null) {
            sendMsg("JoinRoom/" + roomName);
        } else {
            showErrorMessage("참여할 채팅방을 선택해주세요.", "오류");
        }
    }

    // 메시지 전송 버튼 클릭 처리
    private void handleSendButtonClick() {
        if (!myRoomID.isEmpty()) {
            String message = msg_tf.getText().trim();
            if (!message.isEmpty()) {
                sendMsg("SendMsg/" + myRoomID + "/" + message);

                msg_tf.setText("");
                msg_tf.requestFocus();
            }
        } else {
            showErrorMessage("채팅방에 참여해야 메시지를 전송할 수 있다.", "오류");
        }
    }

    // 채팅 종료 버튼 클릭 처리 (전체 종료)
    private void handleClientExitButtonClick() {
        if (!myRoomID.isEmpty()) {
            sendMsg("ExitRoom/" + myRoomID);
        }

        sendMsg("ClientExit/Bye");

        clientVC.removeAllElements();
        roomClientVC.removeAllElements();
        myRoomID = "";

        closeSocket();
        System.exit(0);
    }

    // 소켓 및 스트림 안전하게 닫기
    private void closeSocket() {
        try {
            if (dos != null) {
                dos.close();
            }
            if (dis != null) {
                dis.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 채팅방 탈퇴 버튼 클릭 처리
    private void handleExitRoomButtonClick() {
        sendMsg("ExitRoom/" + myRoomID);

        myRoomID = "";

        exitRoomBtn.setEnabled(false);
        joinRoomBtn.setEnabled(roomClientVC.size() > 0);
        createRoomBtn.setEnabled(true);
        msg_tf.setEditable(false);
        sendBtn.setEnabled(false);

        setTitle("사용자: " + clientID);
    }

    public void keyPressed(KeyEvent e) {
    }

    // KeyListener 인터페이스 메서드 구현 (키가 떼어졌을 때)
    public void keyReleased(KeyEvent e) {
        // Enter 키가 눌렸을 때 메시지 전송 처리
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (!myRoomID.isEmpty()) {
                String message = msg_tf.getText().trim();
                if (!message.isEmpty()) {
                    sendMsg("SendMsg/" + myRoomID + "/" + message);

                    msg_tf.setText("");
                    msg_tf.requestFocus();
                }
            }
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        new Client2025();
    }
}