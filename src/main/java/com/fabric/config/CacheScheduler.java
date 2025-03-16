package com.fabric.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheScheduler {
    @CacheEvict(value = {"econtCities", "econtOffices"}, allEntries = true)
    @Scheduled(fixedRate = 30L * 24 * 60 * 60 * 1000)
    public void clearEcontCitiesCache() {
    }
}


