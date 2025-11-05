package com.libera.ticket.service;

import com.libera.ticket.domain.Channel;
import com.libera.ticket.domain.NotificationKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SpringTemplateEngine templateEngine;
    private final ResendEmailService resendEmailService; // 기존 메일 발송기
    private final SmsService smsService;                 // 기존 문자 발송기
    private final MessageSource messageSource;

    /**
     * 통합 발송 엔트리포인트.
     * @param kind NotificationKind (템플릿/제목 포함)
     * @param channel EMAIL or SMS
     * @param to 수신자 (메일주소 or 전화번호)
     * @param model 타임리프 변수 (detailUrl, cancelUrl 등)
     */
    public void send(NotificationKind kind, Channel channel, String to, Map<String, Object> model) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(to, "to");

        try {
            switch (channel) {
                case EMAIL -> sendEmail(kind, to, model);
                case SMS   -> sendSms(kind, to, model);
                default    -> throw new IllegalArgumentException("Unsupported channel: " + channel);
            }
        } catch (Exception e) {
            log.warn("Notification send failed. kind={}, channel={}, to={}, err={}",
                    kind, channel, to, e.toString());
        }
    }

    private void sendEmail(NotificationKind kind, String to, Map<String, Object> model) {

        String subject = messageSource.getMessage(kind.getSubjectKey(), null, Locale.getDefault());

        // 레이아웃 <title>에 쓰고 싶다면 model에 subject를 넣어줌(선택)
//        if (model == null) model = new HashMap<>();
//        model.putIfAbsent("subject", subject);


        Context ctx = new Context(Locale.getDefault(), model);
        String html = templateEngine.process(kind.getMailTemplateName(), ctx);

            resendEmailService.sendHtml(to, subject, html);
        log.debug("✅ Email sent. kind={}, to={}", kind.name(), to);
    }

    private void sendSms(NotificationKind kind, String to, Map<String, Object> model) {
        Context ctx = new Context(Locale.getDefault(), model == null ? Map.of() : model);
        String text = templateEngine.process(kind.getSmsTemplateName(), ctx).trim();

        smsService.sendSms(to, text);
        log.debug("✅ SMS sent. kind={}, to={}", kind.name(), to);
    }

}