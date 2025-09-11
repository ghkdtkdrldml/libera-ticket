package com.libera.ticket.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Performer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long performerId;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
