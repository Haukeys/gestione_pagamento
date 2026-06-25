package it.itsacademy.gestione_pagamento.service;

import it.itsacademy.gestione_pagamento.dto.PagamentoHistoryDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentRequestDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentResponseDTO;
import it.itsacademy.gestione_pagamento.entity.Pagamento;
import it.itsacademy.gestione_pagamento.entity.TipoPagamento;
import it.itsacademy.gestione_pagamento.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Génère automatiquement le constructeur pour injecter PagamentoRepository
public class PagamentoServiceImpl implements PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final Random random = new Random(); // Générateur aléatoire servant à simuler une passerelle bancaire (ex: Stripe/PayPal)

    /**
     * ÉTAPE 1 DU FLUX GLOBAL : Traitement de la transaction et persistance immédiate.
     * Cette méthode a été allégée au maximum (toute la logique de génération PDF, upload S3 et e-mail
     * a été déplacée de manière asynchrone dans le PaymentListenerAMQP).
     *
     * @param request Le DTO contenant les informations de l'ordre reçu depuis RabbitMQ
     * @return PaymentResponseDTO Une réponse rapide indiquant uniquement si le paiement est accepté ou refusé
     */
    @Override
    @Transactional // Ouvre une transaction de base de données. Si une erreur survient, tout est annulé (rollback)
    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        // Instanciation d'une nouvelle entité JPA 'Pagamento' à sauvegarder en BDD
        Pagamento pagamento = new Pagamento();

        // SIMULATION BANCAIRE : Détermine de façon aléatoire (50% de chance) le résultat de la transaction
        TipoPagamento risultato = random.nextBoolean() ? TipoPagamento.ACCETTATO : TipoPagamento.RIFIUTATO;

        // Remplissage de l'entité avec les informations de la transaction
        pagamento.setStatoPagamento(risultato);
        pagamento.setDataPagamento(LocalDate.now());
        pagamento.setIdOrdine(request.getIdOrdine());

        // PERSISTANCE : Enregistrement immédiat dans la table MySQL.
        // L'entité 'pagamento' récupère alors son ID généré automatiquement par la BDD.
        pagamento = pagamentoRepository.save(pagamento);

        // CONFIGURATION DU DTO DE RÉPONSE ENVOYÉ AU LISTENER AMQP :
        // Note importante : Le champ 'nomeRicevuta' est volontairement mis à 'null' ici.
        // C'est parce que le fichier PDF n'existe pas encore à cette étape ! Il sera généré
        // juste après, de manière asynchrone, par JasperReports via la file 'queue.ricevute'.
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setIdPagamento(pagamento.getId());
        response.setIdOrdine(pagamento.getIdOrdine());
        response.setStatoPagamento(pagamento.getStatoPagamento());
        response.setNomeRicevuta(null);

        return response;
    }

    /**
     * HISTORIQUE DES PAIEMENTS : Permet de récupérer la liste de toutes les tentatives de paiement
     * (acceptées ou refusées) liées à un identifiant de commande précis.
     *
     * @param idOrdine L'UUID de la commande concernée
     * @return List<PagamentoHistoryDTO> Une liste de DTOs allégés contenant l'ID, le statut et la date
     */
    @Override
    @Transactional(readOnly = true) // Mode lecture seule (readOnly) : optimise les performances au niveau de Hibernate/MySQL
    public List<PagamentoHistoryDTO> getPaymentsByOrderId(UUID idOrdine) {
        // 1. Récupération de la liste brute des entités 'Pagamento' depuis le Repository
        return pagamentoRepository.findByIdOrdine(idOrdine)
                .stream() // Ouverture d'un Stream pour transformer (mapper) les objets
                .map(pagamento -> new PagamentoHistoryDTO(
                        pagamento.getId(),
                        pagamento.getStatoPagamento(),
                        pagamento.getDataPagamento()
                )) // Conversion de chaque entité 'Pagamento' en un 'PagamentoHistoryDTO'
                .collect(Collectors.toList()); // Regroupement du résultat dans une liste finale
    }

    /**
     * STATUT COURANT DU PAIEMENT : Utilisé pour la communication synchrone/miroir avec le microservice "Ordini".
     * Permet de savoir si une commande possède un paiement valide ou de récupérer la dernière tentative.
     *
     * @param idOrdine L'UUID de la commande
     * @return PaymentResponseDTO Le DTO contenant le statut de référence et le nom du reçu si existant
     */
    @Override
    @Transactional(readOnly = true) // Mode lecture seule optimisé
    public PaymentResponseDTO getPaymentStatusByOrdineId(UUID idOrdine) {
        // 1. Récupération de l'historique des paiements pour cette commande
        List<Pagamento> pagamenti = pagamentoRepository.findByIdOrdine(idOrdine);

        // SÉCURITÉ : Si aucun paiement (aucune tentative) n'est trouvé pour cet ID, on lève une exception 404 Not Found
        if (pagamenti.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento non trovato");
        }

        // RÈGLE MÉTIER DE SÉLECTION :
        // Une commande peut avoir plusieurs tentatives de paiement (ex: 2 refusés puis 1 accepté).
        // - En priorité, on cherche s'il y a un paiement 'ACCETTATO'.
        // - Si aucun n'est accepté, on sélectionne le tout dernier échec de la liste (le plus récent).
        Pagamento pagamento = pagamenti.stream()
                .filter(p -> p.getStatoPagamento() == TipoPagamento.ACCETTATO)
                .findFirst() // Récupère le premier paiement accepté trouvé
                .orElse(pagamenti.get(pagamenti.size() - 1)); // Sinon, prend le dernier élément de la liste

        // 2. Remplissage du DTO de réponse avec l'entité sélectionnée
        // Ici, 'nomeRicevuta' contiendra le vrai nom du fichier (ex: RICEVUTA_...pdf) si l'étape Jasper s'est déjà exécutée
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setIdPagamento(pagamento.getId());
        response.setIdOrdine(pagamento.getIdOrdine());
        response.setStatoPagamento(pagamento.getStatoPagamento());
        response.setNomeRicevuta(pagamento.getNomeRicevuta());

        return response;
    }

    /**
     * NETTOYAGE DE LA BASE DE DONNÉES : Supprime définitivement de la base de données toutes
     * les lignes de transactions qui ont échoué (statut 'RIFIUTATO').
     * Utile pour des tâches planifiées (Cron Jobs) afin de ne pas encombrer MySQL avec des échecs.
     */
    @Override
    @Transactional // Transaction requise car cette opération modifie (supprime) des lignes en BDD
    public void eliminaPagamentiRifiutati() {
        // Appel de la requête de suppression personnalisée du Repository
        pagamentoRepository.deleteByStatoPagamento(TipoPagamento.RIFIUTATO);
    }
}