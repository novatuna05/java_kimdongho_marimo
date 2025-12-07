
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    private JTextArea textArea;
    private JButton startBtn;
    private JButton stopBtn;
    private JTextField port_tf;

    private ServerSocket serverSocket;
    private int port = 12345;

    private Vector<ClientInfo> clientVC = new Vector<>();
    private Vector<RoomInfo> roomVC = new Vector<>();

    public Server2025() {
        initGUI();
        setupActionListeners();
        setVisible(true);
    }

    public void initGUI() {
        setTitle("Server Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 600, 500);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(null);
        setContentPane(contentPane);

        JLabel portLabel = new JLabel("Port 번호");
        portLabel.setBounds(12, 20, 57, 15);
        contentPane.add(portLabel);

        port_tf = new JTextField("12345");
        port_tf.setBounds(81, 17, 116, 21);
        contentPane.add(port_tf);

        startBtn = new JButton("서버 실행");
        startBtn.setBounds(209, 16, 120, 23);
        contentPane.add(startBtn);

        stopBtn = new JButton("서버 중지");
        stopBtn.setBounds(341, 16, 120, 23);
        stopBtn.setEnabled(false);
        contentPane.add(stopBtn);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(12, 56, 560, 395);
        contentPane.add(scrollPane);

        textArea = new JTextArea();
        scrollPane.setViewportView(textArea);
        textArea.setEditable(false);
    }

    public void setupActionListeners() {
        startBtn.addActionListener(this);
        stopBtn.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startBtn) startServer();
        else if (e.getSource() == stopBtn) stopServer();
    }

    public void startServer() {
        try {
            port = Integer.parseInt(port_tf.getText().trim());
            serverSocket = new ServerSocket(port);
            textArea.append("서버가 포트 " + port + "에서 시작되었습니다.\n");
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);

            new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        textArea.append("클라이언트 Socket 접속 대기중\n");
                        Socket clientSocket = serverSocket.accept();
                        textArea.append("클라이언트 Socket 접속 완료\n");
                        ClientInfo client = new ClientInfo(clientSocket);
                        client.start();
                    }
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        textArea.append("클라이언트 연결 수락 중 오류 발생: " + e.getMessage() + "\n");
                    }
                }
            }).start();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "유효한 포트 번호를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 시작 중 오류 발생: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                textArea.append("서버가 중지되었습니다.\n");
            }
            for (ClientInfo client : clientVC) {
                client.closeStreams();
            }
            clientVC.clear();
            roomVC.clear();

            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 중지 중 오류 발생: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== 클라이언트 쓰레드 =====
    class ClientInfo extends Thread {
        private DataInputStream dis;
        private DataOutputStream dos;
        private Socket clientSocket;
        private String clientID = "";
        private String roomID = "";

        public ClientInfo(Socket socket) {
            try {
                this.clientSocket = socket;
                dis = new DataInputStream(clientSocket.getInputStream());
                dos = new DataOutputStream(clientSocket.getOutputStream());
                initNewClient();
            } catch (IOException e) {
                textArea.append("통신 오류 발생: " + e.getMessage() + "\n");
            }
        }

        private void initNewClient() {
            try {
                clientID = dis.readUTF();
                textArea.append("새 클라이언트 접속: " + clientID + "\n");

                // 기존 클라이언트 목록 전송
                for (ClientInfo c : clientVC) {
                    sendMsg("OldClient/" + c.clientID);
                }

                clientVC.add(this);

                // 기존 방 목록 전송
                for (RoomInfo r : roomVC) {
                    sendMsg("OldRoom/" + r.roomID);
                }

                // 다른 클라이언트에게 새 클라 알림
                broadCast("NewClient/" + clientID);

            } catch (IOException e) {
                textArea.append("새 클라이언트 초기화 중 오류 발생: " + e.getMessage() + "\n");
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String str = dis.readUTF();
                    parseMsg(str);
                }
            } catch (IOException e) {
                textArea.append("통신 중 오류 발생: " + e.getMessage() + "\n");
            } finally {
                handleClientDisconnect();
            }
        }

        private void handleClientDisconnect() {
            try {
                textArea.append("클라이언트 연결 종료: " + clientID + "\n");
                // 방에 있었다면 방에서 제거
                if (roomID != null && !roomID.isEmpty()) {
                    // 방 탈퇴 로직 재사용
                    handleExitRoomProtocol(roomID);
                }
                closeStreams();
                clientVC.remove(this);
                if (!clientSocket.isClosed()) clientSocket.close();
                broadCast("ClientExit/" + clientID);
            } catch (IOException e) {
                textArea.append("클라이언트 종료 처리 중 오류 발생: " + e.getMessage() + "\n");
            }
        }

        void closeStreams() {
            try {
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                textArea.append("스트림 종료 중 오류 발생: " + e.getMessage() + "\n");
            }
        }

        void sendMsg(String msg) {
            try {
                dos.writeUTF(msg);
            } catch (IOException e) {
                textArea.append("메시지 전송 오류: " + e.getMessage() + "\n");
            }
        }

        void sendFile(String senderID, String fileName, byte[] data) {
            try {
                String header = "File/" + senderID + "/" + fileName + "/" + data.length;
                dos.writeUTF(header);
                dos.write(data);
                dos.flush();
            } catch (IOException e) {
                textArea.append("파일 전송 오류: " + e.getMessage() + "\n");
            }
        }

        public void parseMsg(String str) {
            textArea.append(clientID + " 사용자로부터 수신: " + str + "\n");

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
                case "File":
                    handleFileProtocol(st, message); // message = senderID
                    break;
                case "SendMsg":
                    handleSendMsgProtocol(st, message);
                    break;
                case "ClientExit":
                    handleClientExitProtocol();
                    break;
                case "ExitRoom":
                    handleExitRoomProtocol(message);
                    break;
            }
        }

        private void handleNoteProtocol(StringTokenizer st, String receiverID) {
            String note = st.nextToken();
            for (ClientInfo client : clientVC) {
                if (client.clientID.equals(receiverID)) {
                    client.sendMsg("Note/" + clientID + "/" + note);
                    break;
                }
            }
        }

        private void handleCreateRoomProtocol(String roomID) {
            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {
                    sendMsg("CreateRoomFail/방 이름이 중복됩니다.");
                    return;
                }
            }

            RoomInfo newRoom = new RoomInfo(roomID, this);
            roomVC.add(newRoom);

            sendMsg("CreateRoom/" + roomID);
            broadCast("NewRoom/" + roomID);
            broadCast("RoomJlistUpdate/Update");
        }

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

        private void handleFileProtocol(StringTokenizer st, String senderID) {
            try {
                if (roomID == null || roomID.isEmpty()) {
                    textArea.append("방에 속해 있지 않은 사용자의 파일 전송 시도: " + senderID + "\n");
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

                textArea.append(senderID + " 사용자가 방(" + roomID + ")에 파일 전송: " + fileName + "\n");
            } catch (Exception e) {
                textArea.append("파일 수신/브로드캐스트 중 오류: " + e.getMessage() + "\n");
            }
        }

        private void handleSendMsgProtocol(StringTokenizer st, String roomID) {
            String sendMsg = st.nextToken();
            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {
                    r.broadcastRoomMsg("SendMsg/" + clientID + "/" + sendMsg);
                }
            }
        }

        private void handleClientExitProtocol() {
            try {
                // 방에 있었다면 먼저 방에서 나가게 처리
                if (roomID != null && !roomID.isEmpty()) {
                    handleExitRoomProtocol(roomID);
                }
                closeStreams();
                clientVC.remove(this);
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    textArea.append(clientID + " Client Socket 종료.\n");
                }
                broadCast("ClientExit/" + clientID);
            } catch (IOException e) {
                textArea.append("클라이언트 종료 처리 중 오류 발생: " + e.getMessage() + "\n");
            }
        }

        private void handleExitRoomProtocol(String roomID) {
            this.roomID = "";
            textArea.append(clientID + " 사용자가 " + roomID + " 방에서 나감\n");

            for (RoomInfo r : roomVC) {
                if (r.roomID.equals(roomID)) {

                    // 방 안 모든 사람에게 “누가 나갔다” 메시지
                    r.broadcastRoomMsg("ExitRoomMsg/탈퇴/***" + clientID + "님이 채팅방에서 나갔습니다.********");

                    // 방 목록에서 이 클라이언트 제거
                    r.RoomClientVC.remove(this);

                    // 이 클라이언트에게 UI 리셋 용 메시지 (ExitRoom/방이름)
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

        private void broadCast(String str) {
            for (ClientInfo c : clientVC) {
                c.sendMsg(str);
            }
        }
    }

    // ===== 방 정보 =====
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

        public void broadcastFileMsg(String senderID, String fileName, byte[] data) {
            for (ClientInfo c : RoomClientVC) {
                c.sendFile(senderID, fileName, data);
            }
        }
    }

    public static void main(String[] args) {
        new Server2025();
    }
}
