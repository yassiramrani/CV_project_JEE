package org.example.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[] { DatabaseConfig.class }; // Loads DB and Security
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] { AppConfig.class }; // Loads Web components
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" }; // Routes all traffic to Spring
    }
}