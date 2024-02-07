package com.poly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.poly.Main.*;

public class AuthorizationWindow extends JFrame {
    private final JTextField usernameField;
    private final JPasswordField passwordField;

    public static String userName;

    public AuthorizationWindow() {
        int width = 500;
        int height = 200;
        setTitle("Приложение Клиника | Вход");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);

        Toolkit toolkit = getToolkit();
        Dimension size = toolkit.getScreenSize();
        setLocation(size.width / 2 - getWidth() / 2, size.height / 2 - getHeight() / 2);

        setLayout(new BorderLayout());

        JLabel usernameLabel = new JLabel("Имя пользователя:");
        usernameField = new JTextField();

        JLabel passwordLabel = new JLabel("Пароль:");
        passwordField = new JPasswordField();

        JButton signOnButton = new JButton("Войти");
        signOnButton.addActionListener(new SignOnButtonListener());

        JButton signInButton = new JButton("Зарегистрироваться");
        signInButton.addActionListener(new SignInButtonListener());

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        panel.add(usernameLabel);
        panel.add(usernameField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(signOnButton);
        panel.add(signInButton);

        add(panel, BorderLayout.CENTER);
        setVisible(true);
    }

    private class SignOnButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String name = usernameField.getText();
            userName = name;
            String pass = new String(passwordField.getPassword());
            Map<String, String> users;
            try {
                users = getUsersQuery();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }

            if (users.containsKey(name)) {
                boolean isPasswordMatch;
                try {
                    String hashed = hashPassword(pass);
                    isPasswordMatch = users.get(name).equals(hashed);
                } catch (NoSuchAlgorithmException ex) {
                    throw new RuntimeException(ex);
                }
                if (isPasswordMatch) {
                    dispose();
                    try {
                        new MainWindow().setVisible(true);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    showErrorMessage("Неверный пароль!");
                }
            } else {
                showErrorMessage("Пользователя с таким именем не существует!");
            }
            usernameField.setText("");
            passwordField.setText("");
        }
    }

    private class SignInButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String name = usernameField.getText();
            String pass = new String(passwordField.getPassword());

            if (Objects.equals(name, "")) {
                showErrorMessage("Поле 'Имя пользователя' пустое!");
            } else if (Objects.equals(pass, "")) {
                showErrorMessage("Поле 'Пароль' пустое!");
            } else {
                Map<String, String> users;
                try {
                    users = getUsersQuery();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }

                if (users.containsKey(name)) {
                    showErrorMessage("Такой пользователь уже есть");
                } else {
                    String hashed;
                    try {
                        hashed = hashPassword(pass);
                        addUserQuery(name, hashed);
                    } catch (NoSuchAlgorithmException | SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                    showNotification();
                }
            }
            usernameField.setText("");
            passwordField.setText("");
        }
    }

    private void showNotification() {
        JOptionPane.showMessageDialog(new JFrame(), "Вы успешно зарегистрировались!", "Уведомление", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "Предупреждение", JOptionPane.ERROR_MESSAGE);
    }

    private Map<String, String> getUsersQuery() throws SQLException {
        Connection conn = DriverManager.getConnection(url + dbName, user, pass);
        PreparedStatement preparedStatement = conn.prepareStatement(
                "select * from users"
        );
        ResultSet rs = preparedStatement.executeQuery();
        Map<String, String> users = new HashMap<>();
        while (rs.next()) {
            String name = rs.getString("username");
            String pass = rs.getString("password");
            users.put(name, pass);
        }
        return users;
    }

    private void addUserQuery(String username, String password) throws SQLException {
        Connection conn = DriverManager.getConnection(url + dbName, user, pass);
        PreparedStatement preparedStatement = conn.prepareStatement(
                "insert into users (username, password) values (?, ?)"
        );
        preparedStatement.setString(1, username);
        preparedStatement.setString(2, password);
        preparedStatement.executeUpdate();
    }

    private String hashPassword(String pass) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(pass.getBytes());
        byte[] digest = md.digest();
        BigInteger bigInteger = new BigInteger(1, digest);
        return bigInteger.toString(16);
    }
}
