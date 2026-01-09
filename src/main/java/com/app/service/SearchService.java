package com.app.service;

import com.app.entity.AIGCCulturalEntity;
import com.app.entity.CulturalEntity;
import com.app.entity.CulturalResource;
import com.app.repository.AIGCCulturalEntityRepository;
import com.app.repository.CulturalEntityRepository;
import com.app.repository.CulturalResourceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务
 * 提供全文检索和AI检索功能，支持多表查询和高级搜索优化
 */
@Service
public class SearchService {
    
    @Autowired
    private CulturalEntityRepository culturalEntityRepository;
    
    @Autowired
    private AIGCCulturalEntityRepository aigcCulturalEntityRepository;
    
    @Autowired
    private CulturalResourceRepository culturalResourceRepository;
    
    @Autowired
    private AdvancedSearchService advancedSearchService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Value("${python.aigc.service.url:http://localhost:7200}")
    private String pythonAigcServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 全文检索
     * 支持多表查询（cultural_entities, AIGC_cultural_entities, cultural_resources）
     * 使用相关性排序，比Python版本更强大
     */
    @Transactional(readOnly = true)
    public Map<String, Object> fullTextSearch(String keyword, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            result.put("code", 400);
            result.put("msg", "请输入搜索关键词");
            result.put("data", Collections.emptyList());
            return result;
        }
        
        keyword = keyword.trim();
        
        try {
            // 检查是否支持全文索引
            boolean hasFulltext = checkFulltextIndex();
            
            // 查询结果列表
            List<Map<String, Object>> allResults = new ArrayList<>();
            
            // 1. 查询cultural_entities表
            List<Map<String, Object>> entityResults = searchCulturalEntities(keyword, hasFulltext);
            allResults.addAll(entityResults);
            
            // 2. 查询AIGC_cultural_entities表
            List<Map<String, Object>> aigcEntityResults = searchAIGCCulturalEntities(keyword, hasFulltext);
            allResults.addAll(aigcEntityResults);
            
            // 3. 查询cultural_resources表
            List<Map<String, Object>> resourceResults = searchCulturalResources(keyword);
            allResults.addAll(resourceResults);
            
            // 使用高级搜索服务增强结果排序
            allResults = advancedSearchService.enhanceSearchResults(
                keyword,
                allResults,
                item -> (String) item.get("title"),
                item -> (String) item.get("description")
            );
            
            // 记录搜索历史
            advancedSearchService.recordSearch(keyword);
            
            // 分页处理
            int total = allResults.size();
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, total);
            
