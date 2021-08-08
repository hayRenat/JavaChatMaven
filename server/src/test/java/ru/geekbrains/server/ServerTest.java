package ru.geekbrains.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTest {
    private static Server serTest;
    private ClientHandler clientHandler;
    private Socket socket;
    private DataInputStream inClient;
    private DataOutputStream outClient;
    private String actual;
    private String msg = "Hello";


    @BeforeAll
    void setUp() {
        Runnable task = () -> {
            try {
                Thread.sleep(1000);
                socket = new Socket("localhost", 8189);
                inClient = new DataInputStream(socket.getInputStream());
                outClient = new DataOutputStream(socket.getOutputStream());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread client = new Thread(task);
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:database.db");
            Statement statement = connection.createStatement();
            client.start();
            serTest = new Server("test");
            clientHandler = new ClientHandler(serTest, serTest.getSocket(), "test");
            clientHandler.setNickname("Renat");
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            clientHandler.setOut(out);
            serTest.subscribe(clientHandler);
        } catch (SQLException | ClassNotFoundException | IOException throwables) {
            throwables.printStackTrace();
        }
    }

    @Test
    void subscribe() {
        Assertions.assertTrue(serTest.getClients().size()>0);
    }

    @Test
    void unsubscribe() {
        serTest.unsubscribe(clientHandler);
        Assertions.assertTrue(serTest.getClients().size()==0);
    }

    @Test
    void isNickInChat() {
        Assertions.assertTrue((serTest.isNickInChat("Renat")));
    }

}