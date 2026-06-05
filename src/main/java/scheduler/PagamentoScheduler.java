package scheduler;

import it.itsacademy.gestione_pagamento.service.PagamentoServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PagamentoScheduler {

    private final PagamentoServiceImpl pagamentoServiceImpl;

    //Tâche planifiée qui s'exécute toutes les 5 minutes (configuré dans application.properties).

    @Scheduled(fixedRateString = "${cron.clean.payments.rate:300000}")
    public void executeAutomaticCleanup() {
        try {
            pagamentoServiceImpl.cleanFailedPayments();
            System.out.println("[SCHEDULER] Eliminazaione avvenuta con successo tutti i pagamenti  'RIFIUTATO' sono stati eliminati.");
        } catch (Exception e) {
            System.err.println("[SCHEDULER] Errore durante l'eliminazione dei pagamenti 'RIFIUTATo' : " + e.getMessage());
        }
    }
}
