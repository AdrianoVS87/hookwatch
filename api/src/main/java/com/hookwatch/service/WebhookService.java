package com.hookwatch.service;

import com.hookwatch.domain.Webhook;
import com.hookwatch.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;

    @Transactional(readOnly = true)
    public List<Webhook> findAll() {
        return webhookRepository.findAll();
    }

    @Transactional(readOnly = true)
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
