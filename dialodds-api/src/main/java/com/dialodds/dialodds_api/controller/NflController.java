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

    @GetMapping("/games/id/{gameId}")
    public ResponseEntity<Map<String, Object>> getGameById(@PathVariable int gameId) {
        Map<String, Object> game = nflService.getGameById(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(game);
    }

    @PostMapping("/games/{gameId}/result")
    public ResponseEntity<String> updateGameResult(@PathVariable int gameId, @RequestParam String winner) {
        try {
            nflService.updateGameResult(gameId, winner);
            return ResponseEntity.ok("Game result updated successfully and bets settled");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred while updating the game result");
        }
    }

    @GetMapping("/games/pending-results")
    public ResponseEntity<List<Map<String, Object>>> getGamesWithPendingResults() {
        return ResponseEntity.ok(nflService.getGamesWithPendingResults());
    }
}
