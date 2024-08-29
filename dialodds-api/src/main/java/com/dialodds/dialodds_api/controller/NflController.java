package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.NflService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nfl")
public class NflController {

    @Autowired
    private NflService nflService;

    @GetMapping("/weeks")
    public ResponseEntity<List<Integer>> getNflWeeks() {
        return ResponseEntity.ok(nflService.getNflWeeks());
    }

    @GetMapping("/games/{week}")
    public ResponseEntity<List<Map<String, Object>>> getNflGamesByWeek(@PathVariable int week) {
        return ResponseEntity.ok(nflService.getNflGamesByWeek(week));
    }

    @GetMapping("/schedule/{team}")
    public ResponseEntity<List<Map<String, Object>>> getTeamSchedule(@PathVariable String team) {
        return ResponseEntity.ok(nflService.getTeamSchedule(team));
    }
}
