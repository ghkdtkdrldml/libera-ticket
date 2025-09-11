package com.libera.ticket.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Application {
    @Id
    private UUID applicationId = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DomainType domainType;

    @ManyToOne(fetch = FetchType.LAZY)
    private Performer performer; // INVITE일 때만

    @Column(nullable = false, length = 100)
    private String repName;
    @Column(nullable = false, length = 200)
    private String repEmail;
    @Column(nullable = false, length = 50)
    private String repPhone;

    @Column(nullable = false)
    private int totalCount;
    @Column(nullable = false)
    private boolean repDelivery;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppStatus status = AppStatus.SUBMITTED;
}
