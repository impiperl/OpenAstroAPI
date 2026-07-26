package astroapi;

import astroapi.config.AppProperties;
import astroapi.filter.ApiKeyFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class AstroAPIApplication {

    @Autowired
    private ApiKeyFilter apiKeyFilter;

    public static void main(String[] args) {
        SpringApplication.run(AstroAPIApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration() {
        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(apiKeyFilter);
        registration.addUrlPatterns("/api/*"); // Filter auf bestimmte Pfade anwenden
        registration.setOrder(1); // Reihenfolge des Filters
        return registration;
    }
}