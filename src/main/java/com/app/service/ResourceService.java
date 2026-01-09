package com.app.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.*;

/**
 * 资源服务
 * 提供资源详情查询功能，支持多种查询方式
 */
@Service
public class ResourceService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * 获取资源详情
     * 支持通过festival_name或id+table查询
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResourceDetail(String festivalName, Long resourceId, String table) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 如果使用id+table方式，需要转换为festival_name
            if (resourceId != null && table != null && !table.isEmpty()) {
                festivalName = getFestivalNameByIdAndTable(resourceId, table);
                if (festivalName == null) {
                    result.put("success", false);
                    result.put("message", "无法通过id+table获取festival_name");
                    return result;
                }
            }
            
            if (festivalName == null || festivalName.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "缺少festival_name参数或无法通过id+table获取festival_name");
                return result;
            }
            
            festivalName = festivalName.trim();
            
            // 1. 查询cultural_entities表获取实体信息
            String entitySql = "SELECT id, entity_name, description, entity_type, cultural_value " +
                    "FROM cultural_entities " +
                    "WHERE entity_name = :exact OR entity_name LIKE :partial " +
                    "ORDER BY CASE WHEN entity_name = :exact THEN 1 ELSE 2 END " +
                    "LIMIT 1";
            
            Query entityQuery = entityManager.createNativeQuery(entitySql);
            entityQuery.setParameter("exact", festivalName);
            entityQuery.setParameter("partial", "%" + festivalName + "%");
            
            List<Object[]> entityResults = entityQuery.getResultList();
            
            String entityName = festivalName;
            String description = "";
            Long entityId = null;
            
            if (!entityResults.isEmpty()) {
                Object[] entityRow = entityResults.get(0);
                entityId = ((Number) entityRow[0]).longValue();
                entityName = entityRow[1] != null ? entityRow[1].toString() : festivalName;
                description = entityRow[2] != null ? entityRow[2].toString() : "";
            }
            
            // 2. 查询图片列表
            List<Map<String, Object>> imageList = new ArrayList<>();
            
            if (entityId != null) {
                // 通过entity_id查询图片
                String imageSql = "SELECT id, file_name, storage_path, tags, dimensions, crawl_time, resource_id, entity_id " +
                        "FROM crawled_images " +
                        "WHERE entity_id = :entityId " +
                        "ORDER BY " +
                        "  CASE WHEN file_name != 'default.jpg' THEN 0 ELSE 1 END, " +
                        "  CASE " +
                        "    WHEN file_name REGEXP '^[0-9]+\\.[a-zA-Z]+$' THEN 1 " +
                        "    WHEN file_name REGEXP '^[0-9]+-[0-9]+\\.[a-zA-Z]+$' THEN 2 " +
                        "    ELSE 3 " +
                        "  END, " +
                        "  CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(file_name, '-', 1), '.', 1) AS UNSIGNED), " +
                        "  CASE " +
                        "    WHEN file_name REGEXP '^[0-9]+-[0-9]+\\.[a-zA-Z]+$' " +
                        "    THEN CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(file_name, '-', -1), '.', 1) AS UNSIGNED) " +
                        "    ELSE 0 " +
                        "  END";
                
                Query imageQuery = entityManager.createNativeQuery(imageSql);
                imageQuery.setParameter("entityId", entityId);
                
                List<Object[]> imageResults = imageQuery.getResultList();
                
                for (Object[] img : imageResults) {
                    Map<String, Object> imageMap = buildImageMap(img);
                    imageList.add(imageMap);
                    
                    // 如果还没有描述，尝试从图片的entity_id再次查询
                    if (description.isEmpty() && img[7] != null) {
                        Long imgEntityId = ((Number) img[7]).longValue();
                        if (imgEntityId.equals(entityId)) {
                            // 已经查询过了，跳过
                        }
                    }
                }
            } else {
                // 通过tags或festival_name查询图片（备用方案）
                String imageSql = "SELECT id, file_name, storage_path, tags, dimensions, crawl_time, resource_id, entity_id " +
                        "FROM crawled_images " +
                        "WHERE tags LIKE :tagPattern OR festival_name = :festivalName " +
                        "ORDER BY " +
                        "  CASE WHEN file_name != 'default.jpg' THEN 0 ELSE 1 END, " +
                        "  CASE " +
                        "    WHEN file_name REGEXP '^[0-9]+\\.[a-zA-Z]+$' THEN 1 " +
                        "    WHEN file_name REGEXP '^[0-9]+-[0-9]+\\.[a-zA-Z]+$' THEN 2 " +
                        "    ELSE 3 " +
                        "  END, " +
                        "  CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(file_name, '-', 1), '.', 1) AS UNSIGNED), " +
                        "  CASE " +
                        "    WHEN file_name REGEXP '^[0-9]+-[0-9]+\\.[a-zA-Z]+$' " +
                        "    THEN CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(file_name, '-', -1), '.', 1) AS UNSIGNED) " +
                        "    ELSE 0 " +
                        "  END";
                
                Query imageQuery = entityManager.createNativeQuery(imageSql);
                imageQuery.setParameter("tagPattern", "%" + festivalName + "%");
                imageQuery.setParameter("festivalName", festivalName);
                
                List<Object[]> imageResults = imageQuery.getResultList();
                
                for (Object[] img : imageResults) {
                    Map<String, Object> imageMap = buildImageMap(img);
                    imageList.add(imageMap);
                    
                    // 如果还没有描述，尝试从图片的entity_id查询
                    if (description.isEmpty() && img[7] != null) {
                        Long imgEntityId = ((Number) img[7]).longValue();
                        if (imgEntityId != null) {
                            String descSql = "SELECT entity_name, description FROM cultural_entities WHERE id = :entityId LIMIT 1";
                            Query descQuery = entityManager.createNativeQuery(descSql);
                            descQuery.setParameter("entityId", imgEntityId);
                            List<Object[]> descResults = descQuery.getResultList();
                            if (!descResults.isEmpty()) {
                                Object[] descRow = descResults.get(0);
                                entityName = descRow[0] != null ? descRow[0].toString() : entityName;
                                description = descRow[1] != null ? descRow[1].toString() : description;
                                entityId = imgEntityId;
                                break;
                            }
                        }
                    }
                }
            }
            
            // 3. 如果还没有描述，尝试从resource_id关联查询
            if (description.isEmpty() && !imageList.isEmpty()) {
                for (Map<String, Object> img : imageList) {
                    Object resourceIdObj = img.get("resource_id");
                    if (resourceIdObj != null) {
                        Long imgResourceId = ((Number) resourceIdObj).longValue();
                        String resourceSql = "SELECT content_feature_data FROM cultural_resources WHERE id = :resourceId LIMIT 1";
                        Query resourceQuery = entityManager.createNativeQuery(resourceSql);
                        resourceQuery.setParameter("resourceId", imgResourceId);
                        List<Object[]> resourceResults = resourceQuery.getResultList();
                        if (!resourceResults.isEmpty()) {
                            Object contentData = resourceResults.get(0)[0];
                            if (contentData != null) {
                                // 尝试解析JSON（简化处理）
                                String contentStr = contentData.toString();
                                // 这里可以添加JSON解析逻辑，暂时使用原始内容
                                if (contentStr.length() > 0) {
                                    description = contentStr.length() > 500 ? contentStr.substring(0, 500) : contentStr;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            // 4. 如果没有图片，返回默认图片
            if (imageList.isEmpty()) {
                if (entityId != null || !entityName.equals(festivalName)) {
                    Map<String, Object> defaultImage = new HashMap<>();
                    defaultImage.put("id", 0L);
                    defaultImage.put("file_name", "default.jpg");
                    defaultImage.put("image_url", "/default.jpg");
                    defaultImage.put("dimensions", null);
                    defaultImage.put("crawl_time", null);
                    imageList.add(defaultImage);
                } else {
                    result.put("success", false);
                    result.put("message", "未找到节日\"" + festivalName + "\"的资源");
                    return result;
                }
            }
            
            // 5. 构建返回结果
            result.put("success", true);
            result.put("festival_name", festivalName);
            result.put("entity_name", entityName);
            result.put("description", description.isEmpty() ? "暂无简介" : description);
            result.put("images", imageList);
            result.put("total_images", imageList.size());
            
            // 如果通过id+table查询，添加resource_id
            if (resourceId != null && table != null && !table.isEmpty()) {
                if ("cultural_resources".equals(table)) {
                    result.put("resource_id", resourceId);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取资源详情失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 通过id和table获取festival_name
     */
    @SuppressWarnings("unchecked")
    private String getFestivalNameByIdAndTable(Long id, String table) {
        try {
            String sql;
            switch (table) {
                case "cultural_entities":
                    sql = "SELECT entity_name FROM cultural_entities WHERE id = :id";
                    break;
                case "cultural_resources":
                    sql = "SELECT title FROM cultural_resources WHERE id = :id";
                    break;
                case "AIGC_cultural_entities":
                    sql = "SELECT entity_name FROM AIGC_cultural_entities WHERE id = :id";
                    break;
                case "AIGC_cultural_resources":
                    sql = "SELECT title FROM AIGC_cultural_resources WHERE id = :id";
                    break;
                case "crawled_images":
                    // 对于crawled_images表，通过entity_id或resource_id查找festival_name
                    sql = "SELECT ci.entity_id, ci.resource_id, ci.festival_name, ci.tags " +
                          "FROM crawled_images ci WHERE ci.id = :id";
                    break;
                case "AIGC_graph":
                    // AIGC_graph表可能不在数据库中，尝试通过文件名关联查找
                    // 这里先返回null，让前端使用festival_name参数
                    return null;
                default:
                    return null;
            }
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("id", id);
            
            if ("crawled_images".equals(table)) {
                // 处理crawled_images表的特殊查询
                List<Object[]> results = query.getResultList();
                if (!results.isEmpty()) {
                    Object[] row = results.get(0);
                    Long entityId = row[0] != null ? ((Number) row[0]).longValue() : null;
                    Long resourceId = row[1] != null ? ((Number) row[1]).longValue() : null;
                    String festivalName = row[2] != null ? row[2].toString() : null;
                    String tags = row[3] != null ? row[3].toString() : null;
                    
                    // 优先通过entity_id查找
                    if (entityId != null) {
                        Query entityQuery = entityManager.createNativeQuery(
                            "SELECT entity_name FROM cultural_entities WHERE id = :entityId");
                        entityQuery.setParameter("entityId", entityId);
                        List<Object[]> entityResults = entityQuery.getResultList();
                        if (!entityResults.isEmpty() && entityResults.get(0)[0] != null) {
                            return entityResults.get(0)[0].toString();
                        }
                    }
                    
                    // 其次通过resource_id查找
                    if (resourceId != null) {
                        Query resourceQuery = entityManager.createNativeQuery(
                            "SELECT title FROM cultural_resources WHERE id = :resourceId");
                        resourceQuery.setParameter("resourceId", resourceId);
                        List<Object[]> resourceResults = resourceQuery.getResultList();
                        if (!resourceResults.isEmpty() && resourceResults.get(0)[0] != null) {
                            return resourceResults.get(0)[0].toString();
                        }
                    }
                    
                    // 使用festival_name字段
                    if (festivalName != null && !festivalName.isEmpty()) {
                        return festivalName;
                    }
                    
                    // 最后从tags中提取
                    if (tags != null && !tags.isEmpty()) {
                        String[] tagArray = tags.split(",");
                        if (tagArray.length > 0) {
                            return tagArray[0].trim();
                        }
                    }
                }
                return null;
            } else {
                // 其他表的查询
                List<Object[]> results = query.getResultList();
                if (!results.isEmpty() && results.get(0)[0] != null) {
                    return results.get(0)[0].toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 构建图片Map
     */
    private Map<String, Object> buildImageMap(Object[] img) {
        Map<String, Object> imageMap = new HashMap<>();
        
        imageMap.put("id", img[0] != null ? ((Number) img[0]).longValue() : 0L);
        
        String fileName = img[1] != null ? img[1].toString() : null;
        String storagePath = img[2] != null ? img[2].toString() : null;
        
        // 构建图片URL
        String actualFile = null;
        if (storagePath != null && !storagePath.isEmpty()) {
            // 提取文件名
            if (storagePath.contains("/") || storagePath.contains("\\")) {
                actualFile = Paths.get(storagePath).getFileName().toString();
            } else {
                actualFile = storagePath;
            }
        } else if (fileName != null && !fileName.isEmpty()) {
            actualFile = fileName;
        }
        
        String imageUrl;
        if (actualFile != null && !actualFile.isEmpty()) {
            imageUrl = "/api/images/crawled/" + actualFile;
        } else {
            imageUrl = "/default.jpg";
        }
        
        imageMap.put("file_name", fileName);
        imageMap.put("image_url", imageUrl);
        imageMap.put("dimensions", img[4] != null ? img[4].toString() : null);
        imageMap.put("crawl_time", img[5] != null ? img[5].toString() : null);
        imageMap.put("resource_id", img[6] != null ? ((Number) img[6]).longValue() : null);
        imageMap.put("entity_id", img[7] != null ? ((Number) img[7]).longValue() : null);
        
        return imageMap;
    }
    
    /**
     * 获取首页资源列表
     * 从crawled_images和cultural_entities表查询
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHomeResources(int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 从crawled_images表获取图片资源，按resource_id或entity_id分组
            String imageSql = "SELECT id, file_name, storage_path, tags, dimensions, crawl_time, " +
                    "resource_id, entity_id, festival_name " +
                    "FROM crawled_images " +
                    "WHERE resource_id IS NOT NULL AND entity_id IS NOT NULL " +
                    "ORDER BY " +
                    "  CASE WHEN file_name != 'default.jpg' THEN 0 ELSE 1 END, " +
                    "  crawl_time DESC";
            
            Query imageQuery = entityManager.createNativeQuery(imageSql);
            List<Object[]> allImages = imageQuery.getResultList();
            
            // 按resource_id分组，每个资源只保留第一张图片（优先非default图片）
            Map<Long, Object[]> resourceImages = new LinkedHashMap<>();
            
            for (Object[] img : allImages) {
                if (img[6] == null) continue; // resource_id为空，跳过
                
                Long resourceId = ((Number) img[6]).longValue();
                
                // 如果这个资源还没有图片，或者当前图片不是default且已有的是default，则替换
                if (!resourceImages.containsKey(resourceId)) {
                    resourceImages.put(resourceId, img);
                } else {
                    Object[] existing = resourceImages.get(resourceId);
                    String existingFileName = existing[1] != null ? existing[1].toString() : "";
                    String currentFileName = img[1] != null ? img[1].toString() : "";
                    
                    // 如果已有的是default，当前不是default，则替换
                    if ("default.jpg".equals(existingFileName) && !"default.jpg".equals(currentFileName)) {
                        resourceImages.put(resourceId, img);
                    }
                }
            }
            
            // 转换为列表并按时间排序
            List<Object[]> festivalList = new ArrayList<>(resourceImages.values());
            festivalList.sort((a, b) -> {
                // 按crawl_time降序排序
                if (a[5] != null && b[5] != null) {
                    return b[5].toString().compareTo(a[5].toString());
                } else if (a[5] != null) {
                    return -1;
                } else if (b[5] != null) {
                    return 1;
                }
                return 0;
            });
            
            // 分页处理
            int totalCount = festivalList.size();
            int startIdx = (page - 1) * pageSize;
            int endIdx = Math.min(startIdx + pageSize, totalCount);
            List<Object[]> paginatedImages = startIdx < totalCount ? 
                festivalList.subList(startIdx, endIdx) : Collections.emptyList();
            
            // 2. 构建资源列表
            List<Map<String, Object>> resources = new ArrayList<>();
            
            for (Object[] img : paginatedImages) {
                Long entityId = img[7] != null ? ((Number) img[7]).longValue() : null;
                Long resourceId = img[6] != null ? ((Number) img[6]).longValue() : null;
                String festivalName = img[8] != null ? img[8].toString() : null;
                
                String entityName = "";
                String description = "";
                
                // 优先通过entity_id关联查询cultural_entities表
                if (entityId != null) {
                    String entitySql = "SELECT entity_name, description, entity_type, cultural_value " +
                            "FROM cultural_entities WHERE id = :entityId LIMIT 1";
                    Query entityQuery = entityManager.createNativeQuery(entitySql);
                    entityQuery.setParameter("entityId", entityId);
                    List<Object[]> entityResults = entityQuery.getResultList();
                    
                    if (!entityResults.isEmpty()) {
                        Object[] entityRow = entityResults.get(0);
                        entityName = entityRow[0] != null ? entityRow[0].toString() : "";
                        description = entityRow[1] != null ? entityRow[1].toString() : "";
                        if (description.length() > 200) {
                            description = description.substring(0, 200);
                        }
                    }
                }
                
                // 如果没有entity_name，尝试通过resource_id查询
                if (entityName.isEmpty() && resourceId != null) {
                    String resourceSql = "SELECT cr.id, cr.title, cr.content_feature_data " +
                            "FROM cultural_resources cr WHERE cr.id = :resourceId LIMIT 1";
                    Query resourceQuery = entityManager.createNativeQuery(resourceSql);
                    resourceQuery.setParameter("resourceId", resourceId);
                    List<Object[]> resourceResults = resourceQuery.getResultList();
                    
                    if (!resourceResults.isEmpty()) {
                        Object[] resourceRow = resourceResults.get(0);
                        String title = resourceRow[1] != null ? resourceRow[1].toString() : "";
                        if (!title.isEmpty()) {
                            entityName = title;
                        }
                    }
                }
                
                // 使用festival_name作为fallback
                if (entityName.isEmpty() && festivalName != null && !festivalName.isEmpty()) {
                    entityName = festivalName;
                }
                
                // 构建图片URL
                String storagePath = img[2] != null ? img[2].toString() : null;
                String fileName = img[1] != null ? img[1].toString() : null;
                String imageUrl = buildImageUrl(storagePath, fileName);
                
                // 构建资源对象
                Map<String, Object> resource = new HashMap<>();
                resource.put("id", resourceId);
                resource.put("festival_name", entityName);
                resource.put("entity_name", entityName);
                resource.put("description", description.isEmpty() ? "暂无简介" : description);
                resource.put("image_url", imageUrl);
                resource.put("total_images", 1); // 首页只显示一张图片
                resource.put("source", "cultural_entities");
                
                resources.add(resource);
            }
            
            // 3. 如果资源不足，从cultural_entities表补充
            if (resources.size() < pageSize) {
                int remaining = pageSize - resources.size();
                String entitySql = "SELECT id, entity_name, description, related_images_url " +
                        "FROM cultural_entities " +
                        "WHERE id NOT IN (SELECT DISTINCT entity_id FROM crawled_images WHERE entity_id IS NOT NULL) " +
                        "ORDER BY id DESC LIMIT :limit";
                
                Query entityQuery = entityManager.createNativeQuery(entitySql);
                entityQuery.setParameter("limit", remaining);
                List<Object[]> entityResults = entityQuery.getResultList();
                
                for (Object[] entityRow : entityResults) {
                    Map<String, Object> resource = new HashMap<>();
                    resource.put("id", ((Number) entityRow[0]).longValue());
                    resource.put("festival_name", entityRow[1] != null ? entityRow[1].toString() : "");
                    resource.put("entity_name", entityRow[1] != null ? entityRow[1].toString() : "");
                    String desc = entityRow[2] != null ? entityRow[2].toString() : "";
                    if (desc.length() > 200) {
                        desc = desc.substring(0, 200);
                    }
                    resource.put("description", desc.isEmpty() ? "暂无简介" : desc);
                    
                    String imageUrl = entityRow[3] != null ? entityRow[3].toString() : "/default.jpg";
                    resource.put("image_url", imageUrl);
                    resource.put("total_images", 0);
                    resource.put("source", "cultural_entities");
                    
                    resources.add(resource);
                }
            }
            
            // 4. 构建返回结果
            result.put("success", true);
            result.put("resources", resources);
            result.put("total", totalCount);
            result.put("page", page);
            result.put("page_size", pageSize);
            result.put("total_pages", (totalCount + pageSize - 1) / pageSize);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取资源失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 构建图片URL
     */
    private String buildImageUrl(String storagePath, String fileName) {
        String actualFile = null;
        if (storagePath != null && !storagePath.isEmpty()) {
            if (storagePath.contains("/") || storagePath.contains("\\")) {
                actualFile = Paths.get(storagePath).getFileName().toString();
            } else {
                actualFile = storagePath;
            }
        } else if (fileName != null && !fileName.isEmpty()) {
            actualFile = fileName;
        }
        
        if (actualFile != null && !actualFile.isEmpty()) {
            return "/api/images/crawled/" + actualFile;
        } else {
            return "/default.jpg";
        }
    }
}
