package ru.geekbrains.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    //добавил формат даты для логов
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        // /auth login1 password1
                        if (str.startsWith("/auth")) {
                            String[] subStrings = str.split(" ", 3);
                            if (subStrings.length == 3) {
                                String nickFromDB = SQLHandler.getNickByLoginAndPassword(subStrings[1], subStrings[2]);
                                if (nickFromDB != null) {
                                    if (!server.isNickInChat(nickFromDB)) {
                                        nickname = nickFromDB;
                                        sendMsg("/authok " + nickname);
                                        server.subscribe(this);
                                        break;
                                    } else {
                                        sendMsg("This nick already in use");
                                    }
                                } else {
                                    sendMsg("Wrong login/password");
                                }
                            } else {
                                sendMsg("Wrong data format");
                            }
                        }
                        if (str.startsWith("/registration")) {
                            String[] subStr = str.split(" ");
                            // /registration login pass nick
                            if (subStr.length == 4) {
                                if (SQLHandler.tryToRegister(subStr[1], subStr[2], subStr[3])) {
                                    sendMsg("Registration complete");
                                } else {
                                    sendMsg("Incorrect login/password/nickname");
                                }
                            }
                        }
                    }


                    while (true) {
                        String str = in.readUTF();
                        System.out.println("[" + dateFormat.format(new Date()) + "]" + "Сообщение от клиента: " + str);
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            } else if (str.startsWith("/w")) {
                                // личные сообщения
                                // /w nick hello m8! hi
                                final String[] subStrings = str.split(" ", 3);
                                if (subStrings.length == 3) {
                                    final String toUserNick = subStrings[1];
                                    if (server.isNickInChat(toUserNick)) {
                                        server.unicastMsg(toUserNick, "[" + dateFormat.format(new Date()) + "]" + " " + "from " + nickname + ": " + subStrings[2]);
                                        sendMsg("[" + dateFormat.format(new Date()) + "]" + " " + "to " + toUserNick + ": " + subStrings[2]);
                                    } else {
                                        sendMsg("User with nick '" + toUserNick + "' not found in chat room");
                                    }
                                } else {
                                    sendMsg("Wrong private message");
                                }
                            } else if (str.startsWith("/changenick")){
                                // /changenick Ololo
                                String[] subStr = str.split(" ");
                                if (subStr.length == 2){
                                    if (SQLHandler.tryToChangeNick(subStr[1], nickname)){
                                        sendMsg("[" + dateFormat.format(new Date()) + "]" + nickname + " изменён на " + subStr[1]);
                                        sendMsg("Изменения вступят в силу после перезахода");
                                        server.broadcastMsg("[" + dateFormat.format(new Date()) + "]" + nickname + " changed has nickname to " + subStr[1]);
                                        break;
                                    } else {
                                        sendMsg("Incorrect nickname");
                                    }
                                }
                            }
                        } else {
                            server.broadcastMsg("[" + dateFormat.format(new Date()) + "]" + " " + nickname + ": " + str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(this);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }
}
