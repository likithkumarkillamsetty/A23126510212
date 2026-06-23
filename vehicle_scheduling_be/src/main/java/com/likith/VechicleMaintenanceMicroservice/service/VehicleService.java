package com.likith.VechicleMaintenanceMicroservice.service;

import com.likith.VechicleMaintenanceMicroservice.middleware.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class VehicleService {

    private static final String BASE_URL = "http://4.224.186.213/evaluation-service";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJNYXBDbGFpbXMiOnsiYXVkIjoiaHR0cDovLzIwLjI0NC41Ni4xNDQvZXZhbHVhdGlvbi1zZXJ2aWNlIiwiZW1haWwiOiJraWxsYW1zZXR0eWxpa2l0aGt1bWFyLjIzLmNzZUBhbml0cy5lZHUuaW4iLCJleHAiOjE3ODIxOTcyODQsImlhdCI6MTc4MjE5NjM4NCwiaXNzIjoiQWZmb3JkIE1lZGljYWwgVGVjaG5vbG9naWVzIFByaXZhdGUgTGltaXRlZCIsImp0aSI6ImFmMWMzYzIyLWEzZWYtNDAwMS1iZWZmLWJmM2E0OTMzNTBlNCIsImxvY2FsZSI6ImVuLUlOIiwibmFtZSI6Imxpa2l0aCBrdW1hciBraWxsYW1zZXR0eSIsInN1YiI6IjAzZTZmYTZhLTg3ZmEtNGJlMy05NDA3LWM1YjE5YmZkNzQ2ZiJ9LCJlbWFpbCI6ImtpbGxhbXNldHR5bGlraXRoa3VtYXIuMjMuY3NlQGFuaXRzLmVkdS5pbiIsIm5hbWUiOiJsaWtpdGgga3VtYXIga2lsbGFtc2V0dHkiLCJyb2xsTm8iOiJhMjMxMjY1MTAyMTIiLCJhY2Nlc3NDb2RlIjoiTVRxeGFyIiwiY2xpZW50SUQiOiIwM2U2ZmE2YS04N2ZhLTRiZTMtOTQwNy1jNWIxOWJmZDc0NmYiLCJjbGllbnRTZWNyZXQiOiJXR0h0a3F1dVZXdm1CeFlLIn0.1Tn-guYWZ_j-hz3e07VSQ8TPU5zu5FhnsjDVrPNkGIk";
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public Map<String, Object> getDepots() {
        Logger.log("backend", "info", "service", "Fetching depots");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/depots", HttpMethod.GET, entity, Map.class
        );
        Logger.log("backend", "info", "service", "Depots fetched successfully");
        return response.getBody();
    }

    public Map<String, Object> getVehicles() {
        Logger.log("backend", "info", "service", "Fetching vehicles");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/vehicles", HttpMethod.GET, entity, Map.class
        );
        Logger.log("backend", "info", "service", "Vehicles fetched successfully");
        return response.getBody();
    }

    public Map<String, Object> schedule() {
        Logger.log("backend", "info", "service", "Starting schedule calculation");

        Map<String, Object> depotData = getDepots();
        Map<String, Object> vehicleData = getVehicles();

        List<Map<String, Object>> depots = (List<Map<String, Object>>) depotData.get("depots");
        List<Map<String, Object>> vehicles = (List<Map<String, Object>>) vehicleData.get("vehicles");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> schedules = new ArrayList<>();

        for (Map<String, Object> depot : depots) {
            int depotId = (int) depot.get("ID");
            int mechanicHours = (int) depot.get("MechanicHours");

            Logger.log("backend", "info", "service", "Running knapsack for depot: " + depotId);

            List<String> selectedTasks = knapsack(vehicles, mechanicHours);

            Map<String, Object> schedule = new HashMap<>();
            schedule.put("depotId", depotId);
            schedule.put("mechanicHours", mechanicHours);
            schedule.put("selectedTasks", selectedTasks);
            schedules.add(schedule);
        }

        result.put("schedules", schedules);
        Logger.log("backend", "info", "service", "Schedule calculation complete");
        return result;
    }

    private List<String> knapsack(List<Map<String, Object>> vehicles, int capacity) {
        int n = vehicles.size();
        int[] dp = new int[capacity + 1];

        for (int i = 0; i < n; i++) {
            int duration = (int) vehicles.get(i).get("Duration");
            int impact = (int) vehicles.get(i).get("Impact");
            for (int w = capacity; w >= duration; w--) {
                dp[w] = Math.max(dp[w], dp[w - duration] + impact);
            }
        }

        List<String> selected = new ArrayList<>();
        int w = capacity;
        for (int i = n - 1; i >= 0; i--) {
            int duration = (int) vehicles.get(i).get("Duration");
            int impact = (int) vehicles.get(i).get("Impact");
            if (w >= duration && dp[w] == dp[w - duration] + impact) {
                selected.add((String) vehicles.get(i).get("TaskID"));
                w -= duration;
            }
        }

        return selected;
    }
}