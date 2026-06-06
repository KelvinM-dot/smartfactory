package com.jqhc.dataplatform.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class SqliteDataSourceConfig {

    @Bean
    @Primary
    DataSource dataSource(JqhcProperties jqhcProperties) throws Exception {
        Path dbPath = Path.of(jqhcProperties.getSqliteDatabasePath()).toAbsolutePath().normalize();
        Path parent = dbPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:sqlite:file:" + dbPath + "?journal_mode=WAL&busy_timeout=5000");
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setMaximumPoolSize(1);
        return ds;
    }
}
