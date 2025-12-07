import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Server2025 extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField port_tf; // 포트 번호 입력 필드
    private JTextArea textArea = new JTextArea(); // 서버 로그 출력
    private JButton startBtn = new JButton("서버 실행"); // 서버 시작 버튼
    private JButton stopBtn = new JButton("서버 중지"); // 서버 중지 버튼

    // 소켓 생성 및 연결 부분
    private ServerSocket serverSocket; // 서버 소켓
    private Socket cs; // 클라이언트 소켓
    private int port = 12345; // 기본 포트 번호
    private static final String FILE_SAVE_DIR = "server_files/"; // 파일 저장 폴더

    // 기타 변수 관리
    private Vector<ClientInfo> clientVC = new Vector<ClientInfo>(); // 클라이언트 정보 저장 벡터
    private Vector<RoomInfo> roomVC = new Vector<RoomInfo>(); // 방 정보 저장 벡터

    public Server2025() {
        initGUI(); // GUI 초기화 메서드 호출
        setupActionListeners(); // 버튼 리스너 설정
    }

    public void initGUI() {
        setTitle("Server Application"); // 프레임 제목 설정
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 종료 시 프로그램 종료
        setBounds(30, 100, 321, 370); // 프레임 위치 및 크기 설정

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5)); // 패널 테두리 설정
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout()); // 레이아웃 변경

        JPanel topPanel = new JPanel();
        contentPane.add(topPanel, BorderLayout.NORTH); // 상단 패널 추가

        JLabel portNumberLabel = new JLabel("포트 번호");
        topPanel.add(portNumberLabel); // 포트 번호 레이블 추가

        port_tf = new JTextField();
        port_tf.setColumns(20);
        topPanel.add(port_tf); // 포트 번호 입력 필드 추가

        JPanel bottomPanel = new JPanel();
        contentPane.add(bottomPanel, BorderLayout.SOUTH); // 하단 패널 추가

        startBtn.setBounds(12, 286, 138, 23);
        bottomPanel.add(startBtn); // 서버 시작 버튼 추가

        stopBtn.setBounds(155, 286, 138, 23);
        bottomPanel.add(stopBtn);
        stopBtn.setEnabled(false); // 처음 실행 시에는 중지 버튼 비활성화.

        JScrollPane scrollPane = new JScrollPane();
        contentPane.add(scrollPane, BorderLayout.CENTER);

        textArea.setEditable(false);
        scrollPane.setViewportView(textArea); // 로그 출력 영역 설정

        this.setVisible(true); // 화면 보이기
    }

    void setupActionListeners() {
        startBtn.addActionListener(this);
        stopBtn.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startBtn) {
            startServer();
        } else if (e.getSource() == stopBtn) {
            stopServer();
        }
    }

    private void startServer() {
        try {
            port = Integer.parseInt(port_tf.getText().trim());
            serverSocket = new ServerSocket(port);
            textArea.append("서버가 포트 " + port + "에서 시작되었습니다.\n");

            // 파일 저장 디렉토리 생성
            File dir = new File(FILE_SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            startBtn.setEnabled(false); // 서버 시작 후에는 시작 버튼 비활성화
            port_tf.setEditable(false); // 서버 실행 중에는 포트 번호 수정 불가
            stopBtn.setEnabled(true); // 서버 실행 후에는 중지 버튼 활성화

            // 클라이언트 접속을 기다리는 스레드 시작
            waitForClientConnection();
        } catch (NumberFormatException e) {
            textArea.append("잘못된 포트 번호입니다.\n");
        } catch (IOException e) {
            textArea.append("서버 시작 오류: " + e.getMessage() + "\n");
        }
    }

    private void stopServer() {
        // 접속해 있는 모든 클라이언트에게 서버 종료 알림을 보낸 후 정리
        for (ClientInfo c : clientVC) {
            c.sendMsg("ServerShutdown/Bye"); // 서버 종료 안내 메시지 전송
            try {
                c.closeStreams(); // 클라이언트의 입출력 스트림 및 소켓 정리
            } catch (IOException e) {
                e.printStackTrace(); // 정리 과정 중 발생한 예외 출력
            }
        }

        try {
            // 서버 소켓이 열려 있다면 닫기
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // 서버 소켓 종료
            }
            roomVC.removeAllElements(); // 모든 방 정보 삭제
        } catch (IOException e) {
            e.printStackTrace(); // 서버 소켓 종료 중 발생한 예외 출력
        }

        // GUI 상태 원복
        startBtn.setEnabled(true); // 다시 서버 시작할 수 있도록 활성화
        port_tf.setEditable(true); // 포트 번호 다시 수정 가능
        stopBtn.setEnabled(false); // 서버가 꺼졌으므로 중지 버튼 비활성화
    }

    private void waitForClientConnection() {
        // 클라이언트 접속을 기다리는 별도 스레드 생성
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 서버 소켓이 닫히지 않은 동안 계속 접속 대기
                    while (!serverSocket.isClosed()) {
                        textArea.append("클라이언트 Socket 접속 대기중\n");
                        // 클라이언트 접속을 기다렸다가, 접속하면 소켓 반환
                        Socket clientSocket = serverSocket.accept();
                        textArea.append("클라이언트 Socket 접속 완료\n");

                        // 새 클라이언트 정보를 관리할 스레드 생성 및 시작
                        ClientInfo client = new ClientInfo(clientSocket);
                        client.start();
                    }
                } catch (IOException e) {
                    // 서버 소켓이 닫히지 않은 상태에서만 오류 메시지 출력
                    if (!serverSocket.isClosed()) {
                        textArea.append("클라이언트 연결 수락 중 오류 발생: " + e.getMessage() + "\n");
                    }
                }
            }
        }).start();
    }

    class ClientInfo extends Thread {
        private DataInputStream dis;
        private DataOutputStream dos;
        private Socket clientSocket;
        private String clientID = "";
        private String roomID = ""; // 현재 클라이언트가 들어가 있는 채팅방 ID

        public ClientInfo(Socket socket) {
            try {
                this.clientSocket = socket;
                dis = new DataInputStream(clientSocket.getInputStream());
                dos = new DataOutputStream(clientSocket.getOutputStream());
                initNewClient(); // 새 클라이언트 접속 초기화(아이디 등록, 기존 정보 전송 등)
            } catch (IOException e) {
                textArea.append("통신 오류 발생: " + e.getMessage() + "\n");
            }
        }

        public void run() {
            try {
                String msg = "";
                // 파일 전송과 일반 메시지 수신을 분리하여 처리
                while (true) {
                    msg = dis.readUTF();
                    parseMsg(msg); // 수신한 메시지를 프로토콜에 따라 해석/처리
                }
            } catch (IOException e) {
                handleClientExitProtocol();
            }
        }

        private void initNewClient() {
            // 클라이언트가 보낸 ID를 등록하고, 중복 여부 확인 후 처리
            while (true) {
                try {
                    clientID = dis.readUTF();

                    // 현재 접속 중인 클라이언트 목록에서 ID 중복 여부 검사
                    boolean isDuplicate = false;
                    for (int i = 0; i < clientVC.size(); i++) {
                        ClientInfo c = clientVC.elementAt(i);
                        if (c.clientID.equals(clientID)) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (isDuplicate) {
                        // ID가 중복인 경우 클라이언트에게 알리고 연결 종료
                        sendMsg("DuplicateClientID");
                        closeStreams();
                        break;

                    } else {
                        sendMsg("GoodClientID");

                        textArea.append("new Client: " + clientID + "\n");

                        // 새 클라이언트에게 기존 클라이언트 목록 전송
                        for (int i = 0; i < clientVC.size(); i++) {
                            ClientInfo c = clientVC.elementAt(i);
                            sendMsg("OldClient/" + c.clientID);
                        }

                        // 기존 클라이언트들에게 새 클라이언트 접속을 방송
                        broadCast("NewClient/" + clientID);

                        // 새 클라이언트에게 기존 방 목록 전송
                        for (RoomInfo r : roomVC) {
                            sendMsg("OldRoom/" + r.roomID);
                        }

                        // 방 목록 JList 갱신 요청 메시지 전송
                        sendMsg("RoomJlistUpdate/Update");
                        // 접속 중인 클라이언트 목록에 자신 추가
                        clientVC.add(this);
                        // 전체 클라이언트 목록 갱신 방송
                        broadCast("ClientJlistUpdate/Update");
                        break;
                    }
                } catch (IOException e) {
                    textArea.append("통신 중 오류 발생: " + e.getMessage() + "\n");
                    break;
                }
            }
        }

        void sendMsg(String msg) {
            // 현재 클라이언트에게 문자열 메시지 전송
            try {
                dos.writeUTF(msg);
            } catch (IOException e) {
                textArea.append("메시지 전송 오류: " + e.getMessage() + "\n");
            }
        }

        public void parseMsg(String str) {
            // 클라이언트로부터 받은 메시지를 서버 로그에 출력
            textArea.append(clientID + " 사용자로부터 수신한 메시지: " + str + "\n");
            System.out.println(clientID + " 사용자로부터 수신한 메시지: " + str);

            // '/' 구분자를 기준으로 프로토콜과 메시지를 분리
            StringTokenizer st = new StringTokenizer(str, "/");
            String protocol = st.nextToken();
            String message = "";
            if (st.hasMoreTokens()) {
                message = st.nextToken(); // 첫 번째 인자
            }

            // 프로토콜 종류에 따라 분기 처리
            switch (protocol) {
                case "Note":
                    handleNoteProtocol(st, message);
                    break;
                case "CreateRoom":
                    handleCreateRoomProtocol(message);
                    break;
                case "JoinRoom":
                    handleJoinRoomProtocol(st, message);
                    break;
                case "SendMsg":
                    handleSendMsgProtocol(st, message);
                    break;
                case "ImageTransfer": // **이미지 전송 요청 처리**
                    handleImageTransferProtocol(st, message);
                    break;
                case "FileRequest": // **파일 요청 처리**
                    handleFileRequestProtocol(st, message);
                    break;
                case "ClientExit":
                    handleClientExitProtocol();
                    break;
                case "ExitRoom":
                    handleExitRoomProtocol(message);
                    break;
                default:
                    log("알 수 없는 프로토콜: " + protocol);
                    break;
            }
        }

        // **이미지 파일 전송 요청 처리**
        private void handleImageTransferProtocol(StringTokenizer st, String roomID) {
            // ImageTransfer/방ID/파일명/파일크기
            String fileName = st.nextToken();
            long fileSize = Long.parseLong(st.nextToken());

            // 파일명을 중복 방지를 위해 클라이언트 ID와 타임스탬프를 붙여서 저장
            String uniqueFileName = clientID + "_" + System.currentTimeMillis() + "_" + fileName;
            String savePath = FILE_SAVE_DIR + uniqueFileName;

            // File 객체를 생성하여 절대 경로를 얻습니다.
            File savedFile = new File(savePath);

            try (FileOutputStream fos = new FileOutputStream(savedFile)) {
                // 1. 클라이언트가 보낸 바이트 스트림을 DataInputStream을 통해 직접 읽어 파일에 쓰기 (핵심!)
                byte[] buffer = new byte[4096];
                int bytesRead;
                long remaining = fileSize;

                while (remaining > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
                fos.flush();

                // 2. 파일의 '절대 경로'를 얻습니다. **경로 불일치 문제 해결**
                String absolutePath = savedFile.getAbsolutePath();

                textArea.append(clientID + "로부터 " + fileName + " (" + fileSize + " bytes) 수신 완료. 저장 경로: " + absolutePath + "\n");

                // 3. 방 멤버들에게 파일이 도착했음을 알릴 때, '절대 경로'를 전송합니다.
                for (RoomInfo r : roomVC) {
                    if (r.roomID.equals(roomID)) {
                        // ImageReceived/보낸사람ID/파일명/서버저장경로 (절대 경로)
                        r.broadcastRoomMsg("ImageReceived/" + clientID + "/" + fileName + "/" + absolutePath);
                        break;
                    }
                }
            } catch (IOException e) {
                textArea.append("이미지 수신 중 오류 발생: " + e.getMessage() + "\n");
            }
        }

        // **클라이언트 파일 다운로드 요청 처리**
        private void handleFileRequestProtocol(StringTokenizer st, String filePath) {
            // FileRequest/서버에저장된경로(절대경로)/다운로드될파일명/보낸사람ID
            String fileName = st.nextToken();
            String senderID = st.nextToken(); // **보낸 사람 ID 수신**

            File fileToSend = new File(filePath);

            textArea.append(clientID + "가 파일 요청: [" + fileName + "] 경로: " + fileToSend.getAbsolutePath() + "\n");

            if (!fileToSend.exists() || !fileToSend.isFile()) {
                sendMsg("FileError/파일을 찾을 수 없습니다.");
                textArea.append("!!파일 찾기 실패!! 요청 경로에 파일이 존재하지 않음: " + fileToSend.getAbsolutePath() + "\n");
                return;
            }

            long fileSize = fileToSend.length();

            try (FileInputStream fis = new FileInputStream(fileToSend)) {
                // 1. 클라이언트에게 파일 전송을 준비하라고 알림 (메타데이터 전송)
                // FileReady/다운로드될파일명/파일크기/서버저장경로/보낸사람ID
                sendMsg("FileReady/" + fileName + "/" + fileSize + "/" + filePath + "/" + senderID);

                // 2. 파일 데이터를 DataOutputStream을 통해 직접 전송
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                dos.flush();

                textArea.append(clientID + "에게 " + fileName + " 전송 완료.\n");

            } catch (IOException e) {
                textArea.append(clientID + "에게 파일 전송 중 오류 발생: " + e.getMessage() + "\n");
            }
        }

        private void handleNoteProtocol(StringTokenizer st, String receiverID) {
            // Note/받는사람ID/쪽지내용 형식의 프로토콜 처리
            String note = st.nextToken(); // 쪽지 내용

            // 전체 클라이언트 중에서 받는 사람 ID를 찾아 쪽지 전송
            for (ClientInfo c : clientVC) {
                if (c.clientID.equals(receiverID)) {
                    c.sendMsg("Note/" + clientID + "/" + note); // Note/보낸사람ID/쪽지내용
                    break;
                }
            }
        }

        private void handleCreateRoomProtocol(String roomID) {
            // 방 생성 요청 처리: CreateRoom/방이름
            boolean roomExists = false;
            // 같은 이름의 방이 이미 존재하는지 확인
            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {
                    roomExists = true;
                    break;
                }
            }
            if (roomExists) {
                // 이미 존재하면 방 생성 실패 메시지 전송
                sendMsg("CreateRoomFail/OK");
            } else {
                RoomInfo r = new RoomInfo(roomID, this);
                roomVC.add(r);
                this.roomID = roomID; // 현재 클라이언트의 방 ID 설정
                sendMsg("CreateRoom/" + roomID); // 자기 자신에게 방 생성 성공 알림
                broadCast("NewRoom/" + roomID); // 전체에게 새 방 생성 알림
                broadCast("RoomJlistUpdate/Update"); // 방 목록 갱신 요청 방송
            }
        }

        private void handleJoinRoomProtocol(StringTokenizer st, String roomID) {
            // 방 참여 요청 처리: JoinRoom/방이름
            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {
                    // 해당 방에 입장 메시지를 방 안의 모든 사람에게 방송
                    r.broadcastRoomMsg("JoinRoomMsg/가입/***" + clientID + "님이 입장하셨습니다.********");
                    // 방의 클라이언트 목록에 자신 추가
                    r.RoomClientVC.add(this);
                    this.roomID = roomID; // 현재 클라이언트의 방 ID 설정

                    sendMsg("JoinRoom/" + roomID); // 자신에게 방 참여 완료 메시지 전송
                    break;
                }
            }
        }

        private void handleSendMsgProtocol(StringTokenizer st, String roomID) {
            // 방 내부 채팅 메시지 처리: SendMsg/방이름/메시지내용
            String sendMsg = st.nextToken(); // 실제 보낼 메시지 내용
            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {
                    // 해당 방에 있는 모든 클라이언트에게 메시지 방송
                    r.broadcastRoomMsg("SendMsg/" + clientID + "/" + sendMsg);
                }
            }
        }

        // 클라이언트 종료 처리
        private void handleClientExitProtocol() {
            try {
                closeStreams();
                clientVC.remove(this);
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    textArea.append(clientID + " Client Socket 종료.\n");
                }

                broadCast("ClientExit/" + clientID);
                broadCast("ClientJlistUpdate/Update");

            } catch (IOException e) {
                logError("사용자 로그아웃 중 오류 발생", e);
            }
        }

        private void handleExitRoomProtocol(String roomID) {
            // 방 나가기 요청 처리: ExitRoom/방이름
            this.roomID = ""; // 현재 사용자의 방 ID 초기화
            log(clientID + " 사용자가 " + roomID + " 방에서 나감");

            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {
                    // 방 안의 모든 클라이언트에게 퇴장 메시지 방송
                    r.broadcastRoomMsg("ExitRoomMsg/탈퇴/***" + clientID + "님이 채팅방에서 나갔습니다.********");
                    // 방의 클라이언트 목록에서 현재 사용자 제거
                    r.RoomClientVC.remove(this);
                    // 방에 남은 사람이 없으면 방 삭제
                    if (r.RoomClientVC.isEmpty()) {
                        roomVC.remove(r);              // 방 목록에서 제거
                        broadCast("RoomOut/" + roomID); // 방 삭제 알림
                        broadCast("RoomJlistUpdate/Update"); // 방 목록 갱신 방송
                    }
                    break;
                }
            }
        }

        private void broadCast(String str) {
            // 전체 접속 클라이언트에게 같은 메시지 방송
            for (ClientInfo c : clientVC) {
                c.sendMsg(str);
            }
        }

        private void log(String message) {
            System.out.println(clientID + ": " + message);
        }

        private void logError(String message, Exception e) {
            System.err.println(clientID + ": " + message);
            e.printStackTrace();
        }

        public void closeStreams() throws IOException {
            if (dos != null) {
                dos.close();
            }
            if (dis != null) {
                dis.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                textArea.append(clientID + " Client Socket 종료.\n");
            }
        }
    }

    class RoomInfo {

        private String roomID; // 방 이름(방 ID)
        private Vector<ClientInfo> RoomClientVC; // 이 방에 참여 중인 클라이언트 목록

        public RoomInfo(String roomID, ClientInfo c) {

            this.roomID = roomID; // 방 이름 설정

            this.RoomClientVC = new Vector<ClientInfo>(); // 방에 속한 클라이언트 벡터 생성

            this.RoomClientVC.add(c); // 방을 만든 클라이언트를 첫 멤버로 추가

        }

        public void broadcastRoomMsg(String message) {

            // 이 방에 속한 모든 클라이언트에게 메시지 방송
            for (ClientInfo c : RoomClientVC) {
                c.sendMsg(message);
            }
        }
    }

    public static void main(String[] args) {
        new Server2025();
    }
}