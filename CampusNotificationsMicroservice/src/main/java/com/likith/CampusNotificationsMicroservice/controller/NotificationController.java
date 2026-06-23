package com.likith.CampusNotificationsMicroservice.controller;

import com.likith.CampusNotificationsMicroservice.middleware.Logger;
import com.likith.CampusNotificationsMicroservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/notifications/top")
    public ResponseEntity<Map<String, Object>> getTopNotifications(
            @RequestParam(defaultValue = "10") int n) {
        Logger.log("backend", "info", "controller", "GET /notifications/top?n=" + n + " received");
        List<Map<String, Object>> result = notificationService.getTopNotifications(n);
        Logger.log("backend", "info", "controller", "Returning top " + n + " notifications");
        return ResponseEntity.ok(Map.of("notifications", result));
    }
}