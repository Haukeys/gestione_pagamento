package it.itsacademy.gestione_pagamento.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("orders.exchange");
    }

    // --- 1. CONFIGURATION FILE PAIEMENT ---
    @Bean
    public Queue paymentQueue() {
        return new Queue("queue.pagamenti", true);
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentQueue).to(exchange).with("order.routing.payment");
    }

    // --- 2. CONFIGURATION FILE REÇU (JASPER) ---
    @Bean
    public Queue receiptQueue() {
        return new Queue("queue.ricevute", true);
    }

    @Bean
    public Binding bindingReceipt(Queue receiptQueue, TopicExchange exchange) {
        return BindingBuilder.bind(receiptQueue).to(exchange).with("payment.routing.receipt");
    }

    // --- 3. CONFIGURATION FILE EMAIL ---
    @Bean
    public Queue emailQueue() {
        return new Queue("queue.notifications", true);
    }

    @Bean
    public Binding bindingEmail(Queue emailQueue, TopicExchange exchange) {
        return BindingBuilder.bind(emailQueue).to(exchange).with("receipt.routing.email");
    }
}