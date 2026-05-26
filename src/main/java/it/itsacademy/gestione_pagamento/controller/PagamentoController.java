package it.itsacademy.gestione_pagamento.controller;

import it.itsacademy.gestione_pagamento.dto.PagamentoHistoryDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentRequestDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentResponseDTO;
import it.itsacademy.gestione_pagamento.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pagamenti")//GUARDARE BENE SE LA PORTA E QUELLA GIUSTA DIVERSO DA 8080
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    /**
     * Point d'entrée pour le RestClient du projet gestione_ordini.
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> handlePayment(@RequestBody @Valid PaymentRequestDTO request) {
        PaymentResponseDTO response = pagamentoService.processPayment(request);
        return ResponseEntity.ok(response);
    }
    // Dans la classe PagamentoController :
    @GetMapping("/ordine/{idOrdine}")
    public ResponseEntity<List<PagamentoHistoryDTO>> getListPagamenti(@PathVariable("idOrdine") UUID idOrdine) {
        List<PagamentoHistoryDTO> history = pagamentoService.getPaymentsByOrderId(idOrdine);
        return ResponseEntity.ok(history);
    }
}
