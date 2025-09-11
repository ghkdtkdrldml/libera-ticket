package com.libera.ticket.repo;

import com.libera.ticket.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ApplicationMemberRepo extends JpaRepository<ApplicationMember, Long> {
    List<ApplicationMember> findByApplication_ApplicationIdOrderByRowOrderAsc(UUID applicationId);
}
