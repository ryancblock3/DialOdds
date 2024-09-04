package com.dialodds.dialodds_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public int createOrUpdateUser(String discordId, String username) {
        String sql = "INSERT INTO users (discord_id, username) VALUES (?, ?) " +
                "ON CONFLICT (discord_id) DO UPDATE SET username = EXCLUDED.username " +
                "RETURNING id";
        return jdbcTemplate.queryForObject(sql, Integer.class, discordId, username);
    }

    public void addUserToSeason(int userId, int seasonId) {
        String sql = "INSERT INTO user_seasons (user_id, season_id, coins) VALUES (?, ?, (SELECT initial_coins FROM seasons WHERE id = ?))";
        jdbcTemplate.update(sql, userId, seasonId, seasonId);
    }

    public List<Map<String, Object>> getUsersBySeason(int seasonId) {
        String sql = "SELECT u.id, u.username, u.discord_id, us.coins " +
                     "FROM users u " +
                     "JOIN user_seasons us ON u.id = us.user_id " +
                     "WHERE us.season_id = ?";
        return jdbcTemplate.queryForList(sql, seasonId);
    }

    public int getUserCoins(int userId, int seasonId) {
        String sql = "SELECT coins FROM user_seasons WHERE user_id = ? AND season_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId, seasonId);
    }

    @Transactional
    public void createUserAndJoinSeason(String discordId, String username, int seasonId) {
        int userId = createOrUpdateUser(discordId, username);
        addUserToSeason(userId, seasonId);
    }
}
