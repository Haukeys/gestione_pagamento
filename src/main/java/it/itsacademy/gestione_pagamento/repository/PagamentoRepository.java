package it.itsacademy.gestione_pagamento.repository;

import it.itsacademy.gestione_pagamento.entity.Pagamento;
import it.itsacademy.gestione_pagamento.entity.TipoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    List<Pagamento>findByIdOrdine(UUID idOrdine); //per legare pagamenti a un ordine

    @Modifying //Attention, cette méthode ne va pas récupérer des données, elle va modifier la base de données (via un DELETE dans notre cas). Signification du @modifying ici.
    @Transactional//car on doit le gérer dans une transcation
    void deleteByStatoPagamento(TipoPagamento statoPagamento);

    // NOUVEAU : Requête d'écriture pour sauvegarder le nom du fichier
    @Modifying
    @Transactional
    /**
     * MÉTHODE AVEC REQUÊTE PERSONNALISÉE : updateNomeRicevutaById
     * * POURQUOI ON L'UTILISE :
     * Une fois que notre 'RicevutaService' a créé le fichier texte (.txt) sur le disque, on doit enregistrer le nom
     * de ce fichier dans la ligne du paiement correspondant.
     * * POURQUOI UTILISE-T-ON @Query ICI (Au lieu d'une méthode dérivée classique) ?
     * Spring Data JPA sait nativement chercher ('find') ou supprimer ('delete') en analysant le nom d'une méthode.
     * En revanche, les modifications ciblées (les 'UPDATE' SQL) ne sont pas gérées proprement par les conventions de nommage automatiques.
     * Pour effectuer une mise à jour ultra-performante en BDD sans devoir recharger toute l'entité en mémoire, modifier l'objet,
     * puis la ré-enregistrer, on écrit explicitement la requête JPQL (Java Persistence Query Language) dans l'annotation @Query.
     * * EXPLICATION DES ÉLÉMENTS DU CODE :
     * 1. @Modifying & @Transactional : Obligatoires ici aussi car on effectue une écriture (un UPDATE SQL) en base de données.
     * 2. @Query("UPDATE Pagamento p SET p.nomeRicevuta = :nomeRicevuta WHERE p.id = :id") : C'est notre ordre de mise à jour.
     * Les expressions précédées de deux-points (:nomeRicevuta et :id) sont des variables dynamiques.
     * Spring va injecter automatiquement les arguments de la méthode Java 'nomeRicevuta' et 'id' directement à ces emplacements
     * car ils portent exactement le même nom.
     */
    @Query("UPDATE Pagamento p SET p.nomeRicevuta = :nomeRicevuta WHERE p.id = :id")
    void updateNomeRicevutaById(@Param("id") UUID id, @Param("nomeRicevuta") String nomeRicevuta);





}
