package com.cyberassessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncMailService {

    private final JavaMailSender mailSender;

    @Async("applicationTaskExecutor")
    public void send(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send async email to {}", String.join(",", message.getTo() == null ? new String[0] : message.getTo()), e);
        }
    }
}
