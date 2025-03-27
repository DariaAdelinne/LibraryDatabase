import java.sql.*;
import java.util.*;

public class DeleteBook {
    public static void deleteBook(Scanner scanner) {
        try (Connection c = DatabaseConnection.connect()) {
            c.setAutoCommit(false);

            System.out.print("Introduceti titlul cartii pe care doriti sa o stergeti: ");
            String title = scanner.nextLine();

            List<Integer> bookIds = getIds(c, "book", "id", "title", title);
            if (bookIds.isEmpty()) {
                System.out.println("Nu exista nicio carte cu titlul: '" + title + "'");
                return;
            }

            for (int bookId : bookIds) {
                deleteBookWithDependencies(c, scanner, bookId);
            }

            cleanupAllUnusedResources(c);

            c.commit();
            System.out.println("Cartea '" + title + "' a fost stearsa cu succes!");

        } catch (SQLException e) {
            System.err.println("Eroare la stergerea cartii: " + e.getMessage());
        }
    }

    private static void deleteBookWithDependencies(Connection c, Scanner scanner, int bookId) throws SQLException {
        List<Integer> publisherIds = getIds(c, "book_copy", "publisher_id", "book_id", bookId);

        if (publisherIds.size() > 1) {
            System.out.println("Exista mai multe copii ale cartii la edituri diferite.");
            Map<Integer, String> publisherMap = new HashMap<>();
            for (int publisherId : publisherIds) {
                String publisherName = getPublisherName(c, publisherId);
                publisherMap.put(publisherId, publisherName);
                System.out.println(publisherId + ". " + publisherName);
            }
            System.out.print("Introduceti ID-ul editurii pentru care doriti sa stergeti copii: ");
            int chosenPublisherId = Integer.parseInt(scanner.nextLine());
            if (!publisherMap.containsKey(chosenPublisherId)) {
                System.out.println("Editura selectata nu este valida. Anulare stergere.");
                return;
            }
            executeUpdate(c, bookId, chosenPublisherId);
        } else {
            executeUpdate(c, "DELETE FROM book_copy WHERE book_id = ?", bookId);
        }

        List<Integer> authorIds = getIds(c, "book_author", "author_id", "book_id", bookId);
        Integer categoryId = null;
        String categoryQuery = "SELECT category_id FROM book WHERE id = ?";
        try (PreparedStatement pstmt = c.prepareStatement(categoryQuery)) {
            pstmt.setInt(1, bookId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                categoryId = rs.getInt("category_id");
            }
        }

        executeUpdate(c, "DELETE FROM book_author WHERE book_id = ?", bookId);
        executeUpdate(c, "DELETE FROM book WHERE id = ?", bookId);

        cleanupUnusedRecords(c, "author", "book_author", "author_id", authorIds);
        cleanupUnusedRecords(c, "category", "book", "category_id", categoryId == null ? List.of() : List.of(categoryId));
    }

    private static String getPublisherName(Connection c, int publisherId) throws SQLException {
        String query = "SELECT name FROM publisher WHERE id = ?";
        try (PreparedStatement pstmt = c.prepareStatement(query)) {
            pstmt.setInt(1, publisherId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        }
        return "Necunoscut";
    }

    private static List<Integer> getIds(Connection c, String table, String idColumn, String whereColumn, Object value) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT " + idColumn + " FROM " + table + " WHERE " + whereColumn + " = ?";

        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            if (value instanceof String) {
                pstmt.setString(1, (String) value);
            } else {
                pstmt.setInt(1, (Integer) value);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt(idColumn));
            }
        }
        return ids;
    }

    private static void executeUpdate(Connection c, int param1, int param2) throws SQLException {
        try (PreparedStatement pstmt = c.prepareStatement("DELETE FROM book_copy WHERE book_id = ? AND publisher_id = ?")) {
            pstmt.setInt(1, param1);
            pstmt.setInt(2, param2);
            pstmt.executeUpdate();
        }
    }

    private static void executeUpdate(Connection c, String sql, int param) throws SQLException {
        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            pstmt.setInt(1, param);
            pstmt.executeUpdate();
        }
    }

    private static void cleanupUnusedRecords(Connection c, String targetTable, String relationTable, String relationColumn, List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return;

        String checkSql = "SELECT id FROM " + targetTable + " WHERE id = ? AND NOT EXISTS (SELECT 1 FROM " + relationTable + " WHERE " + relationColumn + " = id)";
        String deleteSql = "DELETE FROM " + targetTable + " WHERE id = ?";

        try (PreparedStatement checkStmt = c.prepareStatement(checkSql);
             PreparedStatement deleteStmt = c.prepareStatement(deleteSql)) {
            for (int id : ids) {
                checkStmt.setInt(1, id);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        deleteStmt.setInt(1, id);
                        deleteStmt.executeUpdate();
                    }
                }
            }
        }
    }

    private static void cleanupAllUnusedResources(Connection c) throws SQLException {
        executeCleanup(c, "DELETE FROM author WHERE id NOT IN (SELECT DISTINCT author_id FROM book_author)");
        executeCleanup(c, "DELETE FROM publisher WHERE id NOT IN (SELECT DISTINCT publisher_id FROM book_copy)");
        executeCleanup(c, "DELETE FROM category WHERE id NOT IN (SELECT DISTINCT category_id FROM book)");
    }

    private static void executeCleanup(Connection c, String sql) throws SQLException {
        try (PreparedStatement pstmt = c.prepareStatement(sql)) {
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                System.out.println("Sterse " + deleted + " inregistrari neutilizate.");
            }
        }
    }
}
