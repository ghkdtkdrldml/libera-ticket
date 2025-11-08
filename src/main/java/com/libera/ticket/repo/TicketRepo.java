// com/libera/ticket/repo/TicketRepo.java
package com.libera.ticket.repo;
import com.libera.ticket.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepo extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByToken(String token);

    long countByApplication_ApplicationId(UUID applicationId);

    List<Ticket> findAllByApplication_ApplicationIdOrderByMemberRowAsc(UUID applicationId);

    // ✅ 입장팀(=사용된 티켓 수)
    @Query("""
       select count(t)
       from Ticket t
       where t.status = com.libera.ticket.domain.TicketStatus.USED
    """)
    long countUsedTeams();

    // ✅ 입장인원(=사용된 티켓의 총 인원)
    @Query("""
       select coalesce(sum(
         case when t.application.repDelivery = true
              then t.application.totalCount
              else 1
         end
       ), 0)
       from Ticket t
       where t.status = com.libera.ticket.domain.TicketStatus.USED
    """)
    long sumUsedPeople();

    // ✅ 잔여팀(=미사용 티켓 수, 취소는 제외)
    @Query("""
       select count(t)
       from Ticket t
       where t.status = com.libera.ticket.domain.TicketStatus.ISSUED
    """)
    long countUnusedTeams();

    // ✅ 잔여인원(=미사용 티켓의 총 인원, 취소 제외)
    @Query("""
       select coalesce(sum(
         case when t.application.repDelivery = true
              then t.application.totalCount
              else 1
         end
       ), 0)
       from Ticket t
       where t.status = com.libera.ticket.domain.TicketStatus.ISSUED
    """)
    long sumUnusedPeople();
}
