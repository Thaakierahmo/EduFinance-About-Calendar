package za.ac.cput.edufinance;

import java.time.LocalDate;

/**
 * Model for a row in CALENDAR_EVENTS.
 *
 * @author Thaakierah Mohamed
 */
public class CalendarEvent {

    private long eventId;
    private String title;
    private LocalDate eventDate;
    private String eventType;
    private String description;

    public CalendarEvent() {
    }

    public CalendarEvent(long eventId, String title, LocalDate eventDate,
                         String eventType, String description) {
        this.eventId = eventId;
        this.title = title;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.description = description;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
