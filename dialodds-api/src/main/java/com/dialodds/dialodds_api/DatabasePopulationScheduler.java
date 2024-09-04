package com.dialodds.dialodds_api;

import com.dialodds.dialodds_api.service.OddsPopulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatabasePopulationScheduler {

    @Autowired
    private OddsPopulationService oddsPopulationService;

    // Run every day at 6:00 AM EST
    @Scheduled(cron = "0 0 6 * * *", zone = "America/New_York")
    public void morningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every day at 8:00 PM EST
    @Scheduled(cron = "0 0 20 * * *", zone = "America/New_York")
    public void eveningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Thursday at 6:00 PM EST
    @Scheduled(cron = "0 0 18 * * THU", zone = "America/New_York")
    public void thursdayEveningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Sunday at 11:00 AM EST
    @Scheduled(cron = "0 0 11 * * SUN", zone = "America/New_York")
    public void sundayMorningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Sunday at 2:30 PM EST
    @Scheduled(cron = "0 30 14 * * SUN", zone = "America/New_York")
    public void sundayAfternoonUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Sunday at 6:00 PM EST
    @Scheduled(cron = "0 0 18 * * SUN", zone = "America/New_York")
    public void sundayEveningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Monday at 6:00 PM EST
    @Scheduled(cron = "0 0 18 * * MON", zone = "America/New_York")
    public void mondayEveningUpdate() {
        oddsPopulationService.populateDatabase();
    }
}