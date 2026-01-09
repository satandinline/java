package com.app.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 高级搜索服务
 * 提供同义词扩展、停用词过滤、相似度计算等功能
 * 比Python版本更强大，支持更智能的搜索优化
 */
@Service
public class AdvancedSearchService {
    
    private final Set<String> stopwords = new HashSet<>();
    private final Map<String, List<String>> synonymsDict = new HashMap<>();
    private final List<String> searchHistory = new ArrayList<>();
    
    public AdvancedSearchService() {
        loadStopwords();
        loadSynonyms();
    }
    
    /**
     * 加载停用词表
     */
    private void loadStopwords() {
        try {
            // 尝试从AIGC目录加载
            Path stopwordsPath = Paths.get(System.getProperty("user.dir"), "AIGC", "stopwords.txt");
            if (Files.exists(stopwordsPath)) {
                try (BufferedReader reader = Files.newBufferedReader(stopwordsPath, StandardCharsets.UTF_8)) {
                    reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .forEach(line -> {
                            // 支持逗号分隔的多个停用词
                            String[] words = line.split(",");
                            for (String word : words) {
                                String trimmed = word.trim();
                                if (!trimmed.isEmpty()) {
                                    stopwords.add(trimmed);
                                }
                            }
                        });
                }
            } else {
                // 如果文件不存在，使用默认停用词
                loadDefaultStopwords();
            }
        } catch (Exception e) {
            System.err.println("加载停用词失败，使用默认停用词: " + e.getMessage());
            loadDefaultStopwords();
        }
    }
    
    /**
     * 加载默认停用词
     */
    private void loadDefaultStopwords() {
        String[] defaultStopwords = {
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "它", "她", "他", "们", "这个", "那个", "这些", "那些",
            "什么", "怎么", "为什么", "哪个", "多少", "几", "第一", "第二", "第三", "又", "再", "更", "还", "可以", "能够", "应该"
        };
        stopwords.addAll(Arrays.asList(defaultStopwords));
    }
    
