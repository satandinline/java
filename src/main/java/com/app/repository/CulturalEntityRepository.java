package com.app.repository;

import com.app.entity.CulturalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CulturalEntityRepository extends JpaRepository<CulturalEntity, Long> {
    
    @Query(value = "SELECT * FROM cultural_entities WHERE " +
            "MATCH(entity_name, description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
            "OR entity_name LIKE CONCAT('%', :keyword, '%') " +
            "OR description LIKE CONCAT('%', :keyword, '%')", 
            nativeQuery = true)
    Page<CulturalEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query(value = "SELECT * FROM cultural_entities WHERE " +
            "entity_name LIKE CONCAT('%', :keyword, '%') " +
            "OR description LIKE CONCAT('%', :keyword, '%')", 
            nativeQuery = true)
    Page<CulturalEntity> searchByKeywordLike(@Param("keyword") String keyword, Pageable pageable);
}
