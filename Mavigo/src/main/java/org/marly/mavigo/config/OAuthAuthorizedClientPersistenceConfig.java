package org.marly.mavigo.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

@Configuration
public class OAuthAuthorizedClientPersistenceConfig {

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            DataSource dataSource,
            ClientRegistrationRepository clientRegistrationRepository) {
        return new JdbcOAuth2AuthorizedClientService(new JdbcTemplate(dataSource), clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(
            OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }

    @Bean
    public ApplicationRunner oauth2AuthorizedClientTableInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return args -> {
            String databaseProduct;
            String jdbcUrl;
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                databaseProduct = metaData != null ? metaData.getDatabaseProductName() : "";
                jdbcUrl = metaData != null ? metaData.getURL() : "";
            }

            String productLower = databaseProduct == null ? "" : databaseProduct.toLowerCase();
            String urlLower = jdbcUrl == null ? "" : jdbcUrl.toLowerCase();
            boolean postgres = productLower.contains("postgres")
                    || urlLower.contains("mode=postgresql");
            String binaryType = postgres ? "bytea" : "varbinary";

            String sql = """
                    CREATE TABLE IF NOT EXISTS oauth2_authorized_client (
                      client_registration_id varchar(100) NOT NULL,
                      principal_name varchar(200) NOT NULL,
                      access_token_type varchar(100) NOT NULL,
                      access_token_value %s NOT NULL,
                      access_token_issued_at timestamp NOT NULL,
                      access_token_expires_at timestamp NOT NULL,
                      access_token_scopes varchar(1000) DEFAULT NULL,
                      refresh_token_value %s DEFAULT NULL,
                      refresh_token_issued_at timestamp DEFAULT NULL,
                      created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      PRIMARY KEY (client_registration_id, principal_name)
                    )
                    """.formatted(binaryType, binaryType);

            jdbcTemplate.execute(sql);
        };
    }
}
