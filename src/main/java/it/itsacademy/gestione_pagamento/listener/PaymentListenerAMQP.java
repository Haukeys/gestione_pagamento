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
    public void receivePaymentRequest(PaymentRequestDTO requestDTO) {
        try {
            System.out.println("[8081] JSON ricevuto per ordine ID: " + requestDTO.getIdOrdine());
            pagamentoService.processPayment(requestDTO);
            System.out.println("[8081] Pagamento salvato con successo in base dati !");
        } catch (Exception e) {
            System.err.println("[8081] Errore critico durante le transazione: " + e.getMessage());
            e.printStackTrace();
        }
    }
}





//@Component sans docker
//@RequiredArgsConstructor
//public class PaymentListenerAMQP {
//
//    private final PagamentoService pagamentoService;
//
//    @RabbitListener(queues = "exam_queue")
//    public void receivePaymentRequest(PaymentRequestDTO requestDTO) { //Jackson reconstruit l'objet ici !
//        System.out.println("[8081] JSON ricevuto e convertito per l'ordine ID: " + requestDTO.getIdOrdine());
//
//        // Tu passes directement l'objet à ton service pour traitement
//        pagamentoService.processPayment(requestDTO);
//    }
//}