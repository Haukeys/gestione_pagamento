package it.itsacademy.gestione_pagamento.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j // Injecte la variable statique "log" liée à Logback
@RestControllerAdvice // Indique à Spring d'intercepter les exceptions de tous les contrôleurs
public class GlobalExceptionHandler {

    /**
     * Intercepte spécifiquement les exceptions de type ResponseStatusException (comme le 404 Not Found).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
        // Journalisation de l'erreur au niveau ERROR pour Logback
        log.error("[SERVICE ERREUR] HTTP status Error: {} - {}", ex.getStatusCode(), ex.getReason());

        return new ResponseEntity<>(ex.getReason(), ex.getStatusCode());
    }

    /**
     * Intercepte toutes les autres exceptions non gérées (Runtime, SQL, pannes réseau, etc.).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        // Journalisation de l'erreur complète avec la stacktrace pour l'équipe technique
        log.error("[SERVICE ERROR] Unknown error occur in service: ", ex);

        return new ResponseEntity<>("Internal error occur during payment.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}