package com.libera.ticket.domain;

import lombok.Getter;

@Getter
public enum NotificationKind {

    ENTRY_SUBMITTED       ("entry_submitted",       "mail.subject.entry_submitted"),
    APPLICATION_SUBMITTED ("application_submitted", "mail.subject.application_submitted"),
    ENTRY_CONFIRMED       ("entry_confirmed",       "mail.subject.entry_confirmed"),
    APPLICATION_CONFIRMED ("application_confirmed", "mail.subject.application_confirmed"),
    TICKET_SENT           ("ticket_sent",           "mail.subject.ticket_sent");

    private final String templateName;   // mail/sms 둘 다 이 이름으로 사용
    private final String subjectKey;

    NotificationKind(String templateName, String subjectKey) {
        this.templateName = templateName;
        this.subjectKey = subjectKey;
    }

    public String getMailTemplateName(){
        return "mail/"+this.templateName;
    }

    public String getSmsTemplateName(){
        return "sms/"+this.templateName;
    }
}