    /**
     * 加载同义词表
     */
    private void loadSynonyms() {
        try {
            // 尝试从AIGC目录加载
            Path synonymsPath = Paths.get(System.getProperty("user.dir"), "AIGC", "synonyms.txt");
            if (Files.exists(synonymsPath)) {
                try (BufferedReader reader = Files.newBufferedReader(synonymsPath, StandardCharsets.UTF_8)) {
                    reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .forEach(line -> {
                            // 按逗号分割同义词组
                            String[] synonyms = line.split(",");
                            List<String> synonymList = Arrays.stream(synonyms)
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());
                            
                            if (synonymList.size() > 1) {
                                // 为每个同义词建立映射
                                for (String word : synonymList) {
                                    synonymsDict.put(word, synonymList);
                                }
                            }
                        });
                }
            } else {
                // 如果文件不存在，使用默认同义词
                loadDefaultSynonyms();
            }
        } catch (Exception e) {
            System.err.println("加载同义词失败，使用默认同义词: " + e.getMessage());
            loadDefaultSynonyms();
        }
    }
    
    /**
     * 加载默认同义词
     */
    private void loadDefaultSynonyms() {
        // 中华文化相关同义词
        addSynonymGroup("春节", "新年", "农历新年", "春节假期");
        addSynonymGroup("中秋节", "月饼节", "仲秋节", "八月十五");
        addSynonymGroup("端午节", "龙舟节", "端午", "端阳");
        addSynonymGroup("清明节", "扫墓节", "踏青节");
        addSynonymGroup("元宵节", "灯节", "上元节");
        addSynonymGroup("七夕节", "乞巧节", "中国情人节");
        addSynonymGroup("重阳节", "登高节", "敬老节");
        addSynonymGroup("传统节日", "民俗节日", "民间节日");
        addSynonymGroup("传统文化", "中华文化", "中国文化");
        addSynonymGroup("民间艺术", "民俗艺术", "传统艺术");
    }
    
    /**
     * 添加同义词组
     */
    private void addSynonymGroup(String... words) {
        List<String> synonymList = Arrays.asList(words);
        for (String word : words) {
            synonymsDict.put(word, synonymList);
        }
    }
    
    /**
     * 移除停用词
     */
    public String removeStopwords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (String stopword : stopwords) {
            result = result.replace(stopword, "");
        }
        
        // 清理多余的空格
        return result.replaceAll("\\s+", " ").trim();
    }
    
    /**
     * 使用同义词扩展查询
     */
    public List<String> expandQueryWithSynonyms(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.singletonList(query);
        }
        
        Set<String> expandedQueries = new HashSet<>();
        expandedQueries.add(query);
        
        // 检查查询中的每个词是否有同义词
        for (Map.Entry<String, List<String>> entry : synonymsDict.entrySet()) {
            String word = entry.getKey();
            if (query.contains(word)) {
                List<String> synonyms = entry.getValue();
                for (String synonym : synonyms) {
                    if (!synonym.equals(word)) {
                        String expandedQuery = query.replace(word, synonym);
                        expandedQueries.add(expandedQuery);
                        // 也尝试部分替换
                        expandedQueries.add(query.replace(word, synonym + " " + word));
                    }
                }
            }
        }
        
        return new ArrayList<>(expandedQueries);
    }
    
    /**
     * 预处理搜索查询
     */
    public Map<String, Object> preprocessSearchQuery(String query) {
        Map<String, Object> result = new HashMap<>();
        
        if (query == null || query.isEmpty()) {
            result.put("original_query", "");
            result.put("cleaned_query", "");
            result.put("expanded_queries", Collections.emptyList());
            result.put("query_keywords", Collections.emptyList());
            return result;
        }
        
        // 移除停用词
        String cleanedQuery = removeStopwords(query);
        
        // 扩展查询
        List<String> expandedQueries = expandQueryWithSynonyms(query);
        
        // 提取关键词（简单分词，按字符和常见词组）
        List<String> keywords = extractKeywords(query);
        
        result.put("original_query", query);
        result.put("cleaned_query", cleanedQuery);
        result.put("expanded_queries", expandedQueries);
        result.put("query_keywords", keywords);
        
        return result;
    }
    
    /**
     * 提取关键词（简单实现，可以后续优化为使用专业分词工具）
     */
    private List<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> keywords = new ArrayList<>();
        
        // 简单分词：按常见分隔符分割
        String[] parts = text.split("[\\s,，。、；;：:]+");
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty() && !stopwords.contains(part) && part.length() > 1) {
                keywords.add(part);
            }
        }
        
        // 如果没有分割出关键词，尝试按字符分割（中文）
        if (keywords.isEmpty()) {
            for (char c : text.toCharArray()) {
                if (Character.isLetterOrDigit(c) || (c >= 0x4E00 && c <= 0x9FFF)) {
                    String charStr = String.valueOf(c);
                    if (!stopwords.contains(charStr)) {
                        keywords.add(charStr);
                    }
                }
            }
        }
        
        return keywords;
    }
    
    /**
     * 计算两个文本的相似度（基于词汇重叠和位置）
     */
    public double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }
        
        if (text1.equals(text2)) {
            return 1.0;
        }
        
        // 提取关键词
        List<String> keywords1 = extractKeywords(text1);
        List<String> keywords2 = extractKeywords(text2);
        
        if (keywords1.isEmpty() && keywords2.isEmpty()) {
            return 1.0;
        }
        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> set1 = new HashSet<>(keywords1);
        Set<String> set2 = new HashSet<>(keywords2);
        
        // Jaccard相似度
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        double jaccardSimilarity = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        
        // 位置相似度（如果关键词顺序相同，增加相似度）
        double positionSimilarity = calculatePositionSimilarity(keywords1, keywords2);
        
        // 综合相似度：Jaccard权重0.7，位置权重0.3
        return jaccardSimilarity * 0.7 + positionSimilarity * 0.3;
    }
    
    /**
     * 计算位置相似度
     */
    private double calculatePositionSimilarity(List<String> list1, List<String> list2) {
        if (list1.isEmpty() || list2.isEmpty()) {
            return 0.0;
        }
        
        int matches = 0;
        int maxLen = Math.max(list1.size(), list2.size());
        
        for (int i = 0; i < Math.min(list1.size(), list2.size()); i++) {
            if (list1.get(i).equals(list2.get(i))) {
                matches++;
            }
        }
        
        return maxLen == 0 ? 0.0 : (double) matches / maxLen;
    }
    
    /**
     * 增强搜索结果排序
     */
    public <T> List<T> enhanceSearchResults(String query, List<T> results, 
                                          java.util.function.Function<T, String> titleExtractor,
                                          java.util.function.Function<T, String> contentExtractor) {
        if (query == null || query.isEmpty() || results == null || results.isEmpty()) {
            return results;
        }
        
        // 预处理查询
        Map<String, Object> processed = preprocessSearchQuery(query);
        @SuppressWarnings("unchecked")
        List<String> expandedQueries = (List<String>) processed.get("expanded_queries");
        
        // 为每个结果计算相似度分数
        List<Map.Entry<T, Double>> scoredResults = new ArrayList<>();
        
        for (T result : results) {
            String title = titleExtractor.apply(result);
            String content = contentExtractor.apply(result);
            
            String fullText = (title != null ? title : "") + " " + (content != null ? content : "");
            
            // 计算原始查询相似度
            double origSimilarity = calculateSimilarity(query, fullText);
            
            // 计算扩展查询相似度
            double maxExpSimilarity = 0.0;
            for (String expQuery : expandedQueries) {
                double expSimilarity = calculateSimilarity(expQuery, fullText);
                maxExpSimilarity = Math.max(maxExpSimilarity, expSimilarity);
            }
            
            // 最终相似度取最大值
            double finalSimilarity = Math.max(origSimilarity, maxExpSimilarity);
            
            // 如果标题完全匹配，增加权重
            if (title != null && title.contains(query)) {
                finalSimilarity = Math.max(finalSimilarity, 0.9);
            }
            
            scoredResults.add(new AbstractMap.SimpleEntry<>(result, finalSimilarity));
        }
        
        // 按相似度排序
        scoredResults.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 返回排序后的结果
        return scoredResults.stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * 记录搜索历史
     */
    public void recordSearch(String query) {
        if (query != null && !query.trim().isEmpty()) {
            searchHistory.add(query.trim());
            // 只保留最近100条搜索记录
            if (searchHistory.size() > 100) {
                searchHistory.remove(0);
            }
        }
    }
    
    /**
     * 获取搜索统计信息
     */
    public Map<String, Object> getSearchStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_searches", searchHistory.size());
        stats.put("unique_queries", new HashSet<>(searchHistory).size());
        stats.put("last_search", searchHistory.isEmpty() ? null : searchHistory.get(searchHistory.size() - 1));
        return stats;
    }
}
