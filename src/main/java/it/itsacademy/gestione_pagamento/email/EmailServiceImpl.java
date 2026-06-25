package it.itsacademy.gestione_pagamento.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    // Injection de l'interface principale Spring dédiée à la communication SMTP
    private final JavaMailSender mailSender;

    @Value("${ricevute.storage.path}")
    private String storagePath;

    // NOUVEAU : Envoi du mail au format HTML/MIME avec le PDF en pièce jointe
    public void sendPaymentAcceptedWithAttachment(String email, String username, String nomeFile) throws Exception {
        // Initialisation d'un objet message complexe (permettant le multipart / pièces jointes)
        MimeMessage message = mailSender.createMimeMessage();

        // Assistant de configuration (MimeMessageHelper) activé en mode multipart (true) et encodage UTF-8
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Paramétrage des métadonnées de routage et de contenu textuel
        helper.setTo(email);
        helper.setSubject("Pagamento accettato");
        helper.setText("Ciao " + username + ", il tuo pagamento è stato ACCETTATO. In allegato trovi la tua ricevuta.");

        // Pointage et résolution du fichier physique au sein du volume partagé Docker
        Path fileCompleto = Paths.get(storagePath).resolve(nomeFile);
        FileSystemResource fileResource = new FileSystemResource(fileCompleto.toFile());

        // Rattachement sécurisé du fichier s'il est physiquement localisable sur le disque
        if (fileResource.exists()) {
            helper.addAttachment(nomeFile, fileResource);
        } else {
            // Journalisation d'erreur système si l'échange de fichiers inter-services a échoué
            System.err.println("[Email-Service] Errore: File non trovato sul disco: " + fileCompleto);
        }

        // Transmission finale du paquet SMTP au serveur distant configuré
        mailSender.send(message);
    }

    /**
     * Envoie une notification textuelle simple de confirmation (sans pièce jointe).
     */
    @Override
    public void sendPaymentAccepted(String email, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Pagamento accettato");
        message.setText("Gentile Cliente," + username + ", il suo pagamento è stato ACCETTATO.");
        mailSender.send(message);
    }

    /**
     * Envoie une notification textuelle simple informant du rejet ou de l'échec de la transaction.
     */
    @Override
    public void sendPaymentRejected(String email, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Pagamento rifiutato");
        message.setText("Gentile Cliente," + username + ", il suo pagamento è stato RIFIUTATO.");
        mailSender.send(message);
    }
}