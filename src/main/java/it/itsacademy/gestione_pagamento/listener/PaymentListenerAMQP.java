package it.itsacademy.gestione_pagamento.listener;

import it.itsacademy.gestione_pagamento.client.AuthServiceClient;
import it.itsacademy.gestione_pagamento.dto.PaymentRequestDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentResponseDTO;
import it.itsacademy.gestione_pagamento.dto.UserEmailDTO;
import it.itsacademy.gestione_pagamento.entity.TipoPagamento;
import it.itsacademy.gestione_pagamento.email.EmailServiceImpl;
import it.itsacademy.gestione_pagamento.ricevuta.RicevutaServiceImpl;
import it.itsacademy.gestione_pagamento.repository.PagamentoRepository;
import it.itsacademy.gestione_pagamento.service.PagamentoService;
import it.itsacademy.gestione_pagamento.awss3.S3StorageService;
import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j // Ajout de Lombok pour la gestion moderne des logs
@Component
@RequiredArgsConstructor
public class PaymentListenerAMQP {

    private final PagamentoService pagamentoService;
    private final RicevutaServiceImpl ricevutaService;
    private final EmailServiceImpl emailService;
    private final AuthServiceClient authServiceClient;
    private final PagamentoRepository pagamentoRepository;
    private final S3StorageService s3StorageService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${storage.receipts.path}")
    private String ricevuteStoragePath;

    // Récupère la valeur injectée depuis l'environnement
    @Value("${aws.s3.folder}")
    private String awsS3Folder;
    /**
     * ÉTAPE 1 : Traitement initial et persistance
     */
    @RabbitListener(queues = "queue.pagamenti")
    public void receivePaymentRequest(PaymentRequestDTO requestDTO) {
        try {
            log.info("[Thread: {}] JSON ricevuto per ordine ID: {}", Thread.currentThread().getName(), requestDTO.getIdOrdine());

            // Exécution de la transaction et sauvegarde en BDD
            PaymentResponseDTO response = pagamentoService.processPayment(requestDTO);
            log.info("Pagamento salvato con successo in base dati! Stato: {}", response.getStatoPagamento());

            // Injection de l'ID généré dans le DTO
            requestDTO.setIdPagamento(response.getIdPagamento());

            if (response.getStatoPagamento() == TipoPagamento.ACCETTATO) {
                // Si accepté, on passe à l'étape Jasper (Queue 2)
                rabbitTemplate.convertAndSend("orders.exchange", "payment.routing.receipt", requestDTO);
                log.info("Pagamento ACCETTATO. Spinto verso la generazione ricevuta.");
            } else {
                // Si refusé, on saute Jasper et on va directement aux emails (Queue 3)
                rabbitTemplate.convertAndSend("orders.exchange", "receipt.routing.email", requestDTO);
                log.info("Pagamento RIFIUTATO. Spinto direttamente verso la notifica di rifiuto.");
            }

        } catch (Exception e) {
            log.error("Errore critico durante la transazione dell'ordine ID {}: {}", requestDTO.getIdOrdine(), e.getMessage(), e);
        }
    }

