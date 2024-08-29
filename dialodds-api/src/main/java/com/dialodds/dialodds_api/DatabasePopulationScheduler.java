package com.dialodds.dialodds_api;

import com.dialodds.dialodds_api.service.OddsPopulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatabasePopulationScheduler {

    @Autowired
    private OddsPopulationService oddsPopulationService;

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void scheduleDatabasePopulation() {
        oddsPopulationService.populateDatabase();
    }
}
