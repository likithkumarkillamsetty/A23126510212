package com.likith.CampusNotificationsMicroservice.middleware;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;

public class Logger {

    private static final String LOG_URL = "http://20.244.56.144/evaluation-service/logs";
    private static String authToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJNYXBDbGFpbXMiOnsiYXVkIjoiaHR0cDovLzIwLjI0NC41Ni4xNDQvZXZhbHVhdGlvbi1zZXJ2aWNlIiwiZW1haWwiOiJraWxsYW1zZXR0eWxpa2l0aGt1bWFyLjIzLmNzZUBhbml0cy5lZHUuaW4iLCJleHAiOjE3ODIxOTkwMTksImlhdCI6MTc4MjE5ODExOSwiaXNzIjoiQWZmb3JkIE1lZGljYWwgVGVjaG5vbG9naWVzIFByaXZhdGUgTGltaXRlZCIsImp0aSI6IjE4NmVjNDljLTJmZGQtNDJmYy1hNzRmLTNiODY2Y2FiNTZlYyIsImxvY2FsZSI6ImVuLUlOIiwibmFtZSI6Imxpa2l0aCBrdW1hciBraWxsYW1zZXR0eSIsInN1YiI6IjAzZTZmYTZhLTg3ZmEtNGJlMy05NDA3LWM1YjE5YmZkNzQ2ZiJ9LCJlbWFpbCI6ImtpbGxhbXNldHR5bGlraXRoa3VtYXIuMjMuY3NlQGFuaXRzLmVkdS5pbiIsIm5hbWUiOiJsaWtpdGgga3VtYXIga2lsbGFtc2V0dHkiLCJyb2xsTm8iOiJhMjMxMjY1MTAyMTIiLCJhY2Nlc3NDb2RlIjoiTVRxeGFyIiwiY2xpZW50SUQiOiIwM2U2ZmE2YS04N2ZhLTRiZTMtOTQwNy1jNWIxOWJmZDc0NmYiLCJjbGllbnRTZWNyZXQiOiJXR0h0a3F1dVZXdm1CeFlLIn0.AhD36xr3mtA30FKgNyW0_MOkA60g3SRr7HwpD3Rgvzc";
    public static void log(String stack, String level, String pkg, String message) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);
            Map<String, String> body = Map.of(
                    "stack", stack,
                    "level", level,
                    "package", pkg,
                    "message", message
            );
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(LOG_URL, request, String.class);
        } catch (Exception e) {
        }
    }
}