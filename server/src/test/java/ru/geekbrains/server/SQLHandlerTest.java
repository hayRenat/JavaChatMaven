package ru.geekbrains.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

class SQLHandlerTest {
    private Connection connection = DriverManager.getConnection("jdbc:sqlite:database.db");
    private Statement statement= connection.createStatement();

    SQLHandlerTest() throws SQLException {
    }

    @Test
    void nickIsTrue() {
        String name = "Renat";
        SQLHandler.setStatement(statement);
        Assertions.assertEquals(name,
                SQLHandler.getNickByLoginAndPassword("ren", "123"));
    }
    @Test
    void nickIsFalse() {
        SQLHandler.setStatement(statement);
        Assertions.assertNull(SQLHandler.getNickByLoginAndPassword("ren1123312", "123"));
    }

}