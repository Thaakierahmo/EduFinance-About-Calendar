package za.ac.cput.edufinance;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Quick Oracle JDBC test.
 *
 * @author Thaakierah Mohamed
 */
public class TestDatabase {

    public static void main(String[] args) {

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs =
                     st.executeQuery(
                             "SELECT COUNT(*) FROM CALENDAR_EVENTS")) {

            rs.next();

            System.out.println("Database connection successful.");
            System.out.println(
                    "CALENDAR_EVENTS records: " +
                    rs.getInt(1));

        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }
}
