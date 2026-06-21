package it.itsacademy.gestione_pagamento.ricevuta;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class RicevutaServiceImpl {

    // Injection du chemin du répertoire (Volume Docker) défini dans application.properties
    @Value("${ricevute.storage.path}")
    private String storagePath;

    /**
     * Gère la génération physique du fichier reçu (.txt) sur le disque.
     * * @param idUtente L'identifiant de l'utilisateur
     * @param totale Le montant total payé
     * @return Le nome du fichier généré (ex: RICEVUTA_20260619_1530.txt)
     * @throws IOException En cas de problème d'écriture sur le disque
     */
    public String generareRicevutaFisica(String idUtente, String totale) throws IOException {
        LocalDateTime oraAttuale = LocalDateTime.now();

        // Formatage du nom du fichier demandé : RICEVUTA_YYYYMMDD_HHMM.txt
        DateTimeFormatter formatterFile = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String nomeFile = "RICEVUTA_" + oraAttuale.format(formatterFile) + ".txt";

        // Formatage de la date pour l'affichage à l'intérieur du texte
        DateTimeFormatter formatterTesto = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dataTesto = oraAttuale.format(formatterTesto);

        // Construction de la chaîne de caractèressoit le texte interne au fichier
        String contenuto = String.format(
                "L’utente %s in data %s ha pagato %s.",
                idUtente,
                dataTesto,
                totale
        );

        // Vérification et création du répertoire si nécessaire (Volume Docker)
        Path directoryPath = Paths.get(storagePath);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        // Écriture physique du fichier texte
        Path fileCompleto = directoryPath.resolve(nomeFile);
        Files.writeString(fileCompleto, contenuto, StandardCharsets.UTF_8);

        return nomeFile;
    }
}
