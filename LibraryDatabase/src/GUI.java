import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class GUI extends JFrame {
    private JComboBox<String> tableSelector;
    private JTable dataTable;

    public GUI() {
        setTitle("Library");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setupUI();
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        tableSelector = new JComboBox<>();
        JButton loadButton = new JButton("Load Table");
        topPanel.add(new JLabel("Select Table:"));
        topPanel.add(tableSelector);
        topPanel.add(loadButton);

        dataTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(dataTable);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        loadTableNames();

        loadButton.addActionListener(_ -> loadTableData());  // Evită avertizarea
        tableSelector.addActionListener(_ -> loadTableData()); // Evită avertizarea

        add(mainPanel);
    }

    private void loadTableNames() {
        try (Connection c = DatabaseConnection.connect()) {
            DatabaseMetaData meta = c.getMetaData();
            ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});

            while (tables.next()) {
                tableSelector.addItem(tables.getString("TABLE_NAME"));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading tables: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTableData() {
        String tableName = (String) tableSelector.getSelectedItem();
        if (tableName == null) return;

        try (Connection c = DatabaseConnection.connect()) {

            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);

            dataTable.setModel(buildTableModel(rs));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();

        Vector<String> columnNames = new Vector<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i));
        }

        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> row = new Vector<>();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            data.add(row);
        }

        return new DefaultTableModel(data, columnNames);
    }
}
