package za.ac.cput.edufinance;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EduFinance Hub About + Calendar web application.
 *
 * @author Thaakierah Mohamed
 */
public class Main {

    private static final CalendarDAO DAO = new CalendarDAO();

    public static void main(String[] args) throws Exception {

        // Test Oracle immediately so the user can see whether JDBC is working.
        try (var con = DBConnection.getConnection()) {
            System.out.println("Oracle connection successful: " + con.getMetaData().getURL());
        } catch (SQLException ex) {
            System.err.println("Oracle connection failed: " + ex.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", Main::home);
        server.createContext("/calendar", Main::calendar);
        server.createContext("/about", Main::about);
        server.createContext("/event", Main::event);
        server.createContext("/style.css", Main::style);

        server.start();

        System.out.println("EduFinance Hub is running at http://localhost:8080");

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:8080/calendar"));
            } catch (Exception ignored) {
            }
        }
    }

    private static void home(HttpExchange exchange) throws IOException {
        redirect(exchange, "/calendar");
    }

    private static void calendar(HttpExchange exchange) throws IOException {

        Map<String, String> query = parse(exchange.getRequestURI().getRawQuery());

        String search = query.getOrDefault("search", "");
        String type = query.getOrDefault("type", "ALL");
        String sort = query.getOrDefault("sort", "date_asc");

        YearMonth month;

        try {
            month = YearMonth.parse(
                    query.getOrDefault("month", YearMonth.now().toString()));
        } catch (Exception ex) {
            month = YearMonth.now();
        }

        List<CalendarEvent> events = List.of();
        String dbError = "";

        try {
            events = DAO.findAll(search, type, sort);
        } catch (SQLException ex) {
            dbError = ex.getMessage();
        }

        StringBuilder html = new StringBuilder();

        html.append("<main>");
        html.append("<section class='page-heading'>");
        html.append("<div>");
        html.append("<div class='eyebrow'>CALENDAR</div>");
        html.append("<h1>Calendar</h1>");
        html.append("<p>Track deadlines, payments, and important dates all in one place.</p>");
        html.append("</div>");
        html.append("<a class='btn primary' href='/event'>Add Event +</a>");
        html.append("</section>");

        String message = query.get("message");

        if ("added".equals(message)) {
            html.append("<div class='feedback success'>Event added successfully.</div>");
        }

        if ("updated".equals(message)) {
            html.append("<div class='feedback success'>Event updated successfully.</div>");
        }

        if ("deleted".equals(message)) {
            html.append("<div class='feedback success'>Event deleted successfully.</div>");
        }

        if (!dbError.isBlank()) {
            html.append("<div class='feedback error'>Database error: ")
                    .append(HtmlUtil.escape(dbError))
                    .append("</div>");
        }

        html.append("<form class='filters' method='get' action='/calendar'>");
        html.append("<input type='search' name='search' placeholder='Search events...' value='")
                .append(HtmlUtil.escape(search))
                .append("'>");

        html.append("<select name='type'>");

        String[] types = {
            "ALL", "Bursary", "Workshop",
            "Information", "Scholarship", "Activity"
        };

        for (String t : types) {
            html.append("<option value='").append(t).append("'")
                    .append(t.equals(type) ? " selected" : "")
                    .append(">")
                    .append(t.equals("ALL") ? "All types" : t)
                    .append("</option>");
        }

        html.append("</select>");

        html.append("<select name='sort'>");
        html.append(option("date_asc", "Date: earliest", sort));
        html.append(option("date_desc", "Date: latest", sort));
        html.append(option("title_asc", "Title: A-Z", sort));
        html.append("</select>");

        html.append("<button class='btn dark' type='submit'>Apply</button>");
        html.append("</form>");

        html.append("<section class='calendar-layout'>");

        html.append("<div class='calendar-card'>");
        html.append("<div class='calendar-header'>");
        html.append("<a href='/calendar?month=")
                .append(month.minusMonths(1))
                .append("'>&lsaquo;</a>");

        html.append("<h2>")
                .append(month.format(
                        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
                        .toUpperCase())
                .append("</h2>");

        html.append("<a href='/calendar?month=")
                .append(month.plusMonths(1))
                .append("'>&rsaquo;</a>");
        html.append("</div>");

        html.append("<div class='weekdays'>");

        for (String d : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}) {
            html.append("<span>").append(d).append("</span>");
        }

        html.append("</div>");
        html.append("<div class='days'>");

        int firstDay = month.atDay(1).getDayOfWeek().getValue();

        for (int blank = 1; blank < firstDay; blank++) {
            html.append("<div class='day empty'></div>");
        }

        for (int day = 1; day <= month.lengthOfMonth(); day++) {

            LocalDate date = month.atDay(day);

            html.append("<div class='day ")
                    .append(date.equals(LocalDate.now()) ? "today" : "")
                    .append("'>");

            html.append("<span class='day-number'>")
                    .append(day)
                    .append("</span>");

            for (CalendarEvent event : events) {

                if (date.equals(event.getEventDate())) {

                    html.append("<a class='event-chip' href='/event?id=")
                            .append(event.getEventId())
                            .append("'>")
                            .append(HtmlUtil.escape(event.getTitle()))
                            .append("</a>");
                }
            }

            html.append("</div>");
        }

        html.append("</div>");
        html.append("</div>");

        html.append("<aside class='upcoming'>");
        html.append("<h2>✦ Upcoming Events</h2>");

        int shown = 0;

        for (CalendarEvent event : events) {

            if (!event.getEventDate().isBefore(LocalDate.now()) && shown < 8) {

                shown++;

                html.append("<a class='upcoming-item' href='/event?id=")
                        .append(event.getEventId())
                        .append("'>");

                html.append("<strong>")
                        .append(HtmlUtil.escape(event.getTitle()))
                        .append("</strong>");

                html.append("<small>")
                        .append(event.getEventDate())
                        .append(" · ")
                        .append(HtmlUtil.escape(event.getEventType()))
                        .append("</small>");

                html.append("</a>");
            }
        }

        if (shown == 0) {
            html.append("<p class='muted'>No upcoming events match your filters.</p>");
        }

        html.append("</aside>");
        html.append("</section>");

        html.append("<section class='all-events'>");
        html.append("<div class='eyebrow'>YOUR SCHEDULE</div>");
        html.append("<h2>All events</h2>");

        html.append("<div class='table-wrap'>");
        html.append("<table>");
        html.append("<tr>");
        html.append("<th>Date</th>");
        html.append("<th>Title</th>");
        html.append("<th>Type</th>");
        html.append("<th>Description</th>");
        html.append("<th>Actions</th>");
        html.append("</tr>");

        for (CalendarEvent event : events) {

            html.append("<tr>");

            html.append("<td>")
                    .append(event.getEventDate())
                    .append("</td>");

            html.append("<td><strong>")
                    .append(HtmlUtil.escape(event.getTitle()))
                    .append("</strong></td>");

            html.append("<td>")
                    .append(HtmlUtil.escape(event.getEventType()))
                    .append("</td>");

            html.append("<td>")
                    .append(HtmlUtil.escape(event.getDescription()))
                    .append("</td>");

            html.append("<td class='actions'>");

            html.append("<a class='link' href='/event?id=")
                    .append(event.getEventId())
                    .append("'>Edit</a>");

            html.append("<form method='post' action='/event' ")
                    .append("onsubmit=\"return confirm('Delete this event?');\">");

            html.append("<input type='hidden' name='action' value='delete'>");
            html.append("<input type='hidden' name='id' value='")
                    .append(event.getEventId())
                    .append("'>");

            html.append("<button class='link danger' type='submit'>Delete</button>");
            html.append("</form>");

            html.append("</td>");
            html.append("</tr>");
        }

        html.append("</table>");
        html.append("</div>");
        html.append("</section>");

        html.append("</main>");

        send(exchange, 200, "text/html; charset=UTF-8",
                HtmlUtil.page("Calendar", html.toString()));
    }

