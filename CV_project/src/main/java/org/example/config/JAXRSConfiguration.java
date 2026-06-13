package org.example.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@DataSourceDefinition(
    name = "java:app/jdbc/recruit_ai",
    className = "org.h2.jdbcx.JdbcDataSource",
    url = "jdbc:h2:mem:recruit_ai;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = ""
)
@ApplicationPath("/api")
public class JAXRSConfiguration extends Application {
    // Activates JAX-RS
}
