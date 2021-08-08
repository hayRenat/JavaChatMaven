package ru.geekbrains.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private Server server;
    private Socket socket;
//    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    //добавил формат даты для логов
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
    private static Logger logger = LogManager.getLogger();

    public ClientHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(() -> {
        try (DataInputStream in = new DataInputStream(socket.getInputStream()); DataOutputStream out = new DataOutputStream(socket.getOutputStream())){
            this.out = out;
                try {
                    while (true) {
                        String str = in.readUTF();
                        // /auth login1 password1
                        if (str.startsWith("/auth")) {
                            String[] subStrings = str.split(" ", 3);
                            if (subStrings.length == 3) {
                                logger.info("Пользователь пытается авторизоваться с логином " + subStrings[1]);
                                String nickFromDB = SQLHandler.getNickByLoginAndPassword(subStrings[1], subStrings[2]);
                                if (nickFromDB != null) {
                                    if (!server.isNickInChat(nickFromDB)) {
                                        nickname = nickFromDB;
                                        ClientHandler.this.sendMsg("/authok " + nickname);
                                        server.subscribe(ClientHandler.this);
                                        logger.info("Упешная авторизация пользователя " + subStrings[1]);
                                        break;
                                    } else {
                                        logger.warn("Пользователь " + subStrings[1] + " уже ранее авторизован в чате");
                                        ClientHandler.this.sendMsg("This nick already in use");
                                    }
                                } else {
                                    logger.warn("Неудачная попытка авторизации " + subStrings[1]);
                                    ClientHandler.this.sendMsg("Wrong login/password");
                                }
                            } else {
                                logger.warn("Пользователь ввёл не корректные данные для входа");
                                ClientHandler.this.sendMsg("Wrong data format");
                            }
                        }
                        if (str.startsWith("/registration")) {
                            String[] subStr = str.split(" ");
                            // /registration login pass nick
                            if (subStr.length == 4) {
                                logger.info("Попытка регистрации. Пользователь ввёл login = " + subStr[1] + " password + subStr[3]");
                                if (SQLHandler.tryToRegister(subStr[1], subStr[2], subStr[3])) {
                                    logger.info("Успешная регистрация пользователя " + subStr[1]);
                                    ClientHandler.this.sendMsg("Registration complete");
                                } else {
                                    logger.warn("Неудачная попытка регистрации пользователя " + subStr[1] + ", с ником " + subStr[3]);
                                    ClientHandler.this.sendMsg("Incorrect login/password/nickname");
                                }
                            }
                        }
                    }
                    while (true) {
                        String str = in.readUTF();
                        System.out.println("[" + dateFormat.format(new Date()) + "]" + "Сообщение от клиента: " + str);
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                logger.info("Пользователь " + nickname + " выходит из чата");
                                break;
                            } else if (str.startsWith("/w")) {
                                // личные сообщения
                                // /w nick hello m8! hi
                                final String[] subStrings = str.split(" ", 3);
                                if (subStrings.length == 3) {
                                    final String toUserNick = subStrings[1];
                                    if (server.isNickInChat(toUserNick)) {
                                        server.unicastMsg(toUserNick, "[" + dateFormat.format(new Date()) + "]" + " " + "from " + nickname + ": " + subStrings[2]);
                                        ClientHandler.this.sendMsg("[" + dateFormat.format(new Date()) + "]" + " " + "to " + toUserNick + ": " + subStrings[2]);
                                    } else {
                                        logger.warn("Неудачная попытка отправки личного сообщения от пользователя " + nickname + ", т.к. пользователя с таким ником " + toUserNick + " нет в чате");
                                        ClientHandler.this.sendMsg("User with nick '" + toUserNick + "' not found in chat room");
                                    }
                                } else {
                                    logger.warn("Недостаточно данных для отправки личного сообщения от пользователя" + nickname);
                                    ClientHandler.this.sendMsg("Wrong private message");
                                }
                            } else if (str.startsWith("/changenick")) {
                                // /changenick Ololo
                                String[] subStr = str.split(" ");
                                if (subStr.length == 2) {
                                    if (SQLHandler.tryToChangeNick(subStr[1], nickname)) {
                                        ClientHandler.this.sendMsg("[" + dateFormat.format(new Date()) + "]" + nickname + " изменён на " + subStr[1]);
                                        ClientHandler.this.sendMsg("Изменения вступят в силу после перезахода");
                                        logger.warn("Пользователь " + nickname + "изменил ник на " + subStr[1] + " нет в чате");
                                        server.broadcastMsg("[" + dateFormat.format(new Date()) + "]" + nickname + " changed has nickname to " + subStr[1]);
                                        break;
                                    } else {
                                        logger.warn("Неудачная попытка изменения ника ользователя " + nickname + "на " + subStr[1]);
                                        ClientHandler.this.sendMsg("Incorrect nickname");
                                    }
                                }
                            }
                        } else {
                            server.broadcastMsg("[" + dateFormat.format(new Date()) + "]" + " " + nickname + ": " + str);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Не возможно прочитать команду из входящего потока", e);
                }
        } catch (IOException e) {
            logger.error("Ошибка исполнения ClientHandler" , e);
        } finally {
            try {
                logger.debug("Попытка закрытия сокета");
                System.out.println("закрытие сокета");
                socket.close();
            } catch (IOException e) {
                logger.error("Закрытие сокета завершилось неудачно", e);
            }
            server.unsubscribe(ClientHandler.this);
        }
        });
        executorService.shutdown();
    }

    public ClientHandler(Server server, Socket socket, String test){}

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            logger.error("Неудачная отправка сообщения" , e);
        }
    }

    public String getNickname() {
        return nickname;
    }
//для тестов
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setOut(DataOutputStream out) {
        this.out = out;
    }
}
