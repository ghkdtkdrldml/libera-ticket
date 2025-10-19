// com/libera/ticket/service/ResendEmailService.java
package com.libera.ticket.service;

import com.resend.Resend;
import com.resend.Resend.*;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService {
    private final String apiKey;
    private final String from;

    public ResendEmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from}") String from
    ) {
        this.apiKey = apiKey;
        this.from = from;
    }

    public CreateEmailResponse sendHtml(String to, String subject, String htmlBody) {
        Resend client = new Resend(apiKey);
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(htmlBody)
                .build();
        try {
            return client.emails().send(options);
        } catch (ResendException e) {
            throw new RuntimeException("Resend email 발송 실패: " + e.getMessage(), e);
        }
    }
}
