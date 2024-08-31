package com.dialodds.dialodds_api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DialoddsApiApplication {

    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.configure().load();
        
        // Set environment variables
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
            System.out.println("Setting env variable: " + entry.getKey() + " = " + entry.getValue());
        });
        
        // Print the JDBC_DATABASE_URL to verify it's loaded correctly
        System.out.println("JDBC_DATABASE_URL: " + System.getProperty("JDBC_DATABASE_URL"));
        
        // Print admin credentials
        System.out.println("ADMIN_USERNAME: " + System.getProperty("ADMIN_USERNAME"));
        System.out.println("ADMIN_PASSWORD: " + System.getProperty("ADMIN_PASSWORD"));
        
        SpringApplication.run(DialoddsApiApplication.class, args);
    }
}