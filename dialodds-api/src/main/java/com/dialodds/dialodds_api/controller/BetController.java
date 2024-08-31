package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.BetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bets")
public class BetController {

    @Autowired
    private BetService betService;

    @PostMapping
    public ResponseEntity<Integer> placeBet(@RequestParam int userId, @RequestParam int seasonId,
            @RequestParam int gameId, @RequestParam String betType,
            @RequestParam int amount) {
        int betId = betService.placeBet(userId, seasonId, gameId, betType, amount);
        return ResponseEntity.ok(betId);
    }

    @GetMapping("/users/{userId}/seasons/{seasonId}")
    public ResponseEntity<List<Map<String, Object>>> getUserBets(@PathVariable int userId, @PathVariable int seasonId) {
        return ResponseEntity.ok(betService.getUserBets(userId, seasonId));
    }

    @PostMapping("/settle/{gameId}")
    public ResponseEntity<Void> settleBets(@PathVariable int gameId, @RequestParam String winner) {
        betService.settleBets(gameId, winner);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unsettled/{gameId}")
    public ResponseEntity<List<Map<String, Object>>> getUnsettledBetsForGame(@PathVariable int gameId) {
        return ResponseEntity.ok(betService.getUnsettledBetsForGame(gameId));
    }
}