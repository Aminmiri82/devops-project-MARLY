package org.marly.mavigo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OAuthWebClientConfig {

    @Bean
    WebClient googleApiWebClient(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientRepository authorizedClients) {

        var oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(registrations, authorizedClients);
        oauth.setDefaultClientRegistrationId("google");

        return WebClient.builder()
                .baseUrl("https://tasks.googleapis.com/tasks/v1")
                .apply(oauth.oauth2Configuration()) // ajoute automatiquement le Bearer token du user
                .build();
    }
}