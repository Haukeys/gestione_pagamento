package it.itsacademy.gestione_pagamento.service;

import it.itsacademy.gestione_pagamento.client.AuthServiceClient;
import it.itsacademy.gestione_pagamento.dto.PagamentoHistoryDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentRequestDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentResponseDTO;
import it.itsacademy.gestione_pagamento.dto.UserEmailDTO;
import it.itsacademy.gestione_pagamento.email.EmailServiceImpl;
import it.itsacademy.gestione_pagamento.entity.Pagamento;
import it.itsacademy.gestione_pagamento.entity.TipoPagamento;
import it.itsacademy.gestione_pagamento.repository.PagamentoRepository;
import it.itsacademy.gestione_pagamento.ricevuta.RicevutaServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagamentoServiceImpl implements PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final Random random = new Random(); // Générateur aléatoire pour simuler la banque
    private final EmailServiceImpl emailService;//injection de l email
    private final AuthServiceClient authServiceClient;
    /**
     * Reçoit la demande de paiement, simule le résultat,
     * persiste la transaction et renvoie le rapport au microservice 8080.
     */
    private final RicevutaServiceImpl ricevutaService;

    @Override
    @Transactional
    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        Pagamento pagamento = new Pagamento();

        // RÈGLE MÉTIER : 50% ACCETTATO / 50% RIFIUTATO
        TipoPagamento risultato = random.nextBoolean() ? TipoPagamento.ACCETTATO : TipoPagamento.RIFIUTATO;
        pagamento.setStatoPagamento(risultato);

        pagamento.setDataPagamento(LocalDate.now());
        pagamento.setIdOrdine(request.getIdOrdine());
        // Sauvegarde de l'historique dans MySQL (génère l'UUID automatique de la transaction)
        pagamento = pagamentoRepository.save(pagamento);
        //Ajouter pour faire partir l'email de validation ou de rejet
        UserEmailDTO user =
                authServiceClient.getUser(
                        request.getIdUtente());

        if (risultato == TipoPagamento.ACCETTATO) {

            emailService.sendPaymentAccepted(//MESSAGE PAYEMENT ACCEPTER
                    user.getEmail(),
                    user.getUsername());
            // NOUVEAU : Utilisation du RicevutaService isolé
            try {
                String idUtenteStr = request.getIdUtente().toString();
                String totaleStr = request.getTotale() != null ? request.getTotale().toString() : "0.0";

                // Génération externe du fichier .txt
                String nomeFile = ricevutaService.generareRicevutaFisica(idUtenteStr, totaleStr);

                // Sauvegarde du nom de fichier en BDD MySQL via le repository
                pagamentoRepository.updateNomeRicevutaById(pagamento.getId(), nomeFile);
                pagamento.setNomeRicevuta(nomeFile); // Met à jour l'objet pour la réponse DTO

                System.out.println("[8081] Ricevuta generata dal servizio: " + nomeFile);

            } catch (IOException e) {
                // On log l'erreur d'écriture, mais on ne bloque pas la transaction d'un paiement accepté
                System.err.println("[8081] Errore critico salvataggio file ricevuta: " + e.getMessage());
                e.printStackTrace();
            }
        } else {

            emailService.sendPaymentRejected(//MESSAGE PAYEMENT REFUSER
                    user.getEmail(),
                    user.getUsername());
        }

        // Assemblage manuel et propre du DTO de réponse
        return new PaymentResponseDTO(
                pagamento.getId(),
                request.getIdOrdine(),
                pagamento.getStatoPagamento(),
                pagamento.getNomeRicevuta()
        );
    }
    @Override
    @Transactional(readOnly = true)
    public List<PagamentoHistoryDTO> getPaymentsByOrderId(UUID idOrdine) {
        return pagamentoRepository.findByIdOrdine(idOrdine)
                .stream()
                .map(pagamento -> new PagamentoHistoryDTO(
                        pagamento.getId(),
                        pagamento.getStatoPagamento(),
                        pagamento.getDataPagamento()
                ))
                .collect(Collectors.toList());
    }
    @Override
    public PaymentResponseDTO getPaymentStatusByOrdineId(UUID idOrdine) {
        // 1. On cherche la liste des paiements liés à la commande
        List<Pagamento> pagamenti = pagamentoRepository.findByIdOrdine(idOrdine);

        // Si la liste est vide, on lève l'exception
        if (pagamenti.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento non trovato");
        }
        Pagamento pagamento = pagamenti.stream()//changement pour que au moin avec 1 ok ça change
                .filter(p -> p.getStatoPagamento() == TipoPagamento.ACCETTATO)
                .findFirst()
                .orElse(pagamenti.get(pagamenti.size() - 1));

        // 2. On remplit le DTO de réponse
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setIdPagamento(pagamento.getId());
        response.setIdOrdine(pagamento.getIdOrdine());
        response.setStatoPagamento(pagamento.getStatoPagamento());
        response.setNomeRicevuta(pagamento.getNomeRicevuta()); // ajout pour etre mirroir avec ordini
        return response;
    }
    @Override
    @Transactional
    public void eliminaPagamentiRifiutati() {
        pagamentoRepository.deleteByStatoPagamento(TipoPagamento.RIFIUTATO);
    }
}
