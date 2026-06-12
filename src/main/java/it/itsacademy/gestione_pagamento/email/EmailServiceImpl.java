package it.itsacademy.gestione_pagamento.email;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl
        implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendPaymentAccepted(
            String email,
            String username) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Pagamento accettato");

        message.setText(
                "Ciao " + username +
                        ", il tuo pagamento è stato ACCETTATO."
        );

        mailSender.send(message);
    }

    @Override
    public void sendPaymentRejected(
            String email,
            String username) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Pagamento rifiutato");

        message.setText(
                "Ciao " + username +
                        ", il tuo pagamento è stato RIFIUTATO."
        );

        mailSender.send(message);
    }
}