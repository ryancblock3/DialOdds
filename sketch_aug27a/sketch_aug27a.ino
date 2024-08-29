#include <M5Dial.h>
#include <vector>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <time.h>
#include <sys/time.h>
#include <map>
#include "config.h"

const char* ntpServer = "pool.ntp.org";
const long  gmtOffset_sec = -18000;
const int   daylightOffset_sec = 3600;

const int ENCODER_SENSITIVITY = 4;

unsigned long buttonPressStartTime = 0;

#define BACKGROUND_COLOR 0x0000
#define TEXT_COLOR 0xFFFF
#define HIGHLIGHT_COLOR 0xFD20

enum AppState {
    HOME,
    WEEK_SELECTION,
    GAME_INFO,
    TEAM_SELECTION,
    TEAM_SCHEDULE
};

struct Game {
    String home_team;
    String away_team;
    String commence_time;
    float home_odds;
    float away_odds;
    int id;
    int week;
};

struct DateTimeInfo {
    String date;
    String time;
};

struct TeamName {
    String city;
    String name;
};

struct TeamColors {
    uint16_t primary;
    uint16_t secondary;
};

std::map<String, TeamColors> teamColorMap = {
    {"Arizona Cardinals", {0xB0B7, 0xFFFF}},
    {"Atlanta Falcons", {0xB8E3, 0x0000}},
    {"Baltimore Ravens", {0x6B5D, 0x0000}},
    {"Buffalo Bills", {0x039F, 0xF800}},
    {"Carolina Panthers", {0x0390, 0x0000}},
    {"Chicago Bears", {0x051D, 0xFE60}},
    {"Cincinnati Bengals", {0xFE40, 0x0000}},
    {"Cleveland Browns", {0xFD20, 0x6B4D}},
    {"Dallas Cowboys", {0x039F, 0xC618}},
    {"Denver Broncos", {0xFB40, 0x039F}},
    {"Detroit Lions", {0x049F, 0xC618}},
    {"Green Bay Packers", {0x04E0, 0xFFE0}},
    {"Houston Texans", {0x0011, 0xF800}},
    {"Indianapolis Colts", {0x039F, 0xFFFF}},
    {"Jacksonville Jaguars", {0x0390, 0x0000}},
    {"Kansas City Chiefs", {0xF800, 0xFFE0}},
    {"Las Vegas Raiders", {0xC618, 0xC618}},
    {"Los Angeles Chargers", {0x049F, 0xFFE0}},
    {"Los Angeles Rams", {0x049F, 0xFFE0}},
    {"Miami Dolphins", {0x05FF, 0xFE60}},
    {"Minnesota Vikings", {0x6B5D, 0xFFE0}},
    {"New England Patriots", {0x0011, 0xF800}},
    {"New Orleans Saints", {0xC618, 0xC618}},
    {"New York Giants", {0x039F, 0xF800}},
    {"New York Jets", {0x04E0, 0xFFFF}},
    {"Philadelphia Eagles", {0x049F, 0xC618}},
    {"Pittsburgh Steelers", {0xFFE0, 0xFFE0}},
    {"San Francisco 49ers", {0xB8E3, 0xC618}},
    {"Seattle Seahawks", {0x049F, 0x04E0}},
    {"Tampa Bay Buccaneers", {0xB8E3, 0xC618}},
    {"Tennessee Titans", {0x049F, 0xF800}},
    {"Washington Commanders", {0x8800, 0xFFE0}}
};