    private static void event(HttpExchange exchange) throws IOException {

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            Map<String, String> form = parse(body);

            String action = form.getOrDefault("action", "save");

            try {

                if ("delete".equals(action)) {

                    DAO.delete(Long.parseLong(form.get("id")));
                    redirect(exchange, "/calendar?message=deleted");
                    return;
                }

                String title = form.getOrDefault("title", "").trim();
                String dateText = form.getOrDefault("date", "").trim();
                String type = form.getOrDefault("type", "Information").trim();
                String description = form.getOrDefault("description", "").trim();

                if (title.isBlank() || dateText.isBlank()) {

                    eventForm(
                            exchange,
                            form.get("id"),
                            "Event title and date are required.",
                            title,
                            dateText,
                            type,
                            description);

                    return;
                }

                LocalDate date = LocalDate.parse(dateText);

                Long id =
                        form.get("id") == null || form.get("id").isBlank()
                        ? null
                        : Long.parseLong(form.get("id"));

                if (DAO.duplicateExists(title, date, id)) {

                    eventForm(
                            exchange,
                            form.get("id"),
                            "An identical event already exists on this date.",
                            title,
                            dateText,
                            type,
                            description);

                    return;
                }

                CalendarEvent event =
                        new CalendarEvent(
                                id == null ? 0 : id,
                                title,
                                date,
                                type,
                                description);

                if (id == null) {

                    DAO.insert(event);
                    redirect(exchange, "/calendar?message=added");

                } else {

                    DAO.update(event);
                    redirect(exchange, "/calendar?message=updated");
                }

            } catch (Exception ex) {

                send(exchange, 500, "text/html; charset=UTF-8",
                        HtmlUtil.page(
                                "Error",
                                "<main><div class='feedback error'>" +
                                HtmlUtil.escape(ex.getMessage()) +
                                "</div><a href='/calendar'>Back to Calendar</a></main>"));
            }

            return;
        }

