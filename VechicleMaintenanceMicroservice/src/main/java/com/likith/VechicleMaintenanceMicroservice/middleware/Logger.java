package com.likith.VechicleMaintenanceMicroservice.middleware;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;

public class Logger {

    private static final String LOG_URL = "http://20.244.56.144/evaluation-service/logs";
    private static String authToken = "";

    public static void setToken(String token) {
        authToken = token;
    }

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