package ru.geekbrains.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    TextArea textArea;

    @FXML
    TextField textField, loginField;

    @FXML
    VBox mainBox;

    @FXML
    HBox authPanel, msgPanel;

    @FXML
    PasswordField passField;

    @FXML
    Button sendMsgBtn;

    @FXML
    ListView<String> clientsList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private ObservableList<String> clients;
    private boolean authorized;
    private ReadWriteLinesToFile hystoryService;
    private static Logger logger = LogManager.getLogger();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        authorized = false;
        Platform.runLater(() -> ((Stage) mainBox.getScene().getWindow()).setOnCloseRequest(t -> {
            sendMsg("/end");
            Platform.exit();
        }));

        textField.textProperty().addListener((observableValue, s, t1) -> sendMsgBtn.setDisable(t1.isEmpty()));

        clients = FXCollections.observableArrayList();
        clientsList.setItems(clients);
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
//        authPanel.setVisible(!authorized);
//        authPanel.setManaged(!authorized);
//        msgPanel.setVisible(authorized);
//        msgPanel.setManaged(authorized);
//        clientsList.setVisible(authorized);
//        clientsList.setManaged(authorized);

        if (authorized) {
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
            clientsList.setVisible(true);
            clientsList.setManaged(true);
        } else {
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            clientsList.setVisible(false);
            clientsList.setManaged(false);
            nickname = "";
        }

        Platform.runLater(() -> {
            if (nickname.isEmpty()) {
                ((Stage) mainBox.getScene().getWindow()).setTitle("Java Chat Client");
            } else {
                ((Stage) mainBox.getScene().getWindow()).setTitle("Java Chat Client: " + nickname);
            }
        });

    }

    public void sendMsg() {
        try {
            if (socket != null && !socket.isClosed()) {
                String str = textField.getText();
                out.writeUTF(str);
                textField.clear();
                textField.requestFocus();
            }
        } catch (IOException e) {
            logger.error("Не удалось отправить команду на сервер" + e);
        }
    }

    public void sendMsg(String msg) {
        try {
            if (socket != null && !socket.isClosed()) {
                if (!msg.isEmpty()) {
                    out.writeUTF(msg);
                }
            }
        } catch (IOException e) {
            logger.error("Не удалось отправить команду на сервер" + e);
        }
    }

    public void sendAuth() {
        connect();
        // /auth login1 password1
        sendMsg("/auth " + loginField.getText() + " " + passField.getText());
        loginField.clear();
        passField.clear();
    }

    private void connect() {
            if (socket == null || socket.isClosed()) {
                new Thread(() -> {
                    try (Socket socket = new Socket("localhost", 8189);
                         DataInputStream in = new DataInputStream(socket.getInputStream());
                         DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                        this.socket = socket;
                        this.in = in;
                        this.out = out;
                        try {
                            while (true) {
                                String str = in.readUTF();
                                // /authok nick
                                if (str.startsWith("/authok")) {
                                    nickname = str.split(" ")[1];
                                    setAuthorized(true);
                                    String filename = System.getProperty("user.dir") + "\\history_" + nickname + ".txt";
                                    File file = new File(filename);
                                    if (!file.exists()) {
                                        file.createNewFile();
                                    }
                                    hystoryService = HystoryService.getInstance(file);
                                    List<String> chat = hystoryService.getLastLines(file, 100);
                                    textArea.appendText(chat.toString());
                                    break;
                                } if (str.startsWith("Wrong")) {
                                    setAlertmsg(str);
                                }
                            }
                            while (true) {
                                String str = in.readUTF();
                                if (!str.startsWith("/")) {
                                    textArea.appendText(str + System.lineSeparator());
                                    String filename = System.getProperty("user.dir") + "\\history_" + nickname + ".txt";
                                    File file = new File(filename);
                                    if (!file.exists()) {
                                        file.createNewFile();
                                    }
                                    hystoryService = HystoryService.getInstance(file);
                                    hystoryService.writeLineToFile(str + System.lineSeparator());
                                } else if (str.startsWith("/clientslist")) {
                                    // /clientslist nick1 nick2 nick3
                                    String[] subStr = str.split(" ");
                                    clients.clear();
                                    for (int i = 1; i < subStr.length; i++) {
                                        clients.add(subStr[i]);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            logger.error("Ошибка считывания команды сервера" + e);
                        } finally {
                            setAuthorized(false);
                        }

                    } catch (IOException e) {
                        logger.error("Ошибка коннекта" + e);
                    }
                }).start();
            }
    }

    public void registerBtn() {
        try {
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/registration.fxml"));
            Parent root = fxmlLoader.load();
            stage.setTitle("Registration");
            stage.setScene(new Scene(root, 400, 240));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Ошибка кнопки регистрации" + e);
        }

    }

    public void log() throws IOException {
        File log = new File("src/main/java/ru/geekbrains/client/Log/history_" + nickname + ".txt");
        System.out.println(nickname);
        System.out.println(log.exists());
//        if (!log.exists()){
//            log.createNewFile();
//        }
    }

    public void setAlertmsg(String alertmsg) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.ERROR, alertmsg, ButtonType.OK);
            alert.showAndWait();
        });
    }
}

