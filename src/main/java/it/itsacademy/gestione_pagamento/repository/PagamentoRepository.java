package it.itsacademy.gestione_pagamento.repository;

import it.itsacademy.gestione_pagamento.entity.Pagamento;
import it.itsacademy.gestione_pagamento.entity.TipoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    List<Pagamento>findByIdOrdine(UUID idOrdine); //per legare pagamenti a un ordine

    @Modifying //Attention, cette méthode ne va pas récupérer des données, elle va modifier la base de données (via un DELETE dans notre cas). Signification du @modifying ici.
    @Transactional//car on doit le gérer dans une transcation
    void deleteByStatoPagamento(TipoPagamento statoPagamento);

}
