package com.poly;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Objects;
import java.util.Vector;

import static com.poly.Main.*;
import static com.poly.Main.pass;
import static com.poly.MainWindow.*;

public class ChoosePatientDialog extends JDialog {
    private final JComboBox<String> diagnosis;
    private final JComboBox<String> wards;
    private final JComboBox<String> patients;

    public ChoosePatientDialog(JFrame owner, boolean deleteMode) throws SQLException {
        super(owner, "Выбор пациента", true);
        setSize(500, 150);
        setLocationRelativeTo(owner);

        JLabel label1 = new JLabel("Диагноз:");
        diagnosis = getComboBoxDiagnosis();
        diagnosis.addActionListener(e -> {
            try {
                setComboBoxWards();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        JLabel label2 = new JLabel("Палата:");
        wards = new JComboBox<>();
        wards.addActionListener(e -> {
            try {
                setComboBoxPatients();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        JLabel label3 = new JLabel("Пациент:");
        patients = new JComboBox<>();

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(label1);
        panel.add(diagnosis);
        panel.add(label2);
        panel.add(wards);
        panel.add(label3);
        panel.add(patients);
        getContentPane().add(panel, BorderLayout.NORTH);

        JButton button = getButton(owner, deleteMode);
        panel.add(button);
        getContentPane().add(new JPanel().add(button), BorderLayout.SOUTH);

        setVisible(true);
    }

    private JButton getButton(JFrame owner, boolean deleteMode) {
        JButton button = new JButton();
        if (deleteMode) {
            button.setText("Удалить");
            button.addActionListener(e -> {
                try {
                    deletePatientQuery();
                    dispose();
                    showNotification("Пациент успешно удален!");
                    updateJournalPanel();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } else {
            button.setText("Выбрать");
            button.addActionListener(e -> {
                try {
                    int id = getPatientId();
                    dispose();
                    new EditPatientDialog(owner, id);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
        return button;
    }

    private void setComboBoxWards() throws SQLException {
        String selectedOption = (String) diagnosis.getSelectedItem();
        if (Objects.equals(selectedOption, "")) {
            return;
        }

        String sql = String.format("""
                select Палаты from wards_view
                where Диагноз = '%s'""", selectedOption);

        ResultSet resultSet = getResultSetFromQuery(sql);
        Vector<String> data = new Vector<>();
        while (resultSet.next()) {
            data.add(resultSet.getString("Палаты"));
        }

        wards.removeAllItems();
        for (String item : data) {
            wards.addItem(item);
        }
    }

    private void setComboBoxPatients() throws SQLException {
        String selectedWard = (String) wards.getSelectedItem();
        String sql = String.format("""
                select people.first_name as Фамилия,
                people.last_name as Имя,
                people.pather_name as Отчество
                from people
                inner join wards
                on people.ward_id = (
                select id from wards
                where name = '%s')
                group by people.id
                """, selectedWard);

        ResultSet resultSet = getResultSetFromQuery(sql);
        Vector<String> data = new Vector<>();
        while (resultSet.next()) {
            StringBuilder sb = new StringBuilder();
            sb.append(resultSet.getString(1));
            for (int i = 2; i <= 3; i++) {
                sb.append(" ").append(resultSet.getString(i));
            }
            data.add(String.valueOf(sb));
        }
        patients.removeAllItems();
        for (String item : data) {
            patients.addItem(item);
        }
    }

    private int getPatientId() throws SQLException {
        String selectedPatient = (String) patients.getSelectedItem();
        if (selectedPatient == null) {
            throw new SQLException(new Exception("Невозможно выбрать пациента!"));
        }
        String[] patientNames = selectedPatient.split(" ");
        String sql = String.format("""
                select id from people
                where first_name = '%s'
                and last_name = '%s'
                and pather_name = '%s';
                  """, patientNames[0], patientNames[1], patientNames[2]);

        ResultSet resultSet = getResultSetFromQuery(sql);
        int id = 0;
        while (resultSet.next()) {
            id = resultSet.getInt("id");
        }
        if (id == 0) {
            throw new SQLException(new Exception("Код пациента не получен!"));
        }
        return id;
    }

    private void deletePatientQuery() throws SQLException {
        int id = getPatientId();
        String sql = String.format("""
                delete from people
                where id = %d
                """, id);

        Connection conn = DriverManager.getConnection(url + dbName, user, pass);
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        preparedStatement.executeUpdate();
    }
}
