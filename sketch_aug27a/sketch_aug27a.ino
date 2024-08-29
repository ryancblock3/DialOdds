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

#define BACKGROUND_COLOR 0x0000
#define TEXT_COLOR 0xFFFF
#define HIGHLIGHT_COLOR 0xFD20

struct Game {
    String home_team;
    String away_team;
    String commence_time;
    float home_odds;
    float away_odds;
    int id;
};

struct DateTimeInfo {
    String date;
    String time;
};

struct TeamColors {
    uint16_t primary;
    uint16_t secondary;
};

std::map<String, TeamColors> teamColorMap = {
    {"Arizona Cardinals", {0xB0B7, 0xFFFF}},    // Red, White
    {"Atlanta Falcons", {0xB8E3, 0x0000}},      // Red, Black
    {"Baltimore Ravens", {0x6B5D, 0x0000}},     // Purple, Black
    {"Buffalo Bills", {0x039F, 0xF800}},        // Blue, Red
    {"Carolina Panthers", {0x0390, 0x0000}},    // Blue, Black
    {"Chicago Bears", {0x051D, 0xFE60}},        // Navy, Orange
    {"Cincinnati Bengals", {0xFE40, 0x0000}},   // Orange, Black
    {"Cleveland Browns", {0xFD20, 0x6B4D}},     // Orange, Brown
    {"Dallas Cowboys", {0x039F, 0xC618}},       // Blue, Silver
    {"Denver Broncos", {0xFB40, 0x039F}},       // Orange, Blue
    {"Detroit Lions", {0x049F, 0xC618}},        // Blue, Silver
    {"Green Bay Packers", {0x04E0, 0xFFE0}},    // Green, Yellow
    {"Houston Texans", {0x0011, 0xF800}},       // Navy, Red
    {"Indianapolis Colts", {0x039F, 0xFFFF}},   // Blue, White
    {"Jacksonville Jaguars", {0x0390, 0x0000}}, // Teal, Black
    {"Kansas City Chiefs", {0xF800, 0xFFE0}},   // Red, Yellow
    {"Las Vegas Raiders", {0xC618, 0xC618}},    // Silver, Silver
    {"Los Angeles Chargers", {0x049F, 0xFFE0}}, // Blue, Yellow
    {"Los Angeles Rams", {0x049F, 0xFFE0}},     // Blue, Yellow
    {"Miami Dolphins", {0x05FF, 0xFE60}},       // Teal, Orange
    {"Minnesota Vikings", {0x6B5D, 0xFFE0}},    // Purple, Yellow
    {"New England Patriots", {0x0011, 0xF800}}, // Navy, Red
    {"New Orleans Saints", {0xC618, 0xC618}},   // Gold, Gold
    {"New York Giants", {0x039F, 0xF800}},      // Blue, Red
    {"New York Jets", {0x04E0, 0xFFFF}},        // Green, White
    {"Philadelphia Eagles", {0x049F, 0xC618}},  // Green, Silver
    {"Pittsburgh Steelers", {0xFFE0, 0xFFE0}},  // Yellow, Yellow
    {"San Francisco 49ers", {0xB8E3, 0xC618}},  // Red, Gold
    {"Seattle Seahawks", {0x049F, 0x04E0}},     // Blue, Green
    {"Tampa Bay Buccaneers", {0xB8E3, 0xC618}}, // Red, Pewter
    {"Tennessee Titans", {0x049F, 0xF800}},     // Blue, Red
    {"Washington Commanders", {0x8800, 0xFFE0}} // Burgundy, Gold
};

