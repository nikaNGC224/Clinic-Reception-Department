package com.poly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;

import static com.poly.MainWindow.*;

public class EditPatientDialog extends JDialog {
    private final JTextField firstName;
    private final JTextField lastName;
    private final JTextField patherName;

    private final JComboBox<String> diagnosis;
    private final JComboBox<String> wards;

    private final int patientId;

    private boolean isEmpty = false;

    public EditPatientDialog(JFrame owner, int id) throws SQLException {
        super(owner, "Редактировать пациента", true);
        patientId = id;

        setSize(500, 200);
        setLocationRelativeTo(owner);

        Vector<String> patientData = new Vector<>(getPatientData(patientId));

        JLabel label1 = new JLabel("Фамилия:");
        JLabel label2 = new JLabel("Имя:");
        JLabel label3 = new JLabel("Отчество:");
        firstName = new JTextField(patientData.get(0));
        lastName = new JTextField(patientData.get(1));
        patherName = new JTextField(patientData.get(2));

        for (JTextField jTextField : Arrays.asList(firstName, lastName, patherName)) {
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

        JLabel label4 = new JLabel("Диагноз:");

        diagnosis = getComboBoxDiagnosis();
        diagnosis.setSelectedItem(patientData.get(3));
        diagnosis.addActionListener(e -> {
            try {
                setComboBoxWards();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        JLabel label5 = new JLabel("Палата:");
        wards = new JComboBox<>();
        setComboBoxWards();

        JPanel panel = new JPanel(new GridLayout(5, 2));

        panel.add(label1);
        panel.add(firstName);
        panel.add(label2);
        panel.add(lastName);
        panel.add(label3);
        panel.add(patherName);

        panel.add(label4);
        panel.add(diagnosis);

        panel.add(label5);
        panel.add(wards);

        JButton button = new JButton("Сохранить изменения");
        button.addActionListener(new EditPatientButtonListener());

        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                firstName.setEditable(false);
                lastName.setEditable(false);
                patherName.setEditable(false);
            }
        });
        getContentPane().add(new JPanel().add(button), BorderLayout.SOUTH);

        setVisible(true);
    }

    private class EditPatientButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String name1 = getTextFieldInfo(firstName);
            String name2 = getTextFieldInfo(lastName);
            String name3 = getTextFieldInfo(patherName);

            String diagnosisName = getComboBoxInfo(diagnosis);
            String wardName = getComboBoxInfo(wards);

            if (isEmpty) {
                showErrorMessage("Заполните все поля!");
                isEmpty = false;
            } else {
                try {
                    updatePatient(patientId, name1, name2, name3, diagnosisName, wardName);
                    dispose();
                    showNotification("Изменения сохранены!");
                    updateJournalPanel();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private Vector<String> getPatientData(int id) throws SQLException {
        String sql = String.format("""
                select first_name,
                last_name,
                pather_name,
                diagnosis.name as diagnosis,
                wards.name as ward
                from people
                left join diagnosis
                on people.diagnosis_id = diagnosis.id
                left join wards
                on people.ward_id = wards.id
                where people.id = %d
                """, id);
        // Получаем одного человека
        // Записываем в массив строк
        ResultSet resultSet = getResultSetFromQuery(sql);
        Vector<String> patient = new Vector<>();
        //id и wards не нужны
        while (resultSet.next()) {
            for (int column = 1; column < 5; column++) {
                patient.add(resultSet.getString(column));
            }
        }
        return patient;
    }

    private void setComboBoxWards() throws SQLException {
        String selectedOption = (String) diagnosis.getSelectedItem();
        if (Objects.equals(selectedOption, "")) {
            return;
        }
        // нужны все палаты этого диагноза (не пустые)
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

    private void updatePatient(int id, String name1, String name2, String name3,
                               String diagnosis, String ward) throws SQLException {
        String sql = String.format("""
                select update_patient(%d, '%s', '%s', '%s', '%s', '%s')
                """, id, name1, name2, name3, diagnosis, ward);
        getResultSetFromQuery(sql);
    }

    private String getTextFieldInfo(JTextField field) {
        String text = field.getText();
        if (!isEmpty && text.isEmpty()) {
            isEmpty = true;
        }
        return text;
    }

    private String getComboBoxInfo(JComboBox<String> box) {
        String selectedOption = (String) box.getSelectedItem();
        if (!isEmpty) {
            assert selectedOption != null;
            if (selectedOption.isEmpty()) {
                isEmpty = true;
            }
        }
        return selectedOption;
    }
}
