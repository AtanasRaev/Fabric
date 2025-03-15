package bg.tshirt.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        CaffeineCache clothingCache = new CaffeineCache("clothing",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .build());

        CaffeineCache clothingQueryCache = new CaffeineCache("clothingQuery",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .build());

        CaffeineCache econtCitiesCache = new CaffeineCache("econtCities",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.DAYS)
                        .maximumSize(1)
                        .build());

        CaffeineCache econtOfficesCache = new CaffeineCache("econtOffices",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.DAYS)
                        .maximumSize(100)
                        .build());

        cacheManager.setCaches(List.of(clothingCache, clothingQueryCache, econtCitiesCache, econtOfficesCache));
        return cacheManager;
    }
}
