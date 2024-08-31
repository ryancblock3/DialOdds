package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<Integer> createOrUpdateUser(@RequestParam String discordId, @RequestParam String username) {
        int userId = userService.createOrUpdateUser(discordId, username);
        return ResponseEntity.ok(userId);
    }

    @PostMapping("/{userId}/seasons/{seasonId}")
    public ResponseEntity<Void> addUserToSeason(@PathVariable int userId, @PathVariable int seasonId) {
        userService.addUserToSeason(userId, seasonId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/seasons/{seasonId}")
    public ResponseEntity<List<Map<String, Object>>> getUsersBySeason(@PathVariable int seasonId) {
        return ResponseEntity.ok(userService.getUsersBySeason(seasonId));
    }

    @GetMapping("/{userId}/seasons/{seasonId}/coins")
    public ResponseEntity<Integer> getUserCoins(@PathVariable int userId, @PathVariable int seasonId) {
        return ResponseEntity.ok(userService.getUserCoins(userId, seasonId));
    }

    @PostMapping("/join-season")
    public ResponseEntity<Void> createUserAndJoinSeason(@RequestParam String discordId, @RequestParam String username,
            @RequestParam int seasonId) {
        userService.createUserAndJoinSeason(discordId, username, seasonId);
        return ResponseEntity.ok().build();
    }
}