package com.hookwatch.service;

import com.hookwatch.domain.Webhook;
import com.hookwatch.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;

    public List<Webhook> findAll() {
        return webhookRepository.findAll();
    }

    public Optional<Webhook> findById(UUID id) {
        return webhookRepository.findById(id);
    }

    public Webhook save(Webhook webhook) {
        return webhookRepository.save(webhook);
    }

    public void deleteById(UUID id) {
        webhookRepository.deleteById(id);
    }
}
