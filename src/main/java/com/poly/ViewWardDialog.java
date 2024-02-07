package com.poly;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

import static com.poly.MainWindow.buildTableModel;
import static com.poly.MainWindow.getResultSetFromQuery;

public class ViewWardDialog extends JDialog {
    public ViewWardDialog(JFrame owner, JTable table) {
        super(owner, "�������� ������");
        setSize(700, 600);
        setLocationRelativeTo(owner);

        int row = table.getSelectedRow();
        String strWard = "������: " + table.getValueAt(row, 0);
        String strDiagnosis = " �������: " + table.getValueAt(row, 3);
        JLabel label1 = new JLabel(strWard);
        JLabel label2 = new JLabel(strDiagnosis);
        JPanel panel = new JPanel();
        panel.add(label1);
        panel.add(label2);
        getContentPane().add(panel, BorderLayout.NORTH);

        String sql = String.format("""
                select people.id as ���,
                people.first_name as �������,
                people.last_name as ���,
                people.pather_name as ��������
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
