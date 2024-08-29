# DialOdds Project

DialOdds is a comprehensive project that combines a Spring Boot backend API for NFL game data and odds with an M5Dial application for displaying the information. This README provides an overview of both components and instructions for setting up and running the project.

## Table of Contents
1. [Backend API](#backend-api)
   - [Setup](#backend-setup)
   - [Running the Application](#running-the-backend)
   - [API Endpoints](#api-endpoints)
2. [M5Dial Application](#m5dial-application)
   - [Setup](#m5dial-setup)
   - [Configuration](#m5dial-configuration)
   - [Functionality](#m5dial-functionality)
3. [Project Structure](#project-structure)

## Backend API

The backend API is built using Spring Boot and provides endpoints for retrieving NFL game data and odds.

### Backend Setup

1. Ensure you have Java JDK 11 or later installed.
2. Clone the repository and navigate to the backend directory.
3. Create a `.env` file in the root directory with the following content:
   ```
   JDBC_DATABASE_URL=your_database_url_here
   ```
4. Update the `application.properties` file with your database configuration if necessary.

### Running the Backend

1. Build the project:
   ```
   ./mvnw clean package
   ```
2. Run the application:
   ```
   java -jar target/dialodds-api-0.0.1-SNAPSHOT.jar
   ```

The application will start, and you should see the JDBC_DATABASE_URL printed in the console to verify it's loaded correctly.

### API Endpoints

- `GET /api/nfl/weeks`: Retrieves a list of available NFL weeks.
- `GET /api/nfl/games/{week}`: Retrieves NFL games for a specific week.
- `GET /api/nfl/schedule/{team}`: Retrieves the schedule for a specific NFL team.

You can access the API documentation by opening a web browser and navigating to `http://localhost:8080` when the application is running.

## M5Dial Application

The M5Dial application displays NFL game information and odds on an M5Dial device.

### M5Dial Setup

1. Install the Arduino IDE and set it up for ESP32 development.
2. Install the following libraries:
   - M5Dial
   - ArduinoJson
3. Open the `dialodds.ino` file in the Arduino IDE.

### M5Dial Configuration

1. Copy the `config_template.h` file and rename it to `config.h`.
2. Edit `config.h` and update the following variables:
   ```cpp
   const char *ssid = "YOUR_WIFI_SSID";
   const char *password = "YOUR_WIFI_PASSWORD";
   const char *serverUrl = "YOUR_SERVER_URL";
   ```

### M5Dial Functionality

The M5Dial application provides the following features:
- Displays NFL weeks for selection
- Shows game information for the selected week
- Displays team names, game time, and odds
- Uses team colors for visual enhancement
- Allows navigation through games using the dial

## Project Structure

```
dialodds/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── dialodds/
│   │       │           └── dialodds_api/
│   │       │               ├── controller/
│   │       │               ├── service/
│   │       │               ├── config/
│   │       │               ├── DialoddsApiApplication.java
│   │       │               └── DatabasePopulationScheduler.java
│   │       └── resources/
│   │           └── application.properties
│   ├── pom.xml
│   └── .env
├── m5dial/
│   ├── dialodds.ino
│   ├── config.h
│   └── config_template.h
└── README.md
```

This project combines a robust backend API with a user-friendly M5Dial interface, providing an innovative way to access and display NFL game information and odds.