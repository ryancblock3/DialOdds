package com.dialodds.dialodds_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class BetService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public int placeBet(int userId, int seasonId, int gameId, String betType, int amount) {
        // Check if user has enough coins
        String checkCoinsSQL = "SELECT coins FROM user_seasons WHERE user_id = ? AND season_id = ?";
        Integer userCoins = jdbcTemplate.queryForObject(checkCoinsSQL, Integer.class, userId, seasonId);

        if (userCoins == null || userCoins < amount) {
            throw new IllegalArgumentException("Not enough coins to place bet");
        }

        // Place the bet
        String placeBetSQL = "INSERT INTO bets (user_id, season_id, game_id, bet_type, amount) VALUES (?, ?, ?, ?, ?) RETURNING id";
        Integer betId = jdbcTemplate.queryForObject(placeBetSQL, Integer.class, userId, seasonId, gameId, betType, amount);

        // Update user's coins
        String updateCoinsSQL = "UPDATE user_seasons SET coins = coins - ? WHERE user_id = ? AND season_id = ?";
        jdbcTemplate.update(updateCoinsSQL, amount, userId, seasonId);

        return betId != null ? betId : -1;
    }

    public List<Map<String, Object>> getUserBets(int userId, int seasonId) {
        String sql = "SELECT b.id, b.game_id, g.home_team, g.away_team, b.bet_type, b.amount, b.result " +
                     "FROM bets b JOIN games g ON b.game_id = g.id " +
                     "WHERE b.user_id = ? AND b.season_id = ?";
        return jdbcTemplate.queryForList(sql, userId, seasonId);
    }

    public List<Map<String, Object>> getUnsettledBetsForGame(int gameId) {
        String sql = "SELECT b.id, b.user_id, u.username, b.season_id, b.bet_type, b.amount " +
                     "FROM bets b " +
                     "JOIN users u ON b.user_id = u.id " +
                     "WHERE b.game_id = ? AND b.result IS NULL";
        return jdbcTemplate.queryForList(sql, gameId);
    }

    @Transactional
    public void settleBets(int gameId, String winner) {
        // Get all unsettled bets for this game
        List<Map<String, Object>> unsettledBets = getUnsettledBetsForGame(gameId);

        for (Map<String, Object> bet : unsettledBets) {
            int betId = (Integer) bet.get("id");
            int userId = (Integer) bet.get("user_id");
            int seasonId = (Integer) bet.get("season_id");
            String betType = (String) bet.get("bet_type");
            int amount = (Integer) bet.get("amount");

            boolean won = (betType.equals("home") && winner.equals("home")) || (betType.equals("away") && winner.equals("away"));

            // Update bet result
            String updateBetSQL = "UPDATE bets SET result = ? WHERE id = ?";
            jdbcTemplate.update(updateBetSQL, won ? "won" : "lost", betId);

            if (won) {
                // Get odds for the game
                String getOddsSQL = "SELECT home_odds, away_odds FROM odds WHERE game_id = ?";
                Map<String, Object> odds = jdbcTemplate.queryForMap(getOddsSQL, gameId);
                double winningOdds = betType.equals("home") ? (Double) odds.get("home_odds") : (Double) odds.get("away_odds");

                // Calculate winnings
                int winnings = (int) (amount * winningOdds);

                // Update user's coins
                String updateCoinsSQL = "UPDATE user_seasons SET coins = coins + ? WHERE user_id = ? AND season_id = ?";
                jdbcTemplate.update(updateCoinsSQL, winnings, userId, seasonId);
            }
        }
    }
}