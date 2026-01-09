package com.app.repository;

import com.app.entity.QASession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QASessionRepository extends JpaRepository<QASession, Long> {
    Page<QASession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT s FROM QASession s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<QASession> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    long countByUserId(Long userId);
}
