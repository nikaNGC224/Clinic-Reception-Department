package com.poly;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

import static com.poly.MainWindow.buildTableModel;
import static com.poly.MainWindow.getResultSetFromQuery;

public class ViewWardDialog extends JDialog {
    public ViewWardDialog(JFrame owner, JTable table) {
        super(owner, "Просмотр палаты");
        setSize(700, 600);
        setLocationRelativeTo(owner);

        int row = table.getSelectedRow();
        String strWard = "Палата: " + table.getValueAt(row, 0);
        String strDiagnosis = " Диагноз: " + table.getValueAt(row, 3);
        JLabel label1 = new JLabel(strWard);
        JLabel label2 = new JLabel(strDiagnosis);
        JPanel panel = new JPanel();
        panel.add(label1);
        panel.add(label2);
        getContentPane().add(panel, BorderLayout.NORTH);

        String sql = String.format("""
                select people.id as Код,
                people.first_name as Фамилия,
                people.last_name as Имя,
                people.pather_name as Отчество
                from people
                inner join wards
                on people.ward_id = (
                select id from wards
                where name = '%s')
                group by people.id;
                """, table.getValueAt(row, 0));
        try {
            JTable peopleTable = new JTable(buildTableModel(getResultSetFromQuery(sql)));
            JScrollPane scrollPane = new JScrollPane(peopleTable);
            getContentPane().add(new JPanel().add(scrollPane), BorderLayout.CENTER);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        setVisible(true);
    }
}
