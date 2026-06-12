package it.itsacademy.gestione_pagamento.dto;

//CLASS MIRROIR POUR LA RECEPTION DES DONNER DEUPIS GESTIONE ORDINE AUTH

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailDTO {

    @NotNull(message = "La password non puo essere null")
    private UUID idUtente;

    @NotBlank(message = "La password non puo essere vuota")
    private String username;

    @NotBlank(message = "La password non puo essere vuota")
    private String email;

}
