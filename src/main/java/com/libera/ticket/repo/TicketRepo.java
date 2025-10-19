// com/libera/ticket/repo/TicketRepo.java
package com.libera.ticket.repo;
import com.libera.ticket.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepo extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByToken(String token);

    long countByApplication_ApplicationId(UUID applicationId);

    List<Ticket> findAllByApplication_ApplicationIdOrderByMemberRowAsc(UUID applicationId);
}
