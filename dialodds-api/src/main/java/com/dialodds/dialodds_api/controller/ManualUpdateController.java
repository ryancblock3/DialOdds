package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.OddsPopulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class ManualUpdateController {

    @Autowired
    private OddsPopulationService oddsPopulationService;

    @PostMapping("/update-database")
    public ResponseEntity<String> manuallyUpdateDatabase() {
        String result = oddsPopulationService.manuallyPopulateDatabase();
        return ResponseEntity.ok(result);
    }
}