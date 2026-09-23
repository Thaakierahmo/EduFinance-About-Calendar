package za.ac.cput.edufinance;

/**
 * Small HTML helper.
 *
 * @author Thaakierah Mohamed
 */
public final class HtmlUtil {

    private HtmlUtil() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static String page(String title, String body) {
        return "<!doctype html>" +
                "<html lang='en'>" +
                "<head>" +
                "<meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<title>" + escape(title) + " | EduFinance Hub</title>" +
                "<link rel='stylesheet' href='/style.css'>" +
                "</head>" +
                "<body>" +
                "<div class='shell'>" +
                "<header>" +
                "<a class='brand' href='/calendar'><span class='brand-circle'></span>EduFinance Hub</a>" +
                "<nav><a href='/calendar'>Calendar</a><a href='/about'>About</a></nav>" +
                "</header>" +
                body +
                "<footer>EduFinance Hub · About & Calendar · Thaakierah Mohamed</footer>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
