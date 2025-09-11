package com.libera.ticket.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

@Entity @Getter @Setter @NoArgsConstructor
public class CancelToken {
    @Id private UUID token = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable=false, unique=true)
    private Application application;

    @Column(nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable=false) private LocalDateTime expiredAt;
}
