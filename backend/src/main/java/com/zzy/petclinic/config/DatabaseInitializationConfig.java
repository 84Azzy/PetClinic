package com.zzy.petclinic.config;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
public class DatabaseInitializationConfig {
  @Bean
  @ConditionalOnProperty(name = "app.database.initialize", havingValue = "true")
  ApplicationRunner initializeDatabase(DataSource dataSource) {
    return args ->
        new ResourceDatabasePopulator(
                new ClassPathResource("db/schema.sql"), new ClassPathResource("db/data.sql"))
            .execute(dataSource);
  }
}