    /**
     * ÉTAPE 2 : Génération Jasper PDF & Transfert AWS S3 (Asynchrone non bloquant)
     */
    @RabbitListener(queues = "queue.ricevute")
    public void handleReceiptGeneration(PaymentRequestDTO requestDTO) {
        String nomeFile = null;
        File fileNelVolume = null; // <--- 2. NETTOYÉ (PLUS DE PREFIXE java.io)

        try {
            String idUtenteStr = requestDTO.getIdUtente().toString();
            String totaleStr = requestDTO.getTotale() != null ? requestDTO.getTotale().toString() : "0.0";
            String descrizioneProdotto = requestDTO.getDescrizione() != null ? requestDTO.getDescrizione() : "N/D";
            // 1. Génération physique du PDF via le service Jasper
            nomeFile = ricevutaService.generareRicevutaFisica(idUtenteStr, totaleStr,descrizioneProdotto);

            // Référence vers le fichier situé dans le volume Docker
            fileNelVolume = new File(ricevuteStoragePath + "/" + nomeFile); // <--- 2. NETTOYÉ

            if (!fileNelVolume.exists()) {
                log.error("[VOLUME DOCKER] Errore: Il file generato da Jasper non esiste al percorso: {}", fileNelVolume.getAbsolutePath());
                rabbitTemplate.convertAndSend("orders.exchange", "receipt.routing.email", requestDTO);
                return;
            }

            log.info("[VOLUME DOCKER] File trovato con successo: {} ({} byte).", fileNelVolume.getAbsolutePath(), fileNelVolume.length());

            // 3. CONSTRUCTION DU CHEMIN S3 RESTREINT (Exemple: l.pazienza/12/RICEVUTA_XYZ.pdf)
            String s3Key = awsS3Folder + "/" + idUtenteStr + "/" + nomeFile;
            log.info("Avvio caricamento asincrono su AWS S3 per la chiave: {}", s3Key);

            final String finalNomeFile = nomeFile;
            final File finalFileNelVolume = fileNelVolume; // <--- 2. NETTOYÉ

            // 4. UPLOAD SUR S3 AVEC LA CLÉ COMPLÈTE EN PREMIER PARAMÈTRE
            s3StorageService.uploadPdfAsync(s3Key, fileNelVolume.toPath())
                    .whenComplete((response, exception) -> {

                        if (exception != null) {
                            log.error("[AWS S3] Errore critico durante l'upload su {}: {}", s3Key, exception.getMessage());
                            rabbitTemplate.convertAndSend("orders.exchange", "receipt.routing.email", requestDTO);
                        } else {
                            log.info("[Thread: {}] [AWS S3] Upload completato con successo! ETag ricevuto: {}", Thread.currentThread().getName(), response.eTag());

                            // Sauvegarde du nom de fichier en BDD MySQL
                            pagamentoRepository.updateNomeRicevutaById(requestDTO.getIdPagamento(), finalNomeFile);

                            // On stocke le nom du fichier dans le DTO pour l'envoi de l'email
                            requestDTO.setNomeRicevuta(finalNomeFile);

                            // NETTOYAGE LOCAL (VM Ubuntu)
                            if (finalFileNelVolume.exists()) {
                                boolean isDeleted = finalFileNelVolume.delete();
                                if (isDeleted) {
                                    log.info("[VOLUME DOCKER] File temporaneo locale eliminato con successo dal volume.");
                                } else {
                                    log.warn("[VOLUME DOCKER] Impossibile eliminare il file dal volume.");
                                }
                            }

                            // On pousse vers la file d'attente d'Email
                            rabbitTemplate.convertAndSend("orders.exchange", "receipt.routing.email", requestDTO);
                        }
                    });

        } catch (Exception e) {
            log.error("Errore generale nel listener delle ricevute: {}", e.getMessage(), e);
            rabbitTemplate.convertAndSend("orders.exchange", "receipt.routing.email", requestDTO);
        }
    }

    /**
     * ÉTAPE 3 : Notifications e-mails (Succès ou Échec)
     */
    @RabbitListener(queues = "queue.notifications")
    public void handleEmailNotification(PaymentRequestDTO requestDTO) {
        try {
            // Récupération de l'utilisateur via le client Feign
            UserEmailDTO user = authServiceClient.getUser(requestDTO.getIdUtente());

            if (requestDTO.getNomeRicevuta() != null) {
                // Envoi de l'email de confirmation avec le PDF attaché depuis le volume Docker
                emailService.sendPaymentAcceptedWithAttachment(user.getEmail(), user.getUsername(), requestDTO.getNomeRicevuta());
                log.info("[Queue-Email] Email di conferma con allegato inviata con successo a: {}", user.getEmail());
            } else {
                // Envoi du mail de rejet simple en cas d'absence de reçu (Paiement refusé)
                emailService.sendPaymentRejected(user.getEmail(), user.getUsername());
                log.info("[Queue-Email] Email di errore pagamento inviata con successo a: {}", user.getEmail());
            }
        } catch (Exception e) {
            log.error("[Queue-Email] Errore durante l'invio dell'email per l'utente ID {}: {}", requestDTO.getIdUtente(), e.getMessage(), e);
        }
    }
}