package it.itsacademy.gestione_pagamento.service;

import it.itsacademy.gestione_pagamento.dto.PagamentoHistoryDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentRequestDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PagamentoService {

    public PaymentResponseDTO processPayment(PaymentRequestDTO request);
    public List<PagamentoHistoryDTO> getPaymentsByOrderId(UUID idOrdine);
    public PaymentResponseDTO getPaymentStatusByOrdineId(UUID idOrdine);
}
