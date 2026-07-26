package astroapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the application.
 * <p>
 * This class allows externalized configuration of application-specific settings,
 * particularly security settings, via properties files or environment variables
 * prefixed with `spring.app`.
 * </p>
 */
@ConfigurationProperties(prefix = "spring.app")
public class AppProperties {

    private Security security;

    /**
     * Nested class to hold security-related configuration properties.
     */
    public static class Security {

        private String secretKey; // The secret key used for securing requests
        private int tokenExpiration; // The expiration time for tokens, in seconds

        /**
         * Gets the secret key used for securing requests.
         *
         * @return The secret key as a String.
         */
        public String getSecretKey() {
            return secretKey;
        }

        /**
         * Sets the secret key used for securing requests.
         *
         * @param secretKey The secret key as a String.
         */
        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        /**
         * Gets the token expiration time in seconds.
         *
         * @return The expiration time as an integer.
         */
        public int getTokenExpiration() {
            return tokenExpiration;
        }

        /**
         * Sets the token expiration time in seconds.
         *
         * @param tokenExpiration The expiration time as an integer.
         */
        public void setTokenExpiration(int tokenExpiration) {
            this.tokenExpiration = tokenExpiration;
        }
    }

    /**
     * Gets the security configuration properties.
     *
     * @return The {@link Security} object containing security-related settings.
     */
    public Security getSecurity() {
        return security;
    }

    /**
     * Sets the security configuration properties.
     *
     * @param security The {@link Security} object containing security-related settings.
     */
    public void setSecurity(Security security) {
        this.security = security;
    }
}
