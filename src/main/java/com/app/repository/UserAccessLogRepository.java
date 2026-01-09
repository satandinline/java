package com.app.repository;

import com.app.entity.UserAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserAccessLogRepository extends JpaRepository<UserAccessLog, Long> {
    long countByAccessType(String accessType);
    
    @Query("SELECT COUNT(DISTINCT l.userId) FROM UserAccessLog l WHERE l.accessType = :accessType")
    long countDistinctUsersByAccessType(@Param("accessType") String accessType);
    
    @Query("SELECT COUNT(DISTINCT l.userId) FROM UserAccessLog l WHERE l.accessType = :accessType AND l.accessTime >= :startTime")
    long countDistinctUsersByAccessTypeAndTime(@Param("accessType") String accessType, @Param("startTime") LocalDateTime startTime);
    
    long countByAccessTypeAndAccessTimeGreaterThanEqual(String accessType, LocalDateTime startTime);
}
