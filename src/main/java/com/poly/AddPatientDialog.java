package com.poly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Objects;
import java.util.Vector;

import static com.poly.MainWindow.*;

public class AddPatientDialog extends JDialog {
    private final JTextField firstName;
    private final JTextField lastName;
    private final JTextField patherName;

    private final JComboBox<String> diagnosis;
    private final JComboBox<String> wards;

    private boolean isEmpty = false;

    public AddPatientDialog(JFrame owner) throws SQLException {
        super(owner, "Добавить пациента", true);
        setSize(500, 200);
        setLocationRelativeTo(owner);

        JLabel label1 = new JLabel("Фамилия:");
        firstName = new JTextField();
        JLabel label2 = new JLabel("Имя:");
        lastName = new JTextField();
        JLabel label3 = new JLabel("Отчество:");
        patherName = new JTextField();

        JLabel label4 = new JLabel("Диагноз:");

        diagnosis = getComboBoxDiagnosis();
        diagnosis.addActionListener(e -> {
            try {
                setComboBoxWards();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        JLabel label5 = new JLabel("Палата:");
        wards = new JComboBox<>();

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

        JButton button = new JButton("Добавить");
        button.addActionListener(new AddButtonListener());

        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(new JPanel().add(button), BorderLayout.SOUTH);

        setVisible(true);
    }

    private class AddButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String name1 = getTextFieldInfo(firstName);
            String name2 = getTextFieldInfo(lastName);
            String name3 = getTextFieldInfo(patherName);

            String option1 = getComboBoxInfo(diagnosis);
            String option2 = getComboBoxInfo(wards);

            if (isEmpty) {
                showErrorMessage("Заполните все поля!");
                isEmpty = false;
            } else {
                try {
                    addPatientQuery(name1, name2, name3, option1, option2);
                    dispose();
                    showNotification("Новый пациент успешно добавлен!");
                    updateJournalPanel();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }

            }
        }
    }

    private void setComboBoxWards() throws SQLException {
        String selectedOption = (String) diagnosis.getSelectedItem();
        if (Objects.equals(selectedOption, "")) {
            return;
        }
        String sql = String.format("""
                select Палаты as name from wards_view
                where wards_view.Занято < wards_view.Вместимость
                and Диагноз = '%s' or Диагноз = ''""", selectedOption);

        ResultSet resultSet = getResultSetFromQuery(sql);
        Vector<String> data1 = new Vector<>();
        while (resultSet.next()) {
            data1.add(resultSet.getString("name"));
        }

        if (data1.isEmpty()) {
            showErrorMessage("Нет свободных палат!");
        }
        wards.removeAllItems();
        for (String item : data1) {
            wards.addItem(item);
        }
    }

    private void addPatientQuery(String name1, String name2, String name3, String option1, String option2)
            throws SQLException {
        String sql = String.format("""
                select insert_patient('%s', '%s', '%s', '%s', '%s')
                """, name1, name2, name3, option1, option2);
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
