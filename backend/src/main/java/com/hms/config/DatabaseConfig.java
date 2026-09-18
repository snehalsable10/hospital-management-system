package com.hms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
public class DatabaseConfig {

    /**
     * Configure HikariCP connection pooling
     * Improves database performance with connection pooling
     */
    @Bean
    public DataSource dataSource(
            DatabaseProperties dbProps) {
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbProps.getUrl());
        config.setUsername(dbProps.getUsername());
        config.setPassword(dbProps.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        
        // Connection pool settings
        config.setMaximumPoolSize(20);              // Max connections
        config.setMinimumIdle(5);                   // Min idle connections
        config.setConnectionTimeout(30000);         // 30 seconds
        config.setIdleTimeout(600000);              // 10 minutes
        config.setMaxLifetime(1800000);             // 30 minutes
        config.setAutoCommit(true);
        config.setLeakDetectionThreshold(15000);    // 15 seconds
        
        // Performance tuning
        config.addDataSourceProperty("cachePreparedStatements", true);
        config.addDataSourceProperty("preparedStatementCacheSize", 250);
        config.addDataSourceProperty("preparedStatementCacheSqlLimit", 2048);
        
        return new HikariDataSource(config);
    }

    /**
     * Configure JPA/Hibernate settings
     * Optimize ORM performance
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            DatabaseProperties dbProps) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.hms.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Map<String, Object> properties = new HashMap<>();
        
        // Hibernate dialect
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        
        // DDL (Data Definition Language)
        properties.put("hibernate.hbm2ddl.auto", "update");
        
        // Query optimization
        properties.put("hibernate.use_sql_comments", true);
        properties.put("hibernate.jdbc.batch_size", 20);
        properties.put("hibernate.order_inserts", true);
        properties.put("hibernate.order_updates", true);
        properties.put("hibernate.jdbc.fetch_size", 50);
        
        // Connection pool
        properties.put("hibernate.connection.provider_class",
            "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");

        // Defining a custom EntityManagerFactory bypasses Spring Boot's naming-strategy
        // auto-configuration. Without these, implicitly-named columns stay camelCase
        // (billDate) and no longer match the snake_case @Index/@Column names used throughout.
        properties.put("hibernate.physical_naming_strategy",
            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.put("hibernate.implicit_naming_strategy",
            "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");

        // Second-level cache is disabled: caching is handled at the service layer
        // by Spring Cache + Redis, and no entity opts into Hibernate's L2 cache.
        properties.put("hibernate.cache.use_second_level_cache", false);
        properties.put("hibernate.cache.use_query_cache", false);

        // Logging (dev only)
        if (dbProps.isShowSql()) {
            properties.put("hibernate.show_sql", true);
            properties.put("hibernate.format_sql", true);
            properties.put("hibernate.use_sql_comments", true);
        }
        
        em.setJpaPropertyMap(properties);
        return em;
    }

    /**
     * Database configuration properties
     */
    @Configuration
    @ConfigurationProperties(prefix = "spring.datasource")
    public static class DatabaseProperties {
        private String url;
        private String username;
        private String password;
        private boolean showSql;
        
        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public boolean isShowSql() { return showSql; }
        public void setShowSql(boolean showSql) { this.showSql = showSql; }
    }
}