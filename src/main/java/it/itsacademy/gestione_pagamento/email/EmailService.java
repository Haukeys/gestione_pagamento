package it.itsacademy.gestione_pagamento.email;

public interface EmailService {

    void sendPaymentAccepted(
            String email,
            String username);

    void sendPaymentRejected(
            String email,
            String username);
}