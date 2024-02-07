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
            case "Палаты" -> {
                title = "Удалить палату";
                tableName = "wards";
            }
            case "Диагнозы" -> {
                title = "Удалить диагноз";
                tableName = "diagnosis";
            }
        }
        int choice = JOptionPane.showOptionDialog(owner, "Вы уверены?", title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                executePreparedQuery(String.format("""
                        delete from %s
                        where id = %d
                        """, tableName, id));
                updateDirTable();
                if (dirName.equals("Палаты")) {
                    showNotification("Палата успешно удалена. Оставшиеся пациенты перемещены в свободные палаты " +
                            "автоматически!");
                    updateJournalPanel();
                }
            } catch (SQLException e) {
                switch (dirName) {
                    case "Диагнозы" ->
                            showErrorMessage("Невозможно удалить диагноз: есть пациенты с данным диагнозом!");
                    case "Палаты" ->
                            showErrorMessage("Невозможно удалить палату: для оставшихся в этой палате пациентов " +
                                    "нет свободных мест в других палатах с подходящим диагнозом!");
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
