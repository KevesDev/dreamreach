package com.keves.dreamreach;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class DreamreachApplication {

    @Autowired
    private DataSource dataSource;

    public static void main(String[] args) {
        SpringApplication.run(DreamreachApplication.class, args);
    }

    // Force Flyway to execute before Hibernate validation occurs
    @PostConstruct
    public void migrateDatabase() {
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}