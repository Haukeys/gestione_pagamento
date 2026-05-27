package it.itsacademy.gestione_pagamento.listener;

import it.itsacademy.gestione_pagamento.dto.PaymentRequestDTO;
import it.itsacademy.gestione_pagamento.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentListenerAMQP {

    private final PagamentoService pagamentoService;

    @RabbitListener(queues = "exam_queue")
    public void receivePaymentRequest(PaymentRequestDTO requestDTO) { //Jackson reconstruit l'objet ici !
        System.out.println("[8081] JSON ricevuto e convertito per l'ordine ID: " + requestDTO.getIdOrdine());

        // Tu passes directement l'objet à ton service pour traitement
        pagamentoService.processPayment(requestDTO);
    }
}