std::map<String, TeamName> teamNameMap = {
    {"Arizona Cardinals", {"Arizona", "Cardinals"}},
    {"Atlanta Falcons", {"Atlanta", "Falcons"}},
    {"Baltimore Ravens", {"Baltimore", "Ravens"}},
    {"Buffalo Bills", {"Buffalo", "Bills"}},
    {"Carolina Panthers", {"Carolina", "Panthers"}},
    {"Chicago Bears", {"Chicago", "Bears"}},
    {"Cincinnati Bengals", {"Cincinnati", "Bengals"}},
    {"Cleveland Browns", {"Cleveland", "Browns"}},
    {"Dallas Cowboys", {"Dallas", "Cowboys"}},
    {"Denver Broncos", {"Denver", "Broncos"}},
    {"Detroit Lions", {"Detroit", "Lions"}},
    {"Green Bay Packers", {"Green Bay", "Packers"}},
    {"Houston Texans", {"Houston", "Texans"}},
    {"Indianapolis Colts", {"Indianapolis", "Colts"}},
    {"Jacksonville Jaguars", {"Jacksonville", "Jaguars"}},
    {"Kansas City Chiefs", {"Kansas City", "Chiefs"}},
    {"Las Vegas Raiders", {"Las Vegas", "Raiders"}},
    {"Los Angeles Chargers", {"Los Angeles", "Chargers"}},
    {"Los Angeles Rams", {"Los Angeles", "Rams"}},
    {"Miami Dolphins", {"Miami", "Dolphins"}},
    {"Minnesota Vikings", {"Minnesota", "Vikings"}},
    {"New England Patriots", {"New England", "Patriots"}},
    {"New Orleans Saints", {"New Orleans", "Saints"}},
    {"New York Giants", {"New York", "Giants"}},
    {"New York Jets", {"New York", "Jets"}},
    {"Philadelphia Eagles", {"Philadelphia", "Eagles"}},
    {"Pittsburgh Steelers", {"Pittsburgh", "Steelers"}},
    {"San Francisco 49ers", {"San Francisco", "49ers"}},
    {"Seattle Seahawks", {"Seattle", "Seahawks"}},
    {"Tampa Bay Buccaneers", {"Tampa Bay", "Buccaneers"}},
    {"Tennessee Titans", {"Tennessee", "Titans"}},
    {"Washington Commanders", {"Washington", "Commanders"}}
};

std::vector<int> weeks;
std::vector<Game> games;
std::vector<String> teams;
int currentIndex = 0;
AppState currentState = HOME;

long oldPosition = 0;

void setup() {
    auto cfg = M5.config();
    M5Dial.begin(cfg, true, false);
    
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.fillScreen(BACKGROUND_COLOR);
    
    Serial.begin(115200);
    WiFi.begin(ssid, password);

    while (WiFi.status() != WL_CONNECTED) {
        delay(1000);
        Serial.println("Connecting to WiFi...");
    }
    Serial.println("Connected to WiFi");

    configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);

    fetchWeeks();
    fetchTeams();
    displayMenu();
}

void loop() {
    M5Dial.update();
    
    long newPosition = M5Dial.Encoder.read();
    long positionDifference = newPosition - oldPosition;
    
    if (abs(positionDifference) >= ENCODER_SENSITIVITY) {
        int direction = (positionDifference > 0) ? 1 : -1;
        handleDialRotation(direction);
        oldPosition = newPosition;
    }

    if (M5Dial.BtnA.isPressed()) {
        if (buttonPressStartTime == 0) {
            buttonPressStartTime = millis();
        }
    } else if (M5Dial.BtnA.wasReleased()) {
        unsigned long pressDuration = millis() - buttonPressStartTime;
        handleButtonPress(pressDuration);
        buttonPressStartTime = 0;
    }

    delay(10);
}

void fetchWeeks() {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi not connected");
        return;
    }

    HTTPClient http;
    String url = String(serverUrl) + "/api/nfl/weeks";
    http.begin(url);
    int httpResponseCode = http.GET();

    if (httpResponseCode == 200) {
        String payload = http.getString();
        DynamicJsonDocument doc(1024);
        DeserializationError error = deserializeJson(doc, payload);

        if (error) {
            Serial.print("deserializeJson() failed: ");
            Serial.println(error.c_str());
            return;
        }

        weeks.clear();
        JsonArray weeksArray = doc.as<JsonArray>();
        for (JsonVariant v : weeksArray) {
            weeks.push_back(v.as<int>());
        }
    } else {
        Serial.print("Error code: ");
        Serial.println(httpResponseCode);
    }

    http.end();
    Serial.print("Fetched weeks: ");
    Serial.println(weeks.size());
}

void fetchGamesForWeek(int week) {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi not connected");
        return;
    }

    HTTPClient http;
    String url = String(serverUrl) + "/api/nfl/games/" + String(week);
    http.begin(url);
    int httpResponseCode = http.GET();

    if (httpResponseCode == 200) {
        String payload = http.getString();
        DynamicJsonDocument doc(16384);
        DeserializationError error = deserializeJson(doc, payload);

        if (error) {
            Serial.print("deserializeJson() failed: ");
            Serial.println(error.c_str());
            return;
        }

        games.clear();
        JsonArray gamesArray = doc.as<JsonArray>();
        for (JsonObject gameObj : gamesArray) {
            Game game;
            game.home_team = gameObj["home_team"].as<String>();
            game.away_team = gameObj["away_team"].as<String>();
            game.commence_time = gameObj["commence_time"].as<String>();
            game.home_odds = gameObj["home_odds"].as<float>();
            game.away_odds = gameObj["away_odds"].as<float>();
            game.id = gameObj["id"].as<int>();
            game.week = week;
            games.push_back(game);
        }
    } else {
        Serial.print("Error code: ");
        Serial.println(httpResponseCode);
    }

    http.end();
    Serial.print("Fetched games for week ");
    Serial.print(week);
    Serial.print(": ");
    Serial.println(games.size());
}