            List<Map<String, Object>> pagedResults = start < total ? 
                allResults.subList(start, end) : Collections.emptyList();
            
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", pagedResults);
            result.put("total", total);
            result.put("page", page);
            result.put("page_size", pageSize);
            result.put("total_pages", (total + pageSize - 1) / pageSize);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "搜索失败: " + e.getMessage());
            result.put("data", Collections.emptyList());
        }
        
        return result;
    }
    
    /**
     * 检查是否支持全文索引
     */
    private boolean checkFulltextIndex() {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) as cnt " +
                "FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'cultural_entities' " +
                "AND INDEX_NAME = 'idx_ce_search'"
            );
            Object result = query.getSingleResult();
            return result != null && ((Number) result).intValue() > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 查询cultural_entities表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchCulturalEntities(String keyword, boolean hasFulltext) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            String sql;
            if (hasFulltext) {
                sql = "SELECT " +
                    "ce.id, " +
                    "ce.entity_name as title, " +
                    "ce.description, " +
                    "COALESCE((SELECT ci.storage_path FROM crawled_images ci WHERE ci.entity_id = ce.id LIMIT 1), ce.related_images_url) as image_url, " +
                    "COALESCE(ce.source, '') as source, " +
                    "'传统实体' as type_tag, " +
                    "CASE " +
                    "  WHEN ce.entity_name = :exact THEN 2.0 " +
                    "  WHEN MATCH(ce.entity_name, ce.description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) THEN 1.8 " +
                    "  WHEN ce.entity_name LIKE :partial THEN 1.5 " +
                    "  WHEN ce.description LIKE :partial THEN 1.0 " +
                    "  ELSE 0.5 " +
                    "END as relevance_score, " +
                    "1.0 as type_weight " +
                    "FROM cultural_entities ce " +
                    "WHERE MATCH(ce.entity_name, ce.description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
                    "   OR ce.entity_name LIKE :partial " +
                    "   OR ce.description LIKE :partial";
            } else {
                sql = "SELECT " +
                    "ce.id, " +
                    "ce.entity_name as title, " +
                    "ce.description, " +
                    "COALESCE((SELECT ci.storage_path FROM crawled_images ci WHERE ci.entity_id = ce.id LIMIT 1), ce.related_images_url) as image_url, " +
                    "COALESCE(ce.source, '') as source, " +
                    "'传统实体' as type_tag, " +
                    "CASE " +
                    "  WHEN ce.entity_name = :exact THEN 2.0 " +
                    "  WHEN ce.entity_name LIKE :partial THEN 1.5 " +
                    "  WHEN ce.description LIKE :partial THEN 1.0 " +
                    "  ELSE 0.5 " +
                    "END as relevance_score, " +
                    "1.0 as type_weight " +
                    "FROM cultural_entities ce " +
                    "WHERE ce.entity_name LIKE :partial OR ce.description LIKE :partial";
            }
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("exact", keyword);
            query.setParameter("keyword", keyword);
            query.setParameter("partial", "%" + keyword + "%");
            
            List<Object[]> rows = query.getResultList();
            
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", row[0]);
                item.put("title", row[1]);
                item.put("entity_name", row[1]);
                item.put("description", row[2] != null ? row[2].toString() : "");
                item.put("image_url", formatImageUrl(row[3]));
                item.put("source", row[4] != null ? row[4].toString() : "");
                item.put("tags", Collections.singletonList(row[5] != null ? row[5].toString() : "传统实体"));
                item.put("source_url", row[4] != null ? row[4].toString() : "#");
                item.put("relevance_score", row[6] != null ? ((Number) row[6]).doubleValue() : 0.0);
                item.put("type_weight", row[7] != null ? ((Number) row[7]).doubleValue() : 1.0);
                item.put("combined_score", 
                    ((Number) row[6]).doubleValue() * ((Number) row[7]).doubleValue());
                
                // 生成摘要
                String desc = item.get("description").toString();
                item.put("snippet", desc.length() > 100 ? desc.substring(0, 100) + "..." : desc);
                
                results.add(item);
            }
        } catch (Exception e) {
            System.err.println("查询cultural_entities失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * 查询AIGC_cultural_entities表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchAIGCCulturalEntities(String keyword, boolean hasFulltext) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            String sql;
            if (hasFulltext) {
                sql = "SELECT " +
                    "ace.id, " +
                    "ace.entity_name as title, " +
                    "ace.description, " +
                    "ace.related_images_url as image_url, " +
                    "'AIGC生成' as source, " +
                    "'AI实体' as type_tag, " +
                    "CASE " +
                    "  WHEN ace.entity_name = :exact THEN 1.6 " +
                    "  WHEN MATCH(ace.entity_name, ace.description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) THEN 1.4 " +
                    "  WHEN ace.entity_name LIKE :partial THEN 1.2 " +
                    "  WHEN ace.description LIKE :partial THEN 0.8 " +
                    "  ELSE 0.4 " +
                    "END as relevance_score, " +
                    "0.7 as type_weight " +
                    "FROM AIGC_cultural_entities ace " +
                    "WHERE MATCH(ace.entity_name, ace.description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
                    "   OR ace.entity_name LIKE :partial " +
                    "   OR ace.description LIKE :partial";
            } else {
                sql = "SELECT " +
                    "ace.id, " +
                    "ace.entity_name as title, " +
                    "ace.description, " +
                    "ace.related_images_url as image_url, " +
                    "'AIGC生成' as source, " +
                    "'AI实体' as type_tag, " +
                    "CASE " +
                    "  WHEN ace.entity_name = :exact THEN 1.6 " +
                    "  WHEN ace.entity_name LIKE :partial THEN 1.2 " +
                    "  WHEN ace.description LIKE :partial THEN 0.8 " +
                    "  ELSE 0.4 " +
                    "END as relevance_score, " +
                    "0.7 as type_weight " +
                    "FROM AIGC_cultural_entities ace " +
                    "WHERE ace.entity_name LIKE :partial OR ace.description LIKE :partial";
            }
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("exact", keyword);
            query.setParameter("keyword", keyword);
            query.setParameter("partial", "%" + keyword + "%");
            
            List<Object[]> rows = query.getResultList();
            
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", row[0]);
                item.put("title", row[1]);
                item.put("entity_name", row[1]);
                item.put("description", row[2] != null ? row[2].toString() : "");
                item.put("image_url", formatImageUrl(row[3]));
                item.put("source", row[4] != null ? row[4].toString() : "");
                item.put("tags", Collections.singletonList(row[5] != null ? row[5].toString() : "AI实体"));
                item.put("source_url", row[4] != null ? row[4].toString() : "#");
                item.put("relevance_score", row[6] != null ? ((Number) row[6]).doubleValue() : 0.0);
                item.put("type_weight", row[7] != null ? ((Number) row[7]).doubleValue() : 0.7);
                item.put("combined_score", 
                    ((Number) row[6]).doubleValue() * ((Number) row[7]).doubleValue());
                
                // 生成摘要
                String desc = item.get("description").toString();
                item.put("snippet", desc.length() > 100 ? desc.substring(0, 100) + "..." : desc);
                
                results.add(item);
            }
        } catch (Exception e) {
            System.err.println("查询AIGC_cultural_entities失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * 查询cultural_resources表
     */
    private List<Map<String, Object>> searchCulturalResources(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            List<CulturalResource> resources = culturalResourceRepository.searchByKeyword(keyword, 
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
            
            for (CulturalResource resource : resources) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", resource.getId());
                item.put("title", resource.getTitle());
                item.put("entity_name", resource.getTitle());
                
                String content = resource.getContentFeatureData();
                item.put("description", content != null ? content : "");
                item.put("image_url", "/default.jpg");
                item.put("source", resource.getSourceFrom() != null ? resource.getSourceFrom() : "");
                item.put("tags", Collections.singletonList("资源"));
                item.put("source_url", resource.getSourceUrl() != null ? resource.getSourceUrl() : "#");
                item.put("relevance_score", 0.8);
                item.put("type_weight", 0.6);
                item.put("combined_score", 0.48);
                
                // 生成摘要
                String desc = item.get("description").toString();
                item.put("snippet", desc.length() > 100 ? desc.substring(0, 100) + "..." : desc);
                
                results.add(item);
            }
        } catch (Exception e) {
            System.err.println("查询cultural_resources失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * 格式化图片URL
     */
    private String formatImageUrl(Object imageUrlObj) {
        if (imageUrlObj == null) {
            return "/default.jpg";
        }
        
        String imageUrl = imageUrlObj.toString();
        if (imageUrl.isEmpty() || "null".equals(imageUrl)) {
            return "/default.jpg";
        }
        
        // 如果是从crawled_images关联的路径
        if (imageUrl.contains("crawled_images") || imageUrl.startsWith("crawled_images")) {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            return "/api/images/crawled/" + fileName;
        }
        
        // 如果已经是完整URL
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        
        // 如果是以/开头的路径
        if (imageUrl.startsWith("/")) {
            return imageUrl;
        }
        
        // 其他情况，尝试作为文件名处理
        return "/api/images/crawled/" + imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }
    
    /**
     * AI检索
     * 调用Python AIGC服务进行AI分析，然后使用优化后的关键词进行搜索
     */
    @Transactional(readOnly = true)
    public Map<String, Object> aiSearch(String keyword, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            result.put("code", 400);
            result.put("msg", "请输入搜索关键词");
            result.put("data", Collections.emptyList());
            return result;
        }
        
        keyword = keyword.trim();
        
        try {
            // 1. 调用Python AIGC服务进行AI分析
            Map<String, Object> aiAnalysis = callPythonAISearch(keyword);
            
            // 2. 从AI分析结果中提取优化后的关键词
            String searchQuery = keyword;
            List<String> keywords = Collections.singletonList(keyword);
            
            if (aiAnalysis != null) {
                @SuppressWarnings("unchecked")
                List<String> aiKeywords = (List<String>) aiAnalysis.get("keywords");
                String aiSearchQuery = (String) aiAnalysis.get("search_query");
                
                if (aiKeywords != null && !aiKeywords.isEmpty()) {
                    keywords = aiKeywords;
                    searchQuery = aiKeywords.get(0);
                }
                if (aiSearchQuery != null && !aiSearchQuery.isEmpty()) {
                    searchQuery = aiSearchQuery;
                }
            }
            
            // 3. 使用优化后的关键词进行搜索（支持同义词扩展）
            Map<String, Object> processed = advancedSearchService.preprocessSearchQuery(searchQuery);
            @SuppressWarnings("unchecked")
            List<String> expandedQueries = (List<String>) processed.get("expanded_queries");
            
            // 合并所有查询关键词
            Set<String> allKeywords = new HashSet<>();
            allKeywords.add(searchQuery);
            allKeywords.addAll(keywords);
            allKeywords.addAll(expandedQueries);
            
            // 4. 使用所有关键词进行搜索
            List<Map<String, Object>> allResults = new ArrayList<>();
            
            for (String query : allKeywords) {
                if (query != null && !query.trim().isEmpty()) {
                    List<Map<String, Object>> entityResults = searchCulturalEntities(query, checkFulltextIndex());
                    List<Map<String, Object>> aigcEntityResults = searchAIGCCulturalEntities(query, checkFulltextIndex());
                    List<Map<String, Object>> resourceResults = searchCulturalResources(query);
                    
                    allResults.addAll(entityResults);
                    allResults.addAll(aigcEntityResults);
                    allResults.addAll(resourceResults);
                }
            }
            
            // 5. 去重并增强排序
            allResults = deduplicateResults(allResults);
            allResults = advancedSearchService.enhanceSearchResults(
                keyword,
                allResults,
                item -> (String) item.get("title"),
                item -> (String) item.get("description")
            );
            
            // 记录搜索历史
            advancedSearchService.recordSearch(keyword);
            
            // 6. 分页处理
            int total = allResults.size();
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, total);
            
            List<Map<String, Object>> pagedResults = start < total ? 
                allResults.subList(start, end) : Collections.emptyList();
            
            // 7. 构建返回结果
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", pagedResults);
            result.put("total", total);
            result.put("page", page);
            result.put("page_size", pageSize);
            result.put("total_pages", (total + pageSize - 1) / pageSize);
            result.put("ai_analysis", aiAnalysis != null ? aiAnalysis : Map.of(
                "keywords", keywords,
                "search_query", searchQuery
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            // 如果AI搜索失败，回退到普通搜索
            return fullTextSearch(keyword, page, pageSize);
        }
        
        return result;
    }
    
    /**
     * 调用Python AIGC服务进行AI分析
     */
    private Map<String, Object> callPythonAISearch(String keyword) {
        try {
            String url = pythonAigcServiceUrl + "/api/ai_search?q=" + 
                java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            ).getBody();
            
            if (response != null && response.containsKey("ai_analysis")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> aiAnalysis = (Map<String, Object>) response.get("ai_analysis");
                return aiAnalysis;
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("调用Python AI搜索服务失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 去重搜索结果
     */
    private List<Map<String, Object>> deduplicateResults(List<Map<String, Object>> results) {
        Map<String, Map<String, Object>> uniqueResults = new LinkedHashMap<>();
        
        for (Map<String, Object> result : results) {
            String key = result.get("id") + "_" + result.get("title");
            if (!uniqueResults.containsKey(key)) {
                uniqueResults.put(key, result);
            } else {
                // 如果已存在，保留相似度更高的
                Map<String, Object> existing = uniqueResults.get(key);
                double existingScore = ((Number) existing.getOrDefault("combined_score", 0.0)).doubleValue();
                double currentScore = ((Number) result.getOrDefault("combined_score", 0.0)).doubleValue();
                
                if (currentScore > existingScore) {
                    uniqueResults.put(key, result);
                }
            }
        }
        
        return new ArrayList<>(uniqueResults.values());
    }
}
