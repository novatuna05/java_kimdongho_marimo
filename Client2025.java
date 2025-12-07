package client.client;

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
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

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
    private JList<String> clientJlist = new JList(); // 전체 접속자 명단, 첫번째는 자기 자신 //11-20
    private JList<String> roomJlist = new JList(); // 11-21
    private JTextField msg_tf;
    private JTextArea chatArea = new JTextArea(); // 채팅창 변수
    private JButton noteBtn = new JButton("쪽지 보내기"); // 11-27
    private JButton joinRoomBtn = new JButton("채팅방 참여");
    private JButton createRoomBtn = new JButton("방 만들기");
    private JButton sendBtn = new JButton("전송");
    private JButton exitRoomBtn = new JButton("탈퇴");
    private JButton clientExitBtn = new JButton("채팅종료");

    // 클라이언트 관리
    private Vector<String> clientVC = new Vector<>(); // 전체 접속자 ID 목록
    private Vector<String> roomClientVC = new Vector<>(); // 채팅방 이름 목록
    private String myRoomID = ""; // 현재 클라이언트가 참여한 채팅방 ID

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
        addActionListeners(); // 11-13
    }

    void initializeLoginGUI() {
        loginGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 1
        loginGUI.setBounds(100, 100, 385, 541); // 1
        loginJpanel = new JPanel();
        loginJpanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        loginGUI.setContentPane(loginJpanel); // 1
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

        loginBtn = new JButton("Login"); // 11-13
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
        setBounds(600, 100, 510, 460);
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

        JLabel 채팅방 = new JLabel("채팅방목록");
        채팅방.setBounds(12, 225, 97, 15);
        contentPane.add(채팅방);

        roomJlist.setBounds(12, 240, 108, 107);
        contentPane.add(roomJlist);

        joinRoomBtn.setBounds(6, 357, 60, 23);
        contentPane.add(joinRoomBtn);
        joinRoomBtn.setEnabled(false); // 초기 비활성화

        exitRoomBtn.setBounds(68, 357, 60, 23);
        contentPane.add(exitRoomBtn);
        exitRoomBtn.setEnabled(false); // 초기 비활성화

        createRoomBtn.setBounds(12, 386, 108, 23);
        contentPane.add(createRoomBtn);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(142, 16, 340, 363);
        contentPane.add(scrollPane);
        scrollPane.setViewportView(chatArea); // JTextArea를 스크롤 패인에 추가
        chatArea.setEditable(false); // 채팅 영역 편집 불가 설정

        msg_tf = new JTextField();
        msg_tf.setBounds(144, 387, 268, 21);
        contentPane.add(msg_tf);
        msg_tf.setColumns(10);
        msg_tf.setEditable(false); // 초기 편집 불가

        sendBtn.setBounds(412, 386, 70, 23);
        contentPane.add(sendBtn);
        sendBtn.setEnabled(false); // 초기 비활성화

        this.setVisible(false); // 메인 창은 로그인 성공 후 표시
    }

    // 이벤트 리스너 등록
    void addActionListeners() {
        loginBtn.addActionListener(this); // 로그인 버튼 리스너
        noteBtn.addActionListener(this); // 쪽지 버튼 리스너
        joinRoomBtn.addActionListener(this); // 채팅방 참여 버튼 리스너
        createRoomBtn.addActionListener(this); // 방 만들기 버튼 리스너
        sendBtn.addActionListener(this); // 전송 버튼 리스너
        exitRoomBtn.addActionListener(this); // 채팅방 탈퇴 버튼 리스너
        msg_tf.addKeyListener(this); // 메시지 입력 필드 키보드 리스너 (Enter 처리용)
        clientExitBtn.addActionListener(this); // 채팅 종료 버튼 리스너
    }

    // 서버 연결 시도
    public void connectToServer() {
        if (!socketEstablished) { // 이미 연결되지 않았을 경우에만 시도
            try {
                serverIP = serverIP_tf.getText().trim();
                serverPort = Integer.parseInt(serverPort_tf.getText().trim());
                socket = new Socket(serverIP, serverPort); // 서버에 소켓 연결 시도

                dis = new DataInputStream(socket.getInputStream()); // 입력 스트림 생성
                dos = new DataOutputStream(socket.getOutputStream()); // 출력 스트림 생성
                socketEstablished = true; // 연결 성공 플래그 설정

                sendMyClientID(); // 클라이언트 ID 서버로 전송
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
        sendMsg(clientID); // ID를 서버로 전송

        try {
            String msg = dis.readUTF(); // 서버로부터 응답 메시지 수신
            if ("DuplicateClientID".equals(msg)) {
                // ID 중복 실패 처리
                JOptionPane.showMessageDialog(this, "이미 사용중인 ID입니다.", "중복 ID", JOptionPane.ERROR_MESSAGE);
                clientID_tf.setText("");
                clientID_tf.requestFocus();
                socketEstablished = false;
                socket.close();
                System.exit(0); // 애플리케이션 종료

            } else if ("GoodClientID".equals(msg)) {
                // ID 등록 성공 처리 (서버 코드에 GoodClientID 전송 부분이 없지만 로직상 필요)
                InitializeAndRecvMsg(); // 메인 GUI 초기화 및 메시지 수신 스레드 시작
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버로부터 응답을 받는 중 오류가 발생했습니다.", "통신 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 메인 GUI 표시 및 메시지 수신 스레드 시작
    void InitializeAndRecvMsg() {
        this.setVisible(true); // 메인 창 표시
        this.loginGUI.setVisible(false); // 로그인 창 숨김

        clientVC.add(clientID); // 접속자 목록에 본인 ID 추가
        setTitle("사용자: " + clientID); // 창 제목 설정

        // 서버로부터 메시지를 계속 수신하는 별도 스레드 시작
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg;
                    while (true) {
                        msg = dis.readUTF(); // 메시지 수신 대기
                        System.out.println("서버로부터 받은 메시지: " + msg);
                        parseMsg(msg); // 수신한 메시지 파싱 및 처리
                    }
                } catch (IOException e) {
                    handleServerShutdown(); // 통신 오류 발생 시
                }
            }
        }).start();
    }

    // 서버로 메시지 전송
    void sendMsg(String msg) {
        try {
            dos.writeUTF(msg); // 문자열 메시지 전송
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "메시지 전송 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 수신한 메시지 파싱 및 프로토콜 처리
    void parseMsg(String msg) {
        st = new StringTokenizer(msg, "/"); // '/' 구분자로 메시지 분리
        String protocol = st.nextToken();
        String message = st.nextToken(); // 첫 번째 인자 (대부분 ID나 방 이름)

        // 프로토콜 종류에 따라 분기 처리
        switch (protocol) {
            case "NewClient":
            case "OldClient":
                addClientToList(message); // 새 클라이언트 또는 기존 클라이언트 목록 추가
                break;

            case "Note": // 쪽지 처리
                String note = st.nextToken(); // 쪽지 내용
                showMessageBox(note, message + "님으로부터 쪽지"); // 쪽지 알림 창 표시
                break;

            case "CreateRoom":
                handleCreateRoom(message); // 방 생성 성공 처리
                break;

            case "NewRoom":
            case "OldRoom": // 방 목록 업데이트
                handleAddRoomJlist(message); // 새 방 또는 기존 방 목록 추가
                break;

            case "CreateRoomFail": // 방 생성 실패 처리
                showErrorMessage("방 만들기 실패", "알림"); // 오류 메시지 표시
                break;

            case "JoinRoomMsg": // 방 참여/탈퇴 알림 메시지 처리
                String msg2 = st.nextToken(); // 알림 메시지 내용
                appendToChatArea(message + ": " + msg2); // 채팅창에 메시지 추가
                break;

            case "JoinRoom":
                handleJoinRoom(message); // 방 참여 성공 처리
                break;

            case "SendMsg": // 채팅 메시지 수신 처리
                String chatMsg = st.nextToken(); // 채팅 메시지 내용
                appendToChatArea(message + "님이 전송: " + chatMsg); // 채팅창에 메시지 추가

                // [추가] 메시지를 보낸 사람(message)이 '나(clientID)'가 아닐 때만 수신음 재생
                if (!message.equals(clientID)) {
                    playSound("recv.wav");
                }
                break;

            case "ClientJlistUpdate": // 클라이언트 목록 갱신 요청 처리
                refreshClientJList(); // 클라이언트 목록 디스플레이 갱신
                break;

            case "RoomJlistUpdate": // 방 목록 갱신 요청 처리
                System.out.println("채팅방 목록 갱신");
                refreshRoomJlist(); // 방 목록 디스플레이 갱신
                break;

            case "ClientExit":
                removeClientFromJlist(message); // 클라이언트 퇴장 처리 (목록에서 제거)
                break;

            case "ServerShutdown":
                handleServerShutdown(); // 서버 종료 알림 처리
                break;

            case "RoomOut":
                handleRoomOut(message); // 방 삭제 처리 (방 목록에서 제거)
                break;

            case "ExitRoomMsg": // 다른 클라이언트의 방 탈퇴 메시지 처리
                String exitMsg = st.nextToken();
                appendToChatArea(message + ": " + exitMsg); // 채팅창에 메시지 추가
                break;

            default:
                break; // 처리되지 않은 프로토콜 무시
        }
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
        myRoomID = roomName; // 현재 방 이름 설정
        joinRoomBtn.setEnabled(false); // 참여 버튼 비활성화
        createRoomBtn.setEnabled(false); // 방 생성 버튼 비활성화
        exitRoomBtn.setEnabled(true); // 퇴장 버튼 활성화
        msg_tf.setEditable(true); // 메시지 입력 가능
        sendBtn.setEnabled(true); // 전송 버튼 활성화
        setTitle("사용자: " + clientID + " | 채팅방: " + myRoomID); // 타이틀 업데이트
        appendToChatArea(clientID + "님이 " + myRoomID + " 방을 생성하고 가입했습니다.\n"); // 채팅창에 알림 추가
    }

    // 방 목록(Vector)에 방 이름 추가 및 참여 버튼 활성화
    private void handleAddRoomJlist(String roomName) {
        if (myRoomID.equals("")) { // 현재 방에 참여하고 있지 않을 때
            joinRoomBtn.setEnabled(true); // 참여 버튼 활성화
        }
        roomClientVC.add(roomName); // 방 이름 추가
        roomJlist.setListData(roomClientVC); // 방 목록 화면 갱신
    }

    // 방 JList 화면 갱신
    private void refreshRoomJlist() {
        roomJlist.setListData(roomClientVC);
    }

    // 방 참여 성공 시 처리
    private void handleJoinRoom(String roomName) {
        myRoomID = roomName; // 현재 방 이름 설정
        joinRoomBtn.setEnabled(false); // 참여 버튼 비활성화
        createRoomBtn.setEnabled(false); // 방 생성 버튼 비활성화
        exitRoomBtn.setEnabled(true); // 퇴장 버튼 활성화
        msg_tf.setEditable(true); // 메시지 입력 가능
        sendBtn.setEnabled(true); // 전송 버튼 활성화
        setTitle("사용자: " + clientID + " | 채팅방: " + myRoomID); // 타이틀 업데이트
        appendToChatArea(clientID + "님이 " + myRoomID + " 방에 참여했습니다.\n"); // 채팅창에 알림 추가
        showInfoMessage("채팅방 참여 성공", "알림"); // 성공 메시지 표시
    }

    // 클라이언트 목록(Vector)에서 ID 제거
    private void removeClientFromJlist(String clientID) {
        clientVC.remove(clientID);
    }

    // 서버 종료 시 처리
    private void handleServerShutdown() {
        try {
            closeSocket(); // 소켓 닫기 (안전하게 정리)
            clientVC.removeAllElements(); // 클라이언트 목록 초기화
            roomClientVC.removeAllElements(); // 방 목록 초기화
        } catch (Exception e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(this, "서버가 종료되었습니다.", "서버 종료", JOptionPane.WARNING_MESSAGE);
        System.exit(0); // 애플리케이션 종료
    }

    // 서버에서 방이 삭제되었을 때 처리
    private void handleRoomOut(String roomName) {
        roomClientVC.remove(roomName); // 방 목록에서 지정된 방 제거
        if (roomClientVC.isEmpty()) {
            joinRoomBtn.setEnabled(false);// 방이 없을 때 참여 버튼 비활성화.
        }
    }

    private void showErrorMessage(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    // 채팅창에 메시지 추가.
    private void appendToChatArea(String message) {
        chatArea.append(message + "\n");
    }

    // 정보 메시지 박스 표시
    private void showInfoMessage(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // 액션 이벤트 처리 (버튼 클릭)
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginBtn) {
            System.out.println("로그인 버튼 클릭됨");
            connectToServer(); // 서버 연결 시도
        } else if (e.getSource() == noteBtn) {
            System.out.println("쪽지 버튼 클릭됨");
            handleNoteSendButtonClick(); // 쪽지 보내기 처리
        } else if (e.getSource() == createRoomBtn) {
            handleCreateRoomButtonClick(); // 방 만들기 버튼 처리
        } else if (e.getSource() == joinRoomBtn) {
            handleJoinRoomButtonClick(); // 채팅방 참여 버튼 처리
        } else if (e.getSource() == sendBtn) {
            handleSendButtonClick(); // 메시지 전송 버튼 처리
        } else if (e.getSource() == clientExitBtn) {
            handleClientExitButtonClick(); // 채팅 종료 버튼 처리
        } else if (e.getSource() == exitRoomBtn) {
            System.out.println("채팅방 탈퇴 버튼 클릭됨");
            handleExitRoomButtonClick(); // 채팅방 탈퇴 버튼 처리
        }
    }

    // 쪽지 보내기 버튼 클릭 처리
    public void handleNoteSendButtonClick() {
        String dstClient = (String) clientJlist.getSelectedValue(); // JList에서 선택된 클라이언트 ID
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
            sendMsg("Note/" + dstClient + "/" + note); // 서버로 쪽지 프로토콜 전송
            System.out.println("수신자: " + dstClient + " | 전송 노트: " + note);
        }
    }

    // 방 만들기 버튼 클릭 처리
    private void handleCreateRoomButtonClick() {
        System.out.println("방 만들기 버튼 클릭됨");

        String roomName = JOptionPane.showInputDialog("채팅방 이름 입력:");
        if (roomName == null || roomName.trim().isEmpty()) {
            System.out.println("방 생성 취소 또는 이름 미입력");
            return;
        }
        sendMsg("CreateRoom/" + roomName.trim()); // 서버로 방 생성 요청 프로토콜 전송
    }

    // 채팅방 참여 버튼 클릭 처리
    private void handleJoinRoomButtonClick() {
        System.out.println("채팅방 참여 버튼 클릭됨");
        String roomName = (String) roomJlist.getSelectedValue(); // JList에서 선택된 방 이름
        if (roomName != null) {
            sendMsg("JoinRoom/" + roomName); // 서버로 방 참여 요청 프로토콜 전송
        } else {
            showErrorMessage("참여할 채팅방을 선택해주세요.", "오류");
        }
    }

    // 메시지 전송 버튼 클릭 처리
    private void handleSendButtonClick() {
        if (!myRoomID.isEmpty()) { // 현재 방에 참여 중인지 확인
            String message = msg_tf.getText().trim();
            if (!message.isEmpty()) {
                sendMsg("SendMsg/" + myRoomID + "/" + message); // 서버로 메시지 전송 프로토콜 전송

                playSound("send.wav");

                msg_tf.setText(""); // 입력 필드 초기화
                msg_tf.requestFocus(); // 입력 필드에 포커스 재설정
            }
        } else {
            showErrorMessage("채팅방에 참여해야 메시지를 전송할 수 있다.", "오류");
        }
    }

    // 채팅 종료 버튼 클릭 처리 (전체 종료)
    private void handleClientExitButtonClick() {
        if (!myRoomID.isEmpty()) { // 현재 참여 중인 방이 있다면
            sendMsg("ExitRoom/" + myRoomID); // 서버에 방 탈퇴 알림
        }

        sendMsg("ClientExit/Bye"); // 서버에 클라이언트 종료 알림

        clientVC.removeAllElements(); // 클라이언트 목록 초기화
        roomClientVC.removeAllElements(); // 방 목록 초기화
        myRoomID = ""; // 현재 방 정보 초기화

        closeSocket(); // 소켓 연결 정리
        System.exit(0); // 애플리케이션 종료
    }

    // 소켓 및 스트림 안전하게 닫기
    private void closeSocket() {
        try {
            if (dos != null) {
                dos.close(); // 출력 스트림 닫기
            }
            if (dis != null) {
                dis.close(); // 입력 스트림 닫기
            }
            if (socket != null) {
                socket.close(); // 소켓 연결 닫기
            }
        } catch (IOException e) {
            e.printStackTrace(); // 닫는 중 발생한 예외 로그 출력
        }
    }

    // 채팅방 탈퇴 버튼 클릭 처리
    private void handleExitRoomButtonClick() {
        System.out.println("채팅방 탈퇴 버튼 클릭됨");

        sendMsg("ExitRoom/" + myRoomID); // 서버로 방 탈퇴 요청 프로토콜 전송

        myRoomID = ""; // 현재 방 정보 초기화

        // GUI 상태 변경
        exitRoomBtn.setEnabled(false); // 퇴장 버튼 비활성화
        joinRoomBtn.setEnabled(roomClientVC.size() > 0); // 방 목록이 있으면 참여 버튼 활성화
        createRoomBtn.setEnabled(true); // 방 생성 버튼 활성화
        msg_tf.setEditable(false); // 메시지 입력 불가
        sendBtn.setEnabled(false); // 전송 버튼 비활성화

        setTitle("사용자: " + clientID); // 타이틀을 사용자 ID만으로 변경
    }

    public void keyPressed(KeyEvent e) {
    }

    // KeyListener 인터페이스 메서드 구현 (키가 떼어졌을 때)
    public void keyReleased(KeyEvent e) {
        // Enter 키가 눌렸을 때 메시지 전송 처리
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (!myRoomID.isEmpty()) { // 방에 참여 중일 때만
                String message = msg_tf.getText().trim();
                if (!message.isEmpty()) {
                    sendMsg("SendMsg/" + myRoomID + "/" + message); // 메시지 전송

                    playSound("send.wav");

                    msg_tf.setText(""); // 입력 필드 초기화
                    msg_tf.requestFocus(); // 입력 필드에 포커스 재설정
                }
            }
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void playSound(String fileName) {
        try {
            File file = new File("sounds/" + fileName);
            if (file.exists()) {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
            } else {
                System.out.println("소리 파일을 찾을 수 없습니다: " + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Client2025();
    }
}