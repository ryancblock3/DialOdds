package com.dialodds.dialodds_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class NflService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BetService betService;

    public List<Integer> getNflWeeks() {
        String sql = "SELECT DISTINCT nfl_week " +
                     "FROM games " +
                     "WHERE sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl') " +
                     "AND commence_time > NOW() " +
                     "ORDER BY nfl_week";
        return jdbcTemplate.queryForList(sql, Integer.class);
    }

    public List<Map<String, Object>> getNflGamesByWeek(int week) {
        String sql = "SELECT g.id, g.home_team, g.away_team, g.commence_time, o.home_odds, o.away_odds " +
                     "FROM games g " +
                     "JOIN odds o ON g.id = o.game_id " +
                     "WHERE g.sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl') " +
                     "AND g.nfl_week = ? " +
                     "AND g.commence_time > NOW() " +
                     "ORDER BY g.commence_time";
        return jdbcTemplate.queryForList(sql, week);
    }

    public List<Map<String, Object>> getTeamSchedule(String team) {
        // Remove spaces from the input team name
        String formattedTeam = team.replaceAll("\\s+", "");
        
        String sql = "SELECT g.id, g.home_team, g.away_team, g.commence_time, g.nfl_week, o.home_odds, o.away_odds " +
                     "FROM games g " +
                     "JOIN odds o ON g.id = o.game_id " +
                     "WHERE g.sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl') " +
                     "AND (REPLACE(g.home_team, ' ', '') = ? OR REPLACE(g.away_team, ' ', '') = ?) " +
                     "AND g.commence_time > NOW() " +
                     "ORDER BY g.commence_time";
        return jdbcTemplate.queryForList(sql, formattedTeam, formattedTeam);
    }

    public Map<String, Object> getGameById(int gameId) {
        String sql = "SELECT g.id, g.home_team, g.away_team, g.commence_time, g.nfl_week, o.home_odds, o.away_odds " +
                     "FROM games g " +
                     "JOIN odds o ON g.id = o.game_id " +
                     "WHERE g.id = ? " +
                     "AND g.sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl')";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, gameId);
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public void updateGameResult(int gameId, String winner) {
        if (!winner.equals("home") && !winner.equals("away")) {
            throw new IllegalArgumentException("Winner must be either 'home' or 'away'");
        }

        String sql = "UPDATE games SET result = ? WHERE id = ?";
        int updatedRows = jdbcTemplate.update(sql, winner, gameId);
        
        if (updatedRows == 0) {
            throw new IllegalArgumentException("No game found with id: " + gameId);
        }
        
        // After updating the game result, settle the bets
        betService.settleBets(gameId, winner);
    }

    public List<Map<String, Object>> getGamesWithPendingResults() {
        String sql = "SELECT g.id, g.home_team, g.away_team, g.commence_time, g.nfl_week " +
                     "FROM games g " +
                     "WHERE g.sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl') " +
                     "AND g.result IS NULL " +
                     "AND g.commence_time < NOW() " +
                     "ORDER BY g.commence_time";
        return jdbcTemplate.queryForList(sql);
    }

}
