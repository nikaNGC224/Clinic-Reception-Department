package com.poly;

import com.itextpdf.text.DocumentException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;

import static com.poly.Main.*;

public class MainWindow extends JFrame {
    private JPanel journalButtPanel;
    private static JPanel journalPanel;

    private JPanel dirChoosingPanel;
    private JPanel dirButtPanel;
    private static String dirName = "";
    private static JTable dirTable;
    private int dirSelectedRow = -1;

    private JPanel reportPanel;

    public MainWindow() throws SQLException {
        int width = 800;
        int height = 700;
        setTitle("Приложение Клиника | Клиент");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);

        Toolkit toolkit = getToolkit();
        Dimension size = toolkit.getScreenSize();
        setLocation(size.width / 2 - getWidth() / 2, size.height / 2 - getHeight() / 2);

        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Меню");
        menuBar.add(menu);

        JMenu journal = new JMenu("Журнал");
        createJournalPanels();
        journal.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                getContentPane().setVisible(false);
                getContentPane().removeAll();
                getContentPane().add(journalButtPanel, BorderLayout.NORTH);
                getContentPane().add(journalPanel, BorderLayout.CENTER);
                getContentPane().setVisible(true);
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        });
        menuBar.add(journal);

        JMenu directories = new JMenu("Справочники");
        createDirPanels();
        directories.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                getContentPane().setVisible(false);
                getContentPane().removeAll();
                getContentPane().add(new JPanel().add(new JLabel("Справочники:")), BorderLayout.NORTH);
                getContentPane().add(dirChoosingPanel, BorderLayout.WEST);
                getContentPane().setVisible(true);
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        });
        menuBar.add(directories);

        JMenu reports = new JMenu("Отчёты");
        createReportPanel();
        reports.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                getContentPane().setVisible(false);
                getContentPane().removeAll();
                getContentPane().add(new JPanel().add(new JLabel("Отчёты:")), BorderLayout.NORTH);
                getContentPane().add(reportPanel, BorderLayout.WEST);
                getContentPane().setVisible(true);
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        });
        menuBar.add(reports);

        setJMenuBar(menuBar);
    }

    private void createJournalPanels() throws SQLException {
        JButton addButton = new JButton("Добавить пациента");

        addButton.addActionListener(e -> {
            try {
                new AddPatientDialog(MainWindow.this);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        JButton editButton = new JButton("Редактировать пациента");
        editButton.addActionListener(e -> {
            try {
                new ChoosePatientDialog(MainWindow.this, false);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        JButton deleteButton = new JButton("Удалить пациента");
        deleteButton.addActionListener(e -> {
            try {
                new ChoosePatientDialog(MainWindow.this, true);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        journalButtPanel = new JPanel();
        journalButtPanel.add(addButton);
        journalButtPanel.add(editButton);
        journalButtPanel.add(deleteButton);

        String sql = "select * from wards_view";
        JTable wardTable = new JTable(buildTableModel(getResultSetFromQuery(sql)));
        wardTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    new ViewWardDialog(MainWindow.this, wardTable);
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(wardTable);

        journalPanel = new JPanel();
        journalPanel.add(scrollPane);
    }

    private void createDirPanels() {
        dirChoosingPanel = new JPanel();
        dirChoosingPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        Box box = Box.createVerticalBox();
        JLabel label1 = new JLabel("Палаты");
        JLabel label2 = new JLabel("Диагнозы");

        dirButtPanel = new JPanel();
        JButton backButton = new JButton("Назад");
        backButton.addActionListener(e -> {
            dirName = "";
            getContentPane().setVisible(false);
            getContentPane().removeAll();
            getContentPane().add(new JPanel().add(new JLabel("Справочники:")), BorderLayout.NORTH);
            getContentPane().add(dirChoosingPanel, BorderLayout.WEST);
            getContentPane().setVisible(true);
        });
        dirButtPanel.add(backButton);

        dirTable = new JTable();


        JButton addButton = new JButton("Добавить");
        addButton.addActionListener(e -> new AddDirDialog(MainWindow.this, dirName));
        JButton editButton = getDirEditButton();
        JButton deleteButton = new JButton("Удалить");
        deleteButton.addActionListener(e -> {
            if (dirSelectedRow != -1) {
                try {
                    new DeleteDirDialog(MainWindow.this, dirName, Integer.parseInt(getDirRowData()[0]));
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        for (JButton jButton : Arrays.asList(addButton, editButton, deleteButton)) {
            dirButtPanel.add(jButton);
        }

        if (!Objects.equals(AuthorizationWindow.userName, "admin")) {
            addButton.setVisible(false);
            editButton.setVisible(false);
            deleteButton.setVisible(false);
        }


        for (JLabel jLabel : Arrays.asList(label1, label2)) {
            jLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    jLabel.setForeground(Color.GRAY);
                    if (jLabel == label1) {
                        label2.setForeground(Color.BLACK);
                    } else {
                        label1.setForeground(Color.BLACK);
                    }

                    if (e.getClickCount() == 2) {
                        try {
                            jLabel.setForeground(Color.BLACK);
                            updateDirContentPane(e);
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            });
        }

        label1.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        label2.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        box.add(label1);
        box.add(label2);

        dirChoosingPanel.add(box);
    }

    private String[] getDirRowData() {
        String[] rowData = new String[dirTable.getColumnCount()];
        for (int i = 0; i < dirTable.getColumnCount(); i++) {
            rowData[i] = dirTable.getValueAt(dirSelectedRow, i).toString();
        }
        return rowData;
    }

    private JButton getDirEditButton() {
        JButton editButton = new JButton("Редактировать");
        editButton.addActionListener(e -> {
            if (dirSelectedRow != -1) {
                new EditDirDialog(MainWindow.this, dirName, getDirRowData());
            }

        });
        return editButton;
    }

    private void createReportPanel() {
        reportPanel = new JPanel();
        reportPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        Box box = Box.createVerticalBox();
        JLabel label1 = new JLabel("Отчет по диагнозу ОРВИ");
        JLabel label2 = new JLabel("Отчет по заполненности палат");

        for (JLabel jLabel : Arrays.asList(label1, label2)) {
            jLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    jLabel.setForeground(Color.GRAY);
                    if (jLabel == label1) {
                        label2.setForeground(Color.BLACK);
                    } else {
                        label1.setForeground(Color.BLACK);
                    }
                    if (e.getClickCount() == 2) {
                        jLabel.setForeground(Color.BLACK);
                        String fileName = JOptionPane.showInputDialog(
                                MainWindow.this, "Введите имя отчета:");
                        String title = jLabel.getText();
                        String sql = switch (title) {
                            case "Отчет по диагнозу ОРВИ" -> String.format("""
                                    select row_number() over() as №,
                                        first_name as Фамилия,
                                        last_name as Имя,
                                        pather_name as Отчество,
                                        (
                                            select name from wards
                                            where id = people.ward_id
                                        ) as Палата
                                    from people
                                    join diagnosis
                                    on people.diagnosis_id = (
                                        select id from diagnosis
                                        where name = '%s'
                                    )
                                    group by people.id;
                                    """, "ОРВИ");
                            case "Отчет по заполненности палат" -> """
                                    select row_number() over() as №,
                                    	name as Название,
                                    	(
                                    		select concat(count(people.id) * 100 / wards.max_count, '%') from people
                                    		where people.ward_id = wards.id
                                    	) as Заполненность
                                    from wards
                                    """;
                            default -> "";
                        };

                        try {
                            new ReportGenerator(buildTableModel(getResultSetFromQuery(sql)), fileName, title);
                        } catch (SQLException | DocumentException | IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            });
        }

        label1.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        label2.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        box.add(label1);
        box.add(label2);

        reportPanel.add(box);
    }

    public static void updateJournalPanel() throws SQLException {
        String sql = "select * from wards_view";
        JScrollPane s = (JScrollPane) journalPanel.getComponent(0);
        JViewport v = (JViewport) s.getComponent(0);
        JTable table = (JTable) v.getComponent(0);
        table.setModel(buildTableModel(getResultSetFromQuery(sql)));
    }

    private void updateDirContentPane(MouseEvent e) throws SQLException {
        getContentPane().setVisible(false);
        getContentPane().removeAll();
        dirSelectedRow = -1;
        if (Objects.equals(dirName, "")) {
            dirName = ((JLabel) e.getComponent()).getText();
        }

        String sql = switch (dirName) {
            case "Палаты" -> """
                    select id as Код,
                    name as Название,
                    max_count as Вместимость
                    from wards
                    order by Код""";
            case "Диагнозы" -> """
                    select id as Код,
                    name as Название
                    from diagnosis
                    order by Код""";
            default -> "";
        };

        dirTable = new JTable(buildTableModel(getResultSetFromQuery(sql)));
        JTable finalDirTable = dirTable;
        dirTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dirSelectedRow = finalDirTable.getSelectedRow();
            }
        });
        JScrollPane scrollPane = new JScrollPane(dirTable);
        JPanel dirTablePanel = new JPanel();
        dirTablePanel.add(scrollPane);

        getContentPane().add(dirButtPanel, BorderLayout.NORTH);
        getContentPane().add(dirTablePanel, BorderLayout.CENTER);
        getContentPane().setVisible(true);
    }

    public static void updateDirTable() throws SQLException {
        String sql = switch (dirName) {
            case "Палаты" -> """
                    select id as Код,
                    name as Название,
                    max_count as Вместимость
                    from wards
                    order by Код""";
            case "Диагнозы" -> """
                    select id as Код,
                    name as Название
                    from diagnosis
                    order by Код""";
            default -> "";
        };
        dirTable.setModel(buildTableModel(getResultSetFromQuery(sql)));
    }

    static ResultSet getResultSetFromQuery(String sql) throws SQLException {
        Connection conn = DriverManager.getConnection(url + dbName, user, pass);
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        return preparedStatement.executeQuery();
    }

    static DefaultTableModel buildTableModel(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        Vector<String> columnNames = new Vector<>(columnCount);
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }

        Vector<Vector<String>> data = new Vector<>();
        while (resultSet.next()) {
            Vector<String> row = new Vector<>();
            for (int column = 1; column <= columnCount; column++) {
                row.add(resultSet.getString(column));
            }
            data.add(row);
        }

        return new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JComboBox<String> getComboBoxDiagnosis() throws SQLException {
        String sql = "select name from diagnosis";
        ResultSet resultSet = getResultSetFromQuery(sql);

        Vector<String> data = new Vector<>();
        while (resultSet.next()) {
            data.add(resultSet.getString("name"));
        }
        JComboBox<String> diagnosis = new JComboBox<>();
        if (data.isEmpty()) {
            showErrorMessage("Нет диагнозов! Добавьте новый диагноз");
        } else {
            diagnosis.addItem("");
            for (String item : data) {
                diagnosis.addItem(item);
            }
        }
        return diagnosis;
    }

    static void showNotification(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "Уведомление", JOptionPane.INFORMATION_MESSAGE);
    }

    static void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "Предупреждение", JOptionPane.ERROR_MESSAGE);
    }

}
