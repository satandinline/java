package com.app.repository;

import com.app.entity.QAMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QAMessageRepository extends JpaRepository<QAMessage, Long> {
    Page<QAMessage> findBySessionIdOrderByCreateTimeAsc(Long sessionId, Pageable pageable);
    
    List<QAMessage> findBySessionIdOrderByCreateTimeAsc(Long sessionId);
    
    long countBySessionId(Long sessionId);
    
    @Query("SELECT m FROM QAMessage m WHERE m.sessionId = :sessionId AND m.userMessage IS NOT NULL AND m.userMessage != '' ORDER BY m.createTime ASC")
    List<QAMessage> findUserMessagesBySessionId(@Param("sessionId") Long sessionId);
    
    void deleteBySessionId(Long sessionId);
}
