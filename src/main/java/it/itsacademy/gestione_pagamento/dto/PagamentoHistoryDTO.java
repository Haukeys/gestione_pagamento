package it.itsacademy.gestione_pagamento.dto;

import it.itsacademy.gestione_pagamento.entity.TipoPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagamentoHistoryDTO {
    private UUID idPagamento;
    private TipoPagamento statoPagamento;
    private LocalDate dataPagamento;
}
