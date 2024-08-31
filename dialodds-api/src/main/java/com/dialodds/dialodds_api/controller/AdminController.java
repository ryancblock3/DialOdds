package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.NflService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private NflService nflService;

    @GetMapping("/pending-games")
    public String pendingGames(Model model) {
        model.addAttribute("games", nflService.getGamesWithPendingResults());
        return "pending-games";
    }

    @PostMapping("/update-result")
    public String updateResult(@RequestParam int gameId, @RequestParam String winner) {
        nflService.updateGameResult(gameId, winner);
        return "redirect:/admin/pending-games";
    }
}