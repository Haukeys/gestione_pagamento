package it.itsacademy.gestione_pagamento.ricevuta;

import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class RicevutaServiceImpl {

    // Injection du chemin du répertoire (Volume Docker) défini dans application.properties
    @Value("${ricevute.storage.path}")
    private String storagePath;

    // NOUVEAU : Injection du chemin du dossier Jasper la ou ce trouve le template
    @Value("${jasper.reports.directory}")
    private String jasperReportsDir;

    /**
     * Gère la génération physique du fichier reçu (.txt) sur le disque.
     * * @param idUtente L'identifiant de l'utilisateur
     * @param totale Le montant total payé
     * @return Le nome du fichier généré (ex: RICEVUTA_20260619_1530.txt)
     * @throws IOException En cas de problème d'écriture sur le disque
     */
    public String generareRicevutaFisica(String idUtente, String totale, String descrizioneProdotto) throws IOException {
        LocalDateTime oraAttuale = LocalDateTime.now();

        // Formatage du nom du fichier demandé : RICEVUTA_YYYYMMDD_HHMM.txt
        DateTimeFormatter formatterFile = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String nomeFile = "RICEVUTA_" + oraAttuale.format(formatterFile) + ".pdf";//changer l'extention selon le modele(txt/pdf)

        // Formatage de la date pour l'affichage à l'intérieur du texte
        DateTimeFormatter formatterTesto = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dataTesto = oraAttuale.format(formatterTesto);

        // Vérification et création du répertoire si nécessaire (Volume Docker)
        Path directoryPath = Paths.get(storagePath);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        // Écriture physique du fichier texte
        Path fileCompleto = directoryPath.resolve(nomeFile);
        try {
            // 1. Charger le fichier .jrxml depuis le volume partagé
            Path pathTemplate = Paths.get(jasperReportsDir, "ricevuta.jrxml");
            if (!Files.exists(pathTemplate)) {
                throw new IOException("Template Jasper non trovato al percorso: " + pathTemplate.toAbsolutePath());
            }
            InputStream reportStream = Files.newInputStream(pathTemplate);

            // 2. Charger le logo
            InputStream logoStream = getClass().getResourceAsStream("/images/logo.png");
            if (logoStream == null) {
                System.err.println("[8081] Warning: /images/logo.png not found on classpath!");
            }

            // 3. COMPILATION DIRECTE (Méthode statique propre à la V7)
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // 4. Paramètres
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PARAM_ID_UTENTE", idUtente);
            parameters.put("PARAM_DATA", dataTesto);
            parameters.put("PARAM_DESCRIZIONE", descrizioneProdotto);
            parameters.put("PARAM_TOTALE", totale + "€");
            parameters.put("PARAM_LOGO", logoStream);

            // 5. REMPLISSAGE DIRECT
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // 6. EXPORTATION DIRECTE EN PDF
            JasperExportManager.exportReportToPdfFile(jasperPrint, fileCompleto.toString());

        } catch (JRException e) {
            throw new IOException("Errore durante la generazione del PDF con JasperReports", e);
        }

        return nomeFile;
    }
}