std::vector<int> weeks;
std::vector<Game> games;
int currentWeekIndex = 0;
int currentGameIndex = 0;
bool inWeekSelection = true;

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

    if (M5Dial.BtnA.wasPressed()) {
        handleButtonPress();
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

void handleDialRotation(int direction) {
    if (inWeekSelection) {
        currentWeekIndex = (currentWeekIndex + direction + weeks.size()) % weeks.size();
    } else {
        currentGameIndex = (currentGameIndex + direction + games.size()) % games.size();
    }
    displayMenu();
}

void handleButtonPress() {
    if (inWeekSelection) {
        fetchGamesForWeek(weeks[currentWeekIndex]);
        inWeekSelection = false;
        currentGameIndex = 0;
    } else {
        inWeekSelection = true;
    }
    displayMenu();
}

DateTimeInfo formatDateTime(const String& isoTime) {
    struct tm tm;
    char dateBuffer[32];
    char timeBuffer[32];
    
    // Parse ISO 8601 format
    if (sscanf(isoTime.c_str(), "%d-%d-%dT%d:%d:%d", &tm.tm_year, &tm.tm_mon, &tm.tm_mday, &tm.tm_hour, &tm.tm_min, &tm.tm_sec) == 6) {
        tm.tm_year -= 1900;  // Adjust year
        tm.tm_mon -= 1;      // Adjust month (0-11)
        
        time_t t = mktime(&tm);  // Convert to time_t
        
        // Check if DST is in effect
        bool isDST = false;
        time_t now = time(nullptr);
        struct tm *localTime = localtime(&now);
        isDST = localTime->tm_isdst > 0;
        
        // Adjust for EDT (UTC-4) during DST, or EST (UTC-5) otherwise
        t -= isDST ? 4 * 3600 : 5 * 3600;
        
        // Convert back to tm structure
        struct tm *adjustedTime = localtime(&t);
        
        strftime(dateBuffer, sizeof(dateBuffer), "%b %d, %Y", adjustedTime);
        strftime(timeBuffer, sizeof(timeBuffer), "%I:%M %p", adjustedTime);

        return {String(dateBuffer), String(timeBuffer)};
    }
    
    return {isoTime, ""};  // Return original string if parsing fails
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
    if (inWeekSelection) {
        displayWeekSelection();
    } else {
        displayGameInfo();
    }
}

void displayWeekSelection() {
    M5Dial.Display.fillScreen(BACKGROUND_COLOR);
    
    int radius = 100;
    int centerX = 120;
    int centerY = 120;
    float progress = (float)currentWeekIndex / (weeks.size() - 1);
    
    // Draw a simple progress arc
    int startAngle = 0;
    int endAngle = 360 * progress;
    M5Dial.Display.drawArc(centerX, centerY, radius, radius - 10, startAngle, endAngle, HIGHLIGHT_COLOR);

    M5Dial.Display.setTextDatum(MC_DATUM);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    
    M5Dial.Display.setTextFont(2);
    M5Dial.Display.drawString("Select NFL Week", 120, 60);

    if (currentWeekIndex < weeks.size()) {
        M5Dial.Display.setTextFont(6);
        M5Dial.Display.setTextColor(HIGHLIGHT_COLOR);
        M5Dial.Display.drawString(String(weeks[currentWeekIndex]), 120, 120);
    }

    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.drawString("Press to select", 120, 200);
}

void displayGameInfo() {
    if (games.empty()) {
        M5Dial.Display.fillScreen(BACKGROUND_COLOR);
        M5Dial.Display.setTextDatum(MC_DATUM);
        M5Dial.Display.setTextFont(2);
        M5Dial.Display.setTextColor(TEXT_COLOR);
        M5Dial.Display.drawString("No games for", 120, 100);
        M5Dial.Display.drawString("Week " + String(weeks[currentWeekIndex]), 120, 120);
        return;
    }

    Game &game = games[currentGameIndex];

    M5Dial.Display.fillScreen(BACKGROUND_COLOR);

    M5Dial.Display.setTextDatum(TC_DATUM);
    M5Dial.Display.setTextFont(2);
    M5Dial.Display.setTextColor(HIGHLIGHT_COLOR);
    M5Dial.Display.drawString("Week " + String(weeks[currentWeekIndex]), 120, 10);

    M5Dial.Display.setTextDatum(MC_DATUM);
    M5Dial.Display.setTextFont(2);
    
    // Use team colors for away team
    uint16_t awayTeamColor = teamColorMap[game.away_team].primary;
    M5Dial.Display.setTextColor(awayTeamColor);
    M5Dial.Display.drawString(game.away_team, 120, 40);
    
    M5Dial.Display.setTextColor(TEXT_COLOR);
    M5Dial.Display.drawString("@", 120, 60);
    
    // Use team colors for home team
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

    // Draw game index indicator
    float angle = (float)currentGameIndex / games.size() * 360;
    int x = 120 + cos(angle * PI / 180) * 110;
    int y = 120 + sin(angle * PI / 180) * 110;
    M5Dial.Display.fillCircle(x, y, 5, HIGHLIGHT_COLOR);
}