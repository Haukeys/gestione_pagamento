package it.itsacademy.gestione_pagamento.awss3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    // 1. On injecte les CHEMINS des fichiers secrets à la place des valeurs en texte brut
    @Value("${AWS_S3_ACCESS_KEY_FILE}")
    private String accessKeyFilePath;

    @Value("${AWS_S3_SECRET_KEY_FILE}")
    private String secretKeyFilePath;

    @Bean
    public S3AsyncClient s3AsyncClient() throws IOException {
        // 2. Lecture dynamique du contenu textuel des secrets montés dans /run/secrets/
        // Le .trim() supprime les espaces ou sauts de ligne invisibles qui feraient échouer la connexion AWS
        String accessKey = Files.readString(Paths.get(accessKeyFilePath)).trim();
        String secretKey = Files.readString(Paths.get(secretKeyFilePath)).trim();

        return S3AsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                // Tempo di attesa massima per l'interazione con AWS
                .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(2)))
                .build();
    }
}
