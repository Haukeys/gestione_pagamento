package it.itsacademy.gestione_pagamento.repository;

import it.itsacademy.gestione_pagamento.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    List<Pagamento>findByIdOrdine(UUID idOrdine); //per legare pagamenti ad un ordine

}
