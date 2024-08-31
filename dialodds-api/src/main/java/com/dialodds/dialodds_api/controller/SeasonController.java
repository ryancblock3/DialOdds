package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seasons")
public class SeasonController {

    @Autowired
    private SeasonService seasonService;

    @PostMapping
    public ResponseEntity<Integer> createSeason(@RequestParam int startWeek, @RequestParam int endWeek, @RequestParam int initialCoins) {
        int seasonId = seasonService.createSeason(startWeek, endWeek, initialCoins);
        return ResponseEntity.ok(seasonId);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSeasons() {
        return ResponseEntity.ok(seasonService.getAllSeasons());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveSeasons() {
        return ResponseEntity.ok(seasonService.getActiveSeasons());
    }

    @GetMapping("/{seasonId}")
    public ResponseEntity<Map<String, Object>> getSeasonById(@PathVariable int seasonId) {
        return ResponseEntity.ok(seasonService.getSeasonById(seasonId));
    }

    @DeleteMapping("/{seasonId}")
    public ResponseEntity<Map<String, Object>> deleteSeason(@PathVariable int seasonId) {
        Map<String, Object> result = seasonService.deleteSeason(seasonId);
        return ResponseEntity.ok(result);
    }
}