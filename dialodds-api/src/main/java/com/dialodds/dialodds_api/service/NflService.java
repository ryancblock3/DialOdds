package com.dialodds.dialodds_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NflService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

}
