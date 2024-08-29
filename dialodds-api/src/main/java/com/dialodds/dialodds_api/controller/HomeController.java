package com.dialodds.dialodds_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = "text/html")
    public String home() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>NFL API Home</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            line-height: 1.6;\n" +
                "            color: #333;\n" +
                "            max-width: 800px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        h1 {\n" +
                "            color: #2c3e50;\n" +
                "        }\n" +
                "        .endpoint {\n" +
                "            background-color: #f4f4f4;\n" +
                "            border: 1px solid #ddd;\n" +
                "            border-radius: 4px;\n" +
                "            padding: 10px;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .method {\n" +
                "            font-weight: bold;\n" +
                "            color: #2980b9;\n" +
                "        }\n" +
                "        .path {\n" +
                "            color: #27ae60;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Welcome to the DialOdds API</h1>\n" +
                "    <p>Here are the available endpoints:</p>\n" +
                "    \n" +
                "    <div class=\"endpoint\">\n" +
                "        <span class=\"method\">GET</span> \n" +
                "        <span class=\"path\">/api/nfl/weeks</span>\n" +
                "        <p>Retrieves a list of available NFL weeks.</p>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"endpoint\">\n" +
                "        <span class=\"method\">GET</span> \n" +
                "        <span class=\"path\">/api/nfl/games/{week}</span>\n" +
                "        <p>Retrieves NFL games for a specific week. Replace {week} with the desired week number.</p>\n"
                +
                "    </div>\n" +
                // Add this to the HTML string in the home() method
                "    <div class=\"endpoint\">\n" +
                "        <span class=\"method\">GET</span> \n" +
                "        <span class=\"path\">/api/nfl/schedule/{team}</span>\n" +
                "        <p>Retrieves the schedule for a specific NFL team. Replace {team} with the team name.</p>\n" +
                "    </div>\n" +
                "    \n" +
                "    <p>To use these endpoints, append them to the base URL: <code>http://localhost:8080</code></p>\n" +
                "</body>\n" +
                "</html>";
    }
}