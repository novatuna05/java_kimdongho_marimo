import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class Server2025 extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    // ===== GUI =====
    private JPanel contentPane;
    private JTextArea textArea;
    private JButton startBtn;
    private JButton stopBtn;
    private JTextField port_tf;

    // ===== 서버 소켓 =====
    private ServerSocket serverSocket;
    private int port = 12345;

    // 서버에 저장할 파일/이미지 디렉토리
    private static final String FILE_SAVE_DIR = "server_files";

    // ===== 서버가 관리하는 공용 데이터 =====
    private final Vector<ClientInfo> clientVC = new Vector<>();
    private final Vector<RoomInfo> roomVC = new Vector<>();

    public Server2025() {
        initGUI();
        setupActionListeners();
        setVisible(true);
    }

    // -------------------- GUI 초기화 --------------------
    private void initGUI() {
        setTitle("Server Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 600, 500);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout());
        setContentPane(contentPane);

        // 상단: 포트 + 버튼
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentPane.add(topPanel, BorderLayout.NORTH);

        JLabel portLabel = new JLabel("Port 번호");
        topPanel.add(portLabel);

        port_tf = new JTextField(String.valueOf(port));
        port_tf.setColumns(10);
        topPanel.add(port_tf);

        startBtn = new JButton("서버 실행");
        topPanel.add(startBtn);

        stopBtn = new JButton("서버 중지");
        stopBtn.setEnabled(false);
        topPanel.add(stopBtn);

        // 중앙: 로그 출력 영역
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        contentPane.add(scrollPane, BorderLayout.CENTER);
    }

    private void setupActionListeners() {
        startBtn.addActionListener(this);
        stopBtn.addActionListener(this);
    }

    // -------------------- 버튼 이벤트 --------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startBtn) {
            startServer();
        } else if (e.getSource() == stopBtn) {
            stopServer();
        }
    }

    // -------------------- 서버 시작 --------------------
    private void startServer() {
        try {
            port = Integer.parseInt(port_tf.getText().trim());
            serverSocket = new ServerSocket(port);
            appendLog("서버가 포트 " + port + "에서 시작되었습니다.");

            // 파일 저장 디렉토리 생성
            File dir = new File(FILE_SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            port_tf.setEditable(false);

            waitForClientConnection();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "유효한 포트 번호를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 시작 중 오류 발생: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------- 서버 중지 --------------------
    private void stopServer() {
        // 접속 중인 클라이언트에게 서버 종료 알림 후 정리
        Vector<ClientInfo> clientsCopy = new Vector<>(clientVC);
        for (ClientInfo c : clientsCopy) {
            c.sendMsg("ServerShutdown/Bye");
            try {
                c.closeStreams();
            } catch (IOException ex) {
                appendLog("클라이언트 스트림 정리 중 오류: " + ex.getMessage());
            }
        }

        clientVC.clear();
        roomVC.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                appendLog("서버가 중지되었습니다.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 중지 중 오류 발생: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }

        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        port_tf.setEditable(true);
    }

    // -------------------- 클라이언트 접속 대기 쓰레드 --------------------
    private void waitForClientConnection() {
        new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    appendLog("클라이언트 Socket 접속 대기중");
                    Socket clientSocket = serverSocket.accept();
                    appendLog("클라이언트 Socket 접속 완료");

                    ClientInfo client = new ClientInfo(clientSocket);
                    client.start();
                }
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    appendLog("클라이언트 연결 수락 중 오류 발생: " + e.getMessage());
                }
            }
        }).start();
    }

    // -------------------- 로그 출력 헬퍼 --------------------
    private void appendLog(String msg) {
        textArea.append(msg + "\n");
    }

    // =======================================================================
    //                             클라이언트 쓰레드
    // =======================================================================
    class ClientInfo extends Thread {
        private DataInputStream dis;
        private DataOutputStream dos;
        private Socket clientSocket;
        private String clientID = "";
        private String roomID = "";

        private boolean registered = false; // ID가 정상 등록된 클라이언트인지 여부

        public ClientInfo(Socket socket) {
            try {
                this.clientSocket = socket;
                dis = new DataInputStream(clientSocket.getInputStream());
                dos = new DataOutputStream(clientSocket.getOutputStream());
                initNewClient();
            } catch (IOException e) {
                appendLog("통신 오류 발생(생성자): " + e.getMessage());
            }
        }

        // -------------------- 새 클라이언트 초기화(ID 등록, 중복 확인 등) --------------------
        private void initNewClient() {
            while (true) {
                try {
                    clientID = dis.readUTF(); // 클라이언트가 먼저 ID 전송

                    // ID 중복 검사
                    boolean isDuplicate = false;
                    for (ClientInfo c : clientVC) {
                        if (c.clientID.equals(clientID)) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (isDuplicate) {
                        // 중복이면 실패 메시지 전송 후 등록하지 않음
                        sendMsg("DuplicateClientID");
                        appendLog("중복 ID 접속 시도: " + clientID);
                        return;
                    } else {
                        // 사용 가능한 ID
                        sendMsg("GoodClientID");
                        appendLog("새 클라이언트 접속: " + clientID);

                        // 새 클라에게 기존 클라이언트 목록 전송
                        for (ClientInfo c : clientVC) {
                            sendMsg("OldClient/" + c.clientID);
                        }

                        // 기존 클라이언트들에게 새 클라 접속 방송
                        broadCast("NewClient/" + clientID);

                        // 새 클라에게 기존 방 목록 전송
                        for (RoomInfo r : roomVC) {
                            sendMsg("OldRoom/" + r.roomID);
                        }

                        // 새 클라에게 방 목록 갱신 요청
                        sendMsg("RoomJlistUpdate/Update");

                        // 공용 벡터에 자신 추가
                        clientVC.add(this);

                        // 전체 클라에게 클라이언트 목록 갱신 방송
                        broadCast("ClientJlistUpdate/Update");

                        registered = true;
                        break;
                    }
                } catch (IOException e) {
                    appendLog("새 클라이언트 초기화 중 오류 발생: " + e.getMessage());
                    break;
                }
            }
        }

        @Override
        public void run() {
            if (!registered) {
                // ID 중복 등으로 등록 실패한 경우
                return;
            }

            try {
                while (true) {
                    String str = dis.readUTF();
                    parseMsg(str);
                }
            } catch (IOException e) {
                appendLog("통신 중 오류 발생: " + e.getMessage());
            } finally {
                handleClientDisconnect();
            }
        }

        // -------------------- 클라이언트 연결 종료 처리 --------------------
        private void handleClientDisconnect() {
            if (!registered) {
                // 등록 안 된 클라면 최소한 소켓만 정리
                try {
                    closeStreams();
                } catch (IOException e) {
                    appendLog("클라이언트 스트림 정리 중 오류: " + e.getMessage());
                }
                return;
            }

            try {
                appendLog("클라이언트 연결 종료: " + clientID);

                // 방에 있었다면 방에서 나가게 처리
                if (roomID != null && !roomID.isEmpty()) {
                    handleExitRoomProtocol(roomID);
                }

                clientVC.remove(this);
                closeStreams();
            } catch (IOException e) {
                appendLog("클라이언트 종료 처리 중 오류 발생: " + e.getMessage());
            } finally {
                broadCast("ClientExit/" + clientID);
                broadCast("ClientJlistUpdate/Update");
            }
        }

        // -------------------- 스트림/소켓 종료 --------------------
        public void closeStreams() throws IOException {
            if (dos != null) {
                dos.close();
            }
            if (dis != null) {
                dis.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                appendLog(clientID + " Client Socket 종료.");
            }
        }

        // -------------------- 문자열 메시지 전송 --------------------
        void sendMsg(String msg) {
            try {
                dos.writeUTF(msg);
            } catch (IOException e) {
                appendLog("메시지 전송 오류: " + e.getMessage());
            }
        }

        // -------------------- 파일(바이트) 전송 (File 프로토콜용) --------------------
        void sendFile(String senderID, String fileName, byte[] data) {
            try {
                String header = "File/" + senderID + "/" + fileName + "/" + data.length;
                dos.writeUTF(header);
                dos.write(data);
                dos.flush();
            } catch (IOException e) {
                appendLog("파일 전송 오류: " + e.getMessage());
            }
        }

        // -------------------- 메시지 파싱 --------------------
        public void parseMsg(String str) {
            appendLog(clientID + " 사용자로부터 수신: " + str);

            StringTokenizer st = new StringTokenizer(str, "/");
            String protocol = st.nextToken();
            String message = st.hasMoreTokens() ? st.nextToken() : "";

            switch (protocol) {
                case "Note":
                    handleNoteProtocol(st, message);
                    break;
                case "CreateRoom":
                    handleCreateRoomProtocol(message);
                    break;
                case "JoinRoom":
                    handleJoinRoomProtocol(message);
                    break;
                case "SendMsg":
                    handleSendMsgProtocol(st, message);
                    break;
                case "File":
                    // File/보낸사람ID/파일명/파일크기 (+바이너리 데이터)
                    handleFileProtocol(st, message); // message = senderID
                    break;
                case "ImageTransfer":
                    // ImageTransfer/방ID/파일명/파일크기 (+바이너리 데이터)
                    handleImageTransferProtocol(st, message); // message = roomID
                    break;
                case "FileRequest":
                    // FileRequest/서버저장경로(절대경로)/다운로드될파일명/보낸사람ID
                    handleFileRequestProtocol(st, message); // message = filePath
                    break;
                case "ClientExit":
                    handleClientExitProtocol();
                    break;
                case "ExitRoom":
                    handleExitRoomProtocol(message);
                    break;
                default:
                    appendLog("알 수 없는 프로토콜: " + protocol);
                    break;
            }
        }

        // -------------------- 쪽지 처리: Note/받는사람ID/내용 --------------------
        private void handleNoteProtocol(StringTokenizer st, String receiverID) {
            String note = st.hasMoreTokens() ? st.nextToken() : "";
            for (ClientInfo client : clientVC) {
                if (client.clientID.equals(receiverID)) {
                    client.sendMsg("Note/" + clientID + "/" + note);
                    break;
                }
            }
        }

        // -------------------- 방 생성: CreateRoom/방이름 --------------------
        private void handleCreateRoomProtocol(String roomID) {
            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {
                    // 방 이름 중복
                    sendMsg("CreateRoomFail/방 이름이 중복됩니다.");
                    return;
                }
            }

            RoomInfo newRoom = new RoomInfo(roomID, this);
            roomVC.add(newRoom);
            this.roomID = roomID;

            sendMsg("CreateRoom/" + roomID);     // 자기 자신에게 생성 성공
            broadCast("NewRoom/" + roomID);      // 전체에게 새 방 알림
            broadCast("RoomJlistUpdate/Update"); // 방 목록 갱신
        }

        // -------------------- 방 참가: JoinRoom/방이름 --------------------
        private void handleJoinRoomProtocol(String roomID) {
            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {
                    r.broadcastRoomMsg("JoinRoomMsg/가입/***" + clientID + "님이 입장하셨습니다.********");
                    if (!r.RoomClientVC.contains(this)) {
                        r.RoomClientVC.add(this);
                    }
                    this.roomID = roomID;
                    sendMsg("JoinRoom/" + roomID);
                    break;
                }
            }
        }

        // -------------------- 방 내 채팅: SendMsg/방이름/메시지 --------------------
        private void handleSendMsgProtocol(StringTokenizer st, String roomID) {
            String sendMsg = st.hasMoreTokens() ? st.nextToken() : "";
            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {
                    r.broadcastRoomMsg("SendMsg/" + clientID + "/" + sendMsg);
                }
            }
        }

        // -------------------- (구) 파일 전송: File/보낸사람ID/파일명/파일크기 --------------------
        private void handleFileProtocol(StringTokenizer st, String senderID) {
            try {
                if (roomID == null || roomID.isEmpty()) {
                    appendLog("방에 속해 있지 않은 사용자의 파일 전송 시도: " + senderID);
                    return;
                }

                String fileName = st.nextToken();
                long fileSize = Long.parseLong(st.nextToken());

                byte[] data = new byte[(int) fileSize];
                dis.readFully(data);

                for (RoomInfo r : roomVC) {
                    if (r.roomID.equals(roomID)) {
                        r.broadcastFileMsg(senderID, fileName, data);
                        break;
                    }
                }

                appendLog(senderID + " 사용자가 방(" + roomID + ")에 파일 전송: " + fileName);
            } catch (Exception e) {
                appendLog("파일 수신/브로드캐스트 중 오류: " + e.getMessage());
            }
        }

        // -------------------- 이미지 전송: ImageTransfer/방ID/파일명/파일크기 --------------------
        private void handleImageTransferProtocol(StringTokenizer st, String roomID) {
            if (!st.hasMoreTokens()) return;
            String fileName = st.nextToken();
            if (!st.hasMoreTokens()) return;
            long fileSize = Long.parseLong(st.nextToken());

            // 저장 디렉토리 보장
            File dir = new File(FILE_SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String uniqueFileName = clientID + "_" + System.currentTimeMillis() + "_" + fileName;
            File savedFile = new File(dir, uniqueFileName);

            try (FileOutputStream fos = new FileOutputStream(savedFile)) {
                byte[] buffer = new byte[4096];
                long remaining = fileSize;
                while (remaining > 0) {
                    int bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (bytesRead == -1) break;
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
                fos.flush();

                String absolutePath = savedFile.getAbsolutePath();
                appendLog(clientID + "로부터 " + fileName + " (" + fileSize + " bytes) 수신 완료. 저장 경로: " + absolutePath);

                // 방 멤버들에게 이미지 도착 알림 (서버 저장 경로 포함)
                for (RoomInfo r : roomVC) {
                    if (r.roomID.equals(roomID)) {
                        r.broadcastRoomMsg("ImageReceived/" + clientID + "/" + fileName + "/" + absolutePath);
                        break;
                    }
                }
            } catch (IOException e) {
                appendLog("이미지 수신 중 오류 발생: " + e.getMessage());
            }
        }

        // -------------------- 파일 다운로드 요청: FileRequest/경로/다운로드명/보낸사람ID --------------------
        private void handleFileRequestProtocol(StringTokenizer st, String filePath) {
            if (!st.hasMoreTokens()) return;
            String fileName = st.nextToken();
            if (!st.hasMoreTokens()) return;
            String senderID = st.nextToken();

            File fileToSend = new File(filePath);
            appendLog(clientID + "가 파일 요청: [" + fileName + "] 경로: " + fileToSend.getAbsolutePath());

            if (!fileToSend.exists() || !fileToSend.isFile()) {
                sendMsg("FileError/파일을 찾을 수 없습니다.");
                appendLog("!!파일 찾기 실패!! 요청 경로에 파일이 존재하지 않음: " + fileToSend.getAbsolutePath());
                return;
            }

            long fileSize = fileToSend.length();

            try (FileInputStream fis = new FileInputStream(fileToSend)) {
                // 메타데이터 전송
                // FileReady/다운로드될파일명/파일크기/서버저장경로/보낸사람ID
                sendMsg("FileReady/" + fileName + "/" + fileSize + "/" + filePath + "/" + senderID);

                // 실제 파일 데이터 전송
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                dos.flush();

                appendLog(clientID + "에게 " + fileName + " 전송 완료.");
            } catch (IOException e) {
                appendLog(clientID + "에게 파일 전송 중 오류 발생: " + e.getMessage());
            }
        }

        // -------------------- 클라이언트가 자발적으로 종료: ClientExit --------------------
        private void handleClientExitProtocol() {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close(); // run()에서 IOException 발생 → finally에서 handleClientDisconnect 호출
                    appendLog(clientID + " Client Socket 종료(ClientExit 프로토콜).");
                }
            } catch (IOException e) {
                appendLog("ClientExit 처리 중 오류 발생: " + e.getMessage());
            }
        }

        // -------------------- 방 나가기: ExitRoom/방이름 --------------------
        private void handleExitRoomProtocol(String roomID) {
            this.roomID = "";
            appendLog(clientID + " 사용자가 " + roomID + " 방에서 나감");

            for (RoomInfo r : new Vector<>(roomVC)) {
                if (r.roomID.equals(roomID)) {

                    // 방 안 모두에게 퇴장 메시지
                    r.broadcastRoomMsg("ExitRoomMsg/탈퇴/***" + clientID + "님이 채팅방에서 나갔습니다.********");

                    // 방의 클라이언트 목록에서 제거
                    r.RoomClientVC.remove(this);

                    // 나가는 본인에게 UI 리셋용 메시지
                    sendMsg("ExitRoom/" + roomID);

                    // 방 인원이 0명이면 방 삭제
                    if (r.RoomClientVC.isEmpty()) {
                        roomVC.remove(r);
                        broadCast("RoomOut/" + roomID);
                        broadCast("RoomJlistUpdate/Update");
                    }
                    break;
                }
            }
        }

        // -------------------- 전체 클라이언트 브로드캐스트 --------------------
        private void broadCast(String str) {
            for (ClientInfo c : clientVC) {
                c.sendMsg(str);
            }
        }
    }

    // =======================================================================
    //                               방 정보
    // =======================================================================
    class RoomInfo {
        private String roomID;
        private Vector<ClientInfo> RoomClientVC;

        public RoomInfo(String roomID, ClientInfo c) {
            this.roomID = roomID;
            this.RoomClientVC = new Vector<>();
            this.RoomClientVC.add(c);
        }

        public void broadcastRoomMsg(String message) {
            for (ClientInfo c : RoomClientVC) {
                c.sendMsg(message);
            }
        }

        // (구) File 프로토콜용 파일 브로드캐스트
        public void broadcastFileMsg(String senderID, String fileName, byte[] data) {
            for (ClientInfo c : RoomClientVC) {
                c.sendFile(senderID, fileName, data);
            }
        }
    }

    // -------------------- main --------------------
    public static void main(String[] args) {
        new Server2025();
    }
}
