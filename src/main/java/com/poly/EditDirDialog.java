package com.poly;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.poly.Main.*;
import static com.poly.Main.pass;
import static com.poly.MainWindow.*;
import static com.poly.MainWindow.updateDirTable;

public class EditDirDialog extends JDialog {
    private final String[] rowData;
    private boolean isEmpty = false;

    public EditDirDialog(JFrame owner, String dirName, String[] rowData) {
        super(owner, true);
        this.rowData = rowData;
        setSize(500, 150);
        setLocationRelativeTo(owner);
        String title = "";

        if (Objects.equals(dirName, "Палаты")) {
            title = "Редактировать палату";
            setWardPane();
        } else if ((Objects.equals(dirName, "Диагнозы"))) {
            title = "Редактировать диагноз";
            setDiagnosisPane();
        }
        setTitle(title);
        setVisible(true);
    }

    private void setWardPane() {
        JLabel label1 = new JLabel("Имя палаты:");
        JTextField name = new JTextField(rowData[1]);
        JLabel label2 = new JLabel("Вместимость:");
        JTextField maxCount = new JTextField(rowData[2]);
        setEditableToTextFieldList(Arrays.asList(name, maxCount));

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
        JTextField name = new JTextField(rowData[1]);
        setEditableToTextFieldList(List.of(name));

        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(label1);
        panel.add(name);

        JButton button = getDiagnosisButton(name);

        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(new JPanel().add(button), BorderLayout.SOUTH);
    }

    private void setEditableToTextFieldList(List<JTextField> list) {
        for (JTextField jTextField : list) {
            jTextField.setEditable(false);
            jTextField.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        jTextField.setEditable(true);
                    }
                }
            });
        }
    }

    private JButton getWardButton(JTextField name, JTextField maxCount) {
        JButton button = new JButton("Сохранить изменения");
        button.addActionListener(e -> {
            String text1 = getTextFieldInfo(name);
            int text2 = Integer.parseInt(getTextFieldInfo(maxCount));
            int id = Integer.parseInt(rowData[0]);

            if (isEmpty) {
                showErrorMessage("Заполните все поля!");
                isEmpty = false;
            } else {
                try {
                    updateWardQuery(id, text1, text2);
                    dispose();
                    showNotification("Изменения сохранены!");
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
        JButton button = new JButton("Сохранить изменения");
        button.addActionListener(e -> {
            String text = getTextFieldInfo(name);
            int id = Integer.parseInt(rowData[0]);

            if (isEmpty) {
                showErrorMessage("Заполните поле!");
                isEmpty = false;
            } else {
                try {
                    updateDiagnosisQuery(id, text);
                    dispose();
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

    private void updateWardQuery(int id, String name, int maxCount) throws SQLException {
        executePreparedQuery(String.format("""
                update wards
                set name = '%s', max_count = '%d'
                where id = '%d'
                """, name, maxCount, id));
    }

    private void updateDiagnosisQuery(int id, String name) {
        try {
            executePreparedQuery(String.format("""
                    update diagnosis
                    set name = '%s'
                    where id = '%d'
                    """, name, id));
            showNotification("Изменения сохранены!");
        } catch (SQLException e) {
            showErrorMessage("Невозможно изменить название диагноза: есть пациенты с таким диагнозом!");
        }
    }

    private void executePreparedQuery(String sql) throws SQLException {
        Connection conn = DriverManager.getConnection(url + dbName, user, pass);
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        preparedStatement.executeUpdate();
    }
}
