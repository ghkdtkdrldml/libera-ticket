package com.libera.ticket.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Getter @Setter
@Table(name="tickets", indexes = {
        @Index(name="ux_ticket_token", columnList="token", unique=true)
})
public class Ticket {
    @Id @GeneratedValue
    private UUID ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="application_id", nullable=false)
    private Application application;             // 기존 응모 엔티티

    private Integer memberRow;                   // 몇 번째 참가자(1,2,3..)

    private String memberName;
    private String memberEmail;
    private String memberPhone;

    @Column(nullable=false, length=64)
    private String token;                        // QR 토큰 (랜덤)

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.ISSUED;

    private OffsetDateTime issuedAt = OffsetDateTime.now();
    private OffsetDateTime usedAt;
}
