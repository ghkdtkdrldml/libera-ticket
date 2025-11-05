package com.libera.ticket.repo;

import com.libera.ticket.domain.AppStatus;
import com.libera.ticket.domain.Application;
import com.libera.ticket.domain.DomainType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Query("""
    SELECT SUM(a.totalCount) FROM Application a
    WHERE  a.status != com.libera.ticket.domain.AppStatus.CANCELED
    """)
    Integer sumTotalCount();

    @Query("""
  SELECT new map(
    COALESCE(p.name, '직접응모') as performer,
    COUNT(a) as appCount,
    SUM(a.totalCount) as people,
    SUM(CASE WHEN a.status = com.libera.ticket.domain.AppStatus.CANCELED THEN 1 ELSE 0 END) as canceled,
    SUM(CASE WHEN a.status != com.libera.ticket.domain.AppStatus.CANCELED THEN a.totalCount ELSE 0 END) as validPeople
  )
  FROM Application a
  LEFT JOIN a.performer p
  GROUP BY p.name
  ORDER BY validPeople DESC
  """)
    List<Map<String, Object>> findPerformerStats();

    // ✅ 상태별 카운트
    long countByStatus(AppStatus status);

    @Query("""
      select a from Application a
      left join fetch a.performer p
      where a.status = :status
      """)
    Page<Application> findPageByStatus(@Param("status") AppStatus status, Pageable pageable);

    // 목록: SUBMITTED만 + (연주자명 like q) 필터 (단, '허영우'는 검색 항목에서 제외)
    @Query("""
      select a from Application a
      left join a.performer p
      where a.status != com.libera.ticket.domain.AppStatus.CANCELED
        and ( :q is null or :q = '' or (p is not null and p.name <> '허영우' and p.name like concat('%', :q, '%')) )
      """)
    Page<Application> findSubmittedByPerformerLike(@Param("q") String q, Pageable pageable);

    // 총 응모 수(취소 제외=SUBMITTED), 같은 검색 필터 적용
    @Query("""
      select count(a) from Application a
      left join a.performer p
      where a.status != com.libera.ticket.domain.AppStatus.CANCELED
        and ( :q is null or :q = '' or (p is not null and p.name <> '허영우' and p.name like concat('%', :q, '%')) )
      """)
    long countSubmittedFiltered(@Param("q") String q);

    // 총 인원 수 합계(취소 제외=SUBMITTED), 같은 검색 필터 적용
    @Query("""
      select coalesce(sum(a.totalCount),0) from Application a
      left join a.performer p
      where a.status != com.libera.ticket.domain.AppStatus.CANCELED
        and ( :q is null or :q = '' or (p is not null and p.name <> '허영우' and p.name like concat('%', :q, '%')) )
      """)
    int sumPeopleSubmittedFiltered(@Param("q") String q);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a from Application a
        where a.status = com.libera.ticket.domain.AppStatus.SUBMITTED
    """)
    List<Application> findAllSubmittedForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.applicationId = :id")
    Optional<Application> findByIdForUpdate(@Param("id") UUID id);
}
