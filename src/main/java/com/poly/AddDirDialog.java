package com.poly;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import static com.poly.Main.*;
import static com.poly.Main.pass;
import static com.poly.MainWindow.*;

public class AddDirDialog extends JDialog {
    private boolean isEmpty = false;

    public AddDirDialog(JFrame owner, String dirName) {
        super(owner, true);
        setSize(500, 150);
        setLocationRelativeTo(owner);
        String title = "";

        if (Objects.equals(dirName, "Палаты")) {
            title = "Добавить палату";
            setWardPane();
        } else if ((Objects.equals(dirName, "Диагнозы"))) {
            title = "Добавить диагноз";
            setDiagnosisPane();
        }
        setTitle(title);
        setVisible(true);
    }

    private void setWardPane() {
        JLabel label1 = new JLabel("Имя палаты:");
        JTextField name = new JTextField();
        JLabel label2 = new JLabel("Вместимость:");
        JTextField maxCount = new JTextField();

        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(label1);
        panel.add(name);
        panel.add(label2);
        panel.add(maxCount);

        JButton button = getWardButton(name, maxCount);

        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(new JPanel().add(button), BorderLayout.SOUTH);
    }

    private void setDiagnosisPane() {
        JLabel label1 = new JLabel("Название диагноза:");
        JTextField name = new JTextField();

        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(label1);
        panel.add(name);

        JButton button = getDiagnosisButton(name);

        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(new JPanel().add(button), BorderLayout.SOUTH);
    }

    private JButton getWardButton(JTextField name, JTextField maxCount) {
        JButton button = new JButton("Добавить");
        button.addActionListener(e -> {
            String text1 = getTextFieldInfo(name);
            String text2 = getTextFieldInfo(maxCount);

            if (isEmpty) {
                showErrorMessage("Заполните все поля!");
                isEmpty = false;
            } else {
                try {
                    addWardQuery(text1, text2);
                    dispose();
                    showNotification("Новая палата успешно добавлена!");
                    updateDirTable();
                    updateJournalPanel();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        return button;
    }

    private JButton getDiagnosisButton(JTextField name) {
        JButton button = new JButton("Добавить");
        button.addActionListener(e -> {
            String text = getTextFieldInfo(name);

            if (isEmpty) {
                showErrorMessage("Заполните поле!");
                isEmpty = false;
            } else {
                try {
                    addDiagnosisQuery(text);
                    dispose();
                    showNotification("Новый диагноз успешно добавлен!");
                    updateDirTable();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        return button;
    }

    private String getTextFieldInfo(JTextField field) {
        String text = field.getText();
        if (!isEmpty && text.isEmpty()) {
            isEmpty = true;
        }
        return text;
    }

    private void addWardQuery(String name, String maxCount)
            throws SQLException {
        executePreparedQuery(String.format("""
                insert into wards (name, max_count)
                values ('%s', '%s')
                """, name, maxCount));
    }

    private void addDiagnosisQuery(String name) throws SQLException {
        executePreparedQuery(String.format("""
                insert into diagnosis (name)
                values ('%s')""", name));
    }

    private void executePreparedQuery(String sql) throws SQLException {
        Connection conn = DriverManager.getConnection(url + dbName, user, pass);
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        preparedStatement.executeUpdate();
    }
}
