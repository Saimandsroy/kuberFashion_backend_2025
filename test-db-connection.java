import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDbConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String username = "postgres.hanmurmflpqbfwwvqnsl";
        String password = "KuberFashion2025@";
        
        System.out.println("Testing database connection...");
        System.out.println("URL: " + url);
        System.out.println("Username: " + username);
        
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            if (connection.isValid(10)) {
                System.out.println("✅ Database connection successful!");
                System.out.println("Database Product: " + connection.getMetaData().getDatabaseProductName());
                System.out.println("Database Version: " + connection.getMetaData().getDatabaseProductVersion());
            } else {
                System.out.println("❌ Database connection is not valid");
            }
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
