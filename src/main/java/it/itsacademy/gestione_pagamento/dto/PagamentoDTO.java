package it.itsacademy.gestione_pagamento.dto;


import it.itsacademy.gestione_pagamento.entity.TipoPagamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagamentoDTO {


    private UUID id;

    @NotNull(message = "stato di Pagamento non puo essere null")
    @NotBlank(message = "stato di pagamento non puo essere vuoto")
    private TipoPagamento statoPagamento;

}
