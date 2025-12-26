package com.paseto.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initData() {
        log.info("Checking if database needs to be initialized...");

        // Create indexes for performance optimization
        log.info("Creating database indexes...");
        executeSqlScript("db/migration/V1__Add_Indexes.sql");

        // Check if banners table has data
        Integer bannerCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM banners", Integer.class);

        // Check if products table has data
        Integer productCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM products", Integer.class);

        if (bannerCount == null || bannerCount == 0) {
            log.info("No banners found, inserting dummy data...");
            executeSqlScript("db/migration/V2__Insert_Dummy_Banners.sql");
        } else {
            log.info("Banners already exist: {} records", bannerCount);
        }

        if (productCount == null || productCount == 0) {
            log.info("No products found, inserting dummy data...");
            executeSqlScript("db/migration/V3__Insert_Dummy_Products.sql");
        } else {
            log.info("Products already exist: {} records", productCount);
        }

        log.info("Database initialization completed successfully!");
    }

    private void executeSqlScript(String scriptPath) {
        try {
            ClassPathResource resource = new ClassPathResource(scriptPath);
            String sql = Files.readString(Paths.get(resource.getURI()));
            jdbcTemplate.execute(sql);
            log.info("Successfully executed SQL script: {}", scriptPath);
        } catch (Exception e) {
            log.error("Failed to execute SQL script: {}", scriptPath, e);
        }
    }
}
