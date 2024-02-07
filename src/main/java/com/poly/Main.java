package com.poly;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static final String url = "jdbc:postgresql://localhost:5432/";
    public static final String dbName = "pg_db";
    public static String user;
    public static String pass;

    public static void main(String[] args) {
        String configFile = "config.txt";
        Map<String, String> configMap = parseConfigFile(configFile);
        user = configMap.get("user");
        pass = configMap.get("pass");


        try (Connection conn = DriverManager.getConnection(url + dbName, user, pass)) {
            Class.forName("org.postgresql.Driver");
            if (conn != null) {
                System.out.println("Connection completed");
            } else {
                System.out.println("Connection failed");
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
        SwingUtilities.invokeLater(AuthorizationWindow::new);
    }

    public static Map<String, String> parseConfigFile(String configFile) {
        Map<String, String> configMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] keyValue = line.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    configMap.put(key, value);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return configMap;
    }
}
