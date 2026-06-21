package it.itsacademy.gestione_pagamento.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "data_pagamento",nullable = false)
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPagamento statoPagamento;

    // IMPORTANT : Pour pouvoir lier et chercher par ID de commande
    @Column(name = "id_ordine", nullable = false)
    private UUID idOrdine;

    // NOUVEAU : Colonne optionnelle (si le pagamento est rifiutato) pour stocker le nom de la ricevuta
    @Column(name = "nome_ricevuta", nullable = true)
    private String nomeRicevuta;
}
