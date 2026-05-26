package it.itsacademy.gestione_pagamento.service;

import it.itsacademy.gestione_pagamento.dto.PagamentoHistoryDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentRequestDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentResponseDTO;
import it.itsacademy.gestione_pagamento.entity.Pagamento;
import it.itsacademy.gestione_pagamento.entity.TipoPagamento;
import it.itsacademy.gestione_pagamento.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagamentoServiceImpl implements PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final Random random = new Random(); // Générateur aléatoire pour simuler la banque

    /**
     * Reçoit la demande de paiement, simule le résultat,
     * persiste la transaction et renvoie le rapport au microservice 8080.
     */

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

        // Assemblage manuel et propre du DTO de réponse
        return new PaymentResponseDTO(
                pagamento.getId(),
                request.getIdOrdine(),
                pagamento.getStatoPagamento()
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


}
