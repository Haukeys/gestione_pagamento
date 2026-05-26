package it.itsacademy.gestione_pagamento.dto;

import it.itsacademy.gestione_pagamento.entity.TipoPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDTO {
    private UUID idPagamento;
    private UUID idOrdine;
    private TipoPagamento statoPagamento; // L'enum sera automatiquement converti en String dans le JSON
}
