package com.libera.ticket.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ApplicationMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Application application;

    @Column(nullable = false)
    private int rowOrder; // 1부터
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 200)
    private String email; // 대표수령이면 null
    @Column(length = 50)
    private String phone; // 대표수령이면 null
}
