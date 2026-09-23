package za.ac.cput.edufinance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates Oracle JDBC connections for the EduFinance application.
 * Database credentials are read from environment variables so that
 * passwords are not stored in source code or committed to GitHub.
 *
 * Required environment variables:
 * EDUFINANCE_DB_USER
 * EDUFINANCE_DB_PASSWORD
 *
 * Optional environment variable:
 * EDUFINANCE_DB_URL (defaults to the local FREEPDB1 database)
 *
 * @author Thaakierah Mohamed
 */
public final class DBConnection {

    private static final String DEFAULT_URL =
            "jdbc:oracle:thin:@localhost:1521/FREEPDB1";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("EDUFINANCE_DB_URL", DEFAULT_URL);
        String user = System.getenv("EDUFINANCE_DB_USER");
        String password = System.getenv("EDUFINANCE_DB_PASSWORD");

        if (user == null || user.isBlank() || password == null || password.isBlank()) {
            throw new SQLException(
                    "Database credentials are not configured. Set EDUFINANCE_DB_USER "
                    + "and EDUFINANCE_DB_PASSWORD before running the application."
            );
        }

        return DriverManager.getConnection(url, user, password);
    }
}