        Map<String, String> query =
                parse(exchange.getRequestURI().getRawQuery());

        String id = query.get("id");

        if (id != null && !id.isBlank()) {

            try {

                CalendarEvent event =
                        DAO.findById(Long.parseLong(id));

                if (event != null) {

                    eventForm(
                            exchange,
                            id,
                            "",
                            event.getTitle(),
                            event.getEventDate().toString(),
                            event.getEventType(),
                            event.getDescription());

                    return;
                }

            } catch (Exception ignored) {
            }
        }

        eventForm(
                exchange,
                "",
                "",
                "",
                "",
                "Information",
                "");
    }

    private static void eventForm(
            HttpExchange exchange,
            String id,
            String error,
            String title,
            String date,
            String type,
            String description)
            throws IOException {

        String editing =
                id != null && !id.isBlank()
                ? "Edit Event"
                : "Add New Event";

        StringBuilder html = new StringBuilder();

        html.append("<main>");
        html.append("<div class='eyebrow'>CALENDAR EVENT</div>");
        html.append("<h1>").append(editing).append("</h1>");
        html.append("<p class='muted'>Schedule a new event, deadline, or reminder.</p>");

        if (!error.isBlank()) {
            html.append("<div class='feedback error'>")
                    .append(HtmlUtil.escape(error))
                    .append("</div>");
        }

        html.append("<section class='form-card'>");
        html.append("<form method='post' action='/event'>");

        html.append("<input type='hidden' name='id' value='")
                .append(HtmlUtil.escape(id))
                .append("'>");

        html.append("<label>Event Title");
        html.append("<input required maxlength='100' name='title' value='")
                .append(HtmlUtil.escape(title))
                .append("'>");
        html.append("</label>");

        html.append("<label>Date");
        html.append("<input required type='date' name='date' value='")
                .append(HtmlUtil.escape(date))
                .append("'>");
        html.append("</label>");

        html.append("<label>Event Type");
        html.append("<select name='type'>");

        String[] types = {
            "Bursary", "Workshop", "Information",
            "Scholarship", "Activity"
        };

        for (String t : types) {
            html.append("<option")
                    .append(t.equals(type) ? " selected" : "")
                    .append(">")
                    .append(t)
                    .append("</option>");
        }

        html.append("</select>");
        html.append("</label>");

        html.append("<label>Description");
        html.append("<textarea rows='5' maxlength='500' name='description'>")
                .append(HtmlUtil.escape(description))
                .append("</textarea>");
        html.append("</label>");

        html.append("<div class='form-actions'>");
        html.append("<a class='btn light' href='/calendar'>Cancel</a>");
        html.append("<button class='btn primary' type='submit'>")
                .append(id == null || id.isBlank()
                        ? "Add Event +"
                        : "Save Changes")
                .append("</button>");
        html.append("</div>");

        html.append("</form>");
        html.append("</section>");
        html.append("</main>");

        send(exchange, 200, "text/html; charset=UTF-8",
                HtmlUtil.page(editing, html.toString()));
    }

    private static void about(HttpExchange exchange) throws IOException {

        String body = """
        <main>
            <section class='about-hero'>
                <div class='eyebrow'>ABOUT EDUFINANCE HUB</div>
                <h1>Funding made clearer.<br>Student money made simpler.</h1>
                <p>
                    EduFinance Hub connects students with funding opportunities
                    while providing practical tools for budgeting, planning,
                    financial literacy and important deadlines.
                </p>
                <div class='hero-actions'>
                    <a class='btn primary' href='/calendar'>Explore Funding</a>
                    <a class='btn light' href='#team'>Join Our Community</a>
                </div>
            </section>

            <section class='stats'>
                <article class='stat'>
                    <strong>16+</strong>
                    <span>Meaningful calendar records</span>
                </article>

                <article class='stat'>
                    <strong>4</strong>
                    <span>Core objectives</span>
                </article>

                <article class='stat'>
                    <strong>3</strong>
                    <span>Core values</span>
                </article>

                <article class='stat'>
                    <strong>1</strong>
                    <span>Connected student hub</span>
                </article>
            </section>

            <section>
                <div class='eyebrow'>OUR MISSION</div>
                <h2>We help students act on opportunities.</h2>

                <div class='cards'>

                    <article class='card'>
                        <h3>Connect Students to Bursaries</h3>
                        <p>
                            Make bursaries and scholarships easier to discover,
                            understand and track.
                        </p>
                    </article>

                    <article class='card'>
                        <h3>Provide Financial Tools</h3>
                        <p>
                            Support student budgeting, expenses, savings and
                            financial planning.
                        </p>
                    </article>

                    <article class='card'>
                        <h3>Build Community</h3>
                        <p>
                            Create a student-centred platform that encourages
                            informed decisions and progress.
                        </p>
                    </article>

                    <article class='card'>
                        <h3>Teach Financial Literacy</h3>
                        <p>
                            Make practical money education accessible,
                            understandable and useful.
                        </p>
                    </article>

                </div>
            </section>

            <section class='values'>
                <div class='eyebrow'>CORE VALUES</div>
                <h2>Built for real student needs.</h2>

                <div class='value-grid'>

                    <article>
                        <h3>Accessibility</h3>
                        <p>
                            Clear information and simple navigation for students.
                        </p>
                    </article>

                    <article>
                        <h3>Security</h3>
                        <p>
                            Responsible protection and handling of student data.
                        </p>
                    </article>

                    <article>
                        <h3>Empowerment</h3>
                        <p>
                            Help students make confident financial and funding
                            decisions.
                        </p>
                    </article>

                </div>
            </section>

            <section>
                <div class='eyebrow'>BURSARY PARTNERS</div>
                <h2>Funding information in one place.</h2>

                <div class='partners'>
                    <span>NSFAS</span>
                    <span>Sasol Foundation</span>
                    <span>Funza Lushaka</span>
                    <span>StudyTrust</span>
                </div>
            </section>

            <section id='team'>
                <div class='eyebrow'>TEAM ROLES</div>
                <h2>The people behind EduFinance Hub.</h2>

                <div class='cards team'>

                    <article class='card'>
                        <h3>Project Manager</h3>
                        <p>Coordinates project delivery and collaboration.</p>
                    </article>

                    <article class='card'>
                        <h3>Technical Manager</h3>
                        <p>Coordinates technical implementation and integration.</p>
                    </article>

                    <article class='card'>
                        <h3>Data Manager</h3>
                        <p>Maintains database quality, structure and integrity.</p>
                    </article>

                    <article class='card'>
                        <h3>Information Manager</h3>
                        <p>Organises system information and requirements.</p>
                    </article>

                    <article class='card'>
                        <h3>Lead Modeler</h3>
                        <p>Maintains system models and design consistency.</p>
                    </article>

                    <article class='card'>
                        <h3>Marketing Manager</h3>
                        <p>Supports communication and platform awareness.</p>
                    </article>

                </div>
            </section>
        </main>
        """;

        send(exchange, 200, "text/html; charset=UTF-8",
                HtmlUtil.page("About", body));
    }

    private static void style(HttpExchange exchange) throws IOException {

        String css = """
        :root {
            --purple: #7c4dff;
            --purple-dark: #6039d6;
            --lavender: #cfc5ff;
            --lavender-light: #f6f3ff;
            --ink: #17151f;
            --muted: #6c6878;
            --line: #ded8f2;
            --red: #d64054;
        }

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            background: #f1f1f3;
            color: var(--ink);
            font-family: Arial, Helvetica, sans-serif;
            line-height: 1.5;
        }

        a {
            color: inherit;
        }

        .shell {
            width: min(1180px, 100%);
            min-height: 100vh;
            margin: auto;
            background: white;
        }

        header {
            height: 72px;
            padding: 0 30px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 1px solid #eee;
        }

        .brand {
            display: flex;
            gap: 9px;
            align-items: center;
            text-decoration: none;
            font-weight: 700;
        }

        .brand-circle {
            width: 24px;
            height: 24px;
            border: 2px solid var(--purple);
            border-radius: 50%;
        }

        nav a {
            margin-left: 18px;
            text-decoration: none;
            font-weight: 700;
            color: #5b5277;
        }

        main {
            padding: 38px 42px 60px;
        }

        .page-heading {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            gap: 20px;
        }

        h1 {
            font-size: 42px;
            margin: 4px 0 8px;
        }

        h2 {
            margin: 6px 0 14px;
        }

        .page-heading p,
        .muted {
            color: var(--muted);
        }

        .eyebrow {
            color: var(--purple-dark);
            font-weight: 900;
            letter-spacing: .14em;
            font-size: 12px;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-height: 42px;
            padding: 0 18px;
            border-radius: 999px;
            border: 1px solid transparent;
            text-decoration: none;
            font-weight: 800;
            cursor: pointer;
        }

        .primary {
            background: linear-gradient(135deg, var(--purple), #945eff);
            color: white;
        }

        .light {
            background: white;
            border-color: #d8d1ed;
            color: #2b263b;
        }

        .dark {
            background: #211d30;
            color: white;
        }

        .feedback {
            margin: 16px 0;
            padding: 12px 15px;
            border-radius: 13px;
            font-weight: 700;
        }

        .success {
            background: #e9f8f1;
            color: #126c4a;
        }

        .error {
            background: #fff0f2;
            color: #a52d40;
        }

        .filters {
            display: grid;
            grid-template-columns: 1fr 160px 150px auto;
            gap: 10px;
            margin: 20px 0;
            padding: 12px;
            border: 1px solid var(--line);
            border-radius: 16px;
            background: var(--lavender-light);
        }

        input,
        select,
        textarea {
            width: 100%;
            padding: 11px 12px;
            border: 1px solid #d7d0ed;
            border-radius: 12px;
            background: white;
            font: inherit;
        }

        .calendar-layout {
            display: grid;
            grid-template-columns: 1.65fr .8fr;
            gap: 20px;
        }

        .calendar-card {
            padding: 16px;
            border: 2px solid var(--purple);
            border-radius: 24px;
            background: linear-gradient(145deg, #d8ceff, #c5b5ff);
            box-shadow: 0 6px 0 #9b6cff;
        }

        .calendar-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .calendar-header a {
            width: 34px;
            height: 34px;
            display: grid;
            place-items: center;
            border-radius: 50%;
            background: white;
            text-decoration: none;
            color: var(--purple-dark);
            font-size: 25px;
            font-weight: 900;
        }

        .weekdays,
        .days {
            display: grid;
            grid-template-columns: repeat(7, 1fr);
        }

        .weekdays span {
            text-align: center;
            padding: 8px 4px;
            font-size: 11px;
            font-weight: 900;
        }

        .day {
            min-height: 84px;
            padding: 6px;
            overflow: hidden;
            border-right: 1px solid #eee9fb;
            border-bottom: 1px solid #eee9fb;
            background: white;
        }

        .empty {
            background: #f2effc;
        }

        .today {
            box-shadow: inset 0 0 0 2px var(--purple);
        }

        .day-number {
            display: block;
            font-size: 11px;
            font-weight: 900;
        }

        .event-chip {
            display: block;
            margin-top: 3px;
            padding: 4px;
            overflow: hidden;
            border-radius: 5px;
            background: #eee9ff;
            color: #49358a;
            text-decoration: none;
            font-size: 9px;
            white-space: nowrap;
            text-overflow: ellipsis;
        }

        .upcoming {
            align-self: start;
            padding: 18px;
            border: 2px solid #8d67d8;
            border-radius: 18px;
        }

        .upcoming-item {
            display: block;
            padding: 10px;
            border-radius: 10px;
            text-decoration: none;
        }

        .upcoming-item:hover {
            background: var(--lavender-light);
        }

        .upcoming-item strong,
        .upcoming-item small {
            display: block;
        }

        .upcoming-item small {
            margin-top: 3px;
            color: var(--muted);
        }

        .all-events {
            margin-top: 34px;
        }

        .table-wrap {
            overflow-x: auto;
            border: 1px solid var(--line);
            border-radius: 16px;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            min-width: 760px;
        }

        th,
        td {
            padding: 12px;
            text-align: left;
            vertical-align: top;
            border-bottom: 1px solid #eee9f7;
        }

        th {
            background: #faf9ff;
            font-size: 12px;
        }

        .actions {
            display: flex;
            gap: 10px;
        }

        .actions form {
            margin: 0;
        }

        .link {
            padding: 0;
            border: 0;
            background: none;
            color: var(--purple-dark);
            font-weight: 800;
            cursor: pointer;
        }

        .danger {
            color: var(--red);
        }

        .form-card {
            width: min(650px, 100%);
            margin: 24px auto;
            padding: 26px;
            background: var(--lavender);
        }

        .form-card form {
            display: grid;
            gap: 16px;
        }

        .form-card label {
            display: grid;
            gap: 7px;
            font-size: 12px;
            font-weight: 800;
        }

        .form-actions {
            display: flex;
            justify-content: flex-end;
            gap: 10px;
        }

        .about-hero {
            padding: 48px;
            border: 1px solid #b49df6;
            border-radius: 28px;
            background: linear-gradient(135deg, #eee9ff, #cab9ff);
        }

        .about-hero h1 {
            font-size: 60px;
            line-height: 1.03;
        }

        .about-hero p {
            max-width: 720px;
            color: #4c4560;
            font-size: 18px;
        }

        .hero-actions {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
        }

        .stats {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 12px;
            margin: 18px 0 36px;
        }

        .stat,
        .card {
            padding: 20px;
            border: 1px solid var(--line);
            border-radius: 18px;
        }

        .stat strong {
            display: block;
            color: var(--purple-dark);
            font-size: 32px;
        }

        .cards {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 12px;
        }

        .card p {
            color: var(--muted);
            font-size: 14px;
        }

        .values {
            margin: 30px 0;
            padding: 30px;
            border-radius: 24px;
            background: #211d30;
            color: white;
        }

        .value-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 12px;
        }

        .value-grid article {
            padding: 16px;
            border-radius: 14px;
            background: #2d2740;
        }

        .partners {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-bottom: 30px;
        }

        .partners span {
            padding: 11px 16px;
            border: 1px solid #d1c6f4;
            border-radius: 999px;
            background: #f5f2ff;
            color: #56477d;
            font-weight: 800;
        }

        .team {
            grid-template-columns: repeat(3, 1fr);
        }

        footer {
            padding: 25px 40px;
            border-top: 1px solid #eee;
            color: var(--muted);
        }

        @media (max-width: 850px) {

            .calendar-layout,
            .stats,
            .cards,
            .value-grid {
                grid-template-columns: 1fr 1fr;
            }

            .filters {
                grid-template-columns: 1fr 1fr;
            }
        }

        @media (max-width: 600px) {

            main {
                padding: 24px 15px 45px;
            }

            .calendar-layout,
            .stats,
            .cards,
            .value-grid,
            .filters {
                grid-template-columns: 1fr;
            }

            .about-hero {
                padding: 28px 20px;
            }

            .about-hero h1 {
                font-size: 40px;
            }

            .day {
                min-height: 54px;
            }

            .event-chip {
                width: 7px;
                height: 7px;
                padding: 0;
                border-radius: 50%;
                font-size: 0;
            }
        }
        """;

        send(exchange, 200, "text/css; charset=UTF-8", css);
    }

    private static String option(
            String value,
            String label,
            String selected) {

        return "<option value='" +
                value +
                "'" +
                (value.equals(selected) ? " selected" : "") +
                ">" +
                label +
                "</option>";
    }

    private static Map<String, String> parse(String raw) {

        Map<String, String> result = new LinkedHashMap<>();

        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String pair : raw.split("&")) {

            String[] parts = pair.split("=", 2);

            String key =
                    URLDecoder.decode(
                            parts[0],
                            StandardCharsets.UTF_8);

            String value =
                    parts.length > 1
                    ? URLDecoder.decode(
                            parts[1],
                            StandardCharsets.UTF_8)
                    : "";

            result.put(key, value);
        }

        return result;
    }

    private static void send(
            HttpExchange exchange,
            int status,
            String contentType,
            String body)
            throws IOException {

        byte[] bytes =
                body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set("Content-Type", contentType);

        exchange.sendResponseHeaders(
                status,
                bytes.length);

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(bytes);
        }
    }

    private static void redirect(
            HttpExchange exchange,
            String location)
            throws IOException {

        exchange.getResponseHeaders()
                .set("Location", location);

        exchange.sendResponseHeaders(
                302,
                -1);

        exchange.close();
    }
}
