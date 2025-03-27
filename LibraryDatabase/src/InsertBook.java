import java.sql.*;
import java.util.Scanner;

public class InsertBook {
    public static void insertBook(Scanner scanner) {
        try(Connection c = DatabaseConnection.connect()){
            c.setAutoCommit(false);

            System.out.print("Introduceti titlul cartii: ");
            String title = scanner.nextLine();

            System.out.print("Introduceti categoria: ");
            String category = scanner.nextLine();

            System.out.print("Introduceti autorul: ");
            String author = scanner.nextLine();

            System.out.print("Introduceti editura: ");
            String publisher = scanner.nextLine();

            System.out.print("Introduceti anul publicatiei: ");
            int publicationYear = scanner.nextInt();

            int categoryId = getOrCreate(c, "category", "name", category);
            int authorId = getOrCreate(c, "author", "name", author);
            int publisherId = getOrCreate(c, "publisher", "name", publisher);

            int bookId = insertBookAndGetId(c, title, categoryId);

            linkBookAuthor(c, bookId, authorId);

            linkBookPublisher(c, bookId, publisherId, publicationYear);

            c.commit();
            System.out.println("Cartea '" + title + "' a fost adaugata cu succes!");
        } catch (SQLException e) {
            System.err.println("Eroare la adaugarea cartii: " + e.getMessage());
        }
    }

    public static int getOrCreate(Connection c, String table, String nameColumn, String name) throws SQLException {
        String checkSql = String.format("SELECT id FROM %s WHERE %s = ?", table, nameColumn);
        String insertSql = String.format("INSERT INTO %s (%s) VALUES (?)", table, nameColumn);

        try (PreparedStatement pstmt = c.prepareStatement(checkSql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        try (PreparedStatement pstmt = c.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Nu s-a putut obtine ID-ul pentru " + table);
        }
    }

    private static int insertBookAndGetId(Connection c, String title, int categoryId) throws SQLException {
        String sql = "INSERT INTO book (title, category_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setInt(2, categoryId);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("Nu s-a putut obtine ID-ul cartii");
            }
        }
    }

    private static void linkBookAuthor(Connection c, int bookId, int authorId) throws SQLException {
        String sql = "INSERT INTO book_author (book_id, author_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, authorId);
            pstmt.executeUpdate();
        }
    }

    private static void linkBookPublisher(Connection c, int bookId, int publisherId, int year_published) throws SQLException {
        String sql = "INSERT INTO book_copy (book_id, publisher_id, year_published) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, publisherId);
            pstmt.setInt(3, year_published);
            pstmt.executeUpdate();
        }
    }
}