package it.itsacademy.gestione_pagamento.awss3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
@Slf4j

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3AsyncClient s3AsyncClient;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Upload le fichier PDF de manière asynchrone et non bloquante vers AWS S3
     */
    public CompletableFuture<PutObjectResponse> uploadPdfAsync(String keyName, Path filePath) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .contentType("application/pdf")
                .build();

        return s3AsyncClient.putObject(putObjectRequest, filePath)
                .whenComplete((response, exception) -> {
                    // ATTENZIONE: Questo blocco di codice NON viene eseguito dal thread principale.
                    // Verrà eseguito in futuro dal thread di Netty che riceve la risposta da AWS.
                    if (exception != null) {
                        log.error("[Thread: {}] Error during the upload of {}: {}",
                                Thread.currentThread().getName(), keyName, exception.getMessage()
                        );
                    } else {
                        log.info("[Thread: {}] File uploaded correctly. ETag: {}",
                                Thread.currentThread().getName(), response.eTag()
                        );
                    }
                });
    }
}