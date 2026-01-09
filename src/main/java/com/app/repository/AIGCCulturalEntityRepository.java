package com.app.repository;

import com.app.entity.AIGCCulturalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AIGCCulturalEntityRepository extends JpaRepository<AIGCCulturalEntity, Long> {
    
    @Query(value = "SELECT * FROM AIGC_cultural_entities WHERE " +
            "MATCH(entity_name, description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
            "OR entity_name LIKE CONCAT('%', :keyword, '%') " +
            "OR description LIKE CONCAT('%', :keyword, '%')", 
            nativeQuery = true)
    Page<AIGCCulturalEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query(value = "SELECT * FROM AIGC_cultural_entities WHERE " +
            "entity_name LIKE CONCAT('%', :keyword, '%') " +
            "OR description LIKE CONCAT('%', :keyword, '%')", 
            nativeQuery = true)
    Page<AIGCCulturalEntity> searchByKeywordLike(@Param("keyword") String keyword, Pageable pageable);
}
