package com.libera.ticket.repo;

import com.libera.ticket.domain.AppStatus;
import com.libera.ticket.domain.Application;
import com.libera.ticket.domain.DomainType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ApplicationRepo extends JpaRepository<Application, UUID> {

    // 기존에 있던 메서드(참고): Page<Application> search(String q, DomainType type, AppStatus status, Pageable pageable);

    @Query("""
    select a from Application a
      left join a.performer p
    where (:q is null or :q = '' or
           lower(a.repName) like lower(concat('%',:q,'%')) or
           lower(a.repEmail) like lower(concat('%',:q,'%')) or
           lower(a.repPhone) like lower(concat('%',:q,'%')) or
           (p is not null and lower(p.name) like lower(concat('%',:q,'%'))))
      and (:type is null or a.domainType = :type)
      and (:status is null or a.status = :status)
      and a.createdAt >= coalesce(:start, a.createdAt)
      and a.createdAt <= coalesce(:end,   a.createdAt)
    """)
    Page<Application> search(@Param("q") String q,
                             @Param("type") DomainType type,
                             @Param("status") AppStatus status,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end,
                             Pageable pageable);

    // 상단 통계: totalCount 합계
    @Query("""
    select coalesce(sum(a.totalCount),0) from Application a
      left join a.performer p
    where (:q is null or :q = '' or
           lower(a.repName) like lower(concat('%',:q,'%')) or
           lower(a.repEmail) like lower(concat('%',:q,'%')) or
           lower(a.repPhone) like lower(concat('%',:q,'%')) or
           (p is not null and lower(p.name) like lower(concat('%',:q,'%'))))
      and (:type is null or a.domainType = :type)
      and (:status is null or a.status = :status)
      and a.createdAt >= coalesce(:start, a.createdAt)
      and a.createdAt <= coalesce(:end,   a.createdAt)
    """)
    long sumTotalCount(@Param("q") String q,
                       @Param("type") DomainType type,
                       @Param("status") AppStatus status,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end);

    // 엑셀용: 페이징 없이 정렬 포함 전체 리스트
    @Query("""
    select a from Application a
      left join a.performer p
    where (:q is null or :q = '' or
           lower(a.repName) like lower(concat('%',:q,'%')) or
           lower(a.repEmail) like lower(concat('%',:q,'%')) or
           lower(a.repPhone) like lower(concat('%',:q,'%')) or
           (p is not null and lower(p.name) like lower(concat('%',:q,'%'))))
      and (:type is null or a.domainType = :type)
      and (:status is null or a.status = :status)
      and a.createdAt >= coalesce(:start, a.createdAt)
      and a.createdAt <= coalesce(:end,   a.createdAt)
    """)
    List<Application> searchList(@Param("q") String q,
                                 @Param("type") DomainType type,
                                 @Param("status") AppStatus status,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 Sort sort);

    @Query("SELECT SUM(a.totalCount) FROM Application a")
    Integer sumTotalCount();

    @Query("""
  SELECT new map(
    COALESCE(p.name, '직접응모') as performer,
    COUNT(a) as appCount,
    SUM(a.totalCount) as people,
    SUM(CASE WHEN a.status = com.libera.ticket.domain.AppStatus.CANCELED THEN 1 ELSE 0 END) as canceled,
    SUM(CASE WHEN a.status = com.libera.ticket.domain.AppStatus.SUBMITTED THEN a.totalCount ELSE 0 END) as validPeople
  )
  FROM Application a
  LEFT JOIN a.performer p
  GROUP BY p.name
  ORDER BY validPeople DESC
  """)
    List<Map<String, Object>> findPerformerStats();

    // ✅ 상태별 카운트
    long countByStatus(AppStatus status);
}
