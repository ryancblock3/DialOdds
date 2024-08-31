package com.dialodds.dialodds_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@Service
public class SeasonService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int createSeason(int startWeek, int endWeek, int initialCoins) {
        String sql = "INSERT INTO seasons (start_week, end_week, initial_coins, created_at) VALUES (?, ?, ?, ?) RETURNING id";
        return jdbcTemplate.queryForObject(sql, Integer.class, startWeek, endWeek, initialCoins, LocalDateTime.now());
    }

    public List<Map<String, Object>> getAllSeasons() {
        String sql = "SELECT * FROM seasons";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getActiveSeasons() {
        String sql = "SELECT * FROM seasons WHERE end_week >= (SELECT MAX(nfl_week) FROM games WHERE commence_time > NOW())";
        return jdbcTemplate.queryForList(sql);
    }

    public Map<String, Object> getSeasonById(int seasonId) {
        String sql = "SELECT * FROM seasons WHERE id = ?";
        return jdbcTemplate.queryForMap(sql, seasonId);
    }

    @Transactional
    public Map<String, Object> deleteSeason(int seasonId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // First, delete related records in bets
            String deleteBetsSQL = "DELETE FROM bets WHERE season_id = ?";
            int betsDeleted = jdbcTemplate.update(deleteBetsSQL, seasonId);
            
            // Then, delete related records in user_seasons
            String deleteUserSeasonsSQL = "DELETE FROM user_seasons WHERE season_id = ?";
            int userSeasonsDeleted = jdbcTemplate.update(deleteUserSeasonsSQL, seasonId);
            
            // Finally, delete the season
            String deleteSeasonSQL = "DELETE FROM seasons WHERE id = ?";
            int rowsAffected = jdbcTemplate.update(deleteSeasonSQL, seasonId);
            
            if (rowsAffected > 0) {
                result.put("deleted", true);
                result.put("message", String.format("Season successfully deleted. %d bets and %d user season entries were also removed.", betsDeleted, userSeasonsDeleted));
            } else {
                result.put("deleted", false);
                result.put("message", "Season not found or already deleted.");
            }
        } catch (Exception e) {
            result.put("deleted", false);
            result.put("message", "Error deleting season: " + e.getMessage());
        }
        
        return result;
    }
}