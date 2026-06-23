package com.likith.CampusNotificationsMicroservice.service;

import com.likith.CampusNotificationsMicroservice.middleware.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final String BASE_URL = "http://4.224.186.213/evaluation-service";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJNYXBDbGFpbXMiOnsiYXVkIjoiaHR0cDovLzIwLjI0NC41Ni4xNDQvZXZhbHVhdGlvbi1zZXJ2aWNlIiwiZW1haWwiOiJraWxsYW1zZXR0eWxpa2l0aGt1bWFyLjIzLmNzZUBhbml0cy5lZHUuaW4iLCJleHAiOjE3ODIxOTkwMTksImlhdCI6MTc4MjE5ODExOSwiaXNzIjoiQWZmb3JkIE1lZGljYWwgVGVjaG5vbG9naWVzIFByaXZhdGUgTGltaXRlZCIsImp0aSI6IjE4NmVjNDljLTJmZGQtNDJmYy1hNzRmLTNiODY2Y2FiNTZlYyIsImxvY2FsZSI6ImVuLUlOIiwibmFtZSI6Imxpa2l0aCBrdW1hciBraWxsYW1zZXR0eSIsInN1YiI6IjAzZTZmYTZhLTg3ZmEtNGJlMy05NDA3LWM1YjE5YmZkNzQ2ZiJ9LCJlbWFpbCI6ImtpbGxhbXNldHR5bGlraXRoa3VtYXIuMjMuY3NlQGFuaXRzLmVkdS5pbiIsIm5hbWUiOiJsaWtpdGgga3VtYXIga2lsbGFtc2V0dHkiLCJyb2xsTm8iOiJhMjMxMjY1MTAyMTIiLCJhY2Nlc3NDb2RlIjoiTVRxeGFyIiwiY2xpZW50SUQiOiIwM2U2ZmE2YS04N2ZhLTRiZTMtOTQwNy1jNWIxOWJmZDc0NmYiLCJjbGllbnRTZWNyZXQiOiJXR0h0a3F1dVZXdm1CeFlLIn0.AhD36xr3mtA30FKgNyW0_MOkA60g3SRr7HwpD3Rgvzc";

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public List<Map<String, Object>> getTopNotifications(int n) {
        Logger.log("backend", "info", "service", "Fetching notifications from API");

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/notifications", HttpMethod.GET, entity, Map.class
        );

        List<Map<String, Object>> notifications =
                (List<Map<String, Object>>) response.getBody().get("notifications");

        Logger.log("backend", "info", "service", "Fetched " + notifications.size() + " notifications");

        List<Map<String, Object>> scored = notifications.stream()
                .map(notification -> {
                    Map<String, Object> copy = new HashMap<>(notification);
                    copy.put("priorityScore", calculatePriority(notification));
                    return copy;
                })
                .sorted((a, b) -> Double.compare(
                        (double) b.get("priorityScore"),
                        (double) a.get("priorityScore")
                ))
                .limit(n)
                .collect(Collectors.toList());

        Logger.log("backend", "info", "service", "Returning top " + n + " notifications");
        return scored;
    }

    private double calculatePriority(Map<String, Object> notification) {
        String type = (String) notification.get("Type");
        String timestamp = (String) notification.get("Timestamp");

        int weight = switch (type) {
            case "Placement" -> 3;
            case "Result" -> 2;
            case "Event" -> 1;
            default -> 0;
        };

        LocalDateTime time = LocalDateTime.parse(timestamp,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        long epochSeconds = java.sql.Timestamp.valueOf(time).getTime() / 1000;
        double recencyScore = epochSeconds / 1_000_000.0;

        return weight + recencyScore;
    }
}