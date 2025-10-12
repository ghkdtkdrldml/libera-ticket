package com.libera.ticket.repo;

import com.libera.ticket.domain.AppStatus;
import com.libera.ticket.domain.Application;
import com.libera.ticket.domain.DomainType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ApplicationRepo extends JpaRepository<Application, UUID> {

    @Query("""
    select a from Application a
    left join a.performer p
    where (:q is null or :q = '' or 
           lower(a.repName) like lower(concat('%', :q, '%')) or
           lower(a.repEmail) like lower(concat('%', :q, '%')) or
           lower(a.repPhone) like lower(concat('%', :q, '%')) or
           (p is not null and lower(p.name) like lower(concat('%', :q, '%'))))
      and (:type is null or a.domainType = :type)
      and (:status is null or a.status = :status)
  """)
    Page<Application> search(String q, DomainType type, AppStatus status, Pageable pageable);
}
