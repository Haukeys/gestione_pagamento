package it.itsacademy.gestione_pagamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // A METTRE ABSOLUMENT  SI ON VEUX QUE LE SCHEDULER FONCTIONE QUAND ON LANCE L APPLICATION
public class GestionePagamentoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionePagamentoApplication.class, args);
    }

}
