package it.itsacademy.gestione_pagamento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentRequestDTO {

    @NotNull(message = "Id ordine obbligatorio")
    private UUID idOrdine;

    @NotNull(message = "Totale obbligatorio")
    @Positive(message = "Il totale deve essere maggiore di zero")
    private Double totale;
}


