package it.itsacademy.gestione_pagamento.controller;

import it.itsacademy.gestione_pagamento.dto.PagamentoHistoryDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentRequestDTO;
import it.itsacademy.gestione_pagamento.dto.PaymentResponseDTO;
import it.itsacademy.gestione_pagamento.service.PagamentoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@Slf4j //on ajoute cette extention pour utiliser LogBack() genere automatiquement le champ static log
@RestController
@RequestMapping("/pagamenti")//GUARDARE BENE SE LA PORTA E QUELLA GIUSTA DIVERSO DA 8080
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    /**
     * Point d'entrée pour le RestClient du projet gestione_ordini.
     * HttpServletRequest httpRequest,ajoute pour la gestion des logs
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> handlePayment(@RequestBody @Valid PaymentRequestDTO request,HttpServletRequest httpRequest) {

        // Interception et traçabilité de l'identité de l'appelant
        logRequestInfos(httpRequest);

        PaymentResponseDTO response = pagamentoService.processPayment(request);
        return ResponseEntity.ok(response);
    }
    // Dans la classe PagamentoController :
    @GetMapping("/ordine/{idOrdine}")
    public ResponseEntity<List<PagamentoHistoryDTO>> getListPagamenti(@PathVariable("idOrdine") UUID idOrdine,HttpServletRequest httpRequest) {

        // Interception et traçabilité de l'identité de l'appelant
        logRequestInfos(httpRequest);

        List<PagamentoHistoryDTO> history = pagamentoService.getPaymentsByOrderId(idOrdine);
        return ResponseEntity.ok(history);
    }
    @GetMapping("/ordine/{idOrdine}/status")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatusByOrdineId(@PathVariable("idOrdine") UUID idOrdine,HttpServletRequest httpRequest) {

        // Interception et traçabilité de l'identité de l'appelant
        logRequestInfos(httpRequest);
        PaymentResponseDTO response = pagamentoService.getPaymentStatusByOrdineId(idOrdine);

        return ResponseEntity.ok(response);
    }
    @GetMapping(path = "/health")//pour check health avec docker
    public void health() {}

    /**
     * Méthode utilitaire pour extraire les métadonnées de la Gateway et journaliser l'appel
     * cette methode et placer ici pour bonne pratique car tout ce qui regarder strictement hhtp va implementer ici
     */
    private void logRequestInfos(HttpServletRequest request) {
        // Extraction de la méthode (GET, POST, DELETE, PUT...) et de l'URI demandée
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Extraction des en-têtes(ou header) injectés en amont par l'AuthenticationFilter du Gateway
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-User-Name");

        // Formatage de la chaîne de caractères représentant l'identité
        String userIdentifier;
        if (username != null && userId != null) {
            userIdentifier = username + " (ID: " + userId + ")";
        } else if (username != null) {
            userIdentifier = username;
        } else {
            userIdentifier = "Systeme / Unknow";
        }

        // Transmission de la ligne de journalisation au niveau INFO
        //L'utilisation de "{}" évite de concaténer manuellement des chaînes de caractères, permettant ainsi une évaluation dynamique au moment de l'exécution (optimisation des performances).
        log.info("[API INBOUND] User: {} | HTTP Method: {} | URI: {}", userIdentifier, method, uri);
    }
}
