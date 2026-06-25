package it.itsacademy.gestione_pagamento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentRequestDTO {

    @NotNull(message = "Id ordine obbligatorio")
    private UUID idOrdine;

    @NotNull(message = "Id utente obbligatorio")//ajout pour match le changement effectuer dans gestione ordini au meme niveau
    private UUID idUtente;

    @NotNull(message = "Totale obbligatorio")
    @Positive(message = "Il totale deve essere maggiore di zero")
    private Double totale;

    // Champs de transit pour les queues asynchrones
    private UUID idPagamento;
    private String nomeRicevuta;
    private String descrizione;
}


