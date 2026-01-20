import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String username = "postgres.hanmurmflpqbfwwvqnsl";
        String password = "saimandsroy2005@";
        
        System.out.println("Testing database connection...");
        System.out.println("URL: " + url);
        System.out.println("Username: " + username);
        
        try {
            Class.forName("org.postgresql.Driver");
            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("✅ Database connection successful!");
            connection.close();
        } catch (ClassNotFoundException e) {
            System.err.println("❌ PostgreSQL driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
