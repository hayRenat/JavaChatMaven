package ru.geekbrains.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;


public class SQLHandler {
    private static Connection connection;
    private static Statement statement;
    private static Logger logger = LogManager.getLogger();

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:server/database.db");
            statement = connection.createStatement();
            logger.info("Успешное подключение к БД");
        } catch (Exception e) {
            logger.error("Не удалось подключиться к базе данных " + connection.toString());
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            logger.info("Отключение от БД");
            connection.close();
        } catch (SQLException e) {
            logger.error("Не удалось отключитсья от БД");
            e.printStackTrace();
        }
    }

    public static String getNickByLoginAndPassword(String login, String password) {
        try {
            ResultSet rs = statement.executeQuery("SELECT nickname FROM users WHERE login ='" + login + "' AND password = '" + password + "'");
            logger.debug("Проверка комбинации login = " + login + " password = " + password);
            if (rs.next()) {
                logger.debug("Пользователь найден");
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        logger.debug("Пользователь не найден");
        return null;
    }

    public static boolean tryToRegister(String login, String password, String nickname) {
        try {
            logger.info("Попытка регистрации пользователя с ником " + nickname + " и с логином " + login);
            statement.executeUpdate("INSERT INTO users (login, password, nickname) VALUES ('" + login + "','" + password + "','" + nickname + "')");
            logger.info("Пользователь " + login + " успешно зарегестрирован");
            return true;
        } catch (SQLException e) {
            logger.warn("Неудачная регистрация пользователя login = " + login, " nickname = " + nickname);
            return false;
        }
    }

    public static boolean tryToChangeNick(String newNick, String oldNick) {
        try {
            logger.info("Попытка пользователя с ником " + oldNick + " изменить ник на " + newNick);
            ResultSet rs = statement.executeQuery("SELECT nickname FROM users WHERE nickname = '" + oldNick + "'");
                if (rs.next()) {
                    logger.info("Пользователь " + oldNick + " изменил ник на " + newNick);
            statement.executeUpdate("UPDATE users SET nickname = '" + newNick + "' WHERE nickname = '" + oldNick + "'");
            return true;
                }
        } catch (SQLException e) {
            logger.error("Неудачная попытка пользователя с ником" + oldNick + " изменить ник " + newNick);
            return false;
        }
        logger.warn("Неудачная попытка пользователя с ником" + oldNick + " изменить ник " + newNick);
        return false;
    }
}