void fetchTeams() {
    teams.clear();
    for (const auto& team : teamColorMap) {
        teams.push_back(team.first);
    }
    Serial.print("Fetched teams: ");
    Serial.println(teams.size());
}

void fetchTeamSchedule(const String& team) {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi not connected");
        return;
    }

    HTTPClient http;
    String formattedTeam = team;
    formattedTeam.replace(" ", "");  // Replace spaces with nothing
    String url = String(serverUrl) + "/api/nfl/schedule/" + formattedTeam;
    http.begin(url);
    int httpResponseCode = http.GET();

    if (httpResponseCode == 200) {
        String payload = http.getString();
        DynamicJsonDocument doc(16384);
        DeserializationError error = deserializeJson(doc, payload);

        if (error) {
            Serial.print("deserializeJson() failed: ");
            Serial.println(error.c_str());
            return;
        }

        games.clear();
        JsonArray gamesArray = doc.as<JsonArray>();
        for (JsonObject gameObj : gamesArray) {
            Game game;
            game.home_team = gameObj["home_team"].as<String>();
            game.away_team = gameObj["away_team"].as<String>();
            game.commence_time = gameObj["commence_time"].as<String>();
            game.home_odds = gameObj["home_odds"].as<float>();
            game.away_odds = gameObj["away_odds"].as<float>();
            game.id = gameObj["id"].as<int>();
            game.week = gameObj["nfl_week"].as<int>();
            games.push_back(game);
        }
    } else {
        Serial.print("Error code: ");
        Serial.println(httpResponseCode);
    }

    http.end();
    Serial.print("Fetched games for team ");
    Serial.print(team);
    Serial.print(": ");
    Serial.println(games.size());
}

void handleDialRotation(int direction) {
    switch (currentState) {
        case HOME:
            currentIndex = (currentIndex + direction + 2) % 2;
            break;
        case WEEK_SELECTION:
            currentIndex = (currentIndex + direction + weeks.size()) % weeks.size();
            break;
        case GAME_INFO:
        case TEAM_SCHEDULE:
            currentIndex = (currentIndex + direction + games.size()) % games.size();
            break;
        case TEAM_SELECTION:
            currentIndex = (currentIndex + direction + teams.size()) % teams.size();
            break;
    }
    displayMenu();
}

void handleButtonPress(unsigned long pressDuration) {
    bool isLongPress = pressDuration > 1000;  // Long press if held for more than 1 second

    switch (currentState) {
        case HOME:
            if (currentIndex == 0) {
                currentState = WEEK_SELECTION;
            } else {
                currentState = TEAM_SELECTION;
            }
            currentIndex = 0;
            break;
        case WEEK_SELECTION:
            if (isLongPress) {
                currentState = HOME;
                currentIndex = 0;
            } else {
                fetchGamesForWeek(weeks[currentIndex]);
                currentState = GAME_INFO;
                currentIndex = 0;
            }
            break;
        case GAME_INFO:
            currentState = WEEK_SELECTION;
            currentIndex = 0;
            break;
        case TEAM_SELECTION:
            if (isLongPress) {
                currentState = HOME;
                currentIndex = 0;
            } else {
                fetchTeamSchedule(teams[currentIndex]);
                currentState = TEAM_SCHEDULE;
                currentIndex = 0;
            }
            break;
        case TEAM_SCHEDULE:
            currentState = TEAM_SELECTION;
            currentIndex = 0;
            break;
    }
    displayMenu();
}

