import java.sql.*;
import java.util.Scanner;

public class AddBookPublisher {
    public static void addBookPublisher(Scanner scanner) {
        try (Connection c = DatabaseConnection.connect()) {
            c.setAutoCommit(false);

            System.out.print("Introduceți titlul cărții: ");
            String title = scanner.nextLine();

            System.out.print("Introduceți anul ediției: ");
            int year = scanner.nextInt();
            scanner.nextLine(); // Consumă newline rămas

            System.out.print("Introduceți numele editurii: ");
            String publisherName = scanner.nextLine();

            int bookId = getBookIdByTitle(c, title);
            if (bookId == -1) {
                System.out.println("Cartea nu există în baza de date!");
                return;
            }

            int publisherId = getOrCreatePublisher(c, publisherName);
            insertBookCopy(c, bookId, year, publisherId);

            System.out.println("Ediția cărții a fost adăugată cu succes!");

        } catch (SQLException e) {
            System.err.println("Eroare SQL: " + e.getMessage());
        }
    }

    private static int getBookIdByTitle(Connection c, String title) throws SQLException {
        String sql = "SELECT id FROM book WHERE title = ?";
        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

    private static int getOrCreatePublisher(Connection c, String publisherName) throws SQLException {
        String checkSql = "SELECT id FROM publisher WHERE name = ?";
        String insertSql = "INSERT INTO publisher (name) VALUES (?)";

        try (PreparedStatement checkStmt = c.prepareStatement(checkSql)) {
            checkStmt.setString(1, publisherName);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        try (PreparedStatement insertStmt = c.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, publisherName);
            insertStmt.executeUpdate();
            ResultSet rs = insertStmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static void insertBookCopy(Connection c, int bookId, int year, int publisherId) throws SQLException {
        String sql = "INSERT INTO book_copy (book_id, year_published, publisher_id) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, publisherId);
            pstmt.executeUpdate();
        }
    }
}
