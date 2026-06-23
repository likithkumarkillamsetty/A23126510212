package com.likith.VechicleMaintenanceMicroservice.controller;

import com.likith.VechicleMaintenanceMicroservice.middleware.Logger;
import com.likith.VechicleMaintenanceMicroservice.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Object>> schedule() {
        Logger.log("backend", "info", "controller", "GET /schedule received");
        Map<String, Object> result = vehicleService.schedule();
        Logger.log("backend", "info", "controller", "GET /schedule response sent");
        return ResponseEntity.ok(result);
    }
}