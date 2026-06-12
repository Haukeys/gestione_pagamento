package it.itsacademy.gestione_pagamento.client;

import it.itsacademy.gestione_pagamento.dto.UserEmailDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class AuthServiceClient {

    private final RestClient restClient;

    public AuthServiceClient(@Value("${auth.service.url}")String authUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(authUrl)
                .build();
    }
    public UserEmailDTO getUser(UUID idUtente){
        return restClient.get()
                .uri("/auth/user/{id}",idUtente)//a voir si faut pas ajouter /api
                .retrieve()
                .body(UserEmailDTO.class);

    }
}
