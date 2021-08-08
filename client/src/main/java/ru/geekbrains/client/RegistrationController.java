package ru.geekbrains.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class RegistrationController {
    private Socket socket;
    private static Logger logger = LogManager.getLogger();

    @FXML
    TextField login, password, nickname;

    @FXML
    Label result;

    public void tryToRegister() {
        if (socket == null || socket.isClosed()) {
            try (Socket socket = new Socket("localhost", 8189);
                 DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                this.socket = socket;
                // /registration login pass nick
                out.writeUTF("/registration " + login.getText() + " " + password.getText() + " " + nickname.getText());
                String answer = in.readUTF();
                result.setText(answer);
            } catch (IOException e) {
                logger.error("Ошибка коннекта" + e);
            }
        }
    }
}
