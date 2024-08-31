# DialOdds

DialOdds is an interactive NFL schedule and odds viewer application for the M5Stack Dial device. It allows users to browse NFL games by week or team, view game details, and check the latest odds.

## Features

- Browse NFL games by week or team
- View detailed game information including teams, date, time, and odds
- User-friendly interface utilizing the M5Stack Dial's rotary encoder and button
- Real-time data fetching from a server API
- Colorful team-based display

## Hardware Requirements

- M5Stack Dial device

## Software Dependencies

- M5Dial library
- WiFi library
- HTTPClient library
- ArduinoJson library
- time.h and sys/time.h for time management

## Setup

1. Install the required libraries in your Arduino IDE.
2. Create a `config.h` file in the same directory as the main sketch with the following content:

```cpp
const char* ssid = "Your_WiFi_SSID";
const char* password = "Your_WiFi_Password";
const char* serverUrl = "Your_API_Server_URL";
```

3. Replace the placeholders with your actual WiFi credentials and API server URL.
4. Upload the sketch to your M5Stack Dial device.

## Usage

1. Power on your M5Stack Dial.
2. The device will connect to WiFi and fetch initial data.
3. Use the rotary encoder to navigate through options.
4. Press the button to select an option or enter a submenu.
5. Long press the button to return to the home screen from week or team selection menus.

## Navigation

- Home Screen: Choose between "Weekly" and "By Team" views
- Week Selection: Browse and select NFL weeks
- Game Info: View details for games in the selected week
- Team Selection: Browse and select NFL teams
- Team Schedule: View all games for the selected team

## Notes

- The application uses a custom color scheme for each NFL team.
- Odds are displayed in American odds format.
- Game times are adjusted for Eastern Time (ET).

## Troubleshooting

- If the device fails to connect to WiFi, check your credentials in the `config.h` file.
- If no games are displayed, ensure your API server is running and accessible.

## Future Improvements

- Add more detailed statistical information for each game
- Implement a favorites system for quick access to preferred teams
- Add support for other sports leagues

## Contributing

Contributions to DialOdds are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the [MIT License](LICENSE).