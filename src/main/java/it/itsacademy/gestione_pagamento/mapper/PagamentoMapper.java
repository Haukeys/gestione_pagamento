package it.itsacademy.gestione_pagamento.mapper;

import it.itsacademy.gestione_pagamento.dto.PaymentRequestDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentResponseDTO;
import it.itsacademy.gestione_pagamento.entity.Pagamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PagamentoMapper {

    // On doit fusionner les informations de la BDD (pagamento) et de la requête originale
    @Mapping(target = "idPagamento", source = "pagamento.id")
    @Mapping(target = "idOrdine", source = "request.idOrdine")
    @Mapping(target = "statoPagamento", source = "pagamento.statoPagamento")
    @Mapping(target = "nomeRicevuta", source = "pagamento.nomeRicevuta")
    PaymentResponseDTO toResponseDto(Pagamento pagamento, PaymentRequestDTO request);

    // NOUVELLE MÉTHODE À AJOUTER : Pour le paiement par chèque (Assegno)
    // Convertit directement l'entité car idOrdine et id sont déjà présents à l'intérieur
    @Mapping(target = "idPagamento", source = "id")
    @Mapping(target = "idOrdine", source = "idOrdine")
    @Mapping(target = "statoPagamento", source = "statoPagamento")
    @Mapping(target = "nomeRicevuta", source = "nomeRicevuta")
    PaymentResponseDTO toResponseDtoDirect(Pagamento pagamento);
}