DateTimeInfo formatDateTime(const String& isoTime) {
    struct tm tm;
    char dateBuffer[32];
    char timeBuffer[32];
    
    if (sscanf(isoTime.c_str(), "%d-%d-%dT%d:%d:%d", &tm.tm_year, &tm.tm_mon, &tm.tm_mday, &tm.tm_hour, &tm.tm_min, &tm.tm_sec) == 6) {
        tm.tm_year -= 1900;
        tm.tm_mon -= 1;
        
        time_t t = mktime(&tm);
        
        bool isDST = false;
        time_t now = time(nullptr);
        struct tm *localTime = localtime(&now);
        isDST = localTime->tm_isdst > 0;
        
        t -= isDST ? 4 * 3600 : 5 * 3600;
        
        struct tm *adjustedTime = localtime(&t);
        
        strftime(dateBuffer, sizeof(dateBuffer), "%b %d, %Y", adjustedTime);
        strftime(timeBuffer, sizeof(timeBuffer), "%I:%M %p", adjustedTime);

        return {String(dateBuffer), String(timeBuffer)};
    }
    
    return {isoTime, ""};
}

String formatOdds(float odds) {
    if (odds >= 2.0) {
        int americanOdds = round((odds - 1) * 100);
        return "+" + String(americanOdds);
    } else if (odds < 2.0 && odds > 0) {
        int americanOdds = round(-100 / (odds - 1));
        return String(americanOdds);
    }
    return "N/A";
}

void displayMenu() {
    switch (currentState) {
        case HOME:
            displayHomeScreen();
            break;
        case WEEK_SELECTION:
            displayWeekSelection();
            break;
        case GAME_INFO:
            displayGameInfo();
            break;
        case TEAM_SELECTION:
            displayTeamSelection();
            break;
        case TEAM_SCHEDULE:
            displayTeamSchedule();
            break;
    }
}

void displayHomeScreen() {
    M5Dial.Display.fillScreen(BACKGROUND_COLOR);
    
    M5Dial.Display.setTextDatum(MC_DATUM);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    
    M5Dial.Display.setTextFont(2);
    M5Dial.Display.drawString("NFL Schedule", 120, 60);

    M5Dial.Display.setTextFont(4);
    M5Dial.Display.setTextColor(HIGHLIGHT_COLOR);
    M5Dial.Display.drawString(currentIndex == 0 ? "Weekly" : "By Team", 120, 120);

    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.drawString("Press to select", 120, 200);
}

void displayWeekSelection() {
    M5Dial.Display.fillScreen(BACKGROUND_COLOR);
    
    int radius = 100;
    int centerX = 120;
    int centerY = 120;
    float progress = (float)currentIndex / (weeks.size() - 1);
    
    int startAngle = 0;
    int endAngle = 360 * progress;
    M5Dial.Display.drawArc(centerX, centerY, radius, radius - 10, startAngle, endAngle, HIGHLIGHT_COLOR);

    M5Dial.Display.setTextDatum(MC_DATUM);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    
    M5Dial.Display.setTextFont(2);
    M5Dial.Display.drawString("Select NFL Week", 120, 60);

    if (currentIndex < weeks.size()) {
        M5Dial.Display.setTextFont(6);
        M5Dial.Display.setTextColor(HIGHLIGHT_COLOR);
        M5Dial.Display.drawString(String(weeks[currentIndex]), 120, 120);
    }

    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.drawString("Press to select", 120, 180);
    M5Dial.Display.drawString("Long press for home", 120, 200);
}

void displayGameInfo() {
    if (games.empty()) {
        M5Dial.Display.fillScreen(BACKGROUND_COLOR);
        M5Dial.Display.setTextDatum(MC_DATUM);
        M5Dial.Display.setTextFont(2);
        M5Dial.Display.setTextColor(TEXT_COLOR);
        M5Dial.Display.drawString("No games for", 120, 100);
        M5Dial.Display.drawString("Week " + String(weeks[currentIndex]), 120, 120);
        return;
    }

    Game &game = games[currentIndex];

    M5Dial.Display.fillScreen(BACKGROUND_COLOR);

    M5Dial.Display.setTextDatum(TC_DATUM);
    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(HIGHLIGHT_COLOR);
    M5Dial.Display.drawString("Week " + String(weeks[currentIndex]), 120, 10);

    M5Dial.Display.setTextDatum(MC_DATUM);
    M5Dial.Display.setTextFont(2);
    
    uint16_t awayTeamColor = teamColorMap[game.away_team].primary;
    M5Dial.Display.setTextColor(awayTeamColor);
    M5Dial.Display.drawString(game.away_team, 120, 40);
    
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.drawString("@", 120, 60);
    
    uint16_t homeTeamColor = teamColorMap[game.home_team].primary;
    M5Dial.Display.setTextColor(homeTeamColor);
    M5Dial.Display.drawString(game.home_team, 120, 80);

    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    DateTimeInfo dateTime = formatDateTime(game.commence_time);
    M5Dial.Display.drawString(dateTime.date, 120, 110);
    M5Dial.Display.drawString(dateTime.time, 120, 130);

    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(HIGHLIGHT_COLOR);
    M5Dial.Display.drawString(formatOdds(game.away_odds) + " / " + formatOdds(game.home_odds), 120, 160);

    M5Dial.Display.setTextFont(1);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.drawString("Press to go back", 120, 220);

    float angle = (float)currentIndex / games.size() * 360;
    int x = 120 + cos(angle * PI / 180) * 110;
    int y = 120 + sin(angle * PI / 180) * 110;
    M5Dial.Display.fillCircle(x, y, 5, HIGHLIGHT_COLOR);
}

