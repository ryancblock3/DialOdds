package com.dialodds.dialodds_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
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

    @Value("${API_KEY}")
    private String apiKey;

    @Value("${API_BOOKMAKER}")
    private String bookmakerKey;

    @Value("${API_TARGETED_SPORTS}")
    private String targetedSportsString;

    private List<String> targetedSports;

    @PostConstruct
    public void init() {
        targetedSports = Arrays.asList(targetedSportsString.split(","));
        logger.info("Initialized targeted sports: {}", targetedSports);
        logger.info("API Key: {}", apiKey.substring(0, 4) + "..."); // Log only first 4 characters of API key
        logger.info("Bookmaker Key: {}", bookmakerKey);
    }

    @Transactional
    public void populateDatabase() {
        logger.info("Starting database population");
        logger.info("Targeted sports: {}", targetedSports);

        if (targetedSports == null || targetedSports.isEmpty()) {
            logger.error("No targeted sports configured. Check API_TARGETED_SPORTS environment variable.");
            return;
        }

        for (int i = 0; i < targetedSports.size(); i += 2) {
            if (i + 1 >= targetedSports.size()) {
                logger.error("Incomplete sport configuration at index {}. Skipping.", i);
                continue;
            }

            String sportKey = targetedSports.get(i);
            String marketType = targetedSports.get(i + 1);
            
            logger.info("Processing sport: {}, market type: {}", sportKey, marketType);
            
            try {
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
                } else {
                    logger.warn("Unsupported market type: {} for sport: {}", marketType, sportKey);
                }
                
                logger.info("Successfully updated odds for {}", sportKey);
            } catch (Exception e) {
                logger.error("Error processing sport: {}, market type: {}", sportKey, marketType, e);
            }
            
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
        String url = "https://api.the-odds-api.com/v4/sports/" + sportKey + "/odds";
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("apiKey", apiKey)
            .queryParam("regions", "us")
            .queryParam("markets", market)
            .queryParam("oddsFormat", "decimal")
            .queryParam("bookmakers", bookmakerKey);

        logger.info("Fetching odds from URL: {}", builder.toUriString().replaceAll("apiKey=[^&]+", "apiKey=*****"));

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
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
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/New_York"));
        for (Map<String, Object> event : data) {
            LocalDateTime commenceTime = LocalDateTime.parse((String) event.get("commence_time"), DateTimeFormatter.ISO_DATE_TIME);
            if (commenceTime.isBefore(now)) {
                logger.info("Skipping game {} as it has already started", event.get("id"));
                continue;
            }

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
        // Adjust this method based on the actual NFL season start dates
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

    @Transactional
    public String manuallyPopulateDatabase() {
        try {
            logger.info("Starting manual database population");
            populateDatabase();
            return "Database manually updated successfully";
        } catch (Exception e) {
            logger.error("Error during manual database population", e);
            return "Error updating database: " + e.getMessage();
        }
    }
}