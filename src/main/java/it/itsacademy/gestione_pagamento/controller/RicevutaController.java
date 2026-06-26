package it.itsacademy.gestione_pagamento.controller;

import it.itsacademy.gestione_pagamento.awss3.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
@Slf4j
@RestController
@RequestMapping("/pagamenti/ricevute")
@RequiredArgsConstructor
public class RicevutaController {

    private final S3StorageService s3Service;

    /**
     * 1. Demander la liste des reçus (Version Synchrone avec Log d'erreur)
     * URL Gateway : GET http://localhost:8080/api/pagamenti/ricevute
     */
    @GetMapping
    public ResponseEntity<?> getRicevuteList(@RequestHeader("X-User-Id") String idUtente) {
        try {
            List<String> ricevute = s3Service.listRicevuteSync(idUtente);
            return ResponseEntity.ok(ricevute);
        } catch (Exception e) {
            // LOG IMPORTANT : Il va t'afficher exactement pourquoi tu avais une erreur 500 !
            log.error("Errore durante le recupero della lista S3 per utente {}: {}", idUtente, e.getMessage(), e);
            return ResponseEntity.status(500).body("Errore interno: " + e.getMessage());
        }
    }

    /**
     * 2. Demander le lien URL présigné pour télécharger un reçu spécifique
     * Seul le nom du fichier est nécessaire
     * URL Gateway : GET http://localhost:8080/api/pagamenti/ricevute/download/{nomeFile}
     */
    @GetMapping("/download/{nomeFile}")
    public ResponseEntity<String> getDownloadLink(
            @RequestHeader("X-User-Id") String idUtente, // Extrait de manière sécurisée
            @PathVariable String nomeFile) {
        try {
            String presignedUrl = s3Service.generatePresignedUrlToDownload(idUtente, nomeFile);
            return ResponseEntity.ok(presignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Errore durante la generazione del link sicuro S3");
        }
    }
}
