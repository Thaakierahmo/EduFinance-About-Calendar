package za.ac.cput.edufinance;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD operations for CALENDAR_EVENTS.
 *
 * @author Thaakierah Mohamed
 */
public class CalendarDAO {

    public List<CalendarEvent> findAll(String search, String type, String sort)
            throws SQLException {

        String orderBy = switch (sort == null ? "" : sort) {
            case "date_desc" -> "EVENT_DATE DESC";
            case "title_asc" -> "LOWER(TITLE) ASC";
            default -> "EVENT_DATE ASC";
        };

        StringBuilder sql = new StringBuilder(
                "SELECT EVENT_ID, TITLE, EVENT_DATE, EVENT_TYPE, DESCRIPTION " +
                "FROM CALENDAR_EVENTS WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(TITLE) LIKE ? OR LOWER(DESCRIPTION) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
        }

        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            sql.append("AND EVENT_TYPE = ? ");
            params.add(type);
        }

        sql.append("ORDER BY ").append(orderBy);

        List<CalendarEvent> events = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(map(rs));
                }
            }
        }

        return events;
    }

    public CalendarEvent findById(long eventId) throws SQLException {
        String sql =
                "SELECT EVENT_ID, TITLE, EVENT_DATE, EVENT_TYPE, DESCRIPTION " +
                "FROM CALENDAR_EVENTS WHERE EVENT_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean duplicateExists(String title, LocalDate date, Long ignoreId)
            throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM CALENDAR_EVENTS " +
                "WHERE LOWER(TITLE)=LOWER(?) AND EVENT_DATE=?" +
                (ignoreId == null ? "" : " AND EVENT_ID<>?");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setDate(2, Date.valueOf(date));

            if (ignoreId != null) {
                ps.setLong(3, ignoreId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public void insert(CalendarEvent event) throws SQLException {
        String sql =
                "INSERT INTO CALENDAR_EVENTS " +
                "(TITLE, EVENT_DATE, EVENT_TYPE, DESCRIPTION) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, event.getTitle());
            ps.setDate(2, Date.valueOf(event.getEventDate()));
            ps.setString(3, event.getEventType());
            ps.setString(4, event.getDescription());
            ps.executeUpdate();
        }
    }

    public void update(CalendarEvent event) throws SQLException {
        String sql =
                "UPDATE CALENDAR_EVENTS " +
                "SET TITLE=?, EVENT_DATE=?, EVENT_TYPE=?, DESCRIPTION=? " +
                "WHERE EVENT_ID=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, event.getTitle());
            ps.setDate(2, Date.valueOf(event.getEventDate()));
            ps.setString(3, event.getEventType());
            ps.setString(4, event.getDescription());
            ps.setLong(5, event.getEventId());
            ps.executeUpdate();
        }
    }

    public void delete(long eventId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM CALENDAR_EVENTS WHERE EVENT_ID=?")) {

            ps.setLong(1, eventId);
            ps.executeUpdate();
        }
    }

    private CalendarEvent map(ResultSet rs) throws SQLException {
        return new CalendarEvent(
                rs.getLong("EVENT_ID"),
                rs.getString("TITLE"),
                rs.getDate("EVENT_DATE").toLocalDate(),
                rs.getString("EVENT_TYPE"),
                rs.getString("DESCRIPTION")
        );
    }
}
