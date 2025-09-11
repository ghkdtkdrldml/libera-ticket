package com.libera.ticket.repo;

import com.libera.ticket.domain.Performer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface PerformerRepo extends JpaRepository<Performer, Long> {
    Optional<Performer> findByNameIgnoreCase(String name);
}