void displayTeamSelection() {
    M5Dial.Display.fillScreen(BACKGROUND_COLOR);
    
    int radius = 100;
    int centerX = 120;
    int centerY = 120;
    float progress = (float)currentIndex / (teams.size() - 1);
    
    int startAngle = 0;
    int endAngle = 360 * progress;
    M5Dial.Display.drawArc(centerX, centerY, radius, radius - 10, startAngle, endAngle, HIGHLIGHT_COLOR);

    M5Dial.Display.setTextDatum(MC_DATUM);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    
    M5Dial.Display.setTextFont(2);
    M5Dial.Display.drawString("Select NFL Team", 120, 40);

    if (currentIndex < teams.size()) {
        String currentTeam = teams[currentIndex];
        uint16_t teamColor = teamColorMap[currentTeam].primary;
        
        M5Dial.Display.setTextFont(4);
        uint16_t textColor = (teamColor == 0x0000) ? TEXT_COLOR : teamColorMap[currentTeam].primary;
        M5Dial.Display.setTextColor(textColor);

        // Use the new teamNameMap to get the city and team name
        auto teamNameIt = teamNameMap.find(currentTeam);
        if (teamNameIt != teamNameMap.end()) {
            const TeamName& teamName = teamNameIt->second;

            // Display city name
            M5Dial.Display.drawString(teamName.city, 120, 90);
            
            // Display team name
            M5Dial.Display.drawString(teamName.name, 120, 130);
        } else {
            // Fallback if team is not found in the map
            M5Dial.Display.drawString(currentTeam, 120, 110);
        }
    }

    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.drawString("Press to select", 120, 180);
    M5Dial.Display.drawString("Long press for home", 120, 200);
}

void displayTeamSchedule() {
    if (games.empty()) {
        M5Dial.Display.fillScreen(BACKGROUND_COLOR);
        M5Dial.Display.setTextDatum(MC_DATUM);
        M5Dial.Display.setTextFont(2);
        M5Dial.Display.setTextColor(TEXT_COLOR);
        M5Dial.Display.drawString("No games for", 120, 100);
        M5Dial.Display.drawString(teams[currentIndex], 120, 120);
        return;
    }

    Game &game = games[currentIndex];

    M5Dial.Display.fillScreen(BACKGROUND_COLOR);

    M5Dial.Display.setTextDatum(TC_DATUM);
    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(HIGHLIGHT_COLOR);
    M5Dial.Display.drawString("Week " + String(game.week), 120, 10);

    M5Dial.Display.setTextDatum(MC_DATUM);
    M5Dial.Display.setTextFont(2);
    
    uint16_t awayTeamColor = teamColorMap[game.away_team].primary;
    M5Dial.Display.setTextColor(awayTeamColor);
    M5Dial.Display.drawString(game.away_team, 120, 40);
    
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.drawString("@", 120, 60);
    
    uint16_t homeTeamColor = teamColorMap[game.home_team].primary;
    M5Dial.Display.setTextColor(homeTeamColor);
    M5Dial.Display.drawString(game.home_team, 120, 80);

    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    DateTimeInfo dateTime = formatDateTime(game.commence_time);
    M5Dial.Display.drawString(dateTime.date, 120, 110);
    M5Dial.Display.drawString(dateTime.time, 120, 130);

    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(HIGHLIGHT_COLOR);
    M5Dial.Display.drawString(formatOdds(game.away_odds) + " / " + formatOdds(game.home_odds), 120, 160);

    M5Dial.Display.setTextFont(1);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.drawString("Press to go back", 120, 220);

    float angle = (float)currentIndex / games.size() * 360;
    int x = 120 + cos(angle * PI / 180) * 110;
    int y = 120 + sin(angle * PI / 180) * 110;
    M5Dial.Display.fillCircle(x, y, 5, HIGHLIGHT_COLOR);
}