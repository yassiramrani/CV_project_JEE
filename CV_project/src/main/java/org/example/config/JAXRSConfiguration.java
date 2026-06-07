package org.example.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@DataSourceDefinition(
    name = "java:app/jdbc/recruit_ai",
    className = "org.postgresql.ds.PGSimpleDataSource",
    url = "jdbc:postgresql://localhost:5432/recruit_ai",
    user = "postgres",
    password = "Ben3issa"
)
@ApplicationPath("/api")
public class JAXRSConfiguration extends Application {
    // Activates JAX-RS
}
