package it.itsacademy.gestione_pagamento.awss3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3AsyncClient s3AsyncClient; // Pour le listage asynchrone
    private final S3Presigner s3Presigner;     // Pour la génération de l'URL présignée

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${AWS_S3_FOLDER}")
    private String rootFolder;

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
    /**
     * ÉTAPE 1 : Lister les reçus d'un utilisateur (Version Synchrone)
     */
    public List<String> listRicevuteSync(String idUtente) {
        String prefix = rootFolder + "/" + idUtente + "/";

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        // Le .join() force l'attente du résultat de manière synchrone
        ListObjectsV2Response response = s3AsyncClient.listObjectsV2(listRequest).join();

        return response.contents().stream()
                .map(S3Object::key)
                .map(key -> key.substring(key.lastIndexOf("/") + 1))
                .filter(name -> !name.isEmpty() && name.endsWith(".pdf"))
                .collect(Collectors.toList());
    }

    /**
     * ÉTAPE 2 : Générer l'URL présignée pour le fichier sélectionné
     * Cible le fichier : bucket/l.pazienza/{idUtente}/{nomeFile}
     */
    public String generatePresignedUrlToDownload(String idUtente, String nomeFile) {
        String s3Key = rootFolder + "/" + idUtente + "/" + nomeFile;

        // Préparation de la requête d'accès au fichier sur S3
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        // On configure un lien temporaire valide pendant 5 minutes
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        // Signature de la requête
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        // On retourne l'URL complète sous forme de String
        return presignedRequest.url().toString();
    }

    /**
     * Télécharge un fichier (MultipartFile) directement sur S3 de manière synchrone.
     * Le dossier racine de l'application est masqué via la propriété rootFolder ($).
     */
    public PutObjectResponse uploadMultipartFile(String idUtente, UUID idOrdine, MultipartFile file) throws IOException {
        String nomeFileOriginale = file.getOriginalFilename();

        String keyName = this.rootFolder + "/" + idUtente + "/giustificativi/GIUSTIFICATIVO_" + idOrdine + "_" + nomeFileOriginale;

        log.info("[S3] Clé d'archivage masquée générée : {}", keyName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .contentType(file.getContentType())
                .build();

        // Solution asynchrone : Utiliser AsyncRequestBody.fromBytes() avec file.getBytes()
        return s3AsyncClient.putObject(
                putObjectRequest,
                AsyncRequestBody.fromBytes(file.getBytes())
        ).join();
    }
}