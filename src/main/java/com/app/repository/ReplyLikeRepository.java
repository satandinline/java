package com.app.repository;

import com.app.entity.ReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReplyLikeRepository extends JpaRepository<ReplyLike, Long> {
    Optional<ReplyLike> findByReplyIdAndUserId(Long replyId, Long userId);
    long countByReplyId(Long replyId);
}
