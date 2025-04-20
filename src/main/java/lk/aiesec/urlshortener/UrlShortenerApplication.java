/**
 * URL Shortener Application
 *
 * @author Lahiru Jayathilake
 * @email lahiruthpala@gmail.com
 * @version 1.0
 */

package lk.aiesec.urlshortener;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServletRequest;
import lk.aiesec.urlshortener.models.UrlMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

@SpringBootApplication
@RestController
public class UrlShortenerApplication {

    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerApplication.class);

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String RANGE_FORMAT = "%s!A:B"; // Sheet name with columns A:B
    private static final String ROOT_URL = "https://signup.aiesec.lk/";

    private static final Dotenv dotenv = Dotenv.load(); // Load .env file
    private static final String SPREADSHEET_ID = dotenv.get("GOOGLE_SPREADSHEET_ID");
    private static final String SHEET_NAME = dotenv.get("SHEET_NAME", "links");
    private static final boolean DYNAMIC_SHEET = Boolean.parseBoolean(dotenv.get("DYNAMIC_SHEET", "false"));
    private static final String REGEX_FOR_SHEET_NAME = dotenv.get("REGEX_FOR_SHEET_NAME", ".*");
    private static final String CREDENTIALS_PATH = dotenv.get("GOOGLE_CREDENTIALS_PATH", "src/main/resources/credentials.json");

    private long lastRefreshTime = 0;
    private Map<String, UrlMetadata> urlMetadataMap = new HashMap<>();

    // Modified to use Service Account credentials
    private static GoogleCredentials getCredentials() throws IOException {

        if (CREDENTIALS_PATH == null || CREDENTIALS_PATH.isEmpty()) {
            throw new FileNotFoundException("GOOGLE_CREDENTIALS_PATH is not set in the .env file.");
        }

        File credentialsFile = new File(CREDENTIALS_PATH);
        if (!credentialsFile.exists()) {
            throw new FileNotFoundException("Credentials file not found at: " + CREDENTIALS_PATH);
        }

        try (InputStream in = new FileInputStream(credentialsFile)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));
            credentials.refreshIfExpired();
            return credentials;
        } catch (Exception e) {
            throw new IOException("Failed to load credentials.json: " + e.getMessage(), e);
        }
    }

    public UrlShortenerApplication() throws IOException, GeneralSecurityException {
        refreshUrlMappings();
    }

    private void refreshUrlMappings() throws IOException, GeneralSecurityException {
        logger.info("Refreshing URL mappings from Google Sheets...");

        HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credentials = getCredentials();

        // Create the Sheets API client
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Sheets URL Shortener")
                .build();

        // If DYNAMIC_SHEET is enabled, extract the sheet name dynamically
        String sheetToUse = SHEET_NAME;
        if (DYNAMIC_SHEET) {
            sheetToUse = extractSheetName(SHEET_NAME, REGEX_FOR_SHEET_NAME);
        }

        String range = String.format(RANGE_FORMAT, sheetToUse);
        ValueRange response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            logger.warn("No data found in Google Sheets.");
            return;
        }

        urlMetadataMap.clear();
        for (List<Object> row : values) {
            if (row.size() >= 2) {
                String shortcut = row.get(0).toString().toLowerCase();
                String url = row.get(1).toString();
                String title = row.size() >= 3 ? row.get(2).toString() : null;
                String description = row.size() >= 4 ? row.get(3).toString() : null;
                String image = row.size() >= 5 ? row.get(4).toString() : null;
                String type = row.size() >= 6 ? row.get(5).toString() : null;
                UrlMetadata data = new UrlMetadata(url, title, description, image, type);
                urlMetadataMap.put(shortcut, data);
                logger.debug("Loaded shortcut: [{}] -> [{}]", shortcut, url);
            }
        }

        logger.info("URL mappings refreshed successfully.");
    }

    private String extractSheetName(String baseName, String regex) {
        if (baseName.matches(regex)) {
            return baseName;
        }
        return SHEET_NAME; // Default fallback
    }

    private String generateOpenGraphHtml(UrlMetadata metadata) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta property=\"og:title\" content=\"" + metadata.getTitle() + "\" />\n" +
                "    <meta property=\"og:description\" content=\"" + metadata.getDescription() + "\" />\n" +
                "    <meta property=\"og:image\" content=\"" + metadata.getImageUrl() + "\" />\n" +
                "    <meta property=\"og:url\" content=\"" + metadata.getTargetUrl() + "\" />\n" +
                "    <meta property=\"og:type\" content=\"" + metadata.getType() + "\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "    <p>Redirecting to " + metadata.getTargetUrl() + "</p>\n" +
                "</body>\n" +
                "</html>";
    }

    @GetMapping("/**")
    public String redirect(HttpServletRequest request) throws IOException, GeneralSecurityException {
        long currentTime = System.currentTimeMillis();
        long REFRESH_INTERVAL = 10000;
        if (currentTime - lastRefreshTime > REFRESH_INTERVAL) {
            logger.info("Refreshing cache before processing request...");
            refreshUrlMappings();
            lastRefreshTime = currentTime;
        }

        // Get full request path
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            int index = queryString.indexOf('&');
            if (index != -1) {
                path += "?" + queryString.substring(0, index);
            } else {
                path += "?" + queryString;
            }
        }

        // Construct the full shortcut key
        String fullShortcut = ROOT_URL + path.substring(1); // Remove leading slash

        logger.info("Received request for shortcut: [{}]", fullShortcut);

        // Look up in mapping
        String url = null;
        try {
            url = urlMetadataMap.get(fullShortcut.toLowerCase()).getTargetUrl();
        }catch (Exception e){
            return "<meta http-equiv='refresh' content='0; url=" + ROOT_URL + "'>";
        }
        UrlMetadata metadata = urlMetadataMap.get(fullShortcut.toLowerCase());

        if (metadata != null) {
            // Check if it's Facebook's bot
            String userAgent = request.getHeader("User-Agent");
            boolean isFacebookBot = userAgent != null &&
                    (userAgent.contains("facebookexternalhit") ||
                            userAgent.contains("Facebook") ||
                            userAgent.contains("facebookcatalog"));

            if (isFacebookBot) {
                return generateOpenGraphHtml(metadata);
            } else {
                logger.info("Redirecting [{}] to [{}]", fullShortcut, url);
                return "<meta http-equiv='refresh' content='0; url=" + url + "'>";
            }
        }

        logger.warn("Shortcut [{}] not found. Redirecting to homepage.", fullShortcut);
        return "<meta http-equiv='refresh' content='0; url=" + ROOT_URL + "'>";
    }

    public static void main(String[] args) {
        logger.info("Starting URL Shortener Application...");
        SpringApplication.run(UrlShortenerApplication.class, args);
        logger.info("Application started successfully.");
    }
}