package com.likith.VechicleMaintenanceMicroservice.service;

import com.likith.VechicleMaintenanceMicroservice.middleware.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class VehicleService {

    private static final String BASE_URL = "http://4.224.186.213/evaluation-service";
    private static final String TOKEN = "";

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
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            int duration = (int) vehicles.get(i - 1).get("Duration");
            int impact = (int) vehicles.get(i - 1).get("Impact");
            for (int w = 0; w <= capacity; w++) {
                dp[i][w] = dp[i - 1][w];
                if (duration <= w) {
                    dp[i][w] = Math.max(dp[i][w], dp[i - 1][w - duration] + impact);
                }
            }
        }

        List<String> selected = new ArrayList<>();
        int w = capacity;
        for (int i = n; i >= 1; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                selected.add((String) vehicles.get(i - 1).get("TaskID"));
                w -= (int) vehicles.get(i - 1).get("Duration");
            }
        }

        return selected;
    }
}