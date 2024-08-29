package com.dialodds.dialodds_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OddsPopulationService {

    private static final Logger logger = LoggerFactory.getLogger(OddsPopulationService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.bookmaker}")
    private String bookmakerKey;

    @Value("${api.targeted-sports}")
    private List<String> targetedSports;

    @Scheduled(cron = "${api.update.cron}")
    @Transactional
    public void populateDatabase() {
        logger.info("Starting database population");
        for (String sport : targetedSports) {
            String[] sportInfo = sport.split(",");
            String sportKey = sportInfo[0];
            String marketType = sportInfo[1];
            
            logger.info("Fetching odds for {}", sportKey);
            List<Map<String, Object>> data = fetchOdds(sportKey, marketType);
            
            if (data.isEmpty()) {
                logger.warn("No data available for {}", sportKey);
                continue;
            }
            
            int sportId = insertSport(sportKey, (String) data.get(0).get("sport_title"));
            
            if ("h2h".equals(marketType)) {
                processH2hMarket(sportId, data);
            } else if ("outrights".equals(marketType)) {
                processOutrightMarket(sportId, data);
            }
            
            logger.info("Successfully updated odds for {}", sportKey);
            
            try {
                Thread.sleep(1000); // Be nice to the API
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted", e);
            }
        }
        
        logger.info("Database update completed!");
    }

    private List<Map<String, Object>> fetchOdds(String sportKey, String market) {
        String url = apiUrl + "/" + sportKey + "/odds";
        Map<String, String> params = new HashMap<>();
        params.put("apiKey", apiKey);
        params.put("regions", "us");
        params.put("markets", market);
        params.put("oddsFormat", "decimal");
        params.put("bookmakers", bookmakerKey);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {},
            params
        );

        return response.getBody();
    }

    private int insertSport(String sportKey, String sportTitle) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO sports (key, name) VALUES (?, ?) " +
            "ON CONFLICT (key) DO UPDATE SET name = EXCLUDED.name " +
            "RETURNING id",
            Integer.class,
            sportKey, sportTitle
        );
    }

    private void processH2hMarket(int sportId, List<Map<String, Object>> data) {
        for (Map<String, Object> event : data) {
            int gameId = insertGame(sportId, event);
            List<Map<String, Object>> bookmakers = getTypeSafeList(event.get("bookmakers"));
            for (Map<String, Object> bookmaker : bookmakers) {
                if (bookmakerKey.equals(bookmaker.get("key"))) {
                    List<Map<String, Object>> markets = getTypeSafeList(bookmaker.get("markets"));
                    for (Map<String, Object> market : markets) {
                        if ("h2h".equals(market.get("key"))) {
                            List<Map<String, Object>> outcomes = getTypeSafeList(market.get("outcomes"));
                            double homeOdds = outcomes.stream()
                                .filter(o -> o.get("name").equals(event.get("home_team")))
                                .findFirst()
                                .map(o -> (Double) o.get("price"))
                                .orElse(0.0);
                            double awayOdds = outcomes.stream()
                                .filter(o -> o.get("name").equals(event.get("away_team")))
                                .findFirst()
                                .map(o -> (Double) o.get("price"))
                                .orElse(0.0);
                            insertOdds(gameId, "h2h", homeOdds, awayOdds);
                        }
                    }
                    break; // We only need specified bookmaker odds
                }
            }
        }
    }

    private int insertGame(int sportId, Map<String, Object> event) {
        LocalDateTime commenceTime = LocalDateTime.parse((String) event.get("commence_time"), DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime easternTime = commenceTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("America/New_York")).toLocalDateTime();
        int nflWeek = getNflWeek(easternTime);

        return jdbcTemplate.queryForObject(
            "INSERT INTO games (sport_id, home_team, away_team, commence_time, nfl_week) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (sport_id, home_team, away_team, commence_time) " +
            "DO UPDATE SET updated_at = CURRENT_TIMESTAMP, nfl_week = EXCLUDED.nfl_week " +
            "RETURNING id",
            Integer.class,
            sportId, event.get("home_team"), event.get("away_team"), easternTime, nflWeek
        );
    }

    private void insertOdds(int gameId, String marketType, double homeOdds, double awayOdds) {
        jdbcTemplate.update(
            "INSERT INTO odds (game_id, market_type, home_odds, away_odds) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (game_id, market_type) " +
            "DO UPDATE SET home_odds = EXCLUDED.home_odds, away_odds = EXCLUDED.away_odds, updated_at = CURRENT_TIMESTAMP",
            gameId, marketType, homeOdds, awayOdds
        );
    }

    private void processOutrightMarket(int sportId, List<Map<String, Object>> data) {
        for (Map<String, Object> event : data) {
            List<Map<String, Object>> bookmakers = getTypeSafeList(event.get("bookmakers"));
            for (Map<String, Object> bookmaker : bookmakers) {
                if (bookmakerKey.equals(bookmaker.get("key"))) {
                    List<Map<String, Object>> markets = getTypeSafeList(bookmaker.get("markets"));
                    for (Map<String, Object> market : markets) {
                        if ("outrights".equals(market.get("key"))) {
                            List<Map<String, Object>> outcomes = getTypeSafeList(market.get("outcomes"));
                            for (Map<String, Object> outcome : outcomes) {
                                insertOutrightOdds(sportId, (String) outcome.get("name"), (Double) outcome.get("price"));
                            }
                        }
                    }
                    break; // We only need specified bookmaker odds
                }
            }
        }
    }

    private void insertOutrightOdds(int sportId, String outcomeName, double price) {
        jdbcTemplate.update(
            "INSERT INTO outright_odds (sport_id, outcome_name, price) " +
            "VALUES (?, ?, ?) " +
            "ON CONFLICT (sport_id, outcome_name) " +
            "DO UPDATE SET price = EXCLUDED.price, updated_at = CURRENT_TIMESTAMP",
            sportId, outcomeName, price
        );
    }

    private int getNflWeek(LocalDateTime gameDate) {
        LocalDateTime seasonStart = getNflSeasonStart(gameDate.getYear());
        long daysSinceStart = gameDate.toLocalDate().toEpochDay() - seasonStart.toLocalDate().toEpochDay();
        return Math.max(1, (int) (daysSinceStart / 7) + 1);
    }

    private LocalDateTime getNflSeasonStart(int year) {
        if (year == 2024) {
            return LocalDateTime.of(2024, 9, 5, 0, 0);
        }
        return LocalDateTime.of(year, 9, 5, 0, 0);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getTypeSafeList(Object obj) {
        if (obj instanceof List<?>) {
            return (List<T>) obj;
        }
        return new ArrayList<>();
    }
}