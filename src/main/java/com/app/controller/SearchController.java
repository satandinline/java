package com.app.controller;

import com.app.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 搜索控制器
 * 提供全文检索和AI检索功能
 */
@RestController
@RequestMapping("/api")
public class SearchController {
    
    @Autowired
    private SearchService searchService;

    /**
     * 全文检索接口
     * 支持多表查询，使用相关性排序
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> fullTextSearch(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "keyword", required = false) String keywordAlt,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "100") int pageSize) {
        
        // 支持q和keyword两种参数名
        if (keyword == null || keyword.isEmpty()) {
            keyword = keywordAlt;
        }
        
        if (pageSize <= 0) {
            pageSize = 100;
        }
        if (page < 1) {
            page = 1;
        }
        
        Map<String, Object> result = searchService.fullTextSearch(keyword, page, pageSize);
        
        int code = (Integer) result.get("code");
        if (code == 200) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(code == 400 ? 400 : 500).body(result);
        }
    }

    /**
     * AI检索接口
     * 使用AI分析用户需求，优化搜索关键词，支持同义词扩展
     */
    @GetMapping("/ai_search")
    public ResponseEntity<Map<String, Object>> aiSearch(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "keyword", required = false) String keywordAlt,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "8") int pageSize) {
        
        // 支持q和keyword两种参数名
        if (keyword == null || keyword.isEmpty()) {
            keyword = keywordAlt;
        }
        
        if (pageSize <= 0) {
            pageSize = 8;
        }
        if (page < 1) {
            page = 1;
        }
        
        Map<String, Object> result = searchService.aiSearch(keyword, page, pageSize);
        
        int code = (Integer) result.get("code");
        if (code == 200) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(code == 400 ? 400 : 500).body(result);
        }
    }
}

