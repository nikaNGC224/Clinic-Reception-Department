package com.poly;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static com.poly.Main.*;
import static com.poly.Main.pass;
import static com.poly.MainWindow.*;
import static com.poly.MainWindow.showErrorMessage;

public class DeleteDirDialog {
    public DeleteDirDialog(JFrame owner, String dirName, int id) throws SQLException {
        String title = "";
        String tableName = "";

        switch (dirName) {
            case "������" -> {
                title = "������� ������";
                tableName = "wards";
            }
            case "��������" -> {
                title = "������� �������";
                tableName = "diagnosis";
            }
        }
        int choice = JOptionPane.showOptionDialog(owner, "�� �������?", title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                executePreparedQuery(String.format("""
                        delete from %s
                        where id = %d
                        """, tableName, id));
                updateDirTable();
                if (dirName.equals("������")) {
                    showNotification("������ ������� �������. ���������� �������� ���������� � ��������� ������ " +
                            "�������������!");
                    updateJournalPanel();
                }
            } catch (SQLException e) {
                switch (dirName) {
                    case "��������" ->
                            showErrorMessage("���������� ������� �������: ���� �������� � ������ ���������!");
                    case "������" ->
                            showErrorMessage("���������� ������� ������: ��� ���������� � ���� ������ ��������� " +
                                    "��� ��������� ���� � ������ ������� � ���������� ���������!");
                }
            }
        }
    }

    private void executePreparedQuery(String sql) throws SQLException {
        Connection conn = DriverManager.getConnection(url + dbName, user, pass);
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        preparedStatement.executeUpdate();
    }
}
