package com.civicconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CivicConnect — Digital Municipal Service Management System
 *
 * <p>Spring Boot application entry point. All beans in the
 * {@code com.civicconnect} package and its sub-packages are
 * discovered automatically via {@code @SpringBootApplication}.
 *
 * <p>Startup sequence:
 * <ol>
 *   <li>Spring context loads and wires all beans.</li>
 *   <li>HikariCP opens a connection pool to Supabase PostgreSQL.</li>
 *   <li>Flyway runs pending migrations (V1, V2, …) in order.</li>
 *   <li>Hibernate validates entity mappings against the live schema.</li>
 *   <li>Embedded Tomcat starts and accepts requests on port 8080.</li>
 * </ol>
 *
 * <p>Required environment variables (or application-local.properties):
 * {@code SUPABASE_DB_URL}, {@code SUPABASE_DB_USER},
 * {@code SUPABASE_DB_PASSWORD}, {@code APP_JWT_SECRET}.
 * See {@code .env.example} in the {@code backend/} directory.
 */
@SpringBootApplication
public class CivicConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivicConnectApplication.class, args);
    }
}
